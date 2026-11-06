package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.PasswordResetToken;
import java.util.Optional;

/**
 * Outbound port defining password reset token persistence operations.
 *
 * <p>Isolates the domain service from concrete persistence mechanisms. The adapter implementation
 * lives in {@code krizaka-users-persistence} and bridges to JPA repositories.
 */
public interface PasswordResetTokenRepositoryPort {

  /**
   * Persists a new password reset token record.
   *
   * @param token The password reset token domain record.
   */
  void save(PasswordResetToken token);

  /**
   * Resolves a password reset token by its SHA-256 hash.
   *
   * @param tokenHash The hex-encoded SHA-256 hash of the plaintext token.
   * @return An Optional containing the matching token, if found.
   */
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  /**
   * Deletes a password reset token by its unique identifier.
   *
   * @param id The token record ID.
   */
  void deleteById(String id);

  /**
   * Deletes all password reset tokens associated with the given email.
   *
   * <p>Used to enforce single-use semantics: any prior tokens for the same email are purged before
   * a new token is issued.
   *
   * @param email The user's email address.
   */
  void deleteByEmail(String email);
}
