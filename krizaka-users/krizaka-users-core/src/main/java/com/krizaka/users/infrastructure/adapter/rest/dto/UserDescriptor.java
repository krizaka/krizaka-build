package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Router projection of a user identity for the REST ingress layer.
 *
 * <p>Domain-blind — decoupled from {@code com.krizaka.users.domain.User}. The domain-to-DTO mapping
 * is performed at the controller boundary, not inside this record. Returned by the profile and auth
 * endpoints.
 *
 * @param id The user's unique identifier (UUID string).
 * @param username The user's display name.
 * @param email The user's email address.
 * @param authorities The user's granted authority names.
 * @param preferences The user's preference map.
 */
public record UserDescriptor(
    String id,
    String username,
    String email,
    List<String> authorities,
    Map<String, Object> preferences) {
  /** Compact constructor enforcing fail-fast invariants. */
  public UserDescriptor {
    Objects.requireNonNull(id, "User ID is required");
    Objects.requireNonNull(username, "Username is required");
    authorities = authorities != null ? List.copyOf(authorities) : List.of();
    preferences = preferences != null ? Map.copyOf(preferences) : Map.of();
  }
}
