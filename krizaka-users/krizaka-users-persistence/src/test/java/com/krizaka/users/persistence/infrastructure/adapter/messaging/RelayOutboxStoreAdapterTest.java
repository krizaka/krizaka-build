package com.krizaka.users.persistence.infrastructure.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.*;

import com.krizaka.messaging.outbox.NewOutboxMessage;
import com.krizaka.messaging.outbox.OutboxMessage;
import com.krizaka.users.persistence.domain.model.PendingOutboxEvent;
import com.krizaka.users.persistence.domain.ports.OutboxStore;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class RelayOutboxStoreAdapterTest {

  private final OutboxStore outboxStore = mock(OutboxStore.class);
  private final RelayOutboxStoreAdapter adapter =
      new RelayOutboxStoreAdapter(outboxStore, JsonMapper.builder().build());

  @Test
  void aPendingRowLeavesWithItsRoutingItsMessageIdAndAJsonBody() {
    UUID id = UUID.randomUUID();
    UUID messageId = UUID.randomUUID();
    when(outboxStore.lockPendingBatch(10))
        .thenReturn(
            List.of(
                new PendingOutboxEvent(
                    id,
                    "platform.events",
                    "evt.user.registered",
                    messageId,
                    Map.of("email", "a@b.c"),
                    2,
                    Map.of("kz-type", "evt.user.registered"))));

    OutboxMessage relayed = adapter.lockPendingBatch(10).getFirst();

    assertThat(relayed.id()).isEqualTo(id);
    assertThat(relayed.exchange()).isEqualTo("platform.events");
    assertThat(relayed.routingKey()).isEqualTo("evt.user.registered");
    assertThat(relayed.messageId()).isEqualTo(messageId.toString());
    assertThat(relayed.attempts()).isEqualTo(2);
    assertThat(relayed.headers()).containsEntry("kz-type", "evt.user.registered");
    assertThat(new String(relayed.body(), StandardCharsets.UTF_8))
        .isEqualTo("{\"email\":\"a@b.c\"}");
  }

  @Test
  void bookkeepingIsDelegatedToTheContextsStore() {
    UUID id = UUID.randomUUID();
    Instant cutoff = Instant.now();
    when(outboxStore.purgePublishedBefore(cutoff)).thenReturn(4L);

    adapter.markPublished(id);
    adapter.recordFailure(id, 3);

    verify(outboxStore).markPublished(id);
    verify(outboxStore).recordFailure(id, 3);
    assertThat(adapter.purgePublishedBefore(cutoff)).isEqualTo(4);
  }

  @Test
  void anEventPublishersRowKeepsItsMessageIdAndEnvelope() {
    UUID messageId = UUID.randomUUID();

    adapter.append(
        new NewOutboxMessage(
            "platform.events",
            "evt.user.registered",
            messageId.toString(),
            "{\"email\":\"a@b.c\"}".getBytes(StandardCharsets.UTF_8),
            Map.of("kz-type", "evt.user.registered", "kz-version", "1")));

    ArgumentCaptor<com.krizaka.users.persistence.domain.model.OutboxMessage> appended =
        ArgumentCaptor.forClass(com.krizaka.users.persistence.domain.model.OutboxMessage.class);
    verify(outboxStore).append(appended.capture());
    var row = appended.getValue();
    assertThat(row.messageId()).isEqualTo(messageId);
    assertThat(row.aggregateType()).isEqualTo("user");
    assertThat(row.aggregateId()).isEqualTo(messageId.toString());
    assertThat(row.exchange()).isEqualTo("platform.events");
    assertThat(row.routingKey()).isEqualTo("evt.user.registered");
    assertThat(row.payload()).isEqualTo(Map.of("email", "a@b.c"));
    assertThat(row.headers())
        .containsEntry("kz-type", "evt.user.registered")
        .containsEntry("kz-version", "1");
  }

  @Test
  void aMessageIdTheTableCannotStoreIsRefused() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                adapter.append(
                    new NewOutboxMessage(
                        "platform.events",
                        "evt.user.registered",
                        "not-a-uuid",
                        "{}".getBytes(),
                        null)))
        .withMessageContaining("not-a-uuid");
  }
}
