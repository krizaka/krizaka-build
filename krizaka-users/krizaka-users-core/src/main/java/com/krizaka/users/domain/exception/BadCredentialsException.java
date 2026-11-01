package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * Thrown when credential-based authentication fails due to invalid email or password.
 *
 * <p>Replaces the legacy null-return pattern in {@code IdentityService.authenticate()} with an
 * explicit, fail-fast exception as mandated by ERR-106.
 */
public class BadCredentialsException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public BadCredentialsException(String message) {
    super(message);
  }
}
