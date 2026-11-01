package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserRegisteredEventTest {

  @Test
  void validConstruction_setsFields() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    var event = new UserRegisteredEvent(user, "token-123");
    assertSame(user, event.user());
    assertEquals("token-123", event.plaintextToken());
  }

  @Test
  void nullPlaintextToken_allowed() {
    var user = new User(UUID.randomUUID(), "john", "john@test.com", true, null, null, null, "free");
    var event = new UserRegisteredEvent(user, null);
    assertNull(event.plaintextToken());
  }
}
