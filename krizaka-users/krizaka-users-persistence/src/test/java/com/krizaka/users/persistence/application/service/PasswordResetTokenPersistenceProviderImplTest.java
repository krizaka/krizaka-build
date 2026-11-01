package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.PasswordResetTokenDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.PasswordResetTokenEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.PasswordResetTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenPersistenceProviderImplTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private PasswordResetTokenJpaRepository repository;

  @InjectMocks private PasswordResetTokenPersistenceProviderImpl provider;

  @Test
  @DisplayName("save calls repository save with mapped entity")
  void saveToken() {
    Instant now = Instant.now(FIXED_CLOCK);
    PasswordResetTokenDto dto =
        new PasswordResetTokenDto("token-1", "user@example.com", "hash123", now, now);
    PasswordResetTokenEntity entity = PasswordResetTokenPersistenceProviderImpl.toEntity(dto);

    when(repository.save(any(PasswordResetTokenEntity.class))).thenReturn(entity);

    provider.save(dto);

    verify(repository).save(any(PasswordResetTokenEntity.class));
  }

  @Test
  @DisplayName("findByTokenHash retrieves and maps token")
  void findByTokenHash() {
    Instant now = Instant.now(FIXED_CLOCK);
    PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
    entity.setId("token-1");
    entity.setEmail("user@example.com");
    entity.setTokenHash("hash123");
    entity.setExpiresAt(now);
    entity.setCreatedAt(now);

    when(repository.findByTokenHash("hash123")).thenReturn(Optional.of(entity));

    Optional<PasswordResetTokenDto> result = provider.findByTokenHash("hash123");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("token-1");
  }

  @Test
  @DisplayName("deleteById calls repository delete")
  void deleteById() {
    doNothing().when(repository).deleteById("token-1");
    provider.deleteById("token-1");
    verify(repository).deleteById("token-1");
  }

  @Test
  @DisplayName("deleteByEmail calls repository deleteByEmail")
  void deleteByEmail() {
    doNothing().when(repository).deleteByEmail("user@example.com");
    provider.deleteByEmail("user@example.com");
    verify(repository).deleteByEmail("user@example.com");
  }
}
