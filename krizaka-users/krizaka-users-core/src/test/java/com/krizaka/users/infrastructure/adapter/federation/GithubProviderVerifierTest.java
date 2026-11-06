package com.krizaka.users.infrastructure.adapter.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.krizaka.users.domain.model.ExtractedProfile;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * GitHub sign-in: the access token is first checked against <b>our</b> OAuth app ({@code POST
 * /applications/{client_id}/token}, basic auth with the app's credentials) — a token issued to any
 * other application is refused (ADR-001, finding 3) — and the profile comes from that answer.
 */
class GithubProviderVerifierTest {

  private static final String CLIENT_ID = "Iv1.krizaka";
  private static final String CHECK = "https://api.github.com/applications/" + CLIENT_ID + "/token";
  private static final String EMAILS = "https://api.github.com/user/emails";

  private MockRestServiceServer server;
  private GithubProviderVerifier verifier;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    verifier = new GithubProviderVerifier(builder.build(), CLIENT_ID, "app-secret");
  }

  private static String basic() {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((CLIENT_ID + ":app-secret").getBytes(StandardCharsets.UTF_8));
  }

  private void tokenBelongsToUs(String userJson) {
    server
        .expect(requestTo(CHECK))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", basic()))
        .andExpect(content().json("{\"access_token\":\"gho_token\"}"))
        .andRespond(
            withSuccess("{\"id\":1,\"user\":" + userJson + "}", MediaType.APPLICATION_JSON));
  }

  @Test
  void theSecretIsReadFromTheVaultAtEachCallSoARotationNeedsNoRestart() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer rotating = MockRestServiceServer.bindTo(builder).build();
    String[] current = {"first-secret"};
    var live = new GithubProviderVerifier(builder.build(), CLIENT_ID, () -> current[0]);
    current[0] = "rotated-secret";
    String expected =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":rotated-secret").getBytes(StandardCharsets.UTF_8));
    rotating
        .expect(requestTo(CHECK))
        .andExpect(header("Authorization", expected))
        .andRespond(
            withSuccess(
                "{\"id\":1,\"user\":{\"id\":7,\"login\":\"o\",\"email\":\"o@x.io\"}}",
                MediaType.APPLICATION_JSON));

    assertThat(live.verifyAndExtract("gho_token").providerId()).isEqualTo("7");
    rotating.verify();
  }

  @Test
  void supportsGithubOnly() {
    assertThat(verifier.supports("github")).isTrue();
    assertThat(verifier.supports("GITHUB")).isTrue();
    assertThat(verifier.supports("google")).isFalse();
    assertThat(verifier.tokenLabel()).isEqualTo("GitHub access token");
  }

  @Test
  void aTokenOfOurAppGivesTheProfile() {
    tokenBelongsToUs(
        "{\"id\":12345,\"login\":\"octocat\",\"name\":\"The Octocat\","
            + "\"email\":\"octocat@github.com\","
            + "\"avatar_url\":\"https://avatars.githubusercontent.com/u/12345\"}");

    ExtractedProfile profile = verifier.verifyAndExtract("gho_token");

    assertThat(profile.providerId()).isEqualTo("12345");
    assertThat(profile.email()).isEqualTo("octocat@github.com");
    assertThat(profile.name()).isEqualTo("The Octocat");
    assertThat(profile.avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/12345");
    server.verify();
  }

  @Test
  void aTokenIssuedToAnotherApplicationIsRefused() {
    server.expect(requestTo(CHECK)).andRespond(withResourceNotFound());

    assertThatThrownBy(() -> verifier.verifyAndExtract("gho_token"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not issued to this application");
  }

  @Test
  void aPrivateEmailIsReadFromTheVerifiedPrimaryAddress() {
    tokenBelongsToUs("{\"id\":67890,\"login\":\"private\",\"name\":null,\"email\":null}");
    server
        .expect(requestTo(EMAILS))
        .andExpect(header("Authorization", "Bearer gho_token"))
        .andRespond(
            withSuccess(
                "[{\"email\":\"old@example.com\",\"primary\":false,\"verified\":true},"
                    + "{\"email\":\"unverified@example.com\",\"primary\":true,\"verified\":false},"
                    + "{\"email\":\"me@example.com\",\"primary\":true,\"verified\":true}]",
                MediaType.APPLICATION_JSON));

    ExtractedProfile profile = verifier.verifyAndExtract("gho_token");

    assertThat(profile.email()).isEqualTo("me@example.com");
    assertThat(profile.name()).isEqualTo("private");
  }

  @Test
  void withoutAnyVerifiedAddressTheNoreplyAddressIsUsed() {
    tokenBelongsToUs("{\"id\":67890,\"login\":\"private\",\"email\":\"\"}");
    server.expect(requestTo(EMAILS)).andRespond(withStatus(HttpStatus.FORBIDDEN));

    ExtractedProfile profile = verifier.verifyAndExtract("gho_token");

    assertThat(profile.email()).isEqualTo("67890+private@users.noreply.github.com");
  }

  @Test
  void anEmptyTokenIsRefusedBeforeAnyCall() {
    assertThatThrownBy(() -> verifier.verifyAndExtract("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null or empty");
    server.verify();
  }
}
