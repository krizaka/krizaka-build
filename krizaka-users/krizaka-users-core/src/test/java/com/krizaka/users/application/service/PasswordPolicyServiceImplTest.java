package com.krizaka.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import org.junit.jupiter.api.Test;

/**
 * NIST SP 800-63B-4 through ADR-016 decision 2: 15 characters alone, 256 at most, NFKC, breach
 * check.
 */
class PasswordPolicyServiceImplTest {

  private final PasswordPolicyServiceImpl open =
      new PasswordPolicyServiceImpl(p -> false, mock(AdminUserRepositoryPort.class));

  @Test
  void fifteenCharactersAreEnoughAndFourteenAreNot() {
    assertThat(open.accept("a".repeat(15))).hasSize(15);
    assertThatThrownBy(() -> open.accept("a".repeat(14)))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("15");
  }

  @Test
  void noCompositionRule() {
    assertThat(open.accept("correct horse battery")).isEqualTo("correct horse battery");
    assertThat(open.accept("aaaaaaaaaaaaaaaa")).isNotBlank();
  }

  @Test
  void twoHundredFiftySixIsTheCeiling() {
    assertThat(open.accept("a".repeat(256))).hasSize(256);
    assertThatThrownBy(() -> open.accept("a".repeat(257)))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("256");
  }

  @Test
  void lengthCountsCharactersNotBytesOrUtf16Units() {
    // 15 emoji = 30 UTF-16 units, 60 bytes, 15 characters
    assertThat(open.accept("😀".repeat(15))).isNotBlank();
    assertThatThrownBy(() -> open.accept("😀".repeat(14)))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  void theReturnedPasswordIsNormalisedNfkc() {
    // full-width letters fold to ASCII
    String fullWidth = "ｐａｓｓｗｏｒｄ－ｐａｓｓｗｏｒｄ";
    assertThat(open.accept(fullWidth)).isEqualTo("password-password");
    // 'e' + combining acute = 'é'
    assertThat(open.accept("caf" + "é" + "-au-lait-noir")).isEqualTo("café-au-lait-noir");
  }

  @Test
  void aBreachedPasswordIsRefused() {
    var breached =
        new PasswordPolicyServiceImpl(
            "correct horse battery"::equals, mock(AdminUserRepositoryPort.class));
    assertThatThrownBy(() -> breached.accept("correct horse battery"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("breach");
    assertThat(breached.accept("correct horse battery staple")).isNotBlank();
  }

  @Test
  void aBreachedPasswordIsSpottedAtSignInAndAuditedWithoutItsValue() {
    var audit = mock(AdminUserRepositoryPort.class);
    var service = new PasswordPolicyServiceImpl("short old password"::equals, audit);

    assertThat(service.recheckAtSignIn("u-1", "short old password")).isTrue();

    verify(audit)
        .appendAudit(
            org.mockito.ArgumentMatchers.eq("u-1"),
            org.mockito.ArgumentMatchers.eq("system"),
            org.mockito.ArgumentMatchers.eq("identity.password_breached_at_signin"),
            org.mockito.ArgumentMatchers.eq("u-1"),
            org.mockito.ArgumentMatchers.argThat(d -> !d.contains("short old password")));
  }

  @Test
  void aCleanPasswordAtSignInLeavesNoTraceAndIsNotHeldToTheLengthRule() {
    var audit = mock(AdminUserRepositoryPort.class);
    var service = new PasswordPolicyServiceImpl(p -> false, audit);

    assertThat(service.recheckAtSignIn("u-1", "old")).isFalse();

    verify(audit, never()).appendAudit(any(), any(), any(), any(), any());
  }
}
