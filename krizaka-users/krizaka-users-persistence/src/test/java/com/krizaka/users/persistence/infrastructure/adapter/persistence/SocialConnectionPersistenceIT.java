package com.krizaka.users.persistence.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.users.persistence.domain.model.SocialConnectionRow;
import com.krizaka.users.persistence.domain.ports.SocialConnectionPersistenceProvider;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

/**
 * The connections table of ADR-016 slice 1 ({@code V5__social_connections.sql}): a sealed secret
 * round-trips as stored, and the database itself refuses a value that is not a {@code v2.}
 * envelope.
 */
@SpringBootTest(
    classes = SocialConnectionPersistenceIT.App.class,
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
class SocialConnectionPersistenceIT extends AbstractContainerIntegrationTest {

  @Autowired SocialConnectionPersistenceProvider provider;
  @Autowired JdbcTemplate jdbc;

  @BeforeAll
  static void migrate() throws Exception {
    String sql;
    try (var in =
        SocialConnectionPersistenceIT.class.getResourceAsStream(
            "/db/migration/V5__social_connections.sql")) {
      sql = new String(java.util.Objects.requireNonNull(in).readAllBytes());
    }
    try (var connection =
            java.sql.DriverManager.getConnection(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
        var statement = connection.createStatement()) {
      statement.execute("DROP TABLE IF EXISTS social_connection");
      statement.execute(sql);
      statement.execute(sql); // idempotent
    }
  }

  @Test
  void aSealedSecretRoundTripsAndIsReplacedByTheNextSave() {
    provider.save(
        new SocialConnectionRow("google", "g-id", "v2.k1.aaa.bbb", "k1", "env", Instant.now()));
    assertThat(provider.find("google"))
        .get()
        .extracting(SocialConnectionRow::sealedClientSecret)
        .isEqualTo("v2.k1.aaa.bbb");

    provider.save(
        new SocialConnectionRow("google", "g-id", "v2.k2.ccc.bbb", "k2", "env", Instant.now()));
    assertThat(provider.findAll()).extracting(SocialConnectionRow::keyId).contains("k2");
    assertThat(provider.findAll().stream().filter(r -> "google".equals(r.provider()))).hasSize(1);
  }

  @Test
  void theDatabaseRefusesAClearSecret() {
    assertThatThrownBy(
            () ->
                provider.save(
                    new SocialConnectionRow(
                        "github", "gh-id", "clear-secret", null, "env", Instant.now())))
        .hasMessageContaining("social_connection_sealed");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM social_connection WHERE provider = 'github'", Integer.class))
        .isZero();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.SocialConnectionPersistenceProviderImpl"))
  static class App {
    @org.springframework.context.annotation.Bean
    PersistenceManagedTypes persistenceManagedTypes() {
      return PersistenceManagedTypes.of();
    }
  }
}
