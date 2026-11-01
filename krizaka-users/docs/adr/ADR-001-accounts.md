# ADR-001 — Krizaka accounts: the thin community layer over krizaka-users

- **Status**: Accepted; built and tested in the former `krizaka/krizaka-accounts` repository, **never deployed**.
- **Date**: 2026-10-16 (moved here 2026-10-30, krizaka-internal#752).
- **Why it is here**: `krizaka-accounts` held no code in its default branch, only this design (README, AGENTS). The
  founder merged or deleted repositories that exist for nothing (« au pire on les recrée au bon moment »). The
  repository was deleted; its history is a bundle in `~/.krizaka-private/repo-backups/krizaka-accounts-2026-10-30.bundle`.
  It is recreated, or built as a module of this repository, when `accounts.krizaka.com` is approved (cost: one App Platform instance).
- **The decision of record** is the org ADR: [ADR-001 in krizaka/.github](https://github.com/krizaka/.github/blob/main/docs/adr/ADR-001-krizaka-accounts.md)
  (the sign-in standard of every product, linking by verified e-mail, native PKCE). This document keeps the design of the layer.

## Design (from the former README)

`accounts.krizaka.com` — krizaka-users (sign-in with GitHub, Google,
Apple, Facebook; sessions; roles; export and deletion) plus the thin Krizaka layer: the **community** of the
krizaka.com properties (comments with moderation, saved posts, reply e-mails). Decided in
[ADR-001](https://github.com/krizaka/.github/blob/main/docs/adr/ADR-001-krizaka-accounts.md). A Krizaka account is
not a product account: Orazaka, Yomizaka… keep their own users (House of Brands).

**Status: built and tested, not deployed.** The service needs one App Platform instance ($5/month); it waits on
Oussama's approval (ADR-001, *Cost*). The database `krizaka_accounts` and its pool already exist.

## What it serves

| | Endpoint | Who |
|:--|:--|:--|
| Sign in (browser) | `GET /api/v1/auth/web/{github,google,apple,facebook}/start?return_to=` → provider → `/callback` | anyone |
| Who am I · providers · sign out | `GET /api/v1/auth/web/session` · `GET …/providers` · `POST …/logout` | the allowed sites |
| Read a thread | `GET /api/v1/community/{site}/threads/{post}/comments` | anyone |
| Comment · edit · delete · report | `POST …/comments` · `PUT/DELETE /api/v1/community/comments/{id}` · `POST …/{id}/report` | signed in |
| Moderation queue · decide | `GET /api/v1/community/moderation?status=PENDING\|HIDDEN` · `POST …/moderation/{id}/{approve\|reject\|hide\|restore}` | `ROLE_ADMIN` |
| Saved posts | `GET/PUT /api/v1/me/saved?site=blog` · `DELETE /api/v1/me/saved/{site}/{slug}` | signed in |
| Reply e-mails · mute a thread | `GET/PUT /api/v1/me/notifications` · `POST /api/v1/me/notifications/mute` | signed in |
| Export · delete the account (Loi 25) | `GET /api/v1/me/export` · `DELETE /api/v1/me` `{"confirm":"DELETE"}` | signed in |
| One-click unsubscribe | `GET/POST /api/v1/community/unsubscribe?token=` | the e-mail's link |

The session is an HttpOnly, Secure, SameSite=Lax cookie on accounts.krizaka.com; every state-changing request that
carries it must come from an allowed origin. An account's first comment waits for a moderator; the parameters
(hold first, rate limit, length, edit window, report threshold) are rows of `community.settings`.

## Configuration

| Variable | Default | |
|:--|:--|:--|
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | — / `krizaka_accounts` / — | `krizaka_accounts` through `pool-accounts` (`prepareThreshold=0`) |
| `DATABASE_POOL_SIZE` | `2` | client connections (PgBouncer holds 2 server connections) |
| `IDENTITY_JWT_SECRET` · `IDENTITY_JWT_TTL` | — · `P30D` | session signing, lifetime |
| `AUTH_PUBLIC_URL` · `AUTH_ALLOWED_ORIGINS` | `https://accounts.krizaka.com` · blog, www | callback base, the sites |
| `AUTH_{GITHUB,GOOGLE,FACEBOOK}_CLIENT_ID/_SECRET`, `AUTH_APPLE_{CLIENT_ID,TEAM_ID,KEY_ID,PRIVATE_KEY}` | off | a provider is on when its id is set |
| `AUTH_SIGNUP_ENABLED` | `true` | `false`: existing accounts only |
| `ABUSE_CAPTCHA_SECRET_KEY` | off | Turnstile on every comment |
| `MAIL_API_KEY` · `MAIL_DOMAIN` · `MAIL_API_URL` · `MAIL_FROM` | off · `mg.krizaka.com` | reply e-mails (Mailgun) |
| `APP_BLOG_URL` | `https://blog.krizaka.com` | links in e-mails |

Schema: krizaka-users' `infra/initdb/10-identity.sql`, then `infra/initdb/20-community.sql` (kept in the bundle of the former repository).

## Rules kept from the former AGENTS.md


- **Identity changes go to krizaka-users**, never here: a provider, a sign-in rule, a token, linking. This repository
  holds the community (comments, saved posts, reports, reply e-mails) and the host's wiring.
- **Krizaka accounts are not product accounts** (House of Brands): no product calls this service for its users.
- **One database, one small pool** (`krizaka_accounts`, `pool-accounts` = 2): the cluster has 22 usable connections and
  they are all assigned. Schema: krizaka-users' `10-identity.sql`, then `infra/initdb/20-community.sql`.
- **Business values are rows of `community.settings`**, bounded in `CommunitySettings`; deployment values are
  environment variables named by bounded context (`DATABASE_`, `AUTH_`, `MAIL_`, `ABUSE_`, `APP_`).
- **Privacy (Loi 25)**: no IP address stored, no tracking; export and deletion stay complete when a table is added
  (add it to `AccountDataService` and to the context test).
- **Cost**: one `apps-s-1vcpu-0.5gb` instance. Anything more needs Oussama's written approval.

