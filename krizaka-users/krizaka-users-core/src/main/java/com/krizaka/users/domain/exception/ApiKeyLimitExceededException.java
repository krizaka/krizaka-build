package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Exception thrown when a user attempts to create an API key beyond the per-user maximum. */
public class ApiKeyLimitExceededException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message The detail message.
   */
  public ApiKeyLimitExceededException(String message) {
    super(message);
  }
}
