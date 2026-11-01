package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Persistence DTO of an {@code organization_members} row.
 *
 * @param organizationId The organization.
 * @param userId The member or service account.
 * @param role {@code OWNER}, {@code ADMIN}, {@code MEMBER} or {@code SERVICE}.
 * @param createdAt Creation instant, or {@code null} to let the provider stamp it.
 */
public record OrganizationMemberDto(
    String organizationId, String userId, String role, Instant createdAt) implements Serializable {

  /** Compact constructor enforcing fail-fast invariants. */
  public OrganizationMemberDto {
    Objects.requireNonNull(organizationId, "organizationId cannot be null");
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(role, "role cannot be null");
  }
}
