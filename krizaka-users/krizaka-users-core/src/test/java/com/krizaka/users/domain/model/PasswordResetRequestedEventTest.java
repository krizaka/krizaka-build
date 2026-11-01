package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordResetRequestedEventTest {

  @Test
  void validConstruction_setsFields() {
    var event = new PasswordResetRequestedEvent("user@test.com", "plaintext-token-123");
    assertEquals("user@test.com", event.email());
    assertEquals("plaintext-token-123", event.plaintextToken());
  }

  @Test
  void nullEmail_throws() {
    assertThrows(NullPointerException.class, () -> new PasswordResetRequestedEvent(null, "token"));
  }

  @Test
  void nullPlaintextToken_throws() {
    assertThrows(
        NullPointerException.class, () -> new PasswordResetRequestedEvent("email@test.com", null));
  }
}
