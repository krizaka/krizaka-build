package com.krizaka.payments;

import org.jspecify.annotations.Nullable;

/**
 * The processor's price of a plan.
 *
 * @param amount integer minor units (cents), tax exclusive
 * @param currency lowercase ISO currency
 * @param interval the billing interval, {@code null} for a one-off price
 */
public record Price(long amount, String currency, @Nullable Interval interval) {

  /** How often a recurring price bills. */
  public enum Interval {
    DAY,
    WEEK,
    MONTH,
    YEAR
  }
}
