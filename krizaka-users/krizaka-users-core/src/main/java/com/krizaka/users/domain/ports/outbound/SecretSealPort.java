package com.krizaka.users.domain.ports.outbound;

/**
 * Seals the identity secrets before they reach the database (ADR-016 decision 6): envelope
 * encryption, {@code v2.<key_id>.<wrapped data key>.<sealed secret>}, the same format as krizaka-ts
 * ADR-005. Rotating the key-encryption key re-wraps the data key only.
 */
public interface SecretSealPort {

  /** Whether a vault key is configured; without one, sealing refuses and nothing is stored. */
  boolean available();

  /** The key id new values are wrapped under. */
  String currentKeyId();

  /**
   * Seals {@code secret} under a fresh data key.
   *
   * @throws com.krizaka.users.domain.exception.ConfigurationException when no vault key is set
   */
  String seal(String secret);

  /**
   * Opens a sealed value: the one place identity secrets are read back.
   *
   * @throws com.krizaka.users.domain.exception.SecurityGuardrailException when it is unreadable
   */
  String open(String sealed);

  /** The key id a sealed value is wrapped under. */
  String keyIdOf(String sealed);

  /** Re-wraps the data key of {@code sealed} under the current key; the secret is untouched. */
  String rewrap(String sealed);
}
