# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `kyuhi-confreg-C1-r1`, 2026-10-06. Raw output is in `out/logs/p6-*`. Ports for the local stack: 18080 (backend), 15173 (frontend), 18025 (Mailpit), by D-35, because 8080 and 5173 are taken on this host.

## Summary

| Item | Result |
|---|---|
| Input manifest (25 files) | all hashes match (`p6-manifests.log`) |
| Acceptance manifest (13 files) | all hashes match; re-frozen once under human approval D-33/D-34 (commit 6154891) |
| Tests, final run | backend 172/172 (64 acceptance, 108 unit, architecture and integration), frontend 27/27, end-to-end 4/4: **203 passed, 0 failed** |
| Checks | backend Spotless, PMD, CPD, SpotBugs: clean; frontend Prettier, ESLint, `tsc`: clean; contracts validate |
| Findings | Critical 0, High 0, Medium 3 (2 fixed, 1 accepted), Low 6 (3 fixed, 3 accepted) |
| Secret leaks | `.env` value search: no file, 0 commit messages; gitleaks working tree: none |

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | pass | `p6-final-backend-verify.log` (172/172), `p6-final-frontend-test.log` (27/27), end-to-end 4/4 in `p6-runtime-demo.log`; manifest hashes in `p6-manifests.log` |
| DoD-02 | pass | `p6-final-backend-check.log` (Spotless, PMD, CPD, SpotBugs: 0), `p6-final-frontend-check.log` (Prettier, ESLint, `tsc`: 0); semgrep after fixes: 0 results (`p6-semgrep-rerun.json`; one parser warning, F-07) |
| DoD-03 | pass (record only) | Coverage and mutation below; thresholds in `quality-requirements.md` are "record only" |
| DoD-04 | pass | `ArchitectureTest` ARCH-1 to ARCH-6 (layers, no cycles AR-03, clock AR-05, mail AR-07, configuration AR-04, repositories AR-06) pass in the final run |
| DoD-05 | pass | no open Critical or High; findings table below |
| DoD-06 | pass | runtime demonstration below (`p6-runtime-demo.log`) |
| DoD-07 | pass | traceability table below |
| DoD-08 | phase 7 | READMEs are phase 7 outputs; checked from a clean checkout in phase 7 |
| DoD-09 | phase 7 | `docs/release-notes.md` is a phase 7 output; manual-test items collected below |
| DoD-10 | pass | 36 decision records, each with a resolution or "pending review"; input manifest matches |
| DoD-11 | pass with deviations | see "Evidence checks" |
| DoD-P01 | pass | registration through the fixed API on the running stack: 201 and stored (REG-000012, read back by the organizer), `p6-runtime-demo.log` |

### Coverage and mutation (DoD-03)

| Component | Level | Line | Branch | Source |
|---|---|---|---|---|
| backend | unit (incl. architecture) | 81.3% (401/493) | 89.1% (156/175) | `p6-coverage-backend-unit.csv` |
| backend | acceptance + integration | 88.8% (438/493) | 66.3% (116/175) | `p6-coverage-backend-acceptance-integration.csv` |
| backend | all | 97.2% (479/493) | 90.3% (158/175) | `p6-coverage-backend-all.csv` |
| frontend | unit/component | 93.7% lines, 93.9% statements | 97.3% | `p6-frontend-coverage.log` |

| Component | Scope | Mutation score | Source |
|---|---|---|---|
| backend (PIT, unit tests) | domain, security, config, service, mail, time | 85% (175/206 killed; 14 no coverage are Spring wiring covered only by integration tests) | `p6-backend-mutation.log`, `p6-backend-mutations.csv` |
| frontend (Stryker, command runner, D-36) | all modules except `main.tsx` | 77.8% (238/306) | `p6-frontend-mutation-summary.txt` |

Surviving backend mutants in security, validation and persistence code, classified individually (severity scale rule):

| Mutant | Classification |
|---|---|
| `RegistrationValidator` L38: `length() <= EMAIL_MAX` changed to `<` | test gap: a 254-character malformed e-mail would pass the syntax check. Low; test added (F-04) |
| `AppProperties.parseWorkshops` L121 boundary on the id length | test gap at exactly 20 characters. Low; test added (F-04) |
| `ProblemResponses.write` L18/L21, `TestClockFilter` L39/L42: charset and body write removed | test gap: unit tests did not check the problem body. Low; assertions added (F-04) |
| `RateLimitFilter` L46/L47: clean-up threshold and stale-window predicate | equivalent for behaviour: only memory clean-up; limits are enforced by the window reset in `compute`. Low, accepted |
| `RegistrationService` L80: log text for a missing cause | log text only. Low, accepted |
| `RegistrationService.find` returns empty | covered by acceptance tests (outside the PIT run). Low, accepted |
| `ConfirmationMailer` L34: null check before catalogue lookup | equivalent: the lookup returns null for a null key. Low, accepted |

