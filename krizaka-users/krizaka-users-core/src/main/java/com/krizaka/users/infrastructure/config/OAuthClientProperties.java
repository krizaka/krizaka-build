package com.krizaka.users.infrastructure.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The OAuth clients this host accepts tokens for ({@code
 * spring.security.oauth2.client.registration.<id>}): the verifiers need the client id (Google's
 * audience) and secret (GitHub's token check).
 *
 * @param registration provider id → its client
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public record OAuthClientProperties(Map<String, Registration> registration) {

  /** No registration reads as an empty map. */
  public OAuthClientProperties {
    registration = registration == null ? Map.of() : Map.copyOf(registration);
  }

  /**
   * One OAuth client.
   *
   * @param clientId the client id
   * @param clientSecret the client secret
   */
  public record Registration(String clientId, String clientSecret) {}

  /** The client of a provider; an absent one has blank id and secret. */
  public Registration of(String provider) {
    return registration.getOrDefault(provider, new Registration("", ""));
  }
}
