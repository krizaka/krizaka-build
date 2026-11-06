package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code api_keys} database table.
 *
 * <p>Persists an inbound Personal Access Token: only the SHA-256 {@code keyHash} is stored (never
 * the plaintext secret), alongside a short display {@code keyPrefix} so users can recognise a key
 * in listings. The hash is intentionally <b>not</b> encrypted at rest — it must remain queryable
 * for constant-time lookup during bearer authentication.
 */
@Entity
@Table(name = "api_keys")
public class ApiKeyEntity {

  @Id
  @Column(name = "id", length = 255)
  private String id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "key_prefix", nullable = false, length = 20)
  private String keyPrefix;

  @Column(name = "key_hash", nullable = false, length = 255)
  private String keyHash;

  /** Comma-separated scopes; empty = unrestricted (every key before scoping existed). */
  @Column(name = "scopes", nullable = false, length = 255)
  private String scopes = "";

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  /** Default constructor required by JPA/Hibernate. */
  public ApiKeyEntity() {
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKeyPrefix() {
    return keyPrefix;
  }

  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public String getKeyHash() {
    return keyHash;
  }

  public void setKeyHash(String keyHash) {
    this.keyHash = keyHash;
  }

  public String getScopes() {
    return scopes;
  }

  public void setScopes(String scopes) {
    this.scopes = scopes == null ? "" : scopes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
