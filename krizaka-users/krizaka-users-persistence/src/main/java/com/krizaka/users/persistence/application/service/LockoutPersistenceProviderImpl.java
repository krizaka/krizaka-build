package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.LockoutRow;
import com.krizaka.users.persistence.domain.model.LockoutSettingsRow;
import com.krizaka.users.persistence.domain.ports.LockoutPersistenceProvider;
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

/** Package-private implementation of {@link LockoutPersistenceProvider} (native SQL). */
@Component
@Transactional
class LockoutPersistenceProviderImpl implements LockoutPersistenceProvider {

  private static final String SELECT =
      "SELECT failures, last_failure_at, locked_until FROM login_lockout"
          + " WHERE scope = :s AND subject_hash = :h";

  @PersistenceContext private EntityManager em;

  /** Hibernate returns an Instant, a Timestamp or an OffsetDateTime, depending on the version. */
  private static Instant instant(Object value) {
    return switch (value) {
      case null -> null;
      case Instant i -> i;
      case Timestamp t -> t.toInstant();
      case OffsetDateTime o -> o.toInstant();
      default -> throw new IllegalStateException("unexpected timestamp type " + value.getClass());
    };
  }

  @SuppressWarnings("unchecked")
  private Optional<LockoutRow> read(String sql, String scope, String hash) {
    List<Object[]> rows =
        em.createNativeQuery(sql).setParameter("s", scope).setParameter("h", hash).getResultList();
    return rows.stream()
        .findFirst()
        .map(r -> new LockoutRow(((Number) r[0]).intValue(), instant(r[1]), instant(r[2])));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<LockoutRow> find(String scope, String subjectHash) {
    return read(SELECT, Objects.requireNonNull(scope), Objects.requireNonNull(subjectHash));
  }

  @Override
  public Optional<LockoutRow> findForUpdate(String scope, String subjectHash) {
    return read(
        SELECT + " FOR UPDATE", Objects.requireNonNull(scope), Objects.requireNonNull(subjectHash));
  }

  @Override
  public void save(String scope, String subjectHash, LockoutRow row) {
    Objects.requireNonNull(row, "row cannot be null");
    em.createNativeQuery(
            "INSERT INTO login_lockout (scope, subject_hash, failures, last_failure_at, locked_until)"
                + " VALUES (:s, :h, :f, :l, CAST(:u AS TIMESTAMP WITH TIME ZONE))"
                + " ON CONFLICT (scope, subject_hash) DO UPDATE SET failures = :f,"
                + " last_failure_at = :l, locked_until = CAST(:u AS TIMESTAMP WITH TIME ZONE)")
        .setParameter("s", scope)
        .setParameter("h", subjectHash)
        .setParameter("f", row.failures())
        .setParameter("l", Timestamp.from(row.lastFailureAt()))
        .setParameter("u", row.lockedUntil() == null ? null : Timestamp.from(row.lockedUntil()))
        .executeUpdate();
  }

  @Override
  public void delete(String scope, String subjectHash) {
    em.createNativeQuery("DELETE FROM login_lockout WHERE scope = :s AND subject_hash = :h")
        .setParameter("s", scope)
        .setParameter("h", subjectHash)
        .executeUpdate();
  }

  @Override
  public int deleteIdleBefore(Instant before) {
    return em.createNativeQuery(
            "DELETE FROM login_lockout WHERE last_failure_at < :b"
                + " AND (locked_until IS NULL OR locked_until < :b)")
        .setParameter("b", Timestamp.from(before))
        .executeUpdate();
  }

  @Override
  @Transactional(readOnly = true)
  public LockoutSettingsRow settings() {
    Object[] r =
        (Object[])
            em.createNativeQuery(
                    "SELECT threshold, account_threshold, base_delay_seconds, max_delay_seconds,"
                        + " forget_after_seconds FROM lockout_settings WHERE id = 1")
                .getSingleResult();
    return new LockoutSettingsRow(
        ((Number) r[0]).intValue(),
        ((Number) r[1]).intValue(),
        ((Number) r[2]).longValue(),
        ((Number) r[3]).longValue(),
        ((Number) r[4]).longValue());
  }

  @Override
  public void saveSettings(LockoutSettingsRow row, String updatedBy) {
    em.createNativeQuery(
            "UPDATE lockout_settings SET threshold = :t, account_threshold = :a,"
                + " base_delay_seconds = :b, max_delay_seconds = :m, forget_after_seconds = :f,"
                + " updated_at = CURRENT_TIMESTAMP, updated_by = :u WHERE id = 1")
        .setParameter("t", row.threshold())
        .setParameter("a", row.accountThreshold())
        .setParameter("b", row.baseDelaySeconds())
        .setParameter("m", row.maxDelaySeconds())
        .setParameter("f", row.forgetAfterSeconds())
        .setParameter("u", updatedBy)
        .executeUpdate();
  }
}
