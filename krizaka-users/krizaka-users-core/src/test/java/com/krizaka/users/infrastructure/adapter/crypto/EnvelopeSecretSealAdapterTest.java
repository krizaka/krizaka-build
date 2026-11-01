package com.krizaka.users.infrastructure.adapter.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krizaka.users.domain.exception.ConfigurationException;
import com.krizaka.users.domain.exception.SecurityGuardrailException;
import com.krizaka.users.infrastructure.config.VaultProperties;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The envelope vault (ADR-016 decision 6): the same format as {@code @krizaka/storage}. */
class EnvelopeSecretSealAdapterTest {

  /** 32 bytes of 0x07. */
  private static final String KEK = "BwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwcHBwc=";

  /** Produced by {@code @krizaka/storage}'s vault algorithm (krizaka-ts ADR-005, Node crypto). */
  private static final String TS_VECTOR =
      "v2.k1.lWydbTr4QYkkTPt-7E--Y5eKM_eGc8RQdOM1BUIhFs70wPY7ysK3jHcdxkdIDGkLVBYnxhJUKu-McCCz."
          + "MgnfMfgwtrXQ_Ik1NIwkz33GeR6LSH8Q0GPL-2AZJ7CLpVWimJ_pnyxBA3U";

  private static String otherKey() {
    byte[] k = new byte[32];
    java.util.Arrays.fill(k, (byte) 9);
    return Base64.getEncoder().encodeToString(k);
  }

  private static EnvelopeSecretSealAdapter vault(String key, String id, Map<String, String> old) {
    return new EnvelopeSecretSealAdapter(new VaultProperties(key, id, old));
  }

  @Test
  void opensAValueSealedByTheTypeScriptVault() {
    assertThat(vault(KEK, "k1", Map.of()).open(TS_VECTOR)).isEqualTo("s3cr3t-Ünïcode");
  }

  @Test
  void sealsInTheEnvelopeFormatAndRoundTrips() {
    var v = vault(KEK, "k1", Map.of());
    String sealed = v.seal("client-secret");
    assertThat(sealed).startsWith("v2.k1.").doesNotContain("client-secret");
    assertThat(sealed.split("\\.")).hasSize(4);
    assertThat(v.open(sealed)).isEqualTo("client-secret");
    assertThat(v.keyIdOf(sealed)).isEqualTo("k1");
  }

  @Test
  void aSealedValueIsDifferentEveryTime() {
    var v = vault(KEK, "k1", Map.of());
    assertThat(v.seal("same")).isNotEqualTo(v.seal("same"));
  }

  @Test
  void aTamperedValueIsRefused() {
    var v = vault(KEK, "k1", Map.of());
    String sealed = v.seal("client-secret");
    String tampered =
        sealed.substring(0, sealed.length() - 2) + (sealed.endsWith("AA") ? "BB" : "AA");
    assertThatThrownBy(() -> v.open(tampered)).isInstanceOf(SecurityGuardrailException.class);
    assertThatThrownBy(() -> v.open("v1.abc")).isInstanceOf(SecurityGuardrailException.class);
    assertThatThrownBy(() -> v.open(null)).isInstanceOf(SecurityGuardrailException.class);
  }

  @Test
  void rotationRewrapsTheDataKeyOnly() {
    var before = vault(KEK, "k1", Map.of());
    String sealed = before.seal("client-secret");
    var after = vault(otherKey(), "k2", Map.of("k1", KEK));
    String rewrapped = after.rewrap(sealed);
    assertThat(after.keyIdOf(rewrapped)).isEqualTo("k2");
    assertThat(rewrapped.split("\\.")[3]).isEqualTo(sealed.split("\\.")[3]);
    assertThat(after.open(rewrapped)).isEqualTo("client-secret");
    // the retired key is no longer needed to read it
    assertThat(vault(otherKey(), "k2", Map.of()).open(rewrapped)).isEqualTo("client-secret");
    // and the old value still opens while the retired key is kept
    assertThat(after.open(sealed)).isEqualTo("client-secret");
  }

  @Test
  void aValueWrappedUnderALostKeyIsUnreadable() {
    String sealed = vault(KEK, "k1", Map.of()).seal("client-secret");
    assertThatThrownBy(() -> vault(otherKey(), "k2", Map.of()).open(sealed))
        .isInstanceOf(SecurityGuardrailException.class)
        .hasMessageNotContaining("client-secret");
  }

  @Test
  void withoutAKeyNothingIsSealed() {
    var v = vault("", "k1", Map.of());
    assertThat(v.available()).isFalse();
    assertThatThrownBy(() -> v.seal("x"))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageContaining("AUTH_VAULT_KEY");
  }

  @Test
  void aBadKeyFailsTheStartWithoutPrintingIt() {
    assertThatThrownBy(() -> vault("c2hvcnQ=", "k1", Map.of()))
        .isInstanceOf(ConfigurationException.class)
        .hasMessageNotContaining("c2hvcnQ=");
    assertThatThrownBy(() -> vault(KEK, "bad id!", Map.of()))
        .isInstanceOf(ConfigurationException.class);
  }

  @Test
  void thePropertiesNeverPrintTheKey() {
    assertThat(new VaultProperties(KEK, "k1", Map.of("k0", KEK)).toString()).doesNotContain(KEK);
  }
}
