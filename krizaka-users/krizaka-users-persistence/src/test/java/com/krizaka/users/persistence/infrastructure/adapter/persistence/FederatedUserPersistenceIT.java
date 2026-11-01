package com.krizaka.users.persistence.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.test.sql.InitDb;
import com.krizaka.users.persistence.domain.model.UserDto;
import com.krizaka.users.persistence.domain.ports.UserPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A federated identity survives the round trip through the <b>real</b> {@code users} table ({@code
 * infra/initdb/10-identity.sql}): stored with its provider and provider id, found again by them on
 * the next sign-in (ADR-001, finding 1), and a taken username is reported as taken.
 */
@SpringBootTest(
    classes = FederatedUserPersistenceIT.App.class,
    properties = {
      "krizaka.users.crypto.key=test-crypto-key-0123456789abcdef!",
      "krizaka.users.crypto.salt=0123456789abcdef",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.flyway.enabled=false",
      "krizaka.messaging.outbox.enabled=false",
      "krizaka.messaging.consumer.enabled=false",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration"
    })
class FederatedUserPersistenceIT extends AbstractContainerIntegrationTest {

  @Autowired UserPersistenceProvider users;

  /** The identity schema as committed, after the database bootstrap lines (role, database, \c). */
  @BeforeAll
  static void createTheRealTables() throws Exception {
    String sql =
        Files.readString(
            InitDb.locate(Path.of(System.getProperty("user.dir"))).resolve("10-identity.sql"));
    String schema = sql.substring(sql.indexOf("CREATE TABLE rate_limit_tiers"));
    try (var connection =
            java.sql.DriverManager.getConnection(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
        var statement = connection.createStatement()) {
      statement.execute(
          "DROP TABLE IF EXISTS organization_members, organizations, identity_outbox, admin_audit,"
              + " rate_limits, password_resets,"
              + " api_keys, user_credentials, user_profiles, user_interceptions,"
              + " verification_tokens, authorities, users, rate_limit_tiers CASCADE");
      statement.execute(schema);
    }
  }

  private static UserDto federated(String id, String username, String provider, String sub) {
    return new UserDto(
        id,
        username,
        null,
        username.toLowerCase().replace(' ', '.') + "@example.test",
        true,
        Map.of("language", "en"),
        Set.of("ROLE_USER"),
        List.of(),
        provider,
        sub,
        "free",
        null);
  }

  @Test
  void aFederatedUserIsFoundAgainByItsProviderIdentity() {
    users.create(federated("5b0b6f8e-0000-4000-8000-000000000001", "Ada", "google", "g-1"), null);

    var again = users.findByProviderAndProviderId("google", "g-1");

    assertThat(again).isPresent();
    assertThat(again.get().id()).isEqualTo("5b0b6f8e-0000-4000-8000-000000000001");
    assertThat(again.get().provider()).isEqualTo("google");
    assertThat(again.get().providerId()).isEqualTo("g-1");
    assertThat(users.findByProviderAndProviderId("github", "g-1")).isEmpty();
  }

  @Test
  void aTakenUsernameIsReportedAsTaken() {
    users.create(
        federated("5b0b6f8e-0000-4000-8000-000000000002", "Alex Martin", "github", "42"), null);

    assertThat(users.existsByUsername("Alex Martin")).isTrue();
    assertThat(users.existsByUsername("Alex Martin-7f3a")).isFalse();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EnableJpaRepositories(basePackageClasses = UserRepository.class)
  @EntityScan(basePackageClasses = UserEntity.class)
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.UserPersistenceProviderImpl"))
  static class App {}
}
