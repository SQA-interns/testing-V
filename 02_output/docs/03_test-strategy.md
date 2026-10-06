# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location (relative to `02_output/`) | Tool | Interface used | Frozen |
|---|---|---|---|---|---|
| acceptance | backend | `backend/src/test/java/si/confreg/registration/acceptance/` (9 files, 38 tests) | JUnit 6, Spring Boot test on a random port, Testcontainers `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1`, `java.net.http` | registration API (contract `registration-api.openapi.yaml`), Mailpit API, registration storage contract (`registration-storage.sql`) for "nothing stored" and "no invoice/payment table" | yes |
| acceptance | frontend | `frontend/tests/acceptance/registrationForm.test.tsx` (6 tests) | Vitest 5.0.3, Testing Library, jsdom | rendered page by accessible role and name (contract `registration-form.yaml`); `fetch` stubbed with contract-shaped responses | yes |
| end-to-end | stack | `frontend/tests/e2e/registration.spec.ts` (2 tests) | Playwright 1.63.0, Chromium | browser against the running stack; Mailpit API | yes |
| unit | backend | `backend/src/test/java/si/confreg/registration/{domain,config,security,application,mail,persistence}/` (100 tests incl. architecture) | JUnit 6, Mockito, Spring mock servlet objects | classes directly | no |
| architecture | backend | `backend/src/test/java/si/confreg/registration/architecture/ArchitectureTest.java` (7 tests, ARCH-1..6) | ArchUnit 1.3.2 | compiled classes | no |
| integration | backend | `backend/src/test/java/si/confreg/registration/integration/` (11 tests) | as backend acceptance | running backend, storage, Mailpit, captured log output | no |
| unit | frontend | `frontend/src/*.test.ts(x)` (20 tests) | Vitest, Testing Library | modules and the form component | no |

How the frozen tests satisfy `general/quality/test-strategy.md` and REQ-REG-01 "Standard of verification":

- Every test name contains its AC id (`ac_001_nn_…` in Java, `AC-001-nn …` in TypeScript).
- AC-001-04 and AC-001-07 have API-level tests (`ConfirmationAcceptanceTest`, `ValidationAcceptanceTest`).
- Time-dependent tests send `X-Test-Now`; the backend runs with `app.test-clock=enabled`. No test depends on the system clock.
- Fees, VAT rate, deadline, time zone and workshops are read from the application's configuration through Spring's `Environment` (AR-04); expected amounts follow the oracle (`net × rate`, half-up to 0.01). Instants are derived from the configured deadline and time zone (2026-07-20, deadline day, 23:59:59 and 00:00:00 local, 2026-08-05 with the 2026 configuration).
- The frontend tests use synthetic workshops and amounts passed in as configuration or API responses, because the component must render whatever it is given; the e2e tests read workshops and amounts from the running page.
- Rejection tests also check that nothing changed: no row for the e-mail in storage, no mail in Mailpit within 3 s.
- Each backend test uses a unique e-mail (`ana.novak+<random>@example.org`) so tests are independent of order and of each other's mails.
- Rate limiting (AC-001-13) runs in its own application context with `app.rate-limit-per-hour=3`; all other backend acceptance tests run with a limit high enough not to interfere.
- External services: Mailpit and PostgreSQL containers only; real SMTP and TLS are manual tests (release notes).

## Traceability (AC → tests)

