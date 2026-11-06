package com.krizaka.users.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserDtoTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void validConstruction_setsAllFields() {
    Instant now = Instant.now(FIXED_CLOCK);
    var dto =
        new UserDto(
            "id",
            "john",
            "hash",
            "john@test.com",
            true,
            Map.of("lang", "en"),
            Set.of("ROLE_USER"),
            List.of("REFINER"),
            "local",
            "pid",
            "free",
            now);
    assertEquals("id", dto.id());
    assertEquals("john", dto.username());
    assertEquals("hash", dto.passwordHash());
    assertEquals("john@test.com", dto.email());
    assertTrue(dto.enabled());
    assertEquals(Map.of("lang", "en"), dto.preferences());
    assertEquals(Set.of("ROLE_USER"), dto.authorities());
    assertEquals(List.of("REFINER"), dto.interceptions());
    assertEquals("local", dto.provider());
    assertEquals("pid", dto.providerId());
    assertEquals("free", dto.rateLimitTier());
    assertEquals(now, dto.createdAt());
  }

  @Test
  void nullId_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserDto(null, "u", "h", "e", true, null, null, null, null, null, null, null));
  }

  @Test
  void nullUsername_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserDto("id", null, "h", "e", true, null, null, null, null, null, null, null));
  }

  @Test
  void nullEmail_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new UserDto("id", "u", "h", null, true, null, null, null, null, null, null, null));
  }

  @Test
  void nullEnabled_defaultsToTrue() {
    var dto = new UserDto("id", "u", "h", "e", null, null, null, null, null, null, null, null);
    assertTrue(dto.enabled());
  }

  @Test
  void nullProvider_defaultsToLocal() {
    var dto = new UserDto("id", "u", "h", "e", true, null, null, null, null, null, null, null);
    assertEquals("local", dto.provider());
  }
}
