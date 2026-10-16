package com.krizaka.test.architecture.fixture.arch.cycle.alpha;

import com.krizaka.test.architecture.fixture.arch.cycle.beta.Beta;

/** Alpha needs Beta. */
public class Alpha {
  public Beta beta() {
    return new Beta();
  }
}
