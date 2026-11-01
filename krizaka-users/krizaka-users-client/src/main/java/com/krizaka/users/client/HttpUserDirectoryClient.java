package com.krizaka.users.client;

import com.krizaka.security.token.ServiceTokenProvider;
import com.krizaka.users.domain.model.RateLimitInfo;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserProfile;
import com.krizaka.users.domain.port.UserDirectoryClient;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.http.HttpRequest;
import org.springframework.web.client.RestClient;

/**
 * {@link UserDirectoryClient} over the users service's internal HTTP API.
 *
 * <p>Every call carries a fresh {@code SERVICE} token. Users, profiles and tiers are cached per
 * entry for {@code krizaka.users.client.cache-ttl}: they are read on the authentication path of
 * every request, and change rarely. Provider keys are never cached.
 */
final class HttpUserDirectoryClient implements UserDirectoryClient {

  private final RestClient usersClient;
  private final long cacheTtlMillis;
  private final ConcurrentHashMap<String, CachedEntry<User>> userCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CachedEntry<Optional<UserProfile>>> profileCache =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CachedEntry<Optional<RateLimitInfo>>> tierCache =
      new ConcurrentHashMap<>();

  /** Cached value with TTL expiry (millis since epoch). */
  private record CachedEntry<T>(T value, long expiresAt) {}

  /** Wire snapshot of {@code GET /internal/v1/users/{id}}. */
  record UserSnapshot(
      String id,
      String username,
      String email,
      boolean enabled,
      List<String> authorities,
      Map<String, Object> preferences,
      List<String> activeInterceptions,
      String rateLimitTier) {}

  HttpUserDirectoryClient(RestClient.Builder restClientBuilder, UsersClientProperties properties) {
    Objects.requireNonNull(properties, "UsersClientProperties cannot be null");
    ServiceTokenProvider tokens = tokensOf(properties);
    this.usersClient =
        restClientBuilder
            .baseUrl(properties.baseUrl())
            // On the builder, not per call: /internal/v1 demands the SERVICE authority, and a
            // method added later cannot forget the header.
            .requestInitializer(request -> request.getHeaders().setBearerAuth(tokens.token()))
            .build();
    this.cacheTtlMillis = properties.cacheTtl().toMillis();
  }

  /** Own ES256 key when configured (ADR-001); the legacy shared secret otherwise. */
  static ServiceTokenProvider tokensOf(UsersClientProperties properties) {
    if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
      return new ServiceTokenProvider(properties.serviceSecret(), properties.serviceName());
    }
    try {
      var der =
          java.util.Base64.getMimeDecoder()
              .decode(properties.serviceKey().replaceAll("-----[A-Z ]+-----", "").strip());
      var key =
          (java.security.interfaces.ECPrivateKey)
              java.security.KeyFactory.getInstance("EC")
                  .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(der));
      return ServiceTokenProvider.es256(key, properties.serviceKeyId(), properties.serviceName());
    } catch (java.security.GeneralSecurityException | IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "krizaka.users.client.service-key is not a valid P-256 PKCS#8 PEM");
    }
  }

  @Override
  public User getUser(String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return cached(
        userCache,
        userId,
        () -> {
          UserSnapshot snapshot =
              usersClient
                  .get()
                  .uri("/internal/v1/users/{id}", userId)
                  .retrieve()
                  .body(UserSnapshot.class);
          Objects.requireNonNull(snapshot, "the users service returned no body");
          return toUser(snapshot);
        });
  }

  @Override
  public UserProfile getProfile(String userId) {
    Objects.requireNonNull(userId, "userId cannot be null");
    return cached(
            profileCache,
            userId,
            () ->
                Optional.ofNullable(
                    usersClient
                        .get()
                        .uri("/internal/v1/users/{id}/profile", userId)
                        .exchange(HttpUserDirectoryClient::profileOrNull)))
        .orElse(null);
  }

  private static UserProfile profileOrNull(
      HttpRequest request, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
      throws IOException {
    return response.getStatusCode().is2xxSuccessful() ? response.bodyTo(UserProfile.class) : null;
  }

  @Override
  public Optional<String> getDecryptedApiKey(String userId, String providerName) {
    CredentialSnapshot snapshot =
        usersClient
            .get()
            .uri("/internal/v1/users/{id}/credentials/{provider}", userId, providerName)
            .exchange(
                (request, response) ->
                    response.getStatusCode().is2xxSuccessful()
                        ? response.bodyTo(CredentialSnapshot.class)
                        : null);
    return Optional.ofNullable(snapshot).map(CredentialSnapshot::apiKey);
  }

  @Override
  public Optional<RateLimitInfo> getRateLimit(String tierKey) {
    Objects.requireNonNull(tierKey, "tierKey cannot be null");
    return cached(tierCache, tierKey, () -> fetchTier("/internal/v1/tiers/{key}", tierKey));
  }

  @Override
  public Optional<String> getDefaultTierKey() {
    return cached(tierCache, "__default__", () -> fetchTier("/internal/v1/tiers/default", null))
        .map(RateLimitInfo::tierKey);
  }

  private Optional<RateLimitInfo> fetchTier(String uri, String key) {
    RateLimitInfo info =
        (key != null ? usersClient.get().uri(uri, key) : usersClient.get().uri(uri))
            .exchange(
                (request, response) ->
                    response.getStatusCode().is2xxSuccessful()
                        ? response.bodyTo(RateLimitInfo.class)
                        : null);
    return Optional.ofNullable(info);
  }

  private static User toUser(UserSnapshot snapshot) {
    return new User(
        UUID.fromString(snapshot.id()),
        snapshot.username(),
        snapshot.email(),
        snapshot.enabled(),
        Set.copyOf(snapshot.authorities()),
        snapshot.preferences(),
        snapshot.activeInterceptions(),
        snapshot.rateLimitTier());
  }

  private <T> T cached(
      ConcurrentHashMap<String, CachedEntry<T>> cache, String key, Supplier<T> loader) {
    long now = System.currentTimeMillis();
    CachedEntry<T> entry = cache.get(key);
    if (entry != null && entry.expiresAt() > now) {
      return entry.value();
    }
    T value = loader.get();
    cache.put(key, new CachedEntry<>(value, now + cacheTtlMillis));
    return value;
  }

  /** Wire snapshot of the internal credential endpoint. */
  record CredentialSnapshot(String apiKey) {}
}
