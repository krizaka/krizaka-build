package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.UserInterceptionDto;
import com.krizaka.users.persistence.domain.ports.UserInterceptionPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserInterceptionEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserInterceptionId;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserInterceptionRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserInterceptionPersistenceProvider. */
@Service
@Transactional
class UserInterceptionPersistenceProviderImpl implements UserInterceptionPersistenceProvider {

  private final UserInterceptionRepository repository;

  UserInterceptionPersistenceProviderImpl(UserInterceptionRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "UserInterceptionRepository cannot be null");
  }

  @Override
  public UserInterceptionDto save(UserInterceptionDto interceptionDto, String schemaId) {
    Objects.requireNonNull(interceptionDto, "UserInterceptionDto cannot be null");
    UserInterceptionEntity entity = toEntity(interceptionDto, schemaId);
    UserInterceptionEntity saved = repository.save(entity);
    return toDto(saved);
  }

  @Override
  public void deleteByUserIdAndInterceptionType(String userId, String interceptionType) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(interceptionType, "InterceptionType cannot be null");
    UserInterceptionId id = new UserInterceptionId(userId, interceptionType);
    repository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserInterceptionDto> findByUserIdAndInterceptionType(
      String userId, String interceptionType) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(interceptionType, "InterceptionType cannot be null");
    UserInterceptionId id = new UserInterceptionId(userId, interceptionType);
    return repository.findById(id).map(UserInterceptionPersistenceProviderImpl::toDto);
  }

  private static UserInterceptionDto toDto(UserInterceptionEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserInterceptionDto(
        entity.getId().userId(),
        entity.getId().interceptionType(),
        true, // If entity exists, it is active
        entity.getCreatedAt());
  }

  static UserInterceptionEntity toEntity(UserInterceptionDto dto, String schemaId) {
    if (dto == null) {
      return null;
    }
    UserInterceptionEntity entity = new UserInterceptionEntity();
    UserInterceptionId interceptionId =
        new UserInterceptionId(dto.userId(), dto.interceptionType());
    entity.setId(interceptionId);
    entity.setSchemaId(schemaId);
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
