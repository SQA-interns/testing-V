# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

A public web form where a participant registers for the conference (US-001). The backend checks the input, works out the fee from the submission time (early-bird or regular, Slovenian VAT), stores the registration in PostgreSQL and sends one confirmation e-mail. Organizers read a stored registration through the API with HTTP Basic. No invoices or payments are created here; invoicing belongs to the accounting system.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API under `/api`, PostgreSQL via Flyway and JPA, SMTP e-mail | [backend/README.md](backend/README.md) |
| frontend | React single-page registration form, served by nginx, which proxies `/api` to the backend | [frontend/README.md](frontend/README.md) |

The local stack (`docker-compose.yml`) runs both components with PostgreSQL and Mailpit (a local mail catcher), on 127.0.0.1 only.

## Quick start

Needs Docker Engine with Compose, and Java 21 to build the backend jar (versions: `01_input/01_project/00_setup/tech-stack.md`).

1. In the repository root, copy `01_input/01_project/00_setup/secrets.env.example` to `.env` and fill in `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` (at least 16 characters). `.env` is ignored by git.
2. Build the backend jar (the container image only copies it):
   ```sh
   cd 02_output/backend
   ./mvnw -B package -DskipTests
   ```
3. Start the stack:
   ```sh
   cd 02_output
   docker compose --env-file ../.env up --build
   ```
4. Open:
   - registration form: http://127.0.0.1:3000
   - backend API: http://127.0.0.1:8080/api/workshops
   - sent e-mails (Mailpit): http://127.0.0.1:8025
5. Read a registration as the organizer:
   ```sh
   curl -u "<ORGANIZER_USERNAME>:<ORGANIZER_PASSWORD>" http://127.0.0.1:8080/api/registrations/<registrationNumber>
   ```
6. Stop with `docker compose --env-file ../.env down` (add `-v` to delete the database volume).

## Documentation

| Document | Content |
|---|---|
| [docs/01_acceptance-criteria.md](docs/01_acceptance-criteria.md) | acceptance criteria AC-001-01 to AC-001-12 and field rules |
| [docs/02_specification.md](docs/02_specification.md) | architecture, configuration, rules, security, commands |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, SQL, e-mail and form contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, AC → test map, test runs |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, findings, coverage, runtime demonstration |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision and its status |
| [logs/](logs/) | raw output of builds, tests, scans and the runtime demonstration |
