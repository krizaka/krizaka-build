package com.krizaka.payments.stripe;

/**
 * How a plan is bought, and where its price is found.
 *
 * @param lookupKey the price's lookup key at the processor (prices are its data)
 * @param mode a one-off payment or a subscription
 */
public record PlanConfig(String lookupKey, Mode mode) {

  /** How a plan is bought. */
  public enum Mode {
    PAYMENT,
    SUBSCRIPTION
  }
}
