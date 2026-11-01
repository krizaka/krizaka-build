package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link AuthorityEntity}. */
class AuthorityEntityTest {

  @Test
  void settersAndGetters_roundTrip() {
    AuthorityEntity entity = new AuthorityEntity();
    entity.setId(1L);
    entity.setUserId("user-123");
    entity.setAuthorityName("ROLE_ADMIN");

    assertEquals(1L, entity.getId());
    assertEquals("user-123", entity.getUserId());
    assertEquals("ROLE_ADMIN", entity.getAuthorityName());
  }
}
