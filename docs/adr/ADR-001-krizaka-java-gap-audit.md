# ADR-001 — Krizaka Java: gap audit and roadmap

- **Status:** accepted, 2026-10-20
- **Tracking:** [krizaka/krizaka-build#65](https://github.com/krizaka/krizaka-build/issues/65) (part of the peer review
  [krizaka/krizaka-ui#434](https://github.com/krizaka/krizaka-ui/issues/434))
- **Live status:** the « Features » and « Configuration » pages of [java.krizaka.com](https://java.krizaka.com/features)

## Context

On 2026-10-20 colleagues (developers, an architect, an engineer) read java.krizaka.com and said: **« Krizaka Java is too
weak in configuration and features. »** This ADR takes the remark seriously: it audits every `com.krizaka` artifact
against what a Spring Boot 4 team expects of a serious building-blocks library, says honestly what is there, what is
partial and what is missing, and orders the work by its value to an adopter.

The audit read the code of `main` in the five repositories and the jars published on Maven Central (0.2.0 for
`krizaka-build` and `krizaka-platform-kit`, 0.1.0 for the domain repositories).

## The building blocks, one by one

| Artifact | What it does | Auto-configuration | `@ConfigurationProperties` (metadata on 0.2.0) | Conditions and extension points | Docs |
| :-- | :-- | :-- | :-- | :-- | :-- |
| `krizaka-parent` | Build of every Krizaka JVM artifact: Java 21, pinned plugins, Spotless, JaCoCo, OpenRewrite, Central requirements (sources, javadoc, japicmp) | — | — | properties a child overrides | README, `build` guide |
| `krizaka-bom` | The compatible set of every `com.krizaka` artifact | — | — | import scope | README, `start` guide |
| `krizaka-test-support` | ArchUnit rules (`CodeRules`, `ArchitectureRules`, `FrozenRules`…), `AbstractContainerIntegrationTest` (PostgreSQL + RabbitMQ, singletons), `EventContractTest`, `InitDb` | — | — | abstract base classes | README |
| `krizaka-security` | HS256 session-token verification, `roles` claim → authorities without prefix, `SecurityBaseline` (stateless, preflight and health open, `/internal/v1/**` for `SERVICE`, deny by default), `ServiceTokenProvider` | `SessionJwtAutoConfiguration`, on `krizaka.security.jwt.secret` | `krizaka.security.jwt.secret` — **no metadata generated** (processor not run) | `JwtDecoder`, `JwtAuthenticationConverter` back off | `security` guide |
| `krizaka-messaging` | Events through the outbox (envelope in AMQP headers), quorum queue + DLQ in one line, listener retry then DLQ, `MessageDedup` (JDBC, memory), outbox relay with leasing | 5 auto-configurations | 15 properties — metadata generated, **no defaults shown**, raw Javadoc in descriptions, `retry.max` shows the description of `max-attempts`; the outbox lease hard-coded | every bean `@ConditionalOnMissingBean`; `OutboxStore` is the app's | `messaging`, `events` guides |
| `krizaka-web` | RFC 9457 Problem Details with stable codes and `requestId`, `X-Request-Id` → MDC, Jackson 3 defaults, cursor pagination, declared CORS; imported by `@WebMvcTest` | `KrizakaWebAutoConfiguration` + environment post-processor | 2 properties — metadata hand-written | advice backs off on a custom `ResponseEntityExceptionHandler`, filter by name | `web` guide |
| `krizaka-observability` | Required service names as common meter tags; ECS logs, `health,info,prometheus` exposed, 10 % sampling as lowest-priority defaults | `KrizakaObservabilityAutoConfiguration` + post-processor | 3 required properties — metadata generated | — | `observability` guide |
| `krizaka-app-config` | App-version contract: public `/api/v1/app-config`, `426 Upgrade Required`, N-1 contract with `Deprecation`/`Sunset`, audited admin API | `KrizakaAppConfigAutoConfiguration` | 9 properties — metadata generated, **no defaults shown** | store, reader, controller, filter, admin gate back off | `app-config` guide |
| 5 starters (`web`, `security`, `rabbitmq`, `observability`, `app-config`) | One dependency per capability: the module + the Spring Boot starters it needs | (the modules') | — | — | README, `start` guide |
| `krizaka-users-*` | Identity service (accounts, sessions, OAuth federation, profiles, admin with Law 25 export/erasure) and its typed client | `UsersClientAutoConfiguration`, on `krizaka.users.client.base-url` | 4 properties — **no metadata generated**; **no HTTP timeouts** | `UserDirectoryClient` backs off | `users` guide |
| `krizaka-billing-*` | Credits ledger, subscriptions, entitlements, pack pricing and its client (no-op when disabled) | `BillingClientAutoConfiguration` (a plain `@Configuration`) | 5 properties — **no metadata generated**; **the record's `toString()` printed the service secret** | every port has a no-op fallback | `billing` guide |
| `krizaka-notifications-*` | Notification service: email, SMS, signed webhooks (Standard Webhooks) | service only | service properties | — | `notifications` guide |

## Against what a Spring Boot 4 team expects

**Shipped** means in a released or merged module, tested. **Partial** means part of it is there. **Missing** means not in
any building block — the « Promote from » column says where Krizaka already runs it.

| Capability | Status | What is there | What is missing | Promote from |
| :-- | :-- | :-- | :-- | :-- |
| Starters with sensible defaults | Shipped | 5 starters, lowest-priority defaults, back-off everywhere | persistence starter; client starters | — |
| Full configurability | Partial | most knobs are properties | outbox lease (hard-coded), users-client timeouts, relay threads, dedup retention | — |
| Properties metadata | Partial | generated for 4 of 5 kit modules | `krizaka-security`, both clients; defaults; clean descriptions; a reference | — |
| Actuator health / info | Missing | exposure defaults only | outbox, dedup, app-config health; `krizaka` info | — |
| Micrometer observations and tracing | Partial | common tags; OTel tracing and Prometheus in the starter | observations of the relay, the dedup, the publish | — |
| Test slices and Testcontainers | Partial | `AbstractContainerIntegrationTest`, `@WebMvcTest` imports `krizaka-web`, ArchUnit rules | `@ServiceConnection`, lazy containers (#63), fixtures for the kit's stores | — |
| Security integration | Shipped | HS256 verification, baseline chain, service tokens | asymmetric tokens and JWKS, per-caller service tokens (platform-kit#11) | — |
| Resilience (timeouts, retries, circuit breaking) | Partial | consumer retry + DLQ; billing-client timeouts | users-client timeouts, HTTP retries, circuit breaker | Spring Framework 7 `@Retryable` / `@ConcurrencyLimit` |
| Idempotency | Partial | consumer side (`MessageDedup`) | HTTP `Idempotency-Key` | Orazaka Studio, ADR-083 (`IdempotentRunStartService`) |
| Outbox | Shipped | relay, leasing, purge, envelope | a reference JDBC store and its schema (platform-kit#27) | — |
| Rate limiting | Missing | — | a starter (429 + `Retry-After` as Problem Details) | `krizaka-users` `AuthRateLimitFilter`, Orazaka `RateLimitFilter` (Bucket4j) |
| Audit | Missing | app-config audits its admin changes | an audit trail module | app-config, users `AdminAuditEntry` |
| Multi-tenancy | Missing | — | tenant resolution, MDC, observation key | users#31 |
| i18n | Missing | app-config localizes its message | Problem Details localized by code | — |
| Error handling (RFC 9457) | Shipped | `ProblemDetailsAdvice`, stable codes, 422 with `errors` | localized details | — |
| OpenAPI | Missing | — | springdoc integration with the problem schema; `openapi.json` per `*-api` (users#4) | — |
| Virtual threads | Partial | the services enable them | the kit's schedulers on virtual threads | — |
| Native image hints | Missing | — | `RuntimeHints`, an AOT smoke test | — |
| Migration support | Missing | users uses Flyway internally | migrations for the kit's tables (outbox, dedup, idempotency) | platform-kit#27 |
| Webhook signatures | Missing (in the kit) | the notifications service signs (Standard Webhooks) | sign and verify as a module | `krizaka-notifications` `WebhookSigner` |

Already promoted from Orazaka, for the record: the outbox **leasing** (platform-kit#45), the **Problem Details** contract
(`krizaka-web`), the consumer **dedup**.

## Decision

1. The colleagues are right on configuration and operability; the kit's guarantees (outbox, dedup, error contract,
   security baseline) are sound but **invisible** — to an IDE, to `/actuator`, to a trace — and the HTTP side lacks the
   idempotency and rate limiting a real API needs.
2. Work in slices, each test-first, merged on `main`, documented on java.krizaka.com the same day, released with the
   November train (no Maven Central publish before November: the October quota is spent).
3. java.krizaka.com shows an **honest** « Features » matrix (shipped / partial / planned, each shipped line backed by a
   file the site's build checks exists) and a « Configuration » reference generated from the metadata — never typed.

## Roadmap, by value to adopters

| # | Slice | Why first | Tracking |
| :-- | :-- | :-- | :-- |
| 1 | Configuration metadata with defaults for every property, clean descriptions, the generated reference | the first thing a team touches; the remark itself | platform-kit#47 |
| 2 | Actuator health/info and Micrometer observations | a block nobody can watch is a block nobody runs in production | platform-kit#48 |
| 3 | HTTP `Idempotency-Key` starter, promoted from Orazaka | every API that takes money or starts work needs it; proven in Orazaka | platform-kit#49 |
| 4 | Persistence starter: reference JDBC `OutboxStore`, Flyway migrations, scheduled dedup purge | today every adopter writes its own store and schema | platform-kit#27 |
| 5 | Resilience of the clients and a rate limiting starter | the clients sit on the request path | build#65 |
| 6 | Webhooks module, asymmetric tokens (JWKS) | security and integration | build#65, platform-kit#11 |
| 7 | OpenAPI, test support (`@ServiceConnection`), audit, i18n of problems | developer experience | build#65 |
| 8 | Virtual threads, native image hints, multi-tenancy | platform reach | build#65, users#31 |

## Consequences

- Every new property ships with its description and default, or `ConfigurationReferenceTest` fails the build.
- The site's build fails when a « shipped » feature names a file that does not exist.
- Releases stay paused until November; the merged slices wait in the release-please pull requests.
