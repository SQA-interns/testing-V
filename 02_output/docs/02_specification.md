# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Conference registration: a public single-page form (frontend) posts a registration to a REST API (backend), which prices it, stores it in PostgreSQL, sends a confirmation e-mail and lets organizers read registrations. Decisions referenced here are in `docs/decisions-log.md`; contracts are in `docs/02_contracts/`.

## 1. Components and deployment

| Component | Folder | Runtime | Serves |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot 4.1.1 on Java 21, image `eclipse-temurin` JRE, user `app` (uid 10001) | `/api/**`, `/actuator/health/**` on port 8080 |
| frontend | `02_output/frontend` | React 19 built by Vite; static files in `nginx` image, unprivileged user, port 8080 in the container | `/` (form), `/config.json`; proxies `/api/` to the backend |
| database | – | `postgres` 16 | schema owned by Flyway migrations |
| mail catcher (local, test) | – | `axllent/mailpit` | SMTP 1025, web/API 8025 |

`02_output/docker-compose.yml` (local) starts all four, publishing only on 127.0.0.1: frontend 5173 → 8080, backend 8080, Mailpit 8025, PostgreSQL not published. Every container has a health check (NFR-02): backend `wget -qO- http://127.0.0.1:8080/actuator/health/readiness`, frontend `wget -qO- http://127.0.0.1:8080/healthz`, PostgreSQL `pg_isready`, Mailpit its built-in `readyz`. The backend starts after PostgreSQL and Mailpit are healthy; the frontend after the backend.

Production: both images behind an external nginx reverse proxy that terminates TLS (SB-04) and sets `X-Forwarded-For` and `X-Forwarded-Proto`; the backend runs with Spring profile `prod`.

## 2. Configuration

All settings come from environment variables (ES-01, AR-04). Settings from `environments.md`:

| Variable | Default | Used by | Notes |
|---|---|---|---|
| `APP_CONFERENCE_TZ` | `Europe/Ljubljana` | backend | zone for every business date (AR-05) |
| `APP_EARLY_BIRD_DEADLINE` | `2026-07-31` | backend | ISO date, inclusive (AC-001-01) |
| `APP_FEE_EARLY` | `240.00` | backend | gross EUR (D-19) |
| `APP_FEE_REGULAR` | `300.00` | backend | gross EUR (D-19) |
| `APP_VAT_RATE` | `0.22` | backend | decimal fraction |
| `APP_WORKSHOPS` | `W1=Requirements engineering for AI coding agents;W2=Data spaces in practice;W3=Secure software supply chains` | backend, frontend container | `id=name` pairs separated by `;` |
| `APP_RATE_LIMIT_PER_HOUR` | `100` | backend | per client, see 6.4 |
| `APP_TEST_CLOCK` | `disabled` | backend | `enabled` only in test and local; `prod` refuses to start (SR-04) |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | none (compose: `jdbc:postgresql://db:5432/confreg`, `confreg`) | backend | production URL must use `sslmode=verify-full` (SB-04) |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | none (compose: `mailpit`, `1025`) | backend | |
| `APP_MAIL_TLS` | `false` | backend | `true` sets STARTTLS required; `prod` refuses to start with `false` (SB-04) |
| `APP_MAIL_FROM` | `registration@confreg.local` | backend | sender address |
| `APP_INSECURE_AUTH_ALLOWED` | `false` | backend | added (D-26): local compose sets `true`; `prod` refuses to start with `true` (SR-03) |
| `SPRING_PROFILES_ACTIVE` | none | backend | `prod` in production |

