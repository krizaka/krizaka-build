<!-- krizaka-header -->
<div align="center">

<img src="https://raw.githubusercontent.com/krizaka/.github/main/profile/assets/krizaka.svg" alt="Krizaka" width="72">

# Krizaka Build

**One build for every Krizaka JVM artifact.**

The parent POM, the BOM and the test kit behind every `com.krizaka` artifact on Maven Central: complete Central metadata, Java 21
conventions checked on every build, and a signed release in one command.

[![CI](https://github.com/krizaka/krizaka-build/actions/workflows/ci.yml/badge.svg)](https://github.com/krizaka/krizaka-build/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.krizaka/krizaka-bom?color=3b82f6&label=maven%20central)](https://central.sonatype.com/namespace/com.krizaka)
[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

[Open source at Krizaka](https://www.krizaka.com/en/open-source) · [Website](https://www.krizaka.com) · [Krizaka on GitHub](https://github.com/krizaka)

</div>
<!-- /krizaka-header -->

## What it provides

| Artifact | Packaging | Role |
|:---|:---|:---|
| `com.krizaka:krizaka-parent` | pom | Maven Central metadata (licence, developers, SCM, issues), Java 21, google-java-format checked at `validate`, unit tests (Surefire) and integration tests (Failsafe, `*IT`), JaCoCo, an enforced toolchain (Maven ≥ 3.9, Java ≥ 21, every plugin pinned), and the `release` profile. |
| `com.krizaka:krizaka-bom` | pom | Every `com.krizaka` artifact, at the release of each repository this BOM was tested with: the compatible set. Import it once; it never changes a third-party version. |
| `com.krizaka:krizaka-test-support` | jar (test scope) | The governance kit of the Krizaka repositories: `CodeRules` (hexagonal layering, one class per file, constructor injection, private state, mappers, domain purity), `SourceRules` (no `Environment` injection, virtual threads), `ConfigBindingRules` (beans and configuration types Spring can build), `InitDb` (locates `infra/initdb`), `AbstractContainerIntegrationTest` (one shared PostgreSQL + RabbitMQ per run, wired into Spring), `ServiceRoles` (Testcontainers) and `EventContractTest` (an event serialised by its producer, or read by a consumer's copy, checked against the JSON Schema its `-api` publishes at `events/<routing-key>.v<n>.json`, draft 2020-12). Every rule fails on an empty population — a rule that judges nothing passes nothing. |

## Use the BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.krizaka</groupId>
            <artifactId>krizaka-bom</artifactId>
            <version>0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.krizaka</groupId>
        <artifactId>krizaka-security</artifactId>
    </dependency>
</dependencies>
```

The BOM deliberately carries **only** Krizaka artifacts. The parent therefore has no `<dependencyManagement>` of its own:
a BOM imports its parent's managed versions, and importing `krizaka-bom` must never pin a Spring version you did not
choose. The artifacts are built and tested against Spring Boot `4.0.x` (property `spring-boot.version`).

## Build a Krizaka repository on it

```xml
<parent>
    <groupId>com.krizaka</groupId>
    <artifactId>krizaka-parent</artifactId>
    <version>0.1.0</version>
    <relativePath/>
</parent>
```

| Command | What runs |
|:---|:---|
| `./mvnw verify` | formatting check, compile with `-Xlint:all`, unit and integration tests, coverage report |
| `./mvnw spotless:apply` | formats the sources (google-java-format) |
| `./mvnw verify -Prelease -Dgpg.skip` | everything Central requires except the signature: sources and javadoc jars (javadoc is linted), and the binary compatibility of every jar with its latest release (japicmp) |
| `./mvnw deploy -Prelease` | signs every file and uploads the deployment to the Central Portal |

## Releasing to Maven Central

Each Krizaka JVM repository has its own [SemVer](https://semver.org/) version; **`krizaka-bom` carries the compatible
set** — one property per repository (`krizaka-platform-kit.version`, …) names the release it was tested with.

- **Conventional Commits** — pull request titles are checked by `commitlint` (the squash merge keeps the title).
- **release-please** keeps a release pull request open on `main` (`pom.xml` versions, CHANGELOG). Merging it tags
  `v<version>`, creates the GitHub Release and dispatches CI on the tag (a tag pushed by `GITHUB_TOKEN` starts no
  workflow by itself), which runs the organisation pipeline
  [`krizaka/.github/.github/workflows/maven.yml`](https://github.com/krizaka/.github/blob/main/.github/workflows/maven.yml):
  verify, sign, upload. After a release, release-please proposes the next `-SNAPSHOT`.
- **Order** — Central validates a POM against its parent *on Central*: release `krizaka-build` first and publish it;
  then move `krizaka-parent` (and `krizaka-build.version`) in the repositories built on it to that release, and
  release them (`krizaka-platform-kit`, then the domain services). Before tagging `krizaka-build`, set each BOM
  property to the version of that repository it ships with.
- **Binary compatibility** — the `release` profile compares every jar with its latest release on Central (japicmp)
  and fails on a binary-incompatible change. A deliberate break (a major, or a minor before 1.0) sets
  `japicmp.breakBuildOnBinaryIncompatibleModifications=false` in that repository for that release.
- **Signature** — the Bouncy Castle signer of `maven-gpg-plugin` reads `MAVEN_GPG_KEY` (ASCII-armoured private key) and
  `MAVEN_GPG_PASSPHRASE`; no `gpg` binary is needed.
- **Upload** — `central-publishing-maven-plugin`, server id `central`, a Central Portal user token in
  `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD`. Secrets live in the organisation, never in a repository.
- **Publication is manual** — `central.autoPublish` defaults to `false`: the deployment is validated, then waits on
  [central.sonatype.com](https://central.sonatype.com/publishing/deployments) until someone presses *Publish*. A release
  on Central can never be deleted, so it is checked by eye first. Pass `-Dcentral.autoPublish=true` once the process is
  trusted.

Releasing by hand works the same way:

```bash
export MAVEN_GPG_KEY="$(gpg --armor --export-secret-keys <key-id>)" MAVEN_GPG_PASSPHRASE=…
./mvnw versions:set -DnewVersion=0.1.0 -DprocessAllModules -DgenerateBackupPoms=false
./mvnw deploy -Prelease          # with the `central` server in ~/.m2/settings.xml
```

## Contributing

Issues and pull requests are welcome — see the organisation's
[contributing guide](https://github.com/krizaka/.github/blob/main/CONTRIBUTING.md) and
[security policy](https://github.com/krizaka/.github/blob/main/SECURITY.md). `main` is protected: every change is a pull
request with a green CI.

## License

[Apache License 2.0](LICENSE) © 2026 Krizaka
