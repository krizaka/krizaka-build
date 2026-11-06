package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.ports.outbound.CredentialGrantRepositoryPort;
import com.krizaka.users.persistence.domain.ports.CredentialGrantPersistenceProvider;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Outbound adapter of the credential grants over the persistence module. */
@Component
class CredentialGrantRepositoryAdapter implements CredentialGrantRepositoryPort {

  private final CredentialGrantPersistenceProvider provider;

  CredentialGrantRepositoryAdapter(CredentialGrantPersistenceProvider provider) {
    this.provider = Objects.requireNonNull(provider, "CredentialGrantPersistenceProvider");
  }

  @Override
  public void save(
      String grantHash, String userId, String providerName, String runId, Instant expiresAt) {
    provider.save(grantHash, userId, providerName, runId, expiresAt);
  }

  @Override
  public Optional<String> consume(
      String grantHash, String providerName, String runId, Instant now) {
    return provider.consume(grantHash, providerName, runId, now);
  }

  @Override
  public int purgeExpiredBefore(Instant before) {
    return provider.deleteExpiredBefore(before);
  }
}
