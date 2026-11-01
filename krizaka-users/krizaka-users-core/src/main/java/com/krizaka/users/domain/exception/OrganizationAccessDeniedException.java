package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Thrown when an actor's role in an organization does not allow the action. */
public class OrganizationAccessDeniedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs the exception with the reason. */
  public OrganizationAccessDeniedException(String message) {
    super(message);
  }
}
