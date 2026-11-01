package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.SocialConnectionRow;
import com.krizaka.users.persistence.domain.ports.SocialConnectionPersistenceProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Package-private implementation of {@link SocialConnectionPersistenceProvider} (native SQL). */
@Component
@Transactional
class SocialConnectionPersistenceProviderImpl implements SocialConnectionPersistenceProvider {

  private static final String SELECT =
      "SELECT provider, client_id, sealed_client_secret, key_id, source, updated_at"
          + " FROM social_connection";

  @PersistenceContext private EntityManager em;

  private static SocialConnectionRow toRow(Object[] r) {
    return new SocialConnectionRow(
        (String) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4], instant(r[5]));
  }

  /** Hibernate returns an Instant, a Timestamp or an OffsetDateTime, depending on the version. */
  private static Instant instant(Object value) {
    return switch (value) {
      case Instant i -> i;
      case Timestamp t -> t.toInstant();
      case OffsetDateTime o -> o.toInstant();
      default -> throw new IllegalStateException("unexpected updated_at type " + value.getClass());
    };
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<SocialConnectionRow> findAll() {
    List<Object[]> rows = em.createNativeQuery(SELECT + " ORDER BY provider").getResultList();
    return rows.stream().map(SocialConnectionPersistenceProviderImpl::toRow).toList();
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public Optional<SocialConnectionRow> find(String provider) {
    Objects.requireNonNull(provider, "provider cannot be null");
    List<Object[]> rows =
        em.createNativeQuery(SELECT + " WHERE provider = :p")
            .setParameter("p", provider)
            .getResultList();
    return rows.stream().findFirst().map(SocialConnectionPersistenceProviderImpl::toRow);
  }

  @Override
  public void save(SocialConnectionRow row) {
    Objects.requireNonNull(row, "row cannot be null");
    em.createNativeQuery(
            "INSERT INTO social_connection"
                + " (provider, client_id, sealed_client_secret, key_id, source, updated_at)"
                + " VALUES (:p, :c, :s, :k, :src, :at)"
                + " ON CONFLICT (provider) DO UPDATE SET client_id = :c,"
                + " sealed_client_secret = :s, key_id = :k, source = :src, updated_at = :at")
        .setParameter("p", row.provider())
        .setParameter("c", row.clientId())
        .setParameter("s", row.sealedClientSecret())
        .setParameter("k", row.keyId())
        .setParameter("src", row.source())
        .setParameter(
            "at", Timestamp.from(row.updatedAt() == null ? Instant.now() : row.updatedAt()))
        .executeUpdate();
  }
}
