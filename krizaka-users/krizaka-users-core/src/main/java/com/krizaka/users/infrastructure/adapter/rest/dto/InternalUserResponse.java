package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.model.User;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Full user snapshot served on the internal service-to-service API — everything a downstream host
 * needs to hydrate its {@code User} principal (identity-api contract) without the identity library
 * in-process.
 *
 * @param id the user id (the opaque ActorId other contexts hold)
 * @param username display name
 * @param email email address
 * @param enabled account state
 * @param authorities granted role names
 * @param preferences merged preference map
 * @param activeInterceptions active interception types
 * @param rateLimitTier the user's tier key ({@code null} when untiered)
 */
public record InternalUserResponse(
    String id,
    String username,
    String email,
    boolean enabled,
    List<String> authorities,
    Map<String, Object> preferences,
    List<String> activeInterceptions,
    String rateLimitTier) {

  public InternalUserResponse {
    Objects.requireNonNull(id, "id is required");
    Objects.requireNonNull(username, "username is required");
    Objects.requireNonNull(email, "email is required");
    authorities = authorities == null ? List.of() : List.copyOf(authorities);
    preferences = preferences == null ? Map.of() : Map.copyOf(preferences);
    activeInterceptions =
        activeInterceptions == null ? List.of() : List.copyOf(activeInterceptions);
  }

  /** Maps the identity domain user to its internal wire snapshot. */
  public static InternalUserResponse from(User user) {
    return new InternalUserResponse(
        user.id().toString(),
        user.username(),
        user.email(),
        user.enabled(),
        List.copyOf(user.authorities()),
        user.preferences(),
        user.activeInterceptions(),
        user.rateLimitTier());
  }
}
