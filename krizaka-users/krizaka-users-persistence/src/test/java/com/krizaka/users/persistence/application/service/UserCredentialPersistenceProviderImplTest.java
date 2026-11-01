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
  @DisplayName("save updates existing credential")
  void saveExisting() {
    UserCredentialEntity entity = new UserCredentialEntity();
    entity.setId(1L);
    entity.setUserId("user-1");
    entity.setProviderName("OPENAI");
    entity.setApiKey("old-key");

    UserCredentialDto dto = new UserCredentialDto(1L, "user-1", "OPENAI", "new-key");

    when(repository.findByUserIdAndProviderName("user-1", "OPENAI"))
        .thenReturn(Optional.of(entity));
    when(repository.save(any(UserCredentialEntity.class))).thenReturn(entity);

    UserCredentialDto result = provider.save(dto);

    assertThat(result).isNotNull();
    assertThat(entity.getApiKey()).isEqualTo("new-key");
    verify(repository).save(entity);
  }

  @Test
  @DisplayName("save creates new credential")
  void saveNew() {
    UserCredentialDto dto = new UserCredentialDto(null, "user-1", "OPENAI", "new-key");
    UserCredentialEntity savedEntity = new UserCredentialEntity();
    savedEntity.setId(2L);
    savedEntity.setUserId("user-1");
    savedEntity.setProviderName("OPENAI");
    savedEntity.setApiKey("new-key");

    when(repository.findByUserIdAndProviderName("user-1", "OPENAI")).thenReturn(Optional.empty());
    when(repository.save(any(UserCredentialEntity.class))).thenReturn(savedEntity);

    UserCredentialDto result = provider.save(dto);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(2L);
    verify(repository).save(any(UserCredentialEntity.class));
  }

  @Test
  @DisplayName("deleteByUserIdAndProviderName deletes credential")
  void delete() {
    doNothing().when(repository).deleteByUserIdAndProviderName("user-1", "OPENAI");

    provider.deleteByUserIdAndProviderName("user-1", "OPENAI");

    verify(repository).deleteByUserIdAndProviderName("user-1", "OPENAI");
  }
}
