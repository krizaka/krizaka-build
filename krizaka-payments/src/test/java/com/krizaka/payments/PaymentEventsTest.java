package com.krizaka.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PaymentEventsTest {

  private static final PaymentEvent PAID =
      new PaymentEvent.PaymentPaid("evt_1", "audit", "a1", "cus_1");

  /** A store that can forget, or not. */
  private static final class Store implements EventStore {
    final Set<String> seen = new HashSet<>();
    boolean failToForget;

    @Override
    public boolean markSeen(String id, String type) {
      return seen.add(id);
    }

    @Override
    public void forget(String id) {
      if (failToForget) {
        throw new IllegalStateException("cannot forget");
      }
      seen.remove(id);
    }
  }

  @Test
  void appliesOnceAndASecondDeliveryRunsNoHandler() {
    var store = new Store();
    List<String> ran = new ArrayList<>();
    var handlers = new EventHandlers().on(PaymentEvent.PaymentPaid.class, e -> ran.add(e.kind()));
    assertThat(PaymentEvents.applyOnce(store, PAID, handlers))
        .isEqualTo(PaymentEvents.Outcome.APPLIED);
    assertThat(PaymentEvents.applyOnce(store, PAID, handlers))
        .isEqualTo(PaymentEvents.Outcome.DUPLICATE);
    assertThat(ran).containsExactly("audit");
  }

  @Test
  void anIgnoredEventOrOneWithoutAHandlerIsNotRecorded() {
    var store = new Store();
    var handlers = new EventHandlers().on(PaymentEvent.PaymentPaid.class, e -> true);
    assertThat(PaymentEvents.applyOnce(store, new PaymentEvent.Ignored("evt_2"), handlers))
        .isEqualTo(PaymentEvents.Outcome.IGNORED);
    assertThat(
            PaymentEvents.applyOnce(
                store, new PaymentEvent.SubscriptionStarted("evt_3", "a", null, null), handlers))
        .isEqualTo(PaymentEvents.Outcome.IGNORED);
    assertThat(store.seen).isEmpty();
  }

  @Test
  void aHandlerMayAnswerThatTheEventConcernedNoAccountOfOurs() {
    var handlers = new EventHandlers().on(PaymentEvent.PaymentPaid.class, e -> false);
    assertThat(PaymentEvents.applyOnce(new Store(), PAID, handlers))
        .isEqualTo(PaymentEvents.Outcome.IGNORED);
  }

  @Test
  void aFailedHandlerGivesTheIdBackSoTheRetryApplies() {
    var store = new Store();
    boolean[] fail = {true};
    var handlers =
        new EventHandlers()
            .on(
                PaymentEvent.PaymentPaid.class,
                e -> {
                  if (fail[0]) {
                    throw new IllegalStateException("db down");
                  }
                  return true;
                });
    assertThatThrownBy(() -> PaymentEvents.applyOnce(store, PAID, handlers)).hasMessage("db down");
    assertThat(store.seen).isEmpty();
    fail[0] = false;
    assertThat(PaymentEvents.applyOnce(store, PAID, handlers))
        .isEqualTo(PaymentEvents.Outcome.APPLIED);
  }

  @Test
  void whenGivingTheIdBackFailsTooTheHandlersErrorComesFirstAndTheOtherIsSuppressed() {
    var store = new Store();
    store.failToForget = true;
    var handlers =
        new EventHandlers()
            .on(
                PaymentEvent.PaymentPaid.class,
                e -> {
                  throw new IllegalStateException("db down");
                });
    assertThatThrownBy(() -> PaymentEvents.applyOnce(store, PAID, handlers))
        .hasMessage("db down")
        .satisfies(
            e ->
                assertThat(e.getSuppressed())
                    .singleElement()
                    .extracting(Throwable::getMessage)
                    .isEqualTo("cannot forget"));
    assertThat(store.seen).containsExactly("evt_1");
  }
}
