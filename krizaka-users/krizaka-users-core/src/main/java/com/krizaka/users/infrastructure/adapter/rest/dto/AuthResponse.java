package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.List;
import java.util.Objects;

/**
 * Strongly-typed authentication response DTO for the REST ingress layer.
 *
 * <p>Replaces raw {@code Map.of()} responses with a fail-fast, self-validating record (ERR-106).
 * Domain-blind — contains only primitive router-scoped fields.
 *
 * @param token The user's session token — a signed HS256 JWT issued by {@code TokenService},
 *     carrying the user id as {@code sub}. Every host validates it locally against the shared
 *     secret, so an opaque identifier is no longer an accepted bearer credential.
 * @param username The authenticated user's display name.
 * @param email The authenticated user's email address.
 * @param authorities The user's granted authority names.
 * @param activeInterceptions List of currently active interception types for the user.
 */
public record AuthResponse(
    String token,
    String username,
    String email,
    List<String> authorities,
    List<String> activeInterceptions) {
  /** Compact constructor enforcing fail-fast invariants. */
  public AuthResponse {
    Objects.requireNonNull(token, "Auth token is required");
    Objects.requireNonNull(username, "Username is required");
    Objects.requireNonNull(email, "Email is required");
    authorities = authorities != null ? List.copyOf(authorities) : List.of();
    activeInterceptions =
        activeInterceptions != null ? List.copyOf(activeInterceptions) : List.of();
  }
}
