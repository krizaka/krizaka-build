package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.UserCredential;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port defining user credentials persistence operations, isolating services from database
 * entities.
 */
public interface UserCredentialRepositoryPort {

  /**
   * Retrieves all credentials configured for a user.
   *
   * @param userId The user ID.
   * @return A list of user credential domain records.
   */
  List<UserCredential> findByUserId(String userId);

  /**
   * Resolves a user's API key for a provider.
   *
   * @param userId The user ID.
   * @param providerName The provider name.
   * @return An Optional containing the decrypted API key.
   */
  Optional<String> findApiKeyByUserIdAndProviderName(String userId, String providerName);

  /**
   * Seals and stores a new key.
   *
   * @throws com.krizaka.users.domain.exception.CredentialConflictException when the user already
   *     has a key for the provider (also when a concurrent request just created it)
   */
  void create(String userId, String providerName, String apiKey);

  /**
   * Seals and replaces the user's key for the provider.
   *
   * @throws com.krizaka.users.domain.exception.CredentialNotFoundException when there is none
   */
  void rotate(String userId, String providerName, String apiKey);

  /**
   * Deletes the user's key for the provider.
   *
   * @throws com.krizaka.users.domain.exception.CredentialNotFoundException when there is none
   */
  void delete(String userId, String providerName);
}
