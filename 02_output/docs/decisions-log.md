# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Docker Engine reports 29.8.1, pin is 29.8.0
- Timestamp: 2026-10-06T22:31:30Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker version` reports server and client 29.8.1; `tech-stack.md` platform `docker` pins 29.8.0. Docker Compose v5.5.1 matches. Engine runs and pulls every pinned image.
- Options: 1. continue on 29.8.1, the pin stays authoritative (proposed); 2. ask the human to install 29.8.0.
- Human response: none
- Resolution: pending review (option 1)

## D-02: cloc image tag 2.10 contains cloc 1.98
- Timestamp: 2026-10-06T22:31:30Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run --rm aldanial/cloc:2.10 --version` prints 1.98 (image created 2023-08-19); a second check (`sh -c 'cloc --version'`) has no other binary. Tag resolves and the tool runs (`tech-stack.md` tooling `aldanial/cloc`).
- Options: 1. keep the pinned tag 2.10 and record the reported version 1.98 in code metrics (proposed); 2. change the tag.
- Human response: none
- Resolution: pending review (option 1)

## D-03: Dependency-Check analysers disabled
- Timestamp: 2026-10-06T22:31:30Z
- Phase: 0
- Type: non-blocking
- Trigger: OWASP Dependency-Check 12.1.0 (`tech-stack.md` tooling) has the Sonatype OSS Index analyser on by default; it needs OSS Index credentials that `secrets.env.example` does not list. The RetireJS, Node package and Node audit analysers would duplicate `npm audit` and are out of the backend's scope. All four are disabled in `02_output/backend/pom.xml`; the scan completes with the NVD CVE, CPE, Jar and Known Exploited Vulnerability analysers.
- Options: 1. keep them disabled (proposed); 2. add OSS Index credentials to `secrets.env.example` (human change).
- Human response: none
- Resolution: pending review (option 1)

## D-04: CVE-2025-7962 (CVSS 3.1 7.5) matched to angus-activation 2.0.3
- Timestamp: 2026-10-06T22:31:30Z
- Phase: 0
- Type: blocking
- Trigger: Dependency-Check maps `angus-activation-2.0.3.jar` (transitive from `spring-boot-starter-mail` 4.1.1) to CPE `eclipse:angus_mail:2.0.3`. The CVE (SMTP injection) affects `angus_mail < 2.0.4` and `jakarta_mail < 2.0.2`. Resolved versions (`02_output/logs/00_backend-deps.log`): `angus-mail` 2.0.5, `jakarta.mail-api` 2.1.5, neither flagged. CVSS 3.1 7.5 = High by the severity scale; the tool's own severity field says MEDIUM (CVSS 4.0 6.0). Treated as High. Lowering it needs this decision (`general/quality/severity-scale.md`).
- Options: 1. classify as false positive (Low), add a documented suppression for this CVE on `pkg:maven/org.eclipse.angus/angus-activation@.*` only (proposed); 2. keep as High and change the mail dependency set (`tech-stack.md` change).
- Human response: option 1, 2026-10-06T22:43Z: treat as false positive (Low); the CVE is in the mail library before 2.0.4 and the shipped angus-mail is 2.0.5; suppress for that one artifact only.
- Resolution: option 1; suppression in `02_output/backend/dependency-check-suppressions.xml` (CVE-2025-7962, `pkg:maven/org.eclipse.angus/angus-activation@.*` only); re-scan shows it suppressed and no CVSS ≥ 7 result.

