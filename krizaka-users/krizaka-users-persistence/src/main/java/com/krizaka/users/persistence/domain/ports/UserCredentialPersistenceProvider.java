package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import java.util.List;
import java.util.Optional;

/** Port interface for managing UserCredential persistence operations. */
public interface UserCredentialPersistenceProvider {

  List<UserCredentialDto> findByUserId(String userId);

  Optional<UserCredentialDto> findByUserIdAndProviderName(String userId, String providerName);

  /** Inserts a new credential; empty when the user already has one for the provider. */
  Optional<UserCredentialDto> insert(UserCredentialDto credentialDto);

  /** Replaces the stored (sealed) key of an existing credential; false when there is none. */
  boolean replaceStoredKey(String userId, String providerName, String storedKey);

  /** Deletes the row, its sealed key with it; false when there was none. */
  boolean deleteByUserIdAndProviderName(String userId, String providerName);
}
