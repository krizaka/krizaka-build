package com.krizaka.users.application.service;

import com.krizaka.users.domain.model.UserProfile;
import com.krizaka.users.domain.ports.inbound.UserProfileProvider;
import com.krizaka.users.domain.ports.outbound.UserProfileRepositoryPort;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Package-private implementation of the {@link UserProfileProvider} interface boundary. */
@Service
class UserProfileProviderImpl implements UserProfileProvider {

  private final UserProfileRepositoryPort repository;

  UserProfileProviderImpl(UserProfileRepositoryPort repository) {
    this.repository =
        Objects.requireNonNull(repository, "UserProfileRepositoryPort must not be null");
  }

  @Override
  public UserProfile getProfile(String userId) {
    Objects.requireNonNull(userId, "userId must not be null");
    return repository.findByUserId(userId).orElse(null);
  }
}
