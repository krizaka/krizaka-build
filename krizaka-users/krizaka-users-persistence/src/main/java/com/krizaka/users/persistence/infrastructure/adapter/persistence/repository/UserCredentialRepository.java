package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserCredentialEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA Repository interface for {@link UserCredentialEntity}. */
@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, Long> {

  /**
   * Retrieves all credentials configured for a user.
   *
   * @param userId The user ID.
   * @return A list of credential entities.
   */
  List<UserCredentialEntity> findByUserId(String userId);

  /**
   * Resolves a user's credential by user ID and provider.
   *
   * @param userId The user ID.
   * @param providerName The provider name.
   * @return An Optional containing the credential entity.
   */
  Optional<UserCredentialEntity> findByUserIdAndProviderName(String userId, String providerName);

  /**
   * Deletes a credential configured for a user and provider.
   *
   * @param userId The user ID.
   * @param providerName The provider name.
   */
  void deleteByUserIdAndProviderName(String userId, String providerName);
}
