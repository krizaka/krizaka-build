package com.krizaka.users.domain.ports.outbound;

/** Outbound port defining role authority persistence operations. */
public interface AuthorityRepositoryPort {

  /**
   * Assigns a role authority to a specific user.
   *
   * @param userId The user ID UUID string.
   * @param authorityName The authority/role name (e.g. "ROLE_USER").
   */
  void saveAuthority(String userId, String authorityName);
}
