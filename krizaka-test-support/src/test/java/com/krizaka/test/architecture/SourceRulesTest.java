package com.krizaka.test.architecture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceRulesTest {

  @Test
  void aBeanReadingEnvironmentIsFound(@TempDir Path sources) throws IOException {
    Files.writeString(
        sources.resolve("PriceService.java"),
        "import org.springframework.core.env.Environment;\nclass PriceService {}\n");

    assertThatThrownBy(() -> SourceRules.assertNoEnvironmentInjection(sources))
        .hasMessageContaining("PriceService.java:1");
  }

  @Test
  void aConfigurationClassMayReadEnvironment(@TempDir Path sources) throws IOException {
    Files.writeString(
        sources.resolve("AppConfig.java"),
        "import org.springframework.core.env.Environment;\n@Configuration\nclass AppConfig {}\n");

    assertThatCode(() -> SourceRules.assertNoEnvironmentInjection(sources))
        .doesNotThrowAnyException();
  }

  @Test
  void anEmptySourceTreeIsNotAPass(@TempDir Path sources) {
    assertThatThrownBy(() -> SourceRules.assertNoEnvironmentInjection(sources))
        .hasMessageContaining("examined no");
  }

  @Test
  void virtualThreadsMustBeDeclaredNotMentioned(@TempDir Path module) throws IOException {
    Path resources = Files.createDirectories(module.resolve("src/main/resources"));
    Files.writeString(
        resources.resolve("application.yml"),
        "# spring.threads.virtual.enabled: true is mandatory\nspring:\n  application:\n    name: x\n");

    assertThatThrownBy(() -> SourceRules.assertVirtualThreadsEnabled(module))
        .hasMessageContaining("spring.threads.virtual.enabled=true is missing");

    Files.writeString(
        resources.resolve("application.yml"),
        "spring:\n  threads:\n    virtual:\n      enabled: true\n");
    assertThatCode(() -> SourceRules.assertVirtualThreadsEnabled(module))
        .doesNotThrowAnyException();
  }
}
