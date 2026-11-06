package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.RateLimitEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** JPA Repository for performing database operations on {@link RateLimitEntity}. */
public interface RateLimitRepository extends JpaRepository<RateLimitEntity, String> {

  /**
   * Finds the tier flagged as the system default ({@code is_default = true}).
   *
   * @return the default tier entity, or empty if none is flagged.
   */
  Optional<RateLimitEntity> findFirstByIsDefaultTrue();
}
