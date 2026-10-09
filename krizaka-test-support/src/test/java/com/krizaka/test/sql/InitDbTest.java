package com.krizaka.test.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InitDbTest {

  @Test
  void findsTheRepositoryInitDbFromADeepModule(@TempDir Path repository) throws IOException {
    Path initdb = Files.createDirectories(repository.resolve("infra/initdb"));
    Path module = Files.createDirectories(repository.resolve("users-service/src/test"));

    assertThat(InitDb.locate(module)).isEqualTo(initdb.toAbsolutePath());
  }

  @Test
  void saysWhereItLookedWhenThereIsNone(@TempDir Path nowhere) {
    assertThatIllegalStateException()
        .isThrownBy(() -> InitDb.locate(nowhere))
        .withMessageContaining("infra/initdb");
  }
}