| AC | Tests |
|---|---|
| AC-001-01 | `FeeAcceptanceTest.ac_001_01_*` (2) |
| AC-001-02 | `FeeAcceptanceTest.ac_001_02_*` |
| AC-001-03 | `FeeAcceptanceTest.ac_001_03_*` (2) |
| AC-001-04 | `ConfirmationAcceptanceTest.ac_001_04_*` (3, API level); e2e `AC-001-14 AC-001-04 …` |
| AC-001-05 | `PayerDataAcceptanceTest.ac_001_05_*` (3) |
| AC-001-06 | `FeeAcceptanceTest.ac_001_06_*` (2) |
| AC-001-07 | `ValidationAcceptanceTest.ac_001_07_*` (13, API level) |
| AC-001-08 | `WorkshopAcceptanceTest.ac_001_08_*` (4) |
| AC-001-09 | `RepeatedRegistrationAcceptanceTest.ac_001_09_*` (2) |
| AC-001-10 | `OrganizerAccessAcceptanceTest.ac_001_10_*` |
| AC-001-11 | `OrganizerAccessAcceptanceTest.ac_001_11_*` (3) |
| AC-001-12 | `OrganizerAccessAcceptanceTest.ac_001_12_*` |
| AC-001-13 | `RateLimitAcceptanceTest.ac_001_13_*` |
| AC-001-14 | `registrationForm.test.tsx` "AC-001-14 …" (4); e2e "AC-001-14 AC-001-04 …" |
| AC-001-15 | `registrationForm.test.tsx` "AC-001-15 …" (2); e2e "AC-001-15 …" |

## Phase 3 run (bootstrap code only, before the freeze)

| Suite | Command | Passed | Failed | Reason | Log |
|---|---|---|---|---|---|
| backend acceptance | `./mvnw test` | 0 | 38 | assertion "configuration property app.early-bird-deadline / app.workshops provided by the application": the bootstrap application has no business configuration yet (AR-04, built in phase 4) | `out/logs/03_backend-acceptance-first.log` |
| backend acceptance, diagnostic | `./mvnw test` with the `environments.md` defaults as `-Dapp.*` system properties (tests unchanged) | 0 | 38 | first assertion on the HTTP status: expected 201 or 422, got 401, because the registration API does not exist and the bootstrap's default security rejects every request | `out/logs/03_backend-acceptance-diagnostic.log` |
| frontend acceptance | `npx vitest run tests/acceptance` | 0 | 6 | `TestingLibraryElementError`: no form fields, radio groups or button (the bootstrap page has only a heading) | `out/logs/03_frontend-acceptance-first.log` |
| end-to-end | `npx playwright test` with `E2E_BASE_URL=http://127.0.0.1:5173` (Vite dev server of the bootstrap) and a Mailpit container | 0 | 2 | timeout waiting for the "First name" textbox (form not built) | `out/logs/03_e2e-first.log` |

No build, compile, configuration or harness error: every suite compiles, the application context, both containers and the browser start, and every failure is an assertion about missing behaviour. No test passes on bootstrap code.

The stack-level e2e target (`docker compose up`, frontend on 127.0.0.1:8000) is built in phase 4; the phase 3 run used the bootstrap dev server because the compose file is production deployment code.

## First complete run (before any fix)

Run on 2026-10-06T12:11Z after all phase 5 tests were written, all levels together, before any fix (logs `out/logs/05_first-run-*.log`):

| Suite | Passed | Failed |
|---|---|---|
| backend `./mvnw verify` (acceptance 38, integration 10, unit and architecture 82) | 130 | 0 |
| frontend `npx vitest run --coverage` (acceptance 6, unit 20) | 26 | 0 |
| end-to-end `npx playwright test` against `docker compose up` | 2 | 0 |
| **total** | **158** | **0** |

No failures, so nothing to classify (no implementation defect, no defect in a non-frozen test, no frozen test that appears wrong).

## Phase 5 additions after the first run

Mutation testing showed assertions missing in the unit tests (not defects in the code). Tests added or strengthened, no production behaviour changed: filter response bodies and content types, filter chain pass-through, mail text on the sent message, blank-name validation, single-byte reads past the body limit, stream delegation, rate-limiter memory bound and eviction of idle clients only, `RegistrationService` (number collisions, event, no storage on rejection), REQUIRES_NEW transactions in the dispatcher, entity mapping and attempt counter, and an integration test that the confirmation is stored as `sent` after one attempt with the UTC submission time. Production change: a package-private `RateLimiter.tracked()` for the memory-bound test (commit b6c93db).

## Measures (phase 5)

Coverage (JaCoCo for the backend, V8 for the frontend; the JaCoCo data file is deleted before each run because the agent appends by default):

