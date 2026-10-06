# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `tanej-confreg-C0-r1`, verified 2026-10-06 against the working tree at the phase 6 commits. Raw outputs are in `logs/06_*`. Requirements, acceptance criteria and specification were re-read first (`project/01_requirements/*`, `docs/01_*`, `docs/02_*`).

## Summary

- Full suite 178/178: backend 154 (44 frozen acceptance and architecture, 100 unit, 10 integration), frontend 22 (9 frozen acceptance, 13 unit), end-to-end 2/2 against the local stack.
- Both manifests match (25 input files, 18 frozen files). Inputs and protected files are unchanged since the starting commit `dc8b146`.
- No `.env` value appears in any file or commit message. Gitleaks found one ephemeral framework password in a phase 3 log; it was redacted (F-02).
- No open Critical or High finding. Eleven findings in total: 2 fixed, 9 accepted with reasons (see Findings).
- Decisions D-06 … D-16 are pending review by the product owner (non-blocking); all others are resolved.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 Full suite passes; frozen hashes match | pass | `logs/06_backend-full-run.log` (154/154), `logs/05_frontend-run2.log` (22/22), `logs/06_e2e-run.log` (2/2); `logs/06_integrity.log`: acceptance manifest 18 entries, 0 mismatches (one entry re-hashed with the human's authorization, D-18) |
| DoD-02 Format, lint, type check, static analysis clean | pass | backend `spotless:check pmd:check pmd:cpd-check spotbugs:check`: `logs/05_backend-check.log` (BUILD SUCCESS, 0 bug instances; 2 justified exclusions in `backend/spotbugs-exclude.xml`); frontend `prettier --check`, `eslint`, `tsc --noEmit`: `logs/05_frontend-check.log`; Semgrep `logs/06_semgrep-rerun.json`: 1 result, accepted (F-03) |
| DoD-03 Coverage and mutation recorded (thresholds: record only) | pass | see "Measures" below; `logs/06_coverage-summary.txt`, `logs/06_jacoco-*.csv`, `logs/06_pitest-mutations.csv`, `logs/06_frontend-*-coverage.log`, `logs/06_stryker*.log` |
| DoD-04 AR checked automatically; no dependency cycles | pass | `ArchitectureAcceptanceTest` ARCH-1 … ARCH-5 pass (AR-01 web endpoints only in `api`, AR-02 declared layers, AR-03 no slice cycles, AR-05 only the clock component reads the time, AR-07 only the mail component sends mail). AR-04: acceptance tests run with non-default business values and pass, so the values come from configuration. AR-06: `spring.jpa.hibernate.ddl-auto=validate`, only `db/migration/V1__create_registration.sql` (inspection). AR-08: no invoice is produced; export only (inspection, D-07). |
| DoD-05 No open Critical/High from review and scanners | pass | Findings table: none open at Critical/High; scanners `logs/06_dependency-check-report.json` (0 open), `logs/06_npm-audit.json` (0 Critical/High), `logs/06_semgrep-rerun.json`, `logs/06_gitleaks.json` / `06_gitleaks-dir-rerun.log` |
| DoD-06 Components run in their target environment; core flows work at runtime | pass | `logs/06_runtime-demo.log`, `logs/04_runtime-demo.log`, `logs/06_e2e-run.log` (see "Runtime demonstration") |
| DoD-07 Every AC traces to at least one test and one commit | pass | "Traceability" below |
| DoD-08 READMEs work from a clean checkout | pass (phase 7) | `logs/07_clean-clone.log`: fresh clone of `20b4dc5`, root quick start (compose up, health, page, Mailpit, organizer export), backend build and check, frontend `npm ci`, build, test 22/22, e2e 2/2; the frontend check failed only on README formatting, fixed in `1d09bff` and re-checked in the clone |
| DoD-09 Release notes list everything to test manually | pass (phase 7) | `docs/release-notes.md`, "Must be tested manually by a human": real SMTP delivery, HTTPS proxy, SR-03 over HTTPS, rate limit behind the proxy, production start guards, accounting import, production values, accessibility |
| DoD-10 Every decision resolved or pending review; inputs and protected files unchanged | pass | `docs/decisions-log.md`: D-01 … D-05, D-17, D-18 resolved; D-06 … D-16 "pending review"; `logs/06_integrity.log`: input manifest 25 entries, 0 mismatches, no git change under `01_input/` or protected files since `dc8b146` |
| DoD-11 Evidence checks of `general/phases.md` | pass | freeze commit `2c0b1ef` adds only `docs/03_acceptance-manifest.sha256`; all 18 listed files were committed earlier in phase 3; phase 3 commits (`6583da6` … `2c0b1ef`) touch no file under `backend/src/main` or `frontend/src`; build commits name their AC IDs (`0eab831`, `4ec19da`, `4b305a0`, `2e4d6d8`, `5de7eba`, `d81aa93`, `236c8bf`); commits over the size guide are listed with reasons in `docs/00_progress.md` |
| DoD-P01 A registration through the fixed API is accepted and stored on the running local stack | pass | `logs/04_runtime-demo.log` and `logs/06_runtime-demo.log`: `POST /api/registrations` 201, organizer `GET` 200 with the same data |

## Measures

| Measure | backend | frontend |
|---|---|---|
| Line coverage, unit tests only | 69.0 % (527/764) | 80.95 % (68/84) |
| Branch coverage, unit tests only | 84.1 % (212/252) | 71.9 % (41/57) |
| Line coverage, integration + acceptance only | 89.3 % (682/764) | 90.5 % (76/84), acceptance only |
| Branch coverage, integration + acceptance only | 67.9 % (171/252) | 82.5 % (47/57), acceptance only |
| Line / branch coverage, full suite | 96.9 % / 91.7 % | 95.2 % / 89.5 % |
| Mutation score (scope: validation, security, persistence, business rules) | PIT, unit tests only, classes in `application`, `api`, `domain`, `infrastructure`, `StartupGuard`: 374 mutants, 250 killed, 38 survived, 86 without unit coverage → 67 %; test strength 87 % (`logs/06_pitest.log`). After the added filter tests, `infrastructure.web` alone: 83 % (71 mutants). | Stryker, `src/api.ts` + `src/App.tsx`: not measurable (F-11): Stryker 10.0.0 with vitest 5.0.3 reports 24.5 % (`perTest`) and 2.9 % (`off`), yet both hand-made mutants of `api.ts` (`toFixed(2)`→`toFixed(3)`, `409`→`410`) are killed by the suite (1 and 2 failing tests), so the runner does not activate mutants; the reported values are recorded but not valid |
| Test counts (passed/failed) | acceptance 44/0, unit 100/0, integration 10/0 | acceptance 9/0, unit 13/0; e2e 2/0 |
| Production / test lines of code (cloc) | 1938 / 2411 | 533 / 564 |
| Duplication | CPD: 0 duplications | jscpd (src, tests, e2e): 1 clone, 9 lines, 0.73 % |

The backend mutants without unit coverage sit in classes exercised only through HTTP by the acceptance tests (controllers, `ApiExceptionHandler`, `ExportService`, `SmtpMailGateway`, `PayerTypeConverter`). PIT runs only the unit tests, because the acceptance tests start containers.

### Surviving mutants in security-, validation- and persistence-related code (classified individually)

| Class, line | Mutation | Classification | Reason |
|---|---|---|---|
| `RegistrationValidator` 109, 124, 156, 184, 189 | return "" instead of the value | Low, equivalent | the value is only returned on a path where an error was recorded, so the whole request is rejected and the value is never used |
| `RegistrationValidator` 129, 158 | `>` to `>=` on the e-mail / VAT ID length limit | Low, test gap, accepted | the boundary is covered by the frozen acceptance test AC-001-19 (`…AtTheirLimitAreAccepted`, 30-character VAT ID) through HTTP; PIT runs unit tests only |
| `RegistrationRequestMapper` 45, 56, 66 | `true` for a boolean / empty list for workshops | Low, test gap, accepted | killed by the acceptance tests (student flag AC-001-03, workshop AC-001-16) |
| `AppProperties.parseWorkshops` 67, 74, 75 | empty map for blank input; boundary of the `=` position | Low, equivalent | an `=` at position 0 gives an empty id, which is rejected either way |
| `TestClockFilter` 45, `RequestTimeSource` 26 | remove `setRequestNow` | Low, tool artefact | removing the call by hand makes `TestClockFilterTest.enabledClockUsesHeaderForThatRequestOnly` fail (checked 2026-10-06); PIT reports it as surviving, most likely because of the static `ThreadLocal` in its minion |
| `RateLimitFilter` 42, `SecureCredentialsFilter` 37 | remove `chain.doFilter` | fixed | new test `allowedRequestsArePassedOn` (commit `c81e36e`) |
| `BodySizeLimitFilter.LimitedStream` 63, 64, 66, 75 | byte-wise read not counted | fixed | new test `streamedBodyReadByteByByteIsAlsoLimited` (`c81e36e`) |
| `BodySizeLimitFilter.LimitedStream` 72, 87 | `n > 0` to `n >= 0`; `isFinished` true | Low, equivalent | counting 0 bytes changes nothing; `isFinished` is not used by the JSON reader |
| `RateLimiter` 63 | eviction when more than 100 000 windows are tracked | Low, test gap, accepted | memory guard only; never reached in tests |
| `ProblemWriter` 16, 19 | remove `setCharacterEncoding` / body write | Low | messages are ASCII constants; status and content type, which clients rely on, are asserted |
| `ConfirmationMailService` 90 | `sendOne` returns false for an already-sent registration | Low, equivalent in effect | only shortens a retry round; the next round sends the rest |
| `ConfirmationMessage` 19 … 33 | remove a line of the message body | Low, test gap, accepted | each required line content is asserted by the frozen e-mail acceptance tests AC-001-04/25/26 |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend + frontend + PostgreSQL + Mailpit | local (`docker compose --env-file ../.env up -d --build` in `02_output/`), all ports on 127.0.0.1 | containers healthy; backend runs as uid 10001, nginx as uid 101; readiness and liveness `UP` | pass (`logs/06_runtime-demo.log`) |
| frontend → backend (via nginx `/api` proxy) | local | page `GET /` 200; options for paying (regular 300.00 + 66.00 = 366.00) and student (0.00); paying private registration 201 `CR-000005`; student with workshop W2 201 `CR-000006` (0.00); same e-mail again 409; invalid body 400 with `firstName`, `email`, `payerType` errors; organizer export 200 (xlsx, 4112 bytes); export without credentials 401; one confirmation per registration in Mailpit | pass (`logs/06_runtime-demo.log`) |
| browser | local, Chromium (Playwright) | fill the form, choose a workshop, see the price, submit, see the registration number and total, confirmation e-mail received; workshops and unchecked student box shown | pass (`logs/06_e2e-run.log`, 2/2) |
| backend fixed API | local | `POST /api/registrations` with Slovenian characters 201; organizer `GET /api/registrations/CR-000002` 200 with identical data; 401 without credentials; security headers on frontend and backend | pass (`logs/04_runtime-demo.log`) |
| logs | local | backend container log searched for the e-mail domain, names and "Exception:" after the flows: 0 matches each | pass (SR-01) |

## Security baseline and project requirements

| Item | Implemented in | Checked by |
|---|---|---|
| SB-01 input validated server-side | `RegistrationValidator`, `RegistrationRequestMapper`, `AppProperties` | acceptance AC-001-11 … 19; `RegistrationValidatorTest`, `RegistrationRequestMapperTest`, `ApiProtectionIntegrationTest.wrongJsonTypesAreFieldErrors` |
| SB-02 authentication and authorization | `SecurityConfig` (public: registration, options, health; all else organizer) | AC-001-28 … 30; `ApiProtectionIntegrationTest.otherPathsNeedTheOrganizer` |
| SB-03 credentials hashed; secrets from configuration | BCrypt in `SecurityConfig.organizer`; secrets only from `.env`/environment; no default for secrets | inspection; secret-leak check `logs/06_integrity.log` |
| SB-04 TLS outside the local machine | external HTTPS proxy (environments.md); `StartupGuard` requires `APP_SMTP_TLS=true` in `prod` | `StartupGuardTest.sb04_productionRequiresSmtpTls`; manual test in production (release notes) |
| SB-05 output encoding, parameterised queries | Spring Data JPA (bound parameters, one native query without parameters); React escaping; plain-text mail; string cells in the export | inspection; Semgrep (no injection finding); `ConfirmationEmailAcceptanceTest.ac001_04_sr05_*` |
| SB-06 rate limiting | `RateLimiter`, `RateLimitFilter`, `AuthenticationFailureRecorder` | `RateLimiterTest`, `WebFiltersTest` (429 + `Retry-After`, separate buckets, failed logins) |
| SB-07 no internals in errors and logs | `ApiExceptionHandler`, `ProblemWriter`, `server.error.*` settings | `ApiProtectionIntegrationTest.malformedJsonIs400ProblemWithoutInternals`; container log check (runtime) |
| SB-08 dependencies scanned | OWASP Dependency-Check (test scope included), `npm audit` | `logs/06_dependency-check-report.json` (0 open, 49 suppressed with D-05 evidence), `logs/06_npm-audit.json` (0 Critical/High); F-04, F-09, F-10 |
| SB-09 static analysis and secret scan | SpotBugs, PMD, Semgrep, Gitleaks | `logs/05_backend-check.log`, `logs/06_semgrep-rerun.json`, `logs/06_gitleaks*.json` |
| SB-10 security headers | `SecurityConfig.headers`, `frontend/nginx.conf` | `ApiProtectionIntegrationTest.sb10_*`; runtime header check `logs/04_runtime-demo.log` |
| SB-11 least privilege | non-root users in both images; database user owns only its database | runtime `id -u` (`logs/06_runtime-demo.log`) |
| SB-12 minimal personal data | only the fixed API fields plus `student` (D-06) | inspection of `registration-storage.sql` |
| SB-13 purpose and retention | `security-requirements.md` table; retention defined by the organizer outside this project | inspection; listed in release notes |
| SB-14 consent never preselected | no consent is collected (contract processing); the only checkbox ("I am a student") is unchecked by default | AC-001-20 (frontend acceptance and e2e) |
| SR-01 no personal data in logs | logs only registration numbers and exception class names; no request or SQL logging; nginx `access_log off` | runtime container-log check (0 matches); inspection |
| SR-02 request body limit | `BodySizeLimitFilter` (16 KiB), nginx `client_max_body_size 16k` | `ApiProtectionIntegrationTest.sr02_*`, `WebFiltersTest` |
| SR-03 credentials only over HTTPS except localhost | `SecureCredentialsFilter` (D-16) | `WebFiltersTest` (403 for public plain HTTP, allowed for loopback/HTTPS/private outside `prod`) |
| SR-04 test clock never active in production | `StartupGuard` | `StartupGuardTest.sr04_*` |
| SR-05 safe e-mail content | plain text only, fixed subject, validated recipient, control characters rejected | `ConfirmationEmailAcceptanceTest.ac001_04_sr05_*`, `RegistrationValidatorTest.controlCharactersInNamesAreRejected` |

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `RegistrationAcceptanceTest.ac001_01_*` (3); frontend acceptance "AC-001-01 …" (2); e2e "AC-001-01 AC-001-04 …"; `RegistrationServiceTest` | tests `60af7c7`, `ced23c9`, `a88a2d8`; code `4b305a0`, `5de7eba`, `236c8bf` |
| AC-001-02 | `PricingAcceptanceTest.ac001_02_*` (2); `PricingServiceTest` | `60af7c7`; `4ec19da` |
| AC-001-03 | `PricingAcceptanceTest.ac001_03_*`; `PricingServiceTest.studentIsFreeWithTwoDecimals` | `60af7c7`; `4ec19da` |
| AC-001-04 | `ConfirmationEmailAcceptanceTest.ac001_04_*` (4); e2e; `ConfirmationMessageTest`, `ConfirmationMailServiceTest` | `6a9d815`, `a88a2d8`; `d81aa93` |
| AC-001-05 | `OrganizerAccessAcceptanceTest.ac001_05_*` (2) | `6a9d815`; `5de7eba` |
| AC-001-06, 07 | `PricingAcceptanceTest.ac001_06_*`, `ac001_07_*`; `PricingServiceTest.deadlineDayIsInclusiveInConferenceTimeZone` | `60af7c7`; `4ec19da` |
| AC-001-08 | `PricingAcceptanceTest.ac001_08_*` (2); `PricingServiceTest.vatIsRoundedHalfUpAndAmountsHaveScaleTwo` | `60af7c7`; `4ec19da` |
| AC-001-09, 10 | `PricingAcceptanceTest.ac001_09_*`, `ac001_10_*` | `60af7c7`; `4ec19da` |
| AC-001-11 … 15, 17, 19 | `ValidationAcceptanceTest.ac001_11_*` … `ac001_19_*` (12); `RegistrationValidatorTest` | `faaab86`, `3a87554` (D-18); `4b305a0` |
| AC-001-16 | `RegistrationAcceptanceTest.ac001_16_*` (2) | `60af7c7`; `4b305a0` |
| AC-001-18 | `RegistrationAcceptanceTest.ac001_18_*`; `RegistrationServiceTest.existingEmailIsDuplicate`, `…raceOnUniqueEmailIndexIsDuplicate` | `60af7c7`; `4b305a0` |
| AC-001-20 | frontend acceptance "AC-001-20 …"; e2e "AC-001-20 …" | `ced23c9`, `a88a2d8`; `4b305a0` (options endpoint), `236c8bf` |
| AC-001-21 | frontend acceptance "AC-001-21 …" (2) | `ced23c9`; `236c8bf` |
| AC-001-22 | frontend acceptance "AC-001-22 …"; `App.test.tsx` stale price | `ced23c9`, `6e4e173`; `4b305a0`, `236c8bf` |
| AC-001-23, 24 | frontend acceptance "AC-001-23 …", "AC-001-24 …" (2); `api.test.ts`, `App.test.tsx` | `ced23c9`, `6e4e173`; `236c8bf` |
| AC-001-25, 26 | `ConfirmationEmailAcceptanceTest.ac001_25_*`, `ac001_26_*`; `ConfirmationMessageTest` | `6a9d815`; `d81aa93` |
| AC-001-27 | `MailOutageAcceptanceTest.ac001_27_*`; `ConfirmationMailServiceTest` | `6a9d815`; `d81aa93` |
| AC-001-28 … 30 | `OrganizerAccessAcceptanceTest.ac001_28_*`, `ac001_29_*`, `ac001_30_*` | `6a9d815`; `5de7eba`, `ce11a93` |

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Medium | Semgrep `request-host-used` (WARNING) | `frontend/nginx.conf` forwarded the client-controlled `Host` header to the backend | fixed in `56ea728` (header no longer forwarded); re-verified: Semgrep re-run without this result, e2e 2/2 |
| F-02 | Low | Gitleaks `generic-api-key` | `logs/03_backend-acceptance-run.log` line 107 contained Spring's "Using generated security password", an ephemeral password of the bootstrap skeleton's default user from the phase 3 red run; never a `.env` value, never valid for any running system | redacted in `516768b`; Gitleaks working-tree re-run: no leaks. It remains in git history (history is not rewritten); accepted as it grants access to nothing |
| F-03 | Medium | Semgrep `npm-missing-minimum-release-age` | `frontend/.npmrc` sets no `min-release-age` | accepted: the option needs npm ≥ 11.10, while the human approved npm 10.9.4 (D-02); every npm version is pinned exactly with a committed lock file and the image build uses `npm ci`, so no newly published version is resolved |
| F-04 | Medium | `npm audit` | `qs` (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) via `typed-rest-client` via `@stryker-mutator/core` 10.0.0, rated Moderate | accepted: dev-only mutation tool, not in the production bundle (`npm audit --omit=dev`: 0) |
| F-05 | Low | Dependency-Check | the .NET assembly analyser cannot run (no `dotnet`) and reports an error for native `.dll` files inside Java jars | accepted: no .NET assemblies exist in this project; all Java analysers ran, 0 open results |
| F-06 | Low | PIT | surviving mutants in validation/security code, classified individually above | two test gaps fixed (`c81e36e`); the rest equivalent, covered by frozen acceptance tests, or a tool artefact |
| F-07 | Low | review | locally, every browser request reaches the backend from the nginx container's address, so all local browser clients share one rate-limit bucket (forwarded headers are trusted only in profile `prod`) | accepted: local only; in production the client address comes from the trusted proxy (D-16) |
| F-08 | Medium | review (SB-08) | container images (`postgres`, `eclipse-temurin` JRE/JDK, `node`, `nginx`, `mailpit`) are not scanned, because `tech-stack.md` lists no image scanner | accepted as open item for the release notes: image scanning to be added to the tooling; images are exact pinned tags from official sources |
| F-09 | Medium | Dependency-Check, D-05 (c) | test-scope `docker-java-transport-zerodep` 3.7.1 shades HttpComponents with CVE-2026-71290 (9.1), CVE-2026-54399/-54428 (7.5) | accepted by the human as Medium in D-05 (option 2): test scope only, local Docker daemon only, not in the boot jar, no fixed release; suppressed with this reason |
| F-10 | Low | Dependency-Check, D-05 (a), (b) | CPE false positives on `testcontainers-postgresql` (PostgreSQL server CVEs), `angus-activation` (angus_mail CVE fixed in 2.0.4) and `hibernate-validator` (validator.nu CVE) | accepted as false positives with evidence (D-05), suppressed in `backend/dependency-check-suppressions.xml` |
| F-11 | Medium | Stryker (DoD-03) | the frontend mutation score cannot be measured: `@stryker-mutator/vitest-runner` 10.0.0 does not activate mutants with vitest 5.0.3 (adopted in D-05); manual spot mutations are killed | accepted as open item: thresholds are "record only"; listed in the release notes; resolve with a runner version that supports vitest 5 (tech-stack change, human decision) |

No Critical or High finding is open. No Critical or High was downgraded in this phase; the downgrades in D-05 were decided by the human in phase 0.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | `56ea728`: remove `proxy_set_header Host $host` | stack rebuilt; e2e 2/2 (`logs/06_e2e-run.log`); Semgrep re-run: F-01 rule no longer reported (`logs/06_semgrep-rerun.json`) |
| 2 | F-02 | `516768b`: redact the ephemeral password in the phase 3 log | Gitleaks working-tree scan: no leaks (`logs/06_gitleaks-dir-rerun.log`) |
| 3 | F-06 (part) | `c81e36e`: tests for filter pass-through and byte-wise body limit | `WebFiltersTest` passes; PIT on `infrastructure.web`: 83 %, the targeted mutants killed |
