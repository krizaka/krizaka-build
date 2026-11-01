package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.RateLimitTierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JPA Repository for performing database operations on {@link RateLimitTierEntity}. */
@Repository
public interface RateLimitTierRepository extends JpaRepository<RateLimitTierEntity, String> {}
