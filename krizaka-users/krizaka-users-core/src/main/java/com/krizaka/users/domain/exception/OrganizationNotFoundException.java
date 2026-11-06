package com.krizaka.users.domain.exception;

import java.io.Serial;

/** Thrown when an organization does not exist, or is not visible to the one who asks. */
public class OrganizationNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** Constructs the exception for an organization id or slug. */
  public OrganizationNotFoundException(String organization) {
    super("Organization not found: " + organization);
  }
}
