package com.krizaka.users.domain.port;

import com.krizaka.users.domain.model.RateLimitInfo;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserProfile;
import java.util.Optional;

/**
 * What another service may ask the users service about a user — the read side of its internal API
 * ({@code /internal/v1/users/**}, {@code /internal/v1/tiers/**}).
 *
 * <p>Implemented over HTTP by {@code krizaka-users-client}; a service depends on this contract,
 * never on the users service's implementation.
 */
public interface UserDirectoryClient {

  /**
   * The user with this id: identity, roles, preferences and rate-limit tier.
   *
   * @param userId the user's opaque id
   * @return the user
   * @throws RuntimeException when the user is unknown or the users service is unreachable
   */
  User getUser(String userId);

  /**
   * The user's profile.
   *
   * @param userId the user's opaque id
   * @return the profile, or {@code null} when the user has none
   */
  UserProfile getProfile(String userId);

  /**
   * The user's own provider key (bring-your-own-key), decrypted for the call it is needed for.
   *
   * @param userId the user's opaque id
   * @param providerName the provider, e.g. {@code openai}
   * @return the key, or empty when the user stored none for this provider
   */
  Optional<String> getDecryptedApiKey(String userId, String providerName);

  /**
   * The bucket parameters of a rate-limit tier.
   *
   * @param tierKey the tier's key
   * @return the tier, or empty when unknown
   */
  Optional<RateLimitInfo> getRateLimit(String tierKey);

  /**
   * The key of the tier a user without one falls into.
   *
   * @return the default tier key, or empty when none is flagged
   */
  Optional<String> getDefaultTierKey();
}
