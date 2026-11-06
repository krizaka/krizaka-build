package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserInterceptionEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserInterceptionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link UserInterceptionEntity}. */
@Repository
public interface UserInterceptionRepository
    extends JpaRepository<UserInterceptionEntity, UserInterceptionId> {

  /**
   * Resolves all active interceptions for a given user.
   *
   * @param userId The unique user identifier.
   * @return A list of matching interception entities.
   */
  List<UserInterceptionEntity> findByIdUserId(String userId);
}
