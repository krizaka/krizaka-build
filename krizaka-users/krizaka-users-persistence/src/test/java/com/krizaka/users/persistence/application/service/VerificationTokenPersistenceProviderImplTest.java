package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.VerificationTokenEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.VerificationTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationTokenPersistenceProviderImplTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private VerificationTokenRepository repository;

  @InjectMocks private VerificationTokenPersistenceProviderImpl provider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new VerificationTokenPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("VerificationTokenRepository cannot be null");
  }

  @Test
  @DisplayName("save persists and maps VerificationTokenDto")
  void save() {
    Instant now = Instant.now(FIXED_CLOCK);
    VerificationTokenDto dto =
        new VerificationTokenDto(
            "token-123", "user-1", "EMAIL_VERIFICATION", "hash456", now, false, now);
    VerificationTokenEntity entity = VerificationTokenPersistenceProviderImpl.toEntity(dto);

    when(repository.save(any(VerificationTokenEntity.class))).thenReturn(entity);

    VerificationTokenDto result = provider.save(dto);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo("token-123");
    assertThat(result.userId()).isEqualTo("user-1");
    assertThat(result.tokenHash()).isEqualTo("hash456");
    assertThat(result.used()).isFalse();

    verify(repository).save(any(VerificationTokenEntity.class));
  }

  @Test
  @DisplayName("save throws NullPointerException on null DTO")
  void saveNull() {
    assertThatThrownBy(() -> provider.save(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("VerificationTokenDto cannot be null");
  }

  @Test
  @DisplayName("findByTokenHashAndUsedFalse retrieves and maps VerificationTokenEntity")
  void findByTokenHashAndUsedFalse() {
    VerificationTokenEntity entity = new VerificationTokenEntity();
    entity.setId("token-123");
    entity.setUserId("user-1");
    entity.setTokenType("EMAIL_VERIFICATION");
    entity.setTokenHash("hash456");
    entity.setExpiryTimestamp(Instant.now(FIXED_CLOCK));
    entity.setUsed(false);

    when(repository.findByTokenHashAndUsedFalse("hash456")).thenReturn(Optional.of(entity));

    Optional<VerificationTokenDto> result = provider.findByTokenHashAndUsedFalse("hash456");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("token-123");
    assertThat(result.get().tokenHash()).isEqualTo("hash456");
    verify(repository).findByTokenHashAndUsedFalse("hash456");
  }

  @Test
  @DisplayName("findByTokenHashAndUsedFalse throws NullPointerException on null hash")
  void findByTokenHashNull() {
    assertThatThrownBy(() -> provider.findByTokenHashAndUsedFalse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Token hash cannot be null");
  }

  @Test
  @DisplayName("findById retrieves and maps VerificationTokenEntity")
  void findById() {
    VerificationTokenEntity entity = new VerificationTokenEntity();
    entity.setId("token-123");
    entity.setUserId("user-1");
    entity.setTokenType("EMAIL_VERIFICATION");
    entity.setTokenHash("hash456");
    entity.setExpiryTimestamp(Instant.now(FIXED_CLOCK));
    entity.setUsed(false);

    when(repository.findById("token-123")).thenReturn(Optional.of(entity));

    Optional<VerificationTokenDto> result = provider.findById("token-123");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("token-123");
    verify(repository).findById("token-123");
  }

  @Test
  @DisplayName("findById throws NullPointerException on null ID")
  void findByIdNull() {
    assertThatThrownBy(() -> provider.findById(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Token ID cannot be null");
  }

  @Test
  @DisplayName("deleteById deletes token from repository")
  void deleteById() {
    doNothing().when(repository).deleteById("token-123");
    provider.deleteById("token-123");
    verify(repository).deleteById("token-123");
  }

  @Test
  @DisplayName("deleteById throws NullPointerException on null ID")
  void deleteByIdNull() {
    assertThatThrownBy(() -> provider.deleteById(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Token ID cannot be null");
  }
}
