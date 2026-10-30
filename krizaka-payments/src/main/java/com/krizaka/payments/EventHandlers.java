package com.krizaka.payments;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** One handler per event type the product applies. */
public final class EventHandlers {

  /**
   * A handler of one event type.
   *
   * @param <E> the event type
   */
  @FunctionalInterface
  public interface Handler<E extends PaymentEvent> {
    /**
     * Applies the event.
     *
     * @param event the verified event
     * @return true when applied, false when the event concerned no account of ours (ignored)
     */
    boolean handle(E event);
  }

  private final Map<Class<? extends PaymentEvent>, Handler<PaymentEvent>> handlers =
      new HashMap<>();

  /**
   * Registers the handler of a type.
   *
   * @param type the event type
   * @param handler its handler
   * @param <E> the event type
   * @return these handlers
   */
  public <E extends PaymentEvent> EventHandlers on(Class<E> type, Handler<E> handler) {
    handlers.put(type, event -> handler.handle(type.cast(event)));
    return this;
  }

  Optional<Handler<PaymentEvent>> forEvent(PaymentEvent event) {
    return Optional.ofNullable(handlers.get(event.getClass()));
  }
}
