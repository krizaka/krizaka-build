package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Clean domain DTO representing a User, satisfying ERR-106. */
public record UserDto(
    String id,
    String username,
    String passwordHash,
    String email,
    Boolean enabled,
    Map<String, Object> preferences,
    Set<String> authorities,
    List<String> interceptions,
    String provider,
    String providerId,
    String rateLimitTier,
    Instant createdAt)
    implements Serializable {

  public UserDto {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(username, "username cannot be null");
    Objects.requireNonNull(email, "email cannot be null");
    if (enabled == null) {
      enabled = Boolean.TRUE;
    }
    if (provider == null) {
      provider = "local";
    }
  }
}
