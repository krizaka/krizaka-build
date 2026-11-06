package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA Entity mapping the {@code organizations} database table. */
@Entity
@Table(name = "organizations")
public class OrganizationEntity {

  @Id
  @Column(name = "id", length = 255)
  private String id;

  @Column(name = "slug", nullable = false, length = 63, unique = true)
  private String slug;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public OrganizationEntity() {
    /* JPA requires no-arg constructor */
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
