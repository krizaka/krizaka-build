package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code verification_tokens} database table.
 *
 * <p>Represents a temporary token hash generated for authentication, registration, or password
 * reset flows.
 */
@Entity
@Table(name = "verification_tokens")
public class VerificationTokenEntity {

  @Id
  @Column(name = "id", length = 255)
  private String id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "token_type", nullable = false, length = 100)
  private String tokenType;

  @Column(name = "token_hash", nullable = false, length = 255)
  private String tokenHash;

  @Column(name = "expiry_timestamp", nullable = false)
  private Instant expiryTimestamp;

  @Column(name = "used")
  private Boolean used = false;

  @Column(name = "created_at")
  private Instant createdAt;

  /** Default constructor required by JPA/Hibernate. */
  public VerificationTokenEntity() {
    /* JPA requires no-arg constructor */
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public Instant getExpiryTimestamp() {
    return expiryTimestamp;
  }

  public void setExpiryTimestamp(Instant expiryTimestamp) {
    this.expiryTimestamp = expiryTimestamp;
  }

  public Boolean getUsed() {
    return used;
  }

  public void setUsed(Boolean used) {
    this.used = used;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
