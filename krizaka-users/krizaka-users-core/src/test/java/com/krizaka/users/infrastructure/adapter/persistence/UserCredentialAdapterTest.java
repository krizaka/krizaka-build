package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.UserCredential;
import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import com.krizaka.users.persistence.domain.ports.UserCredentialPersistenceProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCredentialAdapterTest {

  @Mock private UserCredentialPersistenceProvider provider;

  @InjectMocks private UserCredentialAdapter adapter;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserCredentialAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserCredentialPersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("findByUserId maps and returns list of UserCredential")
  void findByUserId() {
    UserCredentialDto dto = new UserCredentialDto(1L, "user-1", "OPENAI", "key-123");
    when(provider.findByUserId("user-1")).thenReturn(List.of(dto));

    List<UserCredential> result = adapter.findByUserId("user-1");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().providerName()).isEqualTo("OPENAI");
    assertThat(result.getFirst().configured()).isTrue();
    verify(provider).findByUserId("user-1");
  }

  @Test
  @DisplayName("findByUserId throws NullPointerException on null userId")
  void findByUserIdNull() {
    assertThatThrownBy(() -> adapter.findByUserId(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");
  }

  @Test
  @DisplayName("findApiKeyByUserIdAndProviderName returns apiKey if present")
  void findApiKey() {
    UserCredentialDto dto = new UserCredentialDto(1L, "user-1", "OPENAI", "key-123");
    when(provider.findByUserIdAndProviderName("user-1", "OPENAI")).thenReturn(Optional.of(dto));

    Optional<String> result = adapter.findApiKeyByUserIdAndProviderName("user-1", "OPENAI");

    assertThat(result).isPresent().contains("key-123");
  }

  @Test
  @DisplayName("findApiKeyByUserIdAndProviderName throws NullPointerException on null arguments")
  void findApiKeyNull() {
    assertThatThrownBy(() -> adapter.findApiKeyByUserIdAndProviderName(null, "OPENAI"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");

    assertThatThrownBy(() -> adapter.findApiKeyByUserIdAndProviderName("user-1", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("ProviderName cannot be null");
  }

  @Test
  @DisplayName("save calls provider save")
  void save() {
    adapter.save("user-1", "OPENAI", "key-123");

    verify(provider)
        .save(
            argThat(
                dto ->
                    "user-1".equals(dto.userId())
                        && "OPENAI".equals(dto.providerName())
                        && "key-123".equals(dto.apiKey())));
  }

  @Test
  @DisplayName("save throws NullPointerException on null parameters")
  void saveNull() {
    assertThatThrownBy(() -> adapter.save(null, "P", "K")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> adapter.save("U", null, "K")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> adapter.save("U", "P", null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("deleteByUserIdAndProviderName calls provider delete")
  void delete() {
    adapter.deleteByUserIdAndProviderName("user-1", "OPENAI");

    verify(provider).deleteByUserIdAndProviderName("user-1", "OPENAI");
  }

  @Test
  @DisplayName("deleteByUserIdAndProviderName throws NullPointerException on null parameters")
  void deleteNull() {
    assertThatThrownBy(() -> adapter.deleteByUserIdAndProviderName(null, "P"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> adapter.deleteByUserIdAndProviderName("U", null))
        .isInstanceOf(NullPointerException.class);
  }
}
