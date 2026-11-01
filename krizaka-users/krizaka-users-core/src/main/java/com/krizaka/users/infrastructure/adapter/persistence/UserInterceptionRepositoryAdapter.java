package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.ports.outbound.UserInterceptionRepositoryPort;
import com.krizaka.users.persistence.domain.model.UserInterceptionDto;
import com.krizaka.users.persistence.domain.ports.UserInterceptionPersistenceProvider;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Outbound persistence adapter implementing {@link UserInterceptionRepositoryPort}. Delegates
 * persistence requests to the clean UserInterceptionPersistenceProvider port.
 */
@Component
class UserInterceptionRepositoryAdapter implements UserInterceptionRepositoryPort {

  private final UserInterceptionPersistenceProvider provider;

  UserInterceptionRepositoryAdapter(UserInterceptionPersistenceProvider provider) {
    this.provider =
        Objects.requireNonNull(provider, "UserInterceptionPersistenceProvider cannot be null");
  }

  @Override
  public void triggerInterception(UUID userId, String interceptionType, String schemaId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(interceptionType, "InterceptionType cannot be null");
    UserInterceptionDto dto =
        new UserInterceptionDto(userId.toString(), interceptionType, true, Instant.now());
    try {
      provider.save(dto, schemaId);
    } catch (DataIntegrityViolationException e) {
      // Interception already exists (unique constraint violation), ignore gracefully
    }
  }

  @Override
  public void deleteInterception(UUID userId, String interceptionType) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(interceptionType, "InterceptionType cannot be null");
    provider.deleteByUserIdAndInterceptionType(userId.toString(), interceptionType);
  }
}
