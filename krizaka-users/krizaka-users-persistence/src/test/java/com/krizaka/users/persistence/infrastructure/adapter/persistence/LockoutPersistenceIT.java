package com.krizaka.users.persistence.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.users.persistence.domain.model.LockoutRow;
import com.krizaka.users.persistence.domain.model.LockoutSettingsRow;
import com.krizaka.users.persistence.domain.ports.LockoutPersistenceProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

/**
 * The durable lockout tables of ADR-016 slice 2 ({@code V6__login_lockout.sql}): counters
 * round-trip as stored, the policy is seeded and replaceable, idle counters are purged.
 */
@SpringBootTest(
    classes = LockoutPersistenceIT.App.class,
    properties = {
      "krizaka.users.crypto.key=test-crypto-key-0123456789abcdef!",
      "krizaka.users.crypto.salt=0123456789abcdef",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.flyway.enabled=false",
      "krizaka.messaging.outbox.enabled=false",
      "krizaka.messaging.consumer.enabled=false",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
          + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
    })
class LockoutPersistenceIT extends AbstractContainerIntegrationTest {

  @Autowired LockoutPersistenceProvider provider;

  @BeforeAll
  static void migrate() throws Exception {
    String sql;
    try (var in =
        LockoutPersistenceIT.class.getResourceAsStream("/db/migration/V6__login_lockout.sql")) {
      sql = new String(java.util.Objects.requireNonNull(in).readAllBytes());
    }
    try (var connection =
            java.sql.DriverManager.getConnection(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
        var statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS login_lockout, lockout_settings");
      statement.execute(sql);
      statement.execute(sql); // idempotent: a second start changes nothing
    }
  }

  @Test
  void aCounterRoundTripsIsReplacedAndForgotten() {
    Instant at = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    provider.save("PAIR", "h1", new LockoutRow(5, at, at.plusSeconds(60)));
    provider.save("PAIR", "h1", new LockoutRow(6, at, null));

    assertThat(provider.find("PAIR", "h1")).contains(new LockoutRow(6, at, null));
    assertThat(provider.findForUpdate("PAIR", "h1")).isPresent();
    assertThat(provider.find("ACCOUNT", "h1")).isEmpty();

    provider.delete("PAIR", "h1");
    assertThat(provider.find("PAIR", "h1")).isEmpty();
  }

  @Test
  void idleCountersArePurgedButALockStillHoldingIsKept() {
    Instant old = Instant.now().minus(2, ChronoUnit.HOURS);
    provider.save("ACCOUNT", "idle", new LockoutRow(2, old, null));
    provider.save("ACCOUNT", "locked", new LockoutRow(30, old, Instant.now().plusSeconds(600)));

    provider.deleteIdleBefore(Instant.now().minus(1, ChronoUnit.HOURS));

    assertThat(provider.find("ACCOUNT", "idle")).isEmpty();
    assertThat(provider.find("ACCOUNT", "locked")).isPresent();
  }

  @Test
  void thePolicyIsSeededAndReplaceable() {
    assertThat(provider.settings()).isEqualTo(new LockoutSettingsRow(5, 25, 60, 900, 1800));

    provider.saveSettings(new LockoutSettingsRow(3, 10, 30, 600, 900), "eric@example.test");

    assertThat(provider.settings()).isEqualTo(new LockoutSettingsRow(3, 10, 30, 600, 900));
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.LockoutPersistenceProviderImpl"))
  static class App {
    @org.springframework.context.annotation.Bean
    PersistenceManagedTypes persistenceManagedTypes() {
      return PersistenceManagedTypes.of();
    }
  }
}
