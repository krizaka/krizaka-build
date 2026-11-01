package com.krizaka.users.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserInterceptionDtoTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void validConstruction_setsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto = new UserInterceptionDto("user-1", "REFINER", true, now);
    assertEquals("user-1", dto.userId());
    assertEquals("REFINER", dto.interceptionType());
    assertTrue(dto.active());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void nullUserId_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(
        NullPointerException.class, () -> new UserInterceptionDto(null, "type", true, now));
  }

  @Test
  void nullInterceptionType_throws() {
    var now = Instant.now(FIXED_CLOCK);
    assertThrows(
        NullPointerException.class, () -> new UserInterceptionDto("user", null, true, now));
  }

  @Test
  void nullActive_defaultsToTrue() {
    var dto = new UserInterceptionDto("user", "ROUTER", null, Instant.now(FIXED_CLOCK));
    assertTrue(dto.active());
  }

  @Test
  void falseActive_preserved() {
    var dto = new UserInterceptionDto("user", "REFINER", false, Instant.now(FIXED_CLOCK));
    assertFalse(dto.active());
  }
}
