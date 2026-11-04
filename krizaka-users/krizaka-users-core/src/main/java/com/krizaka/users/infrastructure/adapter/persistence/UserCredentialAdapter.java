package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.exception.CredentialConflictException;
import com.krizaka.users.domain.exception.CredentialNotFoundException;
import com.krizaka.users.domain.model.UserCredential;
import com.krizaka.users.domain.ports.outbound.SecretSealPort;
import com.krizaka.users.domain.ports.outbound.UserCredentialRepositoryPort;
import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import com.krizaka.users.persistence.domain.ports.UserCredentialPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.converter.CryptoConverter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The user's provider keys (BYOK), sealed before they reach the database (krizaka-internal#2359):
 * the {@code v2.} envelope of {@link SecretSealPort} (AES-256-GCM, a data key per value, the
 * key-encryption key named by id, so rotation re-wraps), bound to {@code <user id>|<provider>} so a
 * value copied to another row does not open. A row still in the old AES-CBC form ({@link
 * CryptoConverter}) is opened once and re-sealed on its first read. The key never leaves this class
 * except to the internal caller that uses it; no message here carries it.
 */
@Component
class UserCredentialAdapter implements UserCredentialRepositoryPort {

  private static final String USER_ID_NULL = "UserId cannot be null";
  private static final String PROVIDER_NAME_NULL = "ProviderName cannot be null";
  private static final String SEALED = "v2.";

  private final UserCredentialPersistenceProvider provider;
  private final SecretSealPort vault;
  private final CryptoConverter legacy;

  UserCredentialAdapter(
      UserCredentialPersistenceProvider provider, SecretSealPort vault, CryptoConverter legacy) {
    this.provider =
        Objects.requireNonNull(provider, "UserCredentialPersistenceProvider cannot be null");
    this.vault = Objects.requireNonNull(vault, "SecretSealPort cannot be null");
    this.legacy = Objects.requireNonNull(legacy, "CryptoConverter cannot be null");
  }

  private static String context(String userId, String providerName) {
    return userId + "|" + providerName;
  }

  @Override
  public List<UserCredential> findByUserId(String userId) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    return provider.findByUserId(userId).stream()
        .map(dto -> new UserCredential(dto.providerName(), true))
        .toList();
  }

  @Override
  public Optional<String> findApiKeyByUserIdAndProviderName(String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    return provider
        .findByUserIdAndProviderName(userId, providerName)
        .map(dto -> open(dto, context(userId, providerName)));
  }

  private String open(UserCredentialDto dto, String context) {
    String stored = dto.apiKey();
    if (stored.startsWith(SEALED)) {
      return vault.open(stored, context);
    }
    String key = legacy.convertToEntityAttribute(stored);
    provider.replaceStoredKey(dto.userId(), dto.providerName(), vault.seal(key, context));
    return key;
  }

  @Override
  public void create(String userId, String providerName, String apiKey) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    Objects.requireNonNull(apiKey, "ApiKey cannot be null");
    String sealed = vault.seal(apiKey, context(userId, providerName));
    provider
        .insert(new UserCredentialDto(null, userId, providerName, sealed))
        .orElseThrow(() -> new CredentialConflictException(providerName));
  }

  @Override
  public void rotate(String userId, String providerName, String apiKey) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    Objects.requireNonNull(apiKey, "ApiKey cannot be null");
    String sealed = vault.seal(apiKey, context(userId, providerName));
    if (!provider.replaceStoredKey(userId, providerName, sealed)) {
      throw new CredentialNotFoundException(providerName);
    }
  }

  @Override
  public void delete(String userId, String providerName) {
    Objects.requireNonNull(userId, USER_ID_NULL);
    Objects.requireNonNull(providerName, PROVIDER_NAME_NULL);
    if (!provider.deleteByUserIdAndProviderName(userId, providerName)) {
      throw new CredentialNotFoundException(providerName);
    }
  }
}
