package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.VerificationToken;
import java.util.Optional;

/** Outbound port defining verification token persistence operations. */
public interface VerificationTokenRepositoryPort {

  /**
   * Saves a new verification token to the persistence store.
   *
   * @param token The VerificationToken record.
   */
  void save(VerificationToken token);

  /**
   * Resolves an unused verification token by its hash.
   *
   * @param tokenHash The token hash.
   * @return An Optional containing the VerificationToken.
   */
  Optional<VerificationToken> findByTokenHashAndUsedFalse(String tokenHash);

  /**
   * Marks a verification token as used in the database.
   *
   * @param tokenId The token unique identifier.
   */
  void markAsUsed(String tokenId);
}
