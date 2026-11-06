package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** JpaRepository interface for {@link UserProfileEntity} persistence operations. */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, String> {}
