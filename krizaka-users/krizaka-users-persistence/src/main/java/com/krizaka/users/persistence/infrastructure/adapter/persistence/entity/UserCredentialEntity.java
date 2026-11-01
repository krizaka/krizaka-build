package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import com.krizaka.users.persistence.infrastructure.adapter.persistence.converter.CryptoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity mapping the {@code user_credentials} database table. Encrypts the API key at rest
 * using {@link CryptoConverter}.
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
  @Convert(converter = CryptoConverter.class)
  private String apiKey;

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

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
