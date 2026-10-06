# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

A public single-page form where participants register for the conference (US-001). The backend prices each registration (early-bird or regular fee, VAT included), stores it in PostgreSQL, sends a confirmation e-mail with the registration number and the fee, and lets organizers read registrations for invoicing by the accounting system. Invoices themselves are produced outside this repository (AR-08).

## Components

| Component | Folder | What it is | README |
|---|---|---|---|
| backend | `backend/` | Spring Boot REST API (`/api/registrations`), PostgreSQL via Flyway, SMTP e-mail | [backend/README.md](backend/README.md) |
| frontend | `frontend/` | React registration form served by nginx; proxies `/api/` to the backend | [frontend/README.md](frontend/README.md) |
| contract check | `tools/contract-check/` | dev-only validator for `docs/02_contracts/` | [tools/contract-check/README.md](tools/contract-check/README.md) |

The local stack (`docker-compose.yml`) runs backend, frontend, PostgreSQL and Mailpit.

## Quick start

Prerequisites: Docker Engine and Docker Compose (versions in `01_input/01_project/00_setup/tech-stack.md`), and a filled `.env` in the repository root (copy `01_input/01_project/00_setup/secrets.env.example`; `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` are required, the password with at least 16 characters).

```sh
cd 02_output
docker compose --env-file ../.env up -d --build --wait
```

| Service | URL |
|---|---|
| Registration form | http://127.0.0.1:5173 |
| Backend API and health | http://127.0.0.1:8080/api/registrations, http://127.0.0.1:8080/actuator/health |
| Mailpit (caught e-mails) | http://127.0.0.1:8025 |

If a port is already taken on your machine, move the host side, for example `BACKEND_HOST_PORT=18080 FRONTEND_HOST_PORT=15173 MAILPIT_HOST_PORT=18025 docker compose --env-file ../.env up -d --build --wait`.

Read a registration as an organizer (export `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` from `.env` into your shell first):

```sh
curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" http://127.0.0.1:8080/api/registrations/REG-000001
```

Stop with `docker compose --env-file ../.env down` (add `-v` to delete the database volume).

## Documentation

| Document | Content |
|---|---|
| [docs/01_acceptance-criteria.md](docs/01_acceptance-criteria.md) | acceptance criteria AC-001-01 to AC-001-18 |
| [docs/02_specification.md](docs/02_specification.md) | behaviour, configuration, architecture, security design |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, storage, e-mail, form and frontend configuration contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, runs, coverage and mutation results |
| [docs/06_verification-report.md](docs/06_verification-report.md) | verification evidence and findings (self-check) |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests, pending decisions |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision record and its resolution |
| [docs/00_preflight-report.md](docs/00_preflight-report.md) | tool, version and dependency checks |
