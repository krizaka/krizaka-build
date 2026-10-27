package com.krizaka.canary;

/** Smallest class a consumer can have; it must pass the quality checks with no baseline. */
public final class Canary {

  private Canary() {}

  /**
   * Returns the canary's name.
   *
   * @return the name
   */
  public static String name() {
    return "canary";
  }
}
