package com.krizaka.payments;

/** Idempotent application of a verified event. */
public final class PaymentEvents {

  /** What {@link #applyOnce} did with an event. */
  public enum Outcome {
    APPLIED,
    DUPLICATE,
    IGNORED
  }

  private PaymentEvents() {}

  /**
   * Applies a verified event once: its id is recorded first, a replay runs no handler. An {@code
   * ignored} event, or one the product has no handler for, is not recorded. When the handler
   * throws, the id is given back ({@link EventStore#forget}) and the error propagates: the webhook
   * answers an error, the processor retries, and the retry applies the event. If giving the id back
   * fails too, the handler's error is thrown with the cleanup's as suppressed, and the id stays
   * recorded.
   *
   * @param store where applied ids are kept
   * @param event the verified, translated event
   * @param handlers one handler per event type the product applies
   * @return {@code APPLIED}, {@code DUPLICATE} (already applied) or {@code IGNORED}
   */
  public static Outcome applyOnce(EventStore store, PaymentEvent event, EventHandlers handlers) {
    var handler = handlers.forEvent(event);
    if (handler.isEmpty()) {
      return Outcome.IGNORED;
    }
    if (!store.markSeen(event.id(), event.type())) {
      return Outcome.DUPLICATE;
    }
    try {
      return handler.get().handle(event) ? Outcome.APPLIED : Outcome.IGNORED;
    } catch (RuntimeException error) {
      giveBack(store, event.id(), error);
      throw error;
    }
  }

  private static void giveBack(EventStore store, String id, RuntimeException handlerError) {
    try {
      store.forget(id);
    } catch (RuntimeException cleanupError) {
      handlerError.addSuppressed(cleanupError);
    }
  }
}
