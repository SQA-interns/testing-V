# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, `docs/02_specification.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API for conference registration: `POST /api/registrations` (public, rate limited) and `GET /api/registrations/{registrationNumber}` (organizer, HTTP Basic). Contract: `../docs/02_contracts/registration-api.openapi.yaml`.

## Prerequisites

- Eclipse Temurin JDK 21.0.10+7 (Maven 3.9.9 is downloaded by the wrapper `./mvnw`; on Windows `mvnw.cmd`).
- Docker Engine (tests start PostgreSQL and Mailpit with Testcontainers; the image build needs Docker).
- Network access to Maven Central on the first build.

## Configuration

Every setting is an environment variable (spec section 2). Defaults are in `src/main/resources/application.yml` and come from `01_input/01_project/00_setup/environments.md`.

| Variable | Default | Purpose |
|---|---|---|
| `APP_CONFERENCE_TZ` | `Europe/Ljubljana` | time zone of every business date |
| `APP_EARLY_BIRD_DEADLINE` | `2026-07-31` | last early-bird day (inclusive) |
| `APP_FEE_EARLY`, `APP_FEE_REGULAR` | `240.00`, `300.00` | gross fees in EUR, VAT included |
| `APP_VAT_RATE` | `0.22` | VAT rate used to split the fee |
| `APP_WORKSHOPS` | `W1=…;W2=…;W3=…` | workshops as `id=name` pairs separated by `;` |
| `APP_RATE_LIMIT_PER_HOUR` | `100` | requests per client and hour on `/api/**` |
| `APP_TEST_CLOCK` | `disabled` | `enabled` honours header `X-Test-Now` (test and local only) |
| `APP_MAIL_FROM` | `registration@confreg.local` | sender address |
| `APP_MAIL_TLS` | `false` | STARTTLS required; must be `true` in production |
| `APP_INSECURE_AUTH_ALLOWED` | `false` | accept organizer credentials over plain HTTP from non-loopback clients (local compose only) |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | none | PostgreSQL; production needs `sslmode=verify-full` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT` | none | SMTP server |
| `SPRING_PROFILES_ACTIVE` | none | `prod` in production |

Secrets, from the repository `.env` (names in `01_input/01_project/00_setup/secrets.env.example`; never commit values):

| Secret | Used for |
|---|---|
| `POSTGRES_PASSWORD` | database password (`spring.datasource.password`) |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | organizer login; password at least 16 characters, hashed at startup |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | production SMTP login (profile `prod` only) |

The `prod` profile refuses to start with the test clock enabled, `APP_MAIL_TLS=false` or `APP_INSECURE_AUTH_ALLOWED=true`.

## Build

```sh
./mvnw -B package -DskipTests        # jar in target/
docker build -t confreg-backend .    # container image (non-root)
```

## Run

The simplest way is the whole stack: see `../README.md`. To run the jar against your own PostgreSQL and SMTP server, export the variables above and the secrets, then:

```sh
./mvnw spring-boot:run
```

Health: `GET /actuator/health/liveness` and `/actuator/health/readiness`.

## Test

```sh
./mvnw -B test       # acceptance, unit, architecture and integration tests (Docker required)
./mvnw -B verify     # tests plus JaCoCo coverage report in target/site/jacoco/
```

| Level | Location |
|---|---|
| Acceptance (frozen, `../docs/03_acceptance-manifest.sha256`) | `src/test/java/si/confreg/registration/acceptance/` |
| Unit | `src/test/java/si/confreg/registration/{domain,config,mail,time,security,service,api}/` |
| Architecture (ArchUnit) | `src/test/java/si/confreg/registration/architecture/` |
| Integration | `src/test/java/si/confreg/registration/integration/` |

Mutation testing (PIT): `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` (scope and command used in phase 6: `../docs/06_verification-report.md`).

## Check

```sh
./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check
```

`./mvnw spotless:apply` fixes formatting. Dependency scan: export `NVD_API_KEY` from `.env`, then `./mvnw -B org.owasp:dependency-check-maven:check` (OSS Index is disabled in `pom.xml`, D-03; suppressions in `dependency-check-suppressions.xml`).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Tests fail with "Could not find a valid Docker environment" | start Docker; Testcontainers needs it |
| Startup fails with "ORGANIZER_PASSWORD must have at least 16 characters" or "must be set" | set both organizer secrets in `.env` |
| Startup fails with `app.…` must … | an `APP_*` value is malformed (date, amount, VAT rate, workshop list) |
| `POST` answers 503 "Please try again later" | the SMTP server did not accept the confirmation; nothing was stored (D-22) |
| Organizer `GET` answers 403 over HTTP | credentials over plain HTTP are refused unless from loopback or `APP_INSECURE_AUTH_ALLOWED=true` (SR-03) |
| Requests answer 429 | rate limit per client reached; wait for the next hour or raise `APP_RATE_LIMIT_PER_HOUR` |
| `mvnw` "Permission denied" on Linux or macOS | `chmod +x mvnw` |
