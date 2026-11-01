package com.krizaka.billing.service.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.krizaka.billing.domain.model.BillableCapability;
import com.krizaka.billing.domain.model.BillableUnit;
import com.krizaka.billing.service.domain.model.PricebookRate;
import com.krizaka.test.container.AbstractContainerIntegrationTest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Guard of the hold sweep's transaction (Sonar java:S2229): the {@code SELECT ... FOR UPDATE SKIP
 * LOCKED} that picks the expired holds must run inside the transaction that releases them,
 * whichever overload the caller uses. A public method that reaches a {@code @Transactional} one
 * through {@code this} bypasses Spring's proxy, so the row locks end at once and two sweepers can
 * release one hold twice. Real proxies (the composition matters), real PostgreSQL (a mock would not
 * race).
 */
class HoldSweepTransactionIT extends AbstractContainerIntegrationTest {

  private static final String LOCKING_SELECT = "FOR UPDATE SKIP LOCKED";
  private static final List<Boolean> lockingSelectInTransaction = new CopyOnWriteArrayList<>();

  private static AnnotationConfigApplicationContext context;
  private static JdbcTemplate jdbc;
  private static CreditLedgerService ledger;

  @BeforeAll
  static void wire() {
    JdbcTemplate setup =
        new JdbcTemplate(
            new DriverManagerDataSource(
                postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword()));
    CreditLedgerConcurrencyIT.SCHEMA.forEach(setup::execute);
    context = new AnnotationConfigApplicationContext(Wiring.class);
    jdbc = context.getBean(JdbcTemplate.class);
    ledger = context.getBean(CreditLedgerService.class);
  }

  @AfterAll
  static void close() {
    context.close();
  }

  @Test
  void theNoArgSweepLocksItsHoldsInsideTheTransactionThatReleasesThem() {
    expiredHold(10);
    lockingSelectInTransaction.clear();

    ledger.sweepExpiredHolds();

    assertThat(lockingSelectInTransaction)
        .as("the locking SELECT of sweepExpiredHolds() ran")
        .isNotEmpty()
        .as("and every run was inside a transaction, or its row locks end at once")
        .containsOnly(true);
  }

  @Test
  void theBatchSweepLocksItsHoldsInsideTheTransactionThatReleasesThem() {
    expiredHold(10);
    lockingSelectInTransaction.clear();

    ledger.sweepExpiredHolds(5);

    assertThat(lockingSelectInTransaction).isNotEmpty().containsOnly(true);
  }

  @Test
  void twoSweepersOnTheSameExpiredHoldsReleaseEachOnce() throws Exception {
    for (int round = 0; round < 30; round++) {
      String actor = "actor-" + UUID.randomUUID();
      jdbc.update(
          "INSERT INTO credit_wallet (actor_id, balance_granted, held) VALUES (?, 500, 40)", actor);
      for (int i = 0; i < 4; i++) {
        expiredHold(actor, 10);
      }
      ExecutorService pool = Executors.newFixedThreadPool(2);
      try {
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> sweeps =
            List.of(pool.submit(() -> sweepAfter(go)), pool.submit(() -> sweepAfter(go)));
        go.countDown();
        int released = 0;
        for (Future<Integer> sweep : sweeps) {
          released += sweep.get();
        }
        assertThat(released).as("holds released, round %d", round).isEqualTo(4);
      } finally {
        pool.shutdownNow();
      }
      assertThat(
              jdbc.queryForObject(
                  "SELECT held FROM credit_wallet WHERE actor_id = ?", Long.class, actor))
          .as("held decremented once per hold, round %d", round)
          .isZero();
    }
  }

  private static int sweepAfter(CountDownLatch go) throws InterruptedException {
    go.await();
    return ledger.sweepExpiredHolds();
  }

  private static void expiredHold(long estimate) {
    String actor = "actor-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO credit_wallet (actor_id, balance_granted, held) VALUES (?, 500, ?)",
        actor,
        estimate);
    expiredHold(actor, estimate);
  }

  private static void expiredHold(String actor, long estimate) {
    jdbc.update(
        "INSERT INTO credit_hold (id, actor_id, estimated_credits, status, capability,"
            + " pricebook_version, correlation_id, expires_at)"
            + " VALUES (?, ?, ?, 'ACTIVE', 'CHAT', 1, 'c', now() - interval '1 minute')",
        UUID.randomUUID(),
        actor,
        estimate);
  }

  /** Real {@code @Transactional} proxies; the JdbcTemplate notes the transaction of each lock. */
  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  static class Wiring {

    @Bean
    DataSource dataSource() {
      return new DriverManagerDataSource(
          postgres().getJdbcUrl(), postgres().getUsername(), postgres().getPassword());
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
      return new JdbcTemplate(dataSource) {
        @Override
        public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
          if (sql.contains(LOCKING_SELECT)) {
            lockingSelectInTransaction.add(
                TransactionSynchronizationManager.isActualTransactionActive());
          }
          return super.queryForList(sql, elementType, args);
        }
      };
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
      return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    CreditLedgerService creditLedgerService(JdbcTemplate jdbcTemplate) {
      PricingService pricing = mock(PricingService.class);
      when(pricing.pinnedRate(anyInt(), any(), any()))
          .thenReturn(
              new PricebookRate(
                  1, BillableCapability.CHAT, null, BillableUnit.KILOTOKEN, BigDecimal.TEN, 1, 50));
      return new CreditLedgerService(
          jdbcTemplate,
          pricing,
          mock(BillingConfigurationService.class),
          mock(OutboxService.class),
          mock(CreditRefusalService.class),
          new SimpleMeterRegistry());
    }
  }
}
