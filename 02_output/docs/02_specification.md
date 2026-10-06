# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Versions are in `tech-stack.md` as amended by D-09, D-10, D-11; they are not repeated here. Contracts are in `docs/02_contracts/` and are validated by `out/tools/validate_contracts.py` (section 9).

## 1. System overview

| Component | Runs as | Talks to |
|---|---|---|
| frontend | static files (React single page) served by nginx in a container, non-root, port 8080 in the container | backend only through `/api` (AR-01); locally its nginx proxies `/api` to the backend |
| backend | Spring Boot service in an `eclipse-temurin` JRE container, non-root; API port 8080, management port 8081 (internal, D-26) | PostgreSQL (JPA, Flyway), SMTP (Mailpit locally) |
| PostgreSQL | `postgres` image | – |
| Mailpit | `axllent/mailpit` image (local and test only) | – |

Local stack: `02_output/docker-compose.yml` publishes only on 127.0.0.1: backend 8080, frontend 3000, Mailpit 8025 (web and API). PostgreSQL, SMTP port 1025 and the management port are not published. In production, the external nginx terminates TLS and routes `/api` to the backend and everything else to the frontend.

## 2. Backend architecture (AR-02, AR-03)

Ports and adapters around one use case. Root package `si.confreg.registration`.

| Package | Responsibility | May depend on |
|---|---|---|
| `domain` | `Registration` value and rules: `PayerType`, `Fees` (net, VAT, gross; half-up to 2 decimals), `FeeSchedule` (early or regular by deadline in the conference time zone), workshop catalogue, validation of field values | JDK only |
| `application` | `RegisterParticipant` use case and `FindRegistration` query; ports `RegistrationStore`, `ConfirmationSender`, `TimeSource`; `BusinessSettings` (fees, deadline, VAT, workshops, time zone) | `domain` |
| `api` | REST controllers, request/response DTOs, request validation mapping, error handler | `application`, `domain` |
| `persistence` | JPA entity, Spring Data repository, `RegistrationStore` adapter, registration-number sequence | `application`, `domain` |
| `mail` | `ConfirmationSender` adapter (Spring `JavaMailSender`, plain text) | `application`, `domain` |
| `clock` | `TimeSource` adapter honouring the test clock; test-clock filter | `application` |
| `security` | Spring Security configuration, organizer user, transport rule (SR-03), rate limiter, security headers | `application` (for settings only) |
| `config` | `@ConfigurationProperties` binding, startup checks (SR-04, organizer password length), wiring | all of the above |

Justification: the business rules (fee by deadline, VAT, validation) are pure and testable without Spring; the two side effects (storage, e-mail) sit behind ports so the use case can make them atomic (D-25); the clock is one component behind a port (AR-05).

ArchUnit rules (written in phase 3 as acceptance tests of AR-02, AR-03; phase 5 may add more):

1. `domain` depends on no other project package and on no `org.springframework..`, `jakarta.persistence..`, `jakarta.mail..` class.
2. `application` depends only on `domain` among project packages and on no `jakarta.persistence..` or `jakarta.mail..` class.
3. `api` does not depend on `persistence`, `mail` or `clock`.
4. `persistence`, `mail`, `clock` do not depend on `api` or on each other.
5. Only `persistence` uses `jakarta.persistence..` and Spring Data; only `mail` uses `org.springframework.mail..` and `jakarta.mail..` (AR-07).
6. Only `clock` calls `Instant.now()`, `LocalDate.now()`, `LocalDateTime.now()`, `ZonedDateTime.now()`, `OffsetDateTime.now()` or `Clock.system*` (AR-05).
7. No cycles between the slices `si.confreg.registration.(*)..` (AR-03).

## 3. Configuration (AR-04, ES-01)

Bound by `config.AppProperties` from `application.properties`, which reads environment variables of the same name. Defaults are in `application.properties` only (not in Java code); secrets have no default anywhere.

