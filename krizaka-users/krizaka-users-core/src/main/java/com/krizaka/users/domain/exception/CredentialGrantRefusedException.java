package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * Thrown when a credential grant cannot be redeemed: unknown, expired, already used, or issued for
 * another provider or run. The message never says which, nor carries the grant.
 */
public class CredentialGrantRefusedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public CredentialGrantRefusedException() {
    super("Credential grant refused");
  }
}
