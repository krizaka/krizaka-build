package com.krizaka.users.infrastructure.adapter.amqp;

import com.krizaka.messaging.topology.MessagingExchanges;
import com.krizaka.users.domain.model.PasswordResetRequestedEvent;
import com.krizaka.users.domain.ports.outbound.PasswordEventPublisher;
import com.krizaka.users.persistence.domain.model.OutboxMessage;
import com.krizaka.users.persistence.domain.ports.OutboxStore;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AMQP adapter for publishing password reset events downstream (AGENTS.md §6: {@code
 * evt.password.reset} on the platform's events exchange ({@code
 * krizaka.messaging.exchanges.events})). Lives in the identity module — the publisher of an
 * identity domain event belongs to the identity context, not to persistence-app.
 *
 * <p>The routing key is this context's contract; the exchange belongs to the platform the service
 * runs on (krizaka-messaging's {@code MessagingExchanges}). Published through the users outbox
 * (at-least-once, deduplicated downstream by {@code messageId}).
 */
@Component
class RabbitPasswordEventAdapter implements PasswordEventPublisher {

  private static final Logger logger = LoggerFactory.getLogger(RabbitPasswordEventAdapter.class);
  private static final String EVT_PASSWORD_RESET = "evt.password.reset";

  private final OutboxStore outboxStore;
  private final String eventsExchange;

  RabbitPasswordEventAdapter(OutboxStore outboxStore, MessagingExchanges exchanges) {
    this.outboxStore = Objects.requireNonNull(outboxStore, "OutboxStore cannot be null");
    this.eventsExchange =
        Objects.requireNonNull(exchanges, "MessagingExchanges cannot be null").events();
  }

  @Override
  public void publish(PasswordResetRequestedEvent event) {
    Objects.requireNonNull(event, "PasswordResetRequestedEvent cannot be null");
    logger.info("Publishing evt.password.reset event downstream for email: {}", event.email());
    outboxStore.append(
        new OutboxMessage("password", event.email(), eventsExchange, EVT_PASSWORD_RESET, event));
  }
}
