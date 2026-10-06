# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Location (relative to `02_output/`) | Interface | Environment | Command | Frozen |
|---|---|---|---|---|---|
| Acceptance (backend) | `backend/src/test/java/si/confreg/registration/acceptance/`, `backend/src/test/resources/acceptance/` | REST API over HTTP (random port), Mailpit HTTP API, storage contract (`SELECT count(*) FROM registration`) | `@SpringBootTest` with Testcontainers `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1`; test clock enabled (`X-Test-Now`) | `cd backend && ./mvnw -B test` (needs Docker) | yes |
| End-to-end (frontend) | `frontend/e2e/` | the registration form in Chromium; Mailpit HTTP API | frontend at `E2E_BASE_URL` (default: Vite dev server on 127.0.0.1:5173, started by Playwright, proxying `/api` to 127.0.0.1:8080); backend, PostgreSQL and Mailpit running, for example `docker compose up` in `02_output`; `MAILPIT_URL` default `http://127.0.0.1:8025` | `cd frontend && npm run test:e2e` | yes |
| Unit / integration | other test paths | – | – | – | phase 5 |

Harness files outside the frozen paths: `frontend/playwright.config.ts` (runner configuration only) and the `e2e` entry in `frontend/tsconfig.json`.

### Design rules applied

- Black box: the tests reference no production class. The Spring Boot application is found by package scanning; behaviour is observed only through HTTP, e-mail in Mailpit and the storage contract table.
- AR-04: the acceptance configuration (`acceptance/acceptance.properties`) deliberately uses non-default business values (deadline 2026-05-15, fees 180.00 and 260.00, VAT 0.095, workshops `A1` and `B2`). Tests read every expected value from the Spring `Environment`, so code that hard-codes the defaults from `environments.md` fails. The end-to-end tests compare values on screen with the e-mail and take the workshop id from the form's own options, never from literals.
- AR-05: deadline instants are derived from the configured date and `APP_CONFERENCE_TZ` (last millisecond of the deadline day, first instant of the next day, and an instant whose UTC date is still the deadline day).
- Rejections (AC-001-08 to -13, -16) check the 4xx status, an unchanged row count and no e-mail to the address (step 1 of the skill).
- Test names contain the AC id: Java methods are named `ac_001_NN_…` and shown as `AC-001-NN …` by `AcIdDisplayNames`; Playwright titles start with the AC id.
- Organizer credentials for the tests are random per run (no secret in the repository).

### AC coverage

| AC | Acceptance tests (backend) | End-to-end |
|---|---|---|
| AC-001-01 | `FeeAcceptanceTest` (3) | – |
| AC-001-02 | `FeeAcceptanceTest` (3) | – |
| AC-001-03 | `ConfirmationEmailAcceptanceTest` (2) | `registration.spec.ts` (1) |
| AC-001-04 | `InvoiceDataAcceptanceTest` (3) | – |
| AC-001-05 | `FeeAcceptanceTest` (2) | – |
| AC-001-06 | `RegistrationResponseAcceptanceTest` (3) | `registration.spec.ts` (1) |
| AC-001-07 | `RegistrationResponseAcceptanceTest` (1) | – |
| AC-001-08 | `ValidationAcceptanceTest` (8) | – |
| AC-001-09 | `ValidationAcceptanceTest` (7) | – |
| AC-001-10 | `ValidationAcceptanceTest` (3) | – |
| AC-001-11 | `ValidationAcceptanceTest` (6) | `registration.spec.ts` (1) |
| AC-001-12 | `ValidationAcceptanceTest` (4) | `registration.spec.ts` (1) |
| AC-001-13 | `ValidationAcceptanceTest` (11) | – |
| AC-001-14 | `WorkshopAcceptanceTest` (1) | `registration.spec.ts` (1) |
| AC-001-15 | `WorkshopAcceptanceTest` (2) | – |
| AC-001-16 | `WorkshopAcceptanceTest` (3) | – |
| AC-001-17 | `FeeAcceptanceTest` (1) | – |
| AC-001-18 | `MailFailureAcceptanceTest` (1) | – |

## Phase 3 run (tests before any production code)

Run on 2026-10-06 against the bootstrap skeleton; logs `out/logs/p3-backend-acceptance-run1.log`, `out/logs/p3-frontend-e2e-run1.log`.

| Suite | Tests | Passed | Failed | Reason (one line per group) |
|---|---|---|---|---|
| Backend acceptance | 64 | 0 | 26 | API answers 401 instead of 2xx: the skeleton has only Spring Security's default protection and no registration endpoint |
| | | | 38 | `relation "registration" does not exist`: the storage contract table is not yet created, because the Flyway migration is not built (the rejection tests read the row count first) |
| End-to-end | 4 | 0 | 4 | the page loads but has no registration form: `getByLabel('First name')` / `getByLabel('Private person')` not found |

No test fails because of a build, configuration or harness error: the application context starts, both containers start and are reachable, and the dev server serves the page. No test passes on bootstrap code.

## First complete run (before any fix)

## Final run (phase 6)
