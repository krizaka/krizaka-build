package com.krizaka.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Each rule passes on well-formed code and fails, with its reason, on the matching defect. */
class CodeRulesTest {

  private static final String GOOD = "com.krizaka.test.architecture.fixture.good";
  private static final String BAD = "com.krizaka.test.architecture.fixture.bad";

  private static final JavaClasses GOOD_CLASSES = new ClassFileImporter().importPackages(GOOD);
  private static final JavaClasses BAD_CLASSES = new ClassFileImporter().importPackages(BAD);

  @Test
  void wellFormedCodePassesEveryRule() {
    assertThatCode(
            () -> {
              CodeRules.assertOneTopLevelClassPerFile(GOOD_CLASSES, GOOD);
              CodeRules.assertNoStandardStreams(GOOD_CLASSES);
              CodeRules.assertNoFieldInjection(GOOD_CLASSES, GOOD);
              CodeRules.assertServicePackageOnlyServices(GOOD_CLASSES, GOOD);
              CodeRules.assertDomainHasNoTransportDtos(GOOD_CLASSES, GOOD);
            })
        .doesNotThrowAnyException();
  }

  @Test
  void aSecondTopLevelClassInAFileIsFound() {
    assertThatThrownBy(() -> CodeRules.assertOneTopLevelClassPerFile(BAD_CLASSES, BAD))
        .hasMessageContaining("TopUpRequest")
        .hasMessageContaining("WalletManager.java");
  }

  @Test
  void standardOutputIsFound() {
    assertThatThrownBy(() -> CodeRules.assertNoStandardStreams(BAD_CLASSES))
        .hasMessageContaining("SLF4J");
  }

  @Test
  void fieldInjectionIsFound() {
    assertThatThrownBy(() -> CodeRules.assertNoFieldInjection(BAD_CLASSES, BAD))
        .hasMessageContaining("ledger");
  }

  @Test
  void aPatternNamedClassInAServicePackageIsFound() {
    assertThatThrownBy(() -> CodeRules.assertServicePackageOnlyServices(BAD_CLASSES, BAD))
        .hasMessageContaining("WalletManager");
  }

  @Test
  void aTransportDtoInTheDomainIsFound() {
    assertThatThrownBy(() -> CodeRules.assertDomainHasNoTransportDtos(BAD_CLASSES, BAD))
        .hasMessageContaining("TopUpRequest");
  }
}
