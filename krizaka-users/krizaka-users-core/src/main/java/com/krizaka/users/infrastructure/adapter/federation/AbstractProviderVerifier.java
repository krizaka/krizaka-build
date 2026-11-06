package com.krizaka.users.infrastructure.adapter.federation;

import com.krizaka.users.domain.model.ExtractedProfile;
import com.krizaka.users.domain.ports.outbound.OAuth2ProviderVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * Base class for OAuth2 provider verifiers.
 *
 * <p>Encapsulates the common token-validation contract shared by all provider implementations
 * (null-check, logging, delegation to the provider-specific verification method).
 *
 * @see OAuth2ProviderVerifier
 */
abstract class AbstractProviderVerifier implements OAuth2ProviderVerifier {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /** Returns the lowercase canonical provider identifier (e.g. "github", "google"). */
  protected abstract String providerId();

  /** Returns a human-readable label for log messages (e.g. "GitHub access token"). */
  protected abstract String tokenLabel();

  /**
   * Provider-specific verification logic.
   *
   * @param idToken the non-null, non-blank token to verify.
   * @return the extracted canonical profile.
   */
  protected abstract ExtractedProfile doVerifyAndExtract(String idToken);

  @Override
  public boolean supports(String providerId) {
    return providerId().equalsIgnoreCase(providerId);
  }

  @Override
  public ExtractedProfile verifyAndExtract(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException(tokenLabel() + " cannot be null or empty");
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Verifying {} (length={})", tokenLabel(), idToken.length());
    }
    return doVerifyAndExtract(idToken);
  }

  /**
   * Extracts a text value from a JSON node, returning {@code null} if the field is absent or null.
   *
   * @param node the parent JSON node.
   * @param field the field name to extract.
   * @return the text value or {@code null}.
   */
  protected static String textOrNull(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value != null && !value.isNull() ? value.asString() : null;
  }

  /**
   * Extracts a field from a JSON node, converting numbers to strings. Returns {@code null} if the
   * field is absent or null.
   *
   * @param node the parent JSON node.
   * @param field the field name to extract.
   * @return the string representation of the value, or {@code null}.
   */
  protected static String extractField(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    return value.isNumber() ? String.valueOf(value.asLong()) : value.asString();
  }
}
