package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.UserInterceptionDto;
import com.krizaka.users.persistence.domain.ports.UserInterceptionPersistenceProvider;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserInterceptionRepositoryAdapterTest {

  @Mock private UserInterceptionPersistenceProvider provider;

  @InjectMocks private UserInterceptionRepositoryAdapter adapter;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserInterceptionRepositoryAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserInterceptionPersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("triggerInterception saves new interception")
  void triggerInterception() {
    UUID userId = UUID.randomUUID();
    adapter.triggerInterception(userId, "REFINER", "schema-1");

    verify(provider)
        .save(
            argThat(
                dto ->
                    dto.userId().equals(userId.toString())
                        && "REFINER".equals(dto.interceptionType())),
            eq("schema-1"));
  }

  @Test
  @DisplayName("triggerInterception ignores DataIntegrityViolationException gracefully")
  void triggerInterceptionDuplicate() {
    UUID userId = UUID.randomUUID();
    doThrow(new DataIntegrityViolationException("duplicate"))
        .when(provider)
        .save(any(UserInterceptionDto.class), anyString());

    adapter.triggerInterception(userId, "REFINER", "schema-1");

    verify(provider).save(any(UserInterceptionDto.class), eq("schema-1"));
  }

  @Test
  @DisplayName("triggerInterception throws NullPointerException on null arguments")
  void triggerInterceptionNull() {
    UUID userId = UUID.randomUUID();
    assertThatThrownBy(() -> adapter.triggerInterception(null, "REFINER", "schema-1"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> adapter.triggerInterception(userId, null, "schema-1"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("deleteInterception calls provider delete")
  void deleteInterception() {
    UUID userId = UUID.randomUUID();
    adapter.deleteInterception(userId, "REFINER");

    verify(provider).deleteByUserIdAndInterceptionType(userId.toString(), "REFINER");
  }

  @Test
  @DisplayName("deleteInterception throws NullPointerException on null arguments")
  void deleteInterceptionNull() {
    UUID userId = UUID.randomUUID();
    assertThatThrownBy(() -> adapter.deleteInterception(null, "REFINER"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> adapter.deleteInterception(userId, null))
        .isInstanceOf(NullPointerException.class);
  }
}
