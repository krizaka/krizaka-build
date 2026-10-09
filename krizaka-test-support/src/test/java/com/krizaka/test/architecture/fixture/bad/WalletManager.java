package com.krizaka.test.architecture.fixture.bad;

import org.springframework.beans.factory.annotation.Autowired;

/** Breaks every rule it can: a pattern name, field injection, standard output, a second class. */
public class WalletManager {

  @Autowired private Object ledger;

  public void report() {
    System.out.println(ledger);
  }
}

class TopUpRequest {}
