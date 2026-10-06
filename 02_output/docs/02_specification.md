# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Conference registration: a single public registration form (frontend) and a REST backend that prices, stores and confirms registrations, and gives the organizer read access and an invoicing export. Contracts: `docs/02_contracts/`. Decisions: `docs/decisions-log.md`.

## 1. Components and deployment

| Component | Folder | Runs as | Talks to |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot jar in an `eclipse-temurin` JRE image, user `app` (uid 10001) | PostgreSQL (JDBC), SMTP server |
| frontend | `02_output/frontend` | static files built by Vite (`node` build image), served by `nginx` as user `nginx` on port 8080; `/api/` is proxied to the backend | backend via `/api` only (AR-01) |
| PostgreSQL | image `postgres` | database `registration`, user from `APP_DB_USER` | – |
| Mailpit | image `axllent/mailpit` | local and test SMTP catcher | – |

Local stack: `02_output/docker-compose.yml`, all ports bound to 127.0.0.1: frontend 3000, backend 8080, Mailpit web/API 8025 (SMTP 1025 internal only); PostgreSQL is not published. Secrets come from the repository-root `.env` (`docker compose --env-file ../.env up --build`). Every `APP_*` setting is passed through to the backend unset-if-absent, so the application default applies (environments.md). Container health checks use the backend readiness endpoint and the nginx root (NFR-02).

The frontend and the API share one origin (nginx proxy in local and production, Vite proxy in development), so the backend allows no cross-origin requests.

## 2. Backend architecture (AR-02, AR-03)

Layered, by technical responsibility, with one root package `si.confreg.registration`:

| Layer (package) | Contains | May depend on |
|---|---|---|
| `api` | REST controllers, request/response DTOs, error mapping | `application`, `domain` |
| `application` | use cases (`RegistrationService`, `RegistrationQueryService`, `ExportService`, `ConfirmationMailService`), pricing (`PricingService`), settings (`AppProperties`, `@ConfigurationProperties`), ports (`TimeSource`, `MailGateway`) | `domain` |
| `domain` | `Registration` entity, `PayerType`, `Price`, `RegistrationRepository` (Spring Data) | nothing in the app |
| `infrastructure` | `RequestTimeSource` (clock, AR-05), `SmtpMailGateway` (AR-07), request filters (rate limit, body size, HTTPS-only credentials, test clock) | `application`, `domain` |
| `config` | Spring wiring: security, startup checks, scheduling, web filters registration | all; nothing depends on `config` |

Why: the application is small and has one story; a classic layered split keeps the pricing rule and the registration use case free of HTTP, SMTP and clock details, so they are unit-testable, while Spring Data repositories stay as interfaces next to the entity. A hexagonal split would add mapping layers without a second adapter to justify them.

ArchUnit rules (in `src/test/.../architecture`, phase 3 acceptance level, checked in phase 6):

