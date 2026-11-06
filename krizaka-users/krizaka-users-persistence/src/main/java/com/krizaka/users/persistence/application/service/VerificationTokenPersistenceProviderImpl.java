package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import com.krizaka.users.persistence.domain.ports.VerificationTokenPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.VerificationTokenEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.VerificationTokenRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of VerificationTokenPersistenceProvider. */
@Service
@Transactional
class VerificationTokenPersistenceProviderImpl implements VerificationTokenPersistenceProvider {

  private final VerificationTokenRepository repository;

  VerificationTokenPersistenceProviderImpl(VerificationTokenRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "VerificationTokenRepository cannot be null");
  }

  @Override
  public VerificationTokenDto save(VerificationTokenDto tokenDto) {
    Objects.requireNonNull(tokenDto, "VerificationTokenDto cannot be null");
    VerificationTokenEntity entity = toEntity(tokenDto);
    VerificationTokenEntity saved = repository.save(entity);
    return toDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VerificationTokenDto> findByTokenHashAndUsedFalse(String tokenHash) {
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    return repository
        .findByTokenHashAndUsedFalse(tokenHash)
        .map(VerificationTokenPersistenceProviderImpl::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VerificationTokenDto> findById(String tokenId) {
    Objects.requireNonNull(tokenId, "Token ID cannot be null");
    return repository.findById(tokenId).map(VerificationTokenPersistenceProviderImpl::toDto);
  }

  @Override
  public void deleteById(String tokenId) {
    Objects.requireNonNull(tokenId, "Token ID cannot be null");
    repository.deleteById(tokenId);
  }

  private static VerificationTokenDto toDto(VerificationTokenEntity entity) {
    if (entity == null) {
      return null;
    }
    return new VerificationTokenDto(
        entity.getId(),
        entity.getUserId(),
        entity.getTokenType(),
        entity.getTokenHash(),
        entity.getExpiryTimestamp(),
        entity.getUsed(),
        entity.getCreatedAt());
  }

  static VerificationTokenEntity toEntity(VerificationTokenDto dto) {
    if (dto == null) {
      return null;
    }
    VerificationTokenEntity entity = new VerificationTokenEntity();
    entity.setId(dto.id());
    entity.setUserId(dto.userId());
    entity.setTokenType(dto.tokenType());
    entity.setTokenHash(dto.tokenHash());
    entity.setExpiryTimestamp(dto.expiryTimestamp());
    entity.setUsed(dto.used());
    entity.setCreatedAt(dto.createdAt());
    return entity;
  }
}
