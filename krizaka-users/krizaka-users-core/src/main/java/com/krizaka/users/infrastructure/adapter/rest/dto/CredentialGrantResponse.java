package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.time.Instant;

/**
 * A grant issued for one run: answered once, never logged.
 *
 * @param grant the opaque single-use secret
 * @param expiresAt when it stops being redeemable
 */
public record CredentialGrantResponse(String grant, Instant expiresAt) {

  @Override
  public String toString() {
    return "CredentialGrantResponse[grant=<redacted>, expiresAt=" + expiresAt + "]";
  }
}
