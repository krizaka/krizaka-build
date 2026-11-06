package com.krizaka.users.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IdentityExceptionsTest {

  @Test
  void badCredentialsException_preservesMessage() {
    var ex = new BadCredentialsException("Invalid password");
    assertEquals("Invalid password", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void userNotFoundException_formatsMessage() {
    var ex = new UserNotFoundException("user-123");
    assertEquals("User not found: user-123", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void userAlreadyExistsException_preservesMessage() {
    var ex = new UserAlreadyExistsException("Email already registered");
    assertEquals("Email already registered", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void invalidRequestException_preservesMessage() {
    var ex = new InvalidRequestException("Missing field");
    assertEquals("Missing field", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  void securityGuardrailException_preservesMessage() {
    var ex = new SecurityGuardrailException("Crypto failure");
    assertEquals("Crypto failure", ex.getMessage());
  }

  @Test
  void securityGuardrailException_preservesCause() {
    var cause = new IllegalStateException("Root cause");
    var ex = new SecurityGuardrailException("Crypto failure", cause);
    assertEquals("Crypto failure", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }
}
