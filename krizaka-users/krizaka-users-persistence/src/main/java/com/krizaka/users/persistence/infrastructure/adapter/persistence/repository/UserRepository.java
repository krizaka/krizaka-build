package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link UserEntity}. */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {

  /**
   * Resolves an enabled user by their email address.
   *
   * @param email The user email.
   * @return An Optional containing the matched user entity, if any.
   */
  Optional<UserEntity> findByEmailAndEnabledTrue(String email);

  /**
   * Resolves a user profile by unique identifier, left join-fetching their authorities and
   * interceptions.
   *
   * @param id The user ID.
   * @return An Optional containing the matched user entity with fetched associations, if any.
   */
  @Query(
      "SELECT u FROM UserEntity u LEFT JOIN FETCH u.authorities LEFT JOIN FETCH u.interceptions WHERE u.id = :id")
  Optional<UserEntity> findByIdWithAssociations(@Param("id") String id);

  /**
   * Resolves an enabled user by email, left join-fetching their authorities and interceptions.
   *
   * @param email The user email.
   * @return An Optional containing the matched user entity with fetched associations, if any.
   */
  @Query(
      "SELECT u FROM UserEntity u LEFT JOIN FETCH u.authorities LEFT JOIN FETCH u.interceptions WHERE u.email = :email AND u.enabled = true")
  Optional<UserEntity> findByEmailAndEnabledTrueWithAssociations(@Param("email") String email);

  /**
   * Counts the number of users registered with the specified email address.
   *
   * @param email The email to check.
   * @return The count of matching users.
   */
  long countByEmail(String email);

  /** Whether a user already has this username. */
  boolean existsByUsername(String username);

  /**
   * Resolves a user by their external identity provider and provider-specific user identifier.
   *
   * @param provider The authentication provider name (e.g., "google", "github").
   * @param providerId The unique user identifier assigned by the external provider.
   * @return An Optional containing the matched user entity, if any.
   */
  Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);

  /**
   * Updates the password hash and sets the password_changed_at timestamp for session invalidation.
   *
   * @param email The user's email address.
   * @param hash The new BCrypt password hash.
   * @return The number of rows updated.
   */
  @Modifying
  @Query(
      "UPDATE UserEntity u SET u.passwordHash = :hash, u.passwordChangedAt = CURRENT_TIMESTAMP"
          + " WHERE u.email = :email")
  int updatePasswordHashByEmail(@Param("email") String email, @Param("hash") String hash);

  /**
   * Replaces the hash by a stronger one of the same password (rehash at sign-in): {@code
   * password_changed_at} is left alone, so no session is invalidated.
   */
  @Modifying
  @Query("UPDATE UserEntity u SET u.passwordHash = :hash WHERE u.email = :email")
  int rehashPasswordByEmail(@Param("email") String email, @Param("hash") String hash);

  /**
   * Resolves any user by email, regardless of enabled status.
   *
   * @param email The user's email address.
   * @return An Optional containing the matched user entity, if any.
   */
  Optional<UserEntity> findByEmail(String email);
}