- ARCH-1 layered architecture exactly as the table above: `api` and `infrastructure` are accessed only by `config`; `application` only by `api`, `infrastructure`, `config`; `domain` by every other layer; `config` by none.
- ARCH-2 no cycles between the slices `si.confreg.registration.(*)..` (AR-03).
- ARCH-3 only `infrastructure.mail` uses `org.springframework.mail` / `jakarta.mail` (AR-07).
- ARCH-4 no class except `infrastructure.clock` calls `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `ZonedDateTime.now()`, `OffsetDateTime.now()` or `Clock.system*()` (AR-05).
- ARCH-5 only `api` uses `org.springframework.web.bind.annotation`.

## 3. Configuration (ES-01, AR-04)

All read through `application.AppProperties`, bound with `${SETTING:default}` placeholders in `application.yml` so that an environment variable or a property of the same name overrides the default; the services read the values when they price or validate, never from constants. Each can be overridden by the environment variable of the same name.

| Setting | Default (`application.yml`) | Meaning |
|---|---|---|
| `APP_CONFERENCE_TZ` | `Europe/Ljubljana` | zone for business dates (AR-05) |
| `APP_EARLY_BIRD_DEADLINE` | `2026-07-31` | last early-bird date, inclusive (D-10) |
| `APP_FEE_EARLY` / `APP_FEE_REGULAR` | `240.00` / `300.00` | net fees in EUR (D-09) |
| `APP_VAT_RATE` | `0.22` | VAT rate (D-09) |
| `APP_WORKSHOPS` | `W1=Requirements engineering for AI coding agents;W2=Data spaces in practice;W3=Secure software supply chains` | `id=title` pairs separated by `;` (D-15) |
| `APP_RATE_LIMIT_PER_HOUR` | `100` | per client and public endpoint (section 6) |
| `APP_TEST_CLOCK` | `disabled` | `enabled` activates `X-Test-Now` (section 5) |
| `APP_DB_URL`, `APP_DB_USER` | none (compose: `jdbc:postgresql://postgres:5432/registration`, `registration`) | database; password `POSTGRES_PASSWORD` |
| `APP_SMTP_HOST`, `APP_SMTP_PORT`, `APP_SMTP_TLS` | none (compose: `mailpit`, `1025`, `false`) | SMTP; credentials `SMTP_USERNAME`, `SMTP_PASSWORD` (optional, production) |
| `APP_MAIL_FROM` | `registration@conference.example` | sender address |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none | organizer login; startup fails if missing or the password is shorter than 16 characters |

Production is the Spring profile `prod` (`SPRING_PROFILES_ACTIVE=prod`). With `prod` the backend refuses to start when `APP_TEST_CLOCK=enabled` (SR-04) or `APP_SMTP_TLS` is not `true` (SB-04), and trusts `X-Forwarded-For`/`X-Forwarded-Proto` from the reverse proxy (`server.forward-headers-strategy=native`); outside `prod` forwarded headers are ignored.

Invalid configuration (unparsable fee, rate, date, zone, workshop list, duplicate workshop id) stops startup with a message naming the setting, never its value when it is a secret.

## 4. Registration API

Contract: `docs/02_contracts/registration-api.openapi.json` (OpenAPI 3.1). Names fixed by `architecture.md`; additions marked.