Secrets (from `.env`, never defaulted, ES-01, ES-02): `POSTGRES_PASSWORD` (mapped to `spring.datasource.password` and the database container), `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `SMTP_USERNAME`, `SMTP_PASSWORD` (production only; mapped to `spring.mail.username`/`password`). The backend refuses to start when `ORGANIZER_USERNAME` or `ORGANIZER_PASSWORD` is empty, or when the password is shorter than 16 characters.

Startup validation: the backend fails fast on an unparseable deadline, fee, VAT rate, zone or workshop list, on fees ≤ 0, and on a VAT rate outside [0, 1).

## 3. Business behaviour

### 3.1 Registration (`POST /api/registrations`)

Order of processing, all within one database transaction:

1. Rate limit (6.4) and body size limit (6.5) are applied before parsing.
2. The JSON body is parsed; malformed JSON, a wrong JSON type or an unknown property → 400.
3. Normalisation: every string is trimmed; an empty string after trimming counts as absent.
4. Validation (SB-01, D-21) collects every violation and answers 400 with all of them (AC-001-08 to AC-001-13, AC-001-16):

| Field | Rule |
|---|---|
| `firstName`, `lastName` | required, 1-100 characters |
| `email` | required, ≤ 254 characters, syntactically valid address: one `@`, local part and a domain with a dot, no whitespace |
| `payerType` | required, `private` or `company` |
| `companyName` | `company`: required, ≤ 200; `private`: must be absent |
| `companyAddress` | `company`: required, ≤ 500; `private`: must be absent |
| `companyVatId` | `company`: required, ≤ 30; `private`: must be absent |
| `workshops` | optional; absent, null or `[]` = no workshop; otherwise exactly one element, a configured workshop id (D-20) |
| all strings | no control characters (U+0000-U+001F, U+007F); a line break is allowed only in `companyAddress` (SR-05) |

5. Pricing (3.2) with `now` from the clock component (6.6).
6. The registration number is assigned (3.3) and the row inserted with `created_at` = `now` in UTC (AR-05).
7. The confirmation e-mail (3.4) is sent synchronously through the mail component (AR-07).
8. Commit, then 201 Created with header `Location: /api/registrations/{registrationNumber}` and the stored registration (AC-001-06).

If step 7 fails, the transaction is rolled back and the API answers 503 with the detail "The registration could not be completed. Please try again later." (D-22, AC-001-18). Rejected requests store nothing and send no e-mail.

Duplicates are allowed (D-23); there is no capacity and no closing date.

### 3.2 Pricing (AC-001-01, -02, -05, -17; D-19)

- `deadlineEnd` = start of the day after `APP_EARLY_BIRD_DEADLINE` in `APP_CONFERENCE_TZ`, as an instant. Early if `now` < `deadlineEnd`, otherwise regular.
- `grossFee` = `APP_FEE_EARLY` or `APP_FEE_REGULAR`; `netFee` = `grossFee` / (1 + `APP_VAT_RATE`), scale 2, `HALF_UP`; `vat` = `grossFee` − `netFee`. All `BigDecimal`, scale 2, EUR.
- Payer type and workshop do not change the price.

### 3.3 Registration number

`REG-` followed by the value of PostgreSQL sequence `registration_number_seq`, zero-padded to 6 digits (`REG-000001`); unique by a database constraint (AC-001-07). A rolled-back attempt consumes a number; gaps are allowed.

### 3.4 Confirmation e-mail (AC-001-03, SR-05, NFR-01)

Defined in `02_contracts/confirmation-email.yaml`: one `text/plain; charset=UTF-8` message to the participant's `email`, from `APP_MAIL_FROM`; a fixed subject containing only the registration number; the body contains the participant's name, registration number, workshop, `grossFee`, `netFee`, `vat` and VAT rate, and the payer's invoice data. No HTML is produced. Header values are built with Jakarta Mail address and subject APIs only; user input never reaches a header except the validated recipient address. SMTP connect, read and write timeouts are 5 seconds.

### 3.5 Reading a registration (`GET /api/registrations/{registrationNumber}`)

Organizer only (HTTP Basic, 6.2). 200 with the stored registration; 404 if the number does not exist (also for a malformed number). This is the read interface from which the accounting system obtains invoice data (AC-001-04, D-18); this system produces no invoice (AR-08).

### 3.6 Frontend (AR-01, `02_contracts/registration-form.yaml`)

One page with one form: first name, last name, e-mail, payer type (radio: private, company; default none selected), company name, address and VAT ID (shown and required only for company), workshop (select: "No workshop" plus the configured workshops), submit button. The workshop list is read at start-up from `/config.json` (`02_contracts/frontend-config.schema.json`), which the frontend container writes from `APP_WORKSHOPS` on start (AR-04). The form talks to the backend only through `POST /api/registrations` (AR-01). On 201 it shows the registration number and the fee; on 400 it shows each field error next to its field; on 429 and 5xx it shows a general error and keeps the input. Client-side checks mirror 3.1 for convenience only; the backend is authoritative (SB-01). Output is rendered by React (escaped, SB-05). No cookies, no local storage of personal data.

## 4. Data (AR-06, ES-08, `02_contracts/registration-storage.sql`)

Flyway migration `V1__create_registration.sql` creates sequence `registration_number_seq` and table `registration`. Amounts are `numeric(10,2)`; `created_at` is `timestamptz` written in UTC; text columns are `varchar` in a UTF-8 database, so Slovenian characters are kept unchanged (NFR-01). Spring `ddl-auto` is `validate`. JPA access only through Spring Data repositories, so every query is parameterised (SB-05).

Personal data (SB-12, SB-13): exactly the fields of the fixed API; purpose and retention as in `security-requirements.md` (retention is set by the organizer outside this project). No other personal data is collected; no consent is needed because nothing beyond the registration itself is processed (SB-14).

## 5. Backend architecture (AR-02, AR-03)

Layered by responsibility, in root package `si.confreg.registration`. Reason: one use case with a few technical concerns. Separating the pure business rules (`domain`) from web, persistence, mail and clock keeps the rules testable without Spring and makes AR-04, AR-05 and AR-07 checkable as package rules.

| Package | Contains | May depend on |
|---|---|---|
| `api` | REST controller, request/response DTOs, problem-details exception handler | `service`, `domain` |
| `service` | registration use case (transaction, validation orchestration, number, mail call) | `domain`, `persistence`, `mail`, `time`, `config` |
| `domain` | `Registration` entity, `PayerType`, `Price`, `PricingPolicy`, `Workshop`, validation rules | nothing in the application (JDK, `jakarta.persistence` annotations only) |
| `persistence` | Spring Data repository, sequence access | `domain` |
| `mail` | confirmation e-mail composition and sending | `domain`, `config` |
| `time` | `AppClock` (the only source of "now"), test-clock request filter | `config` |
| `security` | security filter chain, rate limiter, body size filter, insecure-auth filter | `config` |
| `config` | typed `AppProperties` (all business settings), startup checks | nothing in the application |

ArchUnit rules (written in phase 3 or 5, run with the unit tests):

| Rule | Checks |
|---|---|
| ARCH-1 | layer access exactly as the table above |
| ARCH-2 | no cycles between packages (AR-03) |
| ARCH-3 | only `time` calls `Instant.now`, `LocalDate.now`, `LocalDateTime.now`, `ZonedDateTime.now`, `OffsetDateTime.now` or `Clock.system*` (AR-05) |
| ARCH-4 | only `mail` uses `JavaMailSender` or `jakarta.mail` (AR-07) |
| ARCH-5 | only `config` uses `@Value` or `@ConfigurationProperties` (AR-04) |
| ARCH-6 | only `persistence` declares Spring Data repositories |

## 6. Cross-cutting

### 6.1 Errors (SB-07, ES-07)

All errors use `application/problem+json` (RFC 9457) with `type`, `title`, `status` and `detail`; validation errors add `errors: [{field, message}]`. No stack traces, exception messages, SQL or class names are returned. Statuses: 400 validation or malformed body, 401 missing or wrong credentials (with `WWW-Authenticate: Basic`), 403 credentials over plain HTTP (6.3), 404 unknown registration or path, 405, 413 body too large, 415 not JSON, 429 rate limit (with `Retry-After` in seconds), 503 e-mail failure, 500 anything else.

### 6.2 Authentication and authorization (SB-02, SB-03)

| Endpoint | Access |
|---|---|
| `POST /api/registrations` | public: US-001 is performed by participants, who have no accounts (`security-requirements.md`) |
| `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | public, status only, no details (ES-09) |
| `GET /api/registrations/{registrationNumber}` | organizer (HTTP Basic) |
| everything else | denied (organizer authentication required; no other endpoint exists) |

