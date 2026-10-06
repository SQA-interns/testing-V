# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Location (relative to `02_output/`) | Interface under test | Runs with | Frozen |
|---|---|---|---|---|
| Acceptance, backend | `backend/src/test/java/si/confreg/registration/acceptance/` | REST API (`registration-api.openapi.yaml`), Mailpit API, `registration` table of `registration-storage.sql` (only to prove "nothing stored") | `./mvnw test` (JUnit Jupiter, Spring Boot test on a random port, Testcontainers `postgres` and `axllent/mailpit` images from `tech-stack.md`) | yes (phase 3 freeze) |
| Acceptance, frontend | `frontend/tests/acceptance/` | the rendered page (`registration-form.json` ids), `fetch` replaced by responses shaped per the OpenAPI contract | `npm test` (Vitest, jsdom, Testing Library) | yes |
| End-to-end | `frontend/e2e/` | the real page in Chromium, the real backend through `/api`, Mailpit API | `npm run test:e2e` (Playwright); needs backend on 127.0.0.1:8080 and Mailpit on 127.0.0.1:8025 (local stack); `E2E_BASE_URL=http://127.0.0.1:3000` targets the frontend container | yes |
| Unit / integration | any other test path | implementation | phase 5 | no |

### How the acceptance tests respect the rules

- Black box: no backend test imports production classes; the only production artefact used is the Spring Boot application started by `@SpringBootTest`. The frontend test imports only the page entry component `App`.
- Configuration, not literals (REQ-REG-01 "Data and fixtures", AR-04): the backend harness reads `APP_CONFERENCE_TZ`, `APP_EARLY_BIRD_DEADLINE`, `APP_FEE_EARLY`, `APP_FEE_REGULAR`, `APP_VAT_RATE`, `APP_WORKSHOPS`, `APP_RATE_LIMIT_PER_HOUR` from the configuration table of `project/00_setup/environments.md` (`ConferenceSettings`), starts the backend with exactly these values (`app.*` properties, spec §3) and computes every expected amount, instant and workshop from them. The frontend tests use clearly marked mock API data; the e2e tests compare the page with the e-mail and with `GET /api/workshops`.
- Time: every backend request carries `X-Test-Now`; the backend runs with `app.test-clock=enabled` and profile `test`. No test depends on the system clock. The e2e tests use the real clock and do not assert which fee applies, only that page and e-mail agree.
- Isolation: Mailpit is emptied before each backend test; each test uses its own client address (`X-Forwarded-For`, spec §13) so the per-client rate limit never couples tests; organizer credentials are random per run (test-only, never a real secret).
- Rejections also check that nothing changed: row count of `registration` unchanged and no e-mail (AC-001-07, AC-001-12).

### AC → test map

| AC | Backend acceptance (class › tests) | Frontend acceptance | End-to-end |
|---|---|---|---|
| AC-001-01 | `FeeAcceptanceTest` › `ac001_01_*` (2) | | |
| AC-001-02 | `FeeAcceptanceTest` › `ac001_02_*` (1); `RepeatedRegistrationAcceptanceTest` (fee of the second) | | |
| AC-001-03 | `FeeAcceptanceTest` › `ac001_03_*` (2) | | |
| AC-001-04 | `ConfirmationAcceptanceTest` › `ac001_04_*` (4, API level, incl. NFR-01) | | `AC-001-11 AC-001-04 registration through the page …` |
| AC-001-05 | `PayerDataAcceptanceTest` › `ac001_05_*` (4) | | |
| AC-001-06 | `FeeAcceptanceTest` › `ac001_06_*` (3) | | |
| AC-001-07 | `ValidationAcceptanceTest` › `ac001_07_*` (21, API level) | | `AC-001-11 AC-001-07 server-side validation error …` |
| AC-001-08 | `WorkshopAcceptanceTest` › `ac001_08_*` (4) | | |
| AC-001-09 | `RepeatedRegistrationAcceptanceTest` › `ac001_09_*` (2) | | |
| AC-001-10 | `OrganizerAccessAcceptanceTest` › `ac001_10_*` (5) | | |
| AC-001-11 | `WorkshopAcceptanceTest` › `ac001_11_*` (1) | `registration-form.acceptance.test.tsx` (10) | `AC-001-11 the form offers …`, plus the two above |
| AC-001-12 | `RateLimitAcceptanceTest` › `ac001_12_*` (3) | | |

