package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.AdminAuditEntry;
import com.krizaka.users.domain.model.AdminUserView;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.persistence.domain.model.AdminUserRow;
import com.krizaka.users.persistence.domain.ports.AdminUserPersistenceProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Outbound adapter of the administration: maps the persistence rows to the domain views. */
@Component
class AdminUserRepositoryAdapter implements AdminUserRepositoryPort {

  private static final String ADMIN = "ROLE_ADMIN";

  private final AdminUserPersistenceProvider provider;

  AdminUserRepositoryAdapter(AdminUserPersistenceProvider provider) {
    this.provider = Objects.requireNonNull(provider, "AdminUserPersistenceProvider");
  }

  private static AdminUserView view(AdminUserRow r) {
    return new AdminUserView(
        r.id(),
        r.username(),
        r.email(),
        r.enabled(),
        r.authorities().contains(ADMIN),
        r.provider(),
        r.authorities(),
        r.createdAt());
  }

  @Override
  public List<AdminUserView> search(String query, int page, int size) {
    return provider.search(query, page, size).stream()
        .map(AdminUserRepositoryAdapter::view)
        .toList();
  }

  @Override
  public long count(String query) {
    return provider.count(query);
  }

  @Override
  public Optional<AdminUserView> find(String id) {
    return provider.find(id).map(AdminUserRepositoryAdapter::view);
  }

  @Override
  public long countEnabledAdmins() {
    return provider.countEnabledAdmins();
  }

  @Override
  public void setEnabled(String id, boolean enabled) {
    provider.setEnabled(id, enabled);
  }

  @Override
  public void setAdmin(String id, boolean admin) {
    provider.setAdmin(id, admin);
  }

  @Override
  public void delete(String id) {
    provider.delete(id);
  }

  @Override
  public void appendAudit(
      String actorId, String actorLabel, String action, String targetId, String detail) {
    provider.appendAudit(actorId, actorLabel, action, targetId, detail);
  }

  @Override
  public void appendAuditOutcome(
      String actorId,
      String actorLabel,
      String action,
      String targetId,
      String detail,
      String outcome) {
    provider.appendAuditOutcome(actorId, actorLabel, action, targetId, detail, outcome);
  }

  @Override
  public List<AdminAuditEntry> recentAudit(String targetId, int limit) {
    return provider.recentAudit(targetId, limit).stream()
        .map(
            r ->
                new AdminAuditEntry(
                    r.id(),
                    r.at(),
                    r.actorLabel(),
                    r.action(),
                    r.targetId(),
                    r.detail(),
                    r.requestId(),
                    r.outcome()))
        .toList();
  }

  @Override
  public void anonymiseAudit(String actorId) {
    provider.anonymiseAudit(actorId);
  }
}
