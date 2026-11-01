package com.krizaka.users.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RateLimitTierDtoTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void validConstruction_setsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto = new RateLimitTierDto("free", 100, 10, 60, now);
    assertEquals("free", dto.id());
    assertEquals(100, dto.capacity());
    assertEquals(10, dto.refillTokens());
    assertEquals(60, dto.refillSeconds());
    assertEquals(now, dto.updatedAt());
  }

  @Test
  void nullId_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(NullPointerException.class, () -> new RateLimitTierDto(null, 100, 10, 60, now));
  }

  @Test
  void nullUpdatedAt_allowed() {
    assertDoesNotThrow(() -> new RateLimitTierDto("pro", 500, 50, 30, null));
  }

  @Test
  void equalsAndHashCode() {
    Instant now = Instant.now(FIXED_CLOCK);
    var a = new RateLimitTierDto("free", 100, 10, 60, now);
    var b = new RateLimitTierDto("free", 100, 10, 60, now);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
