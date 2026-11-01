package com.krizaka.users.persistence.application.service;

import com.krizaka.users.persistence.domain.model.AdminAuditRow;
import com.krizaka.users.persistence.domain.model.AdminUserRow;
import com.krizaka.users.persistence.domain.ports.AdminUserPersistenceProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package-private implementation of {@link AdminUserPersistenceProvider} (native SQL over the
 * identity tables).
 */
@Service
@Transactional
class AdminUserPersistenceProviderImpl implements AdminUserPersistenceProvider {

  private static final String ADMIN = "ROLE_ADMIN";
  private static final String SELECT =
      "SELECT u.id, u.username, u.email, u.enabled, u.provider, u.created_at FROM users u";

  @PersistenceContext private EntityManager em;

  private static String like(String query) {
    String q = query == null ? "" : query.trim().toLowerCase();
    return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<AdminUserRow> search(String query, int page, int size) {
    List<Object[]> rows =
        em.createNativeQuery(
                SELECT
                    + " WHERE lower(u.email) LIKE :q OR lower(u.username) LIKE :q"
                    + " ORDER BY u.created_at DESC NULLS LAST, u.id LIMIT :size OFFSET :offset")
            .setParameter("q", like(query))
            .setParameter("size", size)
            .setParameter("offset", (long) page * size)
            .getResultList();
    Map<String, Set<String>> authorities =
        authoritiesOf(rows.stream().map(r -> (String) r[0]).toList());
    return rows.stream().map(r -> toRow(r, authorities.get((String) r[0]))).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long count(String query) {
    return ((Number)
            em.createNativeQuery(
                    "SELECT count(*) FROM users u WHERE lower(u.email) LIKE :q OR lower(u.username) LIKE :q")
                .setParameter("q", like(query))
                .getSingleResult())
        .longValue();
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public Optional<AdminUserRow> find(String id) {
    List<Object[]> rows =
        em.createNativeQuery(SELECT + " WHERE u.id = :id").setParameter("id", id).getResultList();
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(toRow(rows.getFirst(), authoritiesOf(List.of(id)).get(id)));
  }

  @Override
  @Transactional(readOnly = true)
  public long countEnabledAdmins() {
    return ((Number)
            em.createNativeQuery(
                    "SELECT count(*) FROM users u JOIN authorities a ON a.user_id = u.id"
                        + " WHERE a.authority_name = :admin AND u.enabled = true")
                .setParameter("admin", ADMIN)
                .getSingleResult())
        .longValue();
  }

  @Override
  public void setEnabled(String id, boolean enabled) {
    em.createNativeQuery("UPDATE users SET enabled = :e WHERE id = :id")
        .setParameter("e", enabled)
        .setParameter("id", id)
        .executeUpdate();
  }

  @Override
  public void setAdmin(String id, boolean admin) {
    if (admin) {
      em.createNativeQuery(
              "INSERT INTO authorities (user_id, authority_name) VALUES (:id, :a)"
                  + " ON CONFLICT (user_id, authority_name) DO NOTHING")
          .setParameter("id", id)
          .setParameter("a", ADMIN)
          .executeUpdate();
    } else {
      em.createNativeQuery("DELETE FROM authorities WHERE user_id = :id AND authority_name = :a")
          .setParameter("id", id)
          .setParameter("a", ADMIN)
          .executeUpdate();
    }
  }

  @Override
  public void delete(String id) {
    em.createNativeQuery("DELETE FROM users WHERE id = :id").setParameter("id", id).executeUpdate();
  }

  @Override
  public void appendAudit(
      String actorId, String actorLabel, String action, String targetId, String detail) {
    insertAudit(actorId, actorLabel, action, targetId, detail, "ok");
  }

  @Override
  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
  public void appendAuditOutcome(
      String actorId,
      String actorLabel,
      String action,
      String targetId,
      String detail,
      String outcome) {
    insertAudit(actorId, actorLabel, action, targetId, detail, outcome);
  }

  private void insertAudit(
      String actorId,
      String actorLabel,
      String action,
      String targetId,
      String detail,
      String outcome) {
    em.createNativeQuery(
            "INSERT INTO admin_audit (actor_id, actor_label, action, target_id, detail, request_id, outcome)"
                + " VALUES (:actor, :label, :action, :target, :detail, :request, :outcome)")
        .setParameter("actor", actorId)
        .setParameter("label", actorLabel)
        .setParameter("action", action)
        .setParameter("target", targetId)
        .setParameter("detail", detail)
        // The request of the call (ADR-085): set in the logging context by the web layer, null
        // outside a request.
        .setParameter("request", org.slf4j.MDC.get("requestId"))
        .setParameter("outcome", outcome)
        .executeUpdate();
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<AdminAuditRow> recentAudit(String targetId, int limit) {
    String where = targetId == null ? "" : " WHERE target_id = :t OR actor_id = :t";
    var query =
        em.createNativeQuery(
                "SELECT id, at, actor_id, actor_label, action, target_id, detail, request_id, outcome FROM admin_audit"
                    + where
                    + " ORDER BY at DESC, id DESC LIMIT :limit")
            .setParameter("limit", limit);
    if (targetId != null) {
      query.setParameter("t", targetId);
    }
    List<Object[]> rows = query.getResultList();
    return rows.stream()
        .map(
            r ->
                new AdminAuditRow(
                    ((Number) r[0]).longValue(),
                    instant(r[1]),
                    (String) r[2],
                    (String) r[3],
                    (String) r[4],
                    (String) r[5],
                    (String) r[6],
                    (String) r[7],
                    (String) r[8]))
        .toList();
  }

  @Override
  public void anonymiseAudit(String actorId) {
    em.createNativeQuery(
            "UPDATE admin_audit SET actor_label = 'deleted account' WHERE actor_id = :id")
        .setParameter("id", actorId)
        .executeUpdate();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> authoritiesOf(List<String> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    List<Object[]> rows =
        em.createNativeQuery(
                "SELECT user_id, authority_name FROM authorities WHERE user_id IN (:ids)")
            .setParameter("ids", ids)
            .getResultList();
    return rows.stream()
        .collect(
            Collectors.groupingBy(
                r -> (String) r[0],
                Collectors.mapping(r -> (String) r[1], Collectors.toCollection(HashSet::new))));
  }

  private static AdminUserRow toRow(Object[] r, Set<String> authorities) {
    return new AdminUserRow(
        (String) r[0],
        (String) r[1],
        (String) r[2],
        r[3] == null || (Boolean) r[3],
        (String) r[4],
        authorities,
        instant(r[5]));
  }

  private static Instant instant(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Instant i) {
      return i;
    }
    if (value instanceof Timestamp t) {
      return t.toInstant();
    }
    if (value instanceof java.time.OffsetDateTime o) {
      return o.toInstant();
    }
    return Instant.parse(value.toString());
  }
}
