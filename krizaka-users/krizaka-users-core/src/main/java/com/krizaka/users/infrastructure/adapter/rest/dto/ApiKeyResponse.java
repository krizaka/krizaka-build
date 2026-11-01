package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.model.ApiKey;
import java.time.Instant;
import java.util.Objects;

/**
 * DTO record representing an API key's listing metadata. Never carries the plaintext secret.
 *
 * @param id The key identifier.
 * @param name The user-supplied label.
 * @param keyPrefix The short display prefix (e.g. {@code oz_live_9f3a}).
 * @param createdAt Creation instant.
 * @param lastUsedAt Last authentication instant, or {@code null} if never used.
 */
public record ApiKeyResponse(
    String id,
    String name,
    String keyPrefix,
    Instant createdAt,
    Instant lastUsedAt,
    java.util.List<String> scopes) {

  public ApiKeyResponse {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
    scopes = scopes == null ? java.util.List.of() : java.util.List.copyOf(scopes);
  }

  /** An unscoped key. */
  public ApiKeyResponse(
      String id, String name, String keyPrefix, Instant createdAt, Instant lastUsedAt) {
    this(id, name, keyPrefix, createdAt, lastUsedAt, java.util.List.of());
  }

  /**
   * Projects a domain {@link ApiKey} into its REST representation.
   *
   * @param apiKey The domain record.
   * @return The response DTO.
   */
  public static ApiKeyResponse from(ApiKey apiKey) {
    return new ApiKeyResponse(
        apiKey.id(),
        apiKey.name(),
        apiKey.keyPrefix(),
        apiKey.createdAt(),
        apiKey.lastUsedAt(),
        apiKey.scopes().stream().sorted().toList());
  }
}
