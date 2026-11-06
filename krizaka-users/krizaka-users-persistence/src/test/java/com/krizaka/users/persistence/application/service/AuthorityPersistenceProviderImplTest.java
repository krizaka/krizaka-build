package com.krizaka.users.persistence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.krizaka.users.persistence.domain.model.AuthorityDto;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.AuthorityEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.AuthorityRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthorityPersistenceProviderImplTest {

  private AuthorityRepository repository;
  private AuthorityPersistenceProviderImpl provider;

  @BeforeEach
  void setUp() {
    repository = mock(AuthorityRepository.class);
    provider = new AuthorityPersistenceProviderImpl(repository);
  }

  @Test
  void constructorNullCheck() {
    assertThatThrownBy(() -> new AuthorityPersistenceProviderImpl(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AuthorityRepository cannot be null");
  }

  @Test
  void saveAuthority() {
    provider.saveAuthority("user123", "ROLE_ADMIN");
    verify(repository).save(any(AuthorityEntity.class));
  }

  @Test
  void saveAuthorityNullChecks() {
    assertThatThrownBy(() -> provider.saveAuthority(null, "ROLE_ADMIN"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");

    assertThatThrownBy(() -> provider.saveAuthority("user123", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AuthorityName cannot be null");
  }

  @Test
  void findByUserId() {
    AuthorityEntity entity = new AuthorityEntity();
    entity.setId(1L);
    entity.setUserId("user123");
    entity.setAuthorityName("ROLE_ADMIN");

    when(repository.findByUserId("user123")).thenReturn(List.of(entity));

    List<AuthorityDto> result = provider.findByUserId("user123");
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().userId()).isEqualTo("user123");
    assertThat(result.getFirst().authorityName()).isEqualTo("ROLE_ADMIN");
  }

  @Test
  void findByUserIdNullCheck() {
    assertThatThrownBy(() -> provider.findByUserId(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");
  }
}
