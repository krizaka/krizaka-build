package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.krizaka.users.persistence.domain.ports.AuthorityPersistenceProvider;
import org.junit.jupiter.api.Test;

class AuthorityRepositoryAdapterTest {

  @Test
  void constructorNullCheck() {
    assertThatThrownBy(() -> new AuthorityRepositoryAdapter(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AuthorityPersistenceProvider cannot be null");
  }

  @Test
  void saveAuthority() {
    AuthorityPersistenceProvider provider = mock(AuthorityPersistenceProvider.class);
    AuthorityRepositoryAdapter adapter = new AuthorityRepositoryAdapter(provider);

    adapter.saveAuthority("user123", "ROLE_USER");

    verify(provider).saveAuthority("user123", "ROLE_USER");
  }

  @Test
  void saveAuthorityNullChecks() {
    AuthorityPersistenceProvider provider = mock(AuthorityPersistenceProvider.class);
    AuthorityRepositoryAdapter adapter = new AuthorityRepositoryAdapter(provider);

    assertThatThrownBy(() -> adapter.saveAuthority(null, "ROLE_USER"))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("UserId cannot be null");

    assertThatThrownBy(() -> adapter.saveAuthority("user123", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("AuthorityName cannot be null");
  }
}
