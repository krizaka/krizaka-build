package com.krizaka.test.architecture.fixture.security.good.config;

/** The configuration layer may read the environment. */
public class EnvConfig {

  public String key() {
    return System.getenv("CRYPTO_KEY");
  }
}
