package com.krizaka.test.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * Freezes the debt a rule finds today: the violations that exist are recorded in a store committed
 * with the module ({@code archunit_store/}), a <em>new</em> violation fails the build, and a fixed
 * one leaves the store the next time it is refreshed. The debt can only shrink.
 *
 * <p>The store is configured by this library's {@code archunit.properties}: a rule with no store
 * fails (the debt of a new rule is recorded on purpose), a new violation fails, a fixed one is
 * dropped from the store, which is then committed. To record the debt of a new rule, run the
 * module's tests once with {@code -Darchunit.freeze.store.default.allowStoreCreation=true}, review
 * and commit {@code archunit_store/}.
 */
public final class FrozenRules {

  private FrozenRules() {}

  /**
   * Checks {@code rule} against the frozen store: only violations absent from it fail.
   *
   * @param rule the rule to freeze
   * @param classes the imported production classes
   */
  public static void check(ArchRule rule, JavaClasses classes) {
    FreezingArchRule.freeze(rule).check(classes);
  }
}
