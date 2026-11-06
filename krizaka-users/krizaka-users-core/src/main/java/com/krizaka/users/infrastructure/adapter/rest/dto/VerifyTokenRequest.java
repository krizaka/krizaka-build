package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.exception.InvalidRequestException;

/**
 * Request payload for verifying account tokens.
 *
 * @param token The token string.
 */
public record VerifyTokenRequest(String token) {
  /** Compact constructor validating token. */
  public VerifyTokenRequest {
    if (token == null || token.isBlank()) {
      throw new InvalidRequestException("Token is required");
    }
  }
}
