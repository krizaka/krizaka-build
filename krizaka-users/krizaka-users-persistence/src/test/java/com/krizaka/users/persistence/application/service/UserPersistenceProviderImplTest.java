package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.UserDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPersistenceProviderImplTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private UserRepository repository;

  @InjectMocks private UserPersistenceProviderImpl provider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserRepository cannot be null");
  }

  @Test
  @DisplayName("findById retrieves and maps UserEntity")
  void findById() {
    UserEntity entity = new UserEntity();
    entity.setId("user-1");
    entity.setUsername("john");
    entity.setEmail("john@test.com");
    entity.setPreferences(Collections.emptyMap());

    when(repository.findByIdWithAssociations("user-1")).thenReturn(Optional.of(entity));

    Optional<UserDto> result = provider.findById("user-1");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("user-1");
    verify(repository).findByIdWithAssociations("user-1");
  }

  @Test
  @DisplayName("findById throws NullPointerException on null id")
  void findByIdNull() {
    assertThatThrownBy(() -> provider.findById(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("findByEmailAndEnabledTrue retrieves and maps UserEntity")
  void findByEmailAndEnabledTrue() {
    UserEntity entity = new UserEntity();
    entity.setId("user-1");
    entity.setUsername("john");
    entity.setEmail("john@test.com");
    entity.setPreferences(Collections.emptyMap());

    when(repository.findByEmailAndEnabledTrueWithAssociations("john@test.com"))
        .thenReturn(Optional.of(entity));

    Optional<UserDto> result = provider.findByEmailAndEnabledTrue("john@test.com");

    assertThat(result).isPresent();
    assertThat(result.get().email()).isEqualTo("john@test.com");
  }

  @Test
  @DisplayName("findByProviderAndProviderId retrieves and maps UserEntity")
  void findByProviderAndProviderId() {
    UserEntity entity = new UserEntity();
    entity.setId("user-1");
    entity.setUsername("john");
    entity.setEmail("john@test.com");
    entity.setPreferences(Collections.emptyMap());

    when(repository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.of(entity));

    Optional<UserDto> result = provider.findByProviderAndProviderId("google", "google-id");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("user-1");
  }

  @Test
  @DisplayName("countByEmail returns count")
  void countByEmail() {
    when(repository.countByEmail("john@test.com")).thenReturn(5L);

    long count = provider.countByEmail("john@test.com");

    assertThat(count).isEqualTo(5L);
  }

  @Test
  @DisplayName("save updates and persists UserEntity")
  void save() {
    UserEntity existing = new UserEntity();
    existing.setId("user-1");
    existing.setUsername("old");
    existing.setEmail("john@test.com");
    existing.setPreferences(Collections.emptyMap());

    UserDto dto =
        new UserDto(
            "user-1",
            "new",
            "pass",
            "john@test.com",
            true,
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptyList(),
            "local",
            "pid",
            "free",
            Instant.now(FIXED_CLOCK));

    when(repository.findByIdWithAssociations("user-1")).thenReturn(Optional.of(existing));
    when(repository.save(any(UserEntity.class))).thenReturn(existing);

    UserDto result = provider.save(dto);

    assertThat(result).isNotNull();
    verify(repository).save(existing);
  }

  @Test
  @DisplayName("save throws IllegalArgumentException when user not found")
  void saveNotFound() {
    UserDto dto =
        new UserDto(
            "user-1",
            "new",
            "pass",
            "john@test.com",
            true,
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptyList(),
            "local",
            "pid",
            "free",
            Instant.now(FIXED_CLOCK));
    when(repository.findByIdWithAssociations("user-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> provider.save(dto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found:");
  }

  @Test
  @DisplayName("create persists and returns UserDto")
  void create() {
    UserDto dto =
        new UserDto(
            "user-1",
            "new",
            "pass",
            "john@test.com",
            true,
            Collections.emptyMap(),
            Collections.emptySet(),
            Collections.emptyList(),
            "local",
            "pid",
            "free",
            Instant.now(FIXED_CLOCK));
    UserEntity entity = UserPersistenceProviderImpl.toEntity(dto);
    when(repository.save(any(UserEntity.class))).thenReturn(entity);

    UserDto result = provider.create(dto, "pass");

    assertThat(result).isNotNull();
    verify(repository).save(any(UserEntity.class));
  }

  @Test
  @DisplayName("deleteById deletes user")
  void deleteById() {
    doNothing().when(repository).deleteById("user-1");

    provider.deleteById("user-1");

    verify(repository).deleteById("user-1");
  }

  @Test
  @DisplayName("updatePasswordHashByEmail updates password hash")
  void updatePassword() {
    when(repository.updatePasswordHashByEmail("john@test.com", "hash")).thenReturn(1);

    provider.updatePasswordHashByEmail("john@test.com", "hash");

    verify(repository).updatePasswordHashByEmail("john@test.com", "hash");
  }

  @Test
  @DisplayName("findByEmail retrieves and maps UserEntity")
  void findByEmail() {
    UserEntity entity = new UserEntity();
    entity.setId("user-1");
    entity.setUsername("john");
    entity.setEmail("john@test.com");
    entity.setPreferences(Collections.emptyMap());

    when(repository.findByEmail("john@test.com")).thenReturn(Optional.of(entity));

    Optional<UserDto> result = provider.findByEmail("john@test.com");

    assertThat(result).isPresent();
    assertThat(result.get().email()).isEqualTo("john@test.com");
  }
}
