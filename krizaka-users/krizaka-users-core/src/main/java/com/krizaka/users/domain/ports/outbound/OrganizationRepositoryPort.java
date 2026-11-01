package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.ApiKey;
import com.krizaka.users.domain.model.Organization;
import com.krizaka.users.domain.model.OrganizationMember;
import java.util.List;
import java.util.Optional;

/** Outbound port of organization persistence, isolating the services from database entities. */
public interface OrganizationRepositoryPort {

  /** Creates an organization. */
  Organization create(Organization organization);

  /** Resolves an organization by id. */
  Optional<Organization> findById(String id);

  /** Resolves an organization by slug. */
  Optional<Organization> findBySlug(String slug);

  /** The organizations a user belongs to. */
  List<Organization> findByMember(String userId);

  /** Adds a member or changes an existing member's role. */
  OrganizationMember saveMember(OrganizationMember member);

  /** The membership of a user in an organization. */
  Optional<OrganizationMember> findMember(String organizationId, String userId);

  /** Every membership of an organization. */
  List<OrganizationMember> findMembers(String organizationId);

  /** How many owners an organization has. */
  long countOwners(String organizationId);

  /** Removes a membership; the number removed. */
  long deleteMember(String organizationId, String userId);

  /** The API keys of the organization's service accounts, most recent first. */
  List<ApiKey> findServiceKeys(String organizationId);
}
