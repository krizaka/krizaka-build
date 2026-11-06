package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code user_interceptions} database table.
 *
 * <p>Represents a block or check that intercepts a user flow (like onboarding).
 */
@Entity
@Table(name = "user_interceptions")
public class UserInterceptionEntity {

  @EmbeddedId private UserInterceptionId id;

  @Column(name = "schema_id", nullable = false, length = 100)
  private String schemaId;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public UserInterceptionEntity() {
    /* JPA requires no-arg constructor */
  }

  public UserInterceptionId getId() {
    return id;
  }

  public void setId(UserInterceptionId id) {
    this.id = id;
  }

  public String getSchemaId() {
    return schemaId;
  }

  public void setSchemaId(String schemaId) {
    this.schemaId = schemaId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