Frontend survivors (68) are in rendering details (CSS class names, `aria-live`, loading text, the effect clean-up flag in `App.tsx`). Validation on the frontend is a convenience only (the backend is authoritative, SB-01). Low, accepted.

### Evidence checks (`general/phases.md`, DoD-11)

| Check | Result |
|---|---|
| Phase 3 commits add no production code | pass: 99a176f, 220015c, 3781494 touch tests and docs only; 324408d also adds the runner configuration `frontend/playwright.config.ts` and the `e2e` entry in `frontend/tsconfig.json` (harness configuration, no behaviour) |
| Freeze commit adds only the manifest; every listed file committed earlier | pass: original freeze commit and the re-freeze 6154891 each change only `docs/03_acceptance-manifest.sha256` |
| At least one build commit per user story naming its ids | pass: 7 phase 4 commits name AC ids of US-001 (5a35560 to f6204cb, aba0b1b) |
| Commit size guide | deviations: 2f00194 (443 lines, D-27), 5a35560 (407 lines) and aba0b1b (537 lines) have no stated reason; 99a176f states one. Three subjects exceed 72 characters (D-32). F-09, Low |
| Manifest hashes | pass |

## Runtime demonstration

`docker compose --env-file ../.env up -d --build --wait` in `02_output` with `BACKEND_HOST_PORT=18080 FRONTEND_HOST_PORT=15173 MAILPIT_HOST_PORT=18025`, log `out/logs/p6-runtime-demo.log`.

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all four containers | local compose | start with health checks (NFR-02) | db, mailpit, backend, frontend healthy; backend runs as uid 10001, frontend as uid 101 (SB-11) |
| backend | local | `/actuator/health/liveness`, `/readiness` | `{"status":"UP"}` without details |
| frontend | local | `GET /`, `GET /config.json` | 200; workshops from `APP_WORKSHOPS`; CSP, `X-Frame-Options: DENY`, `nosniff`, `no-referrer` on both after F-02 |
| frontend → backend | local | company registration with Slovenian characters through the nginx `/api` proxy (UTF-8 body) | 201, REG-000012, 245.90 + 54.10 = 300.00 (regular fee, today is after the deadline), workshop W2 |
| backend | local | organizer `GET /api/registrations/REG-000012` with `.env` credentials; without credentials | 200 with names intact; 401 |
| Mailpit | local | confirmation e-mail | one message "Registration confirmation REG-000012" with number, fee and names intact (NFR-01) |
| backend | local | invalid registration | 400 |
| backend | local | `X-Test-Now` with the test clock disabled | header ignored, regular fee (SR-04) |
| frontend | local | Playwright end-to-end suite (4 tests) | 4 passed |
| backend, frontend | local | container logs after all flows | no e-mail address, name, company, VAT ID or `example.com` found (SR-01) |

One earlier request in the log answered 400 because Windows `curl` mangled non-ASCII command-line arguments; the backend logged no validation failure for it, and the same body sent from a UTF-8 file was accepted.

