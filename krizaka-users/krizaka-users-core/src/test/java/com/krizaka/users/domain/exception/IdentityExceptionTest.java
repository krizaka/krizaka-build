package com.krizaka.users.domain.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for identity exception hierarchy. */
class IdentityExceptionTest {

  @Test
  @DisplayName("InvalidRequestException carries message")
  void invalidRequestMessage() {
    var ex = new InvalidRequestException("bad input");
    assertEquals("bad input", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("UserAlreadyExistsException carries message")
  void userAlreadyExistsMessage() {
    var ex = new UserAlreadyExistsException("email taken");
    assertEquals("email taken", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("BadCredentialsException carries message")
  void badCredentialsMessage() {
    var ex = new BadCredentialsException("Invalid credentials");
    assertEquals("Invalid credentials", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("UserNotFoundException formats userId in message")
  void userNotFoundMessage() {
    var ex = new UserNotFoundException("user-42");
    assertEquals("User not found: user-42", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("ConfigurationException carries message")
  void configurationExceptionMessage() {
    var ex = new ConfigurationException("missing config");
    assertEquals("missing config", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }

  @Test
  @DisplayName("SecurityGuardrailException carries message")
  void securityGuardrailMessage() {
    var ex = new SecurityGuardrailException("guardrail triggered");
    assertEquals("guardrail triggered", ex.getMessage());
    assertInstanceOf(RuntimeException.class, ex);
  }
}
