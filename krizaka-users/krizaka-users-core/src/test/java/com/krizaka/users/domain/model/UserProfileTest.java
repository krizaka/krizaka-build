package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserProfileTest {

  @Test
  void carriesTheThemeAndTheApplicationsAttributesAsGiven() {
    var profile = new UserProfile("user-1", "dark", Map.of("voiceModel", "shimmer", "level", 3));

    assertEquals("user-1", profile.userId());
    assertEquals("dark", profile.theme());
    assertEquals(Map.of("voiceModel", "shimmer", "level", 3), profile.attributes());
  }

  @Test
  void nullUserId_throws() {
    assertThrows(NullPointerException.class, () -> new UserProfile(null, "dark", null));
  }

  @Test
  void nullTheme_throws() {
    assertThrows(NullPointerException.class, () -> new UserProfile("user", null, null));
  }

  @Test
  void nullAttributes_areEmpty() {
    assertTrue(new UserProfile("user", "dark", null).attributes().isEmpty());
  }

  @Test
  void attributes_areDefensivelyCopied() {
    var mutable = new HashMap<String, Object>();
    mutable.put("key", "value");
    var profile = new UserProfile("user", "dark", mutable);

    mutable.put("other", "x");

    assertEquals(Map.of("key", "value"), profile.attributes());
    assertThrows(UnsupportedOperationException.class, () -> profile.attributes().put("n", "v"));
  }
}