## D-05: npm audit reports Critical and High in frontend dev tooling
- Timestamp: 2026-10-06T22:31:30Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` (`02_output/logs/00_frontend-npm-audit.json`): 2 Critical (`vitest` 3.2.7 via `tinypool` 1.1.1, GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr), 5 High (`jscpd` 4.3.0 via `@jscpd/finder` → `fast-glob` → `micromatch` → `braces` 3.0.3, GHSA-vfj7-8cjw-p6xm), 4 Moderate (`@vitest/mocker` GHSA-82fw-gwwq-j7x9; `qs` via `@stryker-mutator/core` → `typed-rest-client`). Every finding is in a devDependency; `npm audit --omit=dev` finds 0 (runtime: `react`, `react-dom`). Fixes need major versions: `vitest` and `@vitest/coverage-v8` 5.0.3, `jscpd` 5.4.0 (`tech-stack.md` change).
- Options: 1. accept for this run: dev-only tools that run locally on the project's own files and never ship in the nginx image; re-scan in phase 6 and report runtime results separately (proposed); 2. approve `vitest`/`@vitest/coverage-v8` 5.0.3 and `jscpd` 5.4.0 (compatibility with `@stryker-mutator/vitest-runner` 10.0.0 to be verified); 3. other versions named by the human.
- Human response: option 2, 2026-10-06T22:43Z: move `jscpd` to 5.4.0; move `vitest` and `@vitest/coverage-v8` to 5.0.3. Replaces those three `tech-stack.md` entries.
- Resolution: option 2; peer dependencies checked (vitest 5.0.3 accepts vite 6.4.3; `@stryker-mutator/vitest-runner` 10.0.0 accepts vitest ≥ 2); `npm run check`, `build`, `test` pass; `npm audit`: 0 Critical, 0 High, 2 Moderate (`qs` via `@stryker-mutator/core`, dev-only).

## D-06: Conflict on how to handle implementation gaps
- Timestamp: 2026-10-06T22:46:04Z
- Phase: 1
- Type: non-blocking
- Trigger: `REQ-REG-01.md` ("Open questions", last paragraph) says any decision not derivable from the inputs is a blocking decision and is not implemented until answered. `general/working-rules.md` ("Decide and record") says that when a requirement allows two behaviours a user would notice, the agent chooses the more conservative one, records it as pending review and continues. The project file has no `Overrides:` line, so by the precedence in `AGENTS.md` the general rule applies.
- Options: 1. apply `working-rules.md`: conservative choice, pending review, continue (proposed); 2. treat every gap as blocking.
- Human response: none
- Resolution: pending review (option 1). D-07, D-08, D-09 follow this rule; any of them can be reversed before phase 3 freezes the tests.

## D-07: Behaviour of the registration form page (AC-001-11)
- Timestamp: 2026-10-06T22:46:04Z
- Phase: 1
- Type: non-blocking
- Trigger: AR-01 requires a single registration form page; REQ-REG-01 does not describe what the page shows.
- Options: 1. one page; company fields shown only for payer type "company" and not sent for "private"; workshop chosen from the configured list (none or one); on 201 show registration number, net fee, VAT, gross fee; on 422 show each field's error next to it and keep the input (proposed); 2. show only a generic success or error message.
- Human response: none
- Resolution: pending review (option 1)

## D-08: What counts as an invalid field (AC-001-07)
- Timestamp: 2026-10-06T22:46:04Z
- Phase: 1
- Type: non-blocking
- Trigger: REQ-REG-01 AC7 lists the required fields but not what makes a value invalid; AC5 says a private payer is stored without company data but not what happens to company fields sent with `payerType: private`.
- Options: 1. trimmed non-blank values; length limits name 100, e-mail 254, company name 200, address 300, VAT ID 32; e-mail `local@domain` with a dot in the domain; no control characters (SR-05); no country-specific VAT ID check (would reject legitimate EU payers); workshop id must be in `APP_WORKSHOPS`, at most one element; company fields sent with a private payer are ignored and not stored (SB-12) (proposed); 2. reject company fields sent with a private payer with 422; 3. validate VAT IDs per country format.
- Human response: none
- Resolution: pending review (option 1)

## D-09: Confirmation e-mail cannot be sent (AC-001-04)
- Timestamp: 2026-10-06T22:46:04Z
- Phase: 1
- Type: non-blocking
- Trigger: REQ-REG-01 AC4 requires exactly one confirmation e-mail but does not say what happens when the SMTP server fails.
- Options: 1. the registration stays stored and the API still returns 201 (the participant must not lose a valid registration or be pushed to register twice); the failure is logged with the registration number only, no personal data (SR-01); no automatic resend (proposed); 2. roll back the registration and return an error; 3. store and queue the e-mail for retry.
- Human response: none
- Resolution: pending review (option 1); the release notes will list it for manual review.

## D-10: Public read-only endpoint for the workshop list
- Timestamp: 2026-10-06T22:51:13Z
- Phase: 2
- Type: blocking
- Trigger: the form (AR-01, AC-001-11) must offer the workshops configured in `APP_WORKSHOPS` (AR-04: read where used). `security-requirements.md` says every endpoint requires organizer authentication unless a requirement explicitly makes it public; REQ-REG-01 makes only `POST /api/registrations` public. Making another endpoint public changes a project security requirement.
- Options: 1. add `GET /api/workshops`, public, read-only, returns only workshop ids and titles (no personal data), rate limited per client like registrations (proposed; in `docs/02_specification.md` 5.3 and the OpenAPI contract); 2. no new endpoint: the frontend container reads `APP_WORKSHOPS` at start-up and writes it into a static `config.js` served by nginx (the same variable is then parsed in two components); 3. another approach named by the human.
- Human response: option 1, 2026-10-06T22:56:01Z: go with the proposed solution.
- Resolution: option 1; `GET /api/workshops` public, read-only, ids and titles only, rate limited (`docs/02_specification.md` 5.3).

## D-11: Health endpoints on an unpublished management port
- Timestamp: 2026-10-06T22:51:13Z
- Phase: 2
- Type: non-blocking
- Trigger: ES-09 and NFR-02 need health and readiness for container health checks; `security-requirements.md` requires authentication on every endpoint not made public by a requirement.
- Options: 1. Actuator on management port 8081, never published by compose, only `health` (liveness, readiness) exposed, no details, no authentication (reachable only inside the container network) (proposed); 2. health under `/api` behind organizer authentication (health checks would need the organizer password in the container).
- Human response: none
- Resolution: pending review (option 1)

## D-12: OpenAPI validator added as dev-only tool
- Timestamp: 2026-10-06T22:51:13Z
- Phase: 2
- Type: non-blocking
- Trigger: the phase 2 gate needs the contracts validated with a parser; `tech-stack.md` lists no OpenAPI parser.
- Options: 1. `redocly/cli` container image, version 2.57.0, MIT licence, run with `docker run` (nothing installed on the host), dev-only (proposed); 2. no OpenAPI-specific validation (YAML parse only).
- Human response: none
- Resolution: pending review (option 1); JSON contracts are checked with `JSON.parse` (node), the SQL contract by applying it in `postgres` 16.15.

## D-13: Plain-HTTP credentials in the local and test profiles (SR-03)
- Timestamp: 2026-10-06T22:51:13Z
- Phase: 2
- Type: non-blocking
- Trigger: SR-03 allows organizer credentials over plain HTTP only on localhost. In the local Docker stack (bound to 127.0.0.1 only, `environments.md`) the backend sees requests from the Docker bridge gateway, not a loopback address, so a pure address check would block the organizer locally.
- Options: 1. the check is strict by default and in production (secure request or loopback client); profiles `local` and `test` skip it because those environments listen on 127.0.0.1 only (proposed); 2. strict everywhere, organizer reads only from inside the container network in the local stack.
- Human response: none
- Resolution: pending review (option 1)

## D-14: Names of settings that `environments.md` lists without a variable name
- Timestamp: 2026-10-06T22:51:13Z
- Phase: 2
- Type: non-blocking
- Trigger: `environments.md` lists "Database URL and user", "SMTP host, port, TLS" and "Sender address" without variable names, and no frontend port; `APP_WORKSHOPS` has no stated encoding.
- Options: 1. `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `APP_MAIL_STARTTLS` (default false), `APP_MAIL_FROM` (default `registration@confreg.local`); `APP_WORKSHOPS` as `W1=title;W2=title;…`; frontend on 127.0.0.1:3000 (proposed, `docs/02_specification.md` 3); 2. other names chosen by the human.
- Human response: none
- Resolution: pending review (option 1)

