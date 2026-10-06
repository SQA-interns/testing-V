# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Location | Runs with | Interface used | Frozen |
|---|---|---|---|---|
| Acceptance (backend) | `backend/src/test/java/si/confreg/registration/acceptance/` and `backend/src/test/resources/acceptance/` | `./mvnw -B test` (JUnit 6, Testcontainers `postgres` and `axllent/mailpit` from `tech-stack.md`; needs Docker) | HTTP API on a random port; Mailpit HTTP API for e-mail; storage contract (`registration` table) for "nothing stored" | yes (phase 3 freeze commit) |
| Acceptance (architecture) | `…/acceptance/ArchitectureAcceptanceTest.java` | same | compiled classes (ArchUnit), rules declared in `docs/02_specification.md` section 2 | yes |
| End-to-end | `frontend/e2e/`, `frontend/playwright.config.ts` | `npx playwright test` in `02_output/frontend/` with the local stack running (`docker compose up` in `02_output/`), `ORGANIZER_USERNAME`/`ORGANIZER_PASSWORD` exported from `.env` | registration form in Chromium (Vite dev server proxying `/api`); organizer API and Mailpit API to check the outcome | yes |
| Unit / integration | other test paths | phase 5 | implementation | no |

Configuration in the acceptance tests (AR-04): the backend receives the business settings from `acceptance/acceptance-test.properties` under their `environments.md` names. The values deliberately differ from the defaults (fees 100.10 / 200.30, VAT 0.25, deadline 2026-03-15, workshops TA/TB), so a hard-coded default fails. The VAT values exercise half-up rounding (100.10 × 0.25 = 25.025 → 25.03). Every expected amount, date boundary and workshop is computed from that file; no business value is written into a test. The deadline boundaries are computed in `APP_CONFERENCE_TZ`: the first instant after the deadline day (2026-03-15T23:00Z) is still the deadline date in UTC, so a UTC-based implementation fails.

Test clock: the acceptance tests run with `APP_TEST_CLOCK=enabled` and send `X-Test-Now`. The e2e tests do not set the time; they compare what the form shows with what the organizer API returns, so they work with any configuration of the local stack.

## AC → tests

| AC | Acceptance tests (`…acceptance.`) | End-to-end tests (`e2e/registration.e2e.spec.ts`) |
|---|---|---|
| AC-001-01 | `RegistrationFeeAcceptanceTest.AC_001_01_*` (3) | – |
| AC-001-02 | `RegistrationFeeAcceptanceTest.AC_001_02_*` (2) | – |
| AC-001-03 | `ConfirmationEmailAcceptanceTest.AC_001_03_*` (4) | "AC-001-05 AC-001-03 AC-001-06 …" |
| AC-001-04 | `RegistrationStorageAcceptanceTest.AC_001_04_*` (2) | "AC-001-04 company payer details …" |
| AC-001-05 | `RegistrationStorageAcceptanceTest.AC_001_05_*` (6) | "AC-001-05 AC-001-03 AC-001-06 …" |
| AC-001-06 | `RegistrationFeeAcceptanceTest.AC_001_06_*` (3) | "AC-001-05 AC-001-03 AC-001-06 …" |
| AC-001-07 | `WorkshopAcceptanceTest.AC_001_07_*` (4) | "AC-001-07 workshop is chosen …" |
| AC-001-08 | `ValidationAcceptanceTest.AC_001_08_*` (15 parameterised cases + 2) | "AC-001-08 invalid e-mail …" |
| AC-001-09 | `MailFailureAcceptanceTest.AC_001_09_*` (2) | – (needs a failing SMTP server) |
| AC-001-10 | `RegistrationStorageAcceptanceTest.AC_001_10_*` (1) | – |
| AR-02, AR-03, AR-05, AR-07 | `ArchitectureAcceptanceTest` (8 rules) | – |

NFR-01 (č, š, ž through storage and e-mail) is covered by every acceptance and e2e test above: their names and addresses use those characters, and `AC_001_03_emailKeepsSlovenianCharactersOfTheNameUnchanged` and the AC-001-05 field comparisons check them explicitly.

## Phase 3 runs against the bootstrap skeleton

Run 1 (2026-10-06, `out/logs/03_test-design/backend-acceptance-run1.log`): 52 tests, 48 failed (33 failures, 15 errors), 4 passed. The 15 errors in `ValidationAcceptanceTest` were a harness defect: the skeleton answers 401, which passed the `isBetween(400, 499)` check, and reading the error body then threw. The test now asserts 400, the status in the contract, so it fails as an assertion. `AC_001_08_rejectionDoesNotExposeInternals` passed only because the skeleton's 401 has an empty body; it now also asserts 400 and fails.

Run 2 (final before freeze, `backend-acceptance-run2.log`): 52 tests, 49 failed, 3 passed, 0 errors. Every failure is an assertion failure because the behaviour is missing:

| Group | Count | Reason |
|---|---|---|
| Registration, retrieval, workshops, e-mail, fees (expected 201/200/2xx/404, got 401) | 25 | no endpoint; the skeleton's default Spring Security answers 401 |
| Rejections (expected 400, got 401) | 17 | no endpoint or validation |
| Mail failure (expected 5xx, got 401) | 2 | no endpoint |
| ArchUnit rules 1–4 and 7 | 5 | the declared packages do not exist yet ("failed to check any classes") |

Passing only because of bootstrap code (no class violates the rule yet):

- `ArchitectureAcceptanceTest.AR_02_rule5_onlyPersistenceUsesJpa`
- `ArchitectureAcceptanceTest.AR_07_rule5_onlyMailComponentSendsEmail`
- `ArchitectureAcceptanceTest.AR_05_rule6_onlyClockComponentReadsTheCurrentTime`

End-to-end run 1 (`frontend-e2e-run1.log`): 4 tests, 4 failed, at the first form element (`element(s) not found`); the skeleton page has only a heading.

Formatting and lint before the freeze: `./mvnw spotless:apply` (google-java-format) on the backend tests; `npm run check` (prettier, eslint, tsc) clean on the frontend.

## First complete run (before any fix)

Recorded in phase 5.

## Final run (phase 6)

Recorded in phase 6.
