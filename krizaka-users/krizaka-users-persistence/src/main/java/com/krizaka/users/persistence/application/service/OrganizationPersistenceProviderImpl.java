package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.ApiKeyDto;
import com.krizaka.users.persistence.domain.model.OrganizationDto;
import com.krizaka.users.persistence.domain.model.OrganizationMemberDto;
import com.krizaka.users.persistence.domain.ports.ApiKeyPersistenceProvider;
import com.krizaka.users.persistence.domain.ports.OrganizationPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OrganizationEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OrganizationMemberEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OrganizationMemberId;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.OrganizationMemberRepository;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.OrganizationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Persistence of organizations, their memberships and their service accounts' keys. */
@Component
@Transactional
class OrganizationPersistenceProviderImpl implements OrganizationPersistenceProvider {

  private static final String ORG_ID_NULL = "organizationId cannot be null";
  private static final String USER_ID_NULL = "userId cannot be null";

  private final OrganizationRepository organizations;
  private final OrganizationMemberRepository members;
  private final ApiKeyPersistenceProvider apiKeys;

  OrganizationPersistenceProviderImpl(
      OrganizationRepository organizations,
      OrganizationMemberRepository members,
      ApiKeyPersistenceProvider apiKeys) {
    this.organizations =
        Objects.requireNonNull(organizations, "OrganizationRepository cannot be null");
    this.members = Objects.requireNonNull(members, "OrganizationMemberRepository cannot be null");
    this.apiKeys = Objects.requireNonNull(apiKeys, "ApiKeyPersistenceProvider cannot be null");
  }

  @Override
  public OrganizationDto save(OrganizationDto organization) {
    Objects.requireNonNull(organization, "organization cannot be null");
    OrganizationEntity entity = new OrganizationEntity();
    entity.setId(organization.id());
    entity.setSlug(organization.slug());
    entity.setName(organization.name());
    entity.setCreatedAt(
        organization.createdAt() != null ? organization.createdAt() : Instant.now());
    return toDto(organizations.saveAndFlush(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<OrganizationDto> findById(String id) {
    Objects.requireNonNull(id, "id cannot be null");
    return organizations.findById(id).map(OrganizationPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<OrganizationDto> findBySlug(String slug) {
    Objects.requireNonNull(slug, "slug cannot be null");
    return organizations.findBySlug(slug).map(OrganizationPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganizationDto> findByMember(String userId) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    return organizations.findByMember(userId).stream()
        .map(OrganizationPersistenceProviderImpl::toDto)
        .toList();
  }

  @Override
  public OrganizationMemberDto saveMember(OrganizationMemberDto member) {
    Objects.requireNonNull(member, "member cannot be null");
    OrganizationMemberId id = new OrganizationMemberId(member.organizationId(), member.userId());
    OrganizationMemberEntity entity = members.findById(id).orElseGet(OrganizationMemberEntity::new);
    if (entity.getId() == null) {
      entity.setId(id);
      entity.setCreatedAt(member.createdAt() != null ? member.createdAt() : Instant.now());
    }
    entity.setRole(member.role());
    return toDto(members.saveAndFlush(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<OrganizationMemberDto> findMember(String organizationId, String userId) {
    Objects.requireNonNull(organizationId, ORG_ID_NULL);
    Objects.requireNonNull(userId, USER_ID_NULL);
    return members
        .findById(new OrganizationMemberId(organizationId, userId))
        .map(OrganizationPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<OrganizationMemberDto> findMembers(String organizationId) {
    Objects.requireNonNull(organizationId, ORG_ID_NULL);
    return members.findByOrganization(organizationId).stream()
        .map(OrganizationPersistenceProviderImpl::toDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long countByRole(String organizationId, String role) {
    Objects.requireNonNull(organizationId, ORG_ID_NULL);
    Objects.requireNonNull(role, "role cannot be null");
    return members.countByRole(organizationId, role);
  }

  @Override
  public long deleteMember(String organizationId, String userId) {
    Objects.requireNonNull(organizationId, ORG_ID_NULL);
    Objects.requireNonNull(userId, USER_ID_NULL);
    return members.deleteMembership(organizationId, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ApiKeyDto> findServiceKeys(String organizationId) {
    Objects.requireNonNull(organizationId, ORG_ID_NULL);
    return apiKeys.findByUserIds(members.findServiceUserIds(organizationId));
  }

  private static OrganizationDto toDto(OrganizationEntity entity) {
    return new OrganizationDto(
        entity.getId(), entity.getSlug(), entity.getName(), entity.getCreatedAt());
  }

  private static OrganizationMemberDto toDto(OrganizationMemberEntity entity) {
    return new OrganizationMemberDto(
        entity.getId().organizationId(),
        entity.getId().userId(),
        entity.getRole(),
        entity.getCreatedAt());
  }
}
