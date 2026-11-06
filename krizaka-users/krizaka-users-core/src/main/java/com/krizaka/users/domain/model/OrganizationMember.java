package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * An account's place in an organization.
 *
 * @param organizationId The organization.
 * @param userId The member or service account.
 * @param role Its role.
 * @param createdAt When it joined.
 */
public record OrganizationMember(
    String organizationId, String userId, OrganizationRole role, Instant createdAt) {

  /** Compact constructor enforcing fail-fast invariants. */
  public OrganizationMember {
    Objects.requireNonNull(organizationId, "organizationId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(role, "role must not be null");
  }
}
