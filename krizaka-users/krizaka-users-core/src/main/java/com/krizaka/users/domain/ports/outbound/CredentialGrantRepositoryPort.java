package com.krizaka.users.domain.ports.outbound;

import java.time.Instant;
import java.util.Optional;

/** Outbound port of the run-bound credential grants: only their SHA-256 hash is stored. */
public interface CredentialGrantRepositoryPort {

  void save(String grantHash, String userId, String providerName, String runId, Instant expiresAt);

  /**
   * Marks the grant used, in one statement that holds the row lock, if and only if it is unused,
   * unexpired at {@code now} and bound to this provider and run.
   *
   * @return the user the grant was issued for, or empty when it was not consumed
   */
  Optional<String> consume(String grantHash, String providerName, String runId, Instant now);

  /** Deletes grants that expired before {@code before}. */
  int purgeExpiredBefore(Instant before);
}
