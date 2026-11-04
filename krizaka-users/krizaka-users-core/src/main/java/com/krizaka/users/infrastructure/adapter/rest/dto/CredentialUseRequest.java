package com.krizaka.users.infrastructure.adapter.rest.dto;

/**
 * Redeems a grant for the run it was issued for.
 *
 * @param grant the single-use secret
 * @param runId the run it is bound to
 */
public record CredentialUseRequest(String grant, String runId) {

  @Override
  public String toString() {
    return "CredentialUseRequest[grant=<redacted>, runId=" + runId + "]";
  }
}
