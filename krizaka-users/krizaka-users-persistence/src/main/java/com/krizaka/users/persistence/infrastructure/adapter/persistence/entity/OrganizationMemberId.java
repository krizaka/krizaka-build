package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;

/**
 * Composite key of {@link OrganizationMemberEntity}.
 *
 * @param organizationId The organization.
 * @param userId The member.
 */
@Embeddable
public record OrganizationMemberId(
    @Column(name = "organization_id", length = 255) String organizationId,
    @Column(name = "user_id", length = 255) String userId)
    implements Serializable {

  @Serial private static final long serialVersionUID = 1L;
}
