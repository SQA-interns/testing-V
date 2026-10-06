# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `tanej-confreg-C1-r1`, verified on 2026-10-06 at commit `daeccf9` plus this report. Raw outputs: `out/logs/06_verify/`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 Full suite passes; frozen hashes match | Pass | Final run 203/203 (backend 181 incl. 52 frozen acceptance, frontend 18, e2e 4 frozen): `final-run-backend.log`, `final-run-frontend.log`, `final-run-e2e.log`. `docs/03_acceptance-manifest.sha256` 12/12 match (`hash-check.log`); the only change to a frozen file is the human-approved D-32/D-33 line, with its hash updated in the same commit `1036fc9` |
| DoD-02 Format, lint, type check, static analysis clean | Pass | Backend `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check`: BUILD SUCCESS, 0 SpotBugs, 0 PMD, 0 CPD (`out/logs/04_build/backend-check-gate.log`); frontend `npm run check` (prettier, eslint, tsc) exit 0; semgrep 1.177.0 `p/default`: 2 results, triaged F-07, F-08 |
| DoD-03 Coverage and mutation recorded; thresholds ("record only") | Pass (frontend mutation not measurable, D-35) | Backend coverage (JaCoCo): unit 78.9 % lines / 85.1 % branches; acceptance + integration 90.1 % / 70.7 %; all levels 96.9 % / 90.4 % (`backend-coverage.txt`). Backend mutation (PIT, unit tests, all packages): 205/254 killed, 81 %, test strength 93 % (`backend-pitest-after-F-02.log`). Frontend coverage (Vitest, unit and component): 98.79 % statements, 97.91 % branches, 100 % lines. Frontend mutation score: **not measurable** with the pinned Stryker 10.0.0 and the approved Vitest 5.0.3; the 6.7 % result is invalid (mutants never activated, F-03). Side note, not measured in the project: a scratch copy with Vitest 4.1.11 gave 78.4 % |
| DoD-04 AR checked automatically; no cycles | Pass | `ArchitectureAcceptanceTest` (8 rules: AR-02 rules 1–5, AR-07, AR-05 clock rule, AR-03 slice cycles) passes. AR-01: frontend calls only `/api/workshops` and `/api/registrations` (`src/api.ts`); AR-04: business values only in `application.properties` (tests inject other values, `acceptance-test.properties`); AR-06: `ddl-auto=validate`, schema only from `V1__create_registration.sql`; AR-08: no invoice code (D-20) |
| DoD-05 No open Critical or High | Pass | Findings below: none Critical or High open. Dependency-Check (with D-12, D-13, D-16, D-18 suppressions) and npm audit: 0 Critical, 0 High; gitleaks clean after F-01; semgrep: no ERROR |
| DoD-06 Runtime demonstration | Pass | `runtime-demonstration.log`: local stack healthy; US-001 flow through the frontend proxy, organizer read, storage, e-mail; AC-001-09 at runtime: `out/logs/04_build/runtime-mail-failure-check.log` |
| DoD-07 Every AC → test and commit | Pass | Traceability table below |
| DoD-08 READMEs work from a clean checkout | Pass (phase 7) | `out/logs/07_release/clone-check.log`: fresh clone of `d247efc`; backend build, check, 181 tests; frontend `npm ci`, check, 18 tests, build; Compose stack healthy; e2e 4/4 |
| DoD-09 Release notes list manual tests | Pass (phase 7) | `docs/release-notes.md`, "Must be tested manually by a human" (10 items) |
| DoD-10 Decisions resolved or pending review; inputs unchanged | Pass | `docs/00_input-manifest.sha256` 25/25 match and `git diff 9deb956 -- 01_input …` empty (`hash-check.log`). Pending review: D-20 to D-29 (applied options, D-30); D-34 resolved by D-35 |
| DoD-11 Evidence checks of `phases.md` | Pass, one Low exception | Phase 3 commits touch no `src/main` (0 files between `f1770c5` and the freeze); freeze commit `05ef03e` adds only the manifest, all 12 listed files committed earlier in phase 3; build commits name US-001 / AC ids; one commit over the size guide without a stated reason (F-10, `commit-size-check.txt`); all hashes match |
| DoD-P01 Registration accepted and stored on the running stack | Pass | `runtime-demonstration.log`: `POST /api/registrations` → 201 `REG-000014`, row in `registration`, organizer `GET` returns the same JSON |

## Runtime demonstration

