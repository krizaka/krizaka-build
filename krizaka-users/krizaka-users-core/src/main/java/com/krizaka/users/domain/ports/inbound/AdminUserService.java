package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.AdminAuditEntry;
import com.krizaka.users.domain.model.AdminUserPage;
import com.krizaka.users.domain.model.AdminUserView;
import java.util.List;
import java.util.Map;

/**
 * The administration of accounts: list and search, suspend and restore, the administrator role, Law
 * 25 export and erasure of the identity context's data — every action audited. An administrator
 * never acts on their own account.
 */
public interface AdminUserService {

  AdminUserPage list(String query, int page, int size);

  AdminUserView get(String id);

  /** Disables the account: it can no longer sign in or use a key. */
  AdminUserView suspend(AdminActor actor, String id);

  AdminUserView restore(AdminActor actor, String id);

  /** Grants or withdraws the administrator role; the last administrator keeps it. */
  AdminUserView setAdmin(AdminActor actor, String id, boolean admin);

  /**
   * Law 25 portability of what the identity context holds: the account and the audit lines about
   * it.
   */
  Map<String, Object> export(AdminActor actor, String id);

  /**
   * Law 25 erasure of the identity context's data (the account, its authorities, tokens, profile
   * and keys). The records other contexts keep under the same opaque id (jobs, wallets) are theirs
   * to purge.
   */
  void erase(AdminActor actor, String id, String confirmEmail);

  /** The latest audit lines, newest first; {@code targetId} limits them to one account. */
  List<AdminAuditEntry> audit(String targetId, int limit);
}
