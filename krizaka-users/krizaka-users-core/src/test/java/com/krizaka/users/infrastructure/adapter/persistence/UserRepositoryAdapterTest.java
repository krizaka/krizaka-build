package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.Persona;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserSecurityInfo;
import com.krizaka.users.persistence.domain.model.UserDto;
import com.krizaka.users.persistence.domain.ports.UserPersistenceProvider;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private UserPersistenceProvider provider;

  @InjectMocks private UserRepositoryAdapter adapter;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserRepositoryAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserPersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("findById retrieves and maps UserDto")
  void findById() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findById(user.id().toString())).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findById(user.id().toString());

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("findByEmailAndEnabledTrue retrieves and maps UserDto")
  void findByEmailAndEnabledTrue() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByEmailAndEnabledTrue(user.email())).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findByEmailAndEnabledTrue(user.email());

    assertThat(result).isPresent();
    assertThat(result.get().email()).isEqualTo(user.email());
  }

  @Test
  @DisplayName("findByProviderAndProviderId retrieves and maps UserDto")
  void findByProviderAndProviderId() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByProviderAndProviderId("google", "google-id")).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findByProviderAndProviderId("google", "google-id");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("findSecurityInfoByEmail retrieves and returns UserSecurityInfo")
  void findSecurityInfoByEmail() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByEmailAndEnabledTrue(user.email())).thenReturn(Optional.of(dto));

    Optional<UserSecurityInfo> result = adapter.findSecurityInfoByEmail(user.email());

    assertThat(result).isPresent();
    assertThat(result.get().passwordHash()).isEqualTo("hash");
    assertThat(result.get().user().id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("save updates existing user")
  void save() {
    User user = Persona.freeUser();
    UserDto existing =
        new UserDto(
            user.id().toString(),
            user.username(),
            "password-hash",
            user.email(),
            user.enabled(),
            Map.of(),
            Set.of("ROLE_USER"),
            List.of(),
            "local",
            "provider-id",
            "free",
            Instant.now(FIXED_CLOCK));

    when(provider.findById(user.id().toString())).thenReturn(Optional.of(existing));
    when(provider.save(any(UserDto.class))).thenReturn(existing);

    User result = adapter.save(user);

    assertThat(result).isNotNull();
    verify(provider)
        .save(
            argThat(
                dto ->
                    "password-hash".equals(dto.passwordHash()) && "local".equals(dto.provider())));
  }

  @Test
  @DisplayName("save throws IllegalArgumentException when user not found")
  void saveNotFound() {
    User user = Persona.freeUser();
    when(provider.findById(user.id().toString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.save(user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found:");
  }

  @Test
  @DisplayName("create creates a new user")
  void create() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.create(any(UserDto.class), eq("hash"))).thenReturn(dto);

    User result = adapter.create(user, "hash");

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(user.id());
  }

  @Test
  @DisplayName("updatePasswordHashByEmail delegates to provider")
  void updatePassword() {
    adapter.updatePasswordHashByEmail("test@test.com", "new-hash");

    verify(provider).updatePasswordHashByEmail("test@test.com", "new-hash");
  }

  @Test
  @DisplayName("findByEmail retrieves and maps UserDto")
  void findByEmail() {
    User user = Persona.freeUser();
    UserDto dto = UserMapper.toDto(user, "hash");
    when(provider.findByEmail(user.email())).thenReturn(Optional.of(dto));

    Optional<User> result = adapter.findByEmail(user.email());

    assertThat(result).isPresent();
    assertThat(result.get().email()).isEqualTo(user.email());
  }

  @Test
  @DisplayName(
      "createFederated stores the provider identity, without a password (ADR-001 finding 1)")
  void createFederatedStoresTheProviderIdentity() {
    User user = Persona.freeUser();
    when(provider.create(
            org.mockito.ArgumentMatchers.any(UserDto.class), org.mockito.ArgumentMatchers.isNull()))
        .thenAnswer(inv -> inv.getArgument(0));

    adapter.createFederated(user, "github", "583231");

    org.mockito.ArgumentCaptor<UserDto> dto = org.mockito.ArgumentCaptor.forClass(UserDto.class);
    org.mockito.Mockito.verify(provider)
        .create(dto.capture(), org.mockito.ArgumentMatchers.isNull());
    assertThat(dto.getValue().provider()).isEqualTo("github");
    assertThat(dto.getValue().providerId()).isEqualTo("583231");
    assertThat(dto.getValue().passwordHash()).isNull();
  }

  @Test
  @DisplayName("usernameTaken asks the store")
  void usernameTaken() {
    when(provider.existsByUsername("Alex Martin")).thenReturn(true);
    assertThat(adapter.usernameTaken("Alex Martin")).isTrue();
    assertThat(adapter.usernameTaken("Someone Else")).isFalse();
  }
}
