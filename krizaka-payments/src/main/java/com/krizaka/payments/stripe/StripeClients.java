package com.krizaka.payments.stripe;

import com.krizaka.payments.Price;
import com.stripe.StripeClient;
import org.jspecify.annotations.Nullable;

/** Builds the SDK client and reads its prices; shared by the adapter and the catalogue sync. */
final class StripeClients {

  private StripeClients() {}

  static StripeClient create(String secretKey, @Nullable String apiBase) {
    var builder = StripeClient.builder().setApiKey(secretKey);
    if (apiBase != null) {
      builder.setApiBase(apiBase);
    }
    return builder.build();
  }

  static Price toPrice(com.stripe.model.Price row) {
    var recurring = row.getRecurring();
    return new Price(
        row.getUnitAmount() == null ? 0L : row.getUnitAmount(),
        row.getCurrency() == null ? "usd" : row.getCurrency(),
        recurring == null
            ? null
            : Price.Interval.valueOf(recurring.getInterval().toUpperCase(java.util.Locale.ROOT)));
  }
}
