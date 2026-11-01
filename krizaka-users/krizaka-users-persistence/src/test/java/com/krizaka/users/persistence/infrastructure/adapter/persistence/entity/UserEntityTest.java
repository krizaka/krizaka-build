package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UserEntity} getter/setter coverage. */
class UserEntityTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void defaultConstructor_setsDefaults() {
    UserEntity entity = new UserEntity();
    assertNull(entity.getId());
    assertNull(entity.getUsername());
    assertNull(entity.getPasswordHash());
    assertNull(entity.getEmail());
    assertTrue(entity.getEnabled());
    assertEquals("local", entity.getProvider());
    assertNull(entity.getProviderId());
    assertNull(entity.getRateLimitTier());
    assertNull(entity.getCreatedAt());
  }

  @Test
  void settersAndGetters_roundTrip() {
    UserEntity entity = new UserEntity();
    Instant now = Instant.now(FIXED_CLOCK);

    entity.setId("user-123");
    entity.setUsername("testuser");
    entity.setPasswordHash("hash123");
    entity.setEmail("user@test.io");
    entity.setEnabled(false);
    entity.setPreferences(Map.of("theme", "dark"));
    entity.setProvider("google");
    entity.setProviderId("google-456");
    entity.setRateLimitTier("premium");
    entity.setCreatedAt(now);
    entity.setAuthorities(Set.of());
    entity.setInterceptions(Set.of());

    assertEquals("user-123", entity.getId());
    assertEquals("testuser", entity.getUsername());
    assertEquals("hash123", entity.getPasswordHash());
    assertEquals("user@test.io", entity.getEmail());
    assertFalse(entity.getEnabled());
    assertEquals("dark", entity.getPreferences().get("theme"));
    assertEquals("google", entity.getProvider());
    assertEquals("google-456", entity.getProviderId());
    assertEquals("premium", entity.getRateLimitTier());
    assertEquals(now, entity.getCreatedAt());
    assertNotNull(entity.getAuthorities());
    assertNotNull(entity.getInterceptions());
  }
}
