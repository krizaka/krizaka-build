package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.Objects;

/** DTO record representing a request to save or rotate a user credential. */
public record UserCredentialRequest(String providerName, String apiKey) {

  public UserCredentialRequest {
    Objects.requireNonNull(providerName, "providerName must not be null");
    if (providerName.isBlank()) {
      throw new IllegalArgumentException("providerName must not be blank");
    }
    Objects.requireNonNull(apiKey, "apiKey must not be null");
    if (apiKey.isBlank()) {
      throw new IllegalArgumentException("apiKey must not be blank");
    }
  }
}
