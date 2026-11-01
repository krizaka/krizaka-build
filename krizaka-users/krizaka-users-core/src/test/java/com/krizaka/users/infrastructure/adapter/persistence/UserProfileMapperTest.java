package com.krizaka.users.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.krizaka.users.domain.model.UserProfile;
import com.krizaka.users.persistence.domain.model.UserProfileDto;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileMapperTest {

  @Test
  void toDomain_mapsTheThemeAndTheStoredAttributes() {
    var dto = new UserProfileDto("user-1", "dark", Map.of("voiceModel", "shimmer"));

    UserProfile domain = UserProfileMapper.toDomain(dto);

    assertEquals("user-1", domain.userId());
    assertEquals("dark", domain.theme());
    assertEquals(Map.of("voiceModel", "shimmer"), domain.attributes());
  }

  @Test
  void toDomain_null_returnsNull() {
    assertNull(UserProfileMapper.toDomain(null));
  }

  @Test
  void toDomain_nullFields_applyDefaults() {
    UserProfile domain = UserProfileMapper.toDomain(new UserProfileDto("user-1", null, null));

    assertEquals("emerald", domain.theme());
    assertEquals(Map.of(), domain.attributes());
  }

  @Test
  void toDto_storesTheAttributesAsRawPreferences() {
    UserProfileDto dto =
        UserProfileMapper.toDto(new UserProfile("user-1", "dark", Map.of("key", "value")));

    assertEquals("user-1", dto.userId());
    assertEquals("dark", dto.theme());
    assertEquals(Map.of("key", "value"), dto.rawPreferences());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(UserProfileMapper.toDto(null));
  }
}
