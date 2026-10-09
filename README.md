<!-- krizaka-header -->
<div align="center">

<img src="https://raw.githubusercontent.com/krizaka/.github/main/profile/assets/krizaka.svg" alt="Krizaka" width="72">

# Krizaka Build

**One build for every Krizaka JVM artifact.**

The parent POM and the BOM behind every `com.krizaka` artifact on Maven Central: complete Central metadata, Java 21
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
| `com.krizaka:krizaka-bom` | pom | Every `com.krizaka` artifact at one version. Import it once; it never changes a third-party version. |

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
| `./mvnw verify -Prelease -Dgpg.skip` | everything Central requires except the signature: sources and javadoc jars (javadoc is linted) |
| `./mvnw deploy -Prelease` | signs every file and uploads the deployment to the Central Portal |

## Releasing to Maven Central

Releases are cut by CI from a `v*` tag (organisation pipeline:
[`krizaka/.github/.github/workflows/maven.yml`](https://github.com/krizaka/.github/blob/main/.github/workflows/maven.yml)).
The version comes from the tag (`v0.1.0` → `0.1.0`); every Krizaka JVM repository is released at the same version,
in dependency order: `krizaka-build`, then `krizaka-platform-kit`, then the domain services.

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