| Method and path | Access | Purpose | AC |
|---|---|---|---|
| `POST /api/registrations` | public, rate limited | register | 01–04, 06–19, 25–27 |
| `GET /api/registrations/{registrationNumber}` | organizer | read one registration | 28–30 |
| `GET /api/registrations/export` (added) | organizer | invoicing export, `.xlsx` | 05, 29 |
| `GET /api/registration-options?student=` (added) | public, rate limited | workshops and the price that applies now | 20, 22 |
| `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | public, no details | health (ES-09, NFR-02) | – |

Every other path answers 401 without credentials (security requirements: authenticated by default) and 404 with them.

### 4.1 Request `POST /api/registrations`

JSON (`Content-Type: application/json`, body at most 16 KiB, SR-02). Every string is trimmed; an empty string after trimming counts as absent.

| Field | Rule (D-13) | Error code |
|---|---|---|
| `firstName`, `lastName` | required, 1–100 characters, no control characters | `required`, `too_long`, `invalid` |
| `email` | required, at most 254 characters, `local@domain` with a dot in the domain, no whitespace or control characters | `required`, `too_long`, `invalid` |
| `payerType` | required, `private` or `company` | `required`, `invalid` |
| `companyName` | company: required, at most 200, no control characters; private: must be absent | `required`, `too_long`, `invalid`, `not_allowed` |
| `companyAddress` | company: required, at most 500, line breaks allowed, no other control characters; private: must be absent | same |
| `companyVatId` | company: optional, at most 30, letters, digits, space, `.`, `-`, `/`; private: must be absent | `too_long`, `invalid`, `not_allowed` |
| `workshops` | optional array; 0 or 1 element; element must be an id from `APP_WORKSHOPS` (D-11) | `too_many`, `invalid` |
| `student` (added, D-06) | optional boolean, default `false` | `invalid` |

Unknown properties are ignored. A body that is not valid JSON or has a wrong type gives 400 with `errors` naming the field when known. All field errors are reported together.

### 4.2 Processing

1. Validate (4.1). Reject → 400 `application/problem+json` `{type,title,status,detail,errors:[{field,code,message}]}`; nothing stored; no mail.
2. `now` = `TimeSource.now()` (section 5).
3. Price (`PricingService`, D-09, D-10): if `student` → 0.00/0.00/0.00, tier `student`; else `localDate = now` in `APP_CONFERENCE_TZ`; `net = localDate ≤ APP_EARLY_BIRD_DEADLINE ? APP_FEE_EARLY : APP_FEE_REGULAR` (tier `early`/`regular`); `vat = (net × APP_VAT_RATE)` rounded HALF_UP to scale 2; `gross = net + vat`. `BigDecimal` only.
4. Duplicate check on the normalised e-mail (trimmed, lower-cased with `Locale.ROOT`); duplicate → 409 problem `{detail:"A registration with this e-mail address already exists."}`. A unique index on the normalised e-mail closes the race (constraint violation also → 409) (D-12).
5. Store in one transaction with `registeredAt = now` (UTC), registration number from sequence `registration_number_seq` formatted `CR-%06d` (D-15).
6. Answer 201, `Location: /api/registrations/{registrationNumber}`, body = stored registration.
7. After commit, the confirmation mail is handed to the mail dispatcher (section 7).

### 4.3 Stored registration (response)

`registrationNumber`, `firstName`, `lastName`, `email` (as entered, trimmed), `payerType`, `companyName`, `companyAddress`, `companyVatId` (null for private or when absent), `workshop` (id or null), `netFee`, `vat`, `grossFee` (JSON numbers with exactly two decimals), plus added `student` (boolean) and `registeredAt` (ISO-8601 UTC).

### 4.4 Organizer endpoints

HTTP Basic, single in-memory user from `ORGANIZER_USERNAME`; the password is BCrypt-hashed at startup and the plain value discarded (SB-03). `GET /api/registrations/{n}`: 200 registration, 404 problem if unknown. `GET /api/registrations/export`: 200 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="registrations.xlsx"`; sheet `Registrations`, header row then one row per registration ordered by registration number, columns: Registration number, Registered at (conference time zone, `yyyy-MM-dd HH:mm:ss`), First name, Last name, E-mail, Payer type, Company name, Company address, Company VAT ID, Workshop, Student (yes/no), Net fee, VAT, Gross fee. Text is written as string cells (never formulas); amounts as numeric cells with format `0.00` (AC-001-05, D-07).

Missing or wrong credentials → 401 with `WWW-Authenticate: Basic realm="registration"` and no data (AC-001-29). Failed authentications count against the client's rate limit (SB-06).

### 4.5 `GET /api/registration-options`

Query `student` (boolean, default false). Answer: `{workshops:[{id,title}], conferenceTimeZone, earlyBirdDeadline, price:{tier, netFee, vat, grossFee}}`, where `price` uses the same `PricingService` and `TimeSource` as registration (AC-001-22).

### 4.6 Errors (ES-07, SB-07)

All errors are `application/problem+json` with a fixed, user-readable `title`/`detail`; no stack traces, class names, SQL or input values. 413 for an oversized body, 415 for a non-JSON body, 405/404 for unknown routes (after authentication), 429 with `Retry-After` (seconds) for rate limiting, 500 `"An unexpected error occurred."`.

## 5. Time (AR-05)