| Setting | Property | Default | Notes |
|---|---|---|---|
| `APP_CONFERENCE_TZ` | `app.conference-tz` | Europe/Ljubljana | |
| `APP_EARLY_BIRD_DEADLINE` | `app.early-bird-deadline` | 2026-07-31 | ISO date, inclusive, in the conference time zone |
| `APP_FEE_EARLY`, `APP_FEE_REGULAR` | `app.fee-early`, `app.fee-regular` | 240.00, 300.00 | net EUR (D-21) |
| `APP_VAT_RATE` | `app.vat-rate` | 0.22 | |
| `APP_WORKSHOPS` | `app.workshops` | `W1=Requirements engineering for AI coding agents;W2=Data spaces in practice;W3=Secure software supply chains` | `id=title` pairs separated by `;` |
| `APP_RATE_LIMIT_PER_HOUR` | `app.rate-limit-per-hour` | 100 | per client address and hour |
| `APP_TEST_CLOCK` | `app.test-clock` | disabled | `enabled` or `disabled` |
| `APP_MAIL_FROM` | `app.mail-from` | `registration@confreg.local` | sender address |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | Spring | none (compose sets them) | database URL and user |
| `POSTGRES_PASSWORD` | `spring.datasource.password` | none (secret) | |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `APP_MAIL_STARTTLS` | Spring / `spring.mail.properties.mail.smtp.starttls.enable` | none, none, false | |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.username`, `spring.mail.password` | empty (production only) | |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | `app.organizer.username`, `app.organizer.password` | none (secret) | startup fails if missing or the password is shorter than 16 characters |
| `SPRING_PROFILES_ACTIVE` | Spring | none | `prod` in production |

Startup checks (`config`): the `prod` profile with `app.test-clock=enabled` fails startup (SR-04); invalid time zone, date, amounts, VAT rate outside 0–1 or an empty workshop list fail startup.

## 4. Behaviour

### 4.1 Register (`POST /api/registrations`, public, D-29)

1. Rate limit (4.4), body size limit 16 KiB (SR-02, 413), content type `application/json` (415), well-formed JSON (400).
2. Validate (AC-001-08, D-23; rules in `docs/01_acceptance-criteria.md`). Unknown properties are rejected (Jackson `FAIL_ON_UNKNOWN_PROPERTIES`). Strings are trimmed; empty optional strings become null. Failure: 400 `{"error":"validation_failed","fields":[…]}`, nothing stored, no e-mail.
3. `now` = `TimeSource.now()` (4.3). Fee: `netFee` = `APP_FEE_EARLY` if the date of `now` in `APP_CONFERENCE_TZ` is on or before `APP_EARLY_BIRD_DEADLINE`, else `APP_FEE_REGULAR` (AC-001-01, AC-001-02). `vat` = `netFee × APP_VAT_RATE`, half-up to 2 decimals; `grossFee` = `netFee + vat` (AC-001-06, D-21).
4. In one transaction: take the next number from sequence `registration_number_seq` and format it as `REG-` plus six digits (`REG-000001`), insert the row with `created_at` = `now` (UTC, AR-05), send the confirmation e-mail (4.2), commit. If sending fails, roll back and answer 503 `{"error":"registration_unavailable"}` (AC-001-09, D-25).
5. Answer 201 with header `Location: /api/registrations/{registrationNumber}` and the stored registration (AC-001-05).
6. Invoice (AC-001-04, D-20): no invoice is produced here (AR-08). The stored registration contains payer type, company name, address, VAT ID and the three amounts, readable by organizers for the accounting system.

Duplicates are accepted as separate registrations (AC-001-10, D-24). Workshops: none or one configured id, stored as `workshop`; no fee effect (AC-001-07, D-22).

### 4.2 Confirmation e-mail (AC-001-03, SR-05, NFR-01, AR-07)

Contract: `docs/02_contracts/confirmation-email.yaml`. Sent by `mail.SmtpConfirmationSender` through `JavaMailSender` to the configured SMTP server only. Plain text UTF-8 (no HTML, so no markup injection); subject `Registration confirmation REG-000001` (constant text plus the generated number); `To` is the validated address (no CR/LF possible), `From` is `APP_MAIL_FROM`. User input appears only in the body. Headers are set through the Jakarta Mail API, never by string concatenation.

### 4.3 Clock and test clock (AR-05, SR-04)

`clock.ApplicationTimeSource` is the only source of the current time. With `APP_TEST_CLOCK=enabled`, `clock.TestClockFilter` reads `X-Test-Now` (ISO-8601 instant, e.g. `2026-07-31T21:59:59Z`) and the time source returns it for that request only; a malformed value gives 400 `{"error":"invalid_test_clock"}`; no header means system time. With the test clock disabled, the header is ignored. The `prod` profile refuses to start with it enabled (SR-04). Stored timestamps are `timestamptz` in UTC (`hibernate.jdbc.time_zone=UTC`).

### 4.4 Security (SB, SR)

- Authentication: HTTP Basic, one organizer user from `ORGANIZER_USERNAME`/`ORGANIZER_PASSWORD`, password BCrypt-hashed at startup and kept only as the hash (SB-03). Public: `POST /api/registrations` (D-29), `GET /api/workshops` (D-28). Everything else under `/api` requires the organizer (SB-02): 401 with `WWW-Authenticate: Basic` without or with wrong credentials. Non-`/api` paths answer 404. Stateless, no session, no CSRF token (no cookies or browser sessions are used).
- Transport rule (SR-03, D-27): a request carrying `Authorization` is rejected with 403 `{"error":"insecure_transport"}` unless it is secure (directly or `X-Forwarded-Proto: https` from a trusted proxy, `server.forward-headers-strategy=native`), or from a loopback address, or, outside the `prod` profile, from a private address.
- Rate limit (SB-06): per client address (after trusted-proxy resolution), fixed one-hour windows on the system clock (not the test clock), `APP_RATE_LIMIT_PER_HOUR` requests per window across all `/api` endpoints, including failed authentication. Excess: 429 with `Retry-After` in seconds and `{"error":"rate_limited"}`. In-memory, per instance.
- Headers (SB-10): backend responses carry `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx: `Content-Security-Policy: default-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'`, plus the same `nosniff`, `DENY`, `no-referrer`.
- CORS: none allowed (same origin through the proxy).
- Errors (SB-07, ES-07): every error body is one of the documented `{"error": …}` objects; no stack traces, exception messages or SQL (`server.error.include-*=never`, own `@RestControllerAdvice`); 500 is `{"error":"internal_error"}`.
- Logging (SR-01, ES-07): no request bodies, no names, e-mail or company data, no credentials. Logged: registration number, status, error type. Hibernate SQL and binding logging off; Mail debug off.
- SQL (SB-05): JPA with parameters only; no native string-built queries. Output: JSON by Jackson; React escapes text output; no `dangerouslySetInnerHTML`.
- Least privilege (SB-11): backend image runs as an unprivileged user; frontend nginx runs as user `nginx` on port 8080; the database user owns only the application schema.
- Dependencies and source (SB-08, SB-09): Dependency-Check and npm audit, semgrep and gitleaks in phase 6.
- TLS (SB-04): production traffic is HTTPS at the external nginx; SMTP uses STARTTLS in production (`APP_MAIL_STARTTLS=true`); local traffic stays on 127.0.0.1.
- Personal data (SB-12–SB-14): only the fields of the fixed API are collected; purposes and retention as in `security-requirements.md`; private payers cannot submit company data (D-23); no consent is needed (no optional processing) and none is preselected.