The organizer password is BCrypt-hashed at startup into an in-memory user store; the plain value is not kept in a bean (SB-03). Stateless: no session, no cookies; CSRF protection is therefore not needed and disabled for `/api/**`. CORS: no cross-origin access is allowed (the frontend uses the same origin through nginx).

### 6.3 Credentials over plain HTTP (SR-03, D-26)

A request carrying an `Authorization` header is refused with 403 before authentication unless the request is secure (`https`, including `X-Forwarded-Proto: https` from the trusted proxy), its remote address is a loopback address, or `APP_INSECURE_AUTH_ALLOWED=true` (local compose only, where the backend port is bound to 127.0.0.1; `prod` refuses to start with it).

### 6.4 Rate limiting (SB-06)

Fixed one-hour window per client IP across all `/api/**` requests (registration and authentication), limit `APP_RATE_LIMIT_PER_HOUR`; the next request gets 429 with `Retry-After`. The client IP is the remote address; in `prod`, Tomcat's remote-IP handling takes it from `X-Forwarded-For` set by the trusted proxy. In memory, single instance.

### 6.5 Request size (SR-02)

`POST` bodies over 16 KiB are refused with 413, by `Content-Length` and while streaming. Tomcat `max-http-form-post-size` and header size keep their defaults.

### 6.6 Clock and test clock (AR-05, SR-04)

`AppClock` returns the instant for the current request. When `APP_TEST_CLOCK=enabled`, a request header `X-Test-Now` (ISO-8601 instant, UTC) sets it for that request only; an unparseable value → 400. When disabled the header is ignored. The `prod` profile fails startup when the test clock is enabled, and the setting defaults to disabled.

### 6.7 Security headers (SB-10)

Backend: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx: `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'`, the same `nosniff`, framing and referrer headers; `server_tokens off`.

### 6.8 Logging (SR-01, SB-07, ES-07)

