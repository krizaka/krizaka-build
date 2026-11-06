package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.PasswordResetToken;
import com.krizaka.users.persistence.domain.model.PasswordResetTokenDto;
import com.krizaka.users.persistence.domain.ports.PasswordResetTokenPersistenceProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenRepositoryAdapterTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private PasswordResetTokenPersistenceProvider provider;

  @InjectMocks private PasswordResetTokenRepositoryAdapter adapter;

  @Test
  @DisplayName("save maps domain model to DTO and calls provider save")
  void saveToken() {
    Instant now = Instant.now(FIXED_CLOCK);
    PasswordResetToken token =
        new PasswordResetToken("token-1", "user@example.com", "hash123", now);

    doNothing().when(provider).save(any(PasswordResetTokenDto.class));

    adapter.save(token);

    verify(provider).save(any(PasswordResetTokenDto.class));
  }

  @Test
  @DisplayName("findByTokenHash retrieves token from provider and maps to domain model")
  void findByTokenHash() {
    Instant now = Instant.now(FIXED_CLOCK);
    PasswordResetTokenDto dto =
        new PasswordResetTokenDto("token-1", "user@example.com", "hash123", now, now);

    when(provider.findByTokenHash("hash123")).thenReturn(Optional.of(dto));

    Optional<PasswordResetToken> result = adapter.findByTokenHash("hash123");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("token-1");
  }

  @Test
  @DisplayName("deleteById calls provider deleteById")
  void deleteById() {
    doNothing().when(provider).deleteById("token-1");
    adapter.deleteById("token-1");
    verify(provider).deleteById("token-1");
  }

  @Test
  @DisplayName("deleteByEmail calls provider deleteByEmail")
  void deleteByEmail() {
    doNothing().when(provider).deleteByEmail("user@example.com");
    adapter.deleteByEmail("user@example.com");
    verify(provider).deleteByEmail("user@example.com");
  }
}
