package com.krizaka.users.persistence.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.test.container.AbstractContainerIntegrationTest;
import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import com.krizaka.users.persistence.domain.ports.UserCredentialPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.entity.UserCredentialEntity;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.repository.UserCredentialRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

/**
 * krizaka-internal#2359 slice 1a, {@code V7__user_credentials_one_per_provider.sql}: the database
 * holds one key per user and provider (two concurrent creations land once), keeps the newest of the
 * old duplicates, and a delete leaves no row — the sealed value goes with it.
 */
@SpringBootTest(
    classes = UserCredentialPersistenceIT.App.class,
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
class UserCredentialPersistenceIT extends AbstractContainerIntegrationTest {

  @Autowired UserCredentialPersistenceProvider provider;
  @Autowired JdbcTemplate jdbc;

  private String v7() throws Exception {
    try (var in =
        UserCredentialPersistenceIT.class.getResourceAsStream(
            "/db/migration/V7__user_credentials_one_per_provider.sql")) {
      return new String(java.util.Objects.requireNonNull(in).readAllBytes());
    }
  }

  @BeforeEach
  void table() throws Exception {
    jdbc.execute("DROP TABLE IF EXISTS user_credentials");
    // The table as infra/initdb/10-identity.sql creates it (without the users FK), then two old
    // duplicates.
    jdbc.execute(
        "CREATE TABLE user_credentials (id SERIAL PRIMARY KEY, user_id VARCHAR(255) NOT NULL,"
            + " provider_name VARCHAR(255) NOT NULL, api_key VARCHAR(1024) NOT NULL)");
    jdbc.update(
        "INSERT INTO user_credentials (user_id, provider_name, api_key) VALUES"
            + " ('u1', 'openai', 'old'), ('u1', 'openai', 'newest')");
    jdbc.execute(v7());
    jdbc.execute(v7()); // idempotent
  }

  @Test
  void theNewestOldDuplicateIsKept() {
    assertThat(jdbc.queryForList("SELECT api_key FROM user_credentials", String.class))
        .containsExactly("newest");
  }

  @Test
  void twoConcurrentCreationsLandOnce() throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(4);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Optional<UserCredentialDto>>> answers = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      String sealed = "v2.k1.w" + i + ".b" + i;
      answers.add(
          pool.submit(
              () -> {
                start.await();
                return provider.insert(new UserCredentialDto(null, "krizaka", "runway", sealed));
              }));
    }
    start.countDown();
    int landed = 0;
    for (var answer : answers) {
      landed += answer.get().isPresent() ? 1 : 0;
    }
    pool.shutdown();

    assertThat(landed).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM user_credentials WHERE user_id = 'krizaka'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rotateStampsTheRowAndDeleteLeavesNothing() {
    provider.insert(new UserCredentialDto(null, "krizaka", "runway", "v2.k1.a.b"));

    assertThat(provider.replaceStoredKey("krizaka", "runway", "v2.k1.c.d")).isTrue();
    assertThat(provider.replaceStoredKey("someone-else", "runway", "v2.k1.c.d")).isFalse();
    assertThat(provider.findByUserIdAndProviderName("krizaka", "runway"))
        .get()
        .extracting(UserCredentialDto::apiKey)
        .isEqualTo("v2.k1.c.d");

    assertThat(provider.deleteByUserIdAndProviderName("someone-else", "runway")).isFalse();
    assertThat(provider.deleteByUserIdAndProviderName("krizaka", "runway")).isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM user_credentials WHERE user_id = 'krizaka'", Integer.class))
        .isZero();
    assertThat(provider.deleteByUserIdAndProviderName("krizaka", "runway")).isFalse();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EnableJpaRepositories(
      basePackageClasses = UserCredentialRepository.class,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = UserCredentialRepository.class))
  @ComponentScan(
      basePackages = "com.krizaka.users.persistence",
      useDefaultFilters = false,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.REGEX,
              pattern = ".*\\.UserCredentialPersistenceProviderImpl"))
  static class App {
    @Bean
    PersistenceManagedTypes persistenceManagedTypes() {
      return PersistenceManagedTypes.of(UserCredentialEntity.class.getName());
    }
  }
}
