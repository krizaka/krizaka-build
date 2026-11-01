package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.Objects;

/**
 * Internal response carrying a user's decrypted BYOK provider key (service-to-service only — never
 * exposed through the edge).
 *
 * @param apiKey the decrypted provider API key
 */
public record InternalCredentialResponse(String apiKey) {

  public InternalCredentialResponse {
    Objects.requireNonNull(apiKey, "apiKey is required");
  }
}
