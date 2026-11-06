package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.UserDto;
import com.krizaka.users.persistence.domain.ports.UserPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.AuthorityEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserPersistenceProvider. */
@Service
@Transactional
class UserPersistenceProviderImpl implements UserPersistenceProvider {

  private final UserRepository userRepository;
  private static final String EMAIL_CANNOT_BE_NULL = "Email cannot be null";

  UserPersistenceProviderImpl(UserRepository userRepository) {
    this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findById(String id) {
    Objects.requireNonNull(id, "ID cannot be null");
    return userRepository.findByIdWithAssociations(id).map(UserPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findByEmailAndEnabledTrue(String email) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    return userRepository
        .findByEmailAndEnabledTrueWithAssociations(email)
        .map(UserPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findByProviderAndProviderId(String provider, String providerId) {
    Objects.requireNonNull(provider, "Provider cannot be null");
    Objects.requireNonNull(providerId, "ProviderId cannot be null");
    return userRepository
        .findByProviderAndProviderId(provider, providerId)
        .map(UserPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public long countByEmail(String email) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    return userRepository.countByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByUsername(String username) {
    Objects.requireNonNull(username, "Username cannot be null");
    return userRepository.existsByUsername(username);
  }

  @Override
  public UserDto save(UserDto userDto) {
    Objects.requireNonNull(userDto, "UserDto cannot be null");
    UserEntity existing =
        userRepository
            .findByIdWithAssociations(userDto.id())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDto.id()));
    existing.setUsername(userDto.username());
    existing.setEmail(userDto.email());
    existing.setEnabled(userDto.enabled());
    existing.setPreferences(userDto.preferences());
    existing.setRateLimitTier(userDto.rateLimitTier());
    UserEntity saved = userRepository.save(existing);
    return toDto(saved);
  }

  @Override
  public UserDto create(UserDto userDto, String passwordHash) {
    Objects.requireNonNull(userDto, "UserDto cannot be null");
    UserEntity entity = toEntity(userDto);
    entity.setPasswordHash(passwordHash);
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(Instant.now());
    }
    UserEntity saved = userRepository.save(entity);
    return toDto(saved);
  }

  @Override
  public void deleteById(String id) {
    Objects.requireNonNull(id, "ID cannot be null");
    userRepository.deleteById(id);
  }

  @Override
  public void updatePasswordHashByEmail(String email, String passwordHash) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    Objects.requireNonNull(passwordHash, "Password hash cannot be null");
    userRepository.updatePasswordHashByEmail(email, passwordHash);
  }

  @Override
  public void rehashPasswordByEmail(String email, String passwordHash) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    Objects.requireNonNull(passwordHash, "Password hash cannot be null");
    userRepository.rehashPasswordByEmail(email, passwordHash);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserDto> findByEmail(String email) {
    Objects.requireNonNull(email, EMAIL_CANNOT_BE_NULL);
    return userRepository.findByEmail(email).map(UserPersistenceProviderImpl::toDto);
  }

  private static UserDto toDto(UserEntity entity) {
    if (entity == null) {
      return null;
    }

    Set<String> authorities =
        entity.getAuthorities() != null
            ? entity.getAuthorities().stream()
                .map(AuthorityEntity::getAuthorityName)
                .collect(Collectors.toUnmodifiableSet())
            : Collections.emptySet();

    List<String> interceptions = Collections.emptyList();
    if (entity.getInterceptions() != null) {
      interceptions =
          entity.getInterceptions().stream().map(i -> i.getId().interceptionType()).toList();
    }

    return new UserDto(
        entity.getId(),
        entity.getUsername(),
        entity.getPasswordHash(),
        entity.getEmail(),
        entity.getEnabled(),
        entity.getPreferences() != null
            ? Map.copyOf(entity.getPreferences())
            : Collections.emptyMap(),
        authorities,
        interceptions,
        entity.getProvider(),
        entity.getProviderId(),
        entity.getRateLimitTier(),
        entity.getCreatedAt());
  }

  static UserEntity toEntity(UserDto dto) {
    if (dto == null) {
      return null;
    }

    UserEntity entity = new UserEntity();
    entity.setId(dto.id());
    entity.setUsername(dto.username());
    entity.setPasswordHash(dto.passwordHash());
    entity.setEmail(dto.email());
    entity.setEnabled(dto.enabled());
    entity.setPreferences(dto.preferences());
    entity.setProvider(dto.provider());
    entity.setProviderId(dto.providerId());
    entity.setRateLimitTier(dto.rateLimitTier());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
