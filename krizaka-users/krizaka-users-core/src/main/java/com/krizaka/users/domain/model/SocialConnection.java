package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * An identity-provider connection as the domain holds it: the client secret stays sealed (the
 * {@code v2.} envelope). Only the vault port opens it.
 *
 * @param provider the provider id
 * @param clientId the public client id
 * @param sealedClientSecret the sealed envelope, or {@code null} when no secret is set
 * @param keyId the vault key the envelope is wrapped under, or {@code null}
 * @param source {@code env} (imported once from the environment) or {@code admin}
 * @param updatedAt last change, {@code null} before the first save
 */
public record SocialConnection(
    String provider,
    String clientId,
    String sealedClientSecret,
    String keyId,
    String source,
    Instant updatedAt) {

  public SocialConnection {
    Objects.requireNonNull(provider, "provider");
    Objects.requireNonNull(clientId, "clientId");
    Objects.requireNonNull(source, "source");
  }

  /** The record never prints the sealed value. */
  @Override
  public String toString() {
    return "SocialConnection[provider=" + provider + ", keyId=" + keyId + "]";
  }
}
