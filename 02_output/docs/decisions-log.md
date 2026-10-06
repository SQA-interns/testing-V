# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Docker Engine reports 29.8.1, pinned 29.8.0
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `docker` pins Docker Engine 29.8.0; `docker version` and `docker info` both report server 29.8.1 (client 29.8.1). The agent may not install or downgrade host software.
- Options: 1. (proposed default) Accept the installed 29.8.1 patch release as the `docker` platform for this run; nothing else changes. 2. The human installs Docker Engine 29.8.0 and the check is re-run.
- Human response: none
- Resolution: pending review

## D-02: cloc image tag 2.10 reports cloc 1.98
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: non-blocking
- Trigger: tooling `aldanial/cloc:2.10` pulls and runs, but `cloc --version` prints 1.98 (image built 2023-08-19). The preflight skill (step 2) and `tech-stack.md` rules make this non-blocking.
- Options: 1. (chosen) Keep the listed pin `aldanial/cloc:2.10` as authoritative and use it for code metrics. 2. Ask for a different image.
- Human response: none
- Resolution: option 1, pending review

## D-03: OSS Index analyser disabled in OWASP Dependency-Check
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: non-blocking
- Trigger: the Sonatype OSS Index analyser of `dependency-check-maven` needs credentials that `secrets.env.example` does not list (preflight step 6).
- Options: 1. (chosen) Run with `-DossindexAnalyzerEnabled=false`; the NVD, CISA KEV and other default analysers stay enabled. 2. Add OSS Index credentials (would change `secrets.env.example`, human only).
- Human response: none
- Resolution: option 1, pending review

## D-04: NVD API rejects NVD_API_KEY from .env
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: `dependency-check-maven` 12.1.0 fails with "Error updating the NVD Data" (log `out/logs/p0-backend-depcheck.log`). Second check: a direct request to the NVD CVE API 2.0 returns HTTP 404 with the key in the `apiKey` header and HTTP 200 without it, which is NVD's answer to an invalid key. The value is non-empty but is not in NVD's 36-character UUID key format (checked by length and pattern only; value never displayed). The backend dependency scan (preflight step 6) cannot complete.
- Options: 1. (proposed default) The human puts a valid NVD API key into `NVD_API_KEY` in `.env`; the agent re-runs the scan. 2. Run Dependency-Check without a key (allowed by NVD, but the first database download is heavily rate-limited and can take hours).
- Human response: none
- Resolution: pending review

