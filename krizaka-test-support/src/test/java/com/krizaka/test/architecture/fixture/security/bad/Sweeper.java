package com.krizaka.test.architecture.fixture.security.bad;

import org.springframework.transaction.annotation.Transactional;

/** Reproduces krizaka-internal#2153: an unannotated overload delegating to a transactional one. */
public class Sweeper {

  @Transactional
  public int sweep(int batch) {
    return batch;
  }

  /** Reaches {@link #sweep(int)} through this: the proxy, and its transaction, are skipped. */
  public int sweep() {
    return sweep(100);
  }

  /** Not a violation: it calls a method that carries no transaction. */
  public int sweepAll() {
    return plain();
  }

  private int plain() {
    return 1;
  }
}