### 4.5 Read (`GET /api/registrations/{registrationNumber}`, organizer)

Returns 200 with the stored registration, 404 `{"error":"not_found"}` for an unknown number, 401 without valid credentials, 403 per the transport rule (4.4).

### 4.6 Workshops (`GET /api/workshops`, public, D-28)

Returns 200 `[{"id":"W1","title":"…"}, …]` from `APP_WORKSHOPS`, in configured order.

### 4.7 Health (NFR-02, ES-09, D-26)

Management port 8081: `/actuator/health/liveness` and `/actuator/health/readiness` (readiness includes the database), `show-details=never`; nothing else is exposed. Compose health checks call them with `wget` inside the container; the frontend health check fetches `/` from its nginx.

## 5. Persistence (AR-06, ES-08, NFR-01)

Contract: `docs/02_contracts/registration-storage.sql`, applied as Flyway migration `V1__create_registration.sql`. Hibernate `ddl-auto=validate`. Text columns are `varchar` in a UTF-8 database (`POSTGRES_INITDB_ARGS=--encoding=UTF8`), so č, š, ž are stored unchanged.

## 6. Frontend (AR-01)

Contract: `docs/02_contracts/registration-form.yaml`. One page: a form with first name, last name, e-mail, payer type (radio, private preselected), company fields shown and required only for company, an optional workshop select filled from `GET /api/workshops` (with "No workshop"), and a submit button. On 201 it shows the registration number and net, VAT and gross amounts; on 400 it marks the listed fields; on 429, 503 or other errors it shows a generic message. Client-side checks repeat the required-field rules for usability only; the backend is authoritative (SB-01). The local dev server proxies `/api` to `127.0.0.1:8080`.

