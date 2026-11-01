package com.krizaka.users.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * One lockout counter: failures so far, when the last one happened, until when the lock holds.
 *
 * @param failures failures since the counter was last forgotten
 * @param lastFailureAt when the last failure happened
 * @param lockedUntil when the lock ends, {@code null} while not locked
 */
public record LockoutState(int failures, Instant lastFailureAt, Instant lockedUntil) {

  // @fixed: a lock doubles at most 2^10 times its base before the ceiling applies
  private static final int MAX_DOUBLINGS = 10;

  public LockoutState {
    Objects.requireNonNull(lastFailureAt, "lastFailureAt");
  }

  /** The state after one more failure counted against {@code threshold}. */
  public static LockoutState afterFailure(
      LockoutState old, int threshold, LockoutPolicy policy, Instant now) {
    boolean stale =
        old == null
            || Duration.between(old.lastFailureAt(), now).compareTo(policy.forgetAfter()) > 0;
    int failures = stale ? 1 : old.failures() + 1;
    Instant lockedUntil = stale ? null : old.lockedUntil();
    if (failures >= threshold) {
      int doublings = Math.min(failures - threshold, MAX_DOUBLINGS);
      Duration delay = policy.baseDelay().multipliedBy(1L << doublings);
      if (delay.compareTo(policy.maxDelay()) > 0) {
        delay = policy.maxDelay();
      }
      lockedUntil = now.plus(delay);
    }
    return new LockoutState(failures, now, lockedUntil);
  }

  /** Seconds to wait before trying again, {@code 0} when {@code state} is absent or not locked. */
  public static long retryAfterSeconds(LockoutState state, Instant now) {
    if (state == null || state.lockedUntil() == null || !state.lockedUntil().isAfter(now)) {
      return 0;
    }
    long millis = Duration.between(now, state.lockedUntil()).toMillis();
    return Math.max(1, (millis + 999) / 1000);
  }
}
