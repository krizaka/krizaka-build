package com.krizaka.users.domain.model;

import java.time.Instant;
import java.util.Set;

/** An account as the administration sees it: identity and state, never a credential. */
public record AdminUserView(
    String id,
    String username,
    String email,
    boolean enabled,
    boolean admin,
    String provider,
    Set<String> authorities,
    Instant createdAt) {

  public AdminUserView {
    authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
  }
}
