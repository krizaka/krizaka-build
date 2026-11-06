package com.krizaka.users.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krizaka.security.token.ServiceTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** ADR-001 slice 3, users half: per-caller ES256 service keys, HS256 behind a dated cutover. */
class ServiceKeysTest {

  private static final String SECRET = "unit-test-identity-jwt-secret-256bit-key!";
  private static final String PRIVATE = "PRIVATE KEY";
  private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  /** Keys are generated here, never committed. */
  private static KeyPair newKeyPair() throws Exception {
    var generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    return generator.generateKeyPair();
  }

  private static String publicPem(KeyPair pair) {
    return "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(pair.getPublic().getEncoded())
        + "\n-----END PUBLIC KEY-----";
  }

  private static String serviceToken(KeyPair pair, String kid, String sub, Clock clock) {
    return ServiceTokenProvider.es256((ECPrivateKey) pair.getPrivate(), kid, sub, clock).token();
  }

  private static JwtProperties props(
      KeyPair issuer, Map<String, String> callers, String secret, Instant cutoff) {
    var signing =
        new JwtProperties.Signing(
            "-----BEGIN "
                + PRIVATE
                + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                    .encodeToString(issuer.getPrivate().getEncoded())
                + "\n-----END "
                + PRIVATE
                + "-----",
            publicPem(issuer),
            "issuer-1",
            Map.of());
    return new JwtProperties(
        secret, "https://id.test", Duration.ofMinutes(5), signing, cutoff, callers);
  }

  private static JwtDecoder decoder(JwtProperties p) {
    var config = new JwtConfig();
    return config.identityJwtDecoder(p, config.issuerKeys(p), CLOCK);
  }

  @Test
  @DisplayName("A caller signed with its own key is accepted as SERVICE, by its name")
  void callerWithItsOwnKey() throws Exception {
    var billing = newKeyPair();
    var d = decoder(props(newKeyPair(), Map.of("billing", publicPem(billing)), null, null));

    var jwt = d.decode(serviceToken(billing, "billing-1", "billing", CLOCK));

    assertThat(jwt.getSubject()).isEqualTo("billing");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("SERVICE");
  }

  @Test
  @DisplayName("Another key under a registered name, an unknown caller, tampering: all refused")
  void wrongKeyUnknownCallerTampered() throws Exception {
    var billing = newKeyPair();
    var intruder = newKeyPair();
    var d = decoder(props(newKeyPair(), Map.of("billing", publicPem(billing)), null, null));

    assertThatThrownBy(() -> d.decode(serviceToken(intruder, "billing-1", "billing", CLOCK)))
        .isInstanceOf(BadJwtException.class);
    assertThatThrownBy(() -> d.decode(serviceToken(intruder, "x-1", "intruder", CLOCK)))
        .isInstanceOf(BadJwtException.class);

    String good = serviceToken(billing, "billing-1", "billing", CLOCK);
    String[] parts = good.split("\\.");
    String payload =
        new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            .replace("billing", "studio");
    String tampered =
        parts[0]
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
            + "."
            + parts[2];
    assertThatThrownBy(() -> d.decode(tampered)).isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("An expired service token is refused")
  void expired() throws Exception {
    var billing = newKeyPair();
    var d = decoder(props(newKeyPair(), Map.of("billing", publicPem(billing)), null, null));
    var past = Clock.fixed(NOW.minusSeconds(3600), ZoneOffset.UTC);

    assertThatThrownBy(() -> d.decode(serviceToken(billing, "billing-1", "billing", past)))
        .isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("Algorithm confusion: alg none and HS256 keyed with a public PEM are refused")
  void algorithmConfusion() throws Exception {
    var billing = newKeyPair();
    var d = decoder(props(newKeyPair(), Map.of("billing", publicPem(billing)), null, null));
    var enc = Base64.getUrlEncoder().withoutPadding();
    String none =
        enc.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8))
            + "."
            + enc.encodeToString(
                ("{\"sub\":\"billing\",\"roles\":[\"SERVICE\"],\"exp\":"
                        + (NOW.getEpochSecond() + 300)
                        + "}")
                    .getBytes(StandardCharsets.UTF_8))
            + ".";
    // The public PEM is public: an attacker can use its text as an HMAC secret.
    String confused = new ServiceTokenProvider(publicPem(billing), "billing", CLOCK).token();

    assertThatThrownBy(() -> d.decode(none)).isInstanceOf(BadJwtException.class);
    assertThatThrownBy(() -> d.decode(confused)).isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("A caller key signs SERVICE tokens only: it cannot mint a user session")
  void callerCannotMintASession() throws Exception {
    var billing = newKeyPair();
    var d = decoder(props(newKeyPair(), Map.of("billing", publicPem(billing)), null, null));
    var signer = new com.nimbusds.jose.crypto.ECDSASigner((ECPrivateKey) billing.getPrivate());
    var jws =
        new com.nimbusds.jwt.SignedJWT(
            new com.nimbusds.jose.JWSHeader.Builder(com.nimbusds.jose.JWSAlgorithm.ES256)
                .keyID("billing-1")
                .build(),
            new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .subject("billing")
                .claim("roles", java.util.List.of("ROLE_ADMIN"))
                .expirationTime(java.util.Date.from(NOW.plusSeconds(300)))
                .build());
    jws.sign(signer);

    assertThatThrownBy(() -> d.decode(jws.serialize())).isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("HS256 verifies before the dated cutover and nothing after it")
  void hs256CutoverIsDated() throws Exception {
    var issuer = newKeyPair();
    String hs = new ServiceTokenProvider(SECRET, "legacy", CLOCK).token();
    var before = props(issuer, Map.of(), SECRET, NOW.plusSeconds(86_400));
    var after = props(issuer, Map.of(), SECRET, NOW.minusSeconds(1));

    assertThat(decoder(before).decode(hs).getSubject()).isEqualTo("legacy");
    assertThatThrownBy(() -> decoder(after).decode(hs)).isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("Without the secret configured, HS256 is refused outright")
  void noSecretNoHs256() throws Exception {
    var d = decoder(props(newKeyPair(), Map.of(), null, null));

    assertThatThrownBy(() -> d.decode(new ServiceTokenProvider(SECRET, "legacy", CLOCK).token()))
        .isInstanceOf(BadJwtException.class);
  }

  @Test
  @DisplayName("A caller key that is not a P-256 public PEM fails at startup, without the value")
  void badCallerKeyFailsAtStartup() throws Exception {
    var p = props(newKeyPair(), Map.of("billing", "not-a-key-AAAA"), null, null);

    assertThatThrownBy(() -> decoder(p))
        .isInstanceOf(IllegalArgumentException.class)
        .message()
        .doesNotContain("AAAA");
  }
}
