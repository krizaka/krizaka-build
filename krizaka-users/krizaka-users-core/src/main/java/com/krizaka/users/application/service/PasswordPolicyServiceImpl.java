package com.krizaka.users.application.service;

import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.ports.inbound.PasswordPolicyService;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.domain.ports.outbound.BreachedPasswordPort;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Implements {@link PasswordPolicyService}: length in characters, then the breach check. */
@Service
class PasswordPolicyServiceImpl implements PasswordPolicyService {

  private static final Logger LOG = LoggerFactory.getLogger(PasswordPolicyServiceImpl.class);

  /** Single factor: NIST SP 800-63B-4 asks for 15. */
  // @fixed: the NIST SP 800-63B-4 minimum for a password that is the only factor
  static final int MIN_LENGTH = 15;

  // @fixed: the NIST SP 800-63B-4 maximum a verifier must accept is 64; 256 is the ADR-016 cap
  static final int MAX_LENGTH = 256;

  private final BreachedPasswordPort breaches;
  private final AdminUserRepositoryPort audit;

  PasswordPolicyServiceImpl(BreachedPasswordPort breaches, AdminUserRepositoryPort audit) {
    this.breaches = Objects.requireNonNull(breaches, "BreachedPasswordPort");
    this.audit = Objects.requireNonNull(audit, "AdminUserRepositoryPort");
  }

  @Override
  public boolean recheckAtSignIn(String userId, String rawPassword) {
    if (!breaches.isBreached(PasswordPolicyService.normalise(rawPassword))) {
      return false;
    }
    LOG.warn("Signed in with a password found in a known breach: userId={}", userId);
    audit.appendAudit(
        userId,
        "system",
        "identity.password_breached_at_signin",
        userId,
        "source=pwned-passwords-range");
    return true;
  }

  @Override
  public String accept(String rawPassword) {
    Objects.requireNonNull(rawPassword, "password");
    String password = PasswordPolicyService.normalise(rawPassword);
    int length = password.codePointCount(0, password.length());
    if (length < MIN_LENGTH) {
      throw new InvalidRequestException("Password must be at least " + MIN_LENGTH + " characters");
    }
    if (length > MAX_LENGTH) {
      throw new InvalidRequestException("Password must be at most " + MAX_LENGTH + " characters");
    }
    if (breaches.isBreached(password)) {
      throw new InvalidRequestException(
          "This password appears in a known data breach: choose another one");
    }
    return password;
  }
}
