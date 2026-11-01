package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.model.OrganizationMember;
import java.time.Instant;

/**
 * A membership as the API shows it.
 *
 * @param userId The member or service account.
 * @param role Its role.
 * @param createdAt When it joined.
 */
public record OrganizationMemberResponse(String userId, String role, Instant createdAt) {

  /** Maps a domain membership. */
  public static OrganizationMemberResponse from(OrganizationMember member) {
    return new OrganizationMemberResponse(
        member.userId(), member.role().name(), member.createdAt());
  }
}
