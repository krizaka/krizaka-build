package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.OutboxMessage;
import com.krizaka.users.persistence.domain.model.PendingOutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The identity context's transactional outbox (AGENTS.md §6, INTERFACES.md §9): identity domain
 * events ({@code evt.user.*}, {@code evt.password.*}) are appended inside the business transaction
 * and published by the identity relay with the row's {@code messageId}.
 */
public interface OutboxStore {

  /** Appends a message; joins the caller's transaction when one is active. */
  void append(OutboxMessage message);

  /**
   * Locks and returns the next batch of unpublished events whose backoff has elapsed ({@code FOR
   * UPDATE SKIP LOCKED}); must run inside an active transaction.
   */
  List<PendingOutboxEvent> lockPendingBatch(int batchSize);

  /** Marks an event as published (same transaction as the lock). */
  void markPublished(UUID eventId);

  /** Records a failed publish attempt with exponential backoff (same transaction). */
  void recordFailure(UUID eventId, int previousAttempts);

  /** Deletes published events older than the cutoff. */
  long purgePublishedBefore(Instant cutoff);
}
