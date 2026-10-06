# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Versions are in `project/00_setup/tech-stack.md` (as amended by D-05); this document names components and tools by id only. Contracts are in `docs/02_contracts/`.

## 1. System overview

| Component | Folder | Role | Runs as |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot REST API under `/api`; stores registrations in PostgreSQL; sends confirmation e-mail over SMTP | container from `eclipse-temurin` JRE image, non-root user |
| frontend | `02_output/frontend` | React single registration form page (AR-01); built by Vite | container from `nginx` image serving static files, non-root; proxies `/api` to the backend |
| database | – | PostgreSQL 16 (`postgres` image) | local and test: container; production: operated outside this repository |
| mail | – | SMTP; local and test: Mailpit (`axllent/mailpit`) | |

Local stack: `02_output/docker-compose.yml` starts backend, frontend, PostgreSQL and Mailpit, all published on 127.0.0.1 only: backend 8080, frontend 3000, Mailpit web/API 8025 (`environments.md`).

Request flow: browser → frontend nginx (`/` static, `/api/*` proxied) → backend → PostgreSQL / SMTP. In production an external nginx terminates TLS and routes the same paths (SB-04).

## 2. Backend architecture (AR-02 declaration)

Root package `si.confreg.registration`. Layered, with the application layer owning the ports it needs:

| Package | Contains | May depend on (inside the root package) |
|---|---|---|
| `domain` | `Registration` (immutable record), `PayerType`, `Payer`, `Fee` (net, VAT, gross), `FeePolicy` (fee by submission time), `Workshop`, `RegistrationNumber` | nothing |
| `application` | `RegisterParticipant` use case, `RegistrationQuery`, `RegistrationRequest` (input), `ValidationResult`/`FieldError`, `RegistrationValidator`; ports `RegistrationStore`, `ConfirmationSender`, `TimeSource`; `RateLimiter` | `domain` |
| `web` | REST controllers, request/response DTOs, `ApiExceptionHandler`, request filters (body size, rate limit), `RequestTimeSource` (test clock) | `application`, `domain` |
| `persistence` | JPA entity `RegistrationEntity`, Spring Data repository, `JpaRegistrationStore` (implements `RegistrationStore`) | `application`, `domain` |
| `mail` | `SmtpConfirmationSender` (implements `ConfirmationSender`), `ConfirmationMessage` (text renderer) | `application`, `domain` |
| `config` | `AppProperties` (all `APP_*`), `SecurityConfig`, clock and bean wiring, startup guards | all of the above |

Justification: the fee and validation rules (the parts the acceptance criteria constrain most) stay free of Spring, JPA and HTTP so they can be unit- and mutation-tested in isolation; adapters (`web`, `persistence`, `mail`) are replaceable and never call each other.

ArchUnit rules (DoD-04), in the backend test tree, check exactly this declaration:

- A1 `domain` depends on no other package of the root package, and not on `org.springframework..`, `jakarta.persistence..`, `jakarta.servlet..`.
- A2 `application` depends only on `domain` inside the root package, and not on `jakarta.persistence..`, `jakarta.servlet..`, `org.springframework.web..`.
- A3 `web`, `persistence`, `mail` do not depend on each other.
- A4 no package other than `config` depends on `config`, except for reading `AppProperties`.
- A5 no cycles between the slices `si.confreg.registration.(*)..` (AR-03).
- A6 only `persistence` uses `jakarta.persistence` and Spring Data; only `mail` uses `org.springframework.mail` / `jakarta.mail` (AR-07).
- A7 no class outside `config` and `web.RequestTimeSource` calls `Instant.now()`, `LocalDate.now()`, `ZonedDateTime.now()`, `LocalDateTime.now()`, `System.currentTimeMillis()` or `Clock.systemUTC()`/`systemDefaultZone()` (AR-05).

## 3. Configuration (AR-04, ES-01)

Variable names not given in `environments.md` are chosen in D-14. All business values are read from `AppProperties`, bound from environment variables, at the point of use. Defaults live only in `application.yml`, copied from `environments.md`; no business value appears as a literal in Java, TypeScript or tests.

