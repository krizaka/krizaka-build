package com.krizaka.users.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.krizaka.users.domain.model.ApiKey;
import com.krizaka.users.domain.model.GeneratedApiKey;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.ApiKeyService;
import com.krizaka.users.infrastructure.adapter.rest.dto.ApiKeyResponse;
import com.krizaka.users.infrastructure.adapter.rest.dto.CreateApiKeyRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.GeneratedApiKeyResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

  @Mock private ApiKeyService apiKeyService;

  private ApiKeyController controller;
  private User user;
  private String userId;

  @BeforeEach
  void setUp() {
    controller = new ApiKeyController(apiKeyService);
    user =
        new User(
            UUID.randomUUID(), "user", "user@example.com", true, Set.of("ROLE_USER"), Map.of());
    userId = user.id().toString();
  }

  @Test
  void list_returnsMappedResponses() {
    ApiKey apiKey = new ApiKey("key-1", userId, "CI", "oz_live_abcd", Instant.now(), null);
    when(apiKeyService.list(userId)).thenReturn(List.of(apiKey));

    ResponseEntity<List<ApiKeyResponse>> response = controller.list(user);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("CI", response.getBody().getFirst().name());
    assertEquals("oz_live_abcd", response.getBody().getFirst().keyPrefix());
  }

  @Test
  void create_returnsCreatedWithPlaintextOnce() {
    GeneratedApiKey generated =
        new GeneratedApiKey("key-1", "CI", "oz_live_abcd", "oz_live_abcdSECRET");
    when(apiKeyService.create(userId, "CI", Set.of())).thenReturn(generated);

    ResponseEntity<GeneratedApiKeyResponse> response =
        controller.create(new CreateApiKeyRequest("CI"), user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("oz_live_abcdSECRET", response.getBody().key());
  }

  @Test
  void create_passesTheScopesOfAServiceKeyThrough() {
    GeneratedApiKey generated =
        new GeneratedApiKey(
            "key-2", "explorer", "oz_live_wxyz", "oz_live_wxyzSECRET", Set.of("ai:embed"));
    when(apiKeyService.create(userId, "explorer", Set.of("ai:embed"))).thenReturn(generated);

    ResponseEntity<GeneratedApiKeyResponse> response =
        controller.create(new CreateApiKeyRequest("explorer", Set.of("ai:embed")), user);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(java.util.List.of("ai:embed"), response.getBody().scopes());
  }

  @Test
  void revoke_returnsNoContentWhenRevoked() {
    when(apiKeyService.revoke(userId, "key-1")).thenReturn(true);

    ResponseEntity<Void> response = controller.revoke("key-1", user);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void revoke_returnsNotFoundWhenNothingMatched() {
    when(apiKeyService.revoke(userId, "key-x")).thenReturn(false);

    ResponseEntity<Void> response = controller.revoke("key-x", user);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }
}
