package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/** Clean domain DTO representing a UserProfile, satisfying ERR-106. */
public record UserProfileDto(String userId, String theme, Map<String, Object> rawPreferences)
    implements Serializable {

  public UserProfileDto {
    Objects.requireNonNull(userId, "userId cannot be null");
    if (theme == null) {
      theme = "emerald";
    }
  }
}