Totals: backend acceptance 52, frontend acceptance 10, end-to-end 3.

## Acceptance tests at the freeze (phase 3, against the bootstrap skeleton)

Run before any production code beyond the phase 0 skeleton existed. Logs: `out/logs/03_backend-acceptance-first-run.log`, `03_frontend-acceptance-first-run.log`, `03_frontend-e2e-first-run.log`.

| Suite | Passed | Failed | Reason (one line per group) |
|---|---|---|---|
| Backend acceptance | 0 | 52 | every request answers 401 (bootstrap has only Spring Security defaults, no endpoints) where the tests expect 201, 422, 200 or 404; no build, configuration or harness error |
| Frontend acceptance | 0 | 10 | the bootstrap page has only the heading: workshop options and form fields not found |
| End-to-end | 0 | 3 | run against the bootstrap backend jar (throwaway PostgreSQL and Mailpit containers) and the Vite dev server: `GET /api/workshops` answers 401 (2 tests); `#registration-form` not found (1 test) |

No test passes only because of bootstrap code.

Harness defects found and fixed before the freeze (not behavioural): static initialisation order of the random generator in `AcceptanceTestBase`; business settings first read from the backend's own environment (failed as a configuration error), now read from `environments.md` and passed to the backend.

## First complete run (before any fix)

Phase 5, 2026-10-06, all levels together, before any fix. Logs: `out/logs/05_first-full-run-backend.log` (`./mvnw -B verify`), `05_first-full-run-frontend.log` (`npx vitest run`), `05_first-full-run-e2e.log` (Playwright against the compose stack, `E2E_BASE_URL=http://127.0.0.1:3000`).

| Level | Suite (location) | Passed | Failed |
|---|---|---|---|
| Acceptance | backend `acceptance/` (8 classes) | 52 | 0 |
| Acceptance | frontend `tests/acceptance/` | 10 | 0 |
| End-to-end | frontend `e2e/` | 3 | 0 |
| Unit | backend `domain/` (FeePolicyTest 7, DomainValuesTest 9) | 16 | 0 |
| Unit | backend `application/` (RegistrationValidatorTest 24, RegisterParticipantTest 7, RateLimiterTest 9) | 40 | 0 |
| Unit | backend `web/` (WebFiltersTest 27, RegistrationRequestReaderTest 4) | 31 | 0 |
| Unit | backend `mail/` (SmtpConfirmationSenderTest 3), `config/` (StartupGuardsTest 4) | 7 | 0 |
| Architecture | backend `architecture/ArchitectureTest` (rules A1-A7, 10 rules) | 10 | 0 |
| Integration | backend `integration/ApiIntegrationTest` (SB-10, SR-01, SR-02, SB-07, storage) | 6 | 0 |
| Unit | frontend `src/api.test.ts` 8, `src/App.test.tsx` 2 | 10 | 0 |
| **Total** | | **185** | **0** |

Classification: no failures, so nothing to classify. Two defects in non-frozen tests were corrected while writing them, before this run (not counted as failures): a jsdom `requestSubmit()` that native validation would block (now `fireEvent.submit`), and a convoluted registration number literal in `ApiIntegrationTest`.

What the unit and integration tests add beyond the acceptance tests: fee rule in a zone where local and UTC dates differ, rounding half up, scaling; every validation rule incl. limits at the boundary, control characters, wrong JSON types; number collision retry (spec 4.3) and e-mail failure keeping the registration (D-09); sliding window edges; test clock scoping and 400 on a malformed `X-Test-Now`; body limit with and without Content-Length (SR-02); SR-03 loopback detection and 403; failed-login blocking (SB-06); SR-04 and organizer-credential start-up refusals; e-mail contract incl. header-injection rejection (SR-05) and UTF-8 (NFR-01); security headers (SB-10); no personal data in logs (SR-01); architecture rules A1-A7 (DoD-04).

## Final run (phase 6)

Phase 6.
