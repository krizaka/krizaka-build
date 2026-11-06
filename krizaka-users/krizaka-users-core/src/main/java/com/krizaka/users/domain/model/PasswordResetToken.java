package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable domain record representing a password reset token.
 *
 * <p>Stores a SHA-256 hashed token with a time-bound expiration. The plaintext token is never
 * persisted — only the hash is stored and used for lookup during the reset flow.
 *
 * @param id Unique identifier for the token record (UUID string).
 * @param email The email address of the user requesting the reset.
 * @param tokenHash The SHA-256 hex-encoded hash of the plaintext token.
 * @param expiresAt The expiration timestamp (current time + 15 minutes at creation).
 */
public record PasswordResetToken(String id, String email, String tokenHash, Instant expiresAt) {

  /** Compact constructor enforcing fail-fast invariants (ERR-106). */
  public PasswordResetToken {
    Objects.requireNonNull(id, "Password reset token ID cannot be null");
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    Objects.requireNonNull(expiresAt, "Expiration timestamp cannot be null");
  }

  /**
   * Checks whether this token has expired.
   *
   * @return {@code true} if the token's expiration is before the current instant.
   */
  public boolean isExpired() {
    return expiresAt.isBefore(Instant.now());
  }
}
