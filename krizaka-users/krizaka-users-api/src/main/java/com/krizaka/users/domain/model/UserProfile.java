package com.krizaka.users.domain.model;

import java.util.Map;
import java.util.Objects;

/**
 * What a user told the application about themselves: an interface theme, and the attributes the
 * application asks for — typically the answers of its onboarding form.
 *
 * <p>The attributes are the application's vocabulary, not this service's: the users service stores
 * and returns them, and never interprets a key. An application reads the ones it defined and
 * supplies its own defaults.
 *
 * @param userId the user's opaque id
 * @param theme the interface theme the user chose
 * @param attributes application-defined profile attributes; never {@code null}
 */
public record UserProfile(String userId, String theme, Map<String, Object> attributes) {

  /** Rejects a profile without a user or theme and copies the attributes defensively. */
  public UserProfile {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(theme, "theme must not be null");
    attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
  }
}
