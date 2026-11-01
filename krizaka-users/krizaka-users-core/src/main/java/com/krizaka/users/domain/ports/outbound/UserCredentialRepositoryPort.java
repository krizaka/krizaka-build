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
   * Saves or rotates a user's API key for a provider.
   *
   * @param userId The user ID.
   * @param providerName The provider name.
   * @param apiKey The plaintext API key.
   */
  void save(String userId, String providerName, String apiKey);

  /**
   * Deletes a user's credential.
   *
   * @param userId The user ID.
   * @param providerName The provider name.
   */
  void deleteByUserIdAndProviderName(String userId, String providerName);
}
