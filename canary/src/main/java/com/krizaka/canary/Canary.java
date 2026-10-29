package com.krizaka.canary;

import org.jspecify.annotations.Nullable;

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

  /**
   * A nullable input handled the way NullAway wants: tested before use.
   *
   * @param label a label, or null
   * @return the label, or the canary's name
   */
  public static String labelOrName(@Nullable String label) {
    return label == null ? name() : label;
  }
}
