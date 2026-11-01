package com.krizaka.test.architecture.fixture.security.good;

import org.springframework.transaction.annotation.Transactional;

/** The cure: every entry point is annotated and shares a private, unannotated body. */
public class Sweeper {

  @Transactional
  public int sweep(int batch) {
    return run(batch);
  }

  @Transactional
  public int sweep() {
    return run(100);
  }

  private int run(int batch) {
    return batch;
  }
}