## D-05: npm audit reports Critical and High in vitest 3.2.7 and jscpd 4.3.0
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` on the frontend dependency set (log `out/logs/p0-frontend-audit.json`) reports 2 Critical, 5 High and 4 Moderate. Critical: `vitest` 3.2.7 via `tinypool` <=2.1.0 (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr) and `@vitest/mocker` <4.1.11 (GHSA-82fw-gwwq-j7x9). High: `jscpd` 4.3.0 via `@jscpd/finder`, `fast-glob`, `micromatch` and `braces` <=3.0.3 (GHSA-vfj7-8cjw-p6xm). Moderate: `@vitest/coverage-v8` (via vitest), `qs` and `typed-rest-client`. All are dev and test tools; the runtime bundle only contains `react` and `react-dom`, which have no findings. Fixing them changes `tech-stack.md` pins, and accepting them would lower a Critical or High; both need the human.
- Options: 1. (proposed default) Replace `vitest` and `@vitest/coverage-v8` 3.2.7 with 4.1.11, and `jscpd` 4.3.0 with 5.4.0, all MIT. A scratch install with these versions gives 0 Critical, 0 High and 2 Moderate (`qs` via `typed-rest-client`); `vite` 6.4.3 meets the vitest 4.1.11 peer range, `@stryker-mutator/vitest-runner` 10.0.0 needs vitest >=2.0.0, and `tsc` and `vitest run` pass. 2. Same, but with vitest and coverage-v8 5.0.3 (a larger jump; needs node ^24, which is met). 3. Keep the pins and accept the findings as dev-only; this lowers a Critical or High, so it needs written human approval.
- Human response: none
- Resolution: pending review

## D-06: Playwright Chromium browser not installed on the host
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: tooling `@playwright/test` 1.63.0 runs ("Version 1.63.0"), but its bundled Chromium (chromium-1243) is not present in `%LOCALAPPDATA%\ms-playwright`. Downloading it is a host install, which needs human approval (`AGENTS.md`). End-to-end tests (phase 3) need it.
- Options: 1. (proposed default) Approve `npx playwright install chromium` from `02_output/frontend` (per-user download, no admin rights, no version change). 2. The human installs it. 3. Run Playwright in the `mcr.microsoft.com/playwright:v1.63.0` container (a new tool, recorded under the `tech-stack.md` rule).
- Human response: none
- Resolution: pending review

## D-07: ORGANIZER_USERNAME and SMTP_USERNAME values occur in repository paths
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: the secret-leak check (value search, values never displayed) finds the 5-character values of `ORGANIZER_USERNAME` and `SMTP_USERNAME` in 27 places. Every match is part of the run id or the repository path (for example in `docs/00_preflight-report.md`, `03_statistics/run-log.json` and the build logs), not a copy of `.env`. The phase 6 check (`general/skills/verify-release`) must find no secret value, and an organizer login equal to a public name is also easy to guess (SB-02).
- Options: 1. (proposed default) The human sets `ORGANIZER_USERNAME` (and `SMTP_USERNAME`, if it is used) in `.env` to a value that does not occur in the repository or its paths. 2. Keep the values and accept these matches as false positives in phase 6.
- Human response: none
- Resolution: pending review

## D-08: Human response to D-01 (Docker Engine version)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-01; the human answered.
- Options: as in D-01.
- Human response: "go with already installed" (2026-10-06T17:46:32Z)
- Resolution: D-01 option 1. Docker Engine 29.8.1 (installed) replaces the 29.8.0 pin of platform `docker` for this run; `tech-stack.md` is not edited.

## D-09: Human response to D-04 (NVD API key)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-04; the human answered.
- Options: as in D-04.
- Human response: "NVD API key is updated" (2026-10-06T17:46:32Z)
- Resolution: D-04 option 1. The human updated `NVD_API_KEY` in `.env`; the agent re-runs the NVD check and the backend dependency scan.

## D-10: Human response to D-05 (vulnerable dev tooling)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-05; the human answered.
- Options: as in D-05.
- Human response: "Move vitest and @vitest/coverage-v8 to 5.0.3." (2026-10-06T17:46:32Z)
- Resolution: D-05 option 2. `vitest` and `@vitest/coverage-v8` 5.0.3 replace the 3.2.7 pins. Option 2 also includes `jscpd` 5.4.0 in place of 4.3.0, which the answer did not name. The agent applies it because it is the only way to clear the High findings, and it is pending review. `tech-stack.md` is not edited.

## D-11: Human response to D-06 (Playwright Chromium)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-06; the human answered.
- Options: as in D-06.
- Human response: "go ahead and run npx playwright install chromium" (2026-10-06T17:46:32Z)
- Resolution: D-06 option 1. The agent runs `npx playwright install chromium` in `02_output/frontend`.

## D-12: Human response to D-07 (username values)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-07; the human answered.
- Options: as in D-07.
- Human response: "I have updated the credentials to be more secure" (2026-10-06T17:46:32Z)
- Resolution: D-07 option 1. The human changed the credentials in `.env`; the agent re-runs the presence check and the secret-leak value search.

## D-13: ORGANIZER_USERNAME still occurs in repository paths after D-12
- Timestamp: 2026-10-06T17:48:42Z
- Phase: 0
- Type: blocking
- Trigger: re-check after D-12. `SMTP_USERNAME` and all passwords now match nothing. `ORGANIZER_USERNAME` is still 5 characters and still occurs in the repository path and in the run id `Run ID` of `project/00_setup/run-config.md`, so it is found in 8 tracked files (value search; value never displayed).
- Options: 1. (proposed default) The human sets `ORGANIZER_USERNAME` in `.env` to a value that does not occur in the repository or its paths. 2. Keep it and accept these matches as false positives in the phase 6 secret check (the human accepts that the organizer login is guessable).
- Human response: none
- Resolution: pending review

## D-14: Dependency-Check reports CVE-2025-7962 (CVSS v3 7.5) on angus-activation 2.0.3
- Timestamp: 2026-10-06T17:48:42Z
- Phase: 0
- Type: blocking
- Trigger: backend scan with the updated key (log `out/logs/p0-backend-depcheck.log`, report `out/logs/p0-backend-depcheck-report.json`, 67 dependencies) fails the CVSS 7 threshold. Finding: `org.eclipse.angus:angus-activation:2.0.3` matched to CPE `eclipse:angus_mail:2.0.3` for CVE-2025-7962 (SMTP injection with CR/LF). Scores: CVSS v3 7.5 (High under the severity scale), v4 6.0; the tool's severity is MEDIUM. Evidence that it is a false positive: NVD lists as affected only `eclipse:jakarta_mail` < 1.6.8 or 2.0.0 to < 2.0.2, and `eclipse:angus_mail` < 2.0.4. The resolved mail artifacts are `jakarta.mail:jakarta.mail-api` 2.1.5 and `org.eclipse.angus:angus-mail` 2.0.5, both outside these ranges. `angus-activation` is the Jakarta Activation implementation, not Angus Mail; it matched only by vendor name and an equal version number. Lowering a High needs the human (`general/quality/severity-scale.md`). SR-05 (header injection) will still be enforced and tested in the application.
- Options: 1. (proposed default) Classify as a false positive (Low) and add a Dependency-Check suppression for CVE-2025-7962 on `angus-activation` only, with this record as the reason; `angus-mail` stays scanned. 2. Keep it as High and ask for a different dependency set (would change `tech-stack.md`).
- Human response: none
- Resolution: pending review

## D-15: Dependency-Check reports CVE-2025-15104 (Medium) on hibernate-validator 9.1.3.Final
- Timestamp: 2026-10-06T17:48:42Z
- Phase: 0
- Type: non-blocking
- Trigger: `hibernate-validator` 9.1.3.Final (managed by Spring Boot 4.1.1) matched CPE `validator:validator` for CVE-2025-15104, a flaw in the Nu Html Checker (validator.nu). Scores: CVSS v4 6.9, v3 5.3, so Medium. The product is unrelated to Hibernate Validator and is not used. A Medium does not block (`general/quality/severity-scale.md`).
- Options: 1. (chosen) Classify as a false positive (Low), record it here, and suppress it on `hibernate-validator` together with D-14 if D-14 option 1 is approved; otherwise list it in the phase 6 findings. 2. Leave it as Medium.
- Human response: none
- Resolution: option 1, pending review

## D-16: Human response to D-13 (organizer username)
- Timestamp: 2026-10-06T17:55:48Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-13; the human answered.
- Options: as in D-13.
- Human response: "keep the name as is" (2026-10-06T17:55:48Z)
- Resolution: D-13 option 2. `ORGANIZER_USERNAME` stays unchanged. In the phase 6 secret-leak check, matches of this value that are part of the repository path or the run id are classified as false positives with this record as the reason. Any other match is still a finding.

## D-17: Human response to D-14 (CVE-2025-7962 on angus-activation)
- Timestamp: 2026-10-06T17:55:48Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-14; the human answered.
- Options: as in D-14.
- Human response: "is false positive" (2026-10-06T17:55:48Z)
- Resolution: D-14 option 1. CVE-2025-7962 on `org.eclipse.angus:angus-activation` is classified Low (false positive) and suppressed for that artifact only in `backend/dependency-check-suppressions.xml`. CVE-2025-15104 on `hibernate-validator` is suppressed in the same file (D-15).

## D-18: US-001 AC4 "an invoice is issued to the payer" conflicts with AR-08
- Timestamp: 2026-10-06T17:59:35Z
- Phase: 1
- Type: blocking
- Trigger: US-001 AC4 (`project/01_requirements/user-stories.md`) says that after a completed registration "an invoice is issued to the payer". AR-08 (`project/02_design/architecture.md`) says invoicing is owned by the accounting team and invoices are produced by a separate process of the accounting system, which is not part of this repository; `environments.md` lists the accounting system with no local substitute and no interface. Phase 1 must adopt product-owner criteria without changing their meaning, so AC4 cannot be adopted as written without breaking AR-08, and no precedence rule resolves two `project/` files.
- Options: 1. (proposed default) This system does not issue invoices (AR-08). AC-001-04 is met when the payer's invoice data (private: participant name and e-mail; company: company name, address and VAT ID) and the amounts are stored and available to the accounting system through the organizer-only `GET /api/registrations/{registrationNumber}`; release notes list "invoice reaches the payer" for manual testing with accounting. 2. The backend produces and sends an invoice document to the payer itself (contradicts AR-08; needs invoice numbering and legal content that no input defines). 3. The backend notifies the accounting system per registration, for example by e-mail to a configured accounting address (needs a new setting and an interface that no input defines).
- Human response: none
- Resolution: pending review

## D-19: Meaning of "the fee is 240 EUR / 300 EUR" (gross, VAT split, rounding)
- Timestamp: 2026-10-06T17:59:35Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 AC1 and AC2 give one fee; the fixed API stores `netFee`, `vat` and `grossFee`, and `environments.md` sets `APP_FEE_EARLY` 240.00, `APP_FEE_REGULAR` 300.00 and `APP_VAT_RATE` 0.22. Whether the fee includes VAT is not stated; a participant notices the difference (240.00 or 292.80 to pay). No rule says whether VAT differs by payer type (for example reverse charge for foreign companies).
- Options: 1. (chosen, more conservative: the payer never pays more than the advertised fee, and prices advertised to private consumers include VAT) The configured fee is the gross amount: `grossFee` = fee, `netFee` = gross / (1 + rate) rounded half-up to cents, `vat` = gross − net (240.00 = 196.72 + 43.28; 300.00 = 245.90 + 54.10). The same VAT rate applies to every payer type. 2. The configured fee is net and VAT is added (240.00 + 52.80 = 292.80).
- Human response: none
- Resolution: option 1, pending review

## D-20: Workshops (count, validity, effect on fee)
- Timestamp: 2026-10-06T17:59:35Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 does not mention workshops, but the fixed API accepts `workshops` (array of ids) and stores a single `workshop` (id or null); `APP_WORKSHOPS` configures W1, W2 and W3. Not stated: whether a workshop is required, how many may be chosen, whether a workshop costs extra, and whether workshops have a capacity.
- Options: 1. (chosen, more conservative: nothing submitted is silently dropped, and no fee or limit is invented) A workshop is optional; at most one may be chosen; it must be a configured id; anything else is rejected (no silent truncation of the array); a workshop does not change the fee; there is no capacity limit. 2. Accept several workshops and store only the first (silently drops input). 3. Require a workshop.
- Human response: none
- Resolution: option 1, pending review

## D-21: Input validation rules for registration fields
- Timestamp: 2026-10-06T17:59:35Z
- Phase: 1
- Type: non-blocking
- Trigger: `business-rules.md` records no data rules. Not stated: which fields are required, whether company fields are required for company payers or allowed for private payers, the e-mail format, and field lengths (SB-01, SB-12).
- Options: 1. (chosen, more conservative) Required: `firstName`, `lastName`, `email` (syntactically valid), `payerType` (`private` or `company`). For `company`, `companyName`, `companyAddress` and `companyVatId` are required (needed for invoicing). For `private`, non-blank company fields are rejected rather than ignored (SB-12, and no accepted input is silently not stored). Maximum lengths: names 100, e-mail 254, company name 200, address 500, VAT ID 30 characters. No VAT ID format check (not defined). Every violation rejects the whole registration with a 4xx status. 2. Accept company fields for private payers and ignore them. 3. Check the VAT ID against a national format.
- Human response: none
- Resolution: option 1, pending review

## D-22: Confirmation e-mail cannot be sent
- Timestamp: 2026-10-06T17:59:35Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 AC3 requires a confirmation e-mail for every completed registration, but does not say what happens when the SMTP server does not accept the message.
- Options: 1. (chosen, more conservative: no participant believes they are registered without a confirmation, and no reservation exists that the participant was told failed) The e-mail is handed to the SMTP server before the registration is committed; if that fails, nothing is stored and the API answers with a 5xx status asking the participant to try again. 2. Store the registration, answer 2xx, and retry the e-mail later (needs an outbox; AC3 is not met at the end of the request). 3. Store the registration and answer with an error (the participant would register twice).
- Human response: none
- Resolution: option 1, pending review

## D-23: Duplicates, capacity and registration period
- Timestamp: 2026-10-06T17:59:35Z
- Phase: 1
- Type: non-blocking
- Trigger: not stated in US-001 or `scope.md`: whether the same e-mail address may register more than once, whether the conference has a capacity, and whether registration closes at some date.
- Options: 1. (chosen: no restriction that the inputs do not state; a duplicate check could lock out legitimate users, for example an assistant registering colleagues with one address) Duplicates are allowed and each gets its own registration number; no capacity; no closing date. 2. Reject a second registration with the same e-mail address. 3. Close registration at a configured date (needs a new setting).
- Human response: none
- Resolution: option 1, pending review

## D-24: Human response to D-18 to D-23 (agent to choose)
- Timestamp: 2026-10-06T18:02:22Z
- Phase: 1
- Type: blocking
- Trigger: follow-up to D-18 (blocking) and D-19 to D-23 (non-blocking); the human answered each.
- Options: as in D-18 to D-23.
- Human response: for each of D-18, D-19, D-20, D-21, D-22 and D-23: "No further information is available. Choose an option and record it as a decision." (2026-10-06T18:02:22Z)
- Resolution: the agent chooses option 1 of each record, the more conservative behaviour as `general/working-rules.md` requires. D-18: this system issues no invoice (AR-08); AC-001-04 stays as written (payer invoice data and amounts stored and available to accounting through the organizer API); "invoice reaches the payer" goes on the manual-test list in the release notes. D-19: the configured fee is gross, VAT is split out, rounding is half-up to cents. D-20: at most one configured workshop, optional, no effect on the fee, no capacity. D-21: validation rules as listed in D-21. D-22: no registration is stored when the e-mail cannot be sent; 5xx with a retry hint. D-23: duplicates allowed, no capacity, no closing date. These are the agent's choices, not the product owner's, so all six remain pending review in the phase 7 release notes. No acceptance criterion changes.

## D-25: Dev-only contract validation tools added
- Timestamp: 2026-10-06T18:06:20Z
- Phase: 2
- Type: non-blocking
- Trigger: the phase 2 gate requires the contracts to validate with a parser, and `tech-stack.md` lists no OpenAPI, YAML or JSON Schema parser. The `tech-stack.md` rules allow adding a dev-only tool with an exact version and a non-blocking record.
- Options: 1. (chosen) `02_output/tools/contract-check` with `@apidevtools/swagger-parser` 13.1.0 (MIT), `ajv` 8.20.0 (MIT), `ajv-formats` 3.0.1 (MIT) and `yaml` 2.9.1 (ISC), exact pins and a committed lock file; `npm audit`: 0 vulnerabilities. The SQL contract is validated by the pinned `postgres` image. Not part of any shipped component; scanned again in phase 6. 2. Check the contracts with JSON parsing only (does not validate OpenAPI semantics).
- Human response: none
- Resolution: option 1, pending review

## D-26: Configuration settings added or named in the design
- Timestamp: 2026-10-06T18:06:20Z
- Phase: 2
- Type: non-blocking
- Trigger: `environments.md` lists "Database URL and user", "SMTP host, port, TLS" and "Sender address" without variable names. SR-03 forbids organizer credentials over plain HTTP except on localhost, but in the local compose stack the backend sees the Docker bridge address, not a loopback address, although its port is bound to 127.0.0.1 only.
- Options: 1. (chosen) Use `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `APP_MAIL_TLS` (default `false`, required `true` in `prod`) and `APP_MAIL_FROM` (default `registration@confreg.local`). Add `APP_INSECURE_AUTH_ALLOWED` (default `false`; set to `true` only by the local compose file; the `prod` profile refuses to start with it), so credentials over plain HTTP are accepted only from loopback or in the localhost-bound local stack (spec 6.3). 2. Treat every non-TLS request in the local stack as allowed by profile, with no explicit setting (less visible). 3. Refuse organizer access in the local stack (DoD-P01 and the runtime demonstration could not read registrations).
- Human response: none
- Resolution: option 1, pending review

