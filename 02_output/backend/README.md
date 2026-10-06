# Backend

> Written in: phase 0 (skeleton), completed in phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Conference registration API (Spring Boot), root package `si.confreg.registration`. It stores registrations in PostgreSQL, sends the confirmation e-mail over SMTP and serves the fixed registration API (`docs/02_contracts/registration-api.openapi.yaml`). The structure is described in `docs/02_specification.md`, section 2.

## Prerequisites

Exact versions are in `01_input/01_project/00_setup/tech-stack.md`, as amended in `docs/decisions-log.md`.

- JDK 21 (this run: Oracle JDK 21.0.11, approved in D-09; the pin is Eclipse Temurin 21.0.10+7). Maven itself is downloaded by the wrapper (`./mvnw`).
- Docker Engine and Docker Compose. The tests start PostgreSQL and Mailpit with Testcontainers; the local stack runs in Compose.
- Network access to Maven Central for the first build.

## Configuration

Every setting is an environment variable. Defaults live only in `src/main/resources/application.properties`. Secrets have no default, and the application does not start without them.

| Variable | Default | Source |
|---|---|---|
| `APP_CONFERENCE_TZ` | Europe/Ljubljana | `environments.md` |
| `APP_EARLY_BIRD_DEADLINE` | 2026-07-31 (inclusive, conference time zone) | `environments.md` |
| `APP_FEE_EARLY`, `APP_FEE_REGULAR` | 240.00, 300.00 (net EUR, D-21) | `environments.md` |
| `APP_VAT_RATE` | 0.22 | `environments.md` |
| `APP_WORKSHOPS` | `W1=…;W2=…;W3=…` (`id=title` pairs separated by `;`) | `environments.md` |
| `APP_RATE_LIMIT_PER_HOUR` | 100 per client address | `environments.md` |
| `APP_TEST_CLOCK` | disabled (`enabled` honours `X-Test-Now`; refused with profile `prod`) | `environments.md` |
| `APP_MAIL_FROM` | registration@confreg.local | `environments.md` |
| `APP_MAIL_STARTTLS` | false (set `true` in production) | `docs/02_specification.md` |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | none | `environments.md` (set by `docker-compose.yml` locally) |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | none | `environments.md` (Mailpit locally) |
| `SPRING_PROFILES_ACTIVE` | none (`prod` in production) | `docs/02_specification.md` |
| `POSTGRES_PASSWORD` | secret | `.env` (`secrets.env.example`) |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | secret, password at least 16 characters | `.env` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | empty (production only) | `.env` |
| `NVD_API_KEY` | secret, dependency scan only | `.env` |

Ports: 8080 for the API, 8081 for health and readiness (`/actuator/health/liveness`, `/actuator/health/readiness`). Port 8081 is internal and not published (D-26).

## Commands (ES-05)

Run from `02_output/backend/`.

| Purpose | Command |
|---|---|
| build | `./mvnw -B package -DskipTests` (creates `target/registration-backend-0.1.0.jar`) |
| test | `./mvnw -B verify` (all levels; Docker must be running; coverage in `target/site/jacoco/`) |
| check (format, lint, static analysis) | `./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check` |
| format | `./mvnw -B spotless:apply` |
| run | build first, then `docker compose --env-file ../.env up --build` in `02_output/` (see the root README) |
| mutation | `./mvnw -B test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage "-DexcludedTestClasses=si.confreg.registration.acceptance.*,si.confreg.registration.integration.*"` |
| dependency scan | `export NVD_API_KEY="$(sed -n 's/^NVD_API_KEY=//p' ../../.env \| tr -d '\r')"` then `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` |

Test levels: `src/test/java/.../acceptance/` holds the frozen acceptance tests (hashes in `docs/03_acceptance-manifest.sha256`; do not edit). The other packages hold unit and integration tests.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Startup fails with `Could not resolve placeholder 'ORGANIZER_USERNAME'` (or `POSTGRES_PASSWORD`) | A secret is missing. Add it to `.env` at the repository root and start through Compose with `--env-file ../.env`. |
| `ORGANIZER_PASSWORD must have at least 16 characters` | Use a longer random password in `.env`. |
| `The test clock must not be enabled in production (SR-04)` | Remove `APP_TEST_CLOCK=enabled` when running with profile `prod`. |
| Tests fail with `Could not find a valid Docker environment` | Start Docker; Testcontainers needs it for PostgreSQL and Mailpit. |
| 403 `insecure_transport` on an organizer request | Organizer credentials are refused over plain HTTP except from localhost or, outside `prod`, a private address (SR-03, D-27). Use HTTPS through the reverse proxy. |
| 429 `rate_limited` | More than `APP_RATE_LIMIT_PER_HOUR` requests from one address this hour. Wait for `Retry-After` seconds, or raise the limit for local testing. |
| 503 `registration_unavailable` | The SMTP server refused the confirmation. Nothing was stored (D-25). Check `SPRING_MAIL_HOST`/`SPRING_MAIL_PORT`, or that Mailpit is running. |
| `docker compose build` fails with `target/registration-backend-0.1.0.jar not found` | Run the build command first; the image copies the built jar. |
