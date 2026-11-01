package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.LockoutPolicy;
import com.krizaka.users.domain.model.LockoutScope;
import com.krizaka.users.domain.model.LockoutState;
import com.krizaka.users.domain.ports.outbound.LockoutRepositoryPort;
import com.krizaka.users.persistence.domain.model.LockoutRow;
import com.krizaka.users.persistence.domain.model.LockoutSettingsRow;
import com.krizaka.users.persistence.domain.ports.LockoutPersistenceProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Outbound adapter of the lockout: maps the persistence rows to the domain records. */
@Component
class LockoutRepositoryAdapter implements LockoutRepositoryPort {

  private final LockoutPersistenceProvider provider;

  LockoutRepositoryAdapter(LockoutPersistenceProvider provider) {
    this.provider = Objects.requireNonNull(provider, "LockoutPersistenceProvider");
  }

  private static LockoutState domain(LockoutRow r) {
    return new LockoutState(r.failures(), r.lastFailureAt(), r.lockedUntil());
  }

  @Override
  public Optional<LockoutState> find(LockoutScope scope, String subjectHash) {
    return provider.find(scope.name(), subjectHash).map(LockoutRepositoryAdapter::domain);
  }

  @Override
  public Optional<LockoutState> findForUpdate(LockoutScope scope, String subjectHash) {
    return provider.findForUpdate(scope.name(), subjectHash).map(LockoutRepositoryAdapter::domain);
  }

  @Override
  public void save(LockoutScope scope, String subjectHash, LockoutState state) {
    provider.save(
        scope.name(),
        subjectHash,
        new LockoutRow(state.failures(), state.lastFailureAt(), state.lockedUntil()));
  }

  @Override
  public void delete(LockoutScope scope, String subjectHash) {
    provider.delete(scope.name(), subjectHash);
  }

  @Override
  public int purgeIdleBefore(Instant before) {
    return provider.deleteIdleBefore(before);
  }

  @Override
  public LockoutPolicy policy() {
    LockoutSettingsRow s = provider.settings();
    return new LockoutPolicy(
        s.threshold(),
        s.accountThreshold(),
        Duration.ofSeconds(s.baseDelaySeconds()),
        Duration.ofSeconds(s.maxDelaySeconds()),
        Duration.ofSeconds(s.forgetAfterSeconds()));
  }

  @Override
  public void savePolicy(LockoutPolicy p, String updatedBy) {
    provider.saveSettings(
        new LockoutSettingsRow(
            p.threshold(),
            p.accountThreshold(),
            p.baseDelay().toSeconds(),
            p.maxDelay().toSeconds(),
            p.forgetAfter().toSeconds()),
        updatedBy);
  }
}
