package com.krizaka.users.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * The result of presenting a valid API key: its owner and what the key itself may do.
 *
 * @param user the owning, enabled user
 * @param scopes the key's scopes; empty means the full rights of the owner
 */
public record AuthenticatedApiKey(User user, Set<String> scopes) {

  public AuthenticatedApiKey {
    Objects.requireNonNull(user, "user must not be null");
    scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
  }
}
