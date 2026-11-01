package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.PasswordResetTokenDto;
import com.krizaka.users.persistence.domain.ports.PasswordResetTokenPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.PasswordResetTokenEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.PasswordResetTokenJpaRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of PasswordResetTokenPersistenceProvider. */
@Service
@Transactional
class PasswordResetTokenPersistenceProviderImpl implements PasswordResetTokenPersistenceProvider {

  private final PasswordResetTokenJpaRepository repository;

  PasswordResetTokenPersistenceProviderImpl(PasswordResetTokenJpaRepository repository) {
    this.repository =
        Objects.requireNonNull(repository, "PasswordResetTokenJpaRepository cannot be null");
  }

  @Override
  public void save(PasswordResetTokenDto dto) {
    Objects.requireNonNull(dto, "PasswordResetTokenDto cannot be null");
    PasswordResetTokenEntity entity = toEntity(dto);
    repository.save(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<PasswordResetTokenDto> findByTokenHash(String tokenHash) {
    Objects.requireNonNull(tokenHash, "Token hash cannot be null");
    return repository
        .findByTokenHash(tokenHash)
        .map(PasswordResetTokenPersistenceProviderImpl::toDto);
  }

  @Override
  public void deleteById(String id) {
    Objects.requireNonNull(id, "Token ID cannot be null");
    repository.deleteById(id);
  }

  @Override
  public void deleteByEmail(String email) {
    Objects.requireNonNull(email, "Email cannot be null");
    repository.deleteByEmail(email);
  }

  /**
   * Converts a persistence DTO to a JPA entity.
   *
   * @param dto The persistence DTO.
   * @return The JPA entity.
   */
  static PasswordResetTokenEntity toEntity(PasswordResetTokenDto dto) {
    PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
    entity.setId(dto.id());
    entity.setEmail(dto.email());
    entity.setTokenHash(dto.tokenHash());
    entity.setExpiresAt(dto.expiresAt());
    entity.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : Instant.now());
    return entity;
  }

  /**
   * Converts a JPA entity to a persistence DTO.
   *
   * @param entity The JPA entity.
   * @return The persistence DTO.
   */
  private static PasswordResetTokenDto toDto(PasswordResetTokenEntity entity) {
    return new PasswordResetTokenDto(
        entity.getId(),
        entity.getEmail(),
        entity.getTokenHash(),
        entity.getExpiresAt(),
        entity.getCreatedAt());
  }
}
