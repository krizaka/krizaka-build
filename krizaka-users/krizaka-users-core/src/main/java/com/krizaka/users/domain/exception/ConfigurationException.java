package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * Thrown when a required configuration resource (schema, template) cannot be resolved at runtime.
 */
public class ConfigurationException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs exception with a descriptive message. */
  public ConfigurationException(String message) {
    super(message);
  }

  /**
   * Creates the exception with its cause.
   *
   * @param message what went wrong
   * @param cause the underlying failure
   */
  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
