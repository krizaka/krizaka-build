package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.UserInterceptionDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserInterceptionEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserInterceptionId;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserInterceptionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserInterceptionPersistenceProviderImplTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Mock private UserInterceptionRepository repository;

  @InjectMocks private UserInterceptionPersistenceProviderImpl provider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserInterceptionPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserInterceptionRepository cannot be null");
  }

  @Test
  @DisplayName("save persists and returns UserInterceptionDto")
  void save() {
    Instant now = Instant.now(FIXED_CLOCK);
    UserInterceptionDto dto = new UserInterceptionDto("user-1", "REFINER", true, now);
    UserInterceptionEntity entity =
        UserInterceptionPersistenceProviderImpl.toEntity(dto, "schema-1");
    when(repository.save(any(UserInterceptionEntity.class))).thenReturn(entity);

    UserInterceptionDto result = provider.save(dto, "schema-1");

    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo("user-1");
    assertThat(result.interceptionType()).isEqualTo("REFINER");
    verify(repository).save(any(UserInterceptionEntity.class));
  }

  @Test
  @DisplayName("deleteByUserIdAndInterceptionType deletes by composite key")
  void delete() {
    doNothing().when(repository).deleteById(any(UserInterceptionId.class));

    provider.deleteByUserIdAndInterceptionType("user-1", "REFINER");

    verify(repository).deleteById(new UserInterceptionId("user-1", "REFINER"));
  }

  @Test
  @DisplayName("findByUserIdAndInterceptionType retrieves and maps entity")
  void findByUserIdAndInterceptionType() {
    UserInterceptionEntity entity = new UserInterceptionEntity();
    UserInterceptionId id = new UserInterceptionId("user-1", "REFINER");
    entity.setId(id);
    entity.setSchemaId("schema-1");
    entity.setCreatedAt(Instant.now(FIXED_CLOCK));

    when(repository.findById(id)).thenReturn(Optional.of(entity));

    Optional<UserInterceptionDto> result =
        provider.findByUserIdAndInterceptionType("user-1", "REFINER");

    assertThat(result).isPresent();
    assertThat(result.get().userId()).isEqualTo("user-1");
    assertThat(result.get().interceptionType()).isEqualTo("REFINER");
  }
}
