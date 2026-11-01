package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.AuthorityDto;
import com.krizaka.users.persistence.domain.ports.AuthorityPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.AuthorityEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.AuthorityRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of AuthorityPersistenceProvider. */
@Service
@Transactional
class AuthorityPersistenceProviderImpl implements AuthorityPersistenceProvider {

  private final AuthorityRepository repository;

  AuthorityPersistenceProviderImpl(AuthorityRepository repository) {
    this.repository = Objects.requireNonNull(repository, "AuthorityRepository cannot be null");
  }

  @Override
  public void saveAuthority(String userId, String authorityName) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(authorityName, "AuthorityName cannot be null");
    AuthorityEntity entity = new AuthorityEntity();
    entity.setUserId(userId);
    entity.setAuthorityName(authorityName);
    repository.save(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AuthorityDto> findByUserId(String userId) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    return repository.findByUserId(userId).stream()
        .map(AuthorityPersistenceProviderImpl::toDto)
        .toList();
  }

  private static AuthorityDto toDto(AuthorityEntity entity) {
    if (entity == null) {
      return null;
    }
    return new AuthorityDto(entity.getId(), entity.getUserId(), entity.getAuthorityName());
  }
}
