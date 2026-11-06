package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.PasswordResetTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link PasswordResetTokenEntity}. */
@Repository
public interface PasswordResetTokenJpaRepository
    extends JpaRepository<PasswordResetTokenEntity, String> {

  /**
   * Resolves a password reset token by its SHA-256 hash.
   *
   * @param tokenHash The hex-encoded SHA-256 hash of the plaintext token.
   * @return An Optional containing the matching token entity, if any.
   */
  Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

  /**
   * Deletes all password reset tokens associated with the given email.
   *
   * @param email The user's email address.
   */
  void deleteByEmail(String email);
}
