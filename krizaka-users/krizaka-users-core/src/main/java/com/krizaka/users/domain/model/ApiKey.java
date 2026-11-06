package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable domain record describing an inbound API key's listing metadata.
 *
 * <p>Carries only what is safe to expose after creation — never the plaintext secret nor its hash.
 * The plaintext is returned exactly once, via {@link GeneratedApiKey}, at creation time.
 *
 * @param id The key identifier (UUID string).
 * @param userId The owning user ID.
 * @param name The user-supplied label.
 * @param keyPrefix The short display prefix (e.g. {@code oz_live_9f3a}).
 * @param createdAt Creation instant.
 * @param lastUsedAt Last authentication instant, or {@code null} if never used.
 * @param scopes What the key may do, e.g. {@code ai:embed}; empty means the full rights of its
 *     owner (every key created before scoping).
 */
public record ApiKey(
    String id,
    String userId,
    String name,
    String keyPrefix,
    Instant createdAt,
    Instant lastUsedAt,
    Set<String> scopes) {

  /** Compact constructor enforcing fail-fast invariants. */
  public ApiKey {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
    scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
  }

  /** An unscoped key — the full rights of its owner. */
  public ApiKey(
      String id,
      String userId,
      String name,
      String keyPrefix,
      Instant createdAt,
      Instant lastUsedAt) {
    this(id, userId, name, keyPrefix, createdAt, lastUsedAt, Set.of());
  }
}
