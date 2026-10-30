package com.krizaka.payments.stripe;

import com.krizaka.payments.PaymentsException;
import com.krizaka.payments.Price;
import com.stripe.StripeClient;
import com.stripe.param.PriceCreateParams;
import com.stripe.param.PriceListParams;
import com.stripe.param.PriceUpdateParams;
import com.stripe.param.ProductCreateParams;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Creates, idempotently, what the adapter reads as DATA: products and prices found by lookup key. A
 * price is immutable at the processor, so a changed amount, currency or interval creates a new
 * price that takes the lookup key over and retires the old one (existing subscriptions keep
 * theirs). Live mode is refused unless {@code owner} is true.
 */
public final class CatalogueSync {

  /** What happened to one plan. */
  public enum Status {
    EXISTS,
    CREATED,
    REPLACED
  }

  /**
   * The outcome for one plan.
   *
   * @param lookupKey the plan's lookup key
   * @param status what was done
   */
  public record Result(String lookupKey, Status status) {}

  private final StripeClient client;
  private final String product;
  private final boolean live;
  private final boolean owner;

  /**
   * Creates the sync.
   *
   * @param secretKey the API key; {@code sk_live_} means live mode
   * @param apiBase the API's base URL, {@code null} for Stripe's own
   * @param product marks what this catalogue created ({@code metadata.product})
   * @param owner true when the owner approved a live run
   */
  public CatalogueSync(String secretKey, @Nullable String apiBase, String product, boolean owner) {
    this.client = StripeClients.create(secretKey, apiBase);
    this.product = product;
    this.live = secretKey.startsWith("sk_live_");
    this.owner = owner;
  }

  /**
   * Brings the processor's prices to the plans.
   *
   * @param plans the plans, from the product's own data
   * @return one result per plan, in order
   * @throws PaymentsException in live mode without {@code owner}, or when the processor refuses a
   *     call
   */
  public List<Result> sync(List<CataloguePlan> plans) {
    if (live && !owner) {
      throw new PaymentsException(403, "Live mode is the owner's: refusing");
    }
    return plans.stream().map(this::syncOne).toList();
  }

  private Result syncOne(CataloguePlan plan) {
    List<com.stripe.model.Price> found =
        StripePayments.call(
            () ->
                client
                    .prices()
                    .list(
                        PriceListParams.builder()
                            .addLookupKey(plan.lookupKey())
                            .setActive(true)
                            .setLimit(1L)
                            .build())
                    .getData());
    if (found.isEmpty()) {
      String productId = StripePayments.call(() -> createProduct(plan));
      StripePayments.call(() -> createPrice(plan, productId, false));
      return new Result(plan.lookupKey(), Status.CREATED);
    }
    com.stripe.model.Price current = found.getFirst();
    if (StripeClients.toPrice(current)
        .equals(new Price(plan.amount(), plan.currency(), plan.interval()))) {
      return new Result(plan.lookupKey(), Status.EXISTS);
    }
    StripePayments.call(() -> createPrice(plan, current.getProduct(), true));
    StripePayments.call(
        () ->
            client
                .prices()
                .update(current.getId(), PriceUpdateParams.builder().setActive(false).build()));
    return new Result(plan.lookupKey(), Status.REPLACED);
  }

  private String createProduct(CataloguePlan plan) throws com.stripe.exception.StripeException {
    var params = ProductCreateParams.builder().setName(plan.name()).putMetadata("product", product);
    if (plan.taxCode() != null) {
      params.setTaxCode(plan.taxCode());
    }
    return client.products().create(params.build()).getId();
  }

  private com.stripe.model.Price createPrice(
      CataloguePlan plan, String productId, boolean takeKeyOver)
      throws com.stripe.exception.StripeException {
    var params =
        PriceCreateParams.builder()
            .setProduct(productId)
            .setCurrency(plan.currency())
            .setUnitAmount(plan.amount())
            .setTaxBehavior(PriceCreateParams.TaxBehavior.EXCLUSIVE)
            .setLookupKey(plan.lookupKey());
    if (takeKeyOver) {
      params.setTransferLookupKey(true);
    }
    if (plan.interval() != null) {
      params.setRecurring(
          PriceCreateParams.Recurring.builder()
              .setInterval(
                  PriceCreateParams.Recurring.Interval.valueOf(
                      plan.interval().name().toUpperCase(Locale.ROOT)))
              .build());
    }
    return client.prices().create(params.build());
  }
}
