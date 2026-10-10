# Changelog

Versions follow [Semantic Versioning](https://semver.org/) per repository; the BOM carries the compatible set
(`krizaka-bom` names, for each Krizaka repository, the release it was tested with). Releases are written by
[release-please](https://github.com/googleapis/release-please) from the Conventional Commits merged on `main`; the
hand-written history is under *Before release-please*.

## [0.3.0](https://github.com/krizaka/krizaka-build/compare/v0.2.0...v0.3.0) (2026-10-10)


### Features

* **test-support:** EventContractTest — events checked against the JSON Schema their -api publishes ([#7](https://github.com/krizaka/krizaka-build/issues/7)) ([164282a](https://github.com/krizaka/krizaka-build/commit/164282a23b4266b3f52eb9888e5e65ee2f8546a4))

## [0.2.0](https://github.com/krizaka/krizaka-build/compare/v0.1.0...v0.2.0) (2026-10-09)


### Features

* BOM with krizaka-observability and the starters, japicmp, releases by release-please ([#2](https://github.com/krizaka/krizaka-build/issues/2)) ([2635231](https://github.com/krizaka/krizaka-build/commit/2635231ff26d02c6d9ff78e7ee85005a4eb26bd6))
* **test-support:** AbstractContainerIntegrationTest — shared PostgreSQL and RabbitMQ for integration tests ([#1](https://github.com/krizaka/krizaka-build/issues/1)) ([9761832](https://github.com/krizaka/krizaka-build/commit/9761832039d4303587f98620713fd8275daf52cb))


### Bug Fixes

* **parent:** japicmp lets a never-released artifact through ([#5](https://github.com/krizaka/krizaka-build/issues/5)) ([a803000](https://github.com/krizaka/krizaka-build/commit/a803000a995505eb2305a10bf1135c577a5520df))
* **release:** release-please skips the -SNAPSHOT pull request ([#4](https://github.com/krizaka/krizaka-build/issues/4)) ([f21163e](https://github.com/krizaka/krizaka-build/commit/f21163ec67940eda5f43c724a4debd6b9fe86255))

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
