package com.krizaka.test.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ConfigBindingRulesTest {

  private static final String FIXTURE = "com.krizaka.test.architecture.fixture.config";
  private static final String GOOD = "com.krizaka.test.architecture.fixture.good";

  @Test
  void aConfigurationTypeTheBinderCannotBuildIsFound() {
    assertThatThrownBy(
            () ->
                ConfigBindingRules.assertConfigurationBindsUnambiguously(
                    new ClassFileImporter().importPackages(FIXTURE), FIXTURE))
        .hasMessageContaining("AmbiguousProperties")
        .hasMessageContaining("@ConstructorBinding");
  }

  @Test
  void aPackageWithNoConfigurationTypeIsNotAPass() {
    assertThatThrownBy(
            () ->
                ConfigBindingRules.assertConfigurationBindsUnambiguously(
                    new ClassFileImporter().importPackages(GOOD), GOOD))
        .hasMessageContaining("examined no");
  }
}
