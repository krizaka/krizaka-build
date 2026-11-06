package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.exception.InvalidRequestException;

/**
 * Request payload for initiating a password reset.
 *
 * @param email The email address to send the reset token to.
 */
public record ForgotPasswordRequest(String email) {
  /** Compact constructor validating email. */
  public ForgotPasswordRequest {
    if (email == null || email.isBlank()) {
      throw new InvalidRequestException("Email is required");
    }
  }
}
