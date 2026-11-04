package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * Thrown when the user has no key for the provider (or the key is another user's: its existence is
 * not revealed).
 */
public class CredentialNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs the exception for a provider name (never the key). */
  public CredentialNotFoundException(String providerName) {
    super("No credential for provider " + providerName);
  }
}
