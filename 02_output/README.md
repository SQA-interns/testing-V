# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

Participants register for the conference on a single web page (US-001). Each registration is stored, priced by the early-bird deadline (net fee, VAT, gross), and confirmed by e-mail. Organizers read registrations, including the payer data the accounting system needs for invoicing, through an authenticated API. Invoices themselves are produced by the accounting system, outside this repository (AR-08, D-20).

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API, PostgreSQL storage (Flyway), SMTP confirmation | [backend/README.md](backend/README.md) |
| frontend | React registration form, served by nginx | [frontend/README.md](frontend/README.md) |

The local stack (`docker-compose.yml`) runs both components with PostgreSQL and Mailpit (an SMTP catcher with a web inbox). It listens on 127.0.0.1 only.

## Quick start

From a clean checkout, with Docker, a JDK 21 and Node.js 24 installed (versions: `01_input/01_project/00_setup/tech-stack.md` and `docs/decisions-log.md`):

1. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the repository root. Fill in `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` (random, at least 16 characters). `.env` is ignored by git.
2. Build the backend jar:
   ```sh
   cd 02_output/backend && ./mvnw -B package -DskipTests && cd ..
   ```
3. Start the stack from `02_output/`:
   ```sh
   docker compose --env-file ../.env up --build --wait
   ```
4. Use it:
   - registration form: http://127.0.0.1:3000
   - confirmation e-mails (Mailpit): http://127.0.0.1:8025
   - organizer read (with `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` exported from `.env`): `curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" http://127.0.0.1:8080/api/registrations/REG-000001`
5. Stop it with `docker compose --env-file ../.env down`. Add `-v` to delete the stored registrations as well.

Optional settings (fees, deadline, VAT, workshops, rate limit, test clock) go into the same `.env` under their `APP_…` names (backend README). Anything not set uses the defaults from `environments.md`.

## Documentation

| Document | Content |
|---|---|
| [docs/00_preflight-report.md](docs/00_preflight-report.md) | Environment and dependency checks (phase 0) |
| [docs/01_acceptance-criteria.md](docs/01_acceptance-criteria.md) | Acceptance criteria AC-001-01 to AC-001-10 and validation rules |
| [docs/02_specification.md](docs/02_specification.md) | Architecture, configuration, behaviour, security, traceability |
| [docs/02_contracts/](docs/02_contracts/) | API (OpenAPI), storage (SQL), confirmation e-mail, registration form |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | Test levels, AC → tests, first and final runs, coverage and mutation |
| [docs/03_acceptance-manifest.sha256](docs/03_acceptance-manifest.sha256) | Hashes of the frozen acceptance and end-to-end tests |
| [docs/06_verification-report.md](docs/06_verification-report.md) | Definition of Done evidence, runtime demonstration, findings |
| [docs/decisions-log.md](docs/decisions-log.md) | All decisions D-01 to D-35 |
| [docs/release-notes.md](docs/release-notes.md) | Delivered scope, limitations, manual tests, pending decisions |
| [tools/validate_contracts.py](tools/validate_contracts.py) | Parses and validates the contracts |
| `logs/` | Raw outputs of every phase |
