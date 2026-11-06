package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserCredentialTest {

  @Test
  void validConstruction_setsFields() {
    var cred = new UserCredential("openai", true);
    assertEquals("openai", cred.providerName());
    assertTrue(cred.configured());
  }

  @Test
  void notConfigured_isFalse() {
    var cred = new UserCredential("claude", false);
    assertFalse(cred.configured());
  }

  @Test
  void nullProviderName_throws() {
    assertThrows(NullPointerException.class, () -> new UserCredential(null, true));
  }

  @Test
  void blankProviderName_throws() {
    assertThrows(IllegalArgumentException.class, () -> new UserCredential("  ", true));
  }

  @Test
  void equalsAndHashCode() {
    var a = new UserCredential("openai", true);
    var b = new UserCredential("openai", true);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
