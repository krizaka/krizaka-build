package com.krizaka.users.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    var dto = new UserProfileDto("user-1", "dark", Map.of("key", "value"));

    assertEquals("user-1", dto.userId());
    assertEquals("dark", dto.theme());
    assertEquals(Map.of("key", "value"), dto.rawPreferences());
  }

  @Test
  void nullUserId_throws() {
    assertThrows(NullPointerException.class, () -> new UserProfileDto(null, "dark", null));
  }

  @Test
  void nullTheme_defaultsToEmerald() {
    assertEquals("emerald", new UserProfileDto("user-1", null, null).theme());
  }

  @Test
  void nullRawPreferences_allowed() {
    assertNull(new UserProfileDto("user-1", "dark", null).rawPreferences());
  }
}
