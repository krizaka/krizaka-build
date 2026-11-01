package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.VerificationToken;
import com.krizaka.users.domain.ports.outbound.VerificationTokenRepositoryPort;
import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import com.krizaka.users.persistence.domain.ports.VerificationTokenPersistenceProvider;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Outbound persistence adapter implementing {@link VerificationTokenRepositoryPort}. Delegates
 * persistence requests to the clean VerificationTokenPersistenceProvider port.
 */
@Component
class VerificationTokenRepositoryAdapter implements VerificationTokenRepositoryPort {

  private final VerificationTokenPersistenceProvider provider;

  VerificationTokenRepositoryAdapter(VerificationTokenPersistenceProvider provider) {
    this.provider =
        Objects.requireNonNull(provider, "VerificationTokenPersistenceProvider cannot be null");
  }

  @Override
  public void save(VerificationToken token) {
    Objects.requireNonNull(token, "VerificationToken cannot be null");
    VerificationTokenDto dto = VerificationTokenMapper.toDto(token);
    provider.save(dto);
  }

  @Override
  public Optional<VerificationToken> findByTokenHashAndUsedFalse(String tokenHash) {
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    return provider.findByTokenHashAndUsedFalse(tokenHash).map(VerificationTokenMapper::toDomain);
  }

  @Override
  public void markAsUsed(String tokenId) {
    Objects.requireNonNull(tokenId, "Token ID cannot be null");
    provider
        .findById(tokenId)
        .ifPresent(
            dto -> {
              VerificationTokenDto updated =
                  new VerificationTokenDto(
                      dto.id(),
                      dto.userId(),
                      dto.tokenType(),
                      dto.tokenHash(),
                      dto.expiryTimestamp(),
                      true, // set used = true
                      dto.createdAt());
              provider.save(updated);
            });
  }
}
