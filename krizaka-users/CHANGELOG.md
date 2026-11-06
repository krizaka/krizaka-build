# Changelog

All notable changes to this repository are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow [Semantic Versioning](https://semver.org/).
Every Krizaka JVM artifact is released at the same version.

## [0.3.0](https://github.com/krizaka/krizaka-users/compare/v0.2.1...v0.3.0) (2026-10-31)


### Features

* **security:** per-caller ES256 service keys, HS256 behind a dated cutover ([#106](https://github.com/krizaka/krizaka-users/issues/106)) ([339aaf5](https://github.com/krizaka/krizaka-users/commit/339aaf506f35c15f0e07be6361fc8f878c4ec839))

## [0.2.1](https://github.com/krizaka/krizaka-users/compare/v0.2.0...v0.2.1) (2026-10-31)


### Bug Fixes

* **build:** the modules of this repository are managed at project.version ahead of the BOM ([#105](https://github.com/krizaka/krizaka-users/issues/105)) ([5726cc2](https://github.com/krizaka/krizaka-users/commit/5726cc2684c52c3131395e15a97e8109e5ea4120))
* **ci:** maven.yml pin 6bfc6ea4, so the tag deploy writes packages (0.2.0 got 403) ([#98](https://github.com/krizaka/krizaka-users/issues/98)) ([34b370e](https://github.com/krizaka/krizaka-users/commit/34b370e3a219b7f4c1dfdbda95ad33634bc53e63))
* **ci:** the first published version is 0.2.1 (v0.2.0 is a dead tag, its deploy got 403) ([#100](https://github.com/krizaka/krizaka-users/issues/100)) ([f5f7fed](https://github.com/krizaka/krizaka-users/commit/f5f7fed2bea3af36c63256a1aca9ad719bfdceb2))
* **ci:** the release-as pin on 0.2.0 goes (v0.2.0 is a dead tag; the next release is 0.2.1) ([#102](https://github.com/krizaka/krizaka-users/issues/102)) ([011e698](https://github.com/krizaka/krizaka-users/commit/011e6986df79fcf31254deb088a7315856a13246))
* **deps:** krizaka-parent and BOM 0.3.3 (platform kit 0.4.3 on GitHub Packages); no upstream checkout ([#104](https://github.com/krizaka/krizaka-users/issues/104)) ([922dd57](https://github.com/krizaka/krizaka-users/commit/922dd578adc6fd5e6eadcc2e09a90a1ead192ad2))

## [0.2.0](https://github.com/krizaka/krizaka-users/compare/v0.1.0...v0.2.0) (2026-10-31)


### Features

* **accounts:** organizations that own accounts (orazaka[#372](https://github.com/krizaka/krizaka-users/issues/372)) ([#54](https://github.com/krizaka/krizaka-users/issues/54)) ([4927d8b](https://github.com/krizaka/krizaka-users/commit/4927d8b92c96a75b1d1b5e089f03c2b226654d30))
* **admin:** account administration — list, suspend, restore, administrator role, Law 25 export and erasure, audit trail; a context-load test of the service as deployed ([#23](https://github.com/krizaka/krizaka-users/issues/23)) ([05be3ad](https://github.com/krizaka/krizaka-users/commit/05be3ade9aff6823d1e67978f522f9d377e436a5))
* **api-keys:** scoped keys — a key can carry area:action scopes, answered at the token exchange for the edge to enforce ([#16](https://github.com/krizaka/krizaka-users/issues/16)) ([f0c4ab3](https://github.com/krizaka/krizaka-users/commit/f0c4ab39f7fc2094791ee9d7c14c1eb4e4d11f22))
* **audit:** refused and failed actions are audited with their outcome (ADR-085) ([#46](https://github.com/krizaka/krizaka-users/issues/46)) ([f93281a](https://github.com/krizaka/krizaka-users/commit/f93281ab9c1ae4bd4df94f0a088f830144f57f45))
* **audit:** the administration's audit keeps the request id (ADR-085, identity hop) ([#45](https://github.com/krizaka/krizaka-users/issues/45)) ([c45b286](https://github.com/krizaka/krizaka-users/commit/c45b28650b53e0aaba702b018f97e61be9877cf0))
* **auth:** durable lockout, breach recheck at sign-in, GitHub verifier on the vault (ADR-016 slice 2) ([#79](https://github.com/krizaka/krizaka-users/issues/79)) ([9b0fd68](https://github.com/krizaka/krizaka-users/commit/9b0fd68eb39d75014472cba86d4fd996f08d9ad0))
* **client:** krizaka.users.client.* in the generated metadata, described, with its default, and the reference java.krizaka.com renders ([#44](https://github.com/krizaka/krizaka-users/issues/44)) ([7bd4b61](https://github.com/krizaka/krizaka-users/commit/7bd4b61c47c96e3fd13fd4e6adcccaa17e103deb))
* **db:** schema changes are Flyway migrations that run on deploy (orazaka[#372](https://github.com/krizaka/krizaka-users/issues/372)) ([#62](https://github.com/krizaka/krizaka-users/issues/62)) ([b6c6ddb](https://github.com/krizaka/krizaka-users/commit/b6c6ddbefa86a45077a95bb95967fbfe15813c01))
* **events:** JSON Schema of evt.user.registered and evt.password.reset, checked against the published events ([#7](https://github.com/krizaka/krizaka-users/issues/7)) ([0a94d6d](https://github.com/krizaka/krizaka-users/commit/0a94d6d3aba70d54d4a9e0383c0f55e773ecdc1d))
* **identity:** the key exchange carries the owner's tier and requests per minute ([#34](https://github.com/krizaka/krizaka-users/issues/34)) ([9013512](https://github.com/krizaka/krizaka-users/commit/9013512e48c6d02e85da24cd002693d5d471ac28))
* **identity:** vault — identity secrets sealed under AUTH_VAULT_KEY (ADR-016 slice 1) ([#73](https://github.com/krizaka/krizaka-users/issues/73)) ([92a8bd4](https://github.com/krizaka/krizaka-users/commit/92a8bd42f81fcacb3e3d381c771b024e7dedef84))
* **jwt:** ES256 sessions + JWKS endpoint (platform-kit[#11](https://github.com/krizaka/krizaka-users/issues/11) slice 2) ([#60](https://github.com/krizaka/krizaka-users/issues/60)) ([ed42e5f](https://github.com/krizaka/krizaka-users/commit/ed42e5f68c3c2b615d125b691fba07feb81c573c))
* **outbox:** implement OutboxStore.append — event rows keep their messageId and envelope headers ([#2](https://github.com/krizaka/krizaka-users/issues/2)) ([500ff55](https://github.com/krizaka/krizaka-users/commit/500ff554c4d1905f0a2dd73659a0fddd348a46bb))
* **rate-limit:** seed the service tier on a fresh database (orazaka[#234](https://github.com/krizaka/krizaka-users/issues/234)) ([#38](https://github.com/krizaka/krizaka-users/issues/38)) ([3e4f604](https://github.com/krizaka/krizaka-users/commit/3e4f60463f730bb5780d9cf969ba0ef117472150))
* **users:** registration can be closed — AUTH_SIGNUP_ENABLED=false refuses every account creation (403) ([#15](https://github.com/krizaka/krizaka-users/issues/15)) ([9288efe](https://github.com/krizaka/krizaka-users/commit/9288efee6074a89a9989ed039072f3289d6c9f23))


### Bug Fixes

* **auth:** rate limiting and progressive lockout on login, oauth, register, forgot, reset, verify ([#17](https://github.com/krizaka/krizaka-users/issues/17)) ([#18](https://github.com/krizaka/krizaka-users/issues/18)) ([8ccbd29](https://github.com/krizaka/krizaka-users/commit/8ccbd293abe70e3a53f868cc2947f2f8a13aae46))
* **auth:** the rate-limit properties bean was silently dropped, so the service did not start ([#17](https://github.com/krizaka/krizaka-users/issues/17)) ([#20](https://github.com/krizaka/krizaka-users/issues/20)) ([50023f8](https://github.com/krizaka/krizaka-users/commit/50023f8cd242b71a220554f0038a171928ab0f38))
* **build:** client processor paths append to the parent's (NullAway plug-in not found) ([#77](https://github.com/krizaka/krizaka-users/issues/77)) ([3ecb4fc](https://github.com/krizaka/krizaka-users/commit/3ecb4fc8399d6351033d63c8d6d3366cd6fb3a11))
* **build:** users-client annotationProcessorPaths append to the parent's (NullAway profile, krizaka-build[#121](https://github.com/krizaka/krizaka-users/issues/121)) ([#76](https://github.com/krizaka/krizaka-users/issues/76)) ([ad8d353](https://github.com/krizaka/krizaka-users/commit/ad8d353ddeec38d65b5488b9b1865622a8528d0e))
* **deps:** security overrides of the Boot BOM (jackson 3.1.7/2.21.7, amqp-client 5.37.0, opentelemetry 1.62.0, tomcat 11.0.25) ([#27](https://github.com/krizaka/krizaka-users/issues/27)) ([dc37a4a](https://github.com/krizaka/krizaka-users/commit/dc37a4a4bf068a18ae34670101dd8ce4cb537043))
* **deps:** Spring Boot 4.0.8 in the root pom (parent 0.2.0 from Central still manages 4.0.7: netty, postgresql, webmvc alerts) ([#33](https://github.com/krizaka/krizaka-users/issues/33)) ([6c83b7a](https://github.com/krizaka/krizaka-users/commit/6c83b7a8364a68ca2e9e813750afdb55ae31ec42))
* **rate-limit:** seed the service tier in rate_limit_tiers too (the users foreign key) ([#40](https://github.com/krizaka/krizaka-users/issues/40)) ([d7afee0](https://github.com/krizaka/krizaka-users/commit/d7afee017b2645f8cbfbca52947f75079667837b))
* **security:** CryptoConverter as a Spring bean, no statics or getenv ([#28](https://github.com/krizaka/krizaka-users/issues/28)) ([#57](https://github.com/krizaka/krizaka-users/issues/57)) ([d27ae57](https://github.com/krizaka/krizaka-users/commit/d27ae57159f10884cbe0ed3b6fd91fc517ed7c3e))
* **security:** federated sign-in — Google aud and email_verified required, GitHub tokens bound to our app, blank client id = provider off, provider identity stored, unique usernames ([#32](https://github.com/krizaka/krizaka-users/issues/32)) ([2d636ac](https://github.com/krizaka/krizaka-users/commit/2d636ac4c5fba51a13c7069e242d9054d189fdfc))
* **security:** the scan's last test-fixture fingerprint is reviewed ([#95](https://github.com/krizaka/krizaka-users/issues/95)) ([82c22e4](https://github.com/krizaka/krizaka-users/commit/82c22e4d97aac2d3f03c018a5e73f2f99af3bcae))
* **test:** structure tests apply only the rules whose subject the module has (no vacuous rule, GOV-006); stores re-recorded ([#25](https://github.com/krizaka/krizaka-users/issues/25)) ([3274748](https://github.com/krizaka/krizaka-users/commit/3274748d3e7dc2513af40853ec3549f21e7efeba))


### Performance Improvements

* **oauth2:** verify Google ID tokens locally against the cached JWKS ([#42](https://github.com/krizaka/krizaka-users/issues/42)) ([#43](https://github.com/krizaka/krizaka-users/issues/43)) ([0d077d6](https://github.com/krizaka/krizaka-users/commit/0d077d69ee1d57d03d569d0b58025708112df41a))


### Documentation

* **adr:** keep the Krizaka accounts design here, krizaka-accounts is deleted (krizaka-internal[#752](https://github.com/krizaka/krizaka-users/issues/752)) ([#93](https://github.com/krizaka/krizaka-users/issues/93)) ([89e6909](https://github.com/krizaka/krizaka-users/commit/89e6909cc3bf81637345efbfc4fea85251c0b256))
* **agents:** no test, not done — every feature and fix ships with its tests ([#13](https://github.com/krizaka/krizaka-users/issues/13)) ([f283dea](https://github.com/krizaka/krizaka-users/commit/f283deaa6beaa804a6029311f56cc6e82efbbc32))
* **layout:** point AGENTS.md to the repository layout standard ([#56](https://github.com/krizaka/krizaka-users/issues/56)) ([4e474bb](https://github.com/krizaka/krizaka-users/commit/4e474bbf18c67f2ffda7dc41e09f526e01030cac))
* **readme:** Krizaka footer and dead tour image removed ([#19](https://github.com/krizaka/krizaka-users/issues/19)) ([0bf9de7](https://github.com/krizaka/krizaka-users/commit/0bf9de75c744c6b8336ba4e45811d5ed6b35c761))
* **readme:** SonarCloud badges ([#53](https://github.com/krizaka/krizaka-users/issues/53)) ([780ca6f](https://github.com/krizaka/krizaka-users/commit/780ca6f0fe3bd71a5d5ae4020e15c2848accc6e5))
* **readme:** the new Krizaka mark, light and dark, from the organisation profile ([#10](https://github.com/krizaka/krizaka-users/issues/10)) ([dfd0fe8](https://github.com/krizaka/krizaka-users/commit/dfd0fe8029a0214b364f65f389d3ffb6a507f5ad))
* standard README header with the animated Orazaka mark ([0ac46f9](https://github.com/krizaka/krizaka-users/commit/0ac46f9ccbb555971a573ced050b2e2111d7464e))

## [Unreleased]

### Changed

- **Schema changes are versioned Flyway migrations that run on deploy** (`krizaka-users-persistence/src/main/resources/db/migration`,
  `V2` admin_audit, `V3` its request id and outcome, `V4` organizations, orazaka#372): the loose `infra/migrations/*.sql`
  files are gone, nobody applies SQL by hand. An existing database is baselined at version 1 (what `10-identity.sql`
  creates); every migration is idempotent, so a fresh install (which already has the tables) passes through them.

### Added

- **Account administration** (`/api/v1/admin/users`, administrators only, every action audited): search and list,
  suspend and restore (the account can no longer sign in or use a key), grant or withdraw the administrator role (never
  your own, the last administrator keeps it), Law 25 export and erasure of the identity context's data (the typed
  e-mail confirms; the audit keeps the fact, not the person), and the audit trail (`GET /audit`). New table
  `admin_audit`: `infra/initdb/10-identity.sql`, and the Flyway migration `V2__admin_audit.sql` for an existing
  database (applied at startup). Sessions are stateless tokens: a token already issued is not revoked by a
  suspension in the services that only verify its signature. `IdentityServiceContextIT` starts the whole service as
  deployed and drives the administration through its HTTP chain.

- `krizaka.users.registration.enabled` (`AUTH_SIGNUP_ENABLED`, default `true`): `false` closes account creation —
  `POST /api/v1/auth/register` and the first federated sign-in of an unknown identity answer 403 `Registration is closed`,
  nothing is written; existing accounts keep signing in. Deployed environments run with it off.

- Event contracts: `krizaka-users-api` publishes the JSON Schema (draft 2020-12) of each event it emits —
  `events/evt.user.registered.v1.json`, `events/evt.password.reset.v1.json` — and `UserEventsContractTest`
  (`krizaka-test-support` `EventContractTest`) checks that the events serialised by the users service conform.
- The identity outbox implements `krizaka-messaging`'s `OutboxStore.append(NewOutboxMessage)`: a row written by an
  `EventPublisher` keeps its `messageId` and its envelope headers, and the relay publishes them as AMQP headers.
  `IdentityOutboxIT` proves it against the real `identity_outbox` table.

### Changed

- Built on `krizaka-parent` and `krizaka-platform-kit` **0.2.0** (released on Maven Central); this repository's
  version follows the parent (0.2.0, not yet released).
- The service host is never published to Maven Central: it is excluded from the Central bundle by name
  (`excludeArtifacts`) and ships as a Docker image; the `publishable-artifact-size` enforcer rule fails `verify` when a
  published jar exceeds 5 MB.
- `identity_outbox` gains `headers JSONB NOT NULL DEFAULT '{}'` (`infra/initdb/10-identity.sql`). An existing database
  needs `ALTER TABLE identity_outbox ADD COLUMN IF NOT EXISTS headers JSONB NOT NULL DEFAULT '{}'::jsonb;`.

### Fixed

- **Security — federated sign-in** (found while writing ADR-001, krizaka/.github):
  - a Google ID token issued to **any** application was accepted (its `aud` was never compared with our client id):
    the audience and `email_verified` are now required;
  - a GitHub access token issued to **any** OAuth app was accepted: it must now belong to ours
    (`POST /applications/{client_id}/token`, basic auth with the app's credentials);
  - an empty client id (`${GOOGLE_CLIENT_ID:}` unset) still turned the verifier on: a provider is now on exactly when its
    id is set;
  - a federated user was stored as `local` without its provider id, so the second sign-in of the same person found
    nobody and failed on the unique username: the provider identity is stored and found;
  - two people with the same display name could not both sign in: a short suffix is added, and an e-mail address is
    never used as a public username.

## [0.1.0]

First release as a Krizaka building block (formerly `orazaka-users`, part of the Orazaka platform).

### Added

- `krizaka-users-client`: typed HTTP client of the internal API (`UserDirectoryClient`), authenticated with a
  `SERVICE` token, cached per entry — replaces the two copies Orazaka's services carried.
- `UserDirectoryClient` port in `krizaka-users-api`.

### Changed

- Coordinates `com.krizaka:krizaka-users-*` (was `com.orazaka:orazaka-identity*`), packages `com.krizaka.users.*`.
- Configuration under `krizaka.users.*` and `krizaka.users-service.datasource.*` (was `orazaka.identity.*`).
- Security chain built on `krizaka-security`'s `SecurityBaseline`: the CORS preflight and `/error` are now open, as
  in every other service.
- Outbox relayed and messages deduplicated by `krizaka-messaging`.
- Default JWT issuer `krizaka-users` (set `IDENTITY_JWT_ISSUER` to keep another).
- `UserProfile` is `(userId, theme, attributes)`: application-defined attributes replace the AI-assistant fields
  (`voiceModel`, `primaryIndustry`, `aiBehavior`), which the hosting application now stores as attributes.
- Onboarding and feedback forms are the application's (`USERS_ONBOARDING_SCHEMA`, `USERS_FEEDBACK_SCHEMA`); the
  built-in ones ask for a theme, a language and a display name.
- Reserved preference namespaces are declared by the application (`krizaka.users.preferences.reserved-prefixes`).
- Queues and exchanges: events go to `krizaka.messaging.exchanges.events`; database `krizaka_users_db`, role
  `krizaka_users`, tables without a product prefix.

### Removed

- `user_model_prefs` and its persistence code: never read by any service.
