package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A single-use grant to release a user's provider key for one run (GHSA-3pf7-rgf2-4x3m). Carry it
 * with the run, redeem it once, never log it.
 *
 * @param grant the opaque secret
 * @param expiresAt when it stops being redeemable
 */
public record CredentialGrant(String grant, Instant expiresAt) {

  public CredentialGrant {
    Objects.requireNonNull(grant, "grant");
    Objects.requireNonNull(expiresAt, "expiresAt");
  }

  /** Never prints the secret. */
  @Override
  public String toString() {
    return "CredentialGrant[grant=<redacted>, expiresAt=" + expiresAt + "]";
  }
}