`TimeSource` is the only source of the current instant. `RequestTimeSource` returns `Instant.now(systemClock)` unless the test clock is enabled and the current request carried a valid `X-Test-Now` (ISO-8601 instant, e.g. `2026-07-31T21:59:59Z`), which a filter stores for the duration of that request. An invalid `X-Test-Now` with the test clock enabled → 400. With the test clock disabled the header is ignored. All timestamps are stored as `timestamptz` and Hibernate uses `UTC` (`hibernate.jdbc.time_zone=UTC`). Background work (mail retry) uses the system clock.

## 6. Security controls

| Control | Design |
|---|---|
| Authentication (security requirements, SB-02) | Spring Security: public = `POST /api/registrations`, `GET /api/registration-options`, health endpoints; everything else `authenticated()` with HTTP Basic; stateless (no session, no cookies); CSRF disabled because no cookie or session authenticates any request; CORS: no cross-origin access. |
| Rate limit (SB-06, `APP_RATE_LIMIT_PER_HOUR`) | per client IP (remote address; the forwarded address only in `prod`, section 3) and per bucket: `register` (POST), `options`, `auth-failure` (failed organizer logins). Fixed one-hour window counted from the client's first request in it, in memory (single instance). Over the limit → 429 with `Retry-After`. Not applied to health. |
| Body size (SR-02) | filter rejects `Content-Length` > 16 KiB and stops reading a chunked body at 16 KiB → 413. Tomcat `max-http-form-post-size` also 16 KiB. |
| Credentials over HTTPS only (SR-03) | a request carrying `Authorization` that is not secure (`request.isSecure()`, honouring `X-Forwarded-Proto` only in `prod`) and does not come from a loopback address is answered 403 `"HTTPS is required for organizer access."` before authentication. "Localhost" means a loopback remote address or, outside `prod`, a private (site-local) address: in the local stack the published port is bound to 127.0.0.1 but Docker forwards host requests from its bridge gateway. In `prod` only HTTPS (via `X-Forwarded-Proto: https` from the proxy) or a loopback address is accepted (D-16). |
| Test clock (SR-04) | `StartupGuard` fails startup in profile `prod` if `APP_TEST_CLOCK=enabled`. |
| Personal data in logs (SR-01, SB-07, ES-07) | the application logs only registration numbers, counts and error classes; no request logging; Hibernate SQL and bind-parameter logging off; exception messages that may contain values (constraint violations, Jackson errors) are never logged with their message. |
| Mail content (SR-05) | plain text only (`text/plain; charset=UTF-8`), no HTML; subject is a fixed text plus the registration number; recipient is the validated address (no whitespace or control characters, so no header injection); names are inserted as text after control characters are rejected at validation. |
| Headers (SB-10) | backend: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. nginx: `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`, plus the same `nosniff`, `DENY`, `no-referrer`. |
| Queries and output (SB-05) | JPA/Spring Data with bound parameters only; React escapes all rendered text; no `dangerouslySetInnerHTML`. |
| Least privilege (SB-11) | both images run as non-root users; database user owns only the `registration` database. |
| Personal data minimisation (SB-12, SB-13, SB-14) | only the fields of the fixed API plus `student`; purposes and retention as in `security-requirements.md` (retention defined by the organizer outside this project); no consent is collected because processing serves the registration contract, so nothing is preselected (the student checkbox is unchecked by default). |
| Scans (SB-08, SB-09) | OWASP Dependency-Check, `npm audit`, Semgrep, SpotBugs, PMD, Gitleaks in phase 6. |

## 7. Confirmation e-mail (AC-001-04, 25–27, AR-07, D-14)

Contract: `docs/02_contracts/confirmation-email.json`. `SmtpMailGateway` is the only sender. Column `confirmation_sent_at` (null until sent) and `confirmation_attempts` on the registration. After commit, `ConfirmationMailService` is triggered on a single-threaded executor; a scheduled job on the same executor retries every 30 s all registrations with `confirmation_sent_at IS NULL`, oldest first. A send is marked done in its own transaction right after SMTP accepts it, so a mail is sent once per registration under normal operation (at-least-once if the process stops between SMTP acceptance and the update). Failures are logged with registration number and exception class only.

