package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * Thrown when an account would be created while registration is closed ({@code
 * krizaka.users.registration.enabled=false}): neither the register endpoint nor the first sign-in
 * of an unknown federated identity may create one.
 */
public class RegistrationClosedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs the exception with the message every closed entry point answers. */
  public RegistrationClosedException() {
    super("Registration is closed");
  }
}