## D-27: Phase 2 contracts committed in one commit instead of one each
- Timestamp: 2026-10-06T18:07:06Z
- Phase: 2
- Type: non-blocking
- Trigger: a shell command error made the five contracts in `docs/02_contracts/` land in one commit (2f00194, together with D-25, D-26 and the contract-check logs) instead of one commit per contract (`general/phases.md`, commit units). The specification has its own commit (e81dfe4) and the tool too (9a6961c). `general/working-rules.md` forbids rewriting history.
- Options: 1. (chosen) Keep the history, record the deviation here, and list it in the phase 6 evidence. 2. Rewrite history (not allowed).
- Human response: none
- Resolution: option 1, pending review

## D-28: Frozen test AC-001-09 fails for input "@example.com" because of its e-mail check
- Timestamp: 2026-10-06T18:33:39Z
- Phase: 4
- Type: blocking
- Trigger: `ValidationAcceptanceTest.ac_001_09_invalidEmailRejected` parameter [3] (`@example.com`) fails at `AcceptanceTestBase.assertRejected`, assertion "e-mails sent" (log `out/logs/p4-backend-test-run1.log`). The backend did reject the registration: the 4xx status and the unchanged row count assertions passed. `assertRejected` then calls `messagesTo("@example.com")`, which runs the Mailpit search `to:"@example.com"`. That is a substring search and returns every confirmation sent to any `p-…@example.com` address in the run. The test is listed in `docs/03_acceptance-manifest.sha256`; only a human may change it.
- Options: 1. (proposed default) In `AcceptanceTestBase.messagesTo`, keep only messages whose `To` address equals the searched address (case-insensitive), then re-freeze the manifest. This makes the check exact without weakening it. 2. In `assertRejected`, skip the e-mail check when the submitted address is not a complete address. 3. Keep the test; AC-001-09 stays red.
- Human response: none
- Resolution: pending review

