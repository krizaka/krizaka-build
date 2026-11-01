package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.model.Organization;
import java.time.Instant;

/**
 * An organization as the API shows it.
 *
 * @param id The identifier.
 * @param slug The handle.
 * @param name The display name.
 * @param createdAt Creation instant.
 */
public record OrganizationResponse(String id, String slug, String name, Instant createdAt) {

  /** Maps a domain organization. */
  public static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.id(), organization.slug(), organization.name(), organization.createdAt());
  }
}
