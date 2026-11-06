package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExtractedProfileTest {

  @Test
  void validConstruction_setsAllFields() {
    var profile =
        new ExtractedProfile("user@test.com", "google-123", "John Doe", "http://avatar.jpg");
    assertEquals("user@test.com", profile.email());
    assertEquals("google-123", profile.providerId());
    assertEquals("John Doe", profile.name());
    assertEquals("http://avatar.jpg", profile.avatarUrl());
  }

  @Test
  void nullEmail_throws() {
    assertThrows(NullPointerException.class, () -> new ExtractedProfile(null, "id", "name", "url"));
  }

  @Test
  void nullProviderId_throws() {
    assertThrows(
        NullPointerException.class, () -> new ExtractedProfile("email", null, "name", "url"));
  }

  @Test
  void nullAvatarUrl_allowed() {
    var profile = new ExtractedProfile("email@test.com", "id-1", "name", null);
    assertNull(profile.avatarUrl());
  }
}
