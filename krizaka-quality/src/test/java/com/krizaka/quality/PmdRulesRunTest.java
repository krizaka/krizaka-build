package com.krizaka.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.reporting.Report;
import org.junit.jupiter.api.Test;

/**
 * The ADR-015 rules run for real: each fires on the defect it names and stays silent on its cure.
 */
class PmdRulesRunTest {

  private static List<String> rulesFiredOn(String source) {
    PMDConfiguration config = new PMDConfiguration();
    config.setDefaultLanguageVersion(JavaLanguageModule.getInstance().getVersion("21"));
    try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
      pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource("krizaka-quality/pmd-ruleset.xml"));
      pmd.files().addSourceFile(FileId.fromPathLikeString("sample/Sample.java"), source);
      Report report = pmd.performAnalysisAndCollectReport();
      System.err.println(
          "DBG rulesets="
              + pmd.getRulesets().size()
              + " rules="
              + pmd.getRulesets().stream().mapToInt(r -> r.getRules().size()).sum()
              + " violations="
              + report.getViolations().size()
              + " cfg="
              + config.getRuleSetPaths()
              + " cfgerr="
              + report.getConfigurationErrors()
              + " perr="
              + report.getProcessingErrors());
      assertThat(report.getProcessingErrors()).isEmpty();
      return report.getViolations().stream().map(v -> v.getRule().getName()).distinct().toList();
    }
  }

  @Test
  void anElseIfLadderOverStringLiteralsIsFound() {
    String ladder =
        """
        package sample;
        class Sample {
          int of(String p) {
            if (p.equals("google")) { return 1; }
            else if ("github".equals(p)) { return 2; }
            else if (p.equalsIgnoreCase("gitlab")) { return 3; }
            return 0;
          }
        }
        """;
    assertThat(rulesFiredOn(ladder)).contains("KrizakaStringIfLadder");
  }

  @Test
  void aLadderOverConstantsIsFoundToo() {
    String ladder =
        """
        package sample;
        class Sample {
          static final String A = "a";
          static final String B = "b";
          static final String C = "c";
          int of(String p) {
            if (A.equals(p)) { return 1; }
            else if (B.equals(p)) { return 2; }
            else if (C.equals(p)) { return 3; }
            return 0;
          }
        }
        """;
    assertThat(rulesFiredOn(ladder)).contains("KrizakaStringIfLadder");
  }

  @Test
  void twoComparisonsOrAMapAreFine() {
    String fine =
        """
        package sample;
        import java.util.Map;
        class Sample {
          private static final Map<String, Integer> CODES = Map.of("google", 1, "github", 2);
          int of(String p) {
            if (p.equals("google")) { return 1; }
            else if (p.equals("github")) { return 2; }
            return CODES.getOrDefault(p, 0);
          }
        }
        """;
    assertThat(rulesFiredOn(fine)).doesNotContain("KrizakaStringIfLadder");
  }

  @Test
  void aSwitchStatementOverAStringIsFoundButAnEnumIsNot() {
    String onString =
        """
        package sample;
        class Sample {
          int of(String p) {
            switch (p) {
              case "a": return 1;
              default: return 0;
            }
          }
        }
        """;
    String onEnum =
        """
        package sample;
        class Sample {
          enum Kind { A, B }
          int of(Kind k) {
            switch (k) {
              case A: return 1;
              default: return 0;
            }
          }
        }
        """;
    assertThat(rulesFiredOn(onString)).contains("KrizakaSwitchOnString");
    assertThat(rulesFiredOn(onEnum)).doesNotContain("KrizakaSwitchOnString");
  }

  @Test
  void aControllerThatCatchesAndReturnsIsFoundButOneThatLetsItPropagateIsNot() {
    String catches =
        """
        package sample;
        @RestController
        class Sample {
          String create(String body) {
            try {
              return body.trim();
            } catch (IllegalArgumentException ex) {
              return "error";
            }
          }
        }
        @interface RestController {}
        """;
    String propagates =
        """
        package sample;
        @RestController
        class Sample {
          String create(String body) {
            return body.trim();
          }
        }
        @interface RestController {}
        """;
    String serviceCatches =
        """
        package sample;
        class Sample {
          String create(String body) {
            try {
              return body.trim();
            } catch (IllegalArgumentException ex) {
              return "error";
            }
          }
        }
        """;
    assertThat(rulesFiredOn(catches)).contains("KrizakaControllerErrorBody");
    assertThat(rulesFiredOn(propagates)).doesNotContain("KrizakaControllerErrorBody");
    assertThat(rulesFiredOn(serviceCatches)).doesNotContain("KrizakaControllerErrorBody");
  }

  @Test
  void theComplexityLimitsFireOnAMethodThatBranchesTooMuch() {
    StringBuilder body = new StringBuilder();
    for (int i = 0; i < 11; i++) {
      body.append("    if (x == ").append(i).append(") { n++; }\n");
    }
    String source =
        "package sample;\nclass Sample {\n  int of(int x) {\n    int n = 0;\n"
            + body
            + "    return n;\n  }\n}\n";
    assertThat(rulesFiredOn(source)).contains("CyclomaticComplexity", "NPathComplexity");
  }

  @Test
  void deeplyNestedIfsAreFound() {
    String source =
        """
        package sample;
        class Sample {
          int of(int a, int b, int c, int d) {
            if (a > 0) { if (b > 0) { if (c > 0) { if (d > 0) { return 1; } } } }
            return 0;
          }
        }
        """;
    assertThat(rulesFiredOn(source)).contains("AvoidDeeplyNestedIfStmts");
  }

  @Test
  void cleanCodeFiresNothing() {
    String source =
        """
        package sample;
        class Sample {
          /** Doubles. */
          int twice(int x) {
            return x * 2;
          }
        }
        """;
    assertThat(rulesFiredOn(source)).isEmpty();
  }
}
