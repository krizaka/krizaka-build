package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.AdminAuditRow;
import com.krizaka.users.persistence.domain.model.AdminUserRow;
import java.util.List;
import java.util.Optional;

/** Persistence port of the administration: account search and state, and its audit trail. */
public interface AdminUserPersistenceProvider {

  /**
   * A page of accounts whose username or e-mail contains {@code query} (blank = all), newest first.
   */
  List<AdminUserRow> search(String query, int page, int size);

  long count(String query);

  Optional<AdminUserRow> find(String id);

  /** Enabled accounts holding {@code ROLE_ADMIN}. */
  long countEnabledAdmins();

  void setEnabled(String id, boolean enabled);

  /** Grants or withdraws {@code ROLE_ADMIN}. */
  void setAdmin(String id, boolean admin);

  /**
   * Deletes the account; the identity context's tables cascade (authorities, tokens, profile,
   * keys).
   */
  void delete(String id);

  void appendAudit(
      String actorId, String actorLabel, String action, String targetId, String detail);

  /**
   * A line for an action that did not happen ({@code refused}, {@code failed}), written in its own
   * transaction so it survives the rollback of the action it describes.
   */
  void appendAuditOutcome(
      String actorId,
      String actorLabel,
      String action,
      String targetId,
      String detail,
      String outcome);

  /**
   * The latest lines of the trail, newest first; with {@code targetId}, only those about that
   * account.
   */
  List<AdminAuditRow> recentAudit(String targetId, int limit);

  /** Removes the person from the lines they acted in: the label becomes {@code deleted account}. */
  void anonymiseAudit(String actorId);
}
