package com.krizaka.canary.violation;

/** Returns null from a method whose package says nothing is null: NullAway must refuse it. */
public final class Violation {

  private Violation() {}

  /**
   * Breaks the contract.
   *
   * @return never null, says the package; null, says the body
   */
  public static String name() {
    return null;
  }
}
