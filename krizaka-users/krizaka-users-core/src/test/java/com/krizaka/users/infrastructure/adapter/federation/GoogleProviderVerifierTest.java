package com.krizaka.users.infrastructure.adapter.federation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krizaka.users.domain.model.ExtractedProfile;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Real RS256 tokens signed with a throw-away key: the verifier checks the signature locally (no
 * tokeninfo round trip — krizaka-users#42), so a forged or altered token is refused for the right
 * reason.
 */
class GoogleProviderVerifierTest {

  private static final String CLIENT_ID = "1234-krizaka.apps.googleusercontent.com";

  private RSAPrivateKey signingKey;
  private GoogleProviderVerifier verifier;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    signingKey = (RSAPrivateKey) pair.getPrivate();
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withPublicKey((RSAPublicKey) pair.getPublic()).build();
    decoder.setJwtValidator(GoogleProviderVerifier.validator(CLIENT_ID));
    verifier = new GoogleProviderVerifier(decoder, CLIENT_ID);
  }

  @Test
  void supports_google_returnsTrue() {
    assertThat(verifier.supports("google")).isTrue();
    assertThat(verifier.supports("GOOGLE")).isTrue();
  }

  @Test
  void supports_other_returnsFalse() {
    assertThat(verifier.supports("github")).isFalse();
  }

  @Test
  void tokenLabel_returnsGoogleIdToken() {
    assertThat(verifier.tokenLabel()).isEqualTo("Google ID token");
  }

  @Test
  void validToken_returnsExtractedProfile() throws Exception {
    ExtractedProfile profile = verifier.doVerifyAndExtract(token(claims().build(), signingKey));

    assertThat(profile.email()).isEqualTo("user@gmail.com");
    assertThat(profile.providerId()).isEqualTo("google-sub-123");
    assertThat(profile.name()).isEqualTo("Test User");
    assertThat(profile.avatarUrl()).isEqualTo("https://lh3.googleusercontent.com/photo");
  }

  @Test
  void httpsIssuer_succeeds() throws Exception {
    JWTClaimsSet claims = claims().issuer("https://accounts.google.com").build();

    assertThat(verifier.doVerifyAndExtract(token(claims, signingKey)).email())
        .isEqualTo("user@gmail.com");
  }

  @Test
  void aTokenSignedWithAnotherKeyIsRefused() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    RSAPrivateKey forger = (RSAPrivateKey) generator.generateKeyPair().getPrivate();

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(token(claims().build(), forger)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token verification failed");
  }

  @Test
  void invalidIssuer_isRefused() throws Exception {
    String jwt = token(claims().issuer("evil.com").build(), signingKey);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid Google token issuer");
  }

  @Test
  void expiredToken_isRefused() throws Exception {
    String jwt =
        token(
            claims().expirationTime(Date.from(Instant.now().minusSeconds(3600))).build(),
            signingKey);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("expired");
  }

  // Finding 2 (ADR-001): a Google ID token issued to another application must be refused.
  @Test
  void tokenForAnotherApplication_isRefused() throws Exception {
    String jwt =
        token(
            claims().audience("someone-elses-app.apps.googleusercontent.com").build(), signingKey);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("audience");
  }

  @Test
  void unverifiedEmail_isRefused() throws Exception {
    String jwt = token(claims().claim("email_verified", false).build(), signingKey);

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(jwt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("verified");
  }

  @Test
  void missingSub_isRefused() throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer("accounts.google.com")
            .audience(CLIENT_ID)
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("email", "user@gmail.com")
            .claim("email_verified", true)
            .build();

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(token(claims, signingKey)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing required field: Google user ID");
  }

  @Test
  void missingEmail_isRefused() throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer("accounts.google.com")
            .audience(CLIENT_ID)
            .subject("google-sub-123")
            .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
            .claim("email_verified", true)
            .build();

    assertThatThrownBy(() -> verifier.doVerifyAndExtract(token(claims, signingKey)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing required field: email");
  }

  @Test
  void notAJwt_isRefused() {
    assertThatThrownBy(() -> verifier.doVerifyAndExtract("not-a-jwt"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token verification failed");
  }

  @Test
  void verifyAndExtract_nullToken_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.verifyAndExtract(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token cannot be null or empty");
  }

  @Test
  void verifyAndExtract_blankToken_throwsIllegalArgument() {
    assertThatThrownBy(() -> verifier.verifyAndExtract("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Google ID token cannot be null or empty");
  }

  private static JWTClaimsSet.Builder claims() {
    return new JWTClaimsSet.Builder()
        .issuer("accounts.google.com")
        .audience(List.of(CLIENT_ID))
        .subject("google-sub-123")
        .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
        .claim("email", "user@gmail.com")
        .claim("email_verified", true)
        .claim("name", "Test User")
        .claim("picture", "https://lh3.googleusercontent.com/photo");
  }

  private static String token(JWTClaimsSet claims, RSAPrivateKey key) throws JOSEException {
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    jwt.sign(new RSASSASigner(key));
    return jwt.serialize();
  }
}
