package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void fullConstructor_setsAllFields() {
    UUID id = UUID.randomUUID();
    var user =
        new User(
            id,
            "john",
            "john@test.com",
            true,
            Set.of("ROLE_USER"),
            Map.of("language", "fr"),
            List.of("REFINER"),
            "pro");
    assertEquals(id, user.id());
    assertEquals("john", user.username());
    assertEquals("john@test.com", user.email());
    assertTrue(user.enabled());
    assertEquals(Set.of("ROLE_USER"), user.authorities());
    assertEquals("fr", user.preferences().get("language"));
    assertEquals(List.of("REFINER"), user.activeInterceptions());
    assertEquals("pro", user.rateLimitTier());
  }

  @Test
  void nullId_throws() {
    assertThrows(
        NullPointerException.class,
        () -> new User(null, "john", "john@test.com", true, null, null, null, "free"));
  }

  @Test
  void nullUsername_throws() {
    var id = UUID.randomUUID();
    assertThrows(
        IllegalArgumentException.class,
        () -> new User(id, null, "john@test.com", true, null, null, null, "free"));
  }

  @Test
  void blankUsername_throws() {
    var id = UUID.randomUUID();
    assertThrows(
        IllegalArgumentException.class,
        () -> new User(id, "  ", "john@test.com", true, null, null, null, "free"));
  }

  @Test
  void nullEmail_throws() {
    var id = UUID.randomUUID();
    assertThrows(
        IllegalArgumentException.class,
        () -> new User(id, "john", null, true, null, null, null, "free"));
  }

  @Test
  void blankEmail_throws() {
    var id = UUID.randomUUID();
    assertThrows(
        IllegalArgumentException.class,
        () -> new User(id, "john", "  ", true, null, null, null, "free"));
  }

  @Test
  void nullAuthorities_defaultsToEmptySet() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    assertNotNull(user.authorities());
    assertTrue(user.authorities().isEmpty());
  }

  @Test
  void nullPreferences_defaultsWithLanguageEn() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    assertNotNull(user.preferences());
    assertEquals("en", user.preferences().get("language"));
  }

  @Test
  void preferencesWithLanguage_preservesValue() {
    var user =
        new User(
            UUID.randomUUID(),
            "john",
            "john@test.com",
            true,
            null,
            Map.of("language", "fr"),
            null,
            "free");
    assertEquals("fr", user.preferences().get("language"));
  }

  @Test
  void nullActiveInterceptions_defaultsToEmptyList() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    assertNotNull(user.activeInterceptions());
    assertTrue(user.activeInterceptions().isEmpty());
  }

  @Test
  void sevenArgConstructor_setsNullRateLimitTier() {
    var user =
        new User(
            UUID.randomUUID(),
            "john",
            "john@test.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());
    assertNull(user.rateLimitTier());
  }

  @Test
  void sixArgConstructor_setsEmptyInterceptionsAndNullTier() {
    var user =
        new User(UUID.randomUUID(), "john", "john@test.com", true, Set.of("ROLE_USER"), Map.of());
    assertNull(user.rateLimitTier());
    assertTrue(user.activeInterceptions().isEmpty());
  }

  @Test
  void sevenArgConstructor_nullIdGeneratesUUID() {
    var user = new User(null, "john", "john@test.com", true, Set.of(), Map.of(), List.of());
    assertNotNull(user.id());
  }
}
