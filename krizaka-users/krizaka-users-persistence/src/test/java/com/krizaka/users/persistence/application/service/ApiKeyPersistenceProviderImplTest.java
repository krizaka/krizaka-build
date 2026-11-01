package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krizaka.users.persistence.domain.model.ApiKeyDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.ApiKeyEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.ApiKeyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyPersistenceProviderImplTest {

  @Mock private ApiKeyRepository repository;

  @InjectMocks private ApiKeyPersistenceProviderImpl provider;

  private static ApiKeyEntity entity() {
    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId("key-1");
    entity.setUserId("user-1");
    entity.setName("CI");
    entity.setKeyPrefix("oz_live_abcd");
    entity.setKeyHash("hash");
    entity.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
    return entity;
  }

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new ApiKeyPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ApiKeyRepository cannot be null");
  }

  @Test
  @DisplayName("findByUserId maps entities to DTOs")
  void findByUserId() {
    when(repository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(entity()));

    List<ApiKeyDto> result = provider.findByUserId("user-1");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo("key-1");
    assertThat(result.getFirst().keyPrefix()).isEqualTo("oz_live_abcd");
  }

  @Test
  @DisplayName("scopes round-trip as a sorted comma-separated column; an old row has none")
  void scopesRoundTrip() {
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(i -> i.getArgument(0));

    ApiKeyDto saved =
        provider.save(
            new ApiKeyDto(
                "k",
                "u",
                "n",
                "oz_live_abcd",
                "h",
                null,
                null,
                java.util.Set.of("ai:embed", "ai:chat")));

    ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getScopes()).isEqualTo("ai:chat,ai:embed");
    assertThat(saved.scopes()).containsExactlyInAnyOrder("ai:chat", "ai:embed");
    assertThat(new ApiKeyEntity().getScopes()).isEmpty();
    when(repository.findByKeyHash("old")).thenReturn(Optional.of(entity()));
    assertThat(provider.findByKeyHash("old").orElseThrow().scopes()).isEmpty();
  }

  @Test
  @DisplayName("countByUserId delegates to the repository")
  void countByUserId() {
    when(repository.countByUserId("user-1")).thenReturn(4L);
    assertThat(provider.countByUserId("user-1")).isEqualTo(4L);
  }

  @Test
  @DisplayName("findByKeyHash maps the matching entity")
  void findByKeyHash() {
    when(repository.findByKeyHash("hash")).thenReturn(Optional.of(entity()));

    Optional<ApiKeyDto> result = provider.findByKeyHash("hash");

    assertThat(result).isPresent();
    assertThat(result.get().userId()).isEqualTo("user-1");
    assertThat(result.get().keyHash()).isEqualTo("hash");
  }

  @Test
  @DisplayName("save persists all fields and defaults createdAt when absent")
  void saveDefaultsCreatedAt() {
    ApiKeyDto dto = new ApiKeyDto("key-2", "user-1", "laptop", "oz_live_wxyz", "hash2", null, null);
    when(repository.save(any(ApiKeyEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    ApiKeyDto result = provider.save(dto);

    ArgumentCaptor<ApiKeyEntity> captor = ArgumentCaptor.forClass(ApiKeyEntity.class);
    verify(repository).save(captor.capture());
    ApiKeyEntity saved = captor.getValue();
    assertThat(saved.getId()).isEqualTo("key-2");
    assertThat(saved.getKeyHash()).isEqualTo("hash2");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(result.name()).isEqualTo("laptop");
  }

  @Test
  @DisplayName("deleteByIdAndUserId returns the delete count")
  void deleteByIdAndUserId() {
    when(repository.deleteByIdAndUserId("key-1", "user-1")).thenReturn(1L);
    assertThat(provider.deleteByIdAndUserId("key-1", "user-1")).isEqualTo(1L);
  }

  @Test
  @DisplayName("touchLastUsed delegates to the repository")
  void touchLastUsed() {
    Instant now = Instant.now();
    provider.touchLastUsed("key-1", now);
    verify(repository).touchLastUsed("key-1", now);
  }
}
