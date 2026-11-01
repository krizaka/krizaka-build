package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.UserProfileDto;
import com.krizaka.users.persistence.domain.ports.UserProfilePersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserProfileEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserProfileRepository;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of UserProfilePersistenceProvider. */
@Service
@Transactional
class UserProfilePersistenceProviderImpl implements UserProfilePersistenceProvider {

  private final UserProfileRepository userProfileRepository;

  UserProfilePersistenceProviderImpl(UserProfileRepository userProfileRepository) {
    this.userProfileRepository =
        Objects.requireNonNull(userProfileRepository, "UserProfileRepository cannot be null");
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserProfileDto> findByUserId(String userId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    return userProfileRepository.findById(userId).map(UserProfilePersistenceProviderImpl::toDto);
  }

  @Override
  public UserProfileDto save(UserProfileDto profileDto) {
    Objects.requireNonNull(profileDto, "UserProfileDto cannot be null");
    UserProfileEntity entity = toEntity(profileDto);
    UserProfileEntity saved = userProfileRepository.save(entity);
    return toDto(saved);
  }

  private static UserProfileDto toDto(UserProfileEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserProfileDto(
        entity.getUserId(),
        entity.getTheme(),
        entity.getRawPreferences() != null
            ? Map.copyOf(entity.getRawPreferences())
            : Collections.emptyMap());
  }

  static UserProfileEntity toEntity(UserProfileDto dto) {
    if (dto == null) {
      return null;
    }
    UserProfileEntity entity = new UserProfileEntity();
    entity.setUserId(dto.userId());
    entity.setTheme(dto.theme());
    entity.setRawPreferences(dto.rawPreferences());
    return entity;
  }
}