`docker compose --env-file ../.env up --build` in `02_output/` (local environment, default settings, real time 2026-10-06, so the regular fee applies). Log: `out/logs/06_verify/runtime-demonstration.log`.

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local (compose) | start with health checks; `docker compose ps` | all four services `running healthy`; ports published on 127.0.0.1 only; PostgreSQL and the management port 8081 not published |
| backend | local | readiness and liveness on 8081 inside the container (NFR-02, D-26) | `{"status":"UP"}` twice; 8081 not reachable from the host |
| frontend → backend | local | `POST /api/registrations` through the frontend nginx (port 3000), company payer, workshop W2, č/š/ž in names and address | 201, `Location: /api/registrations/REG-000014`, net 300.00 / VAT 66.00 / gross 366.00 (after the deadline: AC-001-02, AC-001-06) |
| backend | local | organizer `GET` with credentials from `.env`; without credentials | 200, identical to the POST response; 401 |
| PostgreSQL | local | stored row | `REG-000014 | Špela Žagar Čeč | company | W2 | 300.00 / 66.00 / 366.00 | 2026-10-06 20:56:20+00` (UTC, NFR-01) |
| Mailpit | local | confirmation e-mail | 1 message, subject `Registration confirmation REG-000014`, body with number, workshop title, amounts, payer, names unchanged |
| frontend | local | `GET /` headers (SB-10) | 200 with CSP, `X-Frame-Options: DENY`, `nosniff`, `no-referrer` |
| backend | local | `GET /api/workshops` headers (SB-10) | CSP `default-src 'none'; frame-ancestors 'none'`, `no-store`, `DENY`, `nosniff`, `no-referrer` |
| backend, frontend | local | container user (SB-11) | uid 10001 and 101 (non-root) |
| backend | local | logs after the flows (SR-01, ES-07) | 0 matches for names, e-mail, company, VAT ID or any `.env` value (`log-personal-data-check.log`) |
| frontend | local | the 4 frozen e2e tests in Chromium | 4/4 pass (`final-run-e2e.log`) |

## Security baseline and project requirements

| Item | Where implemented | How checked |
|---|---|---|
| SB-01 server-side validation | `domain.RegistrationValidator`, `api.RegistrationRequestReader` | `ValidationAcceptanceTest` (17), `RegistrationValidatorTest`, `RegistrationRequestReaderTest` |
| SB-02 authentication and authorization | `security.SecurityConfiguration` (public: POST registration D-29, GET workshops D-28; everything else under `/api` organizer) | `HttpIntegrationTest` (401/404 cases), `AC_001_05_registrationIsNotReadableWithoutOrganizerCredentials`, runtime 401 |
| SB-03 credentials hashed; secrets from configuration | BCrypt in `SecurityConfiguration.organizer`; secrets only via `${…}` without defaults (`application.properties`), startup check of the 16-character minimum | `ApplicationConfigurationTest`; inspection; secret-leak check clean |
| SB-04 TLS outside the local machine | external nginx (production), `APP_MAIL_STARTTLS`; local stack on 127.0.0.1 only | inspection of `docker-compose.yml`; production TLS listed for manual test (release notes) |
| SB-05 encoding, parameterised queries | JPA repository; the only native query has no input; JSON by Jackson; React escapes text; plain-text e-mail | inspection; semgrep (no injection findings) |
| SB-06 rate limiting | `security.RateLimitFilter` on all `/api` (incl. failed authentication) | `FiltersTest` (limit, Retry-After, per client, window reset) |
| SB-07 no internals in errors and logs | `api.ApiExceptionHandler`, `server.error.include-*=never`, filters' `JsonError` | `AC_001_08_rejectionDoesNotExposeInternals`, `AC_001_09_failureResponseHasNoInternalDetails`, `HttpIntegrationTest`, runtime log check |
| SB-08 dependency scan | Dependency-Check 12.1.0, npm audit | `backend-dependency-check.log` / `.json`, `frontend-npm-audit.json`: no Critical/High (F-04 to F-06) |
| SB-09 static analysis and secret scan | SpotBugs, PMD, semgrep, gitleaks | `semgrep.json` (F-07, F-08), `gitleaks-after-F-01.log` |
| SB-10 security headers | `SecurityConfiguration.headers`, `frontend/nginx.conf` | `HttpIntegrationTest.securityHeadersOnApiResponses`, runtime header check |
| SB-11 least privilege | non-root users in both Dockerfiles; DB user owns only the app database | runtime `id -u` 10001 / 101 |
| SB-12 data minimisation | only the fixed API fields; private payers cannot send company data; unknown fields rejected (D-23) | `ValidationAcceptanceTest` cases "private payer with company name", "unknown property" |
| SB-13 purpose and retention | `security-requirements.md` table, unchanged | inspection; retention is defined by the organizer outside the project (release notes) |
| SB-14 consent | no optional processing; nothing preselected except the payer type (not consent) | inspection of `App.tsx` |
| SR-01 no personal data in logs | no request-body or personal-data logging; Hibernate SQL and bind logging off; exception handler logs class names only | `HttpIntegrationTest.personalDataIsNotLogged`; runtime log check (0 matches) |
| SR-02 body size limit | `security.RequestSizeFilter` (16 KiB, also chunked); nginx `client_max_body_size 16k` | `FiltersTest`, `HttpIntegrationTest` (413) |
| SR-03 no organizer credentials over plain HTTP except localhost | `security.TransportFilter` (D-27) | `FiltersTest.credentialsOnlyOverAcceptableTransport` (9 cases); production behaviour behind nginx is a manual test |
| SR-04 test clock never in production | `ApplicationConfiguration.testClockSettings` refuses `prod` with the clock enabled; `TestClockFilter` ignores the header when disabled | `ApplicationConfigurationTest.testClockMustNotBeEnabledInProduction`, `TestClockTest` |
| SR-05 safe e-mail content | plain text only, headers via Jakarta Mail API; e-mail validated without CR/LF | `SmtpConfirmationSenderTest`, `ValidationAcceptanceTest` case "e-mail with header injection" |

