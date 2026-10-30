package com.krizaka.payments.stripe;

import com.krizaka.payments.Price;
import org.jspecify.annotations.Nullable;

/**
 * One product and price a product sells, as data.
 *
 * @param lookupKey the price's lookup key
 * @param name the product's name
 * @param amount integer cents, tax exclusive (the processor adds the taxes)
 * @param currency lowercase ISO currency
 * @param interval the billing interval, {@code null} for a one-off price
 * @param taxCode the processor's tax code of the product (e.g. software as a service), or {@code
 *     null}
 */
public record CataloguePlan(
    String lookupKey,
    String name,
    long amount,
    String currency,
    Price.@Nullable Interval interval,
    @Nullable String taxCode) {}
