package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.ApiKeyDto;
import com.krizaka.users.persistence.domain.model.OrganizationDto;
import com.krizaka.users.persistence.domain.model.OrganizationMemberDto;
import java.util.List;
import java.util.Optional;

/** Persistence port of organizations and their memberships. */
public interface OrganizationPersistenceProvider {

  /** Stores a new organization. */
  OrganizationDto save(OrganizationDto organization);

  /** Resolves an organization by id. */
  Optional<OrganizationDto> findById(String id);

  /** Resolves an organization by its slug. */
  Optional<OrganizationDto> findBySlug(String slug);

  /** The organizations a user belongs to, by name. */
  List<OrganizationDto> findByMember(String userId);

  /** Adds a member, or changes the role of an existing one. */
  OrganizationMemberDto saveMember(OrganizationMemberDto member);

  /** The membership of a user in an organization, if any. */
  Optional<OrganizationMemberDto> findMember(String organizationId, String userId);

  /** Every membership of an organization, oldest first. */
  List<OrganizationMemberDto> findMembers(String organizationId);

  /** How many members hold a role in an organization. */
  long countByRole(String organizationId, String role);

  /** Removes a membership; the number of rows removed. */
  long deleteMember(String organizationId, String userId);

  /** The API keys owned by the service accounts of an organization, most recent first. */
  List<ApiKeyDto> findServiceKeys(String organizationId);
}
