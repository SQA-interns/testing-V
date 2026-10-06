# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `tanej-confreg-C2-r1`, verified 2026-10-06 (13:00–13:25 UTC) on the commit that adds this report. Requirements re-read first: `REQ-REG-01.md`, `docs/01_acceptance-criteria.md`, `docs/02_specification.md`, `docs/02_contracts/`.

**Status: gate passed** (2026-10-06T13:24:07Z). F-02 was High as mapped by the scale; the human lowered it to Low with the recorded evidence (D-18, 2026-10-06T13:24:07Z). No open Critical or High finding.

## Hashes

| Manifest | Files | Result | Method |
|---|---|---|---|
| `docs/00_input-manifest.sha256` | 26 | all match | recomputed, LF-normalised SHA-256 |
| `docs/03_acceptance-manifest.sha256` | 12 | all match | recomputed, paths relative to `02_output/` |
| protected files (`01_input/`, `AGENTS.md`, `README.md`, `03_statistics/metrics.md`, `run-log.template.json`, `usage.md`) | – | unchanged since starting commit `8b4fb64` | `git diff --stat 8b4fb64 HEAD` |

## Test runs and checks (phase 6)

| Command | Result | Log |
|---|---|---|
| backend `./mvnw clean verify` | 149 passed, 0 failed (acceptance 38, integration 11, unit 93, architecture 7) | `out/logs/06_backend-verify.log` |
| backend `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | clean | `out/logs/06_backend-check.log` |
| frontend `npm run check` (Prettier, ESLint, tsc) | clean | `out/logs/06_frontend-check.log` |
| frontend `npm run build` | built | `out/logs/06_frontend-build.log` |
| frontend `npx vitest run` | 26 passed, 0 failed (acceptance 6, unit 20) | `out/logs/06_frontend-test.log` |
| end-to-end `npx playwright test` against the rebuilt stack | 2 passed, 0 failed (also after the F-04/F-05 fixes) | `out/logs/06_e2e.log` |
| **total** | **177 passed, 0 failed** | |

Coverage and mutation (details and survivor classification: `docs/03_test-strategy.md`, "Measures"): backend 99.2 % lines / 95.9 % branches (all tests), frontend 95.9 % / 97.0 %; PIT 94 % (196 mutants, test strength 96 %); Stryker 82.7 % (249 mutants). Thresholds: record only.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | pass | 177/177 tests pass; both manifests match (sections above) |
| DoD-02 | pass | backend Spotless, PMD, CPD, SpotBugs clean (justified exclusions `backend/spotbugs-exclude.xml`); frontend Prettier, ESLint, `tsc --noEmit` clean; Semgrep: see F-02, F-03 |
| DoD-03 | pass | coverage per component, unit and integration separately, and mutation scores recorded in `docs/03_test-strategy.md`; thresholds "record only" |
| DoD-04 | pass | `ArchitectureTest` ARCH-1..6 (layers, no cycles, mail only in `mail`, time only via `ConferenceClock`, framework-free domain, no DDL from code): 7/7 pass |
| DoD-05 | pass | no open Critical/High: F-02 lowered to Low by the human with evidence (D-18); raw Semgrep reports kept |
| DoD-06 | pass | runtime demonstration below; stack started with `docker compose up --build`, all four containers healthy |
| DoD-07 | pass | traceability table below: every AC has tests and commits |
| DoD-08 | phase 7 | READMEs are written and followed from a clean checkout in phase 7 |
| DoD-09 | phase 7 | manual-test list goes into `docs/release-notes.md` in phase 7 (candidates: real SMTP delivery, TLS reverse proxy, D-12 failure follow-up) |
| DoD-10 | pass (with pending items) | every record D-01..D-18 has a resolution or "pending review" (D-10, 11, 12, 14, 15, 16, 17 non-blocking; D-18 blocking); input manifest matches |
| DoD-11 | pass, with F-09 | phase 3 commits add no production code (only tests, Playwright config, docs); freeze commit `f206f7b` adds only the manifest, all 12 listed files were committed before it; build commits name US-001 and AC ids; size guide: F-09 |
| DoD-P01 | pass | `POST /api/registrations` against the running local stack answered 201 and the registration was stored (runtime demonstration) |

## Runtime demonstration

Log: `out/logs/06_runtime-demo.log`, `out/logs/06_e2e.log`, `out/logs/06_f04-f05-verify.log`.

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local, `docker compose up --build` in `02_output/` (fresh volume) | containers start in dependency order on health checks | postgres, mailpit, backend, frontend healthy |
| backend | local | public `POST /api/registrations` with a company payer from Slovenia, Slovenian characters, workshop W2 (DoD-P01) | 201, `CR-2RMJX9N19J`, net 300.00, VAT 66.00, gross 366.00 (regular fee, real date after the deadline) |
| backend | local | organizer `GET /api/registrations/{n}` with credentials from `.env` (not shown) / without credentials | 200 with every field unchanged (`Žiga Čašič`, `Koroška cesta 1`) / 401 `{"status":401,"title":"Unauthorized"}` |
| backend + mail | local, Mailpit | confirmation e-mail | exactly 1 message, subject with the number, plain text with number, payer, workshop title, net, VAT (22 %), gross; Slovenian characters intact (NFR-01) |
| backend + storage | local | stored row | `confirmation_status=sent`, attempts 1, `submitted_at` in UTC (`+00`) |
| frontend → backend | local, nginx proxy on 127.0.0.1:8000 | invalid registration through `/api` | 422 with one error per field |
| frontend | local, Chromium | e2e AC-001-14/04 (form, company payer, first configured workshop, confirmation shown, one e-mail with the number and gross fee) and AC-001-15 (rejected e-mail shown at the field, no e-mail) | 2/2 pass |
| frontend | local | `/config.js` from `APP_WORKSHOPS`; security headers on `/` and `/config.js` | 3 configured workshops; CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy present on both; `Cache-Control: no-cache` on `/config.js` |
| backend | local | readiness on the management port inside the container; same port from the host | `{"status":"UP"}`; not reachable from the host (D-15) |
| all | local | container users | backend uid 100 (`app`), frontend `nginx` (SB-11) |

Observation: the first scripted POST sent non-UTF-8 bytes (Git Bash on Windows encodes command-line arguments in the console code page); the backend answered 400 "Failed to read request" without internals. Re-sent from a UTF-8 file; browsers send UTF-8 (e2e). Not a defect.

## Security baseline and project requirements

ASVS 5.0 Level 1 (both components). Implemented where (spec section) and checked how:

| ID | Implementation | Evidence |
|---|---|---|
| SB-01 | server-side `RegistrationValidator` (spec 4.2); frontend checks are usability only | `ValidationAcceptanceTest` (13), `RegistrationValidatorTest` |
| SB-02 | Spring Security: only `POST /api/registrations` public, all else organizer (spec 8.1) | AC-001-11 tests, `RegistrationIntegrationTest.otherPathsNeedTheOrganizer`, runtime 401 |
| SB-03 | organizer password BCrypt-hashed in memory, no default; secrets from `.env` only | `SecurityConfig`, `ConfigTest.organizerPasswordIsNotShownInText`, startup fails without credentials; leak check clean |
| SB-04 | TLS outside the local machine: credentials only over HTTPS/localhost; production refuses plain SMTP; external nginx TLS | `SecurityUnitTest.credentialsArePassedOnLocalhostOrHttpsOnly`, `ConfigTest.productionRefusesToStartWithoutSmtpTls`; TLS proxy is a manual test (release notes) |
| SB-05 | JPA bound parameters; React text rendering, no `dangerouslySetInnerHTML`; plain-text e-mail | inspection; Semgrep (no injection findings); `ConfirmationMailerTest.bodyHasNoMarkup` |
| SB-06 | sliding hourly limit per client for registrations and failed logins (spec 8.3) | `RateLimitAcceptanceTest`, `FailedLoginRateLimitIntegrationTest`, `SecurityUnitTest`, `SecurityResponseTest` |
| SB-07 | problem details without internals; `server.error.include-*=never` | `RegistrationIntegrationTest.errorsExposeNoInternals`; container logs scanned (no stack traces) |
| SB-08 | dependency scans | OWASP Dependency-Check (NVD): 0 Critical/High (66 jars; one suppressed false positive D-08, one Medium false positive F-06); `npm audit`: 0 Critical/High (F-07); production frontend dependencies: 0 |
| SB-09 | static analysis and secret scan | Semgrep 1.177.0 (`p/default`, `p/java`, `p/typescript`, `p/react`, `p/secrets`): F-02..F-05; gitleaks v8.30.1 history and tree: no leaks after F-01 |
| SB-10 | backend: CSP `default-src 'none'`, X-Frame-Options DENY, nosniff, no-referrer; frontend nginx: CSP, X-Frame-Options, nosniff, no-referrer | `RegistrationIntegrationTest.apiResponsesCarrySecurityHeaders`; runtime header check (F-05 re-verification) |
| SB-11 | non-root backend user, unprivileged nginx on 8080; ports bound to 127.0.0.1; management port not published | runtime check (uid 100, `nginx`), `docker-compose.yml` |
| SB-12 | only AC-001-07 fields plus workshop stored; IPs only in memory ≤ 1 h | storage contract, `RegistrationEntity`; inspection |
| SB-13 | purpose and retention per item (spec 9, `security-requirements.md`) | spec section 9 |
| SB-14 | no consent required (OQ-04); nothing preselected | spec section 9; form contract (no consent field) |
| SR-01 | no personal data in logs: registration number and exception class only | `RegistrationIntegrationTest` (captured output has no e-mail, names, address, VAT ID); container logs of the whole phase 6 run: 0 hits for fixture values |
| SR-02 | 16 KiB body limit (backend filter, nginx `client_max_body_size 16k`) | `RegistrationIntegrationTest.bodyOver16KibAnswers413`, `SecurityUnitTest`, `SecurityResponseTest` |
| SR-03 | credentials only over HTTPS or localhost | `CredentialTransportFilter`; unit tests as SB-04 |
| SR-04 | `StartupGuard` refuses `prod` with the test clock | `ConfigTest.productionRefusesToStartWithTestClock` |
| SR-05 | plain-text mail, user input only in the body and as the validated single recipient; CR/LF rejected | `ValidationAcceptanceTest.ac_001_07_emailWithLineBreakIsRejected`, `ConfirmationMailerTest` |

Secret-leak check (`general/skills/verify-release` step 8, run as written): files listed: none; commit messages: `0`.

## Scanners (raw output in `out/logs/`)

| Tool | Scope | Result | Log |
|---|---|---|---|
| OWASP Dependency-Check 12.1.0 (NVD; OSS Index disabled, D-07) | backend, 66 dependencies | 0 Critical/High; CVE-2025-7962 suppressed (D-08); CVE-2025-15104 Medium (F-06) | `06_owasp.log`, `06_owasp-report.json` |
| npm audit (npm 10.9.4, D-02) | frontend lock file | 0 Critical/High; 2 Moderate (F-07); `--omit=dev`: 0 | `06_npm-audit.json` |
| Semgrep 1.177.0 | `02_output/` without build output and logs, 86 files | 11 results first run, 2 after fixes (F-02, F-03); 1 parse warning on the vendored `mvnw` (F-08) | `06_semgrep.json`, `06_semgrep-after-fixes.json` |
| gitleaks v8.30.1 | git history and working tree | 4 hits (F-01), then no leaks | `06_gitleaks-git.log`, `06_gitleaks-dir.log` |
| cloc 2.10 (reports 1.98, D-05) | code metrics | backend 1365 production / 2276 test lines; frontend 407 / 507 | run log `codeMetrics` |
| CPD / jscpd | duplication | backend 0 duplications (50 tokens); frontend 2.9 % (1 clone, Low) | `06_jscpd/` |

Not scanned: the container images themselves (no image scanner in `tooling`); listed in the release notes.

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `FeeAcceptanceTest.ac_001_01_*` (2) | 1c4ad5d (AC), ec07275 (tests), 451da1e, 601f6b4 |
| AC-001-02 | `FeeAcceptanceTest.ac_001_02_*` | 1c4ad5d, ec07275, 451da1e, 601f6b4 |
| AC-001-03 | `FeeAcceptanceTest.ac_001_03_*` (2); `FeeCalculatorTest` | 1c4ad5d, ec07275, f989292, 451da1e, 601f6b4 |
| AC-001-04 | `ConfirmationAcceptanceTest.ac_001_04_*` (3); e2e "AC-001-14 AC-001-04"; `ConfirmationDispatcherTest`, `ConfirmationMailerTest` | 1c4ad5d, ec07275, 2fa97ef, 601f6b4, e3b8f7d |
| AC-001-05 | `PayerDataAcceptanceTest.ac_001_05_*` (3) | 1c4ad5d, ec07275, ef08e5c, 601f6b4 |
| AC-001-06 | `FeeAcceptanceTest.ac_001_06_*` (2) | 1c4ad5d, ec07275, 451da1e, 601f6b4 |
| AC-001-07 | `ValidationAcceptanceTest.ac_001_07_*` (13); `RegistrationValidatorTest` | 1c4ad5d, ec07275, 451da1e, 601f6b4 |
| AC-001-08 | `WorkshopAcceptanceTest.ac_001_08_*` (4) | 1c4ad5d, ec07275, ef08e5c, 601f6b4 |
| AC-001-09 | `RepeatedRegistrationAcceptanceTest.ac_001_09_*` (2) | 1c4ad5d, ec07275, ef08e5c, 601f6b4 |
| AC-001-10 | `OrganizerAccessAcceptanceTest.ac_001_10_*` | 1c4ad5d, ec07275, 601f6b4 |
| AC-001-11 | `OrganizerAccessAcceptanceTest.ac_001_11_*` (3) | 1c4ad5d, ec07275, 601f6b4 |
| AC-001-12 | `OrganizerAccessAcceptanceTest.ac_001_12_*` | 1c4ad5d, ec07275, 601f6b4 |
| AC-001-13 | `RateLimitAcceptanceTest.ac_001_13_*`; `SecurityUnitTest` | 1c4ad5d, ec07275, 4f42b60 |
| AC-001-14 | `registrationForm.test.tsx` "AC-001-14" (4); e2e "AC-001-14 AC-001-04" | 1c4ad5d, fec99a1, 2fa97ef, 1d0fd49, a803985 |
| AC-001-15 | `registrationForm.test.tsx` "AC-001-15" (2); e2e "AC-001-15" | 1c4ad5d, fec99a1, 2fa97ef, 1d0fd49 |

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | gitleaks (generic-api-key) | 4 hits in committed phase 3 logs: Spring Boot's auto-generated development password printed by the bootstrap app before the organizer security existed; random per test JVM, already gone, not a project secret or `.env` value | fixed: exact fingerprints ignored with the reason in `02_output/.gitleaksignore` (history is not rewritten); re-scan clean (d078f2d) |
| F-02 | High (Semgrep ERROR) → Low (D-18) | Semgrep `use-of-basic-authentication` | organizer endpoint uses HTTP Basic (`registration-api.openapi.yaml:117`) | accepted by the human (2026-10-06T13:24:07Z): Basic is required by the fixed API and `security-requirements.md`; credentials only over HTTPS or localhost (SR-03), BCrypt-only storage, tested per-client rate limit on failed logins; raw reports `06_semgrep.json`, `06_semgrep-after-fixes.json` |
| F-03 | Medium | Semgrep `npm-missing-minimum-release-age` | `.npmrc` has no `min-release-age` | accepted: the option needs npm ≥ 11.10; the approved npm is 10.9.4 (D-02) and the listed 11.6.2 also lacks it; mitigated by exact pins, committed lock file and `npm ci` in the image build (ES-04) |
| F-04 | Medium | Semgrep `ifs-tampering` (4) | `40-runtime-config.sh` changed `IFS` to split `APP_WORKSHOPS` | fixed: here-document with `tr` instead of `IFS` (7d6a3ad); re-verified by script test with quotes and `<script>`, runtime `/config.js`, e2e, Semgrep |
| F-05 | Medium | Semgrep `header-redefinition` (5) | `add_header` in `location = /config.js` replaced the server-level headers (they were repeated by hand) | fixed: `expires -1` instead of `add_header`, server headers inherited (42c4194); re-verified headers on `/` and `/config.js`, e2e, Semgrep |
| F-06 | Medium as reported, Low as classified | OWASP Dependency-Check | CVE-2025-15104 (Nu Html Checker) matched to `hibernate-validator` 9.1.3.Final by CPE name | accepted as false positive: the CVE concerns validator.nu, a different product, not used |
| F-07 | Medium | npm audit | `qs` (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) via `typed-rest-client`, dev-only (Stryker) | accepted: dev tooling, not shipped (`npm audit --omit=dev`: 0); no reachable path from the application |
| F-08 | Low | Semgrep | partial parse of the vendored Maven wrapper script `backend/mvnw` | accepted: third-party script from `maven-wrapper-distribution` 3.3.2 (SHA-1 verified in phase 0), not project code |
| F-09 | Low | DoD-11 commit size | 4 commits exceed ~400 changed lines without a stated reason: 75812bf (673, mostly the vendored wrapper scripts), 1d0fd49 (467, form page), 8ec415d (432) and 23aa290 (434, unit tests); ec07275 states its reason | accepted: each is one logical unit; history is not rewritten |
| F-10 | Low | frontend mutation tool | Stryker vitest runner reports no kills with vitest 5.0.3 | resolved by D-17 (command runner) |

Counts as found: Critical 0, High 1, Medium 5, Low 4. After triage: no open Critical or High.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | `02_output/.gitleaksignore` with the 4 fingerprints and the reason | gitleaks git and dir: no leaks (`06_gitleaks-*.log`) |
| 2 | F-04, F-05 | runtime-config script without `IFS`; nginx `expires -1` for `/config.js` | script test in `nginx:1.30.5-alpine`, rebuilt frontend healthy, headers and `/config.js` checked, e2e 2/2, Semgrep 11 → 2 results (`06_f04-f05-verify.log`, `06_semgrep-after-fixes.json`) |

## Decisions

All 18 records have a resolution or "pending review": resolved D-01..D-09, D-13, D-18; pending review (non-blocking) D-10, D-11, D-12, D-14, D-15, D-16, D-17.
