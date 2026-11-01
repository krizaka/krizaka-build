package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Clean domain DTO representing UserCredential, satisfying ERR-106. */
public record UserCredentialDto(Long id, String userId, String providerName, String apiKey)
    implements Serializable {

  public UserCredentialDto {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(providerName, "providerName cannot be null");
    Objects.requireNonNull(apiKey, "apiKey cannot be null");
  }
}
