package com.krizaka.users.infrastructure.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The vault of the identity secrets ({@code krizaka.users.vault}, ADR-016 decision 6). The
 * key-encryption key is {@code AUTH_VAULT_KEY} (32 bytes in base64); unset, nothing is sealed and
 * nothing is imported. The properties never print the keys.
 *
 * @param key the current key-encryption key, base64 (32 bytes)
 * @param keyId the id new values are wrapped under (1 to 32 letters, digits, {@code -}, {@code _})
 * @param retired the previous keys by id, kept until every value is re-wrapped
 */
@ConfigurationProperties(prefix = "krizaka.users.vault")
public record VaultProperties(String key, String keyId, Map<String, String> retired) {

  /** Defaults: no key, id {@code k1}, no retired key. */
  public VaultProperties {
    key = key == null ? "" : key.trim();
    keyId = keyId == null || keyId.isBlank() ? "k1" : keyId.trim();
    retired = retired == null ? Map.of() : Map.copyOf(retired);
  }

  @Override
  public String toString() {
    return "VaultProperties[keyId=" + keyId + ", configured=" + !key.isEmpty() + "]";
  }
}
