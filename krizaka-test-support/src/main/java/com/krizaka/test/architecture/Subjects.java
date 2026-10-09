package com.krizaka.test.architecture;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.Map;

/**
 * Guards a rule against judging nothing.
 *
 * <p>A rule that scans an empty population reports green and is believed: a misspelt path, a
 * renamed package or a moved file turns a guard into a decoration. Every rule that scans files or
 * classes itself passes its population through {@link #require} first.
 */
public final class Subjects {

  private Subjects() {}

  /**
   * Fails the calling rule when the population it is about to judge is empty.
   *
   * @param rule the rule's identifier, e.g. {@code CFG-001}
   * @param population what the rule scanned, in words
   * @param subjects the population itself
   * @param <T> the collection type, returned unchanged
   * @return {@code subjects}, so the call can wrap the scan
   */
  public static <T extends Collection<?>> T require(String rule, String population, T subjects) {
    if (subjects == null || subjects.isEmpty()) {
      fail(emptyMessage(rule, population));
    }
    return subjects;
  }

  /**
   * The same guard for a population held as a map.
   *
   * @param rule the rule's identifier
   * @param population what the rule scanned, in words
   * @param subjects the population
   * @param <T> the map type, returned unchanged
   * @return {@code subjects}
   */
  public static <T extends Map<?, ?>> T require(String rule, String population, T subjects) {
    if (subjects == null || subjects.isEmpty()) {
      fail(emptyMessage(rule, population));
    }
    return subjects;
  }

  private static String emptyMessage(String rule, String population) {
    return "["
        + rule
        + "] examined no "
        + population
        + " — a rule that judges the empty set reports green and is believed. Either the scan is"
        + " pointed at the wrong place, or what it guards has moved.";
  }
}
