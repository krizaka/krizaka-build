package com.krizaka.users.domain.ports.outbound;

/** Outbound port defining all secure password encoding and token hashing operations. */
public interface CryptographyPort {

  /**
   * Computes the secure SHA-256 hash of a verification token string.
   *
   * @param token Plaintext token.
   * @return The hex-encoded SHA-256 hash string.
   */
  String hashToken(String token);

  /**
   * Encodes a raw plaintext password using a secure hashing algorithm.
   *
   * @param password Plaintext password.
   * @return The secure password hash.
   */
  String encodePassword(String password);

  /**
   * Verifies if a raw password matches the encoded password hash.
   *
   * @param rawPassword Plaintext password.
   * @param encodedPassword Encoded password hash.
   * @return True if they match; false otherwise.
   */
  boolean matchesPassword(String rawPassword, String encodedPassword);

  /**
   * Whether a stored hash is weaker than what a new password gets today (legacy BCrypt, scrypt, a
   * lower Argon2 cost): the caller rehashes it after a successful sign-in.
   */
  boolean needsRehash(String encodedPassword);
}
