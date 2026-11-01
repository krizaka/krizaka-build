package com.krizaka.users.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserCredentialDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    var dto = new UserCredentialDto(1L, "user-1", "openai", "sk-key-123");
    assertEquals(1L, dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("openai", dto.providerName());
    assertEquals("sk-key-123", dto.apiKey());
  }

  @Test
  void nullUserId_throws() {
    assertThrows(
        NullPointerException.class, () -> new UserCredentialDto(1L, null, "openai", "key"));
  }

  @Test
  void nullProviderName_throws() {
    assertThrows(
        NullPointerException.class, () -> new UserCredentialDto(1L, "user-1", null, "key"));
  }

  @Test
  void nullApiKey_throws() {
    assertThrows(
        NullPointerException.class, () -> new UserCredentialDto(1L, "user-1", "openai", null));
  }

  @Test
  void nullId_allowed() {
    var dto = new UserCredentialDto(null, "user-1", "openai", "key");
    assertNull(dto.id());
  }
}
