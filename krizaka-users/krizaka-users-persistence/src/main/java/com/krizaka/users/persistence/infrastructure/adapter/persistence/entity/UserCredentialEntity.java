package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code user_credentials} database table. {@code api_key} holds the key as
 * the service sealed it (a {@code v2.} envelope bound to user and provider, krizaka-internal#2359),
 * or, for a row written before, the old AES-CBC text the service re-seals on first read: this
 * entity never sees a plaintext key, so it converts nothing.
 */
@Entity
@Table(name = "user_credentials")
public class UserCredentialEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "provider_name", nullable = false, length = 255)
  private String providerName;

  @Column(name = "api_key", nullable = false, length = 1024)
  private String apiKey;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false)
  private Instant updatedAt;

  /** Default constructor required by JPA/Hibernate. */
  public UserCredentialEntity() {
    /* JPA requires no-arg constructor */
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
