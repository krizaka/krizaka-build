package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.User;

/**
 * Issues claims-enriched session JWTs (MICROSERVICES_TARGET_ARCHITECTURE Phase 2): downstream
 * services authorize locally from the claims ({@code sub}, roles, tier) — the hot path never makes
 * a synchronous identity hop.
 */
public interface TokenService {

  /**
   * Issues a signed session token for an authenticated user.
   *
   * @param user the authenticated user
   * @return the compact JWT (treated as an opaque bearer token by every client)
   */
  String issue(User user);
}
