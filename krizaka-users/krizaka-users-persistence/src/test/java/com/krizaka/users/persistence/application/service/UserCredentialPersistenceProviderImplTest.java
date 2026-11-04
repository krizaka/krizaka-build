package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserCredentialEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserCredentialRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCredentialPersistenceProviderImplTest {

  @Mock private UserCredentialRepository repository;

  @InjectMocks private UserCredentialPersistenceProviderImpl provider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserCredentialPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserCredentialRepository cannot be null");
  }

  @Test
  @DisplayName("findByUserId returns mapped list")
  void findByUserId() {
    UserCredentialEntity entity = new UserCredentialEntity();
    entity.setId(1L);
    entity.setUserId("user-1");
    entity.setProviderName("OPENAI");
    entity.setApiKey("key-123");

    when(repository.findByUserId("user-1")).thenReturn(List.of(entity));

    List<UserCredentialDto> result = provider.findByUserId("user-1");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().apiKey()).isEqualTo("key-123");
  }

  @Test
  @DisplayName("findByUserIdAndProviderName returns mapped Dto if present")
  void findByUserIdAndProviderName() {
    UserCredentialEntity entity = new UserCredentialEntity();
    entity.setId(1L);
    entity.setUserId("user-1");
    entity.setProviderName("OPENAI");
    entity.setApiKey("key-123");

    when(repository.findByUserIdAndProviderName("user-1", "OPENAI"))
        .thenReturn(Optional.of(entity));

    Optional<UserCredentialDto> result = provider.findByUserIdAndProviderName("user-1", "OPENAI");

    assertThat(result).isPresent();
    assertThat(result.get().apiKey()).isEqualTo("key-123");
  }

  @Test
  @DisplayName("insert refuses a second credential for the same provider")
  void insertExisting() {
    when(repository.insertIfAbsent("user-1", "OPENAI", "v2.k1.c.d")).thenReturn(0);

    assertThat(provider.insert(new UserCredentialDto(null, "user-1", "OPENAI", "v2.k1.c.d")))
        .isEmpty();
  }

  @Test
  @DisplayName("insert creates a new credential")
  void insertNew() {
    UserCredentialEntity savedEntity = new UserCredentialEntity();
    savedEntity.setId(2L);
    savedEntity.setUserId("user-1");
    savedEntity.setProviderName("OPENAI");
    savedEntity.setApiKey("v2.k1.a.b");
    when(repository.insertIfAbsent("user-1", "OPENAI", "v2.k1.a.b")).thenReturn(1);
    when(repository.findByUserIdAndProviderName("user-1", "OPENAI"))
        .thenReturn(Optional.of(savedEntity));

    assertThat(provider.insert(new UserCredentialDto(null, "user-1", "OPENAI", "v2.k1.a.b")))
        .get()
        .extracting(UserCredentialDto::id)
        .isEqualTo(2L);
  }

  @Test
  @DisplayName("replaceStoredKey and delete say whether a row was there")
  void replaceAndDelete() {
    when(repository.replaceStoredKey("user-1", "OPENAI", "v2.k1.c.d")).thenReturn(1);
    when(repository.replaceStoredKey("user-2", "OPENAI", "v2.k1.c.d")).thenReturn(0);
    when(repository.deleteByUserIdAndProviderName("user-1", "OPENAI")).thenReturn(1L);
    when(repository.deleteByUserIdAndProviderName("user-2", "OPENAI")).thenReturn(0L);

    assertThat(provider.replaceStoredKey("user-1", "OPENAI", "v2.k1.c.d")).isTrue();
    assertThat(provider.replaceStoredKey("user-2", "OPENAI", "v2.k1.c.d")).isFalse();
    assertThat(provider.deleteByUserIdAndProviderName("user-1", "OPENAI")).isTrue();
    assertThat(provider.deleteByUserIdAndProviderName("user-2", "OPENAI")).isFalse();
  }
}
