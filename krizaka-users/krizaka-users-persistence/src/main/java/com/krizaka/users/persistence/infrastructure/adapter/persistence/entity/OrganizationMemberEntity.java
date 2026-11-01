package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA Entity mapping the {@code organization_members} database table. */
@Entity
@Table(name = "organization_members")
public class OrganizationMemberEntity {

  @EmbeddedId private OrganizationMemberId id;

  @Column(name = "role", nullable = false, length = 16)
  private String role;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public OrganizationMemberEntity() {
    /* JPA requires no-arg constructor */
  }

  public OrganizationMemberId getId() {
    return id;
  }

  public void setId(OrganizationMemberId id) {
    this.id = id;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
