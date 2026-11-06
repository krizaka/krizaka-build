package com.krizaka.users.infrastructure.adapter.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.users.infrastructure.config.IdentityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * The real encoder chain (ADR-016 decision 2): Argon2id for new passwords, legacy hashes still
 * verified and flagged.
 */
class PasswordHashingTest {

  private final SpringSecurityCryptographyAdapter crypto =
      new SpringSecurityCryptographyAdapter(new IdentityConfig().passwordEncoder());

  @Test
  void newPasswordsAreArgon2idAndNeedNoRehash() {
    String hash = crypto.encodePassword("correct horse battery staple");

    assertThat(hash).startsWith("{argon2}");
    assertThat(crypto.matchesPassword("correct horse battery staple", hash)).isTrue();
    assertThat(crypto.matchesPassword("another horse battery staple", hash)).isFalse();
    assertThat(crypto.needsRehash(hash)).isFalse();
  }

  @Test
  void aLegacyBcryptHashStillSignsInAndIsFlaggedForRehash() {
    String legacy = new BCryptPasswordEncoder().encode("old-password-123");

    assertThat(legacy).startsWith("$2");
    assertThat(crypto.matchesPassword("old-password-123", legacy)).isTrue();
    assertThat(crypto.matchesPassword("wrong", legacy)).isFalse();
    assertThat(crypto.needsRehash(legacy)).isTrue();
  }

  @Test
  void beyondSeventyTwoBytesEveryByteCounts() {
    // BCrypt keeps only the first 72 bytes: two long passwords differing after them were the same
    // password
    String common = "x".repeat(80);
    String hash = crypto.encodePassword(common + "-one");

    assertThat(crypto.matchesPassword(common + "-one", hash)).isTrue();
    assertThat(crypto.matchesPassword(common + "-two", hash)).isFalse();
  }

  @Test
  void anOverlongPasswordAgainstALegacyBcryptHashDoesNotThrow() {
    String legacy = new BCryptPasswordEncoder().encode("short-legacy");

    assertThat(crypto.matchesPassword("y".repeat(300), legacy)).isFalse();
  }

  @Test
  void aCompatibilityFormMatchesTheNormalisedHash() {
    String hash = crypto.encodePassword("password-password-1"); // NFKC form
    assertThat(crypto.matchesPassword("ｐａｓｓｗｏｒｄ－ｐａｓｓｗｏｒｄ－１", hash)).isTrue();
  }
}