## D-15: Acceptance manifest also lists the three phase 3 run logs
- Timestamp: 2026-10-06T23:07:27Z
- Phase: 3
- Type: non-blocking
- Trigger: `docs/03_acceptance-manifest.sha256` (freeze commit 3fece11) was built from every tracked path containing `acceptance` or `e2e`; besides the 13 test files it lists `logs/03_backend-acceptance-first-run.log`, `logs/03_frontend-acceptance-first-run.log` and `logs/03_frontend-e2e-first-run.log`. Only a human may change the manifest after the freeze (`general/working-rules.md`).
- Options: 1. keep the manifest unchanged: the logs are evidence that never changes, freezing them is harmless; later runs write to new log files (proposed); 2. the human removes the three lines.
- Human response: none
- Resolution: pending review (option 1)

## D-16: Frontend mutation testing does not work with vitest 5.0.3
- Timestamp: 2026-10-06T23:56:27Z
- Phase: 6
- Type: blocking
- Trigger: `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` 10.0.0 (`tech-stack.md`, the latest published version) run with vitest 5.0.3 (approved in D-05), but mutants are never activated: score 2.16 % with `coverageAnalysis` perTest and with off, while mutants such as `if (!response.ok)` → `if (false)` are certainly killed by `src/api.test.ts` (`out/logs/06_frontend-stryker.log`, `06_frontend-stryker-coverage-off.log`). DoD-03 needs a recorded mutation score; the project threshold is "record only".
- Options: 1. record the frontend mutation score as not measurable with the pinned tools; backend mutation score (PIT, 82 %) and frontend line/branch coverage are recorded; list it as an open item for the next tool update (proposed); 2. measure once in a throw-away copy outside the repository with vitest 3.2.7 (the version D-05 replaced for its Critical advisories), report the score, change nothing in the repository; 3. another mutation tool or version named by the human.
- Human response: none
- Resolution: pending

