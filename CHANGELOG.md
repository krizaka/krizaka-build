# Changelog

Versions follow [Semantic Versioning](https://semver.org/) per repository; the BOM carries the compatible set
(`krizaka-bom` names, for each Krizaka repository, the release it was tested with). Releases are written by
[release-please](https://github.com/googleapis/release-please) from the Conventional Commits merged on `main`; the
hand-written history is under *Before release-please*.

## Before release-please

Written by hand, in the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.

### Unreleased when release-please took over (shipped in the first release below it)

#### Added

- `krizaka-bom`: `krizaka-observability` and the four starters (`krizaka-spring-boot-starter-web`, `-security`,
  `-rabbitmq`, `-observability`); one version property per repository (`krizaka-platform-kit.version`,
  `krizaka-users.version`, `krizaka-notifications.version`, `krizaka-billing.version`, defaulting to the BOM's own
  version).
- `krizaka-parent`: `japicmp-maven-plugin` in the `release` profile — every jar is compared with its latest release on
  Maven Central (resolved by `build-helper:released-version`) and a binary-incompatible change fails the build
  (`japicmp.breakBuildOnBinaryIncompatibleModifications`, `japicmp.skip`).
- Releases by release-please and Conventional Commits pull request titles (`commitlint`).
- `krizaka-test-support`: `AbstractContainerIntegrationTest` — one PostgreSQL (`postgres:16-alpine`) and one RabbitMQ
  (`rabbitmq:4-management-alpine`) per JVM, shared by every integration test of the run and handed to Spring through
  `@DynamicPropertySource` (`spring.datasource.*`, `spring.rabbitmq.*`).

### Version 0.1.0

#### Added

- `krizaka-parent`: Maven Central metadata, Java 21 build conventions (google-java-format, Surefire/Failsafe, JaCoCo,
  enforced toolchain with pinned plugins), and the `release` profile (sources, javadoc, Bouncy Castle signature, Central
  Portal upload with manual publication by default).
- `krizaka-bom`: `krizaka-security`, `krizaka-messaging`, and the `krizaka-users`, `krizaka-notifications` and
  `krizaka-billing` contracts and clients.
- `krizaka-test-support`: generic ArchUnit code rules, source rules, configuration-binding rules, the `infra/initdb`
  locator and Testcontainers helpers (PostgreSQL, RabbitMQ).
