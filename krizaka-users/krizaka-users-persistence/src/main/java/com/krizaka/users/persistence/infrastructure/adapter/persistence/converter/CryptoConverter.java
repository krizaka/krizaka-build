package com.krizaka.users.persistence.infrastructure.adapter.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * Transparent two-way JPA AttributeConverter that encrypts/decrypts fields written to the database
 * using AES-256.
 *
 * <p>A Spring bean: Hibernate resolves it through Spring's bean container, so the key and salt
 * ({@code krizaka.users.crypto.key} / {@code krizaka.users.crypto.salt}) arrive by constructor from
 * the context that owns it — no static state, no environment fallback. A missing or blank value
 * fails the startup.
 */
@Component
@Converter
public class CryptoConverter implements AttributeConverter<String, String> {

  private final TextEncryptor encryptor;

  /**
   * @throws IllegalStateException if the key or the salt is blank
   */
  public CryptoConverter(
      @Value("${krizaka.users.crypto.key}") String key,
      @Value("${krizaka.users.crypto.salt}") String salt) {
    if (key.isBlank() || salt.isBlank()) {
      throw new IllegalStateException(
          "krizaka.users.crypto.key and krizaka.users.crypto.salt must be configured");
    }
    this.encryptor = Encryptors.text(key, salt);
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return attribute == null ? null : encryptor.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return dbData == null ? null : encryptor.decrypt(dbData);
  }
}
