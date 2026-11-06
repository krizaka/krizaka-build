package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Clean persistence DTO representing an inbound API key row, satisfying ERR-106.
 *
 * @param id The key identifier (UUID string).
 * @param userId The owning user ID.
 * @param name The user-supplied label.
 * @param keyPrefix The short display prefix (e.g. {@code oz_live_9f3a}).
 * @param keyHash The SHA-256 hash of the plaintext secret.
 * @param createdAt Creation instant.
 * @param lastUsedAt Last authentication instant, or {@code null} if never used.
 */
public record ApiKeyDto(
    String id,
    String userId,
    String name,
    String keyPrefix,
    String keyHash,
    Instant createdAt,
    Instant lastUsedAt,
    Set<String> scopes)
    implements Serializable {

  public ApiKeyDto {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
    Objects.requireNonNull(keyPrefix, "keyPrefix cannot be null");
    Objects.requireNonNull(keyHash, "keyHash cannot be null");
    scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
  }

  /** An unscoped key (full rights of its owner) — the shape every key had before scoping. */
  public ApiKeyDto(
      String id,
      String userId,
      String name,
      String keyPrefix,
      String keyHash,
      Instant createdAt,
      Instant lastUsedAt) {
    this(id, userId, name, keyPrefix, keyHash, createdAt, lastUsedAt, Set.of());
  }
}
