# Backend: registration API

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

Spring Boot REST API for conference registration (US-001): public `POST /api/registrations`, organizer-only `GET /api/registrations/{registrationNumber}`, PostgreSQL storage via Flyway, one confirmation e-mail per registration. Contract: [`../docs/02_contracts/registration-api.openapi.yaml`](../docs/02_contracts/registration-api.openapi.yaml); design: [`../docs/02_specification.md`](../docs/02_specification.md).

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 (run used Oracle JDK 21.0.11, D-01; listed: Temurin 21.0.10+7) | `JAVA_HOME` must point to it |
| Maven | none to install | `./mvnw` (wrapper 3.3.2) downloads Maven 3.9.9 |
| Docker | Engine 29.8.0, Compose 5.5.1 | tests start PostgreSQL and Mailpit containers (Testcontainers); local stack |

On Windows run the commands in Git Bash, or use `mvnw.cmd` instead of `./mvnw`.

## Configuration

Every setting is an environment variable (`environments.md`); defaults live in `src/main/resources/application.yml`.

| Variable | Default | Source |
|---|---|---|
| `APP_CONFERENCE_TZ` | `Europe/Ljubljana` | setting |
| `APP_EARLY_BIRD_DEADLINE` | `2026-07-31` (inclusive, conference time zone) | setting |
| `APP_FEE_EARLY` / `APP_FEE_REGULAR` | `240.00` / `300.00` | setting |
| `APP_VAT_RATE` | `0.22` | setting |
| `APP_WORKSHOPS` | `W1=…;W2=…;W3=…` (`id=title` separated by `;`) | setting |
| `APP_RATE_LIMIT_PER_HOUR` | `100` | setting |
| `APP_TEST_CLOCK` | `disabled` (`enabled` only in test and local; production refuses to start with it) | setting |
| `APP_MAIL_FROM` | `registration@localhost` | setting |
| `APP_MAIL_RETRY_INTERVAL` / `APP_MAIL_MAX_ATTEMPTS` | `PT60S` / `10` | setting |
| `DATABASE_URL`, `DATABASE_USER` | none (compose sets them; production: use `sslmode=require`) | setting |
| `SMTP_HOST`, `SMTP_PORT` | none (compose: `mailpit`, `1025`) | setting |
| `SMTP_TLS`, `SMTP_AUTH` | `false` (`SMTP_TLS=true` required with the `prod` profile) | setting |
| `SPRING_PROFILES_ACTIVE` | none; `prod` in production | setting |
| `POSTGRES_PASSWORD` | none | secret, `.env` |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none (startup fails if empty) | secret, `.env` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | none | secret, production only |

Secrets are listed in `01_input/01_project/00_setup/secrets.env.example`; copy it to `.env` at the repository root and fill the values. Never commit `.env`.

## Commands (ES-05)

Run in `02_output/backend/`.

| Purpose | Command |
|---|---|
| build | `./mvnw -DskipTests package` (jar in `target/registration-backend-0.1.0.jar`) |
| test | `./mvnw verify` (acceptance, integration, unit and architecture tests; JaCoCo report in `target/site/jacoco/`) |
| check | `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` |
| run | `docker compose up --build` in `02_output/` after the build (see the root README) |
| format | `./mvnw spotless:apply` |
| mutation | `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` |
| dependency scan | `NVD_API_KEY=… ./mvnw org.owasp:dependency-check-maven:check` (key from `.env`) |

Running outside Docker (`./mvnw spring-boot:run`) needs PostgreSQL and an SMTP server and every variable without a default set in the shell.

## Endpoints

| Endpoint | Access | Notes |
|---|---|---|
| `POST /api/registrations` | public, rate limited per client | 201 stored registration; 422 one error per field; 429 with `Retry-After`; body ≤ 16 KiB |
| `GET /api/registrations/{registrationNumber}` | organizer, HTTP Basic, HTTPS or localhost only | 200 / 401 / 404 |
| `GET /actuator/health/liveness`, `/readiness` | port 8081 on the container loopback only | used by the container health check |

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Startup fails with a binding error on `app.organizer` | `ORGANIZER_USERNAME` or `ORGANIZER_PASSWORD` empty: fill them in `.env` |
| `Could not resolve placeholder 'DATABASE_URL'` | running outside compose without the database variables |
| Startup fails: "test clock must not be enabled in production" or "SMTP_TLS must be true" | `prod` profile with `APP_TEST_CLOCK=enabled` or without `SMTP_TLS=true` |
| Tests fail at container start | Docker is not running |
| Organizer request answers 401 with correct credentials | the request is plain HTTP to a non-local host name; use HTTPS (or localhost locally) |
| Organizer requests answer 429 | too many failed logins from this client in the last hour; wait for `Retry-After` |
| A participant got no e-mail | the confirmation is retried every `APP_MAIL_RETRY_INTERVAL`; after `APP_MAIL_MAX_ATTEMPTS` the log says "manual follow-up needed" with the registration number |
