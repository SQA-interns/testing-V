# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Location (relative to `02_output/`) | Interface | Environment | Command | Frozen |
|---|---|---|---|---|---|
| Acceptance (backend) | `backend/src/test/java/si/confreg/registration/acceptance/`, `backend/src/test/resources/acceptance/` | REST API over HTTP (random port), Mailpit HTTP API, storage contract (`SELECT count(*) FROM registration`) | `@SpringBootTest` with Testcontainers `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1`; test clock enabled (`X-Test-Now`) | `cd backend && ./mvnw -B test` (needs Docker) | yes |
| End-to-end (frontend) | `frontend/e2e/` | the registration form in Chromium; Mailpit HTTP API | frontend at `E2E_BASE_URL` (default: Vite dev server on 127.0.0.1:5173, started by Playwright, proxying `/api` to 127.0.0.1:8080); backend, PostgreSQL and Mailpit running, for example `docker compose up` in `02_output`; `MAILPIT_URL` default `http://127.0.0.1:8025` | `cd frontend && npm run test:e2e` | yes |
| Unit (backend) | `backend/src/test/java/si/confreg/registration/{domain,config,mail,time,security,service,api}/` | classes directly (Mockito, Spring mock servlet objects) | JUnit 6, no Spring context | `cd backend && ./mvnw -B test` | no |
| Architecture (backend) | `backend/src/test/java/si/confreg/registration/architecture/` | compiled classes | ArchUnit rules ARCH-1 to ARCH-6 (spec section 5) | `cd backend && ./mvnw -B test` | no |
| Integration (backend) | `backend/src/test/java/si/confreg/registration/integration/`, `backend/src/test/resources/integration/` | REST API over HTTP, Mailpit, captured log output | `@SpringBootTest` with Testcontainers PostgreSQL and Mailpit; non-default business values | `cd backend && ./mvnw -B test` | no |
| Unit / component (frontend) | `frontend/src/*.test.ts(x)`, setup `frontend/tests/setup.ts` | modules and React components (Testing Library, jsdom) | Vitest | `cd frontend && npm test` | no |

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

Phase 5, 2026-10-06, all levels together: backend `./mvnw -B test` (acceptance, unit, architecture, integration), frontend `npm test`, end-to-end against the compose stack on ports 18080/15173/18025 (D-35). Logs: `out/logs/p5-first-run-backend.log`, `p5-first-run-frontend.log`, `p5-first-run-e2e.log`.

| Suite | Tests | Passed | Failed |
|---|---|---|---|
| Backend (64 acceptance, 106 unit, architecture and integration) | 170 | 169 | 1 |
| Frontend unit and component | 27 | 27 | 0 |
| End-to-end | 4 | 4 | 0 |
| **Total** | **201** | **200** | **1** |

| Failing test | Class | Action |
|---|---|---|
| `RegistrationValidatorTest.workshopRules` (input `"workshops": [null]`) | Implementation defect: `RegistrationValidator` called `contains(null)` on an immutable set, which throws `NullPointerException`; the API would have answered 500 instead of 400 | fixed in `RegistrationValidator` (null entry is "must be a configured workshop id"); no test changed |

Non-frozen test defects: none. Frozen tests that appear wrong: none in this run (D-28 and D-29 were resolved in phase 4).

### Phase 5 final run

Backend 170/170 (`out/logs/p5-final-run-backend.log`), frontend 27/27, end-to-end 4/4 (first run, unchanged code paths). Coverage and mutation scores are measured in phase 6.

## Final run (phase 6)

2026-10-06, after the phase 6 fixes (`docs/06_verification-report.md`).

| Suite | Tests | Passed | Failed | Log |
|---|---|---|---|---|
| Backend acceptance | 64 | 64 | 0 | `out/logs/p6-final-backend-verify.log` |
| Backend unit, architecture, integration | 108 | 108 | 0 | same |
| Frontend unit and component | 27 | 27 | 0 | `out/logs/p6-final-frontend-test.log` |
| End-to-end (stack on 18080/15173/18025, D-35) | 4 | 4 | 0 | `out/logs/p6-runtime-demo.log` |
| **Total** | **203** | **203** | **0** | |

Changed since phase 5: unit tests added for mutation-testing gaps (F-04). No frozen test changed in phase 6.

| Measure | Backend | Frontend |
|---|---|---|
| Line coverage | unit 81.3%, acceptance + integration 88.8%, all 97.2% | unit 93.7% |
| Branch coverage | unit 89.1%, acceptance + integration 66.3%, all 90.3% | unit 97.3% |
| Mutation score | 85% (PIT, unit tests; domain, security, config, service, mail, time) | 77.8% (Stryker command runner, D-36) |
