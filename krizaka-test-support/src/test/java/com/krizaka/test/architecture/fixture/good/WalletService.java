package com.krizaka.test.architecture.fixture.good;

/** A well-formed service: named after its capability, dependencies through the constructor. */
public class WalletService {

  private final Object ledger;

  public WalletService(Object ledger) {
    this.ledger = ledger;
  }

  public Object ledger() {
    return ledger;
  }
}
