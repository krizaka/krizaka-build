package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.PasswordResetToken;
import com.krizaka.users.domain.ports.outbound.PasswordResetTokenRepositoryPort;
import com.krizaka.users.persistence.domain.model.PasswordResetTokenDto;
import com.krizaka.users.persistence.domain.ports.PasswordResetTokenPersistenceProvider;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Outbound persistence adapter implementing {@link PasswordResetTokenRepositoryPort}.
 *
 * <p>Bridges the identity domain model to the persistence provider using DTO mapping. Follows the
 * same adapter pattern established by {@link VerificationTokenRepositoryAdapter}.
 */
@Component
class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

  private final PasswordResetTokenPersistenceProvider provider;

  PasswordResetTokenRepositoryAdapter(PasswordResetTokenPersistenceProvider provider) {
    this.provider =
        Objects.requireNonNull(provider, "PasswordResetTokenPersistenceProvider cannot be null");
  }

  @Override
  public void save(PasswordResetToken token) {
    Objects.requireNonNull(token, "PasswordResetToken cannot be null");
    PasswordResetTokenDto dto = PasswordResetTokenMapper.toDto(token);
    provider.save(dto);
  }

  @Override
  public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    return provider.findByTokenHash(tokenHash).map(PasswordResetTokenMapper::toDomain);
  }

  @Override
  public void deleteById(String id) {
    Objects.requireNonNull(id, "Token ID cannot be null");
    provider.deleteById(id);
  }

  @Override
  public void deleteByEmail(String email) {
    Objects.requireNonNull(email, "Email cannot be null");
    provider.deleteByEmail(email);
  }
}
