# Release notes

> Written in: phase 7 · Agent: writes

Version 0.1.0 · run `kyuhi-confreg-C2-r1` · story US-001 (REQ-REG-01).

## Delivered

- **Registration form** (frontend, AC-001-11): participant data, payer type (company fields only for company payers), optional workshop (none or one, from the configured list), confirmation with registration number, net fee, VAT and gross fee; field errors shown next to each field.
- **Registration API** (`POST /api/registrations`, public, AC-001-01 to AC-001-09):
  - fee by submission time in the conference time zone: early bird up to and including `APP_EARLY_BIRD_DEADLINE`, regular afterwards; VAT at the Slovenian rate for every payer, also from other EU states;
  - validation with one error per field (422), nothing stored and no e-mail on rejection;
  - company data stored for company payers only; no invoice or payment request;
  - repeated registrations kept, each with its own number, fee and confirmation;
  - exactly one plain-text UTF-8 confirmation e-mail per registration.
- **Organizer read** (`GET /api/registrations/{registrationNumber}`, HTTP Basic, AC-001-10).
- **Workshop list** (`GET /api/workshops`, public, read-only, D-10).
- **Protection:** per-client rate limit on public endpoints and failed logins (AC-001-12), 16 KiB body limit, credentials only over HTTPS or localhost, security headers, no personal data in logs, test clock refused outside `local`/`test`.
- **Operation:** Flyway-managed PostgreSQL schema, timestamps in UTC; health and readiness probes on an unpublished management port; non-root container images; local stack with `docker compose`.
- **Evidence:** 190 automated tests (52 backend and 10 frontend acceptance tests, 3 end-to-end tests, all frozen; 125 unit, architecture and integration tests); verification report in `docs/06_verification-report.md`.

## Known limitations

- The rate limiter keeps its counters in memory: they reset when the backend restarts and are not shared between several backend instances.
- A confirmation e-mail that cannot be sent is logged (registration number only) and not retried; the registration stays stored (D-09).
- The backend container image copies a jar built on the host (`./mvnw -B package -DskipTests`); `tech-stack.md` lists no JDK build image.
- Behind a reverse proxy, the client address for rate limiting comes from `X-Forwarded-For`, trusted from Tomcat's internal proxy ranges. The proxy must overwrite that header, as the frontend's nginx does; a client reaching the backend directly from an internal range could choose its own key.
- The organizer uses HTTP Basic (required by the architecture; accepted in D-17); there are no participant accounts.
- `poi-ooxml` is declared as listed in `tech-stack.md`, but no Excel export is built: no criterion asks for one.
- The frontend mutation score cannot be measured with Stryker 10.0.0 and vitest 5.0.3 (F-09, D-16).
- Development-only advisories remain in `qs` under Stryker (F-04, moderate); nothing from them ships.
- CVE-2025-7962 is suppressed for `angus-activation` only, as a false positive (D-04).

## Must be tested manually by a human

| What | Why it is not automated | How |
|---|---|---|
| Confirmation e-mail delivery to a real mailbox through the production SMTP server, with `SMTP_USERNAME`, `SMTP_PASSWORD` and `APP_MAIL_STARTTLS=true` | tests use Mailpit (`environments.md`) | register with a real address; check the e-mail arrives once, with readable č/š/ž, correct number and amounts, and is not marked as spam |
| TLS and the external nginx reverse proxy | not part of this repository | over HTTPS: form and API work; `X-Forwarded-Proto: https` reaches the backend; organizer read works over HTTPS and is refused (403) over plain HTTP from another host (SR-03); security headers present |
| Rate limiting behind the production proxy | the local stack has one client address | from two client addresses, check that one client's limit does not affect the other and that a client cannot change its key with its own `X-Forwarded-For` |
| Production start-up refusals | profile set by the deployment | start without profile `local`/`test` and `APP_TEST_CLOCK=enabled`: start must fail; start with a short `ORGANIZER_PASSWORD`: start must fail |
| Configured values for the live conference | values belong to the organizer | check `APP_*` settings (fees, deadline, VAT, workshops) of the deployment against the organizer's figures |
| Form usability and accessibility in the browsers the organizer supports | only Chromium is automated | keyboard-only registration, screen-reader labels and error announcements, small screens |

## Decisions pending review

| Decision | Choice made |
|---|---|
| D-01 | Docker Engine 29.8.1 used instead of the pinned 29.8.0 |
| D-02 | cloc image tag 2.10 contains cloc 1.98; kept |
| D-03 | Dependency-Check analysers needing unlisted credentials (OSS Index) and the duplicate Node analysers disabled |
| D-06 | gaps handled by the working rules (conservative choice, continue) rather than as blocking |
| D-07 | form behaviour: company fields only for company payers, errors next to fields, confirmation replaces the form |
| D-08 | validity rules: length limits, no control characters, no country-specific VAT ID check, company fields ignored for private payers |
| D-09 | e-mail failure keeps the registration, answers 201, no retry |
| D-11 | health endpoints unauthenticated on unpublished port 8081 |
| D-12 | `redocly/cli` 2.57.0 added as a dev-only OpenAPI validator |
| D-13 | plain-HTTP credential check skipped in the `local` and `test` profiles |
| D-14 | names of unnamed settings (`SPRING_DATASOURCE_*`, `SPRING_MAIL_*`, `APP_MAIL_FROM`, `APP_MAIL_STARTTLS`), `APP_WORKSHOPS` encoding, frontend port 3000 |
| D-15 | the acceptance manifest also lists the three phase 3 run logs |

Resolved with the human: D-04, D-05, D-10, D-16, D-17.
