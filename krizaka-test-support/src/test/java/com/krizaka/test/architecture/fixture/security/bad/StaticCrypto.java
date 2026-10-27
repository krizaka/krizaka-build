package com.krizaka.test.architecture.fixture.security.bad;

/**
 * Reproduces krizaka-users#57: crypto state held in a static mutable field, read from the
 * environment.
 */
public class StaticCrypto {

  private static String key = System.getenv("CRYPTO_KEY");

  public String key() {
    return key;
  }
}
