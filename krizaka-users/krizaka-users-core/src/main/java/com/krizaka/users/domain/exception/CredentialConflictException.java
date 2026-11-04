package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Thrown when the user already has a key for the provider: rotate it, or delete it first. */
public class CredentialConflictException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs the exception for a provider name (never the key). */
  public CredentialConflictException(String providerName) {
    super("A credential already exists for provider " + providerName);
  }
}
