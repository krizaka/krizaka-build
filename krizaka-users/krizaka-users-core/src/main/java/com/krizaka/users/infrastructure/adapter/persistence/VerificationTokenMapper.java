package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.VerificationToken;
import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private static mapper isolating VerificationTokenDto and VerificationToken domain
 * mapping. Satisfies ERR-107.
 */
final class VerificationTokenMapper {

  private VerificationTokenMapper() {}

  /** Maps VerificationTokenDto to clean domain record. */
  static VerificationToken toDomain(VerificationTokenDto dto) {
    if (dto == null) {
      return null;
    }
    return new VerificationToken(
        dto.id(),
        dto.userId(),
        dto.tokenType(),
        dto.tokenHash(),
        dto.expiryTimestamp(),
        dto.used());
  }

  /** Maps domain VerificationToken to VerificationTokenDto. */
  static VerificationTokenDto toDto(VerificationToken domain) {
    if (domain == null) {
      return null;
    }
    return new VerificationTokenDto(
        domain.id() != null ? domain.id() : UUID.randomUUID().toString(),
        domain.userId(),
        domain.tokenType() != null ? domain.tokenType() : "EMAIL_VERIFICATION",
        domain.tokenHash(),
        domain.expiryTimestamp(),
        domain.used(),
        Instant.now());
  }
}
