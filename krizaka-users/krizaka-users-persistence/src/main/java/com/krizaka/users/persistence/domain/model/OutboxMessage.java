package com.krizaka.users.persistence.domain.model;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable request to append a message to the identity outbox (AGENTS.md §6): committed with the
 * caller's transaction and published asynchronously by the identity relay. Context-local copy of
 * the platform outbox contract — no shared messaging jar across the service boundary.
 *
 * @param aggregateType the aggregate the message belongs to (e.g. {@code user})
 * @param aggregateId the aggregate identifier
 * @param exchange the target exchange (the events exchange)
 * @param routingKey the routing key ({@code evt.{aggregate}.{type}})
 * @param payload the message payload, serialized to JSON by the store
 * @param messageId the AMQP message id consumers deduplicate on; {@code null} lets the store choose
 *     one
 * @param headers the AMQP headers the message carries (the event envelope); empty when none
 */
public record OutboxMessage(
    String aggregateType,
    String aggregateId,
    String exchange,
    String routingKey,
    Object payload,
    UUID messageId,
    Map<String, String> headers) {

  public OutboxMessage {
    requireNonBlank(aggregateType, "aggregateType");
    requireNonBlank(aggregateId, "aggregateId");
    requireNonBlank(exchange, "exchange");
    requireNonBlank(routingKey, "routingKey");
    Objects.requireNonNull(payload, "payload cannot be null");
    headers = headers != null ? Map.copyOf(headers) : Map.of();
  }

  /**
   * A message whose id the store chooses, without headers.
   *
   * @param aggregateType the aggregate the message belongs to
   * @param aggregateId the aggregate identifier
   * @param exchange the target exchange
   * @param routingKey the routing key
   * @param payload the message payload
   */
  public OutboxMessage(
      String aggregateType,
      String aggregateId,
      String exchange,
      String routingKey,
      Object payload) {
    this(aggregateType, aggregateId, exchange, routingKey, payload, null, Map.of());
  }

  private static void requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " cannot be blank");
    }
  }
}
