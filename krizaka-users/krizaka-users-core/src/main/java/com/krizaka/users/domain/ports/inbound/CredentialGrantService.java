package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.CredentialGrant;

/**
 * Run-bound release of a user's provider key (GHSA-3pf7-rgf2-4x3m). A decrypted key leaves the
 * identity context only here: against a short-lived, single-use grant bound to the user, the
 * provider and the run. A service token alone is never enough.
 */
public interface CredentialGrantService {

  /**
   * Issues a grant on the user's own authority (the service that owns the run forwards the user's
   * session): the user consents to this run using this provider key.
   *
   * @throws com.krizaka.users.domain.exception.CredentialNotFoundException when the user has no key
   *     for the provider
   */
  CredentialGrant issue(String userId, String providerName, String runId);

  /**
   * Redeems a grant once and answers the decrypted key, for the call it is needed for.
   *
   * @param caller the authenticated service redeeming it (audited)
   * @throws com.krizaka.users.domain.exception.CredentialGrantRefusedException when the grant is
   *     unknown, expired, used, or bound to another provider or run
   * @throws com.krizaka.users.domain.exception.CredentialNotFoundException when the key was deleted
   *     after the grant was issued
   */
  String redeem(String grant, String providerName, String runId, String caller);
}