## Security evidence

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator`, JSON strictness (`fail-on-unknown-properties`), `BodySizeLimitFilter` | acceptance AC-001-08..16; `RegistrationValidatorTest`; `RegistrationIntegrationTest.malformedUnknownAndWrongTypeBodiesAreBadRequests` |
| SB-02 | `SecurityConfiguration` (POST public, everything else organizer) | `InvoiceDataAcceptanceTest` (401), `RegistrationIntegrationTest.organizerEndpointRequiresValidCredentials`, `otherEndpointsNeedOrganizer…` |
| SB-03 | `OrganizerAccount` (BCrypt at startup, min. 16 characters), secrets only from environment | `AppPropertiesTest.organizerAccountRequiresCredentials`; inspection of `application.yml` (no secret defaults) |
| SB-04 | production behind TLS proxy; `APP_MAIL_TLS` required in `prod`; datasource `sslmode=verify-full` documented | `AppPropertiesTest.productionRefusesUnsafeSettings`; TLS itself is a manual test (no local TLS) |
| SB-05 | Spring Data (parameterised), React escaping, plain-text e-mail | inspection; `RegistrationFormTest` "renders user input as text"; `ConfirmationMailerTest.headerInjectionInNameStaysInBody` |
| SB-06 | `RateLimitFilter` on all `/api/**` | `SecurityFiltersTest` rate limit tests; `LimitsIntegrationTest` (429 with `Retry-After`, also for authentication) |
| SB-07 | `ApiExceptionHandler`, `ProblemResponses`, `server.error.*` | `ApiExceptionHandlerTest.unexpectedErrorsRevealNoInternals`; integration body checks; runtime logs |
| SB-08 | Dependency-Check (OSS Index off, D-03), npm audit | `p6-backend-depcheck.log`: 67 deps, 0 open, 2 suppressed false positives (D-15, D-17); frontend runtime 0, dev tooling 2 Moderate (F-06) |
| SB-09 | semgrep, gitleaks, SpotBugs, PMD | `p6-semgrep*.json`, `p6-gitleaks*`, check logs |
| SB-10 | backend headers (`SecurityConfiguration`), nginx headers | `RegistrationIntegrationTest.securityHeadersOnApiResponses`; runtime `/` and `/config.json` headers |
| SB-11 | non-root users in both Dockerfiles | runtime `id -u`: 10001 and 101 |
| SB-12, SB-13 | only fixed-API fields stored (`registration-storage.sql`); purpose and retention in spec 4 | inspection |
| SB-14 | no consent needed (spec 4) | inspection |
| SR-01 | logging policy (spec 6.8): only field names, registration numbers, class names | `RegistrationIntegrationTest.personalDataNeverWrittenToLogs`; runtime container logs |
| SR-02 | `BodySizeLimitFilter` (16 KiB), nginx `client_max_body_size 16k` | `SecurityFiltersTest` body tests; `RegistrationIntegrationTest.oversizedBodyIsRefused` |
| SR-03 | `InsecureCredentialsFilter`, `APP_INSECURE_AUTH_ALLOWED` (D-26) | `SecurityFiltersTest` credentials tests; `AppPropertiesTest` (prod refuses) |
| SR-04 | `TestClockFilter` disabled by default; `ConfigurationSetup` refuses in `prod` | `AppClockTest`, `AppPropertiesTest.productionRefusesUnsafeSettings`, `LimitsIntegrationTest`, runtime |
| SR-05 | validator rejects control characters; mail headers built from fixed text and the validated address | `RegistrationValidatorTest.controlCharactersRejected…`, `ConfirmationMailerTest.headerInjection…` |
| NFR-01 | UTF-8 storage and e-mail | `RegistrationIntegrationTest.slovenianCharactersSurviveStorageAndEmail`; runtime |
| NFR-02 | actuator probes, container health checks | runtime (all healthy) |

## Traceability

Test names contain the AC id (`ac_001_NN_…` shown as `AC-001-NN …`). Implementation commits name their AC ids; ranges such as `AC-001-08..AC-001-16` cover every id in them.

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `FeeAcceptanceTest` ×3; `PricingPolicyTest` | tests 99a176f; code a0e8c3f |
| AC-001-02 | `FeeAcceptanceTest` ×3; `PricingPolicyTest` | tests 99a176f; code a0e8c3f |
| AC-001-03 | `ConfirmationEmailAcceptanceTest` ×2; e2e "AC-001-06 AC-001-03 …"; `ConfirmationMailerTest` | tests 99a176f, 324408d; code 38ec700 |
| AC-001-04 | `InvoiceDataAcceptanceTest` ×3 | tests 99a176f; code c2af805, f6204cb |
| AC-001-05 | `FeeAcceptanceTest` ×2; `PricingPolicyTest.splitRoundsNetHalfUp` | tests 99a176f; code a0e8c3f |
| AC-001-06 | `RegistrationResponseAcceptanceTest` ×3; e2e | tests 220015c, 324408d; code 1c0b284, c2af805, aba0b1b |
| AC-001-07 | `RegistrationResponseAcceptanceTest` ×1 | tests 220015c; code 1c0b284 |
| AC-001-08 | `ValidationAcceptanceTest` ×8 | tests 220015c; code ba39d0f |
| AC-001-09 | `ValidationAcceptanceTest` ×7 | tests 220015c, 756c37f (D-33); code ba39d0f |
| AC-001-10 | `ValidationAcceptanceTest` ×3 | tests 220015c; code ba39d0f |
| AC-001-11 | `ValidationAcceptanceTest` ×6; e2e | tests 220015c, 324408d; code ba39d0f, aba0b1b |
| AC-001-12 | `ValidationAcceptanceTest` ×4; e2e | tests 220015c, 324408d; code ba39d0f |
| AC-001-13 | `ValidationAcceptanceTest` ×11 | tests 220015c; code ba39d0f |
| AC-001-14 | `WorkshopAcceptanceTest` ×1; e2e | tests 220015c, 324408d; code ba39d0f, aba0b1b |
| AC-001-15 | `WorkshopAcceptanceTest` ×2 | tests 220015c; code ba39d0f |
| AC-001-16 | `WorkshopAcceptanceTest` ×3; `RegistrationValidatorTest.workshopRules` | tests 220015c; code ba39d0f, 6a874e4 |
| AC-001-17 | `FeeAcceptanceTest` ×1 | tests 99a176f; code a0e8c3f |
| AC-001-18 | `MailFailureAcceptanceTest` ×1; `RegistrationServiceTest.mailFailureBecomesNotCompleted` | tests 220015c, 756c37f (D-34); code c2af805 |

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | gitleaks (`generic-api-key`) | `logs/p3-backend-acceptance-run1.log` lines 119 and 356 contained the password Spring Security generated for the bootstrap skeleton during the phase 3 run. It was random, used only by a test application that no longer exists, and is not a `.env` value | fixed in the working tree (61db21e, gitleaks dir re-run: no leaks). The copy in git history stays because history may not be rewritten; accepted (cannot be used, the code now defines its own user store) |
| F-02 | Medium | semgrep `header-redefinition` (WARNING) | nginx `location = /config.json` used `add_header`, which dropped the server-level `X-Frame-Options` and `Referrer-Policy` for that response (SB-10) | fixed c4a7974; semgrep re-run 0 results; runtime headers present |
| F-03 | Medium | semgrep `request-host-used` (WARNING) | nginx forwarded the client `Host` header to the backend | fixed ad899c5 (default upstream host); semgrep re-run 0 results; e2e 4/4 through the proxy |
| F-04 | Low | PIT surviving mutants | unit-test gaps: e-mail syntax at exactly 254 characters, workshop id at 20 characters, problem bodies of filter responses | fixed 4d8d381 (tests added, 172/172 pass) |
| F-05 | Low | `.env` value check, step 8 | the archived gitleaks history report copied commit author e-mail addresses (personal data); one contains the `SMTP_USERNAME` value | fixed fe436b6 (author fields removed); step 8 re-run: no file, 0 commit messages |
| F-06 | Medium | npm audit | `qs` 6.15.1 (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) through `typed-rest-client` of `@stryker-mutator/core` (dev-only) | accepted: not in the runtime bundle (`npm audit --omit=dev`: 0); only Stryker's dashboard reporter uses it, which is not configured |
| F-07 | Low | semgrep parser error | semgrep could not parse `docs/02_contracts/registration-api.openapi.yaml` (non-ASCII example values) | accepted: the file validates with `@apidevtools/swagger-parser` and `yaml` (`p6-contract-check.log`); no code |
| F-08 | Low | tooling | Stryker's vitest runner does not activate mutants with vitest 5.0.3 | worked around with the command runner (D-36) |
| F-09 | Low | process evidence | three commits exceed the size guide without a stated reason (2f00194, 5a35560, aba0b1b); three subjects exceed 72 characters (D-32); contracts in one commit (D-27) | accepted and recorded; history may not be rewritten |

Dependency-Check false positives CVE-2025-7962 (angus-activation) and CVE-2025-15104 (hibernate-validator) are suppressed by D-17 and D-15 and not counted.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01, F-02, F-03, F-04, F-05 | redact log; nginx: no `add_header` in `/config.json`, no `Host` forwarding; unit tests for mutation gaps; author fields removed from gitleaks reports | semgrep 0 results; gitleaks working tree clean; `.env` check clean; backend 172/172, frontend 27/27, e2e 4/4; runtime headers verified |

No Critical or High finding was found, so no loop was required by the severity rule; the loop covers the Medium and Low fixes.

## Manual tests for the release notes

- Delivery of the confirmation e-mail to a real mailbox through the production SMTP server with STARTTLS (`environments.md`).
- TLS and the external nginx reverse proxy, including `X-Forwarded-Proto` and `X-Forwarded-For` handling (SR-03, SB-04, SB-06 per client).
- The invoice reaching the payer through the accounting system from the organizer API data (AC-001-04, D-18).
- Production start with `SPRING_PROFILES_ACTIVE=prod` refuses the test clock, plain SMTP and insecure authentication.
