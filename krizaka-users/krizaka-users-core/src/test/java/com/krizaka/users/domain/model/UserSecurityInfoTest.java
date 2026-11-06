package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserSecurityInfoTest {

  @Test
  void validConstruction_setsFields() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    var info = new UserSecurityInfo(user, "bcrypt-hash-123");
    assertSame(user, info.user());
    assertEquals("bcrypt-hash-123", info.passwordHash());
  }

  @Test
  void nullUser_throws() {
    assertThrows(NullPointerException.class, () -> new UserSecurityInfo(null, "hash"));
  }

  @Test
  void nullPasswordHash_allowed() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    var info = new UserSecurityInfo(user, null);
    assertNull(info.passwordHash());
  }
}
