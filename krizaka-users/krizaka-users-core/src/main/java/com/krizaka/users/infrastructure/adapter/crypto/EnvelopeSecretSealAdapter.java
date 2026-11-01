package com.krizaka.users.infrastructure.adapter.crypto;

import com.krizaka.users.domain.exception.ConfigurationException;
import com.krizaka.users.domain.exception.SecurityGuardrailException;
import com.krizaka.users.domain.ports.outbound.SecretSealPort;
import com.krizaka.users.infrastructure.config.VaultProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Envelope encryption of the identity secrets: AES-256-GCM, a fresh data key per value wrapped by
 * the key-encryption key named in the envelope, {@code v2.<key_id>.<wrapped data key>.<sealed
 * secret>} with each part {@code base64url(iv | tag | ciphertext)} — byte for byte the format of
 * {@code @krizaka/storage}'s vault (krizaka-ts ADR-005), so both languages open each other's
 * values. A value is opened only here; keys and plaintext never reach a log or an exception
 * message.
 */
@Component
@EnableConfigurationProperties(VaultProperties.class)
class EnvelopeSecretSealAdapter implements SecretSealPort {

  private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");
  private static final String BODY_AAD = "body";
  private static final int KEY_BYTES = 32;
  private static final int IV_BYTES = 12;
  private static final int TAG_BYTES = 16;

  private final SecureRandom random = new SecureRandom();
  private final String currentId;
  private final Map<String, byte[]> ring = new HashMap<>();

  EnvelopeSecretSealAdapter(VaultProperties properties) {
    this.currentId = properties.keyId();
    if (!KEY_ID.matcher(currentId).matches()) {
      throw new ConfigurationException("The vault key id is 1 to 32 letters, digits, - or _");
    }
    properties.retired().forEach((id, key) -> ring.put(id, parse(key, "A retired vault key")));
    if (!properties.key().isEmpty()) {
      ring.put(currentId, parse(properties.key(), "AUTH_VAULT_KEY"));
    }
  }

  private static byte[] parse(String base64, String what) {
    byte[] key;
    try {
      key = Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      throw new ConfigurationException(what + " must be 32 bytes in base64");
    }
    if (key.length != KEY_BYTES) {
      throw new ConfigurationException(what + " must be 32 bytes in base64");
    }
    return key;
  }

  @Override
  public boolean available() {
    return ring.containsKey(currentId);
  }

  @Override
  public String currentKeyId() {
    return currentId;
  }

  @Override
  public String seal(String secret) {
    byte[] kek = currentKey();
    byte[] dataKey = new byte[KEY_BYTES];
    random.nextBytes(dataKey);
    return "v2."
        + currentId
        + "."
        + gcm(dataKey, kek, currentId)
        + "."
        + gcm(secret.getBytes(StandardCharsets.UTF_8), dataKey, BODY_AAD);
  }

  @Override
  public String open(String sealed) {
    Parts p = split(sealed);
    return new String(ungcm(p.body(), unwrap(p), BODY_AAD), StandardCharsets.UTF_8);
  }

  @Override
  public String keyIdOf(String sealed) {
    return split(sealed).keyId();
  }

  @Override
  public String rewrap(String sealed) {
    byte[] kek = currentKey();
    Parts p = split(sealed);
    return "v2." + currentId + "." + gcm(unwrap(p), kek, currentId) + "." + p.body();
  }

  private byte[] currentKey() {
    byte[] kek = ring.get(currentId);
    if (kek == null) {
      throw new ConfigurationException(
          "AUTH_VAULT_KEY is not set: identity secrets are not sealed");
    }
    return kek;
  }

  private record Parts(String keyId, String wrapped, String body) {}

  private static Parts split(String sealed) {
    String[] parts = sealed == null ? new String[0] : sealed.split("\\.");
    if (parts.length != 4 || !"v2".equals(parts[0]) || !KEY_ID.matcher(parts[1]).matches()) {
      throw unreadable();
    }
    return new Parts(parts[1], parts[2], parts[3]);
  }

  private byte[] unwrap(Parts p) {
    byte[] kek = ring.get(p.keyId());
    if (kek == null) {
      throw new SecurityGuardrailException("No vault key " + p.keyId() + " to open the value");
    }
    return ungcm(p.wrapped(), kek, p.keyId());
  }

  private static SecurityGuardrailException unreadable() {
    return new SecurityGuardrailException("Unreadable sealed value");
  }

  private String gcm(byte[] plain, byte[] key, String aad) {
    byte[] iv = new byte[IV_BYTES];
    random.nextBytes(iv);
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(TAG_BYTES * 8, iv));
      cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
      byte[] out = cipher.doFinal(plain); // ciphertext | tag
      int cut = out.length - TAG_BYTES;
      byte[] packed = new byte[IV_BYTES + out.length];
      System.arraycopy(iv, 0, packed, 0, IV_BYTES);
      System.arraycopy(out, cut, packed, IV_BYTES, TAG_BYTES); // iv | tag | ciphertext
      System.arraycopy(out, 0, packed, IV_BYTES + TAG_BYTES, cut);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(packed);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("The vault could not seal a value", e);
    }
  }

  private static byte[] ungcm(String packed, byte[] key, String aad) {
    try {
      byte[] raw = Base64.getUrlDecoder().decode(packed);
      if (raw.length < IV_BYTES + TAG_BYTES) {
        throw unreadable();
      }
      byte[] iv = Arrays.copyOfRange(raw, 0, IV_BYTES);
      byte[] tag = Arrays.copyOfRange(raw, IV_BYTES, IV_BYTES + TAG_BYTES);
      byte[] body = Arrays.copyOfRange(raw, IV_BYTES + TAG_BYTES, raw.length);
      byte[] joined = new byte[body.length + TAG_BYTES];
      System.arraycopy(body, 0, joined, 0, body.length);
      System.arraycopy(tag, 0, joined, body.length, TAG_BYTES);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(TAG_BYTES * 8, iv));
      cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
      return cipher.doFinal(joined);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new SecurityGuardrailException("The sealed value failed its integrity check");
    }
  }
}
