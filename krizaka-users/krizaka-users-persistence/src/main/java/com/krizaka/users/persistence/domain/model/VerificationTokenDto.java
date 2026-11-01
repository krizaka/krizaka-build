package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Clean domain DTO representing VerificationToken, satisfying ERR-106. */
public record VerificationTokenDto(
    String id,
    String userId,
    String tokenType,
    String tokenHash,
    Instant expiryTimestamp,
    Boolean used,
    Instant createdAt)
    implements Serializable {

  public VerificationTokenDto {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(tokenType, "tokenType cannot be null");
    Objects.requireNonNull(tokenHash, "tokenHash cannot be null");
    Objects.requireNonNull(expiryTimestamp, "expiryTimestamp cannot be null");
    if (used == null) {
      used = Boolean.FALSE;
    }
  }
}
