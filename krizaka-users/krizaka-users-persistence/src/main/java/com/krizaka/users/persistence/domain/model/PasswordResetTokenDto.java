package com.krizaka.users.persistence.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persistence DTO for password reset tokens, bridging the domain model and JPA entity layers.
 *
 * @param id Unique identifier for the token record (UUID string).
 * @param email The email address of the user requesting the reset.
 * @param tokenHash The SHA-256 hex-encoded hash of the plaintext token.
 * @param expiresAt The expiration timestamp.
 * @param createdAt The creation timestamp.
 */
public record PasswordResetTokenDto(
    String id, String email, String tokenHash, Instant expiresAt, Instant createdAt) {

  /** Compact constructor enforcing fail-fast invariants (ERR-106). */
  public PasswordResetTokenDto {
    Objects.requireNonNull(id, "ID cannot be null");
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    Objects.requireNonNull(expiresAt, "Expiration timestamp cannot be null");
  }
}
