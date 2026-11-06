package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.exception.InvalidRequestException;

/**
 * Request payload for executing a password reset.
 *
 * @param token The plaintext reset token received by the user.
 * @param newPassword The new password to set.
 */
public record ResetPasswordRequest(String token, String newPassword) {
  /** Compact constructor validating required fields. */
  public ResetPasswordRequest {
    if (token == null || token.isBlank()) {
      throw new InvalidRequestException("Reset token is required");
    }
    if (newPassword == null || newPassword.isBlank()) {
      throw new InvalidRequestException("New password is required");
    }
  }
}
