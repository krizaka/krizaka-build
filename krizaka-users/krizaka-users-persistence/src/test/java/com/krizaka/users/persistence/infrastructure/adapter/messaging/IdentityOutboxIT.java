package com.krizaka.users.persistence.infrastructure.adapter.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.messaging.outbox.NewOutboxMessage;
import com.krizaka.messaging.outbox.OutboxMessage;
import com.krizaka.messaging.outbox.OutboxStore;
import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.test.sql.InitDb;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.OutboxEventEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The identity outbox against its <b>real</b> table ({@code infra/initdb/10-identity.sql}): a row
 * written through krizaka-messaging's {@code OutboxStore.append} keeps the event's message id and
 * envelope headers, and the relay reads them back exactly.
 */
@SpringBootTest(
    classes = IdentityOutboxIT.App.class,
    properties = {
      "krizaka.users.crypto.key=test-crypto-key-0123456789abcdef!",
      "krizaka.users.crypto.salt=0123456789abcdef",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.flyway.enabled=false",
      "krizaka.messaging.outbox.enabled=false",
      "krizaka.messaging.consumer.enabled=false",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
    })
class IdentityOutboxIT extends AbstractContainerIntegrationTest {

  @Autowired
  @Qualifier("identityRelayOutboxStoreAdapter")
  OutboxStore relayStore;

  @Autowired TransactionTemplate transactions;
  @Autowired JdbcTemplate jdbc;

  /** Creates {@code identity_outbox} exactly as the context's bootstrap SQL declares it. */
  @BeforeAll
  static void createTheRealTable() throws Exception {
    String sql =
        Files.readString(
            InitDb.locate(Path.of(System.getProperty("user.dir"))).resolve("10-identity.sql"));
    Matcher table =
        Pattern.compile("CREATE TABLE identity_outbox \\(.*?\\n\\);", Pattern.DOTALL).matcher(sql);
    assertThat(table.find()).as("identity_outbox in 10-identity.sql").isTrue();
    try (var connection =
            java.sql.DriverManager.getConnection(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
        var statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS identity_outbox");
      statement.execute(table.group());
    }
  }

  @Test
  void anEventPublishersRowIsRelayedWithItsMessageIdAndEnvelope() {
    String messageId = UUID.randomUUID().toString();
    Map<String, String> envelope =
        Map.of(
            "kz-type", "evt.user.registered",
            "kz-version", "1",
            "kz-producer", "krizaka-users");

    transactions.executeWithoutResult(
        status ->
            relayStore.append(
                new NewOutboxMessage(
                    "platform.events",
                    "evt.user.registered",
                    messageId,
                    "{\"email\":\"a@b.c\"}".getBytes(StandardCharsets.UTF_8),
                    envelope)));

    List<OutboxMessage> batch = transactions.execute(status -> relayStore.lockPendingBatch(10));
    assertThat(batch)
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.messageId()).isEqualTo(messageId);
              assertThat(row.exchange()).isEqualTo("platform.events");
              assertThat(row.routingKey()).isEqualTo("evt.user.registered");
              assertThat(row.headers()).isEqualTo(envelope);
              assertThat(new String(row.body(), StandardCharsets.UTF_8))
                  .isEqualTo("{\"email\":\"a@b.c\"}");
            });
    assertThat(
            jdbc.queryForObject(
                "SELECT aggregate_type FROM identity_outbox WHERE message_id = ?::uuid",
                String.class,
                messageId))
        .isEqualTo("user");
  }

  /** The outbox beans of this module, on the real table, without the rest of persistence. */
  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EnableJpaRepositories(
      basePackageClasses = OutboxEventRepository.class,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = OutboxEventRepository.class))
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.(OutboxStoreImpl|RelayOutboxStoreAdapter)"))
  static class App {

    /** The outbox entity only: the other entities need the context's encryption key. */
    @org.springframework.context.annotation.Bean
    org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes persistenceManagedTypes() {
      return org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes.of(
          OutboxEventEntity.class.getName());
    }

    @org.springframework.context.annotation.Bean
    tools.jackson.databind.json.JsonMapper jsonMapper() {
      return tools.jackson.databind.json.JsonMapper.builder().build();
    }
  }
}