## Traceability

Commits: `out/logs/06_verify/traceability-commits.txt` (from `git log`); tests: `docs/03_test-strategy.md`, "AC → tests".

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `RegistrationFeeAcceptanceTest.AC_001_01_*` (3), `FeeScheduleTest`, `RegisterParticipantTest.registersWithEarlyFeeStoresThenSends` | `b431e9d` (tests), `09eacd4` (fee rules), `51cf03f` (use case) |
| AC-001-02 | `RegistrationFeeAcceptanceTest.AC_001_02_*` (2), `FeeScheduleTest`, `RegisterParticipantTest.usesRegularFeeAfterDeadline`; runtime | `b431e9d`, `09eacd4`, `51cf03f` |
| AC-001-03 | `ConfirmationEmailAcceptanceTest.AC_001_03_*` (4), e2e "AC-001-05 AC-001-03 AC-001-06", `SmtpConfirmationSenderTest` | `b431e9d`, `e58e7c8` (mail), `966a56b` (form) |
| AC-001-04 | `RegistrationStorageAcceptanceTest.AC_001_04_*` (2), e2e "AC-001-04" | `a4f0e3f`, `1faf5c7` (storage), `966a56b` |
| AC-001-05 | `RegistrationStorageAcceptanceTest.AC_001_05_*` (6), e2e, `JpaRegistrationStoreTest` | `a4f0e3f`, `1faf5c7`, `db0a0d7` (API), `966a56b` |
| AC-001-06 | `RegistrationFeeAcceptanceTest.AC_001_06_*` (3), e2e, `FeesTest` | `b431e9d`, `09eacd4`, `966a56b` |
| AC-001-07 | `WorkshopAcceptanceTest.AC_001_07_*` (4), e2e "AC-001-07", `RegistrationValidatorTest.workshopRules` | `a4f0e3f`, `09eacd4`, `db0a0d7`, `966a56b` |
| AC-001-08 | `ValidationAcceptanceTest.AC_001_08_*` (17), e2e "AC-001-08", `RegistrationValidatorTest`, `RegistrationRequestReaderTest`, `App.test.tsx` | `d3faa4c`, `09eacd4`, `db0a0d7`, `966a56b` |
| AC-001-09 | `MailFailureAcceptanceTest.AC_001_09_*` (2), `RegisterParticipantTest.mailFailurePropagatesSoTheTransactionRollsBack`, `SmtpConfirmationSenderTest.smtpFailureBecomesConfirmationFailed`; runtime check | `d3faa4c`, `1036fc9` (D-32), `51cf03f`, `db0a0d7` |
| AC-001-10 | `RegistrationStorageAcceptanceTest.AC_001_10_*` (1) | `a4f0e3f` (test), `51cf03f` (use case: no uniqueness rule, D-24) |

## Findings

