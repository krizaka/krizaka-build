package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Clean domain DTO representing UserInterception, satisfying ERR-106. */
public record UserInterceptionDto(
    String userId, String interceptionType, Boolean active, Instant createdAt)
    implements Serializable {

  public UserInterceptionDto {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(interceptionType, "interceptionType cannot be null");
    if (active == null) {
      active = Boolean.TRUE;
    }
  }
}
