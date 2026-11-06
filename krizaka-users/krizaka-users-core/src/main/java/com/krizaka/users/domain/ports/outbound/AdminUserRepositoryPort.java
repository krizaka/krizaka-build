package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.AdminAuditEntry;
import com.krizaka.users.domain.model.AdminUserView;
import java.util.List;
import java.util.Optional;

/** Outbound port of the administration: account search and state, and the audit trail. */
public interface AdminUserRepositoryPort {

  List<AdminUserView> search(String query, int page, int size);

  long count(String query);

  Optional<AdminUserView> find(String id);

  long countEnabledAdmins();

  void setEnabled(String id, boolean enabled);

  void setAdmin(String id, boolean admin);

  void delete(String id);

  void appendAudit(
      String actorId, String actorLabel, String action, String targetId, String detail);

  /**
   * An audit line for an action that did not happen: {@code outcome} is {@code refused} or {@code
   * failed}.
   */
  void appendAuditOutcome(
      String actorId,
      String actorLabel,
      String action,
      String targetId,
      String detail,
      String outcome);

  List<AdminAuditEntry> recentAudit(String targetId, int limit);

  void anonymiseAudit(String actorId);
}
