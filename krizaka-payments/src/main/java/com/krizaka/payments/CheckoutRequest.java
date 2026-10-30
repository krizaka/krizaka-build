package com.krizaka.payments;

/**
 * A hosted payment page for a plan.
 *
 * @param kind the plan, named by the product
 * @param customerId the processor's customer
 * @param accountId the product's account, carried back by the webhook
 * @param successUrl where the buyer returns after paying
 * @param cancelUrl where the buyer returns after cancelling
 * @param language the page's language
 */
public record CheckoutRequest(
    String kind,
    String customerId,
    String accountId,
    String successUrl,
    String cancelUrl,
    Language language) {}
