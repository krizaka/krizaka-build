package com.krizaka.users.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** ADR-001: the client signs with its own ES256 key; the shared secret is the legacy path. */
class ServiceKeyClientTest {

  private static String generatedPrivatePem() throws Exception {
    var generator = KeyPairGenerator.getInstance("EC");
    generator.initialize(new ECGenParameterSpec("secp256r1"));
    String type = "PRIVATE KEY";
    return "-----BEGIN "
        + type
        + "-----\n"
        + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
            .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
        + "\n-----END "
        + type
        + "-----";
  }

  @Test
  void aServiceKeySignsEs256WithItsKid() throws Exception {
    var props =
        new UsersClientProperties(
            "http://u", generatedPrivatePem(), "billing-1", null, "billing", Duration.ZERO);

    String[] parts = HttpUserDirectoryClient.tokensOf(props).token().split("\\.");
    String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
    String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

    assertThat(header).contains("\"alg\":\"ES256\"").contains("\"kid\":\"billing-1\"");
    assertThat(claims).contains("\"sub\":\"billing\"");
  }

  @Test
  void exactlyOneOfKeyAndSecretIsRequired() throws Exception {
    assertThatThrownBy(
            () -> new UsersClientProperties("http://u", null, null, null, "billing", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new UsersClientProperties(
                    "http://u",
                    generatedPrivatePem(),
                    "k",
                    "krizaka-test-secret-at-least-32-characters!",
                    "billing",
                    Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new UsersClientProperties(
                    "http://u", generatedPrivatePem(), null, null, "billing", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void anInvalidKeyFailsWithoutPrintingIt() {
    var props =
        new UsersClientProperties(
            "http://u", "SECRETMATERIAL", "k", null, "billing", Duration.ZERO);

    assertThatThrownBy(() -> HttpUserDirectoryClient.tokensOf(props))
        .isInstanceOf(IllegalArgumentException.class)
        .message()
        .doesNotContain("SECRETMATERIAL");
  }

  @Test
  void toStringRedactsTheKey() throws Exception {
    String pem = generatedPrivatePem();
    var props = new UsersClientProperties("http://u", pem, "k", null, "billing", Duration.ZERO);

    assertThat(props.toString()).doesNotContain(pem.substring(30, 70));
  }
}
