package com.krizaka.users.infrastructure.adapter.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krizaka.users.domain.model.ExtractedProfile;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AbstractProviderVerifierTest {

  private static class DummyProviderVerifier extends AbstractProviderVerifier {
    @Override
    protected String providerId() {
      return "dummy";
    }

    @Override
    protected String tokenLabel() {
      return "Dummy token";
    }

    @Override
    protected ExtractedProfile doVerifyAndExtract(String idToken) {
      return new ExtractedProfile("dummy-email", "dummy-sub", "Dummy User", "http://avatar");
    }
  }

  @Test
  void supports() {
    DummyProviderVerifier verifier = new DummyProviderVerifier();
    assertThat(verifier.supports("dummy")).isTrue();
    assertThat(verifier.supports("DUMMY")).isTrue();
    assertThat(verifier.supports("other")).isFalse();
  }

  @Test
  void verifyAndExtractValid() {
    DummyProviderVerifier verifier = new DummyProviderVerifier();
    ExtractedProfile profile = verifier.verifyAndExtract("valid-token");
    assertThat(profile).isNotNull();
    assertThat(profile.providerId()).isEqualTo("dummy-sub");
    assertThat(profile.email()).isEqualTo("dummy-email");
    assertThat(profile.name()).isEqualTo("Dummy User");
    assertThat(profile.avatarUrl()).isEqualTo("http://avatar");
  }

  @Test
  void verifyAndExtractInvalid() {
    DummyProviderVerifier verifier = new DummyProviderVerifier();
    assertThatThrownBy(() -> verifier.verifyAndExtract(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Dummy token cannot be null or empty");

    assertThatThrownBy(() -> verifier.verifyAndExtract("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Dummy token cannot be null or empty");
  }

  @Test
  void githubProviderVerifierSupports() {
    GithubProviderVerifier verifier =
        new GithubProviderVerifier(RestClient.create(), "id", "secret");
    assertThat(verifier.supports("github")).isTrue();
    assertThat(verifier.tokenLabel()).isEqualTo("GitHub access token");
  }

  @Test
  void googleProviderVerifierSupports() {
    GoogleProviderVerifier verifier =
        new GoogleProviderVerifier(GoogleProviderVerifier.googleDecoder("id"), "id");
    assertThat(verifier.supports("google")).isTrue();
    assertThat(verifier.tokenLabel()).isEqualTo("Google ID token");
  }
}
