package com.krizaka.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Each structural rule passes on well-formed code and fails, with its reason, on the defect. */
class ArchitectureRulesTest {

  private static final String ROOT = "com.krizaka.test.architecture.fixture.arch";
  private static final JavaClasses GOOD = new ClassFileImporter().importPackages(ROOT + ".good");
  private static final JavaClasses BAD = new ClassFileImporter().importPackages(ROOT + ".bad");
  private static final JavaClasses CYCLE = new ClassFileImporter().importPackages(ROOT + ".cycle");

  @Test
  void wellFormedCodePassesEveryRule() {
    assertThatCode(
            () -> {
              ArchitectureRules.assertLayers(
                  GOOD, "..good.web..", "..good.service..", "..good.repository..");
              ArchitectureRules.assertNamingConventions(GOOD);
              ArchitectureRules.assertConfigurationReadOnlyInConfig(GOOD, "..config..");
              ArchitectureRules.assertTransactionalOnlyOnServices(GOOD, "..service..");
            })
        .doesNotThrowAnyException();
  }

  @Test
  void aLayerSkippedIsFound() {
    assertThatThrownBy(
            () ->
                ArchitectureRules.assertLayers(
                    BAD, "..bad.web..", "..bad.service..", "..bad.repository.."))
        .hasMessageContaining("OrderController");
  }

  @Test
  void aCallUpIsFound() {
    assertThatThrownBy(
            () ->
                ArchitectureRules.assertLayers(
                    BAD, "..bad.web..", "..bad.service..", "..bad.repository.."))
        .hasMessageContaining("OrderRepository");
  }

  @Test
  void aCycleBetweenPackagesIsFound() {
    assertThatThrownBy(() -> ArchitectureRules.assertNoPackageCycles(CYCLE, ROOT + ".cycle"))
        .hasMessageContaining("Cycle");
  }

  @Test
  void aControllerNotNamedControllerIsFound() {
    assertThatThrownBy(() -> ArchitectureRules.controllersNamedControllerRule().check(BAD))
        .hasMessageContaining("OrderApi");
  }

  @Test
  void aServiceWithAPatternNameIsFound() {
    assertThatThrownBy(() -> ArchitectureRules.servicesNamedServiceRule().check(BAD))
        .hasMessageContaining("OrderManager");
  }

  @Test
  void aValueOutsideConfigurationIsFound() {
    assertThatThrownBy(() -> ArchitectureRules.valueOnlyInConfigRule("..config..").check(BAD))
        .hasMessageContaining("page");
  }

  @Test
  void anEnvironmentReadOutsideConfigurationIsFound() {
    assertThatThrownBy(
            () -> ArchitectureRules.noEnvironmentOutsideConfigRule("..config..").check(BAD))
        .hasMessageContaining("getenv");
  }

  @Test
  void aTransactionOnAControllerIsFound() {
    assertThatThrownBy(
            () -> ArchitectureRules.assertTransactionalOnlyOnServices(BAD, "..service.."))
        .hasMessageContaining("OrderController");
  }
}
