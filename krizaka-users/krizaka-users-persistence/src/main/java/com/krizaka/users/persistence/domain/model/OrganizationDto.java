package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Persistence DTO of an {@code organizations} row.
 *
 * @param id The organization identifier.
 * @param slug The unique, URL-safe handle.
 * @param name The display name.
 * @param createdAt Creation instant, or {@code null} to let the provider stamp it.
 */
public record OrganizationDto(String id, String slug, String name, Instant createdAt)
    implements Serializable {

  /** Compact constructor enforcing fail-fast invariants. */
  public OrganizationDto {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(slug, "slug cannot be null");
    Objects.requireNonNull(name, "name cannot be null");
  }
}
