package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.AuthorityEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link AuthorityEntity}. */
@Repository
public interface AuthorityRepository extends JpaRepository<AuthorityEntity, Long> {

  /**
   * Finds all authorities configured for a given user.
   *
   * @param userId The unique user identifier.
   * @return A list of matching authority entities.
   */
  List<AuthorityEntity> findByUserId(String userId);
}
