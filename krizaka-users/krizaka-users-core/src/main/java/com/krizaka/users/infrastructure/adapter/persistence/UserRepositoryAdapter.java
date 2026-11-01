package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserSecurityInfo;
import com.krizaka.users.domain.ports.outbound.UserRepositoryPort;
import com.krizaka.users.persistence.domain.model.UserDto;
import com.krizaka.users.persistence.domain.ports.UserPersistenceProvider;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Outbound persistence adapter implementing {@link UserRepositoryPort}. Delegates persistence
 * requests to the clean UserPersistenceProvider port.
 */
@Component
class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserPersistenceProvider provider;

  UserRepositoryAdapter(UserPersistenceProvider provider) {
    this.provider = Objects.requireNonNull(provider, "UserPersistenceProvider cannot be null");
  }

  @Override
  public Optional<User> findById(String userId) {
    return provider.findById(userId).map(UserMapper::toDomain);
  }

  @Override
  public Optional<User> findByEmailAndEnabledTrue(String email) {
    return provider.findByEmailAndEnabledTrue(email).map(UserMapper::toDomain);
  }

  @Override
  public Optional<User> findByProviderAndProviderId(String providerName, String providerId) {
    return provider.findByProviderAndProviderId(providerName, providerId).map(UserMapper::toDomain);
  }

  @Override
  public Optional<UserSecurityInfo> findSecurityInfoByEmail(String email) {
    return provider
        .findByEmailAndEnabledTrue(email)
        .map(dto -> new UserSecurityInfo(UserMapper.toDomain(dto), dto.passwordHash()));
  }

  @Override
  public User save(User user) {
    UserDto existingDto =
        provider
            .findById(user.id().toString())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + user.id()));

    UserDto updateDto =
        new UserDto(
            user.id().toString(),
            user.username(),
            existingDto.passwordHash(),
            user.email(),
            user.enabled(),
            user.preferences(),
            user.authorities(),
            user.activeInterceptions(),
            existingDto.provider(),
            existingDto.providerId(),
            user.rateLimitTier(),
            existingDto.createdAt());
    UserDto saved = provider.save(updateDto);
    return UserMapper.toDomain(saved);
  }

  @Override
  public User create(User user, String passwordHash) {
    UserDto dto = UserMapper.toDto(user, passwordHash);
    UserDto saved = provider.create(dto, passwordHash);
    return UserMapper.toDomain(saved);
  }

  @Override
  public User createFederated(User user, String providerName, String providerId) {
    UserDto saved =
        provider.create(UserMapper.toFederatedDto(user, providerName, providerId), null);
    return UserMapper.toDomain(saved);
  }

  @Override
  public boolean usernameTaken(String username) {
    return provider.existsByUsername(username);
  }

  @Override
  public void updatePasswordHashByEmail(String email, String passwordHash) {
    provider.updatePasswordHashByEmail(email, passwordHash);
  }

  @Override
  public void rehashPasswordByEmail(String email, String passwordHash) {
    provider.rehashPasswordByEmail(email, passwordHash);
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return provider.findByEmail(email).map(UserMapper::toDomain);
  }
}
