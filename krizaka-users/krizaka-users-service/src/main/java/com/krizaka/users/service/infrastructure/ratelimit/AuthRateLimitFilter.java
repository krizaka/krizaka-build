package com.krizaka.users.service.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.krizaka.users.domain.ports.inbound.LoginLockoutService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Throttles the public authentication endpoints (krizaka-users#17): login, oauth, register, forgot,
 * reset and verify. Answers {@code 429} with {@code Retry-After} and an RFC 9457 {@code
 * application/problem+json} body.
 *
 * <ul>
 *   <li>per client address, per endpoint, a Bucket4j token bucket;
 *   <li>per e-mail address on the endpoints that send a message ({@code register}, {@code forgot}):
 *       a stranger's mailbox cannot be flooded through us, however many addresses the caller has;
 *   <li>on {@code login}, a progressive lockout of failed attempts (durable, {@link
 *       LoginLockoutService}).
 * </ul>
 */
final class AuthRateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
  private static final String BASE = "/api/v1/auth/";
  private static final int MAX_BODY_BYTES = 16 * 1024;
  private static final Pattern EMAIL = Pattern.compile("\"email\"\\s*:\\s*\"([^\"\\\\]{1,320})\"");

  private final AuthRateLimitProperties props;
  private final ClientIpResolver ips;
  private final LoginLockoutService lockout;
  private final Ticker ticker;
  private final Cache<String, Bucket> buckets;

  AuthRateLimitFilter(AuthRateLimitProperties props, Ticker ticker, LoginLockoutService lockout) {
    this.props = props;
    this.ticker = ticker;
    this.ips = new ClientIpResolver(props.trustedProxies(), props.clientIpHeader());
    this.lockout = lockout;
    this.buckets =
        Caffeine.newBuilder()
            .ticker(ticker)
            .maximumSize(200_000)
            .expireAfterAccess(Duration.ofHours(2))
            .build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return !props.enabled()
        || !"POST".equals(request.getMethod())
        || !path.startsWith(BASE)
        || budgetFor(path.substring(BASE.length())) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String endpoint = request.getRequestURI().substring(BASE.length());
    String ip = ips.resolve(request);

    HttpServletRequest effective = request;
    String email = null;
    if (needsAccount(endpoint)) {
      byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
      effective = new CachedBodyRequest(request, body);
      email = extractEmail(body);
    }

    long wait = takeIp(endpoint, ip);
    if (wait == 0 && email != null && sendsMail(endpoint)) {
      wait = takeEmail(endpoint, email);
    }
    if (wait == 0 && email != null && "login".equals(endpoint)) {
      wait = lockedFor(email, ip);
    }
    if (wait > 0) {
      log.warn("Auth throttled: endpoint={} ip={} retryAfter={}s", endpoint, ip, wait);
      tooManyRequests(response, wait);
      return;
    }

    chain.doFilter(effective, response);

    if (email != null && "login".equals(endpoint)) {
      String account = email;
      if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
        record(() -> lockout.recordFailure(account, ip));
      } else if (response.getStatus() / 100 == 2) {
        record(() -> lockout.recordSuccess(account, ip));
      }
    }
  }

  /** The lockout store is the database: if it fails, the per-address budget still protects. */
  private long lockedFor(String email, String ip) {
    try {
      return lockout.retryAfterSeconds(email, ip);
    } catch (RuntimeException e) {
      log.error("Lockout check unavailable ({}): not enforced", e.getClass().getSimpleName());
      return 0;
    }
  }

  private void record(Runnable write) {
    try {
      write.run();
    } catch (RuntimeException e) {
      log.error("Lockout update lost ({})", e.getClass().getSimpleName());
    }
  }

  private AuthRateLimitProperties.Budget budgetFor(String endpoint) {
    return switch (endpoint) {
      case "login" -> props.login();
      case "oauth" -> props.oauth();
      case "register" -> props.register();
      case "forgot" -> props.forgot();
      case "reset" -> props.reset();
      case "verify" -> props.verify();
      default -> null;
    };
  }

  private static boolean needsAccount(String endpoint) {
    return "login".equals(endpoint) || sendsMail(endpoint);
  }

  private static boolean sendsMail(String endpoint) {
    return "register".equals(endpoint) || "forgot".equals(endpoint);
  }

  private long takeIp(String endpoint, String ip) {
    return take("ip|" + endpoint + "|" + ip, budgetFor(endpoint));
  }

  private long takeEmail(String endpoint, String email) {
    // One mailbox is mailed by register and forgot alike: they share the budget.
    return take("mail|" + email, props.emailMessages());
  }

  /** Takes a token; returns {@code 0} when granted, else the seconds to wait. */
  private long take(String key, AuthRateLimitProperties.Budget budget) {
    Bucket bucket =
        buckets.get(
            key,
            k ->
                Bucket.builder()
                    .withCustomTimePrecision(
                        new io.github.bucket4j.TimeMeter() {
                          @Override
                          public long currentTimeNanos() {
                            return ticker.read();
                          }

                          @Override
                          public boolean isWallClockBased() {
                            return false;
                          }
                        })
                    .addLimit(
                        Bandwidth.builder()
                            .capacity(budget.requests())
                            .refillIntervally(budget.requests(), budget.window())
                            .build())
                    .build());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return 0;
    }
    return Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
  }

  private static String extractEmail(byte[] body) {
    if (body.length == 0 || body.length > MAX_BODY_BYTES) {
      return null;
    }
    Matcher m = EMAIL.matcher(new String(body, StandardCharsets.UTF_8));
    return m.find() ? m.group(1).trim().toLowerCase(Locale.ROOT) : null;
  }

  private static void tooManyRequests(HttpServletResponse response, long retryAfter)
      throws IOException {
    response.setStatus(429);
    response.setHeader("Retry-After", Long.toString(retryAfter));
    response.setContentType("application/problem+json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(
            "{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
                + "\"detail\":\"Too many attempts. Try again in "
                + retryAfter
                + " seconds.\",\"retryAfterSeconds\":"
                + retryAfter
                + "}");
  }

  /** Replays an already-read body to the rest of the chain. */
  private static final class CachedBodyRequest
      extends jakarta.servlet.http.HttpServletRequestWrapper {
    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request, byte[] body) {
      super(request);
      this.body = body;
    }

    @Override
    public jakarta.servlet.ServletInputStream getInputStream() {
      ByteArrayInputStream in = new ByteArrayInputStream(body);
      return new jakarta.servlet.ServletInputStream() {
        @Override
        public int read() {
          return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
          return in.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
          return in.available() == 0;
        }

        @Override
        public boolean isReady() {
          return true;
        }

        @Override
        public void setReadListener(jakarta.servlet.ReadListener listener) {
          throw new UnsupportedOperationException();
        }
      };
    }

    @Override
    public java.io.BufferedReader getReader() {
      return new java.io.BufferedReader(
          new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
  }
}
