package com.krizaka.payments.stripe;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The adapter's configuration, passed in: the module reads no environment.
 *
 * @param secretKey the API key ({@code sk_test_…} or {@code sk_live_…})
 * @param webhookSecret the webhook endpoint's signing secret
 * @param plans the plans, by the product's name for them
 * @param apiBase the API's base URL, {@code null} for Stripe's own (a test points it at a local
 *     server)
 */
public record StripeConfig(
    String secretKey,
    String webhookSecret,
    Map<String, PlanConfig> plans,
    @Nullable String apiBase) {

  /** Secrets are never printed. */
  @Override
  public String toString() {
    return "StripeConfig[plans=" + plans.keySet() + ", apiBase=" + apiBase + "]";
  }
}
