package com.krizaka.users.domain.ports.inbound;

/** The rules of a new password (NIST SP 800-63B-4, ADR-016 decision 2). */
public interface PasswordPolicyService {

  /** Unicode NFKC: the form every password is hashed and compared in. */
  static String normalise(String password) {
    return java.text.Normalizer.normalize(password, java.text.Normalizer.Form.NFKC);
  }

  /**
   * Accepts a new password: NFKC-normalised, at least 15 characters (single factor; 8 once a second
   * factor is required), at most 256, no composition rule, not in a known breach.
   *
   * @return the normalised password, the one to hash
   * @throws com.krizaka.users.domain.exception.InvalidRequestException when it is refused
   */
  String accept(String rawPassword);

  /**
   * The password just proved right at sign-in is looked up in the breach corpus again (a password
   * can be breached after it was chosen). Fail-open: an unreachable service answers {@code false}.
   * A hit is audited (user id, never the password); the sign-in is not blocked here.
   *
   * @return whether the password appears in a known breach
   */
  boolean recheckAtSignIn(String userId, String rawPassword);
}
