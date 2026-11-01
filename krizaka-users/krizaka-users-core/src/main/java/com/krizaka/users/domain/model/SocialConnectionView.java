package com.krizaka.users.domain.model;

import java.time.Instant;

/**
 * What the administration sees of a connection: whether a secret is set, never its value, sealed or
 * not.
 *
 * @param provider the provider id
 * @param clientId the public client id
 * @param secretSet whether a client secret is stored
 * @param keyId the vault key id the secret is wrapped under
 * @param source where the connection came from
 * @param updatedAt last change
 */
public record SocialConnectionView(
    String provider,
    String clientId,
    boolean secretSet,
    String keyId,
    String source,
    Instant updatedAt) {

  /** The view of {@code connection}. */
  public static SocialConnectionView of(SocialConnection connection) {
    return new SocialConnectionView(
        connection.provider(),
        connection.clientId(),
        connection.sealedClientSecret() != null,
        connection.keyId(),
        connection.source(),
        connection.updatedAt());
  }
}
