package com.krizaka.users.domain.model;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable domain record returned exactly once, when an API key is created.
 *
 * <p>The {@link #plaintext} secret is never persisted (only its SHA-256 hash is) and is never
 * retrievable again — the caller must surface it to the user immediately.
 *
 * @param id The key identifier (UUID string).
 * @param name The user-supplied label.
 * @param keyPrefix The short display prefix (e.g. {@code oz_live_9f3a}).
 * @param plaintext The full plaintext secret, shown once.
 * @param scopes What the key may do (empty = the full rights of its owner).
 */
public record GeneratedApiKey(
    String id, String name, String keyPrefix, String plaintext, Set<String> scopes) {

  /** Compact constructor enforcing fail-fast invariants. */
  public GeneratedApiKey {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(keyPrefix, "keyPrefix must not be null");
    Objects.requireNonNull(plaintext, "plaintext must not be null");
    scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
  }

  /** An unscoped key. */
  public GeneratedApiKey(String id, String name, String keyPrefix, String plaintext) {
    this(id, name, keyPrefix, plaintext, Set.of());
  }
}
