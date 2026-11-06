package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.exception.InvalidRequestException;

/**
 * Request data containing registration details.
 *
 * @param username The user's username.
 * @param email The user's email address.
 * @param password The user's password.
 * @param language The user's preferred language.
 */
public record RegisterRequest(String username, String email, String password, String language) {
  /** Compact constructor validating required fields. */
  public RegisterRequest {
    if (username == null
        || username.isBlank()
        || email == null
        || email.isBlank()
        || password == null
        || password.isBlank()) {
      throw new InvalidRequestException("username, email, and password are required");
    }
  }
}
