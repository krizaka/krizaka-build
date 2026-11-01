package com.krizaka.users.domain.model;

/** What a lockout counter is about. */
public enum LockoutScope {
  /** One account from one address: an attacker cannot lock a victim out from elsewhere. */
  PAIR,
  /** One account from anywhere: a botnet spreading its guesses is still stopped. */
  ACCOUNT
}
