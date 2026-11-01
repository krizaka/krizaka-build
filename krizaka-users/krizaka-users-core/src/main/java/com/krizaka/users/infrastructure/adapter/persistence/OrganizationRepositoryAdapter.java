package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.ApiKey;
import com.krizaka.users.domain.model.Organization;
import com.krizaka.users.domain.model.OrganizationMember;
import com.krizaka.users.domain.model.OrganizationRole;
import com.krizaka.users.domain.ports.outbound.OrganizationRepositoryPort;
import com.krizaka.users.persistence.domain.model.ApiKeyDto;
import com.krizaka.users.persistence.domain.model.OrganizationDto;
import com.krizaka.users.persistence.domain.model.OrganizationMemberDto;
import com.krizaka.users.persistence.domain.ports.OrganizationPersistenceProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Concrete persistence adapter implementing {@link OrganizationRepositoryPort}: it delegates to the
 * clean {@link OrganizationPersistenceProvider} and maps its DTOs into domain records.
 */
@Component
class OrganizationRepositoryAdapter implements OrganizationRepositoryPort {

  private final OrganizationPersistenceProvider provider;

  OrganizationRepositoryAdapter(OrganizationPersistenceProvider provider) {
    this.provider =
        Objects.requireNonNull(provider, "OrganizationPersistenceProvider cannot be null");
  }

  @Override
  public Organization create(Organization organization) {
    Objects.requireNonNull(organization, "organization cannot be null");
    return toDomain(
        provider.save(
            new OrganizationDto(
                organization.id(),
                organization.slug(),
                organization.name(),
                organization.createdAt())));
  }

  @Override
  public Optional<Organization> findById(String id) {
    return provider
        .findById(Objects.requireNonNull(id, "id cannot be null"))
        .map(OrganizationRepositoryAdapter::toDomain);
  }

  @Override
  public Optional<Organization> findBySlug(String slug) {
    return provider
        .findBySlug(Objects.requireNonNull(slug, "slug cannot be null"))
        .map(OrganizationRepositoryAdapter::toDomain);
  }

  @Override
  public List<Organization> findByMember(String userId) {
    return provider.findByMember(Objects.requireNonNull(userId, "userId cannot be null")).stream()
        .map(OrganizationRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public OrganizationMember saveMember(OrganizationMember member) {
    Objects.requireNonNull(member, "member cannot be null");
    return toDomain(
        provider.saveMember(
            new OrganizationMemberDto(
                member.organizationId(),
                member.userId(),
                member.role().name(),
                member.createdAt())));
  }

  @Override
  public Optional<OrganizationMember> findMember(String organizationId, String userId) {
    return provider.findMember(organizationId, userId).map(OrganizationRepositoryAdapter::toDomain);
  }

  @Override
  public List<OrganizationMember> findMembers(String organizationId) {
    return provider.findMembers(organizationId).stream()
        .map(OrganizationRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public long countOwners(String organizationId) {
    return provider.countByRole(organizationId, OrganizationRole.OWNER.name());
  }

  @Override
  public long deleteMember(String organizationId, String userId) {
    return provider.deleteMember(organizationId, userId);
  }

  @Override
  public List<ApiKey> findServiceKeys(String organizationId) {
    return provider.findServiceKeys(organizationId).stream()
        .map(OrganizationRepositoryAdapter::toDomain)
        .toList();
  }

  private static Organization toDomain(OrganizationDto dto) {
    return new Organization(dto.id(), dto.slug(), dto.name(), dto.createdAt());
  }

  private static OrganizationMember toDomain(OrganizationMemberDto dto) {
    return new OrganizationMember(
        dto.organizationId(), dto.userId(), OrganizationRole.valueOf(dto.role()), dto.createdAt());
  }

  private static ApiKey toDomain(ApiKeyDto dto) {
    return new ApiKey(
        dto.id(),
        dto.userId(),
        dto.name(),
        dto.keyPrefix(),
        dto.createdAt(),
        dto.lastUsedAt(),
        dto.scopes());
  }
}