## D-17: Semgrep "use-of-basic-authentication" (ERROR = High) lowered to Low
- Timestamp: 2026-10-06T23:56:27Z
- Phase: 6
- Type: blocking
- Trigger: semgrep 1.177.0 rule `use-of-basic-authentication` (severity ERROR, mapped to High by `severity-scale.md`) on `docs/02_contracts/registration-api.openapi.yaml:117` (`organizerBasic`, `scheme: basic`). Lowering a High needs written evidence and a blocking decision.
- Evidence: HTTP Basic is required by `project/02_design/architecture.md` (fixed API: "Organizer only: HTTP Basic") and `security-requirements.md`; credentials are accepted only over HTTPS or from loopback outside the local/test profiles (SR-03, `InsecureCredentialsFilter`, `WebFiltersTest`); the password exists only as a BCrypt hash in memory (SB-03, `SecurityConfig`); failed logins are rate limited per client (SB-06, `RateLimitFilter` auth-failure bucket, `WebFiltersTest.failedLoginsBlockFurtherCredentialsFromThatClient`); sessions are stateless and the only Basic-protected operation is a read.
- Options: 1. lower to Low and accept (proposed); 2. keep High (release blocked) and change the authentication scheme (contradicts architecture.md).
- Human response: option 1. Given in this session during phase 0, before the finding existed (time not recorded by the harness; before 2026-10-06T22:31Z): "Lower it to Low and accept it: HTTP Basic is required by architecture.md and security-requirements.md, credentials only over HTTPS or localhost, password only as a BCrypt hash, failed logins rate limited."
- Resolution: option 1; F-01 lowered to Low and accepted. Each condition in the human response is implemented and tested (see Evidence).
