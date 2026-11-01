package com.krizaka.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.exception.ApiKeyLimitExceededException;
import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.model.ApiKey;
import com.krizaka.users.domain.model.GeneratedApiKey;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.ApiKeyService;
import com.krizaka.users.domain.ports.outbound.ApiKeyRepositoryPort;
import com.krizaka.users.domain.ports.outbound.CryptographyPort;
import com.krizaka.users.domain.ports.outbound.UserRepositoryPort;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

  private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440001";

  @Mock private ApiKeyRepositoryPort apiKeyRepository;
  @Mock private UserRepositoryPort userRepository;
  @Mock private CryptographyPort cryptography;

  @InjectMocks private ApiKeyServiceImpl service;

  private User enabledUser() {
    return new User(
        UUID.fromString(USER_ID), "user", "user@example.com", true, Set.of("ROLE_USER"), Map.of());
  }

  @Test
  @DisplayName(
      "create generates an oz_live_ secret, persists only its hash, and returns plaintext once")
  void createHappyPath() {
    when(apiKeyRepository.countByUserId(USER_ID)).thenReturn(3L);
    when(cryptography.hashToken(anyString())).thenReturn("hashed");

    GeneratedApiKey generated = service.create(USER_ID, "  CI pipeline  ");

    assertThat(generated.name()).isEqualTo("CI pipeline");
    assertThat(generated.plaintext()).startsWith("oz_live_");
    assertThat(generated.keyPrefix()).hasSize(12).isEqualTo(generated.plaintext().substring(0, 12));
    verify(apiKeyRepository)
        .save(
            anyString(),
            eq(USER_ID),
            eq("CI pipeline"),
            eq(generated.keyPrefix()),
            eq("hashed"),
            eq(Set.of()));
  }

  @Test
  @DisplayName("create persists the scopes of a service key, sorted and trimmed")
  void createScoped() {
    when(apiKeyRepository.countByUserId(USER_ID)).thenReturn(0L);
    when(cryptography.hashToken(anyString())).thenReturn("hashed");

    GeneratedApiKey generated = service.create(USER_ID, "explorer", Set.of(" ai:embed "));

    assertThat(generated.scopes()).containsExactly("ai:embed");
    verify(apiKeyRepository)
        .save(
            anyString(),
            eq(USER_ID),
            eq("explorer"),
            anyString(),
            eq("hashed"),
            eq(Set.of("ai:embed")));
  }

  @Test
  @DisplayName("create refuses a malformed scope before generating a secret")
  void createRejectsMalformedScope() {
    for (String bad : new String[] {"embed", "AI:embed", "ai:", "ai:embed;drop", "*:*"}) {
      assertThatThrownBy(() -> service.create(USER_ID, "k", Set.of(bad)))
          .isInstanceOf(InvalidRequestException.class)
          .hasMessageContaining("scope");
    }
    verify(cryptography, never()).hashToken(anyString());
  }

  @Test
  @DisplayName("authenticateKey answers the scopes of the presented key")
  void authenticateKeyCarriesScopes() {
    ApiKey match =
        new ApiKey(
            "key-1", USER_ID, "svc", "oz_live_abcd", Instant.now(), null, Set.of("ai:embed"));
    when(cryptography.hashToken("oz_live_secret")).thenReturn("h");
    when(apiKeyRepository.findByKeyHash("h")).thenReturn(Optional.of(match));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(enabledUser()));

    var result = service.authenticateKey("oz_live_secret");

    assertThat(result).isPresent();
    assertThat(result.get().scopes()).containsExactly("ai:embed");
    assertThat(service.authenticate("oz_live_secret")).isPresent();
  }

  @Test
  @DisplayName("create rejects a blank name before touching the repository")
  void createBlankName() {
    assertThatThrownBy(() -> service.create(USER_ID, "   "))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("must not be blank");
    verify(apiKeyRepository, never())
        .save(anyString(), anyString(), anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("create throws when the per-user maximum is reached")
  void createLimitReached() {
    when(apiKeyRepository.countByUserId(USER_ID))
        .thenReturn((long) ApiKeyService.MAX_KEYS_PER_USER);

    assertThatThrownBy(() -> service.create(USER_ID, "one more"))
        .isInstanceOf(ApiKeyLimitExceededException.class);
    verify(apiKeyRepository, never())
        .save(anyString(), anyString(), anyString(), anyString(), anyString(), any());
  }

  @Test
  @DisplayName("revoke reports success when a row is deleted")
  void revokeDeletes() {
    when(apiKeyRepository.deleteByIdAndUserId("key-1", USER_ID)).thenReturn(1L);
    assertThat(service.revoke(USER_ID, "key-1")).isTrue();
  }

  @Test
  @DisplayName("revoke reports failure when nothing matched the owner")
  void revokeNoMatch() {
    when(apiKeyRepository.deleteByIdAndUserId("key-x", USER_ID)).thenReturn(0L);
    assertThat(service.revoke(USER_ID, "key-x")).isFalse();
  }

  @Test
  @DisplayName("authenticate ignores tokens without the oz_ prefix without hashing or lookups")
  void authenticateNonApiKeyToken() {
    assertThat(service.authenticate("some-session-uuid")).isEmpty();
    verify(cryptography, never()).hashToken(anyString());
    verify(apiKeyRepository, never()).findByKeyHash(anyString());
  }

  @Test
  @DisplayName("authenticate resolves a valid key to its enabled owner and stamps last-used")
  void authenticateValid() {
    ApiKey match = new ApiKey("key-1", USER_ID, "CI", "oz_live_abcd", Instant.now(), null);
    when(cryptography.hashToken("oz_live_secret")).thenReturn("h");
    when(apiKeyRepository.findByKeyHash("h")).thenReturn(Optional.of(match));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(enabledUser()));

    Optional<User> result = service.authenticate("oz_live_secret");

    assertThat(result).isPresent();
    assertThat(result.get().id()).hasToString(USER_ID);
    verify(apiKeyRepository).touchLastUsed(eq("key-1"), any(Instant.class));
  }

  @Test
  @DisplayName("authenticate rejects a disabled owner and does not stamp last-used")
  void authenticateDisabledUser() {
    ApiKey match = new ApiKey("key-1", USER_ID, "CI", "oz_live_abcd", Instant.now(), null);
    User disabled =
        new User(
            UUID.fromString(USER_ID),
            "user",
            "user@example.com",
            false,
            Set.of("ROLE_USER"),
            Map.of());
    when(cryptography.hashToken("oz_live_secret")).thenReturn("h");
    when(apiKeyRepository.findByKeyHash("h")).thenReturn(Optional.of(match));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(disabled));

    assertThat(service.authenticate("oz_live_secret")).isEmpty();
    verify(apiKeyRepository, never()).touchLastUsed(anyString(), any(Instant.class));
  }

  @Test
  @DisplayName("authenticate returns empty when the key hash matches nothing")
  void authenticateUnknownHash() {
    when(cryptography.hashToken("oz_live_secret")).thenReturn("h");
    when(apiKeyRepository.findByKeyHash("h")).thenReturn(Optional.empty());

    assertThat(service.authenticate("oz_live_secret")).isEmpty();
  }
}
