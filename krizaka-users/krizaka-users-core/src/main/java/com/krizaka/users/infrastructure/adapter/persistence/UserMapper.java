package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.persistence.domain.model.UserDto;
import java.util.UUID;

/**
 * Package-private static mapper isolating UserDto and User domain mapping boilerplate. Satisfies
 * ERR-107.
 */
final class UserMapper {

  private UserMapper() {}

  /** Maps UserDto to immutable domain User record. */
  static User toDomain(UserDto dto) {
    if (dto == null) {
      return null;
    }
    return new User(
        UUID.fromString(dto.id()),
        dto.username(),
        dto.email(),
        dto.enabled(),
        dto.authorities(),
        dto.preferences(),
        dto.interceptions(),
        dto.rateLimitTier());
  }

  /** Maps domain User to UserDto. */
  static UserDto toDto(User user, String passwordHash) {
    if (user == null) {
      return null;
    }
    return new UserDto(
        user.id().toString(),
        user.username(),
        passwordHash,
        user.email(),
        user.enabled(),
        user.preferences(),
        user.authorities(),
        user.activeInterceptions(),
        "local",
        null,
        user.rateLimitTier(),
        null);
  }

  /** Maps a domain User signing in through an external provider (no password) to UserDto. */
  static UserDto toFederatedDto(User user, String provider, String providerId) {
    return new UserDto(
        user.id().toString(),
        user.username(),
        null,
        user.email(),
        user.enabled(),
        user.preferences(),
        user.authorities(),
        user.activeInterceptions(),
        provider,
        providerId,
        user.rateLimitTier(),
        null);
  }
}
