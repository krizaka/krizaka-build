package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Clean domain DTO representing RateLimitTier, satisfying ERR-106. */
public record RateLimitTierDto(
    String id, int capacity, int refillTokens, int refillSeconds, Instant updatedAt)
    implements Serializable {

  public RateLimitTierDto {
    Objects.requireNonNull(id, "id cannot be null");
  }
}
