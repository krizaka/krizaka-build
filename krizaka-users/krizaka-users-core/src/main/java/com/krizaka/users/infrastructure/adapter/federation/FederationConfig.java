package com.krizaka.users.infrastructure.adapter.federation;

import com.krizaka.users.domain.ports.inbound.SocialConnectionService;
import com.krizaka.users.infrastructure.config.OAuthClientProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Package-private configuration declaring the OAuth2 provider verifiers. A provider is on exactly
 * when its client id is set: an empty environment variable (the default `${GOOGLE_CLIENT_ID:}`)
 * leaves it off. The verifier gets the client it checks tokens against.
 */
@Configuration
class FederationConfig {

  @Bean
  @ConditionalOnExpression(
      "'${spring.security.oauth2.client.registration.google.client-id:}'.trim() != ''")
  GoogleProviderVerifier googleProviderVerifier(OAuthClientProperties clients) {
    String clientId = clients.of("google").clientId();
    return new GoogleProviderVerifier(GoogleProviderVerifier.googleDecoder(clientId), clientId);
  }

  @Bean
  @ConditionalOnExpression(
      "'${spring.security.oauth2.client.registration.github.client-id:}'.trim() != ''")
  GithubProviderVerifier githubProviderVerifier(
      RestClient identityRestClient,
      OAuthClientProperties clients,
      ObjectProvider<SocialConnectionService> vault) {
    OAuthClientProperties.Registration github = clients.of("github");
    String fromEnvironment = github.clientSecret() == null ? "" : github.clientSecret();
    // The vault is the source of truth (rotated by an administrator); the environment value is
    // only what is imported at first start, and the fallback while no vault key is configured.
    return new GithubProviderVerifier(
        identityRestClient,
        github.clientId(),
        () ->
            vault.stream()
                .findFirst()
                .flatMap(connections -> connections.clientSecret("github"))
                .orElse(fromEnvironment));
  }
}
