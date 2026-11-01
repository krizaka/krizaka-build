package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.LockoutPolicy;
import com.krizaka.users.domain.model.LockoutScope;
import com.krizaka.users.domain.model.LockoutState;
import java.time.Instant;
import java.util.Optional;

/** Outbound port of the durable lockout: counters and policy (subjects are hashes, never mail). */
public interface LockoutRepositoryPort {

  Optional<LockoutState> find(LockoutScope scope, String subjectHash);

  /** Reads and locks the counter for the rest of the transaction. */
  Optional<LockoutState> findForUpdate(LockoutScope scope, String subjectHash);

  void save(LockoutScope scope, String subjectHash, LockoutState state);

  void delete(LockoutScope scope, String subjectHash);

  /** Forgets the counters whose last failure is older than {@code before}; returns how many. */
  int purgeIdleBefore(Instant before);

  LockoutPolicy policy();

  void savePolicy(LockoutPolicy policy, String updatedBy);
}