| Variable | Property | Default | Notes |
|---|---|---|---|
| `APP_CONFERENCE_TZ` | `app.conference-tz` | `Europe/Ljubljana` | `ZoneId` |
| `APP_EARLY_BIRD_DEADLINE` | `app.early-bird-deadline` | `2026-07-31` | `LocalDate`, inclusive |
| `APP_FEE_EARLY` | `app.fee-early` | `240.00` | `BigDecimal`, EUR |
| `APP_FEE_REGULAR` | `app.fee-regular` | `300.00` | `BigDecimal`, EUR |
| `APP_VAT_RATE` | `app.vat-rate` | `0.22` | `BigDecimal` |
| `APP_WORKSHOPS` | `app.workshops` | `W1=Requirements engineering for AI coding agents;W2=Data spaces in practice;W3=Secure software supply chains` | `id=title` pairs separated by `;`, order kept |
| `APP_RATE_LIMIT_PER_HOUR` | `app.rate-limit-per-hour` | `100` | per client, see §7.4 |
| `APP_TEST_CLOCK` | `app.test-clock` | `disabled` | `enabled` or `disabled`, see §6 |
| `APP_MAIL_FROM` | `app.mail-from` | `registration@confreg.local` | sender address (`environments.md` "Sender address") |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | `spring.datasource.*` | none | database URL and user, environment-specific |
| `POSTGRES_PASSWORD` | `spring.datasource.password` | none (secret) | |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `APP_MAIL_STARTTLS` | `spring.mail.*` | none / `false` | SMTP host, port, STARTTLS required when `true` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.username/password` | empty (production only) | secrets |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | `app.organizer.*` | none (secret) | startup fails if missing or password shorter than 16 characters |
| `SPRING_PROFILES_ACTIVE` | | none | `local` (compose), `test` (automated tests); production runs with no profile or `production` |

The frontend has no business configuration of its own; it reads the workshop list from the backend (§5.3, D-10).

## 4. Domain rules

### 4.1 Fee (AC-001-01, -02, -03, -06)

1. `submittedAt` = `TimeSource.now()` (an `Instant`), taken once when a request has passed validation (Q1).
2. `submissionDate` = `submittedAt` converted to `APP_CONFERENCE_TZ`, as a `LocalDate`.
3. `net` = `APP_FEE_EARLY` if `submissionDate` ≤ `APP_EARLY_BIRD_DEADLINE`, else `APP_FEE_REGULAR`.
4. `vat` = `net × APP_VAT_RATE`, `setScale(2, RoundingMode.HALF_UP)`; `gross` = `net + vat`. Net is also scaled to 2.
5. The same rule for every payer type and country (AC-001-06): no field influences the fee except the submission time.

Expected values with the 2026 defaults: early 240.00 / 52.80 / 292.80; regular 300.00 / 66.00 / 366.00; boundary 2026-07-31T21:59:59Z early, 2026-07-31T22:00:00Z regular.

### 4.2 Validation (AC-001-07, D-08, SB-01, SR-05)

Server-side only is authoritative. Field rules: `01_acceptance-criteria.md` "Field rules". Details:

- Text values are trimmed before checks and stored trimmed. "Control characters" = any char in Unicode category `Cc` (includes CR, LF, TAB, NUL).
- E-mail: pattern `^[^@\s]+@[^@\s]+\.[^@\s]+$` after trimming, ≤ 254 chars; stored as entered (trimmed), no case change.
- `payerType`: exactly `private` or `company` (case-sensitive).
- `companyName`/`companyAddress`/`companyVatId`: checked only for `company`; for `private` they are ignored and stored as null.
- `workshops`: absent, null or `[]` → no workshop. One element → must equal an id in `APP_WORKSHOPS`. More than one element (even the same id twice) → error on `workshops`.
- All field errors are collected; at most one error per field (the first rule that fails, in the order: required → type → length → characters → format/allowed value).
- A JSON value of the wrong type for a known field (for example a number for `workshops`, an object for `email`) is an error on that field. A body that is not a JSON object → 400. Unknown properties are ignored.

### 4.3 Registration number

`REG-` followed by 10 characters from the Crockford Base32 alphabet (`0-9A-HJKMNP-TV-Z`), generated with `SecureRandom`; unique constraint in the database; on the (unlikely) collision the store retries with a new number, at most 3 times. Not guessable, so the number reveals neither count nor order.

### 4.4 Repeated registrations (AC-001-09, NG4)

No uniqueness on e-mail; each valid request creates one new row; existing rows are never updated (the store has no update operation).

## 5. REST API (contract: `02_contracts/registration-api.openapi.yaml`)

### 5.1 `POST /api/registrations` (public, AC-001-01..09, -12)

- Request: `application/json`, fields from the fixed API. Max body 16 KiB (SR-02).
- Order of processing: body size limit → rate limit (§7.4) → parse → validate → compute fee → store → send confirmation → respond.
- 201 `Created`, `Location: /api/registrations/{registrationNumber}`, body = stored registration (§5.4).
- 422 `application/problem+json`: `{"type":"about:blank","title":"Validation failed","status":422,"errors":{"<field>":"<message>",...}}`, exactly one entry per invalid field. Nothing stored, no e-mail.
- 400 malformed JSON (problem+json, no field details); 413 body too large; 415 wrong content type; 429 rate limit exceeded (`Retry-After` header in seconds).
- No invoice, payment or payment-link data anywhere (AC-001-05, NG3, AR-08).

### 5.2 `GET /api/registrations/{registrationNumber}` (organizer, AC-001-10)

- HTTP Basic, user `ORGANIZER_USERNAME`, role `ORGANIZER`. 200 with stored registration; 401 (`WWW-Authenticate: Basic realm="confreg"`) without or with wrong credentials, body without registration data; 404 unknown number (only after successful authentication); 403 when credentials arrive over plain HTTP from a non-loopback client (§7.2).

### 5.3 `GET /api/workshops` (public, AC-001-11; approved in D-10)

- Returns `[{"id":"W1","title":"…"}, …]` from `APP_WORKSHOPS`, in configured order. No personal data; rate limited with the same per-client limiter as registrations but in a separate bucket.

### 5.4 Stored registration (JSON)

`registrationNumber`, `status` (`registered`), `firstName`, `lastName`, `email`, `payerType`, `companyName`, `companyAddress`, `companyVatId` (null for private), `workshop` (id or null), `netFee`, `vat`, `grossFee` (JSON numbers with exactly two decimals, for example `292.80`), `submittedAt` (ISO-8601 UTC). The fixed fields of `architecture.md` are all present; `status` and `submittedAt` are additional.

### 5.5 Other paths

Any other path under `/api` → 401 when unauthenticated (secure by default), 404 when authenticated. No CORS configuration: the browser reaches the API through the same origin; cross-origin requests get no CORS headers.

## 6. Time (AR-05, SR-04)

- One clock component: `TimeSource` port, implemented by `web.RequestTimeSource`. It returns `Clock.systemUTC().instant()` unless `APP_TEST_CLOCK=enabled` and the current HTTP request carries `X-Test-Now`; then it returns that instant. An `X-Test-Now` value that is not ISO-8601 → 400. Outside a request (startup) it returns system time.
- The rate limiter, fee policy and `submittedAt` all use `TimeSource`.
- Timestamps are stored as `timestamptz` and written in UTC (`hibernate.jdbc.time_zone=UTC`); JVM default zone is not used for business dates.
- SR-04: at startup, if `APP_TEST_CLOCK=enabled` and neither profile `local` nor `test` is active (production, or no profile), the application refuses to start with a clear message. The `test` profile used by automated tests enables the test clock.

## 7. Security

### 7.1 Authentication and authorization (SB-02, SB-03, `security-requirements.md`)

- Spring Security, stateless (no session, no cookies), HTTP Basic only.
- Public: `POST /api/registrations` (REQ-REG-01 override), `GET /api/workshops` (D-10). Everything else under `/api` requires role `ORGANIZER`.
- One in-memory organizer from `ORGANIZER_USERNAME`/`ORGANIZER_PASSWORD`; the password is BCrypt-hashed at startup and the plain value is not kept in any bean field (SB-03).
- CSRF protection disabled: no cookies or sessions are used, so no ambient credentials exist except Basic, which is only used on a read-only GET.

### 7.2 Credentials over plain HTTP (SR-03, SB-04)

- (D-13) A request carrying an `Authorization` header is rejected with 403 before authentication when `request.isSecure()` is false and the client address is not loopback (`127.0.0.0/8`, `::1`) — unless profile `local` or `test` is active (the local environment is bound to 127.0.0.1 only; Docker's port forwarding makes the client appear as the bridge gateway).
- Behind the production reverse proxy `server.forward-headers-strategy=native` lets `X-Forwarded-Proto: https` mark the request secure and `X-Forwarded-For` give the client address (trusted proxies: Tomcat's default internal ranges).

### 7.3 Request size (SR-02)

A servlet filter rejects requests to `/api/**` with `Content-Length` > 16 KiB with 413, and wraps the input stream so a chunked body stops at 16 KiB + 1 byte with 413.

### 7.4 Rate limiting (SB-06, AC-001-12)

- Key: client address (`request.getRemoteAddr()` after forwarded-header processing) plus bucket (`registration`, `workshops`, `auth-failure`).
- Sliding window of one hour, using `TimeSource`; limit `APP_RATE_LIMIT_PER_HOUR` per key. A request that would exceed it gets 429 with `Retry-After` and is not counted; nothing is stored or sent.
- `auth-failure`: each failed Basic authentication counts; when the limit is reached, further requests with credentials from that client get 429 for the rest of the window (protects the organizer login).
- In-memory store (single backend instance); entries older than one hour are pruned on access.

### 7.5 Headers and output (SB-05, SB-07, SB-10, ES-07, SR-01)

- Backend responses: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`.
- Frontend nginx: `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`; `server_tokens off`.
- JSON is produced by Jackson (encoded); React escapes all rendered text; no `dangerouslySetInnerHTML`.
- SQL only through JPA with bound parameters (SB-05).
- Error responses are problem+json with fixed titles; no stack traces, exception messages or SQL (`server.error.include-*: never`).
- Logs (SR-01, ES-07): application log lines contain only registration numbers, field names (never values), status codes and error classes. Hibernate SQL parameter logging, request-body logging and mail debug are off. Jakarta validation messages never echo values.

### 7.6 Containers (SB-11)

Both images run as a non-root user with read-only application files; backend listens on 8080 (API) and 8081 (management, not published, D-11); frontend nginx listens on 8080 inside the container.

### 7.7 Personal data (SB-12..SB-14)

Stored items exactly as `security-requirements.md` "Personal data": names, e-mail; company name, address, VAT ID (company payers only). Purpose and retention as stated there (retention defined by the organizer outside this project). No consent field (Q4 assumption; SB-14 not triggered because no consent-based processing is introduced).

## 8. Persistence (AR-06, ES-08; contract: `02_contracts/registration-storage.sql`)

- Flyway migration `V1__create_registration.sql` = the contract DDL; `spring.jpa.hibernate.ddl-auto=validate`.
- Table `registration` with check constraints for payer type, company-field consistency, positive amounts and `gross_fee = net_fee + vat`.
- Charset UTF-8 end to end (database `UTF8`, JDBC, JSON, e-mail), NFR-01.

## 9. E-mail (AR-07, AC-001-04, SR-05, NFR-01; contract: `02_contracts/confirmation-email.json`)

- Sent only by `mail.SmtpConfirmationSender` through Spring's `JavaMailSender` to the configured SMTP server; one message per stored registration, after the database commit.
- `text/plain; charset=UTF-8` only, no HTML, so user input cannot inject markup. Subject contains only the fixed text and the registration number. Recipient is the validated `email`; names appear only in the body. Validation rejects control characters, so no CR/LF can reach headers; addresses are set through `InternetAddress`, never by string concatenation.
- On failure (D-09): log `confirmation e-mail failed for <registrationNumber>: <exception class>`; registration remains stored; response stays 201; no retry.

## 10. Frontend (AC-001-11, D-07; contract: `02_contracts/registration-form.json`)

- One page (`App`), no router. On load it requests `GET /api/workshops`; the workshop control offers "No workshop" plus each configured workshop (radio group, at most one).
- Fields: first name, last name, e-mail, payer type (radio: private / company); company fields appear only for company and are omitted from the request for private.
- Submit sends `POST /api/registrations` with `workshops: []` or `[id]`. While pending the submit button is disabled (prevents double submission).
- 201 → confirmation section with registration number, net fee, VAT, gross fee (EUR, two decimals) and the form hidden.
- 422 → each `errors[field]` shown next to its field (`aria-describedby`), values kept; no registration number shown.
- 429 / network / other → one general error message; values kept.
- Element ids and test ids are fixed in the contract so the end-to-end tests do not depend on layout.

## 11. Health and runtime (ES-09, NFR-02, DoD-06, DoD-P01)

- Actuator on management port 8081 (not published): `/actuator/health/liveness` and `/actuator/health/readiness` (readiness includes the database), no details shown; no other actuator endpoints exposed (D-11).
- Compose health checks: backend `wget -q -O- http://127.0.0.1:8081/actuator/health/readiness`; frontend `wget -q -O- http://127.0.0.1:8080/`; postgres `pg_isready`; frontend `depends_on` backend `service_healthy`, backend `depends_on` postgres `service_healthy` and mailpit `service_started`.

## 12. Commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend | `./mvnw -B package -DskipTests` | `./mvnw -B verify` (unit, integration, acceptance; needs Docker for Testcontainers) | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `docker compose up` in `02_output/` (or `./mvnw spring-boot:run` with the variables of §3) |
| frontend | `npm ci && npm run build` | `npm test` (unit/component); `npm run test:e2e` (end-to-end, needs the local stack) | `npm run check` | `docker compose up` in `02_output/` (or `npm run dev` with the backend on 8080) |

## 13. Test design hooks (for phase 3)

- Backend acceptance tests: `src/test/java/si/confreg/registration/acceptance/`, `@SpringBootTest(webEnvironment = RANDOM_PORT)` with profile `test`, PostgreSQL via Testcontainers, Mailpit via Testcontainers (`GenericContainer` from the pinned image); HTTP through `RestClient` or MockMvc against the public API only; time through `X-Test-Now`; client address isolation for rate-limit tests via `X-Forwarded-For` (trusted from loopback).
- Frontend end-to-end tests: `02_output/frontend/e2e/`, Playwright against the running local stack (`http://127.0.0.1:3000`), Mailpit API on 8025.
- Expected amounts are computed in tests from the configuration the backend is started with (read from `application.yml` / environment), never written as literals (REQ-REG-01 "Data and fixtures").

## 14. Out of scope

Student registration (NG1), cancellation/change (NG2), check-in, payment, invoicing (NG3, AR-08), duplicate detection (NG4), changing configured values (NG5). `poi-ooxml` is in `tech-stack.md` ("Excel export") but no criterion requires an export, so no export is built; the dependency stays declared as listed.

## Traceability

| AC / SR / SB / AR / NFR / ES | Section |
|---|---|
| AC-001-01 | 4.1, 5.1 |
| AC-001-02 | 4.1, 5.1 |
| AC-001-03 | 4.1, 6 |
| AC-001-04 | 5.1, 9 |
| AC-001-05 | 4.2, 5.1, 5.4, 8, 14 |
| AC-001-06 | 4.1 |
| AC-001-07 | 4.2, 5.1 |
| AC-001-08 | 4.2, 5.4 |
| AC-001-09 | 4.3, 4.4 |
| AC-001-10 | 5.2, 7.1 |
| AC-001-11 | 5.3, 10 |
| AC-001-12 | 7.4 |
| SR-01 | 7.5, 9 |
| SR-02 | 5.1, 7.3 |
| SR-03 | 7.2 |
| SR-04 | 6 |
| SR-05 | 4.2, 9 |
| SB-01 | 4.2 |
| SB-02 | 7.1 |
| SB-03 | 7.1, 3 |
| SB-04 | 1, 7.2 |
| SB-05 | 7.5 |
| SB-06 | 7.4 |
| SB-07 | 7.5 |
| SB-08 | 12 (`dependency-check`, `npm audit` in phase 6) |
| SB-09 | 12 (`spotbugs`, `pmd`, `semgrep`, `gitleaks` in phase 6) |
| SB-10 | 7.5 |
| SB-11 | 7.6 |
| SB-12 | 4.2, 7.7 |
| SB-13 | 7.7 |
| SB-14 | 7.7 |
| AR-01 | 1, 10 |
| AR-02 | 2 |
| AR-03 | 2 (A5) |
| AR-04 | 3 |
| AR-05 | 6, 2 (A7) |
| AR-06 | 8 |
| AR-07 | 9, 2 (A6) |
| AR-08 | 5.1, 14 |
| NFR-01 | 8, 9 |
| NFR-02 | 11 |
| ES-01, ES-02 | 3 |
| ES-05 | 12 |
| ES-07 | 7.5 |
| ES-08 | 8 |
| ES-09 | 11 |
