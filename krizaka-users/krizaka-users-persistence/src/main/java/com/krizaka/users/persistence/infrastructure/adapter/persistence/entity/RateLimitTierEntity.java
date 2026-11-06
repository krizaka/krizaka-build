package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA Entity mapping the {@code rate_limit_tiers} database table.
 *
 * <p>Represents a configured rate limiting tier that dictates bucket capacity, refill quotas, and
 * timing bounds.
 */
@Entity
@Table(name = "rate_limit_tiers")
public class RateLimitTierEntity {

  @Id
  @Column(name = "id", length = 50)
  private String id;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Column(name = "refill_tokens", nullable = false)
  private int refillTokens;

  @Column(name = "refill_seconds", nullable = false)
  private int refillSeconds;

  @Column(name = "updated_at")
  private Instant updatedAt;

  /** Default constructor required by JPA/Hibernate. */
  public RateLimitTierEntity() {
    /* JPA requires no-arg constructor */
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int getRefillTokens() {
    return refillTokens;
  }

  public void setRefillTokens(int refillTokens) {
    this.refillTokens = refillTokens;
  }

  public int getRefillSeconds() {
    return refillSeconds;
  }

  public void setRefillSeconds(int refillSeconds) {
    this.refillSeconds = refillSeconds;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
