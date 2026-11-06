package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.LockoutRow;
import com.krizaka.users.persistence.domain.model.LockoutSettingsRow;
import java.time.Instant;
import java.util.Optional;

/** Persistence port of the durable sign-in lockout (counters keyed by scope and hash). */
public interface LockoutPersistenceProvider {

  Optional<LockoutRow> find(String scope, String subjectHash);

  /** Reads the row and locks it until the end of the transaction. */
  Optional<LockoutRow> findForUpdate(String scope, String subjectHash);

  /** Inserts or replaces the counter. */
  void save(String scope, String subjectHash, LockoutRow row);

  void delete(String scope, String subjectHash);

  int deleteIdleBefore(Instant before);

  LockoutSettingsRow settings();

  void saveSettings(LockoutSettingsRow row, String updatedBy);
}
