package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.ports.CredentialGrantPersistenceProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of {@link CredentialGrantPersistenceProvider} (native SQL). */
@Component
@Transactional
class CredentialGrantPersistenceProviderImpl implements CredentialGrantPersistenceProvider {

  @PersistenceContext private EntityManager em;

  @Override
  public void save(
      String grantHash, String userId, String providerName, String runId, Instant expiresAt) {
    em.createNativeQuery(
            "INSERT INTO credential_grants (grant_hash, user_id, provider_name, run_id, expires_at)"
                + " VALUES (:h, :u, :p, :r, :e)")
        .setParameter("h", Objects.requireNonNull(grantHash))
        .setParameter("u", Objects.requireNonNull(userId))
        .setParameter("p", Objects.requireNonNull(providerName))
        .setParameter("r", Objects.requireNonNull(runId))
        .setParameter("e", Timestamp.from(expiresAt))
        .executeUpdate();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<String> consume(
      String grantHash, String providerName, String runId, Instant now) {
    List<Object> rows =
        em.createNativeQuery(
                "UPDATE credential_grants SET used_at = :n WHERE grant_hash = :h"
                    + " AND provider_name = :p AND run_id = :r AND used_at IS NULL"
                    + " AND expires_at > :n RETURNING user_id")
            .setParameter("n", Timestamp.from(now))
            .setParameter("h", Objects.requireNonNull(grantHash))
            .setParameter("p", Objects.requireNonNull(providerName))
            .setParameter("r", Objects.requireNonNull(runId))
            .getResultList();
    return rows.stream().findFirst().map(Object::toString);
  }

  @Override
  public int deleteExpiredBefore(Instant before) {
    return em.createNativeQuery("DELETE FROM credential_grants WHERE expires_at < :b")
        .setParameter("b", Timestamp.from(before))
        .executeUpdate();
  }
}
