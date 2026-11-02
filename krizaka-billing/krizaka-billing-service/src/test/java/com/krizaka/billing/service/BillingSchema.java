package com.krizaka.billing.service;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Builds the billing schema in a test container the way the service does at startup: Flyway's
 * {@code db/migration} as the owner role (krizaka-internal#2255). The initdb file only creates the
 * role and the database.
 */
public final class BillingSchema {

  private BillingSchema() {}

  /**
   * Migrates {@code krizaka_billing_db} of the container.
   *
   * @param postgres the started container, its initdb file run
   * @param role the owner role
   * @param password the role's password
   */
  public static void migrate(PostgreSQLContainer<?> postgres, String role, String password) {
    Flyway.configure()
        .dataSource(
            "jdbc:postgresql://%s:%d/krizaka_billing_db"
                .formatted(postgres.getHost(), postgres.getMappedPort(5432)),
            role,
            password)
        .locations("classpath:db/migration")
        .load()
        .migrate();
  }
}