## D-29: Frozen test AC-001-18 never makes the SMTP server unreachable
- Timestamp: 2026-10-06T18:33:39Z
- Phase: 4
- Type: blocking
- Trigger: `MailFailureAcceptanceTest.ac_001_18_registrationNotStoredWhenEmailCannotBeSent` registers `spring.mail.host` and `spring.mail.port` (a closed port) in its own `@DynamicPropertySource`, but the base class registers the same keys for Mailpit, and the base class's values win. The backend therefore sent the e-mail to Mailpit, answered 201 and stored REG-000016 (Mailpit holds that confirmation). The test asserts a 5xx, so it fails without exercising the behaviour. Spec 3.1 (rollback and 503) is implemented in `RegistrationService`. The test is frozen.
- Options: 1. (proposed default) Remove the subclass `@DynamicPropertySource`. Instead, pause the shared Mailpit container (`docker pause` through the Testcontainers Docker client) around the request in `MailFailureAcceptanceTest`, and unpause it in `finally`. With the 5-second SMTP timeouts the send then fails, which tests the real "SMTP unavailable" case. Then re-freeze the manifest. 2. Make the base class read the mail host and port from an overridable static hook. 3. Keep the test; AC-001-18 stays red.
- Human response: none
- Resolution: pending review

## D-30: JDK build image added for the backend container
- Timestamp: 2026-10-06T18:33:39Z
- Phase: 4
- Type: non-blocking
- Trigger: `tech-stack.md` lists only the JRE image `eclipse-temurin:21.0.10_7-jre-alpine`. Building the jar inside `docker compose build` needs a JDK.
- Options: 1. (chosen) Use `eclipse-temurin:21.0.10_7-jdk-alpine` (same Temurin release and licence, GPL-2.0-with-classpath-exception, build stage only, not shipped) as the build stage of `backend/Dockerfile`; verified to pull and report 21.0.10+7. 2. Build the jar on the host and copy it in (`docker compose up` would not work from a clean checkout).
- Human response: none
- Resolution: option 1, pending review

