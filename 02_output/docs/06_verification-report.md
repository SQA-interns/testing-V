# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `kyuhi-confreg-C2-r1`, verified 2026-10-06/07 (UTC) on the commits up to and including the phase 6 fixes. All raw output is in `out/logs/06_*`.

## Summary

- Full suite: **190 passed, 0 failed** (backend 167, frontend 20, end-to-end 3); every check command clean.
- Manifests: input manifest 26/26 and acceptance manifest 16/16 hashes match; `01_input/` and protected files unchanged since the starting commit (`06_manifest-check.log`).
- No open Critical or High finding. One High (semgrep, F-01) lowered to Low with the human's answer (D-17).
- Open item (accepted in D-16): F-09, the frontend mutation score cannot be measured with the pinned Stryker 10.0.0 and vitest 5.0.3.
- Secret-leak check: no `.env` value in any tracked or untracked file, 0 in commit messages.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 Full suite passes; frozen hashes match | pass | `06_final-run-backend.log` (`./mvnw -B clean verify`: 167/167), `06_final-run-frontend.log` (`npx vitest run`: 20/20), `06_final-run-e2e.log` (Playwright vs. compose stack: 3/3); `06_manifest-check.log` |
| DoD-02 Format, lint, type check, static analysis without errors | pass | backend `./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check` exit 0 (`06_final-check-backend.log`; SpotBugs exclusions narrow and justified, `backend/spotbugs-exclude.xml`); frontend `npm run check` (prettier, eslint, `tsc --noEmit`) and `npm run build` exit 0 (`06_final-check-frontend.log`); semgrep findings triaged F-01, F-02, F-06 |
| DoD-03 Coverage and mutation recorded; thresholds met | pass with open item | coverage recorded (below); thresholds are "record only"; backend mutation recorded (PIT 82 %); frontend mutation recorded as not measurable with the pinned tools (F-09, accepted in D-16) |
| DoD-04 AR checked automatically; no cycles | pass | `ArchitectureTest` rules A1–A7 (AR-02, AR-03 slice cycle check, AR-05, AR-07), 10/10; AR-01 by e2e (single page, `/api` only) and nginx config; AR-04 by acceptance harness (backend started with settings from `environments.md`); AR-06 Flyway (`ddl-auto: validate`, runtime demo step 9); AR-08 no invoice code (AC-001-05 tests) |
| DoD-05 No open Critical/High | pass | findings table; F-01 lowered with D-17 |
| DoD-06 Components run in target environment; core flows at runtime | pass | `06_runtime-demo.log` (compose stack, profile `local`), `06_final-run-e2e.log` (browser flows against the frontend container) |
| DoD-07 Every AC traces to a test and a commit | pass | traceability table below |
| DoD-08 READMEs work from a clean checkout | phase 7 | READMEs are written in phase 7; its gate runs the clean-checkout check |
| DoD-09 Release notes list manual tests | phase 7 | `docs/release-notes.md` is written in phase 7 |
| DoD-10 Decisions resolved or pending review; inputs unchanged | pass | D-01..D-03, D-06..D-09, D-11..D-15 pending review (non-blocking); D-04, D-05, D-10, D-16, D-17 resolved; inputs unchanged (`06_manifest-check.log`) |
| DoD-11 Evidence checks of `phases.md` | pass with F-10 | phase 3 commits add no production code; freeze commit 3fece11 adds only the manifest, every listed file committed earlier in phase 3; build commits name US/AC ids; manifest hashes match; two commits exceed the size guide without a stated reason (F-10, `06_commit-size-check.log`) |
| DoD-P01 Registration accepted and stored on the running stack | pass | `06_runtime-demo.log` steps 3, 4, 9: 201, organizer read 200, row in PostgreSQL |

## Measures

| Measure | Backend | Frontend |
|---|---|---|
| Line coverage, unit | 70.0 % (455/650) | 80.8 % (63/78) |
| Branch coverage, unit | 83.8 % (196/234) | 75.4 % (43/57) |
| Line coverage, integration/acceptance | 88.0 % (572/650) | 92.3 % (72/78) (component acceptance tests) |
| Branch coverage, integration/acceptance | 69.7 % (163/234) | 82.5 % (47/57) |
| Mutation score (validation, security, persistence, business rules) | 82 % (216/262 killed, test strength 93 %) on domain, application, web filters, mail, start-up guards; persistence adapter covered by integration tests only (PIT with Testcontainers timed out, `06_backend-pitest.log` first attempt) | not measurable with the pinned tools (F-09, D-16) |
| Tests, final run | 167 passed, 0 failed | 20 passed (+3 e2e), 0 failed |

