package com.krizaka.users.infrastructure.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.krizaka.users.domain.model.VerificationToken;
import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VerificationTokenMapperTest {
  private static final java.time.Clock FIXED_CLOCK =
      java.time.Clock.fixed(
          java.time.Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);

  @Test
  void toDomain_mapsAllFields() {
    Instant exp = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var dto =
        new VerificationTokenDto(
            "id-1", "user-1", "EMAIL_VERIFICATION", "hash", exp, false, Instant.now(FIXED_CLOCK));
    VerificationToken domain = VerificationTokenMapper.toDomain(dto);
    assertEquals("id-1", domain.id());
    assertEquals("user-1", domain.userId());
    assertEquals("EMAIL_VERIFICATION", domain.tokenType());
    assertEquals("hash", domain.tokenHash());
    assertEquals(exp, domain.expiryTimestamp());
    assertFalse(domain.used());
  }

  @Test
  void toDomain_null_returnsNull() {
    assertNull(VerificationTokenMapper.toDomain(null));
  }

  @Test
  void toDto_mapsAllFields() {
    Instant exp = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var domain = new VerificationToken("id-1", "user-1", "EMAIL_VERIFICATION", "hash", exp, false);
    VerificationTokenDto dto = VerificationTokenMapper.toDto(domain);
    assertEquals("id-1", dto.id());
    assertEquals("user-1", dto.userId());
    assertEquals("EMAIL_VERIFICATION", dto.tokenType());
    assertEquals("hash", dto.tokenHash());
    assertEquals(exp, dto.expiryTimestamp());
    assertFalse(dto.used());
  }

  @Test
  void toDto_null_returnsNull() {
    assertNull(VerificationTokenMapper.toDto(null));
  }

  @Test
  void roundTrip_preservesData() {
    Instant exp = Instant.now(FIXED_CLOCK).plusSeconds(3600);
    var original =
        new VerificationToken("id-1", "user-1", "EMAIL_VERIFICATION", "hash", exp, false);
    VerificationToken roundTripped =
        VerificationTokenMapper.toDomain(
            new VerificationTokenDto(
                "id-1",
                "user-1",
                "EMAIL_VERIFICATION",
                "hash",
                exp,
                false,
                Instant.now(FIXED_CLOCK)));
    assertEquals(original, roundTripped);
  }
}
