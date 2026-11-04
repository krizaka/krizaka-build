package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserCredentialEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
   * @return the number of rows deleted (0 or 1: V7's unique index)
   */
  long deleteByUserIdAndProviderName(String userId, String providerName);

  /**
   * Inserts the credential unless the user already has one for the provider (V7's unique index).
   *
   * @return 1 when inserted, 0 when one exists (also when a concurrent request just created it)
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          "INSERT INTO user_credentials (user_id, provider_name, api_key)"
              + " VALUES (:userId, :providerName, :storedKey)"
              + " ON CONFLICT (user_id, provider_name) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(
      @Param("userId") String userId,
      @Param("providerName") String providerName,
      @Param("storedKey") String storedKey);

  /**
   * Replaces the stored (sealed) key and stamps {@code updated_at}.
   *
   * @return the number of rows changed (0 when the user has no key for the provider)
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update UserCredentialEntity c set c.apiKey = :storedKey, c.updatedAt = CURRENT_TIMESTAMP"
          + " where c.userId = :userId and c.providerName = :providerName")
  int replaceStoredKey(
      @Param("userId") String userId,
      @Param("providerName") String providerName,
      @Param("storedKey") String storedKey);
}
