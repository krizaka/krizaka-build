package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OrganizationEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data repository of organizations. */
@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, String> {

  /** Resolves an organization by its unique slug. */
  Optional<OrganizationEntity> findBySlug(String slug);

  /** The organizations a user belongs to, by name. */
  @Query(
      "SELECT o FROM OrganizationEntity o WHERE o.id IN "
          + "(SELECT m.id.organizationId FROM OrganizationMemberEntity m WHERE m.id.userId = :userId)"
          + " ORDER BY o.name")
  List<OrganizationEntity> findByMember(@Param("userId") String userId);
}