## 8. Storage (AR-06, ES-08, NFR-01)

Contract and migration `V1__create_registration.sql`: `docs/02_contracts/registration-storage.sql` (copied verbatim to `backend/src/main/resources/db/migration/`). Database encoding UTF-8; `varchar` columns hold Slovenian characters unchanged. Flyway is the only schema writer; `spring.jpa.hibernate.ddl-auto=validate`.

## 9. Frontend (AR-01, AC-001-20 … 24)

Contract: `docs/02_contracts/registration-form.json`. One page, React, no router. On load it calls `GET /api/registration-options?student=false` and renders the form; toggling "I am a student" re-fetches the price. Labels are associated with inputs; errors use `aria-describedby` and `role="alert"`. Payer type radio buttons (`private` default); company fields shown only for `company` and omitted from the request otherwise. Workshops are radio buttons including "No workshop" (default). Submit disables the button while sending. 201 → confirmation panel with registration number, workshop and amounts; 400 → field errors next to the fields, values kept; 409, 413, 429, 5xx or network failure → one message at the top, values kept. No data is stored in the browser.

## 10. Build, test and check commands (ES-05)

As recorded in `docs/00_preflight-report.md` ("Component commands"). Acceptance and end-to-end tests live under paths containing `acceptance` or `e2e` (`general/phases.md`).

## Traceability

| AC / SR / SB / NFR / AR | Section |
|---|---|
| AC-001-01 | 4.1, 4.2, 4.3, 9 |
| AC-001-02 | 4.2 step 3, 3 |
| AC-001-03 | 4.1 (`student`), 4.2 step 3 |
| AC-001-04 | 7, contract `confirmation-email.json` |
| AC-001-05 | 4.4 (export) |
| AC-001-06, AC-001-07 | 4.2 step 3, 5 |
| AC-001-08 | 4.2 step 3, 4.3 |
| AC-001-09, AC-001-10 | 4.2 step 3 |
| AC-001-11 … AC-001-15, AC-001-19 | 4.1 |
| AC-001-16, AC-001-17 | 4.1 (`workshops`), 3 (`APP_WORKSHOPS`) |
| AC-001-18 | 4.2 step 4, 8 |
| AC-001-20 … AC-001-24 | 9, 4.5 |
| AC-001-25, AC-001-26 | 7, contract `confirmation-email.json` |
| AC-001-27 | 7 |
| AC-001-28 … AC-001-30 | 4.4, 6 (authentication) |
| SR-01 | 6 (personal data in logs) |
| SR-02 | 4.1, 6 (body size) |
| SR-03 | 6 (credentials over HTTPS only) |
| SR-04 | 3, 5, 6 (test clock) |
| SR-05 | 6 (mail content), 7 |
| SB-01 | 4.1 |
| SB-02 | 4.4, 6 |
| SB-03 | 4.4, 3 |
| SB-04 | 3 (`prod` requires SMTP TLS), 1 (HTTPS at the external proxy) |
| SB-05 | 6 (queries and output), 4.4 (string cells) |
| SB-06 | 6 (rate limit) |
| SB-07 | 4.6, 6 |
| SB-08, SB-09 | 6 (scans) |
| SB-10 | 6 (headers) |
| SB-11 | 1, 6 (least privilege) |
| SB-12, SB-13, SB-14 | 6 (personal data minimisation) |
| NFR-01 | 8, 7 (`charset=UTF-8`) |
| NFR-02 | 1, 4 (health) |
| AR-01 | 1, 9 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 |
| AR-05 | 5, 2 (ARCH-4) |
| AR-06 | 8 |
| AR-07 | 7, 2 (ARCH-3) |
| AR-08 | 4.4 (export only), D-07 |
| DoD-P01 | 1, 4.2 |
| ES-01 … ES-09 | 3, 1, 10, 6, 8, 4 |
