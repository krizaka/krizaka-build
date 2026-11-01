package com.krizaka.users.persistence.domain.model;

import java.time.Instant;
import java.util.Set;

/** One account as the administration reads it: who it is and in what state — never a secret. */
public record AdminUserRow(
    String id,
    String username,
    String email,
    boolean enabled,
    String provider,
    Set<String> authorities,
    Instant createdAt) {

  public AdminUserRow {
    authorities = authorities == null ? Set.of() : Set.copyOf(authorities);
  }
}