## 7. Error and status summary

| Situation | Status | Body |
|---|---|---|
| registered | 201 | registration |
| invalid field values or unknown property | 400 | `validation_failed` + `fields` |
| malformed JSON | 400 | `malformed_request` |
| malformed `X-Test-Now` (test clock enabled) | 400 | `invalid_test_clock` |
| missing or wrong organizer credentials | 401 | `unauthorized` |
| credentials over insecure transport | 403 | `insecure_transport` |
| unknown registration or path | 404 | `not_found` |
| body over 16 KiB | 413 | `payload_too_large` |
| not JSON | 415 | `unsupported_media_type` |
| rate limit | 429 | `rate_limited` |
| unexpected failure | 500 | `internal_error` |
| e-mail not accepted by SMTP | 503 | `registration_unavailable` |

## 8. Not built

- Invoice generation (AR-08, D-20).
- Excel export: `poi-ooxml` is listed in `tech-stack.md` (and stays in the `pom.xml`, which must match it), but no requirement asks for an export, so no code uses it (ES-10).
- Participant accounts, payment, cancellation, editing (not in US-001).

## 9. Contract validation

`out/tools/validate_contracts.py` parses every contract: the OpenAPI document with PyYAML and validates it against the OpenAPI 3.1 JSON Schema (`out/tools/schemas/openapi-3.1.json`, from spec.openapis.org) with `jsonschema`, and checks every `$ref` resolves; the YAML contracts against their JSON Schemas in `out/tools/schemas/`. `registration-storage.sql` is parsed by executing it in `postgres` (`tech-stack.md` image) in a throw-away container. Output: `out/logs/02_design/contract-validation.log`.

## Traceability

| AC / SR / SB / NFR / AR | Section |
|---|---|
| AC-001-01, AC-001-02 | 4.1 step 3, 4.3 |
| AC-001-03 | 4.1 step 4, 4.2 |
| AC-001-04 | 4.1 step 6, 8 (D-20) |
| AC-001-05 | 4.1 step 5, 4.5, 5 |
| AC-001-06 | 4.1 step 3 |
| AC-001-07 | 4.1, 4.6 |
| AC-001-08 | 4.1 step 2, 7 |
| AC-001-09 | 4.1 step 4, 7 |
| AC-001-10 | 4.1 |
| SR-01 | 4.4 Logging |
| SR-02 | 4.1 step 1, 7 |
| SR-03 | 4.4 Transport rule (D-27) |
| SR-04 | 3 Startup checks, 4.3 |
| SR-05 | 4.2 |
| SB-01 | 4.1 step 2, 6 |
| SB-02 | 4.4 Authentication |
| SB-03 | 4.4 Authentication, 3 |
| SB-04 | 4.4 TLS |
| SB-05 | 4.4 SQL |
| SB-06 | 4.4 Rate limit |
| SB-07 | 4.4 Errors, 7 |
| SB-08, SB-09 | 4.4 Dependencies and source |
| SB-10 | 4.4 Headers |
| SB-11 | 4.4 Least privilege, 1 |
| SB-12, SB-13, SB-14 | 4.4 Personal data |
| NFR-01 | 4.2, 5 |
| NFR-02 | 4.7 |
| AR-01 | 1, 6 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 |
| AR-05 | 4.3, 2 rule 6 |
| AR-06 | 5 |
| AR-07 | 4.2, 2 rule 5 |
| AR-08 | 4.1 step 6, 8 |
