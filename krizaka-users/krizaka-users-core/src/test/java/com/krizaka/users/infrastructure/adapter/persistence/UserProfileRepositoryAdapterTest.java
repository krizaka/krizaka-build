package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.UserProfile;
import com.krizaka.users.persistence.domain.model.UserProfileDto;
import com.krizaka.users.persistence.domain.ports.UserProfilePersistenceProvider;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileRepositoryAdapterTest {

  @Mock private UserProfilePersistenceProvider provider;

  @InjectMocks private UserProfileRepositoryAdapter adapter;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserProfileRepositoryAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserProfilePersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("findByUserId retrieves and maps UserProfileDto to domain model")
  void findByUserId() {
    UserProfileDto dto = new UserProfileDto("user-123", "dark", Map.of("voiceModel", "alloy"));
    when(provider.findByUserId("user-123")).thenReturn(Optional.of(dto));

    Optional<UserProfile> result = adapter.findByUserId("user-123");

    assertThat(result).isPresent();
    assertThat(result.get().userId()).isEqualTo("user-123");
    assertThat(result.get().theme()).isEqualTo("dark");
    assertThat(result.get().attributes()).containsEntry("voiceModel", "alloy");

    verify(provider).findByUserId("user-123");
  }

  @Test
  @DisplayName("findByUserId returns empty Optional when not found")
  void findByUserIdNotFound() {
    when(provider.findByUserId("nonexistent")).thenReturn(Optional.empty());

    Optional<UserProfile> result = adapter.findByUserId("nonexistent");

    assertThat(result).isEmpty();
    verify(provider).findByUserId("nonexistent");
  }

  @Test
  @DisplayName("findByUserId throws NullPointerException on null userId")
  void findByUserIdNull() {
    assertThatThrownBy(() -> adapter.findByUserId(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");
  }
}
