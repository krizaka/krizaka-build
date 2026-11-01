package com.krizaka.users.persistence.infrastructure.adapter.messaging;

import com.krizaka.messaging.outbox.NewOutboxMessage;
import com.krizaka.messaging.outbox.OutboxMessage;
import com.krizaka.users.persistence.domain.model.PendingOutboxEvent;
import com.krizaka.users.persistence.domain.ports.OutboxStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Hands the identity outbox to the krizaka-messaging relay (AGENTS.md §6).
 *
 * <p>The relay — claim a batch, publish each row with its {@code messageId} as the AMQP message id,
 * mark it, back off on failure, purge after the retention window — is krizaka-messaging's, and runs
 * on its own scheduler. This adapter only translates: the context's {@link OutboxStore} keeps its
 * schema and its claim ({@code FOR UPDATE SKIP LOCKED}), and a row's {@code Map} payload is
 * serialised to JSON at the moment it leaves.
 *
 * <p>It also takes the rows an {@code EventPublisher} writes ({@link #append}): the event's {@code
 * messageId} and envelope headers are stored as given, and published as stored.
 */
@Component("identityRelayOutboxStoreAdapter")
@ConditionalOnClass(RabbitTemplate.class)
class RelayOutboxStoreAdapter implements com.krizaka.messaging.outbox.OutboxStore {

  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  /** An event row carries no aggregate of its own: the type's aggregate, the message as its id. */
  private static final String EVENT_AGGREGATE = "event";

  private final OutboxStore outboxStore;
  private final ObjectMapper objectMapper;

  RelayOutboxStoreAdapter(OutboxStore outboxStore, ObjectMapper objectMapper) {
    this.outboxStore = Objects.requireNonNull(outboxStore, "OutboxStore cannot be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper cannot be null");
  }

  @Override
  public void append(NewOutboxMessage message) {
    UUID messageId = messageId(message.messageId());
    outboxStore.append(
        new com.krizaka.users.persistence.domain.model.OutboxMessage(
            aggregateOf(message.routingKey()),
            messageId.toString(),
            message.exchange(),
            message.routingKey(),
            objectMapper.readValue(message.body(), MAP_TYPE),
            messageId,
            message.headers()));
  }

  @Override
  public List<OutboxMessage> lockPendingBatch(int batchSize) {
    return outboxStore.lockPendingBatch(batchSize).stream().map(this::toRelayed).toList();
  }

  @Override
  public void markPublished(UUID id) {
    outboxStore.markPublished(id);
  }

  @Override
  public void recordFailure(UUID id, int previousAttempts) {
    outboxStore.recordFailure(id, previousAttempts);
  }

  @Override
  public long purgePublishedBefore(Instant cutoff) {
    return outboxStore.purgePublishedBefore(cutoff);
  }

  private OutboxMessage toRelayed(PendingOutboxEvent event) {
    return new OutboxMessage(
        event.id(),
        event.exchange(),
        event.routingKey(),
        event.messageId().toString(),
        objectMapper.writeValueAsBytes(event.payload()),
        event.attempts(),
        event.headers());
  }

  /** {@code evt.user.registered} → {@code user}; anything shorter is a plain event. */
  private static String aggregateOf(String routingKey) {
    String[] segments = routingKey.split("\\.");
    return segments.length > 2 && !segments[1].isBlank() ? segments[1] : EVENT_AGGREGATE;
  }

  /** The identity outbox stores message ids as UUIDs, which is what an EventPublisher writes. */
  private static UUID messageId(String messageId) {
    try {
      return UUID.fromString(messageId);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "identity_outbox.message_id is a UUID; refusing messageId '" + messageId + "'", e);
    }
  }
}
