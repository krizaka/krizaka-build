package com.krizaka.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.LockoutPolicy;
import com.krizaka.users.domain.model.LockoutScope;
import com.krizaka.users.domain.model.LockoutState;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.domain.ports.outbound.LockoutRepositoryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/** The durable lockout (ADR-016 slice 2): same rules as before, now surviving the process. */
class LoginLockoutServiceImplTest {

  /** The store in memory; what is kept in it is what the database would keep. */
  static final class MemoryStore implements LockoutRepositoryPort {
    final Map<String, LockoutState> rows = new HashMap<>();
    LockoutPolicy policy = LockoutPolicy.defaults();

    private static String key(LockoutScope s, String h) {
      return s + "|" + h;
    }

    @Override
    public Optional<LockoutState> find(LockoutScope scope, String hash) {
      return Optional.ofNullable(rows.get(key(scope, hash)));
    }

    @Override
    public Optional<LockoutState> findForUpdate(LockoutScope scope, String hash) {
      return find(scope, hash);
    }

    @Override
    public void save(LockoutScope scope, String hash, LockoutState state) {
      rows.put(key(scope, hash), state);
    }

    @Override
    public void delete(LockoutScope scope, String hash) {
      rows.remove(key(scope, hash));
    }

    @Override
    public int purgeIdleBefore(Instant before) {
      return 0;
    }

    @Override
    public LockoutPolicy policy() {
      return policy;
    }

    @Override
    public void savePolicy(LockoutPolicy next, String updatedBy) {
      policy = next;
    }
  }

  private final MemoryStore store = new MemoryStore();
  private final AdminUserRepositoryPort audit = mock(AdminUserRepositoryPort.class);
  private Instant now = Instant.parse("2026-10-28T12:00:00Z");

  private LoginLockoutServiceImpl service() {
    Clock clock =
        new Clock() {
          @Override
          public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return now;
          }
        };
    return new LoginLockoutServiceImpl(store, audit, clock);
  }

  @Test
  void theFifthFailureLocksTheAddressAndTheLockSurvivesANewServiceInstance() {
    var first = service();
    for (int i = 0; i < 5; i++) {
      first.recordFailure("eric@x.io", "198.51.100.7");
    }

    // "restart": a new instance over the same store still refuses
    var restarted = service();
    assertThat(restarted.retryAfterSeconds("eric@x.io", "198.51.100.7")).isBetween(59L, 60L);
    // the lock is for this address; the owner elsewhere is not locked
    assertThat(restarted.retryAfterSeconds("eric@x.io", "198.51.100.8")).isZero();

    now = now.plusSeconds(61);
    assertThat(restarted.retryAfterSeconds("eric@x.io", "198.51.100.7")).isZero();
  }

  @Test
  void theLockDoublesWithEveryFurtherFailureUpToTheCeiling() {
    var s = service();
    for (int i = 0; i < 5; i++) {
      s.recordFailure("lea@x.io", "198.51.100.7");
    }
    now = now.plusSeconds(61);
    s.recordFailure("lea@x.io", "198.51.100.7");
    assertThat(s.retryAfterSeconds("lea@x.io", "198.51.100.7")).isBetween(100L, 120L);

    for (int i = 0; i < 20; i++) {
      now = now.plus(Duration.ofMinutes(16));
      s.recordFailure("lea@x.io", "198.51.100.7");
    }
    assertThat(s.retryAfterSeconds("lea@x.io", "198.51.100.7")).isLessThanOrEqualTo(900L);
  }

  @Test
  void aDistributedAttackLocksTheAccountForEveryAddressAndTheCaseOfTheMailChangesNothing() {
    var s = service();
    for (int i = 0; i < 25; i++) {
      s.recordFailure(i % 2 == 0 ? "Victim@X.io" : "victim@x.io", "192.0.2." + (i + 1));
    }
    assertThat(s.retryAfterSeconds("victim@x.io", "192.0.2.200")).isPositive();
  }

  @Test
  void aSuccessClearsThePairButKeepsTheAccountWideCounter() {
    var s = service();
    for (int i = 0; i < 4; i++) {
      s.recordFailure("a@x.io", "198.51.100.7");
    }
    s.recordSuccess("a@x.io", "198.51.100.7");
    s.recordFailure("a@x.io", "198.51.100.7");

    assertThat(s.retryAfterSeconds("a@x.io", "198.51.100.7")).isZero();
    assertThat(store.rows.keySet()).anyMatch(k -> k.startsWith("ACCOUNT|"));
  }

  @Test
  void theStoreHoldsHashesNeverTheMailbox() {
    var s = service();
    s.recordFailure("eric@x.io", "198.51.100.7");

    assertThat(store.rows.keySet())
        .noneMatch(k -> k.contains("eric") || k.contains("198.51"))
        .allMatch(k -> k.matches("(PAIR|ACCOUNT)\\|[0-9a-f]{64}"));
  }

  @Test
  void changingThePolicyIsAuditedAndTakesEffectAtOnce() {
    var s = service();
    s.updatePolicy(
        new AdminActor("u-1", "eric@example.test"),
        new LockoutPolicy(2, 10, Duration.ofMinutes(5), Duration.ofHours(1), Duration.ofHours(2)));

    s.recordFailure("a@x.io", "198.51.100.7");
    s.recordFailure("a@x.io", "198.51.100.7");

    assertThat(s.retryAfterSeconds("a@x.io", "198.51.100.7")).isBetween(299L, 300L);
    verify(audit)
        .appendAudit(
            ArgumentMatchers.eq("u-1"),
            ArgumentMatchers.eq("eric@example.test"),
            ArgumentMatchers.eq("identity.lockout_policy_changed"),
            ArgumentMatchers.eq("lockout-policy"),
            ArgumentMatchers.contains("from=5/25/"));
  }

  @Test
  void aSenselessPolicyIsRefused() {
    assertThatThrownBy(
            () ->
                new LockoutPolicy(
                    0, 10, Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofHours(1)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
            () ->
                new LockoutPolicy(
                    5, 3, Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofHours(1)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
            () ->
                new LockoutPolicy(
                    5, 25, Duration.ofMinutes(10), Duration.ofMinutes(5), Duration.ofHours(1)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(
            () ->
                new LockoutPolicy(
                    5, 25, Duration.ofMinutes(1), Duration.ofDays(30), Duration.ofHours(1)))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void failuresAreForgottenAfterAQuietTime() {
    var s = service();
    for (int i = 0; i < 4; i++) {
      s.recordFailure("a@x.io", "198.51.100.7");
    }
    now = now.plus(Duration.ofMinutes(31));
    s.recordFailure("a@x.io", "198.51.100.7");

    assertThat(s.retryAfterSeconds("a@x.io", "198.51.100.7")).isZero();
  }
}
