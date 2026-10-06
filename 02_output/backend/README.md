# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API of the conference registration (`docs/02_specification.md`, contract `docs/02_contracts/registration-api.openapi.json`). Root package `si.confreg.registration`, layers `api`, `application`, `domain`, `infrastructure`, `config`.

## Prerequisites

- JDK 21 (this run: Oracle JDK 21.0.11, approved in D-01; pinned in `tech-stack.md`: Temurin 21.0.10+7). Maven is downloaded by the wrapper (`./mvnw`, Maven 3.9.9).
- Docker, for the tests (Testcontainers starts PostgreSQL and Mailpit) and for the container image.

## Configuration

Every setting is an environment variable (or a property of the same name); defaults are in `src/main/resources/application.yml`.

| Setting | Default | Source |
|---|---|---|
| `APP_CONFERENCE_TZ` | `Europe/Ljubljana` | environments.md |
| `APP_EARLY_BIRD_DEADLINE` | `2026-07-31` (inclusive) | environments.md |
| `APP_FEE_EARLY` / `APP_FEE_REGULAR` | `240.00` / `300.00` (net, EUR) | environments.md |
| `APP_VAT_RATE` | `0.22` | environments.md |
| `APP_WORKSHOPS` | `W1=…;W2=…;W3=…` (`id=title` separated by `;`) | environments.md, D-15 |
| `APP_RATE_LIMIT_PER_HOUR` | `100` | environments.md |
| `APP_TEST_CLOCK` | `disabled` (`enabled` honours the `X-Test-Now` header; refused in profile `prod`) | environments.md |
| `APP_MAIL_FROM` | `registration@conference.example` | environments.md |
| `APP_DB_URL`, `APP_DB_USER` | required (compose sets them) | environments.md |
| `APP_SMTP_HOST`, `APP_SMTP_PORT`, `APP_SMTP_TLS` | required host/port, TLS `false` (must be `true` in `prod`) | environments.md |
| `SPRING_PROFILES_ACTIVE` | none; `prod` in production (trusts forwarded headers, enforces SR-04 and SMTP TLS) | specification §3 |

Secrets (from the repository-root `.env`, never committed): `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters), optional `SMTP_USERNAME`/`SMTP_PASSWORD` (production), `NVD_API_KEY` (dependency scan only).

## Build

```sh
./mvnw -B package -DskipTests        # target/registration-backend-0.1.0-SNAPSHOT.jar
docker build -t confreg-backend .     # non-root image (uid 10001)
```

## Run

```sh
docker compose --env-file ../.env up -d --build   # from 02_output/, whole stack
```

Without Docker for the backend itself: export the required settings above (PostgreSQL and an SMTP server must be reachable), then `./mvnw spring-boot:run`. API on port 8080, health on `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

## Test

```sh
./mvnw -B verify      # unit, integration and acceptance tests (needs Docker), JaCoCo report in target/site/jacoco
./mvnw -B org.pitest:pitest-maven:mutationCoverage -DtargetTests='si.confreg.registration.application.*Test,si.confreg.registration.api.*Test,si.confreg.registration.infrastructure.*Test,si.confreg.registration.config.*Test'
```

Acceptance tests (frozen, `docs/03_acceptance-manifest.sha256`) are in `src/test/java/si/confreg/registration/acceptance/`.

## Check

```sh
./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check   # format, lint, duplication, static analysis
./mvnw -B spotless:apply                                                  # reformat
NVD_API_KEY=… ./mvnw -B dependency-check:check                           # dependency scan (fails at CVSS >= 7)
```

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Could not resolve placeholder 'APP_DB_URL'` (or `APP_SMTP_HOST`) at startup | a required setting is missing; export it or use compose |
| `ORGANIZER_PASSWORD must have at least 16 characters` | set a longer password in `.env` |
| `APP_TEST_CLOCK must not be enabled in production` | remove `APP_TEST_CLOCK=enabled` when `SPRING_PROFILES_ACTIVE=prod` |
| Tests fail with `Could not find a valid Docker environment` | start Docker; Testcontainers needs it |
| `403 HTTPS is required for organizer access` | organizer credentials were sent over plain HTTP from a non-local address; use HTTPS (SR-03) |
| `429 Too many requests` | `APP_RATE_LIMIT_PER_HOUR` reached for this client; wait `Retry-After` seconds |
| Confirmation e-mail not received | the registration is kept and the mail retried every 30 s; check SMTP settings and the log line `Confirmation for CR-… not sent yet` |
| Dependency-Check reports `.NET Assembly Analyzer could not be initialized` | harmless: no .NET assemblies in this project (F-05) |
