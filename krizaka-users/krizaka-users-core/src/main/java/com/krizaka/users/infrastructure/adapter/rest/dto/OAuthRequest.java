package com.krizaka.users.infrastructure.adapter.rest.dto;

import com.krizaka.users.domain.exception.InvalidRequestException;

/**
 * Request data containing an external OAuth2 identity token for the Token-Exchange pattern.
 *
 * <p>The frontend (NextAuth) performs the OAuth2 protocol negotiation and forwards the resulting
 * identity token to the backend for verification and reconciliation.
 *
 * @param provider The identity provider identifier (e.g., "google", "github").
 * @param idToken The raw identity token or access token issued by the external provider.
 * @param email Optional email hint for logging (actual email is extracted from the token).
 * @param username Optional username hint (fallback to provider profile name if blank).
 */
public record OAuthRequest(String provider, String idToken, String email, String username) {
  /** Compact constructor validating required fields. */
  public OAuthRequest {
    if (provider == null || provider.isBlank()) {
      throw new InvalidRequestException("Provider is required for OAuth login");
    }
    if (idToken == null || idToken.isBlank()) {
      throw new InvalidRequestException("Identity token is required for OAuth login");
    }
  }
}
