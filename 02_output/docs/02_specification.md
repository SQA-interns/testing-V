# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Story US-001, criteria AC-001-01 to AC-001-15. Contracts in `docs/02_contracts/`:

| Contract | Interface | Validated with |
|---|---|---|
| `registration-api.openapi.yaml` | Registration API (REST) | `redocly/cli:2.58.2 lint` (D-13): valid, 0 warnings |
| `registration-storage.sql` | Registration storage (SQL) | applied to `postgres:16.15-alpine` with `ON_ERROR_STOP` |
| `confirmation-email.yaml` | E-mail (SMTP) | YAML parser (PyYAML `safe_load`) |
| `registration-form.yaml` | Registration form (UI) | YAML parser (PyYAML `safe_load`) |

## 1. Components

| Component | Folder | Role | Runs as |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot 4.1.1 REST API, root package `si.confreg.registration` | container from `eclipse-temurin:21.0.10_7-jre-alpine`, non-root |
| frontend | `02_output/frontend` | React 19 single page with the registration form, built by Vite | container from `nginx:1.30.5-alpine` (build stage `node:24.13.0-alpine`), non-root |
| PostgreSQL | – | registration storage | `postgres:16.15-alpine` |
| Mailpit | – | local and test SMTP catcher | `axllent/mailpit:v1.31.1` |

The frontend calls the backend only under `/api` (AR-01): the frontend's nginx proxies `/api/` to the backend, so the browser sees one origin and no CORS is allowed.

## 2. Backend architecture (AR-02, AR-03)

Declared architecture: **layered by technical concern**, with a pure domain core. Reason: one story, one aggregate (registration), three outward interfaces (HTTP, SQL, SMTP); layers keep the fee and validation rules free of framework code so they can be tested and mutated alone.

| Package (`si.confreg.registration.`) | Contains | May depend on |
|---|---|---|
| `api` | REST controllers, request parsing, response DTOs, error handler (problem details) | `application`, `domain`, `config` |
| `security` | Spring Security configuration, rate-limit filter, body-size filter, transport check for credentials | `config` |
| `application` | use cases (`RegistrationService`), confirmation dispatch and retry scheduling | `domain`, `persistence`, `mail`, `config` |
| `domain` | fee calculation, validation rules, registration number generator, value types | `config` (read-only settings types) |
| `persistence` | JPA entity, Spring Data repository | `domain` |
| `mail` | the only component that sends e-mail (AR-07): message composition and SMTP send | `domain`, `config` |
| `config` | typed settings (`AppSettings`), the clock component (`ConferenceClock`), test-clock filter, startup guards | – (only Spring and JDK) |

ArchUnit rules (test level: unit/architecture, phase 5; listed here because they are the declaration):

| Rule | Checks |
|---|---|
| ARCH-1 | the layer table above (`layeredArchitecture()`, `whereLayer(...).mayOnlyBeAccessedByLayers(...)`) |
| ARCH-2 (AR-03) | `slices().matching("si.confreg.registration.(*)..").should().beFreeOfCycles()` |
| ARCH-3 (AR-07) | only `mail` accesses `org.springframework.mail..` and `jakarta.mail..` |
| ARCH-4 (AR-05) | only `config.ConferenceClock` calls `Instant.now`, `LocalDate(Time).now`, `ZonedDateTime.now`, `OffsetDateTime.now`, `System.currentTimeMillis` or `Clock.system*` |
| ARCH-5 | `domain` does not depend on `org.springframework.web..`, `jakarta.persistence..` or `jakarta.servlet..` |
| ARCH-6 (AR-06) | no class uses `JdbcTemplate`/`EntityManager` native DDL; `spring.jpa.hibernate.ddl-auto` is `validate` (configuration test) |

## 3. Configuration (ES-01, AR-04)

Every setting is read from an environment variable of the same name, with the default from `environments.md` where one is allowed. Typed binding: `AppSettings` (`@ConfigurationProperties("app")`, validated at startup; the application fails to start on an invalid value). Business values are injected where used and never hard-coded in code or tests.

