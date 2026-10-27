package com.krizaka.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

class QualityRulesTest {

  private static Document parse(String resource) throws Exception {
    try (InputStream in =
        QualityRulesTest.class.getResourceAsStream("/krizaka-quality/" + resource)) {
      assertThat(in).as(resource).isNotNull();
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    }
  }

  @Test
  void pmdRulesetCoversTheSonarFindingsWeHave() throws Exception {
    NodeList rules = parse("pmd-ruleset.xml").getElementsByTagName("rule");
    StringBuilder refs = new StringBuilder();
    for (int i = 0; i < rules.getLength(); i++) {
      refs.append(rules.item(i).getAttributes().getNamedItem("ref").getNodeValue()).append('\n');
    }
    assertThat(refs.toString())
        .contains("CognitiveComplexity")
        .contains("AvoidDuplicateLiterals")
        .contains("UncommentedEmptyMethodBody")
        .contains("ExcessiveParameterList")
        .contains("UnusedFormalParameter")
        .contains("AvoidReassigningLoopVariables");
  }

  @Test
  void spotbugsFilterSelectsSqlInjectionAndRegexBugs() throws Exception {
    NodeList list = parse("spotbugs-include.xml").getElementsByTagName("Bug");
    StringBuilder all = new StringBuilder();
    for (int i = 0; i < list.getLength(); i++) {
      var a = list.item(i).getAttributes();
      all.append(
              a.getNamedItem(a.getNamedItem("pattern") != null ? "pattern" : "category")
                  .getNodeValue())
          .append(',');
    }
    assertThat(all.toString()).contains("SQL_INJECTION_JDBC").contains("REDOS");
  }

  @Test
  void theEmptyExcludeFilterExcludesNothing() throws Exception {
    assertThat(parse("spotbugs-exclude-empty.xml").getElementsByTagName("Match").getLength())
        .isZero();
  }
}
