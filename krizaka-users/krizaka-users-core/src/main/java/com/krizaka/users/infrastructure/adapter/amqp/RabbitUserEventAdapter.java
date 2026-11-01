package com.krizaka.users.infrastructure.adapter.amqp;

import com.krizaka.messaging.topology.MessagingExchanges;
import com.krizaka.users.domain.model.UserRegisteredEvent;
import com.krizaka.users.domain.ports.outbound.UserEventPublisher;
import com.krizaka.users.persistence.domain.model.OutboxMessage;
import com.krizaka.users.persistence.domain.ports.OutboxStore;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter for publishing user registration events downstream (AGENTS.md §6: {@code
 * evt.user.registered} on the platform's events exchange ({@code
 * krizaka.messaging.exchanges.events})). Lives in the identity module — the publisher of an
 * identity domain event belongs to the identity context, not to persistence-app.
 *
 * <p>The routing key is this context's contract; the exchange belongs to the platform the service
 * runs on (krizaka-messaging's {@code MessagingExchanges}). Published through the users outbox
 * (at-least-once, deduplicated downstream by {@code messageId}).
 */
@Component
class RabbitUserEventAdapter implements UserEventPublisher {

  private static final Logger logger = LoggerFactory.getLogger(RabbitUserEventAdapter.class);
  private static final String EVT_USER_REGISTERED = "evt.user.registered";

  private final OutboxStore outboxStore;
  private final String eventsExchange;

  RabbitUserEventAdapter(OutboxStore outboxStore, MessagingExchanges exchanges) {
    this.outboxStore = Objects.requireNonNull(outboxStore, "OutboxStore cannot be null");
    this.eventsExchange =
        Objects.requireNonNull(exchanges, "MessagingExchanges cannot be null").events();
  }

  @Override
  public void publish(UserRegisteredEvent event) {
    Objects.requireNonNull(event, "UserRegisteredEvent cannot be null");
    logger.info(
        "Publishing evt.user.registered event downstream for user: {}", event.user().email());
    outboxStore.append(
        new OutboxMessage(
            "user", event.user().id().toString(), eventsExchange, EVT_USER_REGISTERED, event));
  }
}
