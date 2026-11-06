package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.VerificationToken;
import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import com.krizaka.users.persistence.domain.ports.VerificationTokenPersistenceProvider;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerificationTokenRepositoryAdapterTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private VerificationTokenPersistenceProvider provider;

  @InjectMocks private VerificationTokenRepositoryAdapter adapter;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new VerificationTokenRepositoryAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("VerificationTokenPersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("save calls provider save")
  void save() {
    Instant now = Instant.now(FIXED_CLOCK);
    VerificationToken token =
        new VerificationToken("token-1", "user-1", "EMAIL_VERIFICATION", "hash-1", now, false);

    adapter.save(token);

    verify(provider)
        .save(
            argThat(
                dto ->
                    "token-1".equals(dto.id())
                        && "user-1".equals(dto.userId())
                        && "EMAIL_VERIFICATION".equals(dto.tokenType())
                        && "hash-1".equals(dto.tokenHash())
                        && dto.expiryTimestamp().equals(now)
                        && !dto.used()));
  }

  @Test
  @DisplayName("save throws NullPointerException on null token")
  void saveNull() {
    assertThatThrownBy(() -> adapter.save(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("VerificationToken cannot be null");
  }

  @Test
  @DisplayName("findByTokenHashAndUsedFalse returns mapped VerificationToken")
  void findByTokenHash() {
    Instant now = Instant.now(FIXED_CLOCK);
    VerificationTokenDto dto =
        new VerificationTokenDto(
            "token-1", "user-1", "EMAIL_VERIFICATION", "hash-1", now, false, now);
    when(provider.findByTokenHashAndUsedFalse("hash-1")).thenReturn(Optional.of(dto));

    Optional<VerificationToken> result = adapter.findByTokenHashAndUsedFalse("hash-1");

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo("token-1");
    assertThat(result.get().tokenHash()).isEqualTo("hash-1");
    verify(provider).findByTokenHashAndUsedFalse("hash-1");
  }

  @Test
  @DisplayName("findByTokenHashAndUsedFalse throws NullPointerException on null hash")
  void findByTokenHashNull() {
    assertThatThrownBy(() -> adapter.findByTokenHashAndUsedFalse(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Token hash cannot be null");
  }

  @Test
  @DisplayName("markAsUsed sets used=true and saves token")
  void markAsUsed() {
    Instant now = Instant.now(FIXED_CLOCK);
    VerificationTokenDto dto =
        new VerificationTokenDto(
            "token-1", "user-1", "EMAIL_VERIFICATION", "hash-1", now, false, now);
    when(provider.findById("token-1")).thenReturn(Optional.of(dto));

    adapter.markAsUsed("token-1");

    verify(provider).findById("token-1");
    verify(provider).save(argThat(VerificationTokenDto::used));
  }

  @Test
  @DisplayName("markAsUsed throws NullPointerException on null ID")
  void markAsUsedNull() {
    assertThatThrownBy(() -> adapter.markAsUsed(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Token ID cannot be null");
  }
}
