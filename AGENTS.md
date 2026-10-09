# krizaka-build — Scope (agent-neutral)

> The build foundation of every **Krizaka** JVM artifact (`com.krizaka`, published on Maven Central). It is not part of
> a product: Orazaka consumes it (its `orazaka-parent` inherits `krizaka-parent`), and so can any application.
> When this repository is cloned inside the Orazaka workspace (`krizaka/krizaka-build`), the workspace contract
> ([`krizaka/orazaka/AGENTS.md`](https://github.com/krizaka/orazaka/blob/main/AGENTS.md)) applies as well.

## Rules of this repository

- **`krizaka-parent` has no `<dependencyManagement>`.** `krizaka-bom` inherits it, and a BOM carries its parent's
  managed versions into every importer. Importing `krizaka-bom` must change no third-party version.
- **`krizaka-bom` lists every published `com.krizaka` artifact, and only those**, each at its repository's version
  property (`krizaka-platform-kit.version`, …; default `${project.version}`). The BOM carries the compatible set; a
  new artifact in any Krizaka repository is added here in the same release.
- **Releases are release-please's.** Pull request titles are Conventional Commits; nobody edits a version or the
  generated part of the CHANGELOG by hand. japicmp (release profile) guards the SemVer boundary.
- **Every plugin is pinned** (enforcer `requirePluginVersions`). A plugin version moves in this repository, once, for
  every Krizaka artifact.
- **Nothing Orazaka-specific** (Spring AI, `.env` profiles, SonarCloud, governance rules) — those belong to
  `orazaka-parent` in `krizaka/orazaka-build`.
- Maven Central requirements are part of the build, not of the release day: CI runs `verify -Prelease -Dgpg.skip` on
  every change, so a missing javadoc or source jar fails the pull request.

## Definition of done

1. `./mvnw verify -Prelease -Dgpg.skip` is green.
2. Inside the Orazaka workspace, `./mvnw install` from the workspace root is green (every consumer still builds).
3. A change to the release mechanics is reflected in [README.md](README.md#releasing-to-maven-central) and in the
   organisation pipeline (`krizaka/.github/.github/workflows/maven.yml`).
