package com.krizaka.users.persistence.domain.ports;

import java.time.Instant;
import java.util.Optional;

/** Persistence port of the run-bound credential grants (GHSA-3pf7-rgf2-4x3m). */
public interface CredentialGrantPersistenceProvider {

  void save(String grantHash, String userId, String providerName, String runId, Instant expiresAt);

  /**
   * One conditional {@code UPDATE … RETURNING}: sets {@code used_at} only on an unused, unexpired
   * grant of this provider and run, under the row lock, so a grant is consumed at most once.
   *
   * @return the grant's user id when this call consumed it
   */
  Optional<String> consume(String grantHash, String providerName, String runId, Instant now);

  int deleteExpiredBefore(Instant before);
}
