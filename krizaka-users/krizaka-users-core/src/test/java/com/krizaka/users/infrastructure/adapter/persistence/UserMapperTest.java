package com.krizaka.users.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.persistence.domain.model.UserDto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toDomain_mapsAllFields() {
    var dto =
        new UserDto(
            "550e8400-e29b-41d4-a716-446655440000",
            "john",
            "hash",
            "john@test.com",
            true,
            Map.of("language", "en"),
            Set.of("ROLE_USER"),
            List.of("REFINER"),
            "local",
            "pid",
            "free",
            Instant.now(FIXED_CLOCK));
    User user = UserMapper.toDomain(dto);
    assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), user.id());
    assertEquals("john", user.username());
    assertEquals("john@test.com", user.email());
    assertTrue(user.enabled());
    assertEquals(Set.of("ROLE_USER"), user.authorities());
    assertEquals("en", user.preferences().get("language"));
    assertEquals("free", user.rateLimitTier());
  }

  @Test
  void toDomain_null_returnsNull() {
    assertNull(UserMapper.toDomain(null));
  }

  @Test
  void toDto_mapsAllFields() {
    var user =
        new User(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "john",
            "john@test.com",
            true,
            Set.of("ROLE_USER"),
            Map.of("language", "en"),
            List.of("REFINER"),
            "free");
    UserDto dto = UserMapper.toDto(user, "hashed-password");
    assertEquals("550e8400-e29b-41d4-a716-446655440000", dto.id());
    assertEquals("john", dto.username());
    assertEquals("hashed-password", dto.passwordHash());
    assertEquals("john@test.com", dto.email());
    assertTrue(dto.enabled());
    assertEquals("local", dto.provider());
    assertEquals("free", dto.rateLimitTier());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(UserMapper.toDto(null, "hash"));
  }
}
