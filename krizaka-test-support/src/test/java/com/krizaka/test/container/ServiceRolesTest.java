package com.krizaka.test.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceRolesTest {

  @Test
  void quotesTheRoleAsAnIdentifierAndThePasswordAsALiteral() {
    assertThat(ServiceRoles.alterRole("krizaka_users", "s3cret"))
        .isEqualTo("ALTER ROLE \"krizaka_users\" WITH PASSWORD 's3cret'");
  }

  @Test
  void quotesCannotBreakOutOfTheStatement() {
    assertThat(ServiceRoles.alterRole("a\"b", "x'; DROP ROLE postgres; --"))
        .isEqualTo("ALTER ROLE \"a\"\"b\" WITH PASSWORD 'x''; DROP ROLE postgres; --'");
  }
}