## D-31: Host ports 8080 and 5173 are already in use on this machine
- Timestamp: 2026-10-06T18:33:39Z
- Phase: 4
- Type: blocking
- Trigger: `environments.md` puts the backend on port 8080 and Mailpit on 8025. On the host, the container `backtesting-tool` (not part of this project) publishes 0.0.0.0:8080, and another process answers on 127.0.0.1:5173, so `docker compose up` failed with "port is already allocated". The agent does not stop other projects' containers. The compose file now keeps the defaults (8080, 5173, 8025) but lets `BACKEND_HOST_PORT`, `FRONTEND_HOST_PORT` and `MAILPIT_HOST_PORT` move the host side. The stack ran healthy on 18080, 15173 and 18025, and the 4 end-to-end tests passed there (`out/logs/p4-frontend-e2e.log`).
- Options: 1. (proposed default) Keep the defaults; for this run's checks and the phase 6 runtime demonstration, use the override ports and record them in the evidence. 2. The human stops `backtesting-tool` (and the process on 5173) during the phase 6 demonstration, so the default ports are used.
- Human response: none
- Resolution: pending review

## D-32: Three phase 4 commit subjects exceed 72 characters
- Timestamp: 2026-10-06T18:33:39Z
- Phase: 4
- Type: non-blocking
- Trigger: `general/working-rules.md` limits commit subjects to 72 characters. Commits a0e8c3f (93), c2af805 (90) and f6204cb (84) are longer because they list every AC id. History may not be rewritten.
- Options: 1. (chosen) Keep them, record the deviation here for the phase 6 evidence, and keep later subjects within 72 characters (ranges such as AC-001-08..16). 2. Rewrite history (not allowed).
- Human response: none
- Resolution: option 1, pending review
