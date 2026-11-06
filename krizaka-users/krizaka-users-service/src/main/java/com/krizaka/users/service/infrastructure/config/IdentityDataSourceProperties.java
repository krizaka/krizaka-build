package com.krizaka.users.service.infrastructure.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The identity service's own datasource wiring ({@code krizaka.users-service.datasource}), bound
 * from {@code IDENTITY_DB_*} only. A dedicated prefix (instead of {@code spring.datasource}) keeps
 * the shared local {@code .env} — which exports {@code SPRING_DATASOURCE_*} for the app database —
 * from hijacking this service's connection through Spring's env-var precedence over yaml.
 *
 * @param url the JDBC URL of {@code krizaka_users_db}
 * @param username the service's own database role
 * @param password the role's password
 */
@ConfigurationProperties(prefix = "krizaka.users-service.datasource")
public record IdentityDataSourceProperties(String url, String username, String password) {

  public IdentityDataSourceProperties {
    Objects.requireNonNull(url, "identity datasource url is required");
    Objects.requireNonNull(username, "identity datasource username is required");
    Objects.requireNonNull(password, "identity datasource password is required");
    if (!url.startsWith("jdbc:postgresql:")) {
      throw new IllegalArgumentException("identity datasource url must be a postgresql JDBC url");
    }
  }
}
