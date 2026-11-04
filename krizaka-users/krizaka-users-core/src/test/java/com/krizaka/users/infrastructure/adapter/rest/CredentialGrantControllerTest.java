package com.krizaka.users.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krizaka.users.domain.exception.CredentialGrantRefusedException;
import com.krizaka.users.domain.model.CredentialGrant;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.CredentialGrantService;
import com.krizaka.users.infrastructure.adapter.rest.dto.CredentialGrantRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Consent to a run is a live session: an {@code oz_} API key never obtains a grant. */
class CredentialGrantControllerTest {

  private final CredentialGrantService grants = mock(CredentialGrantService.class);
  private final CredentialGrantController controller = new CredentialGrantController(grants);
  private final User alice =
      new User(
          UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
          "alice",
          "alice@example.test",
          true,
          Set.of("ROLE_USER"),
          Map.of());

  @Test
  void aSessionGetsAGrantForItsOwnKey() {
    when(grants.issue(alice.id().toString(), "openai", "run-1"))
        .thenReturn(new CredentialGrant("g", Instant.parse("2026-11-04T00:10:00Z")));

    var response =
        controller.issue(
            "openai",
            new CredentialGrantRequest("run-1"),
            new UsernamePasswordAuthenticationToken(alice, "session.jwt.value"));

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody().grant()).isEqualTo("g");
    assertThat(response.getBody().toString()).doesNotContain("grant=g");
  }

  @Test
  void anApiKeyNeverGetsAGrant() {
    var byApiKey = new UsernamePasswordAuthenticationToken(alice, "oz_live_secret");

    assertThatThrownBy(
            () -> controller.issue("openai", new CredentialGrantRequest("run-1"), byApiKey))
        .isInstanceOf(CredentialGrantRefusedException.class);
    verify(grants, never()).issue(anyString(), anyString(), anyString());
  }
}
