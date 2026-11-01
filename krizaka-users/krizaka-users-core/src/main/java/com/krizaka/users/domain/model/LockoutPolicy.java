package com.krizaka.users.domain.model;

import com.krizaka.users.domain.exception.InvalidRequestException;
import java.time.Duration;
import java.util.Objects;

/**
 * The thresholds of the progressive lockout of failed sign-ins, kept in the database and changed by
 * an administrator (audited).
 *
 * @param threshold consecutive failures of one account from one address before the first lock
 * @param accountThreshold failures of one account from any address before it is locked for all
 * @param baseDelay first lock; doubles with every further failure
 * @param maxDelay ceiling of a lock
 * @param forgetAfter quiet time after which the failures are forgotten
 */
public record LockoutPolicy(
    int threshold,
    int accountThreshold,
    Duration baseDelay,
    Duration maxDelay,
    Duration forgetAfter) {

  // @fixed: ceiling an administrator may set, so a typo cannot lock an account for months
  private static final Duration MAX_LOCK = Duration.ofDays(1);

  public LockoutPolicy {
    Objects.requireNonNull(baseDelay, "baseDelay");
    Objects.requireNonNull(maxDelay, "maxDelay");
    Objects.requireNonNull(forgetAfter, "forgetAfter");
    if (threshold < 1 || accountThreshold < threshold) {
      throw new InvalidRequestException(
          "Lockout thresholds: threshold >= 1 and accountThreshold >= threshold");
    }
    if (baseDelay.isNegative() || baseDelay.isZero() || maxDelay.compareTo(baseDelay) < 0) {
      throw new InvalidRequestException("Lockout delays: 0 < baseDelay <= maxDelay");
    }
    if (maxDelay.compareTo(MAX_LOCK) > 0 || forgetAfter.isNegative() || forgetAfter.isZero()) {
      throw new InvalidRequestException("Lockout: maxDelay <= 24 h and forgetAfter > 0");
    }
  }

  /** The production stance, also what a fresh database holds. */
  public static LockoutPolicy defaults() {
    return new LockoutPolicy(
        5, 25, Duration.ofMinutes(1), Duration.ofMinutes(15), Duration.ofMinutes(30));
  }
}
