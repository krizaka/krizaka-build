package com.krizaka.users.persistence.infrastructure.adapter.persistence.repository;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OrganizationMemberEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OrganizationMemberId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data repository of organization memberships. */
@Repository
public interface OrganizationMemberRepository
    extends JpaRepository<OrganizationMemberEntity, OrganizationMemberId> {

  /** Every membership of an organization, oldest first. */
  @Query(
      "SELECT m FROM OrganizationMemberEntity m WHERE m.id.organizationId = :organizationId"
          + " ORDER BY m.createdAt, m.id.userId")
  List<OrganizationMemberEntity> findByOrganization(@Param("organizationId") String organizationId);

  /** How many members hold a role in an organization. */
  @Query(
      "SELECT COUNT(m) FROM OrganizationMemberEntity m"
          + " WHERE m.id.organizationId = :organizationId AND m.role = :role")
  long countByRole(@Param("organizationId") String organizationId, @Param("role") String role);

  /** The user ids of the service accounts of an organization. */
  @Query(
      "SELECT m.id.userId FROM OrganizationMemberEntity m"
          + " WHERE m.id.organizationId = :organizationId AND m.role = 'SERVICE'")
  List<String> findServiceUserIds(@Param("organizationId") String organizationId);

  /** Removes a membership; the number of rows removed. */
  @Modifying
  @Query(
      "DELETE FROM OrganizationMemberEntity m"
          + " WHERE m.id.organizationId = :organizationId AND m.id.userId = :userId")
  int deleteMembership(
      @Param("organizationId") String organizationId, @Param("userId") String userId);
}
