package com.krizaka.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.krizaka.test.events.EventContractTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The events krizaka-users publishes conform to the schemas krizaka-users-api ships. */
class UserEventsContractTest extends EventContractTest {

  private static final User USER =
      new User(
          UUID.fromString("7f9c2a52-1d1e-4a4a-9a51-6a0f3b1c2d3e"),
          "ada",
          "ada@example.com",
          false,
          Set.of("ROLE_USER"),
          Map.of("language", "fr", "theme", "dark"),
          List.of(),
          "free");

  @Test
  void userRegisteredWithAVerificationToken() {
    String json =
        assertConforms("evt.user.registered", 1, new UserRegisteredEvent(USER, "verify-me"));

    assertThat(json).contains("\"plaintextToken\":\"verify-me\"");
  }

  @Test
  void userRegisteredWithoutAToken() {
    assertDoesNotThrow(
        () -> {
          assertConforms("evt.user.registered", 1, new UserRegisteredEvent(USER, null));
        });
  }

  @Test
  void passwordReset() {
    assertDoesNotThrow(
        () -> {
          assertConforms(
              "evt.password.reset",
              1,
              new PasswordResetRequestedEvent("ada@example.com", "reset-me"));
        });
  }
}
