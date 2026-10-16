package com.krizaka.test.architecture.fixture.arch.cycle.beta;

import com.krizaka.test.architecture.fixture.arch.cycle.alpha.Alpha;

/** Beta needs Alpha. */
public class Beta {
  public Alpha alpha() {
    return new Alpha();
  }
}
