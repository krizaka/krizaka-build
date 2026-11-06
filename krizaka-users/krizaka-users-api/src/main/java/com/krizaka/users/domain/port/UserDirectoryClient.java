package com.krizaka.users.domain.port;

import com.krizaka.users.domain.model.CredentialGrant;
import com.krizaka.users.domain.model.RateLimitInfo;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserProfile;
import java.util.Optional;

/**
 * What another service may ask the users service about a user — the read side of its internal API
 * ({@code /internal/v1/users/**}, {@code /internal/v1/tiers/**}, {@code
 * /internal/v1/credentials/**}).
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
   * Asks for a single-use grant to release the user's own provider key for one run, on the user's
   * authority: {@code userSessionToken} is the bearer the user called the run's service with. The
   * service that owns the run asks once, when the user starts it, and carries the grant with the
   * run (GHSA-3pf7-rgf2-4x3m).
   *
   * @param userSessionToken the user's own session token (never a service token)
   * @param providerName the provider, e.g. {@code openai}
   * @param runId the run (job id, chat turn id) the key is released for
   * @return the grant, or empty when the user stored no key for this provider
   */
  Optional<CredentialGrant> grantCredential(
      String userSessionToken, String providerName, String runId);

  /**
   * Redeems a grant, once, for the decrypted key of the run it was issued for. Keep the key in
   * memory for the call only.
   *
   * @param grant the grant from {@link #grantCredential}
   * @param providerName the provider it was issued for
   * @param runId the run it was issued for
   * @return the key, or empty when the grant is refused (unknown, expired, used, another run) or
   *     the key was deleted
   */
  Optional<String> useCredential(CredentialGrant grant, String providerName, String runId);

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
