package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Exception thrown when input parameters for a request or command are invalid or missing. */
public class InvalidRequestException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message The detail message.
   */
  public InvalidRequestException(String message) {
    super(message);
  }
}
