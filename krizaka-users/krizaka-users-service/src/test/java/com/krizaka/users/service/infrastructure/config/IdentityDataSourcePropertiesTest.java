package com.krizaka.users.service.infrastructure.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IdentityDataSourcePropertiesTest {

  @Test
  @DisplayName("Valid identity datasource wiring is accepted")
  void validWiring() {
    var properties =
        new IdentityDataSourceProperties(
            "jdbc:postgresql://localhost:5432/krizaka_users_db", "krizaka_users", "secret");
    assertThat(properties.url()).contains("krizaka_users_db");
    assertThat(properties.username()).isEqualTo("krizaka_users");
  }

  @Test
  @DisplayName("Missing values and non-postgres URLs are rejected")
  void invalidWiringRejected() {
    assertThatNullPointerException()
        .isThrownBy(() -> new IdentityDataSourceProperties(null, "u", "p"));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new IdentityDataSourceProperties("jdbc:postgresql://localhost:5432/db", null, "p"));
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                new IdentityDataSourceProperties("jdbc:postgresql://localhost:5432/db", "u", null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IdentityDataSourceProperties("jdbc:mysql://x/db", "u", "p"));
  }
}
