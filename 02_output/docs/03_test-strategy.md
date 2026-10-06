# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location (relative to `02_output/`) | Tool | Interface used | Run with | Frozen |
|---|---|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/confreg/registration/acceptance/` | JUnit 6, Spring Boot test (random port), Testcontainers (`postgres`, `axllent/mailpit` images from `tech-stack.md`), `java.net.http`, Apache POI | HTTP API (`docs/02_contracts/registration-api.openapi.json`), Mailpit HTTP API for received mail | `./mvnw -B test` (needs Docker) | yes |
| Acceptance (architecture) | backend | `backend/src/test/java/si/confreg/registration/acceptance/architecture/` | ArchUnit | compiled production classes (rules ARCH-1 … ARCH-5 of `docs/02_specification.md` section 2) | `./mvnw -B test` | yes |
| Acceptance | frontend | `frontend/tests/acceptance/` | Vitest, Testing Library, jsdom | rendered page (labels, roles, test ids of `docs/02_contracts/registration-form.json`); `fetch` answered per the OpenAPI contract | `npm test` | yes |
| End-to-end | frontend + stack | `frontend/e2e/`, `frontend/playwright.config.ts` | Playwright (Chromium) | browser against the local stack (`docker compose up`), Mailpit HTTP API | `npm run e2e` with the stack running | yes |
| Unit / integration | both | any other test path | JUnit/Mockito, Vitest | implementation | `./mvnw -B verify`, `npm test` | no (phase 5) |

### Harness conventions the implementation must meet (from the specification)

- Backend settings are given to the application under their environment-variable names (`APP_FEE_EARLY`, `APP_DB_URL`, `ORGANIZER_PASSWORD`, …) as Spring properties (`@DynamicPropertySource`); `application.yml` therefore reads them through `${NAME:default}` placeholders (specification section 3). The harness also sets `spring.datasource.*` and `spring.mail.host/port`, so the bootstrap skeleton starts.
- Business values in the backend tests (`AcceptanceConfig`) deliberately differ from the defaults in `environments.md` (other deadline, fees, VAT rate, workshops), so tests only pass when the code reads configuration (AR-04); expected amounts are derived from these values. The regular fee makes half-up and half-even VAT rounding differ (AC-001-08).
- Time is set per request with `X-Test-Now` and `APP_TEST_CLOCK=enabled` (AR-05).
- The mail catcher sits behind a local TCP switch (`SmtpSwitch`) so a test can make SMTP unreachable without touching the backend (AC-001-27).
- "Nothing stored" is observed through the organizer export (row count before and after); "no e-mail" through Mailpit after a 3 s settling time.
- Organizer credentials are random per run; no credential is in the repository.
- The e2e tests read workshops and amounts from the running stack's `GET /api/registration-options`, never from literals.

## Phase 3 run against the bootstrap skeleton (red)

Run 2026-10-06, commit before the freeze. Logs: `logs/03_backend-acceptance-run.log`, `logs/03_frontend-acceptance-run.log`, `logs/03_e2e-run.log`.

| Suite | Tests | Passed | Failed | Reason for the failures |
|---|---|---|---|---|
| Backend acceptance (API) | 39 | 0 | 39 | Behaviour missing: `POST /api/registrations` answers 401 (no public endpoint yet; the skeleton's default security protects every path); rejection tests stop at the organizer export, which answers 401 (export missing); `GET /api/registrations/CR-999999` gives 401 instead of 404. The Spring context, PostgreSQL and Mailpit containers start in every test class (no setup error). |
| Backend architecture | 5 | 3 | 2 | ARCH-1 (layers) and ARCH-2 (slice cycles) fail because the declared layers do not exist yet (empty layers / no slices). |
| Frontend acceptance | 9 | 0 | 9 | Behaviour missing: the skeleton page has no form (`Unable to find role="radio" and name /Alpha workshop/`). |
| End-to-end | 2 | 0 | 2 | Behaviour missing: with the skeleton page served on port 3000 (`vite preview`), `GET /api/registration-options` is not answered by a backend (`ok() == false`, empty JSON). Without anything on port 3000 the tests fail with `ECONNREFUSED`; the local stack is built in phase 4. |
| **Total** | **55** | **3** | **52** | |

### Tests that pass only because the bootstrap code already provides the behaviour

| Test | Why it passes on the skeleton |
|---|---|
| `ArchitectureAcceptanceTest.arch3_ar07_onlyMailComponentSendsMail` | the skeleton has no class that uses mail |
| `ArchitectureAcceptanceTest.arch4_ar05_onlyClockComponentReadsCurrentTime` | the skeleton has no class that reads the current time |
| `ArchitectureAcceptanceTest.arch5_ar01_onlyApiLayerDefinesWebEndpoints` | the skeleton defines no web endpoint |

## AC → test traceability

| AC | Tests |
|---|---|
| AC-001-01 | `RegistrationAcceptanceTest.ac001_01_*` (3), frontend "AC-001-01 …" (2), e2e "AC-001-01 AC-001-04 …" |
| AC-001-02 | `PricingAcceptanceTest.ac001_02_*` (2) |
| AC-001-03 | `PricingAcceptanceTest.ac001_03_studentRegistersForFreeBeforeAndAfterDeadline` |
| AC-001-04 | `ConfirmationEmailAcceptanceTest.ac001_04_*` (4), e2e "AC-001-01 AC-001-04 …" |
| AC-001-05 | `OrganizerAccessAcceptanceTest.ac001_05_*` (2) |
| AC-001-06 | `PricingAcceptanceTest.ac001_06_*` |
| AC-001-07 | `PricingAcceptanceTest.ac001_07_*` |
| AC-001-08 | `PricingAcceptanceTest.ac001_08_*` (2) |
| AC-001-09 | `PricingAcceptanceTest.ac001_09_*` |
| AC-001-10 | `PricingAcceptanceTest.ac001_10_*` |
| AC-001-11 | `ValidationAcceptanceTest.ac001_11_*` (2) |
| AC-001-12 | `ValidationAcceptanceTest.ac001_12_*` |
| AC-001-13 | `ValidationAcceptanceTest.ac001_13_*` |
| AC-001-14 | `ValidationAcceptanceTest.ac001_14_*` (2) |
| AC-001-15 | `ValidationAcceptanceTest.ac001_15_*` (2) |
| AC-001-16 | `RegistrationAcceptanceTest.ac001_16_*` (2) |
| AC-001-17 | `ValidationAcceptanceTest.ac001_17_*` (2) |
| AC-001-18 | `RegistrationAcceptanceTest.ac001_18_*` |
| AC-001-19 | `ValidationAcceptanceTest.ac001_19_*` (2) |
| AC-001-20 | frontend "AC-001-20 …", e2e "AC-001-20 …" |
| AC-001-21 | frontend "AC-001-21 …" (2) |
| AC-001-22 | frontend "AC-001-22 …" |
| AC-001-23 | frontend "AC-001-23 …" |
| AC-001-24 | frontend "AC-001-24 …" (2) |
| AC-001-25 | `ConfirmationEmailAcceptanceTest.ac001_25_*` |
| AC-001-26 | `ConfirmationEmailAcceptanceTest.ac001_26_*` |
| AC-001-27 | `MailOutageAcceptanceTest.ac001_27_*` |
| AC-001-28 | `OrganizerAccessAcceptanceTest.ac001_28_*` |
| AC-001-29 | `OrganizerAccessAcceptanceTest.ac001_29_*` |
| AC-001-30 | `OrganizerAccessAcceptanceTest.ac001_30_*` |
| NFR-01 | `RegistrationAcceptanceTest.ac001_01_nfr01_*`, `ConfirmationEmailAcceptanceTest.ac001_04_nfr01_*` |
| SR-05 | `ConfirmationEmailAcceptanceTest.ac001_04_sr05_*` (plain text, UTF-8, markup not interpreted) |
| AR-01 … AR-07 | `ArchitectureAcceptanceTest` (ARCH-1 … ARCH-5) |

Controls without an acceptance criterion (SR-01 … SR-04, SB-06, SB-10, request size, test-clock refusal in `prod`) are covered by unit and integration tests in phase 5 and by the phase 6 review.

## First complete run (before any fix)

## Final run (phase 6)