Logs never contain request bodies, field values, e-mail addresses, names, addresses or VAT IDs. The application logs only event names, registration numbers, HTTP status and exception class names. Hibernate SQL and parameter logging and Spring web request logging stay off; the mail debug flag stays off. Validation failures are logged with field names only.

### 6.9 Health (ES-09, NFR-02)

Actuator exposes only `health`, with liveness and readiness probes enabled; readiness includes the database. `show-details: never`.

## 7. Test approach (summary; details in `docs/03_test-strategy.md`)

- Acceptance tests (backend, black-box over HTTP): the Spring Boot application on a random port with Testcontainers PostgreSQL and a Mailpit container, test clock enabled, e-mails read from the Mailpit API. Path `src/test/java/.../acceptance/`.
- End-to-end (frontend): Playwright with Chromium against the frontend and the backend (`/api` proxied), path `frontend/e2e/`.
- Unit and integration tests in phase 5; ArchUnit rules ARCH-1 to ARCH-6.

## 8. Contracts

| Interface | Contract | Validated by |
|---|---|---|
| Registration API (REST) | `02_contracts/registration-api.openapi.yaml` (OpenAPI 3.1) | `tools/contract-check`: `@apidevtools/swagger-parser` validate |
| Registration storage (SQL) | `02_contracts/registration-storage.sql` | applied to `postgres:16.15-alpine` with `psql -v ON_ERROR_STOP=1` |
| E-mail (SMTP) | `02_contracts/confirmation-email.yaml` | `tools/contract-check`: YAML parse plus required keys |
| Registration form (UI) | `02_contracts/registration-form.yaml` | `tools/contract-check`: YAML parse plus required keys |
| Frontend configuration | `02_contracts/frontend-config.schema.json` | `tools/contract-check`: Ajv compiles the schema (draft 2020-12) |

Command: `cd 02_output/tools/contract-check && npm ci && npm run validate`, and for SQL the `docker run` in `tools/contract-check/README.md`. Tools added under the `tech-stack.md` rule: D-25.

## Traceability

| AC / SR / SB / NFR / AR | Section |
|---|---|
| AC-001-01 | 3.2, 6.6 |
| AC-001-02 | 3.2, 6.6 |
| AC-001-03 | 3.1 step 7, 3.4 |
| AC-001-04 | 3.5, 4 |
| AC-001-05 | 3.2 |
| AC-001-06 | 3.1 step 8, 3.5 |
| AC-001-07 | 3.3, 3.1 (duplicates) |
| AC-001-08 | 3.1 step 4 |
| AC-001-09 | 3.1 step 4 |
| AC-001-10 | 3.1 step 4 |
| AC-001-11 | 3.1 step 4 |
| AC-001-12 | 3.1 step 4 |
| AC-001-13 | 3.1 step 4 |
| AC-001-14 | 3.1 step 4, 4 |
| AC-001-15 | 3.1 step 4 |
| AC-001-16 | 3.1 step 4 |
| AC-001-17 | 3.2 |
| AC-001-18 | 3.1 (step 7 failure) |
| SR-01 | 6.8 |
| SR-02 | 6.5 |
| SR-03 | 6.3 |
| SR-04 | 6.6, 2 |
| SR-05 | 3.1 step 4 (control characters), 3.4 |
| SB-01 | 3.1 step 4, 3.6 |
| SB-02 | 6.2 |
| SB-03 | 6.2, 2 (secrets) |
| SB-04 | 1 (production proxy), 2 (`sslmode`, `APP_MAIL_TLS`), 6.3 |
| SB-05 | 4 (parameterised queries), 3.6 (escaped output), 3.4 (plain-text e-mail) |
| SB-06 | 6.4 |
| SB-07 | 6.1, 6.8 |
| SB-08 | `docs/00_preflight-report.md`; re-scanned in phase 6 (Dependency-Check, npm audit) |
| SB-09 | phase 6: semgrep, gitleaks, SpotBugs, PMD |
| SB-10 | 6.7 |
| SB-11 | 1 (non-root containers) |
| SB-12 | 4 (personal data) |
| SB-13 | 4 (purpose and retention) |
| SB-14 | 4 (no consent needed) |
| NFR-01 | 3.4, 4 |
| NFR-02 | 1 (health checks), 6.9 |
| AR-01 | 3.6 |
| AR-02 | 5 |
| AR-03 | 5 (ARCH-2) |
| AR-04 | 2, 3.6, 5 (ARCH-5) |
| AR-05 | 3.2, 6.6, 5 (ARCH-3) |
| AR-06 | 4 |
| AR-07 | 3.4, 5 (ARCH-4) |
| AR-08 | 3.5 |
| DoD-P01 | 1 (local stack), 3.1 |
