package com.krizaka.users.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtConfigTest {

  @Test
  @DisplayName("Encoder and decoder are a matching HS256 pair over the shared secret")
  void encoderDecoderRoundTrip() {
    var properties =
        new JwtProperties(
            "unit-test-identity-jwt-secret-256bit-key!", "krizaka-users", Duration.ofMinutes(5));
    var config = new JwtConfig();

    String token =
        config
            .identityJwtEncoder(properties, IssuerKeys.none())
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    JwtClaimsSet.builder().subject("user-1").build()))
            .getTokenValue();
    Jwt decoded = config.identityJwtDecoder(properties, IssuerKeys.none()).decode(token);

    assertThat(decoded.getSubject()).isEqualTo("user-1");
  }
}