Sources: `06_backend-coverage-unit.csv`, `06_backend-coverage-integration.csv` (JaCoCo 0.8.12; architecture tests run in both sets), `06_frontend-coverage-unit.log`, `06_frontend-coverage-acceptance.log` (v8), `06_backend-pitest-after-F08.log`, `06_backend-pitest-mutations-after-F08.csv`. Code size (cloc reports 1.98, D-02): backend 1,546 Java production lines, 2,487 test lines; frontend 440 production lines, 589 test lines (`06_cloc.log`, `06_cloc-frontend.log`); duplication: CPD no duplicates, jscpd 0 % (`06_jscpd/`).

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all four services | local: `docker compose --env-file ../.env up --build` in `02_output/`, profile `local`, 127.0.0.1 only | health of every container | backend, frontend, postgres, mailpit healthy |
| frontend (nginx) | local | `GET /` with security headers; `/api` proxy | 200; CSP, nosniff, DENY, no-referrer present |
| backend via frontend | local | workshop list; registration of a company payer with Slovenian characters (DoD-P01, NFR-01); organizer read with HTTP Basic; read without credentials; invalid registration | 200; 201 with fee 300.00 / 66.00 / 366.00 (system date after the deadline); 200 identical data; 401; 422 with one error per field |
| mail | local Mailpit | confirmation e-mail | exactly 1 message, subject with number, body with net, VAT, gross and workshop title, "Čedomir" intact |
| database | local PostgreSQL | stored row; Flyway history | row with UTC timestamp; migration V1 successful |
| backend | local | readiness on management port 8081 inside the container; port not reachable from the host; non-root users; backend log free of the demo's personal data | `{"status":"UP"}`; not reachable; `app` / `nginx`; 0 matching lines |
| frontend + backend + mail | local, Chromium | e2e: workshop options, registration through the page with confirmation and e-mail, server-side validation message | 3/3 passed against the container (`06_final-run-e2e.log`) and against the Vite dev server in phase 4 |

The request bodies of the API demonstration were sent from UTF-8 files: Windows `curl.exe` re-encodes non-ASCII command-line arguments, which the backend correctly rejected as malformed JSON (400) in a first attempt (kept in the log header).

## Security baseline and project requirements

| Item | Implemented in | Checked by |
|---|---|---|
| SB-01 server-side validation | `application.RegistrationValidator`, `web.RegistrationRequestReader` | `ValidationAcceptanceTest` (21), `RegistrationValidatorTest` (24), `RegistrationRequestReaderTest` |
| SB-02 authN/authZ on non-public operations | `config.SecurityConfig` (only POST registrations and GET workshops public, D-10) | `OrganizerAccessAcceptanceTest`, `ApiIntegrationTest.unknownApiPathsNeedAuthenticationThenAre404` |
| SB-03 salted slow hashes; secrets from config | BCrypt in `SecurityConfig`; secrets only from environment, no defaults (`application.yml`); start-up refusal (`StartupGuards`) | inspection; `StartupGuardsTest` |
| SB-04 TLS outside the local machine | production behind external nginx with HTTPS (`environments.md`); forwarded headers trusted (`server.forward-headers-strategy: native`) | inspection; manual test in release notes |
| SB-05 output encoding; parameterised queries | Jackson JSON, React text rendering, JPA only | inspection; semgrep (no injection findings) |
| SB-06 rate limiting | `web.RateLimitFilter`, `application.RateLimiter` (registration, workshops, failed logins) | `RateLimitAcceptanceTest`, `RateLimiterTest`, `WebFiltersTest` |
| SB-07 no internals in errors/logs | `web.Problems`, `ApiExceptionHandler`, `server.error.*: never` | `ApiIntegrationTest.malformedRequestsGetProblemResponsesWithoutDetails`; error bodies in all logs inspected |
| SB-08 dependency scan | OWASP Dependency-Check, npm audit | `06_backend-dependency-check.log/.json`, `06_frontend-npm-audit.json`: no Critical/High (F-03, F-04) |
| SB-09 static analysis and secret scan | SpotBugs, PMD, semgrep, gitleaks | `06_final-check-backend.log`, `06_semgrep.json`, `06_gitleaks-*.json` (F-01, F-02, F-05, F-06) |
| SB-10 security headers | backend `SecurityConfig.headers`; frontend `nginx/default.conf.template` | `ApiIntegrationTest.securityHeadersOnApiResponses`; runtime demo |
| SB-11 least privilege | non-root users in both images; management port not published | runtime demo step 10 |
| SB-12 data minimisation | only AC-001-07 fields and workshop stored; company fields dropped for private payers (D-08) | `PayerDataAcceptanceTest`; storage contract |
| SB-13 purpose and retention | `security-requirements.md` "Personal data"; spec 7.7 | inspection |
| SB-14 consent | not triggered (Q4 assumption: no acknowledgment field) | inspection |
| SR-01 no personal data in logs | logs carry registration numbers and class names only | `ApiIntegrationTest.personalDataNeverReachesTheLog`; runtime demo step 11 |
| SR-02 request size limit | `web.RequestBodyLimitFilter` (16 KiB), nginx `client_max_body_size 16k` | `WebFiltersTest` (3 cases), `ApiIntegrationTest.bodyLargerThanTheLimitIs413` |
| SR-03 credentials only over HTTPS or localhost | `web.InsecureCredentialsFilter` (D-13) | `WebFiltersTest` (loopback table, 403, pass-through) |
| SR-04 test clock never in production | `config.StartupGuards` | `StartupGuardsTest` |
| SR-05 safe e-mail content | plain text only, fixed subject, `InternetAddress` strict parsing, control characters rejected | `SmtpConfirmationSenderTest.invalidAddressesAreRejected`, `ValidationAcceptanceTest` "email header injection" |
| NFR-01 Slovenian characters | UTF-8 end to end | `ConfirmationAcceptanceTest.ac001_04_slovenianCharactersSurviveStorageAndEmail`, `SmtpConfirmationSenderTest`, runtime demo |
| NFR-02 health and readiness in container health checks | Actuator probes on 8081, compose health checks | runtime demo |

