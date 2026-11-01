package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.PasswordResetTokenDto;
import java.util.Optional;

/** Port interface for managing password reset token persistence operations. */
public interface PasswordResetTokenPersistenceProvider {

  /**
   * Persists a password reset token.
   *
   * @param dto The password reset token DTO.
   */
  void save(PasswordResetTokenDto dto);

  /**
   * Resolves a password reset token by its SHA-256 hash.
   *
   * @param tokenHash The hex-encoded SHA-256 hash.
   * @return An Optional containing the matching DTO, if found.
   */
  Optional<PasswordResetTokenDto> findByTokenHash(String tokenHash);

  /**
   * Deletes a password reset token by ID.
   *
   * @param id The token record ID.
   */
  void deleteById(String id);

  /**
   * Deletes all password reset tokens for the given email.
   *
   * @param email The user's email address.
   */
  void deleteByEmail(String email);
}
