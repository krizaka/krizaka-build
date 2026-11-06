package com.krizaka.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.model.RateLimitInfo;
import com.krizaka.users.persistence.domain.model.RateLimitDto;
import com.krizaka.users.persistence.domain.ports.RateLimitPersistenceProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitProviderImplTest {

  @Mock private RateLimitPersistenceProvider provider;

  @InjectMocks private RateLimitProviderImpl rateLimitProvider;

  @Test
  @DisplayName("Constructor throws NullPointerException on null provider")
  void constructorValidation() {
    assertThatThrownBy(() -> new RateLimitProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("RateLimitPersistenceProvider cannot be null");
  }

  @Test
  @DisplayName("getRateLimit retrieves and maps rate limit to domain info")
  void getRateLimit() {
    RateLimitDto dto = new RateLimitDto("premium", 100, 150);
    when(provider.findById("premium")).thenReturn(Optional.of(dto));

    Optional<RateLimitInfo> result = rateLimitProvider.getRateLimit("premium");

    assertThat(result).isPresent();
    assertThat(result.get().requestsPerMinute()).isEqualTo(100);
    assertThat(result.get().concurrentJobs()).isEqualTo(150);

    verify(provider).findById("premium");
  }

  @Test
  @DisplayName("getRateLimit returns empty when provider does not find details")
  void getRateLimitNotFound() {
    when(provider.findById("nonexistent")).thenReturn(Optional.empty());

    Optional<RateLimitInfo> result = rateLimitProvider.getRateLimit("nonexistent");

    assertThat(result).isEmpty();
    verify(provider).findById("nonexistent");
  }

  @Test
  @DisplayName("getRateLimit throws NullPointerException when tierKey is null")
  void getRateLimitNullKey() {
    assertThatThrownBy(() -> rateLimitProvider.getRateLimit(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Tier key cannot be null");
  }

  @Test
  @DisplayName("getDefaultTierKey delegates to the persistence provider")
  void getDefaultTierKey() {
    when(provider.findDefaultTierKey()).thenReturn(Optional.of("free"));

    assertThat(rateLimitProvider.getDefaultTierKey()).contains("free");

    verify(provider).findDefaultTierKey();
  }
}
