package com.krizaka.test.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventContractTestTest extends EventContractTest {

  enum Kind {
    SMALL,
    LARGE
  }

  record SampleCreated(UUID id, String name, Kind kind, Instant occurredAt) {}

  record SampleCopy(String name) {}

  @Test
  void aConformingEventPassesAndReturnsItsJson() {
    String json =
        assertConforms(
            "evt.sample.created",
            1,
            new SampleCreated(
                UUID.randomUUID(), "first", Kind.LARGE, Instant.parse("2026-10-09T10:00:00Z")));

    assertThat(json).contains("\"occurredAt\":\"2026-10-09T10:00:00Z\"").contains("\"LARGE\"");
  }

  @Test
  void aMissingRequiredFieldFailsWithItsLocation() {
    assertThatThrownBy(
            () ->
                assertConforms(
                    "evt.sample.created",
                    1,
                    new SampleCreated(UUID.randomUUID(), null, Kind.SMALL, Instant.now())))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("does not conform")
        .hasMessageContaining("name");
  }

  @Test
  void aWrongTypeFails() {
    assertThatThrownBy(
            () ->
                assertJsonConforms(
                    "evt.sample.created",
                    1,
                    "{\"id\":\"" + UUID.randomUUID() + "\",\"name\":7,\"occurredAt\":\"x\"}"))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("name");
  }

  @Test
  void aConsumerCopyReadsAValidExample() {
    SampleCopy copy =
        assertReadable(
            "evt.sample.created",
            1,
            "{\"id\":\""
                + UUID.randomUUID()
                + "\",\"name\":\"n\",\"occurredAt\":\"2026-10-09T10:00:00Z\"}",
            SampleCopy.class);

    assertThat(copy.name()).isEqualTo("n");
  }

  @Test
  void anInvalidExampleIsRefusedBeforeTheCopyReadsIt() {
    assertThatThrownBy(
            () -> assertReadable("evt.sample.created", 1, "{\"name\":\"n\"}", SampleCopy.class))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("does not conform");
  }

  @Test
  void aMissingSchemaNamesItsPath() {
    assertThatThrownBy(() -> schema("evt.sample.unknown", 1))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("events/evt.sample.unknown.v1.json");
  }

  @Test
  void aSchemaOfAnotherDialectIsRefused() {
    assertThatThrownBy(() -> schema("evt.sample.legacy", 1))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("2020-12");
  }

  @Test
  void versionsStartAtOne() {
    assertThat(schemaPath("evt.user.registered", 2))
        .isEqualTo("events/evt.user.registered.v2.json");
    assertThatThrownBy(() -> schemaPath("evt.user.registered", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
