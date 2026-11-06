package com.krizaka.users.domain.exception;

import java.io.Serial;

/**
 * An administrative action the rules refuse (your own account, the last administrator, a mismatched
 * confirmation).
 */
public class AdminActionRefusedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final String code;

  public AdminActionRefusedException(String code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * A stable code the console translates: {@code SELF}, {@code LAST_ADMIN}, {@code
   * CONFIRM_MISMATCH}.
   */
  public String code() {
    return code;
  }
}
