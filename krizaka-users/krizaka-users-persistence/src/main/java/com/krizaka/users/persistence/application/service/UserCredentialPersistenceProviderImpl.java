package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import com.krizaka.users.persistence.domain.ports.UserCredentialPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserCredentialEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserCredentialRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserCredentialPersistenceProvider. */
@Service
@Transactional
class UserCredentialPersistenceProviderImpl implements UserCredentialPersistenceProvider {

  private static final String USER_ID_NULL = "UserId cannot be null";
  private static final String PROVIDER_NAME_NULL = "ProviderName cannot be null";

  private final UserCredentialRepository repository;

  UserCredentialPersistenceProviderImpl(UserCredentialRepository repository) {
    this.repository = Objects.requireNonNull(repository, "UserCredentialRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserCredentialDto> findByUserId(String userId) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    return repository.findByUserId(userId).stream()
        .map(UserCredentialPersistenceProviderImpl::toDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserCredentialDto> findByUserIdAndProviderName(
      String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    return repository
        .findByUserIdAndProviderName(userId, providerName)
        .map(UserCredentialPersistenceProviderImpl::toDto);
  }

  @Override
  public UserCredentialDto save(UserCredentialDto credentialDto) {
    Objects.requireNonNull(credentialDto, "UserCredentialDto cannot be null");
    Optional<UserCredentialEntity> existingOpt =
        repository.findByUserIdAndProviderName(
            credentialDto.userId(), credentialDto.providerName());

    UserCredentialEntity entity;
    if (existingOpt.isPresent()) {
      entity = existingOpt.get();
    } else {
      entity = new UserCredentialEntity();
      entity.setUserId(credentialDto.userId());
      entity.setProviderName(credentialDto.providerName());
    }
    entity.setApiKey(credentialDto.apiKey());
    UserCredentialEntity saved = repository.save(entity);
    return toDto(saved);
  }

  @Override
  public void deleteByUserIdAndProviderName(String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    repository.deleteByUserIdAndProviderName(userId, providerName);
  }

  private static UserCredentialDto toDto(UserCredentialEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserCredentialDto(
        entity.getId(), entity.getUserId(), entity.getProviderName(), entity.getApiKey());
  }
}
