package com.krizaka.users.domain.ports.outbound;

/** Checks a password against the known breaches (Pwned Passwords, k-anonymity range API). */
public interface BreachedPasswordPort {

  /**
   * Whether the password appears in a known breach. Fail-open: when the service cannot be reached
   * the answer is {@code false} (logged), and the password is checked again at the next sign-in.
   */
  boolean isBreached(String password);
}
