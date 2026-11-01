package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.Objects;

/**
 * Internal request exchanging an inbound {@code oz_} API key for a short session JWT (performed by
 * the edge so downstream services only ever see JWTs).
 *
 * @param apiKey the presented {@code oz_}-prefixed key
 */
public record TokenExchangeRequest(String apiKey) {

  public TokenExchangeRequest {
    Objects.requireNonNull(apiKey, "apiKey is required");
    if (apiKey.isBlank()) {
      throw new IllegalArgumentException("apiKey cannot be blank");
    }
  }
}