| Variable | Property | Default | Notes |
|---|---|---|---|
| `APP_CONFERENCE_TZ` | `app.conference-tz` | `Europe/Ljubljana` | `ZoneId` |
| `APP_EARLY_BIRD_DEADLINE` | `app.early-bird-deadline` | `2026-07-31` | `LocalDate`, inclusive, in the conference time zone |
| `APP_FEE_EARLY` | `app.fee-early` | `240.00` | `BigDecimal` |
| `APP_FEE_REGULAR` | `app.fee-regular` | `300.00` | `BigDecimal` |
| `APP_VAT_RATE` | `app.vat-rate` | `0.22` | `BigDecimal` |
| `APP_WORKSHOPS` | `app.workshops` | `W1=Requirements engineering for AI coding agents;W2=Data spaces in practice;W3=Secure software supply chains` | `id=title` pairs separated by `;`; ids unique |
| `APP_RATE_LIMIT_PER_HOUR` | `app.rate-limit-per-hour` | `100` | per client (D-11) |
| `APP_TEST_CLOCK` | `app.test-clock` | `disabled` | `enabled` only in test and local; see 8.5 |
| `APP_MAIL_FROM` | `app.mail-from` | `registration@localhost` | sender address (environments.md "Sender address") |
| `APP_MAIL_RETRY_INTERVAL` | `app.mail-retry-interval` | `PT60S` | D-12 |
| `APP_MAIL_MAX_ATTEMPTS` | `app.mail-max-attempts` | `10` | D-12 |
| `DATABASE_URL` | `spring.datasource.url` | none (compose and tests set it) | local and test only |
| `DATABASE_USER` | `spring.datasource.username` | none (compose: `confreg`) | |
| `POSTGRES_PASSWORD` | `spring.datasource.password` | none (secret) | `.env` |
| `SMTP_HOST`, `SMTP_PORT` | `spring.mail.host`, `.port` | none (compose: `mailpit`, `1025`) | |
| `SMTP_TLS` | `spring.mail.properties.mail.smtp.starttls.required` | `false` | must be `true` in production (8.6) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.username`, `.password` | none (secret) | production only |
| `SMTP_AUTH` | `spring.mail.properties.mail.smtp.auth` | `false` | `true` in production when the SMTP account needs login (added in phase 4) |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | `app.organizer.username`, `.password` | none (secret) | startup fails if empty |
| `SPRING_PROFILES_ACTIVE` | – | none | `prod` in production |

Frontend runtime configuration (D-14): the frontend reads `APP_WORKSHOPS` from `/config.js` (`window.APP_CONFIG = {workshops: [{id, title}]}`), generated from the environment variable when the frontend container starts (and by the Vite dev server in development). No new public backend endpoint is added. `docker-compose.yml` passes the same `APP_WORKSHOPS` to both containers; its compose-level default repeats the backend default, the only duplicated value.

## 4. Registration API behaviour

Contract: `registration-api.openapi.yaml`.

### 4.1 `POST /api/registrations` (AC-001-01 to 09, 13)

Processing order (each step stops the request on failure):

1. Rate limit (8.3) → 429. Body size > 16 KiB → 413 (SR-02). Content type not JSON → 415.
2. Parse the body as a JSON tree; not a JSON object → 400. Unknown properties are ignored.
3. Validate (4.2) → 422 with exactly one error per rejected field; nothing stored, no e-mail.
4. `now = ConferenceClock.now()` (honours the test clock, 8.5) is the submission time (OQ-01).
5. Fee (4.3), registration number (4.4).
6. In one transaction: insert the registration with `status = registered`, `confirmation_status = pending`.
7. After commit: the mail component sends the confirmation (section 6); a send failure does not change the response (D-12).
8. Respond 201 with the stored registration (as read back) and `Location: /api/registrations/{registrationNumber}`.

Nothing else is created: no invoice, no payment request, no payment provider call (AC-001-05, NG3). There is no duplicate detection or uniqueness on e-mail (AC-001-09, NG4).

### 4.2 Validation (AC-001-07, D-10)

Implemented once, in `domain.RegistrationValidator`, on the parsed JSON tree; returns the first error per field in field order `firstName, lastName, email, payerType, companyName, companyAddress, companyVatId, workshops`.

| Field | Rule (error message) |
|---|---|
| any text field | must be a JSON string when present; trimmed; no control characters (`\p{Cc}`) ("must not contain control characters"); max length per contract ("must be at most N characters") |
| `firstName`, `lastName` | required, not blank ("is required") |
| `email` | required; one address `local@domain.tld`: no whitespace, exactly one `@`, a dot in the domain, local part ≤ 64, total ≤ 254 ("must be a valid e-mail address") |
| `payerType` | required; exactly `private` or `company` ("must be private or company") |
| `companyName`, `companyAddress`, `companyVatId` | required and validated only when `payerType = company`; ignored and stored as null otherwise; no country-specific VAT ID format |
| `workshops` | absent, null or `[]` = no workshop; must be an array of strings; more than one entry → "at most one workshop may be selected"; id not in `APP_WORKSHOPS` → "unknown workshop" |

Stored text values are the trimmed values. The frontend repeats the required/length checks for usability only; the backend is authoritative (SB-01).

### 4.3 Fee (AC-001-01, 02, 03, 06)

`domain.FeeCalculator`, inputs from `AppSettings`:

- `submissionDate = now.atZone(APP_CONFERENCE_TZ).toLocalDate()`
- `netFee = submissionDate ≤ APP_EARLY_BIRD_DEADLINE ? APP_FEE_EARLY : APP_FEE_REGULAR`
- `vat = (netFee × APP_VAT_RATE).setScale(2, HALF_UP)`; `grossFee = netFee + vat`, all at scale 2.
- The same rate applies to every payer, including companies from other EU member states (AC-001-06); the payer has no influence on the fee.

The comparison is on the local date in the conference time zone, never on the UTC date (oracle note: 2026-07-31T22:00:00Z is regular).

### 4.4 Registration number

`CR-` followed by 10 characters from the Crockford base-32 alphabet, drawn from `SecureRandom` (not guessable, no sequence). Uniqueness is enforced by `uk_registration_number`; on a collision the number is regenerated (at most 3 tries, then 500).

### 4.5 `GET /api/registrations/{registrationNumber}` (AC-001-10, 11, 12)

Organizer only (8.1). 200 with the stored registration; 404 when the number is unknown (problem details, no data); 401 when not authenticated. Response fields exactly as the contract's `Registration` schema; amounts as JSON numbers with two decimals.

### 4.6 Errors (SB-07, ES-07)

All errors are RFC 9457 problem details (`application/problem+json`) with a generic `title`; no stack traces, exception messages, SQL or class names (`server.error.include-*` = `never`). Unexpected exceptions: 500 with "Internal error", logged with the exception class and the request path only.

## 5. Storage (AR-05, AR-06, ES-08, NFR-01)

Contract: `registration-storage.sql`, implemented as Flyway migration `V1__create_registration.sql` (same DDL). Hibernate runs with `ddl-auto: validate`; the schema changes only through new Flyway migrations. `submitted_at` and `confirmation_sent_at` are `timestamptz`, written as UTC (`hibernate.jdbc.time_zone = UTC`). Amounts are `numeric(10,2)` (`BigDecimal`). The database uses UTF8; the JDBC driver and JSON are UTF-8, so č, š, ž survive storage unchanged (NFR-01). All access goes through Spring Data JPA with bound parameters (SB-05).

## 6. E-mail (AC-001-04, AR-07, SR-05, D-12, NFR-01)

Contract: `confirmation-email.yaml`.

- `application.ConfirmationDispatcher` listens for the registration-stored event **after commit** and calls `mail.ConfirmationMailer.send(registration)`; on success it sets `confirmation_status = sent`, `confirmation_sent_at`.
- On failure the status stays `pending` and `confirmation_attempts` increases. A scheduled job (`APP_MAIL_RETRY_INTERVAL`) retries pending confirmations older than one interval; after `APP_MAIL_MAX_ATTEMPTS` the status becomes `failed` and a warning with the registration number is logged (the organizer follows up manually; listed in the release notes).
- Exactly one message per registration in the normal case; the status update after a successful send prevents a second send.
- Plain text, UTF-8, fixed subject with the registration number; user input appears only in the body and as the single recipient address, which validation (4.2) guarantees has no control characters (no header injection, SR-05). No HTML part (no markup injection).
- Logs: registration number, attempt count and exception class only (SR-01; `MailException` messages can contain addresses and are never logged).

## 7. Frontend (AC-001-14, AC-001-15, AR-01, AR-04)

Contract: `registration-form.yaml`.

- One page (`App`) with the form, a confirmation region and an alert region; components `RegistrationForm`, `Confirmation`; module `api.ts` (the only module that calls `fetch`, only `/api/registrations`); module `config.ts` (reads `window.APP_CONFIG`).
- Company fields are rendered and sent only for a company payer. Workshop radio group: one option per configured workshop plus "No workshop" (default); sends `[]` or `[id]`.
- 201 → confirmation with registration number and the three amounts formatted with two decimals and "EUR". 422 → per-field error text next to each field (`aria-invalid`, `aria-describedby`), values kept. 429 and other failures → generic alert, no internal details.
- Output only through React text rendering; no `dangerouslySetInnerHTML` (SB-05).
- nginx (frontend container): serves the static build and `/config.js`; proxies `/api/` to `backend:8080` with `X-Forwarded-For`/`X-Forwarded-Proto`; adds `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` (SB-10); `client_max_body_size 16k` (SR-02); listens on 8080 as the unprivileged `nginx` user (SB-11).
- `/config.js` is written at container start by a script in `/docker-entrypoint.d/` that JSON-encodes `APP_WORKSHOPS` (ids and titles escaped, so a value cannot break out of the script).

## 8. Security

ASVS 5.0 Level 1 for both components (`security-requirements.md`).

### 8.1 Authentication and authorization (SB-02, SB-03)

- Spring Security, stateless, no sessions, CSRF disabled (no cookies or browser sessions; the only authenticated client is the organizer with HTTP Basic).
- `POST /api/registrations`: public (REQ-REG-01 override, limited to this endpoint). Every other path, including `GET /api/registrations/**` and every unknown path: organizer authentication required (`security-requirements.md`).
- Organizer credentials: `ORGANIZER_USERNAME`/`ORGANIZER_PASSWORD` from the environment, no defaults; the password is BCrypt-hashed in memory at startup and the plain value is not kept (SB-03). Startup fails when either is empty.
- Actuator health: see 10.2 (D-15).

### 8.2 Credentials only over HTTPS or localhost (SR-03, SB-04)

The backend honours `X-Forwarded-For`/`X-Forwarded-Proto` only from internal proxies (`server.forward-headers-strategy: native`, Tomcat's default private-address list). A request carrying an `Authorization` header is rejected with 401 (no data) unless `request.isSecure()` or the requested host is `localhost`, `127.0.0.1` or `::1`. Production runs behind the external nginx with HTTPS (`X-Forwarded-Proto: https`). Locally all ports are bound to 127.0.0.1.

### 8.3 Rate limiting (SB-06, AC-001-13, D-11)

`security.RateLimitFilter`, before authentication: per client IP (after forwarded-header resolution), sliding one-hour window, timestamps from `ConferenceClock`, in memory (single instance).

- `POST /api/registrations`: every request counts; request number `APP_RATE_LIMIT_PER_HOUR + 1` and later within the hour → 429 with `Retry-After`, nothing stored, no e-mail.
- Organizer endpoints: failed authentications count against a separate per-IP budget of `APP_RATE_LIMIT_PER_HOUR`; when exhausted, every request from that IP to organizer endpoints gets 429 until the window frees.
- Memory bound: idle client entries are removed after one hour; at most 100 000 tracked clients (oldest evicted).

### 8.4 Input, output and headers (SB-01, SB-05, SB-10, SR-02)

- Validation on the server (4.2). Body limit 16 KiB on the API (`security.BodySizeFilter` checks `Content-Length` and the stream) → 413.
- JPA bound parameters only; React text rendering; plain-text e-mail.
- Backend response headers via Spring Security: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer`. CORS: none allowed.

### 8.5 Test clock (AR-05, SR-04)

- `config.ConferenceClock` is the only source of the current time. When `app.test-clock = enabled` and the request has a valid `X-Test-Now` (ISO-8601 instant), `TestClockFilter` binds that instant for the request; `now()` returns it; otherwise the system UTC clock. An invalid `X-Test-Now` with the test clock enabled → 400.
- When disabled, the header is ignored.
- `config.StartupGuard` refuses to start (application context fails) when the `prod` profile is active and the test clock is enabled (SR-04, `environments.md`).

### 8.6 Transport in production (SB-04)

With the `prod` profile, `StartupGuard` also refuses to start unless `SMTP_TLS=true`. TLS to the browser is terminated by the external nginx (manual test, release notes). The database connection in production is configured by the operator through `DATABASE_URL` (`sslmode=require` documented in the backend README).

### 8.7 Logs (SR-01, SB-07, ES-07)

No request or response bodies are logged; no names, e-mail addresses, postal addresses or VAT IDs appear in any log statement (registration number only). Hibernate SQL and parameter logging stay off; Spring MVC request logging off. Exceptions from validation or mail are logged by class name. Checked in phase 6 by grepping logs of an acceptance run for the fixture values.

### 8.8 Supply chain and scanning (SB-08, SB-09)

OWASP Dependency-Check (backend), `npm audit` (frontend), Semgrep and gitleaks (all): run in phase 0 and phase 6 (`general/skills/verify-release`).

### 8.9 Least privilege (SB-11)

Backend image runs as a dedicated non-root user; frontend nginx as `nginx` (uid 101) on port 8080; PostgreSQL and Mailpit as provided by their images; only the backend (8080), frontend (8000) and Mailpit web (8025) ports are published, on 127.0.0.1.

## 9. Personal data (SB-12, SB-13, SB-14)

| Item | Requirement needing it | Purpose and retention |
|---|---|---|
| first name, last name, e-mail | AC-001-07 (participant) | identify and contact (confirmation); retention set by the organizer (`security-requirements.md`) |
| company name, address, VAT ID | AC-001-05, AC-001-07 (company payer) | invoicing by the accounting system; retention set by the organizer |
| workshop | AC-001-08 | attendance planning (not personal by itself) |

No other personal data is collected (no IP addresses stored; the rate limiter keeps IPs in memory for at most one hour). No consent or privacy acknowledgment field (OQ-04; SB-14 applies only where consent is required).

## 10. Deployment and runtime (ES-05, ES-09, NFR-02)

### 10.1 Local stack

`02_output/docker-compose.yml`, started with `docker compose up` in `02_output/`:

| Service | Image / build | Ports (host) | Health check |
|---|---|---|---|
| `postgres` | `postgres:16.15-alpine`, DB `confreg`, user `confreg` | none | `pg_isready` |
| `mailpit` | `axllent/mailpit:v1.31.1` | `127.0.0.1:8025` (web and API) | `/mailpit readyz` |
| `backend` | `backend/Dockerfile` (JRE image + jar built by `./mvnw package`) | `127.0.0.1:8080` | readiness on the management port (10.2) |
| `frontend` | `frontend/Dockerfile` (node build stage + nginx) | `127.0.0.1:8000` | `wget` of `/` |

Secrets come from the repository-root `.env` through `env_file: ../.env` on the services that need them (`backend`, `postgres`); settings pass through from the environment. The backend image is built from the jar produced on the host by the approved JDK (D-01), because no JDK image is listed in `tech-stack.md`.

### 10.2 Health and readiness (ES-09, NFR-02, D-15)

Actuator `health` with liveness and readiness groups is exposed only on a separate management port 8081, bound to the container's loopback address and not published; it shows status only (no details). The backend container health check calls `http://127.0.0.1:8081/actuator/health/readiness`; compose starts the frontend after the backend is healthy and the backend after PostgreSQL is healthy.

### 10.3 Production

Backend and frontend containers behind the external nginx with HTTPS; `SPRING_PROFILES_ACTIVE=prod`; external SMTP with TLS. Manual tests: real mailbox delivery, TLS/reverse proxy (release notes).

## 11. Tests (overview; details in `docs/03_test-strategy.md`)

| Level | Component | Tool | Location |
|---|---|---|---|
| acceptance (frozen) | backend | JUnit 6, Spring Boot test on a random port, Testcontainers PostgreSQL and Mailpit, HTTP client against the public API, test clock | `backend/src/test/java/si/confreg/registration/acceptance/` |
| acceptance (frozen) | frontend | Vitest + Testing Library, `fetch` stubbed per contract | `frontend/tests/acceptance/` |
| end-to-end (frozen) | stack | Playwright against the compose stack | `frontend/tests/e2e/` |
| unit / integration / architecture | both | JUnit, ArchUnit, Vitest | elsewhere (phase 5) |

Business values in tests come from the same configuration (AR-04); time-dependent tests use `X-Test-Now` (REQ-REG-01 "Standard of verification").

## Traceability

| AC / SR / SB / AR / NFR | Section |
|---|---|
| AC-001-01 | 4.1, 4.3 |
| AC-001-02 | 4.1, 4.3 |
| AC-001-03 | 4.3, 8.5 |
| AC-001-04 | 4.1, 6 |
| AC-001-05 | 4.1, 4.2, 5 |
| AC-001-06 | 4.3 |
| AC-001-07 | 4.1, 4.2 |
| AC-001-08 | 4.2, 5 |
| AC-001-09 | 4.1, 5 |
| AC-001-10 | 4.5, 8.1 |
| AC-001-11 | 4.5, 8.1, 8.2 |
| AC-001-12 | 4.5 |
| AC-001-13 | 8.3 |
| AC-001-14 | 7 |
| AC-001-15 | 7 |
| AR-01 | 1, 7 |
| AR-02 | 2 |
| AR-03 | 2 (ARCH-2) |
| AR-04 | 3, 7, 11 |
| AR-05 | 4.3, 5, 8.5 (ARCH-4) |
| AR-06 | 5 (ARCH-6) |
| AR-07 | 6 (ARCH-3) |
| AR-08 | 4.1 (no invoicing) |
| SR-01 | 6, 8.7 |
| SR-02 | 4.1, 7, 8.4 |
| SR-03 | 8.2 |
| SR-04 | 8.5 |
| SR-05 | 4.2, 6 |
| SB-01 | 4.2, 8.4 |
| SB-02 | 8.1 |
| SB-03 | 8.1 |
| SB-04 | 8.2, 8.6 |
| SB-05 | 5, 7, 8.4 |
| SB-06 | 8.3 |
| SB-07 | 4.6, 8.7 |
| SB-08 | 8.8 |
| SB-09 | 8.8 |
| SB-10 | 7, 8.4 |
| SB-11 | 8.9 |
| SB-12 | 9 |
| SB-13 | 9 |
| SB-14 | 9 |
| NFR-01 | 5, 6 |
| NFR-02 | 10.1, 10.2 |
