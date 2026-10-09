package com.krizaka.test.container;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * Gives a per-service database role its password inside a test container.
 *
 * <p>Krizaka services own their database and their role, created by the repository's {@code
 * infra/initdb/*.sql} <b>without</b> a password: the password comes from the environment at
 * deployment time, so no guessable password is ever committed, and a skipped step locks the door
 * rather than leaving it open (fail closed).
 *
 * <p>An integration test that mounts the same initdb file inherits the same passwordless role, and
 * has to perform the step a deployment performs. Without it every connection as the service role
 * fails with {@code password authentication failed} — a wiring gap, not a finding about the code
 * under test.
 *
 * <p>Call it once, after the container starts and before the first application connection.
 */
public final class ServiceRoles {

  private ServiceRoles() {}

  /**
   * Assigns a login password to a role created by an initdb script.
   *
   * <p>Connects as the container's superuser, the only account that can authenticate before this
   * runs.
   *
   * @param container the started PostgreSQL container
   * @param role the service role named by the initdb file
   * @param password the password the test's DataSource will present
   * @throws IllegalStateException when the statement cannot be applied — a test that proceeded past
   *     this would fail later with a misleading authentication error
   */
  public static void assignPassword(
      JdbcDatabaseContainer<?> container, String role, String password) {
    try (Connection connection =
            DriverManager.getConnection(
                container.getJdbcUrl(), container.getUsername(), container.getPassword());
        Statement jdbc = connection.createStatement()) {
      jdbc.execute(alterRole(role, password));
    } catch (SQLException e) {
      throw new IllegalStateException("Could not assign a password to role " + role, e);
    }
  }

  /** {@code ALTER ROLE} takes no bind parameters, so the identifier and literal are quoted here. */
  static String alterRole(String role, String password) {
    return "ALTER ROLE %s WITH PASSWORD %s"
        .formatted(quoteIdentifier(role), quoteLiteral(password));
  }

  private static String quoteIdentifier(String identifier) {
    return '"' + identifier.replace("\"", "\"\"") + '"';
  }

  private static String quoteLiteral(String literal) {
    return '\'' + literal.replace("'", "''") + '\'';
  }
}
