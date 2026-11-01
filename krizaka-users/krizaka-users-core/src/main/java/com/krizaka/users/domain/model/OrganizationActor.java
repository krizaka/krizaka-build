package com.krizaka.users.domain.model;

import java.util.Objects;

/**
 * Who acts on an organization: a user, and whether the platform's administrator role lets them act
 * on any organization (the operator console), whatever their membership.
 *
 * @param userId The acting user.
 * @param platformAdmin Whether they hold the platform administrator role.
 */
public record OrganizationActor(String userId, boolean platformAdmin) {

  /** Compact constructor enforcing fail-fast invariants. */
  public OrganizationActor {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
