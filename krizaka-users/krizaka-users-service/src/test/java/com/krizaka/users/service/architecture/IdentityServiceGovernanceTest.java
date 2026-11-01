package com.krizaka.users.service.architecture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.krizaka.test.architecture.ConfigBindingRules;
import com.krizaka.test.architecture.SourceRules;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Governance for krizaka-users-service — for now, only the rule that could not be left out.
 *
 * <p>This service had no governance suite at all (the M0.5 inventory records it). It is created
 * here for [CFG-001] alone: the identity library's configuration types and this service's own are
 * bound only in this process, so no other suite's classpath reaches them. The rest of the rules
 * every other service runs — SEAM-002, ERR-103, ERR-129, ERR-130 — are still absent, and still
 * recorded as absent; adding them is not this change.
 */
class IdentityServiceGovernanceTest {

  @Test
  @DisplayName(
      "[CFG-001] every type the configuration binder builds has a constructor it can choose")
  void configurationBindsUnambiguously() {
    assertDoesNotThrow(
        () -> {
          ConfigBindingRules.assertConfigurationBindsUnambiguously("com.krizaka");
          ConfigBindingRules.assertInjectableComponentsHaveOneConstructor("com.krizaka");
        });
  }

  @Test
  @DisplayName("[ERR-113] No Environment injection in production beans")
  void noEnvironmentInjection() {
    assertDoesNotThrow(
        () -> {
          SourceRules.assertNoEnvironmentInjection(Path.of("src", "main", "java"));
        });
  }
}
