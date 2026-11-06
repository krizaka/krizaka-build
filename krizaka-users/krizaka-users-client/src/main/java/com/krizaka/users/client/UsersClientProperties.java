package com.krizaka.users.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the users service is and how to call it ({@code krizaka.users.client}).
 *
 * @param baseUrl Base URL of the users service, e.g. 'http://localhost:8083'. Setting it switches
 *     the client on.
 * @param serviceKey This caller's own P-256 private key (PKCS#8 PEM, from its environment, never
 *     committed): the SERVICE token of every call is signed ES256 with it (ADR-001), and the users
 *     service holds only the public half. Preferred.
 * @param serviceKeyId The kid of serviceKey. Required with it.
 * @param serviceSecret Legacy shared HS256 secret, accepted only until the users service's dated
 *     cutover. Exactly one of serviceKey and serviceSecret is required.
 * @param serviceName Who is calling: the subject of the SERVICE token, visible in the users
 *     service's logs. Required.
 * @param cacheTtl How long a user, profile or tier is reused before it is fetched again.
 */
@ConfigurationProperties(prefix = "krizaka.users.client")
public record UsersClientProperties(
    String baseUrl,
    String serviceKey,
    String serviceKeyId,
    String serviceSecret,
    String serviceName,
    @DefaultValue("60s") Duration cacheTtl) {

  /** Legacy form: the shared HS256 secret. */
  public UsersClientProperties(
      String baseUrl, String serviceSecret, String serviceName, Duration cacheTtl) {
    this(baseUrl, null, null, serviceSecret, serviceName, cacheTtl);
  }

  /** Refuses an incomplete client: a call that cannot authenticate fails at startup, not later. */
  @ConstructorBinding
  public UsersClientProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      throw new IllegalArgumentException("krizaka.users.client.base-url is required");
    }
    boolean hasKey = serviceKey != null && !serviceKey.isBlank();
    boolean hasSecret = serviceSecret != null && !serviceSecret.isBlank();
    if (hasKey == hasSecret) {
      throw new IllegalArgumentException(
          "krizaka.users.client needs exactly one of service-key (ES256) and service-secret"
              + " (legacy HS256)");
    }
    if (hasKey && (serviceKeyId == null || serviceKeyId.isBlank())) {
      throw new IllegalArgumentException("krizaka.users.client.service-key-id is required");
    }
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("krizaka.users.client.service-name is required");
    }
    if (cacheTtl == null || cacheTtl.isNegative()) {
      throw new IllegalArgumentException("krizaka.users.client.cache-ttl cannot be negative");
    }
  }

  /**
   * Hides the secret.
   *
   * @return a description without the secret
   */
  @Override
  public String toString() {
    String shape =
        "UsersClientProperties[baseUrl=%s, serviceName=%s, cacheTtl=%s, "
            + "serviceKey=<redacted>, serviceSecret=<redacted>]";
    return shape.formatted(baseUrl, serviceName, cacheTtl);
  }
}
