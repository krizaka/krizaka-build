package com.krizaka.users.persistence.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.test.sql.InitDb;
import com.krizaka.users.persistence.domain.ports.AdminUserPersistenceProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * The administration's queries against the identity context's <b>real</b> tables ({@code
 * infra/initdb/10-identity.sql}): search, the administrator role, suspension, erasure with its
 * cascade, and the audit trail with the person removed from the lines they acted in.
 */
@SpringBootTest(
    classes = AdminUserPersistenceIT.App.class,
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
class AdminUserPersistenceIT extends AbstractContainerIntegrationTest {

  @Autowired AdminUserPersistenceProvider provider;
  @Autowired JdbcTemplate jdbc;

  @BeforeAll
  static void createTheRealTables() throws Exception {
    String sql =
        Files.readString(
            InitDb.locate(Path.of(System.getProperty("user.dir"))).resolve("10-identity.sql"));
    try (var connection =
            java.sql.DriverManager.getConnection(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
        var statement = connection.createStatement()) {
      for (String table :
          new String[] {"rate_limit_tiers", "users", "authorities", "admin_audit"}) {
        Matcher m =
            Pattern.compile("CREATE TABLE " + table + " \\(.*?\\n\\);", Pattern.DOTALL)
                .matcher(sql);
        assertThat(m.find()).as(table + " in 10-identity.sql").isTrue();
        statement.execute("DROP TABLE IF EXISTS " + table + " CASCADE");
        statement.execute(m.group());
      }
    }
  }

  private void user(String id, String username, String email, boolean admin, boolean enabled) {
    jdbc.update(
        "INSERT INTO users (id, username, email, enabled) VALUES (?, ?, ?, ?)",
        id,
        username,
        email,
        enabled);
    jdbc.update("INSERT INTO authorities (user_id, authority_name) VALUES (?, 'ROLE_USER')", id);
    if (admin) {
      jdbc.update("INSERT INTO authorities (user_id, authority_name) VALUES (?, 'ROLE_ADMIN')", id);
    }
  }

  @Test
  void searchFindsByEmailOrNameAndTreatsWildcardsAsText() {
    user("a1", "ada", "ada@example.test", true, true);
    user("b1", "bob_smith", "bob@example.test", false, true);
    assertThat(provider.search("ADA@", 0, 10)).extracting("id").containsExactly("a1");
    assertThat(provider.search("bob_", 0, 10)).extracting("id").containsExactly("b1");
    assertThat(provider.search("%", 0, 10)).isEmpty();
    assertThat(provider.count("example.test")).isGreaterThanOrEqualTo(2);
    assertThat(provider.find("a1").orElseThrow().authorities()).contains("ROLE_ADMIN", "ROLE_USER");
  }

  @Test
  void theAdministratorRoleAndSuspensionAreWrittenAndCounted() {
    user("c1", "cleo", "cleo@example.test", false, true);
    long before = provider.countEnabledAdmins();
    provider.setAdmin("c1", true);
    provider.setAdmin("c1", true);
    assertThat(provider.countEnabledAdmins()).isEqualTo(before + 1);
    provider.setEnabled("c1", false);
    assertThat(provider.countEnabledAdmins()).isEqualTo(before);
    assertThat(provider.find("c1").orElseThrow().enabled()).isFalse();
    provider.setAdmin("c1", false);
    assertThat(provider.find("c1").orElseThrow().authorities()).containsExactly("ROLE_USER");
  }

  @Test
  void theAuditKeepsTheRequestOfTheCall() {
    // ADR-085: every operator action carries the request it was made in (the id of the edge's
    // X-Request-Id).
    org.slf4j.MDC.put("requestId", "req-0001-abcdef");
    try {
      provider.appendAudit("a9", "ada@example.test", "admin.user_suspended", "t9", null);
    } finally {
      org.slf4j.MDC.remove("requestId");
    }
    provider.appendAudit("a9", "ada@example.test", "admin.user_restored", "t9", null);

    var lines = provider.recentAudit("t9", 10);
    assertThat(lines)
        .filteredOn(l -> "admin.user_suspended".equals(l.action()))
        .extracting("requestId")
        .containsExactly("req-0001-abcdef");
    assertThat(lines)
        .filteredOn(l -> "admin.user_restored".equals(l.action()))
        .extracting("requestId")
        .containsNull();
  }

  @Test
  void aRefusedActionLeavesARowWithItsOutcome() {
    provider.appendAuditOutcome(
        "a8", "ada@example.test", "admin.user_suspended", "a8", "SELF", "refused");
    provider.appendAudit("a8", "ada@example.test", "admin.user_restored", "t8", null);

    var lines = provider.recentAudit("a8", 10);
    assertThat(lines)
        .filteredOn(l -> "admin.user_suspended".equals(l.action()))
        .extracting("outcome", "detail")
        .containsExactly(org.assertj.core.groups.Tuple.tuple("refused", "SELF"));
    assertThat(lines)
        .filteredOn(l -> "admin.user_restored".equals(l.action()))
        .extracting("outcome")
        .containsExactly("ok");
  }

  @Test
  void erasureCascadesAndTheAuditKeepsTheFactNotThePerson() {
    user("d1", "dan", "dan@example.test", false, true);
    provider.appendAudit("d1", "dan@example.test", "auth.sign_in", "d1", null);
    provider.appendAudit("a1", "ada@example.test", "admin.user_suspended", "d1", "why");
    provider.anonymiseAudit("d1");
    provider.delete("d1");
    assertThat(provider.find("d1")).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM authorities WHERE user_id = 'd1'", Long.class))
        .isZero();
    var lines = provider.recentAudit("d1", 10);
    assertThat(lines).extracting("actorLabel").contains("deleted account", "ada@example.test");
    assertThat(lines).noneMatch(l -> "dan@example.test".equals(l.actorLabel()));
    assertThat(provider.recentAudit(null, 5)).hasSizeLessThanOrEqualTo(5);
  }

  /** The administration's provider on the real tables, without the rest of persistence. */
  @SpringBootConfiguration
  @EnableAutoConfiguration
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.AdminUserPersistenceProviderImpl"))
  static class App {

    /** No entity: the provider is native SQL over the identity tables. */
    @org.springframework.context.annotation.Bean
    PersistenceManagedTypes persistenceManagedTypes() {
      return PersistenceManagedTypes.of();
    }
  }
}
