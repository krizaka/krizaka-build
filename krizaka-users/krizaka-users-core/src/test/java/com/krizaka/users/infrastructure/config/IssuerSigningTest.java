package com.krizaka.users.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krizaka.users.infrastructure.adapter.rest.JwksController;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Slice 2 of ADR-001: the issuer signs ES256, publishes the JWKS, rotates by kid. */
class IssuerSigningTest {

  private static final String SECRET = "unit-test-identity-jwt-secret-256bit-key!";
  private static final String ISSUER = "https://id.test";

  /** Keys are generated here, never committed. */
  private static KeyPair newKeyPair() throws Exception {
    var generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    return generator.generateKeyPair();
  }

  private static String pem(String type, byte[] der) {
    return "-----BEGIN "
        + type
        + "-----\n"
        + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
        + "\n-----END "
        + type
        + "-----";
  }

  private static String publicPem(KeyPair pair) {
    return pem("PUBLIC KEY", pair.getPublic().getEncoded());
  }

  private static JwtProperties.Signing signing(KeyPair pair, String kid, Map<String, String> old) {
    return new JwtProperties.Signing(
        pem("PRIVATE KEY", pair.getPrivate().getEncoded()), publicPem(pair), kid, old);
  }

  private static JwtProperties props(JwtProperties.Signing signing) {
    return new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(5), signing);
  }

  private static String mint(JwtEncoder encoder, JwsAlgorithm alg, String kid) {
    var header = JwsHeader.with(alg);
    if (kid != null) {
      header.keyId(kid);
    }
    var claims = JwtClaimsSet.builder().issuer(ISSUER).subject("u1").build();
    return encoder.encode(JwtEncoderParameters.from(header.build(), claims)).getTokenValue();
  }

  private final JwtConfig config = new JwtConfig();

  private JwtEncoder encoder(JwtProperties p) {
    return config.identityJwtEncoder(p, config.issuerKeys(p));
  }

  @Test
  @DisplayName("Tokens are ES256 with the kid in the header and verify from the published JWKS")
  void signsEs256AndVerifiesFromPublishedJwks() throws Exception {
    var p = props(signing(newKeyPair(), "k1", Map.of()));

    String token = mint(encoder(p), SignatureAlgorithm.ES256, "k1");

    Jwt jwt = config.identityJwtDecoder(p, config.issuerKeys(p)).decode(token);
    assertThat(jwt.getHeaders()).containsEntry("alg", "ES256").containsEntry("kid", "k1");
    assertThat(jwt.getIssuer()).hasToString(ISSUER);
  }

  @Test
  @DisplayName("The JWKS holds public keys only: current and retired kids, no private member")
  void jwksPublishesPublicKeysOfCurrentAndRetired() throws Exception {
    var retired = newKeyPair();
    var p = props(signing(newKeyPair(), "k2", Map.of("k1", publicPem(retired))));

    var body = new JwksController(config.issuerKeys(p)).jwks().getBody();

    var set = JWKSet.parse(new com.nimbusds.jose.shaded.gson.Gson().toJson(body));
    assertThat(set.getKeys()).extracting(JWK::getKeyID).containsExactlyInAnyOrder("k1", "k2");
    assertThat(set.getKeys()).allSatisfy(k -> assertThat(k.isPrivate()).isFalse());
    assertThat(String.valueOf(body)).doesNotContain("\"d\"").doesNotContain("PRIVATE");
  }

  @Test
  @DisplayName(
      "Rotation: a token signed under the old kid still verifies; new ones carry the new kid")
  void rotationKeepsOldKidVerifiable() throws Exception {
    var oldPair = newKeyPair();
    var before = props(signing(oldPair, "k1", Map.of()));
    String oldToken = mint(encoder(before), SignatureAlgorithm.ES256, "k1");

    var after = props(signing(newKeyPair(), "k2", Map.of("k1", publicPem(oldPair))));
    var decoder = config.identityJwtDecoder(after, config.issuerKeys(after));
    String newToken = mint(encoder(after), SignatureAlgorithm.ES256, "k2");

    assertThat(decoder.decode(oldToken).getHeaders()).containsEntry("kid", "k1");
    assertThat(decoder.decode(newToken).getHeaders()).containsEntry("kid", "k2");
  }

  @Test
  @DisplayName("A kid the issuer never published, or a foreign key under a known kid, is rejected")
  void foreignKeyIsRejected() throws Exception {
    var p = props(signing(newKeyPair(), "k1", Map.of()));
    var decoder = config.identityJwtDecoder(p, config.issuerKeys(p));
    var foreign = props(signing(newKeyPair(), "k1", Map.of()));
    var unknown = props(signing(newKeyPair(), "kx", Map.of()));

    assertThatThrownBy(() -> decoder.decode(mint(encoder(foreign), SignatureAlgorithm.ES256, "k1")))
        .isInstanceOf(BadJwtException.class);
    assertThatThrownBy(() -> decoder.decode(mint(encoder(unknown), SignatureAlgorithm.ES256, "kx")))
        .isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("Until slice 3, HS256 service tokens still verify beside ES256 sessions")
  void hs256ServiceTokensStillVerify() throws Exception {
    var p = props(signing(newKeyPair(), "k1", Map.of()));
    var hmac =
        new NimbusJwtEncoder(
            new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

    Jwt jwt =
        config
            .identityJwtDecoder(p, config.issuerKeys(p))
            .decode(mint(hmac, MacAlgorithm.HS256, null));

    assertThat(jwt.getSubject()).isEqualTo("u1");
  }

  @Test
  @DisplayName("Without a signing key the issuer stays HS256 and publishes an empty JWKS")
  void legacyModeHasEmptyJwks() {
    var p = new JwtProperties(SECRET, ISSUER, Duration.ofMinutes(5));

    assertThat(config.issuerKeys(p).enabled()).isFalse();
    assertThat(new JwksController(config.issuerKeys(p)).jwks().getBody())
        .containsEntry("keys", java.util.List.of());
  }

  @Test
  @DisplayName("A private key that does not match its public key fails at startup, key unprinted")
  void mismatchedPairFailsWithoutLeakingKey() throws Exception {
    var a = newKeyPair();
    var b = newKeyPair();
    var bad =
        new JwtProperties.Signing(
            pem("PRIVATE KEY", a.getPrivate().getEncoded()), publicPem(b), "k1", Map.of());

    assertThatThrownBy(() -> IssuerKeys.from(bad))
        .isInstanceOf(IllegalArgumentException.class)
        .message()
        .doesNotContain("PRIVATE")
        .doesNotContain(Base64.getEncoder().encodeToString(a.getPrivate().getEncoded()));
  }

  @Test
  @DisplayName("The signing key never appears in toString (logs, actuator, error messages)")
  void privateKeyIsRedacted() throws Exception {
    var pair = newKeyPair();
    var s = signing(pair, "k1", Map.of());
    String b64 = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

    assertThat(s.toString()).doesNotContain("PRIVATE").doesNotContain(b64.substring(0, 40));
    assertThat(props(s).toString()).doesNotContain(SECRET).doesNotContain(b64.substring(0, 40));
  }
}
