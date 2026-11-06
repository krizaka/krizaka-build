package com.krizaka.users.infrastructure.adapter.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserCredentialResponseTest {

  @Test
  void constructor_validInputs_createsRecord() {
    var response = new UserCredentialResponse("openai", true);
    assertEquals("openai", response.providerName());
    assertTrue(response.configured());
  }

  @Test
  void constructor_nullProviderName_throws() {
    assertThrows(NullPointerException.class, () -> new UserCredentialResponse(null, true));
  }

  @Test
  void constructor_blankProviderName_throws() {
    assertThrows(IllegalArgumentException.class, () -> new UserCredentialResponse("  ", true));
  }

  @Test
  void equalsHashCodeToString() {
    var resp1 = new UserCredentialResponse("openai", true);
    var resp2 = new UserCredentialResponse("openai", true);
    var resp3 = new UserCredentialResponse("anthropic", false);

    assertEquals(resp1, resp2);
    assertNotEquals(resp1, resp3);
    assertEquals(resp1.hashCode(), resp2.hashCode());
    assertNotNull(resp1.toString());
  }
}
