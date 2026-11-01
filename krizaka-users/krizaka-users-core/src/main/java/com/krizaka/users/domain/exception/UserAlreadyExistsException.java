package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * Exception thrown when a user registration fails because the requested email is already
 * registered.
 */
public class UserAlreadyExistsException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message The detail message.
   */
  public UserAlreadyExistsException(String message) {
    super(message);
  }
}
