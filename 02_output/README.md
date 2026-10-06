# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

A public registration page for the conference. Participants register, choose at most one workshop and see their fee (early bird or regular; free for students). The backend stores the registration, sends a confirmation e-mail, and gives the organizer read access and an Excel export from which the accounting system issues the invoice.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API, PostgreSQL, SMTP (Java 21) | [backend/README.md](backend/README.md) |
| frontend | React single-page registration form, served by nginx (Node.js build) | [frontend/README.md](frontend/README.md) |

The local stack (both components, PostgreSQL 16, Mailpit) is defined in [docker-compose.yml](docker-compose.yml).

## Quick start

Prerequisites: Docker Engine with Docker Compose, and a `.env` file at the repository root (copy `01_input/01_project/00_setup/secrets.env.example` and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD`, the last one at least 16 characters).

```sh
cd 02_output
docker compose --env-file ../.env up -d --build
```

| URL | What |
|---|---|
| http://127.0.0.1:3000 | registration page |
| http://127.0.0.1:8080/actuator/health | backend health |
| http://127.0.0.1:8025 | Mailpit (received confirmation e-mails) |

Organizer access (HTTP Basic with the credentials from `.env`):

```sh
curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" http://127.0.0.1:8080/api/registrations/CR-000001
curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" -o registrations.xlsx http://127.0.0.1:8080/api/registrations/export
```

Stop with `docker compose --env-file ../.env down` (add `-v` to delete the database).

## Documentation

| Document | Content |
|---|---|
| [docs/01_acceptance-criteria.md](docs/01_acceptance-criteria.md) | acceptance criteria of US-001 |
| [docs/02_specification.md](docs/02_specification.md) | design: architecture, configuration, API, security, e-mail, storage |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, SQL, e-mail and form contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels and runs |
| [docs/06_verification-report.md](docs/06_verification-report.md) | verification, findings, traceability |
| [docs/decisions-log.md](docs/decisions-log.md) | decisions D-01 … D-18 |
| [docs/release-notes.md](docs/release-notes.md) | what was delivered, limitations, manual tests |
| [docs/00_preflight-report.md](docs/00_preflight-report.md) | environment check and commands |
