package com.krizaka.rewrite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RewriteTest;

/**
 * The style is found on the classpath, and with it OpenRewrite no longer folds imports into a star
 * import.
 */
class KrizakaJavaStyleTest implements RewriteTest {

  private static final String STYLE = "com.krizaka.rewrite.KrizakaJavaStyle";

  private static NamedStyles style() {
    return Environment.builder()
        .scanRuntimeClasspath()
        .build()
        .activateStyles(List.of(STYLE))
        .stream()
        .filter(s -> STYLE.equals(s.getName()))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void neverFoldsIntoAStarImport() {
    ImportLayoutStyle layout = style().getStyle(ImportLayoutStyle.class);
    assertThat(layout).isNotNull();
    assertThat(layout.getClassCountToUseStarImport()).isEqualTo(Integer.MAX_VALUE);
    assertThat(layout.getNameCountToUseStarImport()).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void keepsSixImportsOfOnePackageAsWritten() {
    // Autodetection folds a package of this size into `import java.util.*;` (the #133 incident).
    rewriteRun(
        spec ->
            spec.recipe(new OrderImports(false, null))
                .parser(JavaParser.fromJavaVersion().styles(List.of(style()))),
        java(
            """
            import java.util.ArrayList;
            import java.util.HashMap;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            class A {
              List<String> a = new ArrayList<>();
              Map<String, String> b = new HashMap<>();
              Set<String> c = new HashSet<>();
            }
            """));
  }
}
