package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Immutable domain record representing an email verification token. */
public record VerificationToken(
    String id,
    String userId,
    String tokenType,
    String tokenHash,
    Instant expiryTimestamp,
    boolean used) {
  public VerificationToken {
    Objects.requireNonNull(id, "Verification token ID cannot be null");
    Objects.requireNonNull(userId, "User ID cannot be null");
    Objects.requireNonNull(tokenType, "Token type cannot be null");
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    Objects.requireNonNull(expiryTimestamp, "Expiry timestamp cannot be null");
  }
}
