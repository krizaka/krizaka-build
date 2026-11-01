package com.krizaka.users.domain.model;

import java.util.Objects;

/**
 * Application event published when a password reset is requested.
 *
 * <p>Carries the plaintext token and target email for downstream email integration. The plaintext
 * token is transient and never persisted — only used for email dispatch.
 *
 * @param email The email address the reset was requested for.
 * @param plaintextToken The generated plaintext token to include in the reset link.
 */
public record PasswordResetRequestedEvent(String email, String plaintextToken) {

  /** Compact constructor enforcing fail-fast invariants. */
  public PasswordResetRequestedEvent {
    Objects.requireNonNull(email, "Email cannot be null");
    Objects.requireNonNull(plaintextToken, "Plaintext token cannot be null");
  }
}
