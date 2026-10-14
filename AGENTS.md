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

## Tests

- **No test, not done.** A change to the generic test kit ships with its own tests; a change to the parent or the BOM
  is proven by the workspace build of every consumer (definition of done, 2), never assumed.
- **A fix starts with a failing test** (or a failing consumer build) that reproduces the bug.
- CI runs `./mvnw verify -Prelease -Dgpg.skip` on every pull request and must be green; nothing is skipped to get there.

## The documentation site — `site/` (java.krizaka.com)

- **Krizaka Java's documentation lives here**, because this repository owns what every `com.krizaka` artifact shares (the
  parent POM, the BOM) and is the one that releases first. Decision of 2026-10-14: each thing Krizaka does has its site,
  named like it and built from its repository — `www.krizaka.com` is the company, `ui.krizaka.com` Krizaka UI,
  **`java.krizaka.com` this**. A static Next.js export (`output: "export"`, no server) with Fumadocs, `@krizaka/ui`,
  `@krizaka/tokens`, `@krizaka/fonts`; dark and light, English at `/` and French at `/fr/`.
- **The guides are written here** (`site/content/docs/<page>.mdx` and `<page>.fr.mdx`, listed in `meta.json` and
  `meta.fr.json`); `npm run lint` (`scripts/check-content.mjs`) fails on a page without its twin, or a French page that
  drops a code block, a version token, a component or an internal link. Every user-facing string of the chrome is in
  `site/messages/{en,fr}.json` (`krizaka-i18n check`).
- **The building blocks map is generated, never written**: `scripts/gen-building-blocks.mjs` reads the POMs of this repository
  and a shallow clone of `krizaka-platform-kit`, `krizaka-users`, `krizaka-notifications` and `krizaka-billing` (or
  `KRIZAKA_REPOS_DIR`) → `lib/generated/building-blocks.json` (git-ignored), checked by `lib/model.ts` at build. A fact the
  map lacks is added to that generator.
- **Versions are data**: the pages write `%krizaka-bom%` (an artifact id between percent signs); `scripts/gen-registry-versions.mjs`
  reads Maven Central and the build replaces them — a page never states a typed version. The site publishes nothing to
  Maven Central and its commits are `docs(site):` / `chore(site):` — never `feat:`/`fix:`, which would make release-please
  release the parent POM.
- **CI** (`.github/workflows/site.yml`) lints, tests and builds on every pull request; **deployment** is DigitalOcean App
  Platform's static site `krizaka-java-site` (`.do/app.yaml`, project `krizaka`), rebuilt on every push to `main`.
- Tests: `cd site && npm test` (`tests/*.test.mjs`). A change to a generator, a script or the content rules ships with its test.

## Definition of done

1. `./mvnw verify -Prelease -Dgpg.skip` is green.
2. Inside the Orazaka workspace, `./mvnw install` from the workspace root is green (every consumer still builds).
3. A change to the release mechanics is reflected in [README.md](README.md#releasing-to-maven-central) and in the
   organisation pipeline (`krizaka/.github/.github/workflows/maven.yml`).