Secret-leak check (skill step 8, `06_secret-leak-check.log`): no tracked or untracked file contains a `.env` value of 6 or more characters; commit messages: 0. The first run listed the raw gitleaks report, whose commit-author e-mail field equals one `.env` value; the author fields were removed from the archived reports before committing. One `.env` value is shorter than 6 characters and is not covered by the skill's check; it equals part of the run id and repository path (preflight report, "Notes").

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `FeeAcceptanceTest` ac001_01 ×2; `FeePolicyTest` | c41766c (test), cbebaf9, fe68ab3 |
| AC-001-02 | `FeeAcceptanceTest` ac001_02; `RepeatedRegistrationAcceptanceTest`; `FeePolicyTest` | c41766c, cbebaf9, fe68ab3 |
| AC-001-03 | `FeeAcceptanceTest` ac001_03 ×2; `FeePolicyTest.deadlineIsJudgedInTheConferenceZoneNotUtc` | c41766c, cbebaf9, 95ad23d, fe68ab3 |
| AC-001-04 | `ConfirmationAcceptanceTest` ×4; e2e "registration through the page"; `SmtpConfirmationSenderTest`; `RegisterParticipantTest` | c41766c, 8ced02a, b40fd80, 0b0f18f, 18d49aa, d160c6d |
| AC-001-05 | `PayerDataAcceptanceTest` ×4; `DomainValuesTest` | 1909095, cbebaf9, ef1d5ee |
| AC-001-06 | `FeeAcceptanceTest` ac001_06 ×3 | c41766c, cbebaf9 |
| AC-001-07 | `ValidationAcceptanceTest` ×21; e2e "server-side validation error"; `RegistrationValidatorTest`; `RegistrationRequestReaderTest` | 1909095, 8ced02a, b40fd80, 18d49aa, 9111433 |
| AC-001-08 | `WorkshopAcceptanceTest` ac001_08 ×4 | 7903ec1, b40fd80, 18d49aa |
| AC-001-09 | `RepeatedRegistrationAcceptanceTest` ×2; `RegisterParticipantTest` | 7903ec1, b40fd80, ef1d5ee |
| AC-001-10 | `OrganizerAccessAcceptanceTest` ×5 | 7903ec1, 18d49aa, fe68ab3 |
| AC-001-11 | `WorkshopAcceptanceTest` ac001_11; `registration-form.acceptance.test.tsx` ×10; e2e ×3; `api.test.ts`, `App.test.tsx` | 7903ec1, 1c53751, 8ced02a, 18d49aa, d1e5738, 23744fb |
| AC-001-12 | `RateLimitAcceptanceTest` ×3; `RateLimiterTest`; `WebFiltersTest` | 7903ec1, b40fd80, 95ad23d, 26e5af7 |