Severity per `general/quality/severity-scale.md`; tool levels mapped by its tool table.

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | gitleaks v8.30.1 (`generic-api-key`, 4) | Spring Boot's per-run generated password of the bootstrap skeleton's default user, printed in two committed phase 3 test logs (`391f6a6`). Random per test JVM, not an `.env` value, protected nothing that still exists | Fixed: redacted in the files; history not rewritten (working rules), the four fingerprints are in `02_output/.gitleaksignore` with the reason; re-scan clean |
| F-02 | Low | PIT 1.30.0 | Unit tests of the three security filters did not check that allowed requests reach the next filter (surviving `chain.doFilter` mutants); covered only by acceptance tests | Fixed: shared assertion in `FiltersTest`; PIT 198 → 205 killed |
| F-03 | Medium | Stryker 10.0.0 | Frontend mutation score not measurable: with Vitest 5.0.3 (D-11) no mutant is activated; the 6.7 % result is invalid | Resolved by human decision D-35 (D-34 option 3): recorded as not measurable with the pinned tools. Side note, not measured in the project: 78.4 % with Vitest 4.1.11 in a scratch copy |
| F-04 | Medium | Dependency-Check (CVSS v3 5.3) | CVE-2026-64607 in httpclient5 5.5.1 shaded in `docker-java-transport-zerodep` 3.7.1 (test scope, Testcontainers' connection to the local Docker daemon) | Accepted: test scope only, not shipped; no fixed docker-java release (3.7.1 latest on 2026-10-06, D-08 re-check); the related Critical/High are handled by D-13 |
| F-05 | Low | Dependency-Check (CVSS v4 6.9 / v3 5.3, reported Medium) | CVE-2025-15104 matched to `hibernate-validator` 9.1.3.Final | Accepted as false positive: the CVE concerns Nu Html Checker (validator.nu); the project does not use Bean Validation annotations either |
| F-06 | Medium | npm audit | `qs` advisories (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) via `typed-rest-client` via `@stryker-mutator/core` 10.0.0 | Accepted: dev-only mutation tool, never in the built bundle or image; no Stryker release without it |
| F-07 | Medium | semgrep (MEDIUM) `npm-missing-minimum-release-age` | `.npmrc` sets no `min-release-age` | Accepted: the setting needs npm ≥ 11.10; the approved npm 10.9.4 and the pinned 11.6.2 do not support it. Mitigation: exact versions, committed lock file with integrity hashes, `npm ci` in the image build |
| F-08 | Low | semgrep (WARNING → Medium) `nginx missing-internal` | `/api/` proxy location in `frontend/nginx.conf` without `internal` | Lowered to Low as false positive: the location must be reachable by the browser; the proxy target is a fixed host (`backend:8080`), no client input selects it, so there is no SSRF |
| F-09 | Low | PIT, Stryker | Remaining surviving mutants: equivalent mutants (e.g. `null` → `""` for values already rejected), cleanup threshold of the rate limiter, content-type details of error bodies, untested config boundaries; each security, validation or persistence survivor is listed in `backend-pit-survivors.txt` and `backend-pit-mutations-after-F-02.xml` | Accepted: no behaviour a user or attacker can reach; mutation threshold is "record only" |
| F-10 | Low | DoD-11 evidence check | Commit `51cf03f` changes 454 lines (14 files), over the 400-line guide, without a stated reason | Accepted: history is never rewritten; the reason (one use case with its ports, clock and settings that only compile together) is recorded here |

Scanner coverage notes: semgrep could not parse `docs/02_contracts/registration-api.openapi.yaml` (the YAML parser fails at line 35 near non-ASCII example values; PyYAML and the OpenAPI schema validation parse it, `out/logs/02_design/contract-validation.log`) and only partially parsed the vendor script `backend/mvnw`. Container images are not covered by the listed scanners.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 (2026-10-06T20:57Z–20:58Z) | F-01 | Redacted 4 lines in two phase 3 logs; `02_output/.gitleaksignore` with the 4 fingerprints of `391f6a6` (`5ae0fbe`) | gitleaks `dir` on `02_output`: no leaks (without ignore file); `detect` on history with the ignore file: no leaks (`gitleaks-*after-F-01.log`) |
| 2 (2026-10-06T20:58Z–21:03Z) | F-02 | `FiltersTest` asserts that a request reaches the next filter exactly when not rejected and that errors are UTF-8 JSON; boundary tests for empty and 16 KiB chunked bodies; `FindRegistration` positive case (`daeccf9`) | 29/29 targeted tests; PIT 205/254 (81 %), strength 93 % (`backend-pitest-after-F-02.log`); final run 203/203 |

No Critical or High finding occurred, so no blocking verify → fix loop was needed for DoD-05.
