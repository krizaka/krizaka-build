package com.krizaka.users.service.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.benmanes.caffeine.cache.Ticker;
import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.LockoutPolicy;
import com.krizaka.users.domain.model.LockoutState;
import com.krizaka.users.domain.ports.inbound.LoginLockoutService;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The brute-force guard of the public auth endpoints (krizaka-users#17). */
class AuthRateLimitFilterTest {

  /** Same shapes as AuthController: 401 on a wrong password, 200 otherwise. */
  @RestController
  @RequestMapping("/api/v1/auth")
  static class FakeAuth {
    @PostMapping("/login")
    ResponseEntity<Object> login(@RequestBody Map<String, String> body) {
      return "right".equals(body.get("password"))
          ? ResponseEntity.ok(Map.of("token", "t"))
          : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid"));
    }

    @PostMapping("/register")
    ResponseEntity<Object> register(@RequestBody Map<String, String> body) {
      return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("email", body.get("email")));
    }

    @PostMapping("/forgot")
    ResponseEntity<Object> forgot(@RequestBody Map<String, String> body) {
      return ResponseEntity.ok(Map.of("message", "ok"));
    }
  }

  private final AtomicLong nanos = new AtomicLong();
  private final FakeLockout lockout = new FakeLockout(nanos);
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    Ticker clock = nanos::get;
    var props =
        new AuthRateLimitProperties(
            true,
            java.util.List.of("127.0.0.0/8", "10.0.0.0/8"),
            "",
            new AuthRateLimitProperties.Budget(20, java.time.Duration.ofMinutes(1)),
            new AuthRateLimitProperties.Budget(30, java.time.Duration.ofMinutes(1)),
            new AuthRateLimitProperties.Budget(5, java.time.Duration.ofHours(1)),
            new AuthRateLimitProperties.Budget(5, java.time.Duration.ofHours(1)),
            new AuthRateLimitProperties.Budget(10, java.time.Duration.ofMinutes(15)),
            new AuthRateLimitProperties.Budget(20, java.time.Duration.ofMinutes(1)),
            new AuthRateLimitProperties.Budget(3, java.time.Duration.ofHours(1)));
    mvc =
        MockMvcBuilders.standaloneSetup(new FakeAuth())
            .addFilters(new AuthRateLimitFilter(props, clock, lockout))
            .build();
  }

  private ResultActions call(String path, String json, String ip) throws Exception {
    return mvc.perform(
        post("/api/v1/auth/" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .with(
                r -> {
                  r.setRemoteAddr(ip);
                  return r;
                }));
  }

  private static String creds(String email, String password) {
    return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
  }

  @Test
  void aSingleAddressGuessingManyAccountsGetsA429WithRetryAfterAndProblemJson() throws Exception {
    int refused = 0;
    for (int i = 0; i < 25; i++) {
      var result = call("login", creds("user" + i + "@x.io", "wrong"), "203.0.113.9").andReturn();
      if (result.getResponse().getStatus() == 429) {
        refused++;
        assertThat(i).isEqualTo(20 + refused - 1); // the 21st request is the first refused
        assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After"))).isPositive();
      }
    }
    assertThat(refused).isEqualTo(5);
    call("login", creds("another@x.io", "wrong"), "203.0.113.9")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":429")));
    // another address is not affected
    call("login", creds("another@x.io", "wrong"), "203.0.113.10")
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sixWrongPasswordsLockTheAccountEvenForTheRightPasswordThenItOpensAgain() throws Exception {
    for (int i = 0; i < 5; i++) {
      call("login", creds("eric@x.io", "wrong"), "198.51.100.7")
          .andExpect(status().isUnauthorized());
    }
    call("login", creds("eric@x.io", "right"), "198.51.100.7")
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
    // the lock is for this address: the owner elsewhere still gets in
    call("login", creds("eric@x.io", "right"), "198.51.100.8").andExpect(status().isOk());

    nanos.addAndGet(java.time.Duration.ofSeconds(61).toNanos());
    call("login", creds("eric@x.io", "right"), "198.51.100.7").andExpect(status().isOk());
  }

  @Test
  void theLockDoublesWithEveryFurtherFailure() throws Exception {
    for (int i = 0; i < 5; i++) {
      call("login", creds("lea@x.io", "wrong"), "198.51.100.7");
    }
    nanos.addAndGet(java.time.Duration.ofSeconds(61).toNanos());
    call("login", creds("lea@x.io", "wrong"), "198.51.100.7").andExpect(status().isUnauthorized());
    // 6th failure: locked 120 s now
    var refused = call("login", creds("lea@x.io", "right"), "198.51.100.7").andReturn();
    assertThat(refused.getResponse().getStatus()).isEqualTo(429);
    assertThat(Long.parseLong(refused.getResponse().getHeader("Retry-After")))
        .isBetween(100L, 120L);
  }

  @Test
  void aDistributedAttackOnOneAccountLocksItForEveryone() throws Exception {
    for (int i = 0; i < 25; i++) {
      call("login", creds("victim@x.io", "wrong"), "192.0.2." + (i + 1))
          .andExpect(status().isUnauthorized());
    }
    call("login", creds("victim@x.io", "right"), "192.0.2.200")
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void registerAndForgotAreThrottledPerAddressAndPerMailbox() throws Exception {
    // a mailbox is mailed at most 3 times an hour, however many addresses ask
    for (int i = 0; i < 3; i++) {
      call("forgot", "{\"email\":\"Victim@X.io\"}", "192.0.2." + (i + 1))
          .andExpect(status().isOk());
    }
    call("forgot", "{\"email\":\"victim@x.io\"}", "192.0.2.50")
        .andExpect(status().isTooManyRequests());
    call("register", "{\"email\":\"victim@x.io\",\"username\":\"v\"}", "192.0.2.51")
        .andExpect(status().isTooManyRequests());

    // one address cannot spray many mailboxes: 5 an hour
    for (int i = 0; i < 5; i++) {
      call("register", "{\"email\":\"n" + i + "@x.io\"}", "203.0.113.1")
          .andExpect(status().isCreated());
    }
    call("register", "{\"email\":\"n9@x.io\"}", "203.0.113.1")
        .andExpect(status().isTooManyRequests());

    nanos.addAndGet(java.time.Duration.ofHours(1).toNanos() + 1);
    call("register", "{\"email\":\"n9@x.io\"}", "203.0.113.1").andExpect(status().isCreated());
  }

  @Test
  void aForgedForwardedForDoesNotBuyAFreshBudget() throws Exception {
    // behind the edge: the peer is the edge (10.x); the edge appends the real peer to the header
    for (int i = 0; i < 20; i++) {
      mvc.perform(
              post("/api/v1/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(creds("u" + i + "@x.io", "wrong"))
                  .header("X-Forwarded-For", "1.1.1." + i + ", 203.0.113.99")
                  .with(
                      r -> {
                        r.setRemoteAddr("10.0.0.5");
                        return r;
                      }))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(creds("z@x.io", "wrong"))
                .header("X-Forwarded-For", "9.9.9.9, 203.0.113.99")
                .with(
                    r -> {
                      r.setRemoteAddr("10.0.0.5");
                      return r;
                    }))
        .andExpect(status().isTooManyRequests());
  }

  /** The durable lockout in memory, on the test clock: the rules are the domain's own. */
  static final class FakeLockout implements LoginLockoutService {
    private final AtomicLong nanos;
    private final Map<String, LockoutState> states = new java.util.HashMap<>();
    private final LockoutPolicy policy = LockoutPolicy.defaults();

    FakeLockout(AtomicLong nanos) {
      this.nanos = nanos;
    }

    private Instant now() {
      return Instant.EPOCH.plusNanos(nanos.get());
    }

    @Override
    public long retryAfterSeconds(String account, String ip) {
      return Math.max(
          LockoutState.retryAfterSeconds(states.get("p|" + account + "|" + ip), now()),
          LockoutState.retryAfterSeconds(states.get("a|" + account), now()));
    }

    @Override
    public void recordFailure(String account, String ip) {
      states.compute(
          "p|" + account + "|" + ip,
          (k, old) -> LockoutState.afterFailure(old, policy.threshold(), policy, now()));
      states.compute(
          "a|" + account,
          (k, old) -> LockoutState.afterFailure(old, policy.accountThreshold(), policy, now()));
    }

    @Override
    public void recordSuccess(String account, String ip) {
      states.remove("p|" + account + "|" + ip);
    }

    @Override
    public LockoutPolicy policy() {
      return policy;
    }

    @Override
    public LockoutPolicy updatePolicy(AdminActor actor, LockoutPolicy next) {
      throw new UnsupportedOperationException();
    }
  }
}
