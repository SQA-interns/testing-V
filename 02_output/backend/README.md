# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, `docs/02_specification.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API for conference registration (`si.confreg.registration`). Public: `POST /api/registrations`, `GET /api/workshops`. Organizer (HTTP Basic): `GET /api/registrations/{registrationNumber}`. Contract: [../docs/02_contracts/registration-api.openapi.yaml](../docs/02_contracts/registration-api.openapi.yaml).

## Prerequisites

- Eclipse Temurin JDK 21. Maven is not needed: `./mvnw` downloads Apache Maven 3.9.9.
- Docker, for the Testcontainers tests (PostgreSQL and Mailpit images) and for the container image.
- Exact versions: `01_input/01_project/00_setup/tech-stack.md`.

## Configuration

Every setting is an environment variable. Business values have their defaults in `src/main/resources/application.yml`, copied from `01_input/01_project/00_setup/environments.md`.

| Variable | Default | Purpose |
|---|---|---|
| `APP_CONFERENCE_TZ` | `Europe/Ljubljana` | time zone for the deadline |
| `APP_EARLY_BIRD_DEADLINE` | `2026-07-31` | last early-bird day, inclusive |
| `APP_FEE_EARLY` / `APP_FEE_REGULAR` | `240.00` / `300.00` | net fee in EUR |
| `APP_VAT_RATE` | `0.22` | VAT rate |
| `APP_WORKSHOPS` | `W1=…;W2=…;W3=…` | workshops as `id=title` separated by `;` |
| `APP_RATE_LIMIT_PER_HOUR` | `100` | requests per client and hour (registration, workshop list, failed logins) |
| `APP_TEST_CLOCK` | `disabled` | `enabled` honours the `X-Test-Now` header; allowed only with profile `local` or `test` |
| `APP_MAIL_FROM` | `registration@confreg.local` | sender address |
| `APP_MAIL_STARTTLS` | `false` | require STARTTLS for SMTP |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | none | PostgreSQL connection |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | none | SMTP server |
| `SPRING_PROFILES_ACTIVE` | none | `local` for the compose stack; production runs without `local`/`test` |

Secrets (listed in `01_input/01_project/00_setup/secrets.env.example`, values in the repository-root `.env`, never in code):

| Secret | Purpose |
|---|---|
| `POSTGRES_PASSWORD` | database password |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | organizer login; start-up fails if missing or if the password is shorter than 16 characters |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | production SMTP account (not needed locally) |
| `NVD_API_KEY` | only for the dependency scan |

## Build

```sh
./mvnw -B package -DskipTests
```

Produces `target/registration-backend-0.1.0.jar`, which `Dockerfile` copies into the runtime image.

## Run

With the whole local stack (recommended): see the [root README](../README.md), `docker compose --env-file ../.env up --build` in `02_output/`.

Standalone, against your own PostgreSQL and SMTP server:

```sh
export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/confreg SPRING_DATASOURCE_USERNAME=confreg
export SPRING_MAIL_HOST=127.0.0.1 SPRING_MAIL_PORT=1025 SPRING_PROFILES_ACTIVE=local
# POSTGRES_PASSWORD, ORGANIZER_USERNAME and ORGANIZER_PASSWORD must also be set in the environment
./mvnw spring-boot:run
```

API on port 8080; health on the management port 8081 (`/actuator/health/liveness`, `/actuator/health/readiness`).

## Test

```sh
./mvnw -B verify
```

Runs unit, architecture (ArchUnit), integration and acceptance tests (`src/test/java/.../acceptance/`, frozen) and writes the JaCoCo report to `target/site/jacoco/`. Needs Docker. The acceptance tests read the business settings from `01_input/01_project/00_setup/environments.md`.

Mutation testing (optional): `./mvnw -B test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage`.
Dependency scan (optional, needs `NVD_API_KEY` in the environment): `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check`.

## Check

```sh
./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check
```

Format with `./mvnw spotless:apply`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Could not find a valid Docker environment` in tests | Docker is not running; start it. |
| Start-up fails with `ORGANIZER_PASSWORD is missing or shorter than 16 characters` | Set the organizer secrets in the environment or `.env`. |
| Start-up fails with `APP_TEST_CLOCK=enabled is only allowed…` | The test clock needs profile `local` or `test` (SR-04). |
| Start-up fails creating `JavaMailSender` or `confirmationSender` | `SPRING_MAIL_HOST` is not set. |
| `403` when reading as organizer | Credentials over plain HTTP from a non-loopback client outside the `local`/`test` profiles; use HTTPS (SR-03). |
| `429` | The client's hourly limit (`APP_RATE_LIMIT_PER_HOUR`) is used up; `Retry-After` says when to retry. |
| `400` for a body with non-ASCII characters sent with Windows `curl.exe` | The shell re-encoded the argument; send the body from a UTF-8 file with `--data-binary @file.json`. |
