package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.Objects;

/** A request to store a new provider key. Its {@code toString} never shows the key. */
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

  @Override
  public String toString() {
    return "UserCredentialRequest[providerName=" + providerName + ", apiKey=***]";
  }
}