| Component | Tests | Line | Branch | Log |
|---|---|---|---|---|
| backend | unit and architecture (100) | 83.1 % (412/496) | 93.2 % (136/146) | `out/logs/05_jacoco-unit.csv` |
| backend | acceptance and integration (49) | 90.5 % (449/496) | 71.2 % (104/146) | `out/logs/05_jacoco-integration.csv` |
| backend | all (149) | 99.2 % (492/496) | 95.9 % (140/146) | `out/logs/05_final-backend.log` |
| frontend | unit (20) | 89.7 % (87/97) | 90.9 % (60/66) | `out/logs/05_coverage-frontend-unit.log` |
| frontend | acceptance (6) | 82.5 % (80/97) | 68.2 % (45/66) | `out/logs/05_coverage-frontend-acceptance.log` |
| frontend | all (26) | 95.9 % (93/97) | 97.0 % (64/66) | `out/logs/05_coverage-frontend-all.log` |

Thresholds: record only (`quality-requirements.md`).

Mutation score, backend (PIT 1.30.0 with pitest-junit5-plugin 1.2.3, runs on JUnit 6):

| Scope | Tests | Mutants | Killed | Score | Test strength | Log |
|---|---|---|---|---|---|---|
| domain, security, application, config, mail | unit | 196 | 185 | 94 % | 96 % | `out/logs/05_pitest-backend.log`, `05_pitest-backend-mutations.xml` |
| persistence | integration and acceptance (organizer, payer, confirmation) | 4 | 3 | 75 % | 75 % | `out/logs/05_pitest-persistence.log`; the survivor is killed by `RegistrationEntityTest` added afterwards |

Surviving backend mutants, classified individually (`severity-scale.md`: validation, security and persistence code):

| Class:line | Mutant | Classification |
|---|---|---|
| `RegistrationValidator`:73, 100, 108, 135, 142, 147 | return value of an error branch replaced with `""` | equivalent: the value is discarded because the validator throws after collecting the error; Low |
| `RateLimiter`:57 | `prune` call removed in `record` | equivalent for behaviour (`exhausted` and `tryAcquire` prune before counting); only memory is affected, bounded by the eviction; Low |
| `BodySizeFilter$LimitedInputStream`:79 | `count > 0` → `count >= 0` | equivalent: counting 0 bytes changes nothing; Low |
| `SecurityConfig`:55, 60, 66 | bean method returns null (no coverage in unit scope) | covered by the acceptance tests AC-001-10/11 and the integration tests, which fail without these beans; Low |
| `RegistrationEntity`:107 (persistence run) | `confirmationAttempts()` returns 0 | killed by `RegistrationEntityTest` added after the run; Low |

Mutation score, frontend (Stryker 10.0.0, command runner `npm test` per mutant, D-17; the vitest runner did not report kills with vitest 5.0.3, `out/logs/05_stryker-frontend.log`, `-2.log`):

| File | Killed | Timed out | Survived | Score |
|---|---|---|---|---|
| `api.ts` | 43 | 4 | 10 | 82.5 % |
| `App.tsx` | 1 | 0 | 0 | 100 % |
| `config.ts` | 19 | 1 | 4 | 83.3 % |
| `Confirmation.tsx` | 0 | 1 | 5 | 16.7 % |
| `format.ts` | 3 | 0 | 2 | 60.0 % |
| `RegistrationForm.tsx` | 126 | 8 | 22 | 85.9 % |
| **all** | **192** | **14** | **43** | **82.7 %** |

Log: `out/logs/05_stryker-frontend-command.log`. Surviving frontend mutants, by group: the `{" "}` separators in `Confirmation.tsx` (checked only by the e2e test, which Stryker does not run); `maxLength` attribute values (usability only, the backend enforces lengths, SB-01); defensive type guards in `api.ts`/`config.ts` (malformed server or config data); `typeof` shortcut in `format.ts` (equivalent for finite numbers). None is in authoritative validation, security or persistence code (all server-side); classified Low, record only.

## Phase 5 final run

Backend `./mvnw verify`: 149 passed, 0 failed (`out/logs/05_final-backend.log`); frontend `npx vitest run`: 26 passed; e2e: 2 passed (first-run log, code unchanged since); backend and frontend check commands clean.

## Final run (phase 6)
