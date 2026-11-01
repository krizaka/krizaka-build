package com.krizaka.users.infrastructure.adapter.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserCredentialRequestTest {

  @Test
  void constructor_validInputs_createsRecord() {
    var request = new UserCredentialRequest("openai", "sk-123");
    assertEquals("openai", request.providerName());
    assertEquals("sk-123", request.apiKey());
  }

  @Test
  void constructor_nullProviderName_throws() {
    assertThrows(NullPointerException.class, () -> new UserCredentialRequest(null, "sk-123"));
  }

  @Test
  void constructor_blankProviderName_throws() {
    assertThrows(IllegalArgumentException.class, () -> new UserCredentialRequest("  ", "sk-123"));
  }

  @Test
  void constructor_nullApiKey_throws() {
    assertThrows(NullPointerException.class, () -> new UserCredentialRequest("openai", null));
  }

  @Test
  void constructor_blankApiKey_throws() {
    assertThrows(IllegalArgumentException.class, () -> new UserCredentialRequest("openai", "  "));
  }

  @Test
  void equalsHashCodeToString() {
    var req1 = new UserCredentialRequest("openai", "sk-123");
    var req2 = new UserCredentialRequest("openai", "sk-123");
    var req3 = new UserCredentialRequest("anthropic", "sk-456");

    assertEquals(req1, req2);
    assertNotEquals(req1, req3);
    assertEquals(req1.hashCode(), req2.hashCode());
    assertNotNull(req1.toString());
  }
}
