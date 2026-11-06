package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA Entity mapping the {@code authorities} database table.
 *
 * <p>Represents a security authority role granted to a user.
 */
@Entity
@Table(
    name = "authorities",
    uniqueConstraints =
        @UniqueConstraint(
            name = "unique_user_authority",
            columnNames = {"user_id", "authority_name"}))
public class AuthorityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "user_id", nullable = false, length = 255)
  private String userId;

  @Column(name = "authority_name", nullable = false, length = 100)
  private String authorityName;

  /** Default constructor required by JPA/Hibernate. */
  public AuthorityEntity() {
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

  public String getAuthorityName() {
    return authorityName;
  }

  public void setAuthorityName(String authorityName) {
    this.authorityName = authorityName;
  }
}
