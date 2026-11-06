package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.ExtractedProfile;

/**
 * Strategy interface for verifying external OAuth2 identity tokens and extracting user profiles.
 *
 * <p>Each concrete implementation handles a specific identity provider (e.g., Google, GitHub) and
 * is conditionally loaded based on configuration properties. This follows the Open-Closed
 * Principle: adding a new provider requires only a new implementation class and its config flag.
 *
 * <p><strong>Contract:</strong>
 *
 * <ul>
 *   <li>{@link #supports(String)} — Checks if this verifier handles the given provider identifier.
 *   <li>{@link #verifyAndExtract(String)} — Validates the token and extracts a canonical profile.
 * </ul>
 *
 * @see ExtractedProfile
 */
public interface OAuth2ProviderVerifier {

  /**
   * Determines whether this verifier supports the given provider identifier.
   *
   * @param providerId The provider name (e.g., "google", "github").
   * @return {@code true} if this verifier handles the specified provider.
   */
  boolean supports(String providerId);

  /**
   * Verifies the authenticity of the provided identity token and extracts a canonical profile.
   *
   * @param idToken The raw identity token issued by the external provider.
   * @return An {@link ExtractedProfile} containing the verified user attributes.
   * @throws IllegalArgumentException if the token is invalid, expired, or unverifiable.
   */
  ExtractedProfile verifyAndExtract(String idToken);
}
