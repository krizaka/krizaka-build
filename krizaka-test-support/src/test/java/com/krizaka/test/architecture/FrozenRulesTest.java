package com.krizaka.test.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The debt a rule finds today is recorded, may shrink, and may not grow. */
class FrozenRulesTest {

  private static final String ROOT = "com.krizaka.test.architecture.fixture.arch";
  private static final JavaClasses BAD = new ClassFileImporter().importPackages(ROOT + ".bad");

  @TempDir Path store;

  @BeforeEach
  void pointTheStoreAtATempDirectory() {
    ArchConfiguration.get().reset();
    ArchConfiguration.get().setProperty("freeze.store.default.path", store.toString());
  }

  @AfterEach
  void restoreTheConfiguration() {
    ArchConfiguration.get().reset();
  }

  private static void record() {
    ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "true");
    ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
  }

  private static void freeze() {
    ArchConfiguration.get().setProperty("freeze.store.default.allowStoreCreation", "false");
    ArchConfiguration.get().setProperty("freeze.store.default.allowStoreUpdate", "true");
  }

  @Test
  void anExistingViolationIsRecordedThenTolerated() {
    record();
    assertThatCode(() -> FrozenRules.check(ArchitectureRules.controllersNamedControllerRule(), BAD))
        .doesNotThrowAnyException();

    freeze();
    assertThatCode(() -> FrozenRules.check(ArchitectureRules.controllersNamedControllerRule(), BAD))
        .doesNotThrowAnyException();
  }

  @Test
  void aNewViolationFailsOnceTheStoreHoldsTheRule() {
    JavaClasses good = new ClassFileImporter().importPackages(ROOT + ".good");
    record();
    FrozenRules.check(ArchitectureRules.controllersNamedControllerRule(), good);

    freeze();
    assertThatThrownBy(
            () -> FrozenRules.check(ArchitectureRules.controllersNamedControllerRule(), BAD))
        .hasMessageContaining("OrderApi");
  }

  @Test
  void aFixedViolationDoesNotBreakTheBuild() {
    JavaClasses good = new ClassFileImporter().importPackages(ROOT + ".good");
    record();
    FrozenRules.check(ArchitectureRules.controllersNamedControllerRule(), BAD);

    freeze();
    assertThatCode(
            () -> FrozenRules.check(ArchitectureRules.controllersNamedControllerRule(), good))
        .doesNotThrowAnyException();
  }

  @Test
  void aDeveloperCanRecordANewRuleFromTheCommandLine() {
    System.setProperty("archunit.freeze.store.default.allowStoreCreation", "true");
    try {
      ArchConfiguration.get().reset();
      assertThat(
              ArchConfiguration.get()
                  .getPropertyOrDefault("freeze.store.default.allowStoreCreation", "false"))
          .isEqualTo("true");
    } finally {
      System.clearProperty("archunit.freeze.store.default.allowStoreCreation");
    }
  }
}
