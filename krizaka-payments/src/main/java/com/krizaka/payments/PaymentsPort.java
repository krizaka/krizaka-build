package com.krizaka.payments;

import org.jspecify.annotations.Nullable;

/**
 * The payments port. The adapter is chosen by the product's configuration; another processor is
 * another adapter.
 */
public interface PaymentsPort {

  /**
   * Creates the processor's customer for an account (idempotent per account).
   *
   * @param email the customer's email
   * @param accountId the product's account
   * @return the processor's customer id
   */
  String createCustomer(String email, String accountId);

  /**
   * A hosted payment page for a plan; the amount, the taxes and the invoice are the processor's.
   *
   * @param request the plan and the buyer
   * @return the page's URL
   */
  String checkout(CheckoutRequest request);

  /**
   * The hosted page where the customer manages the subscription and the invoices, and cancels.
   *
   * @param customerId the processor's customer
   * @param returnUrl where the customer returns
   * @param language the page's language
   * @return the page's URL
   */
  String portal(String customerId, String returnUrl, Language language);

  /**
   * The processor's current price of a plan (read by lookup key).
   *
   * @param kind the plan, named by the product
   * @return its price
   * @throws PaymentsException 404 when the plan or its price is unknown
   */
  Price price(String kind);

  /**
   * Verifies the notification's proof of origin over the raw body and translates it.
   *
   * @param rawBody the body, byte for byte as received
   * @param signature the signature header, {@code null} when absent
   * @return the event in the domain's words
   * @throws PaymentsException 400 on a missing, bad or replayed signature
   */
  PaymentEvent parseWebhook(String rawBody, @Nullable String signature);
}
