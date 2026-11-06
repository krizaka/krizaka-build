package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.VerificationTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link VerificationTokenEntity}. */
@Repository
public interface VerificationTokenRepository
    extends JpaRepository<VerificationTokenEntity, String> {

  /**
   * Resolves an unused verification token by its hash.
   *
   * @param tokenHash The hashed verification token value.
   * @return An Optional containing the matching token entity, if any.
   */
  Optional<VerificationTokenEntity> findByTokenHashAndUsedFalse(String tokenHash);
}
