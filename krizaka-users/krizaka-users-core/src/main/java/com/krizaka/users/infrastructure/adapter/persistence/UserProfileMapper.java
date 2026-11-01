package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.UserProfile;
import com.krizaka.users.persistence.domain.model.UserProfileDto;
import java.util.Map;

/**
 * Package-private static mapper isolating UserProfileDto and UserProfile domain mapping
 * boilerplate. Satisfies ERR-107.
 */
final class UserProfileMapper {

  private UserProfileMapper() {}

  /** Maps UserProfileDto to clean domain record. */
  static UserProfile toDomain(UserProfileDto dto) {
    if (dto == null) {
      return null;
    }
    return new UserProfile(
        dto.userId(),
        dto.theme() == null ? "emerald" : dto.theme(),
        dto.rawPreferences() == null ? Map.of() : dto.rawPreferences());
  }

  /** Maps domain UserProfile to UserProfileDto. */
  static UserProfileDto toDto(UserProfile domain) {
    if (domain == null) {
      return null;
    }
    return new UserProfileDto(domain.userId(), domain.theme(), domain.attributes());
  }
}
