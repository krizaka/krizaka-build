package com.krizaka.payments;

/** Where the ids of the notifications already applied are kept (one line per notification). */
public interface EventStore {

  /**
   * Records the id. Must be atomic (a unique key).
   *
   * @param id the event's id
   * @param type the event's type
   * @return true when the id is new, false when it was already there
   */
  boolean markSeen(String id, String type);

  /**
   * Gives the id back after its handler failed, so the processor's retry runs it. A store that does
   * not override it keeps the id.
   *
   * @param id the event's id
   */
  default void forget(String id) {
    // keeping the id is this store's choice
  }
}
