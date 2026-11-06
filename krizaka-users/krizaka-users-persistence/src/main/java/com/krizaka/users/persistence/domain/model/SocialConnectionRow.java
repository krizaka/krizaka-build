package com.krizaka.users.persistence.domain.model;

import java.time.Instant;

/**
 * One identity-provider connection as stored: the client secret is the sealed envelope, never the
 * clear value.
 *
 * @param provider the provider id ({@code google}, {@code github}…)
 * @param clientId the public client id
 * @param sealedClientSecret the {@code v2.} envelope, or {@code null} when no secret is set
 * @param keyId the vault key the envelope is wrapped under, or {@code null}
 * @param source where the connection came from ({@code env} or {@code admin})
 * @param updatedAt last change
 */
public record SocialConnectionRow(
    String provider,
    String clientId,
    String sealedClientSecret,
    String keyId,
    String source,
    Instant updatedAt) {

  /** The sealed value is a secret in transit: it never appears in a log line or a string dump. */
  @Override
  public String toString() {
    return "SocialConnectionRow[provider=" + provider + ", keyId=" + keyId + "]";
  }
}
