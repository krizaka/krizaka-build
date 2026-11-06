package com.krizaka.users.persistence.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Read view of an unpublished identity-outbox row handed to the relay.
 *
 * @param id the outbox row identifier
 * @param exchange the target exchange
 * @param routingKey the routing key
 * @param messageId the AMQP message id consumers deduplicate on (AGENTS.md §6)
 * @param payload the JSON payload as a map
 * @param attempts publish attempts so far
 * @param headers the AMQP headers the message carries; empty when none
 */
public record PendingOutboxEvent(
    UUID id,
    String exchange,
    String routingKey,
    UUID messageId,
    Map<String, Object> payload,
    int attempts,
    Map<String, String> headers) {

  public PendingOutboxEvent {
    Objects.requireNonNull(id, "id cannot be null");
    Objects.requireNonNull(exchange, "exchange cannot be null");
    Objects.requireNonNull(routingKey, "routingKey cannot be null");
    Objects.requireNonNull(messageId, "messageId cannot be null");
    payload = payload != null ? Map.copyOf(payload) : Map.of();
    if (attempts < 0) {
      throw new IllegalArgumentException("attempts cannot be negative");
    }
    headers = headers != null ? Map.copyOf(headers) : Map.of();
  }
}
