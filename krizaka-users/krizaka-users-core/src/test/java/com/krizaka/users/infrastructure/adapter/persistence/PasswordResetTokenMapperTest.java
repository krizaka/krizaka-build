package com.krizaka.users.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.krizaka.users.domain.model.PasswordResetToken;
import com.krizaka.users.persistence.domain.model.PasswordResetTokenDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PasswordResetTokenMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toDto_mapsAllFields() {
    Instant exp = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var domain = new PasswordResetToken("id-1", "john@test.com", "hash", exp);
    PasswordResetTokenDto dto = PasswordResetTokenMapper.toDto(domain);
    assertEquals("id-1", dto.id());
    assertEquals("john@test.com", dto.email());
    assertEquals("hash", dto.tokenHash());
    assertEquals(exp, dto.expiresAt());
  }

  @Test
  void toDomain_mapsAllFields() {
    Instant exp = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var dto = new PasswordResetTokenDto("id-1", "john@test.com", "hash", exp, null);
    PasswordResetToken domain = PasswordResetTokenMapper.toDomain(dto);
    assertEquals("id-1", domain.id());
    assertEquals("john@test.com", domain.email());
    assertEquals("hash", domain.tokenHash());
    assertEquals(exp, domain.expiresAt());
  }

  @Test
  void roundTrip_preservesData() {
    Instant exp = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var original = new PasswordResetToken("id-1", "john@test.com", "hash", exp);
    PasswordResetToken roundTripped =
        PasswordResetTokenMapper.toDomain(PasswordResetTokenMapper.toDto(original));
    assertEquals(original, roundTripped);
  }
}
