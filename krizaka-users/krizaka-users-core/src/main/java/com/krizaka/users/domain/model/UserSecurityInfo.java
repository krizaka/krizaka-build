package com.krizaka.users.domain.model;

import java.util.Objects;

/**
 * Immutable domain record wrapping a resolved {@link User} along with their stored password hash
 * for authentication checks.
 */
public record UserSecurityInfo(User user, String passwordHash) {
  public UserSecurityInfo {
    Objects.requireNonNull(user, "User cannot be null");
  }
}
