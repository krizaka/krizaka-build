package com.krizaka.users.persistence.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.users.persistence.domain.ports.CredentialGrantPersistenceProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * The credential grants of GHSA-3pf7-rgf2-4x3m ({@code V8__credential_grants.sql}) on a real
 * Postgres: a grant is consumed at most once, even by concurrent redemptions, and only for its
 * provider and run, before it expires.
 */
@SpringBootTest(
    classes = CredentialGrantPersistenceIT.App.class,
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
class CredentialGrantPersistenceIT extends AbstractContainerIntegrationTest {

  @Autowired CredentialGrantPersistenceProvider provider;

  @BeforeAll
  static void migrate() throws Exception {
    String sql;
    try (var in =
        CredentialGrantPersistenceIT.class.getResourceAsStream(
            "/db/migration/V8__credential_grants.sql")) {
      // Without the users FK here (no users table in this database); UsersMigrationsIT runs V8 with
      // it.
      sql =
          new String(java.util.Objects.requireNonNull(in).readAllBytes())
              .replace(" REFERENCES users(id) ON DELETE CASCADE", "");
    }
    try (var connection =
            java.sql.DriverManager.getConnection(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
        var statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS credential_grants");
      statement.execute(sql);
      statement.execute(sql); // idempotent
    }
  }

  private static Instant in(long minutes) {
    return Instant.now().plus(minutes, ChronoUnit.MINUTES);
  }

  @Test
  void aGrantIsConsumedOnceForItsProviderAndRun() {
    provider.save("a".repeat(64), "alice", "openai", "run-1", in(10));

    assertThat(provider.consume("a".repeat(64), "openai", "run-2", Instant.now())).isEmpty();
    assertThat(provider.consume("a".repeat(64), "anthropic", "run-1", Instant.now())).isEmpty();
    assertThat(provider.consume("a".repeat(64), "openai", "run-1", Instant.now()))
        .contains("alice");
    assertThat(provider.consume("a".repeat(64), "openai", "run-1", Instant.now())).isEmpty();
  }

  @Test
  void anExpiredGrantIsNotConsumedAndIsPurged() {
    provider.save("b".repeat(64), "alice", "openai", "run-1", in(-1));

    assertThat(provider.consume("b".repeat(64), "openai", "run-1", Instant.now())).isEmpty();
    assertThat(provider.deleteExpiredBefore(Instant.now())).isPositive();
  }

  @Test
  void concurrentRedemptionsConsumeTheGrantExactlyOnce() throws Exception {
    String hash = "c".repeat(64);
    provider.save(hash, "alice", "openai", "run-1", in(10));
    int racers = 8;
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(racers);
    try {
      List<Future<Optional<String>>> results = new ArrayList<>();
      for (int i = 0; i < racers; i++) {
        Callable<Optional<String>> race =
            () -> {
              start.await();
              return provider.consume(hash, "openai", "run-1", Instant.now());
            };
        results.add(pool.submit(race));
      }
      start.countDown();
      long winners = 0;
      for (Future<Optional<String>> r : results) {
        if (r.get().isPresent()) {
          winners++;
        }
      }
      assertThat(winners).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.CredentialGrantPersistenceProviderImpl"))
  static class App {
    @org.springframework.context.annotation.Bean
    PersistenceManagedTypes persistenceManagedTypes() {
      return PersistenceManagedTypes.of();
    }
  }
}
