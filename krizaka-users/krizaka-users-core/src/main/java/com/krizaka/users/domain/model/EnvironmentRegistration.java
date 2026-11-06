package com.krizaka.users.domain.model;

/**
 * An OAuth client the host was started with (its environment): imported once as a sealed
 * connection.
 *
 * @param provider the provider id
 * @param clientId the public client id
 * @param clientSecret the clear client secret, read from the environment and sealed at once
 */
public record EnvironmentRegistration(String provider, String clientId, String clientSecret) {

  /** Whether the environment actually configures this provider (a client id and a secret). */
  public boolean configured() {
    return clientId != null
        && !clientId.isBlank()
        && clientSecret != null
        && !clientSecret.isBlank();
  }

  /** The record never prints the secret. */
  @Override
  public String toString() {
    return "EnvironmentRegistration[provider=" + provider + "]";
  }
}
