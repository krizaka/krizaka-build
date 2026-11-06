package com.krizaka.users.domain.model;

import java.util.Objects;

/**
 * Immutable domain record representing a user's API credential configuration status.
 *
 * @param providerName The AI provider name (e.g. "openai", "claude", "gemini").
 * @param configured Whether the credential exists for this user.
 */
public record UserCredential(String providerName, boolean configured) {

  /** Compact constructor validating arguments. */
  public UserCredential {
    Objects.requireNonNull(providerName, "providerName must not be null");
    if (providerName.isBlank()) {
      throw new IllegalArgumentException("providerName must not be blank");
    }
  }
}
