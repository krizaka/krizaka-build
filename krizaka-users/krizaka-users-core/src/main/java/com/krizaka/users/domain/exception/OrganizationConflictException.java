package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Thrown when an organization change cannot be made as asked: a taken slug, the last owner. */
public class OrganizationConflictException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs the exception with the reason. */
  public OrganizationConflictException(String message) {
    super(message);
  }
}
