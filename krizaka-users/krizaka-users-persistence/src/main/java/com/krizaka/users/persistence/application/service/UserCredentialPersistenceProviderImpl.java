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
  public Optional<UserCredentialDto> insert(UserCredentialDto credentialDto) {
    Objects.requireNonNull(credentialDto, "UserCredentialDto cannot be null");
    // One statement against V7's unique (user_id, provider_name): two concurrent creations land
    // once,
    // and the loser sees 0 rows instead of a constraint error that would doom the transaction.
    int inserted =
        repository.insertIfAbsent(
            credentialDto.userId(), credentialDto.providerName(), credentialDto.apiKey());
    if (inserted == 0) {
      return Optional.empty();
    }
    return repository
        .findByUserIdAndProviderName(credentialDto.userId(), credentialDto.providerName())
        .map(UserCredentialPersistenceProviderImpl::toDto);
  }

  @Override
  public boolean replaceStoredKey(String userId, String providerName, String storedKey) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    Objects.requireNonNull(storedKey, "StoredKey cannot be null");
    return repository.replaceStoredKey(userId, providerName, storedKey) == 1;
  }

  @Override
  public boolean deleteByUserIdAndProviderName(String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    return repository.deleteByUserIdAndProviderName(userId, providerName) > 0;
  }

  private static UserCredentialDto toDto(UserCredentialEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserCredentialDto(
        entity.getId(), entity.getUserId(), entity.getProviderName(), entity.getApiKey());
  }
}
