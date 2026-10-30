package com.krizaka.payments;

import java.io.Serial;

/** A payment call or webhook the port refuses; {@link #status()} is the HTTP status to answer. */
public class PaymentsException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final int status;

  /**
   * Creates the exception.
   *
   * @param status the HTTP status
   * @param message what went wrong
   */
  public PaymentsException(int status, String message) {
    super(message);
    this.status = status;
  }

  /**
   * The HTTP status.
   *
   * @return the status
   */
  public int status() {
    return status;
  }
}
