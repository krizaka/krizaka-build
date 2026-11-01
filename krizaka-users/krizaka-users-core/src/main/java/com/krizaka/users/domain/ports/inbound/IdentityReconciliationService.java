package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.User;

/**
 * Port interface defining the OAuth2 identity federation and reconciliation contract.
 *
 * <p>The router binds exclusively to this interface via constructor injection. The concrete
 * implementation ({@code IdentityReconciliationServiceImpl}) is package-private within {@code
 * krizaka-users-core} and discovered by Spring component scanning.
 *
 * @see com.krizaka.users.domain.ports.outbound.OAuth2ProviderVerifier
 * @see com.krizaka.users.domain.model.ExtractedProfile
 */
public interface IdentityReconciliationService {

  /**
   * Verifies an external identity token and reconciles the user against the local database.
   *
   * <p>Delegates token verification to the matching provider verifier, then either resolves the
   * existing user or provisions a new federated user record with no password.
   *
   * @param provider The provider identifier (e.g., "google", "github").
   * @param idToken The raw identity token issued by the external provider.
   * @return The fully resolved domain {@link User} record.
   * @throws IllegalArgumentException if no verifier supports the given provider, or if the token is
   *     invalid.
   */
  User reconcile(String provider, String idToken);
}
