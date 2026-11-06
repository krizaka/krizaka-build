package com.krizaka.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.UserProfile;
import com.krizaka.users.domain.ports.outbound.UserProfileRepositoryPort;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileProviderImplTest {

  @Mock private UserProfileRepositoryPort repository;

  @InjectMocks private UserProfileProviderImpl provider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null repository")
  void constructorValidation() {
    assertThatThrownBy(() -> new UserProfileProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserProfileRepositoryPort must not be null");
  }

  @Test
  @DisplayName("getProfile retrieves and returns UserProfile")
  void getProfile() {
    UserProfile profile = new UserProfile("user-1", "dark", Map.of("voiceModel", "alloy"));
    when(repository.findByUserId("user-1")).thenReturn(Optional.of(profile));

    UserProfile result = provider.getProfile("user-1");

    assertThat(result).isNotNull();
    assertThat(result.userId()).isEqualTo("user-1");
    assertThat(result.theme()).isEqualTo("dark");
    assertThat(result.attributes()).containsEntry("voiceModel", "alloy");

    verify(repository).findByUserId("user-1");
  }

  @Test
  @DisplayName("getProfile returns null when not found")
  void getProfileNotFound() {
    when(repository.findByUserId("nonexistent")).thenReturn(Optional.empty());

    UserProfile result = provider.getProfile("nonexistent");

    assertThat(result).isNull();
    verify(repository).findByUserId("nonexistent");
  }

  @Test
  @DisplayName("getProfile throws NullPointerException on null userId")
  void getProfileNullKey() {
    assertThatThrownBy(() -> provider.getProfile(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("userId must not be null");
  }
}
