package com.krizaka.users.infrastructure.adapter.crypto;

import com.krizaka.users.domain.ports.inbound.PasswordPolicyService;
import com.krizaka.users.domain.ports.outbound.CryptographyPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Concrete outbound infrastructure adapter implementing {@link CryptographyPort}. Wraps {@link
 * PasswordEncoder} and standard SHA-256 digesting.
 */
@Component
class SpringSecurityCryptographyAdapter implements CryptographyPort {

  private final PasswordEncoder passwordEncoder;

  SpringSecurityCryptographyAdapter(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to hash token: SHA-256 digest unavailable", e);
    }
  }

  @Override
  public String encodePassword(String password) {
    return passwordEncoder.encode(password);
  }

  @Override
  public boolean matchesPassword(String rawPassword, String encodedPassword) {
    // Passwords are hashed in NFKC; a hash made from the raw form before that rule still matches.
    String normalised = PasswordPolicyService.normalise(rawPassword);
    return matchesSafely(normalised, encodedPassword)
        || (!normalised.equals(rawPassword) && matchesSafely(rawPassword, encodedPassword));
  }

  private boolean matchesSafely(String password, String encodedPassword) {
    try {
      return passwordEncoder.matches(password, encodedPassword);
    } catch (IllegalArgumentException e) {
      // e.g. BCrypt refuses a password over 72 bytes: it cannot be the one that was hashed
      return false;
    }
  }

  @Override
  public boolean needsRehash(String encodedPassword) {
    return passwordEncoder.upgradeEncoding(encodedPassword);
  }
}
