package com.krizaka.users.infrastructure.adapter.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.krizaka.users.infrastructure.config.OAuthClientProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/** Unit tests for {@link FederationConfig}: a provider is on exactly when its client id is set. */
class FederationConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(FederationConfig.class)
          .withBean(RestClient.class, () -> mock(RestClient.class))
          .withBean(ObjectMapper.class, ObjectMapper::new)
          .withBean(
              OAuthClientProperties.class,
              () ->
                  new OAuthClientProperties(
                      Map.of(
                          "google", new OAuthClientProperties.Registration("g-id", "g-secret"),
                          "github", new OAuthClientProperties.Registration("gh-id", "gh-secret"))));

  @Test
  void verifiersExistWhenTheirClientIdIsSet() {
    runner
        .withPropertyValues(
            "spring.security.oauth2.client.registration.google.client-id=g-id",
            "spring.security.oauth2.client.registration.github.client-id=gh-id")
        .run(
            ctx -> {
              assertThat(ctx).hasSingleBean(GoogleProviderVerifier.class);
              assertThat(ctx).hasSingleBean(GithubProviderVerifier.class);
            });
  }

  // An empty variable (`${GOOGLE_CLIENT_ID:}`) used to turn the verifier on, with no client to
  // check.
  @Test
  void anEmptyClientIdLeavesTheProviderOff() {
    runner
        .withPropertyValues(
            "spring.security.oauth2.client.registration.google.client-id=",
            "spring.security.oauth2.client.registration.github.client-id=  ")
        .run(
            ctx -> {
              assertThat(ctx).doesNotHaveBean(GoogleProviderVerifier.class);
              assertThat(ctx).doesNotHaveBean(GithubProviderVerifier.class);
            });
  }
}
