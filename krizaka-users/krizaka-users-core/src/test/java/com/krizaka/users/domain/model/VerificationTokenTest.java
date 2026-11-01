package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class VerificationTokenTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void validConstruction_setsAllFields() {
    Instant expiry = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var token = new VerificationToken("id-1", "user-1", "EMAIL_VERIFY", "hash", expiry, false);
    assertEquals("id-1", token.id());
    assertEquals("user-1", token.userId());
    assertEquals("EMAIL_VERIFY", token.tokenType());
    assertEquals("hash", token.tokenHash());
    assertEquals(expiry, token.expiryTimestamp());
    assertFalse(token.used());
  }

  @Test
  void nullId_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(
        NullPointerException.class,
        () -> new VerificationToken(null, "user", "type", "hash", now, false));
  }

  @Test
  void nullUserId_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(
        NullPointerException.class,
        () -> new VerificationToken("id", null, "type", "hash", now, false));
  }

  @Test
  void nullTokenType_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(
        NullPointerException.class,
        () -> new VerificationToken("id", "user", null, "hash", now, false));
  }

  @Test
  void nullTokenHash_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(
        NullPointerException.class,
        () -> new VerificationToken("id", "user", "type", null, now, false));
  }

  @Test
  void nullExpiryTimestamp_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new VerificationToken("id", "user", "type", "hash", null, false));
  }
}
