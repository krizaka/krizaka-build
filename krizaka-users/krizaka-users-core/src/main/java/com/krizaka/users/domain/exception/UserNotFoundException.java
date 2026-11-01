package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Thrown when a user lookup fails because no matching record exists. */
public class UserNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs exception with a descriptive message. */
  public UserNotFoundException(String userId) {
    super("User not found: " + userId);
  }
}
