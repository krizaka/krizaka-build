package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.Objects;

/** A request to replace (rotate) a provider key. Its {@code toString} never shows the key. */
public record RotateCredentialRequest(String apiKey) {

  public RotateCredentialRequest {
    Objects.requireNonNull(apiKey, "apiKey must not be null");
    if (apiKey.isBlank()) {
      throw new IllegalArgumentException("apiKey must not be blank");
    }
  }

  @Override
  public String toString() {
    return "RotateCredentialRequest[apiKey=***]";
  }
}
