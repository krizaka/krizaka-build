package com.krizaka.test.sql;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Locates a repository's {@code infra/initdb} directory — the bootstrap SQL a service owns and its
 * integration tests load into a real database.
 */
public final class InitDb {

  private InitDb() {}

  /**
   * Walks up from {@code startDir} to the first {@code infra/initdb} directory.
   *
   * <p>A test runs with the module as its working directory; the SQL lives at the repository root.
   * Walking up finds it from any module of the repository, and inside a workspace that clones the
   * repository at any depth.
   *
   * @param startDir where to start, usually {@code Path.of(System.getProperty("user.dir"))}
   * @return the {@code infra/initdb} directory
   * @throws IllegalStateException when no ancestor has one
   */
  public static Path locate(Path startDir) {
    Path current = startDir.toAbsolutePath();
    while (current != null) {
      Path candidate = current.resolve("infra").resolve("initdb");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("Could not locate infra/initdb walking up from " + startDir);
  }
}
