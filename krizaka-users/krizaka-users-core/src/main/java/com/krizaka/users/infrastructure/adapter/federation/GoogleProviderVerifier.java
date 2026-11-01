package com.krizaka.users.infrastructure.adapter.federation;

import com.krizaka.users.domain.model.ExtractedProfile;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * OAuth2 provider verifier for Google identity tokens.
 *
 * <p>The token is verified <b>locally</b>: its RS256 signature against Google's published key set
 * (JWKS, fetched once and cached by the decoder — no network call per login, and the token never
 * travels in a URL), then its issuer ({@code accounts.google.com}), its expiry and — ADR-001,
 * finding 2 — an audience that is <b>our</b> client id: a token issued to any other application is
 * refused. The e-mail address must be verified by Google. Then {@code sub}, {@code email}, {@code
 * name} and {@code picture} are extracted.
 */
class GoogleProviderVerifier extends AbstractProviderVerifier {

  static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

  private static final Set<String> ISSUERS =
      Set.of("accounts.google.com", "https://accounts.google.com");

  private final JwtDecoder decoder;
  private final String clientId;

  GoogleProviderVerifier(JwtDecoder decoder, String clientId) {
    this.decoder = Objects.requireNonNull(decoder, "Google JWT decoder");
    this.clientId = Objects.requireNonNull(clientId, "Google client id");
    logger.info("Google OAuth2 provider verifier initialized");
  }

  /** The decoder used in production: Google's JWKS (cached by Nimbus) and the checks below. */
  static JwtDecoder googleDecoder(String clientId) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URL).build();
    decoder.setJwtValidator(validator(clientId));
    return decoder;
  }

  /** Expiry (with the usual clock skew), issuer, and audience = this application. */
  static OAuth2TokenValidator<Jwt> validator(String clientId) {
    return new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(),
        jwt ->
            ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid Google token issuer", null)),
        jwt -> {
          List<String> audience = jwt.getAudience();
          return audience != null && audience.contains(clientId)
              ? OAuth2TokenValidatorResult.success()
              : OAuth2TokenValidatorResult.failure(
                  new OAuth2Error(
                      "invalid_token", "Google ID token audience is not this application", null));
        });
  }

  @Override
  protected String providerId() {
    return "google";
  }

  @Override
  protected String tokenLabel() {
    return "Google ID token";
  }

  @Override
  protected ExtractedProfile doVerifyAndExtract(String idToken) {
    Jwt jwt;
    try {
      jwt = decoder.decode(idToken);
    } catch (JwtException e) {
      throw new IllegalArgumentException(
          "Google ID token verification failed: " + e.getMessage(), e);
    }

    if (!"true".equalsIgnoreCase(String.valueOf(jwt.getClaims().get("email_verified")))) {
      throw new IllegalArgumentException("Google account e-mail address is not verified");
    }

    String sub = extractRequired(jwt.getSubject(), "Google user ID");
    String email = extractRequired(jwt.getClaimAsString("email"), "email");
    return new ExtractedProfile(
        email, sub, jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
  }

  private static String extractRequired(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Google token is missing required field: " + label);
    }
    return value;
  }
}
