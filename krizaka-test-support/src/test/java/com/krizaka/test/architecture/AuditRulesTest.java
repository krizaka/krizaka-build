package com.krizaka.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** The ADR-015 build rules: each fails on the defect it names and passes on its cure. */
class AuditRulesTest {

  private static final String ROOT = "com.krizaka.test.architecture.fixture.audit";
  private static final JavaClasses BAD = new ClassFileImporter().importPackages(ROOT + ".bad");
  private static final JavaClasses GOOD = new ClassFileImporter().importPackages(ROOT + ".good");

  @Test
  void cleanCodePassesEveryRule() {
    assertThatCode(
            () -> {
              AuditRules.assertControllersDoNotReachPersistence(GOOD);
              AuditRules.assertNoUtilityClasses(GOOD);
              AuditRules.assertRequestBodyIsValidated(GOOD);
              AuditRules.assertSpringBeanFieldsAreFinal(GOOD);
              AuditRules.assertEveryPackageNullMarked(GOOD);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void aControllerReachingPersistenceIsFound() {
    assertThatThrownBy(() -> AuditRules.assertControllersDoNotReachPersistence(BAD))
        .hasMessageContaining("OrderController")
        .hasMessageContaining("OrderStore");
  }

  @Test
  void utilityClassesAreFound() {
    assertThatThrownBy(() -> AuditRules.assertNoUtilityClasses(BAD))
        .hasMessageContaining("DateUtils")
        .hasMessageContaining("StringHelper");
  }

  @Test
  void aRequestBodyWithoutValidIsFound() {
    assertThatThrownBy(() -> AuditRules.assertRequestBodyIsValidated(BAD))
        .hasMessageContaining("OrderController.create")
        .hasMessageContaining("without @Valid");
  }

  @Test
  void aNonFinalFieldOfABeanIsFound() {
    assertThatThrownBy(() -> AuditRules.assertSpringBeanFieldsAreFinal(BAD))
        .hasMessageContaining("OrderController.store");
  }

  @Test
  void anUnmarkedPackageIsReportedOnce() {
    assertThatThrownBy(() -> AuditRules.assertEveryPackageNullMarked(BAD))
        .hasMessageContaining("bad.web")
        .hasMessageContaining("not @NullMarked")
        .satisfies(
            e ->
                org.assertj.core.api.Assertions.assertThat(
                        countOf(e.getMessage(), "bad.web> is not"))
                    .isEqualTo(1));
  }

  private static int countOf(String text, String part) {
    return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
  }
}
