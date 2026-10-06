# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

Public registration for the conference (US-001): participants submit one form with their data, payer and optional workshop; the system stores the registration with the early-bird or regular fee (net, VAT, gross), answers with a registration number and sends one confirmation e-mail. The organizer reads registrations over an authenticated API. Invoicing is done later by the accounting system and is not part of this repository.

## Components

| Component | Folder | What it is |
|---|---|---|
| backend | [`backend/`](backend/README.md) | Spring Boot REST API, PostgreSQL via Flyway, confirmation e-mail |
| frontend | [`frontend/`](frontend/README.md) | React registration form served by nginx |

The local stack (`docker-compose.yml`) adds PostgreSQL 16 and Mailpit (local SMTP catcher).

## Quick start (local)

Prerequisites: Docker Engine 29.8.0 with Compose 5.5.1, JDK 21, Git Bash on Windows.

1. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` at the repository root and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (random, at least 16 characters).
2. Build the backend jar: `cd 02_output/backend && ./mvnw -DskipTests package`
3. Start the stack: `cd 02_output && docker compose up --build -d`
4. Wait until `docker compose ps` shows all four services healthy (about a minute), then open:
   - registration form: http://127.0.0.1:8000
   - Mailpit (received confirmations): http://127.0.0.1:8025
   - API: http://127.0.0.1:8080/api/registrations (organizer `GET` with HTTP Basic from `.env`)
5. Stop with `docker compose down` (add `-v` to delete the database).

Business values (fees, deadline, VAT rate, workshops, rate limit) can be overridden with the `APP_*` variables listed in the backend README.

## Documentation

| Document | Content |
|---|---|
| [`docs/01_acceptance-criteria.md`](docs/01_acceptance-criteria.md) | acceptance criteria AC-001-01..15 |
| [`docs/02_specification.md`](docs/02_specification.md) | architecture, behaviour, security, deployment |
| [`docs/02_contracts/`](docs/02_contracts/) | API (OpenAPI), storage (SQL), e-mail, form contracts |
| [`docs/03_test-strategy.md`](docs/03_test-strategy.md) | test levels, runs, coverage, mutation |
| [`docs/06_verification-report.md`](docs/06_verification-report.md) | verification self-check, findings |
| [`docs/release-notes.md`](docs/release-notes.md) | what is delivered, limitations, manual tests |
| [`docs/decisions-log.md`](docs/decisions-log.md) | decisions D-01..D-18 |
| [`docs/00_preflight-report.md`](docs/00_preflight-report.md), [`docs/00_progress.md`](docs/00_progress.md) | setup checks, run progress |
| `logs/` | raw tool output referenced by the documents |