Assumptions applied (REQ-REG-01 "Standard of verification"): Q3 — each registration stores its own fee and gets its own confirmation (AC-001-09 tests); Q4 — no privacy acknowledgment field. New open questions raised during the run: D-06 to D-09, D-10 (answered), D-11 to D-14, D-16.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low (reported High) | semgrep `use-of-basic-authentication` (ERROR) | OpenAPI contract uses HTTP Basic for the organizer | accepted; lowered with the human's answer and evidence in D-17 |
| F-02 | Medium | semgrep `npm-missing-minimum-release-age` | `frontend/.npmrc` sets no minimum release age for new package versions | accepted: every version is pinned exactly, the lock file is committed and builds use `npm ci`, so no new version is ever resolved |
| F-03 | Low (reported Medium) | Dependency-Check CVE-2025-15104, CVSS 5.3 | matched to `hibernate-validator` 9.1.3 through CPE `validator:validator` (Nu Html Checker) | accepted as false positive: different product |
| F-04 | Medium | npm audit (2 moderate) | `qs` 6.15.1 via `typed-rest-client` 2.3.1 via `@stryker-mutator/core` 10.0.0 (dev only) | accepted: no fixed version within Stryker's range `~2.3.0`; used only by Stryker's dashboard reporter, which is not used; not in the shipped image |
| F-05 | Low | gitleaks `generic-api-key` (history) | `logs/03_backend-acceptance-first-run.log` line 111 contains Spring Boot's generated development password of the phase 3 bootstrap test context | accepted: random per test JVM, belonged to no running system; the log is frozen (D-15) and in history |
| F-06 | Low | semgrep parse warning | vendored `backend/mvnw` partly not parsed | accepted: upstream Maven wrapper script |
| F-07 | Low | runtime demonstration | `submittedAt` in the 201 response had nanoseconds; the stored value has microseconds, so the response differed from the stored registration | fixed d160c6d (+ test 4789e31); re-verified `06_fix-F07.log`, `06_fix-F07-runtime.log` |
| F-08 | Low | PIT | surviving mutants in security and validation code: missing pass-through assertion in the credentials filter, rate-limit boundaries, chunked body reads | fixed by tests 26e5af7 (79 % → 82 %); remaining survivors classified below |
| F-09 | Medium | Stryker | frontend mutation score not measurable: mutants never activated with vitest 5.0.3 | accepted as open item (D-16, human): revisit with the next Stryker or vitest version |
| F-10 | Low | commit-size check | d1e5738 (form page: component, API client and stylesheet, 452 lines) and fd52f82 (three application unit-test classes, 541 lines) exceed the size guide without a stated reason | accepted: each is one reviewable unit (the page does not work without its client; the three classes test one layer); history is not rewritten |

Remaining PIT survivors (F-08), classified individually: `RegistrationValidator` lines 121, 129, 134, 162, 166 (empty value instead of null after an error is recorded) — equivalent; `RegistrationRequestReader` line 61 — equivalent (type error already recorded); `RateLimitFilter` lines 54, 57 (one bucket name emptied, buckets stay distinct) — equivalent; `RateLimiter` line 67 (prune on record), line 86 (future hits in retry-after, cannot be the minimum) — equivalent; line 73 (memory cap off by one) — Low; `RequestBodyLimitFilter` line 33 (`resetBuffer`) and `LimitedRequest` lines 66, 72 — Low (content of single-byte reads not asserted; `read > 0` boundary equivalent); `WorkshopCatalog` line 37 — equivalent (both paths reject); `Problems.write` lines 38, 41 (content type, charset) — Low. No coverage in PIT's unit-only run for controllers and the exception handler; they are covered by the acceptance and integration tests.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-07 | truncate the submission time to microseconds in `RegisterParticipant` (d160c6d); test 4789e31 | 15 targeted tests pass (`06_fix-F07.log`); runtime: POST response equals stored registration (`06_fix-F07-runtime.log`); final full run 190/190 |
| 2 | F-08 | assertions and boundary tests in `WebFiltersTest`, `RateLimiterTest` (26e5af7) | PIT 216/262 killed (`06_backend-pitest-after-F08.log`); final full run 190/190 |
| 3 | F-04 | tried `npm audit fix` | no fix within the pinned Stryker range; package files unchanged (`06_fix-F04-npm-audit-fix.log`); accepted |

No Critical or High finding needed a fix loop.

## Decisions

All records have a resolution or are pending review. D-16 and D-17 record the human's answers for F-09 and F-01.
