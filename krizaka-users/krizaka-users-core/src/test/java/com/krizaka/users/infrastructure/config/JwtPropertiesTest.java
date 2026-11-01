package com.krizaka.users.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  private static final String SECRET = "unit-test-identity-jwt-secret-256bit-key!";

  @Test
  @DisplayName("Valid properties are accepted")
  void validPropertiesAccepted() {
    var properties = new JwtProperties(SECRET, "krizaka-users", Duration.ofMinutes(30));

    assertThat(properties.issuer()).isEqualTo("krizaka-users");
  }

  @Test
  @DisplayName("A short secret is rejected (needs 256-bit HS256 material)")
  void shortSecretRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new JwtProperties("too-short", "iss", Duration.ofMinutes(30)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new JwtProperties(null, "iss", Duration.ofMinutes(30)));
  }

  @Test
  @DisplayName("Blank issuer and non-positive ttl are rejected")
  void blankIssuerAndBadTtlRejected() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new JwtProperties(SECRET, " ", Duration.ofMinutes(30)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new JwtProperties(SECRET, "iss", Duration.ZERO));
  }
}
