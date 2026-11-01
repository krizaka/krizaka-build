package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * An organization: a company or an SME that owns accounts.
 *
 * @param id The identifier.
 * @param slug The unique, URL-safe handle.
 * @param name The display name.
 * @param createdAt Creation instant.
 */
public record Organization(String id, String slug, String name, Instant createdAt) {

  /** Compact constructor enforcing fail-fast invariants. */
  public Organization {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(slug, "slug must not be null");
    Objects.requireNonNull(name, "name must not be null");
  }
}
