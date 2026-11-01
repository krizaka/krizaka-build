package com.krizaka.users.persistence.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthorityDtoTest {

  @Test
  void validConstruction_setsAllFields() {
    var dto = new AuthorityDto(1L, "user-1", "ROLE_ADMIN");
    assertEquals(1L, dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("ROLE_ADMIN", dto.authorityName());
  }

  @Test
  void nullUserId_throws() {
    assertThrows(NullPointerException.class, () -> new AuthorityDto(1L, null, "ROLE_USER"));
  }

  @Test
  void nullAuthorityName_throws() {
    assertThrows(NullPointerException.class, () -> new AuthorityDto(1L, "user-1", null));
  }
}
