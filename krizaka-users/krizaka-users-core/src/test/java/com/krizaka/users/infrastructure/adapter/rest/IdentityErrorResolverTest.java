package com.krizaka.users.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.krizaka.users.domain.exception.ApiKeyLimitExceededException;
import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.exception.RegistrationClosedException;
import com.krizaka.users.domain.exception.UserAlreadyExistsException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdentityErrorResolverTest {

  private final IdentityErrorResolver resolver = new IdentityErrorResolver();

  @Test
  @DisplayName("handleUserAlreadyExists returns 409 Conflict")
  void handleUserAlreadyExists() {
    ResponseEntity<Map<String, String>> response =
        resolver.handleUserAlreadyExists(new UserAlreadyExistsException("Email taken"));

    assertNotNull(response);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("Email taken", response.getBody().get("error"));
  }

  @Test
  @DisplayName("handleApiKeyLimitExceeded returns 409 Conflict")
  void handleApiKeyLimitExceeded() {
    ResponseEntity<Map<String, String>> response =
        resolver.handleApiKeyLimitExceeded(new ApiKeyLimitExceededException("Limit reached"));

    assertNotNull(response);
    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals("Limit reached", response.getBody().get("error"));
  }

  @Test
  @DisplayName("handleInvalidRequest returns 400 Bad Request")
  void handleInvalidRequest() {
    ResponseEntity<Map<String, String>> response =
        resolver.handleInvalidRequest(new InvalidRequestException("Invalid prompt"));

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals("Invalid prompt", response.getBody().get("error"));
  }

  @Test
  @DisplayName("handleRegistrationClosed returns 403 Forbidden")
  void handleRegistrationClosed() {
    ResponseEntity<Map<String, String>> response =
        resolver.handleRegistrationClosed(new RegistrationClosedException());

    assertNotNull(response);
    assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    assertEquals("Registration is closed", response.getBody().get("error"));
  }
}
