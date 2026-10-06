# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host JDK is Oracle 21.0.11, not Temurin 21.0.10+7
- Timestamp: 2026-10-06T18:06:00Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `java` pins Eclipse Temurin JDK 21.0.10+7. `java -version` reports Oracle Java SE 21.0.11+9-LTS-211; second check: `JAVA_HOME` is `C:\Program Files\Java\jdk-21.0.11`, its `release` file says `IMPLEMENTOR="Oracle Corporation"`, and no Temurin JDK is installed under `C:\Program Files\Eclipse Adoptium`. Different vendor and version, so the pin does not run as listed.
- Options:
  1. (default) Human installs Eclipse Temurin JDK 21.0.10+7 from https://adoptium.net and points `JAVA_HOME`/`PATH` at it.
  2. Human approves Oracle JDK 21.0.11+9 as an amendment to the `java` entry for this run.
- Human response: none
- Resolution: pending review

## D-02: Host Node.js is 24.10.0 with npm 10.9.4, not 24.13.0 with npm 11.6.2
- Timestamp: 2026-10-06T18:06:00Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `node` pins Node.js 24.13.0 with npm 11.6.2 (also tooling `npm audit (npm 11.6.2)`). `node --version` reports v24.10.0 and `npm --version` reports 10.9.4; second check: `node -p process.versions.node` gives 24.10.0, and the only install on `PATH` is `C:\Program Files\nodejs` (no nvm/fnm/volta).
- Options:
  1. (default) Human installs Node.js 24.13.0 (ships with npm 11.6.2) from https://nodejs.org.
  2. Human approves Node.js 24.10.0 / npm 10.9.4 as an amendment for this run.
- Human response: none
- Resolution: pending review

## D-03: cloc image tag 2.10 reports cloc 1.98
- Timestamp: 2026-10-06T18:06:00Z
- Phase: 0
- Type: non-blocking
- Trigger: tooling `aldanial/cloc:2.10` resolves and runs, but `--version` prints `1.98`; second check: its report header reads `github.com/AlDanial/cloc v 1.98`. Pin resolves and runs, tool reports another version (`tech-stack.md` rules).
- Options:
  1. (default) Keep the pinned tag `2.10` as authoritative and record the reported version 1.98 in metrics.
  2. Ask for another tag.
- Human response: none
- Resolution: option 1; the pinned tag stays authoritative

## D-04: npm audit finds Critical and High vulnerabilities in vitest 3.2.7 and jscpd 4.3.0
- Timestamp: 2026-10-06T18:06:00Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` (npm 11.6.2 in `node:24.13.0-alpine`) over every npm entry of `tech-stack.md`: 2 Critical, 5 High, 4 Moderate.
  - Critical: `vitest` 3.2.7 through `tinypool` <= 2.1.1 (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr, prototype pollution to RCE) and `@vitest/mocker` (GHSA-82fw-gwwq-j7x9, path traversal, fixed in vitest 4.1.11); `@vitest/coverage-v8` 3.2.7 is affected through vitest.
  - High: `jscpd` 4.3.0 through `@jscpd/finder` → `fast-glob` → `micromatch` → `braces` (GHSA-vfj7-8cjw-p6xm, CVSS 7.5). Fix: jscpd 5.4.0.
  - Moderate (not blocking): `qs` via `typed-rest-client` via `@stryker-mutator/core` 10.0.0 (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g).
  All affected packages are dev/test tooling and are not in the shipped frontend bundle, but lowering a Critical or High needs a human decision (`severity-scale.md`).
- Options:
  1. (default) Amend `vitest` and `@vitest/coverage-v8` to 4.1.11 and `jscpd` to 5.4.0. Verified: resolves; peer ranges fit `vite` 6.4.3, `jsdom` 26.1.0 and `@stryker-mutator/vitest-runner` 10.0.0; re-audit shows 0 Critical, 0 High, 2 Moderate (`qs`, above).
  2. Amend `vitest` and `@vitest/coverage-v8` to 5.0.3 and `jscpd` to 5.4.0. Same re-audit result; larger jump from the pinned major.
  3. Keep the pins and accept the findings as dev-only (lowers Critical/High on written evidence).
- Human response: none
- Resolution: pending review

## D-05: Preflight scans run the pinned tools in pinned containers
- Timestamp: 2026-10-06T18:06:00Z
- Phase: 0
- Type: non-blocking
- Trigger: the host JDK and Node do not match their pins (D-01, D-02), and installing software on the host needs approval. Running the scans with the wrong host versions would be a substitution.
- Options:
  1. (default) Run `npm audit` with npm 11.6.2 inside `node:24.13.0-alpine`, and `dependency-check-maven` 12.1.0 with Maven 3.9.9 inside `eclipse-temurin:21.0.10_7-jre-alpine` (both images from `tech-stack.md`), on scratch manifests listing exactly the `tech-stack.md` dependency set. Nothing is installed on the host.
  2. Wait for D-01 and D-02 before scanning.
- Human response: none
- Resolution: option 1

## D-06: Dependency-Check analysers disabled: OSS Index (credentials) and .NET assembly (no dotnet)
- Timestamp: 2026-10-06T18:46:00Z
- Phase: 0
- Type: non-blocking
- Trigger: the first preflight run of `org.owasp:dependency-check-maven` 12.1.0 (D-05) ended in BUILD FAILURE: the OSS Index analyser got HTTP 401 Unauthorized from `ossindex.sonatype.org` (it now needs an account that `secrets.env.example` does not list), and the .NET assembly analyser could not start because `dotnet` is not installed (the project has no .NET code). The NVD data download itself completed with `NVD_API_KEY`.
- Options:
  1. (default) Disable the OSS Index analyser (`ossindexAnalyzerEnabled=false`) and the assembly analyser (`assemblyAnalyzerEnabled=false`); scan with the NVD-based analysers. Use the same settings in the backend `pom.xml` for phase 6.
  2. Human adds an OSS Index account to the secrets list (input change, needs approval).
- Human response: none
- Resolution: option 1

## D-07: Dependency-Check false positive on testcontainers-postgresql 2.0.5 (4 Critical, 24 High)
- Timestamp: 2026-10-06T18:55:00Z
- Phase: 0
- Type: blocking
- Trigger: `dependency-check-maven` 12.1.0 (run 2, D-06) reports 4 Critical (CVE-2015-0244, CVE-2015-3166, CVE-2019-10211, CVE-2018-1115) and 24 High CVEs, plus Medium/Low, on `org.testcontainers:testcontainers-postgresql` 2.0.5 (test scope). Evidence (`out/logs/00_preflight/dependency-check-report.json`): the tool derived `cpe:2.3:a:postgresql:postgresql:2.0.5`, treating the Java module's own version 2.0.5 as PostgreSQL server 2.0.5; every matched CVE is a PostgreSQL server CVE (2007-2026). The jar is the Testcontainers Java module and contains no PostgreSQL server code; the server actually used is the `postgres:16.15-alpine` image from `tech-stack.md`. Lowering a Critical/High needs a human decision (`severity-scale.md`).
- Options:
  1. (default) Classify as Low (false positive, scanner match on the wrong product) and add a Dependency-Check suppression for this CPE on this jar only (`cpe:/a:postgresql:postgresql` on `pkg:maven/org.testcontainers/testcontainers-postgresql@.*`), committed in the backend with this decision id.
  2. Keep it as reported, which blocks the phase 0 gate with no version to move to.
- Human response: none
- Resolution: pending review

## D-08: Critical and High in httpclient5/httpcore5 shaded in docker-java-transport-zerodep 3.7.1 (test scope, no fix)
- Timestamp: 2026-10-06T18:55:00Z
- Phase: 0
- Type: blocking
- Trigger: `dependency-check-maven` 12.1.0 reports, on `com.github.docker-java:docker-java-transport-zerodep` 3.7.1 (pulled in by `testcontainers-junit-jupiter` 2.0.5, test scope): CVE-2026-71290 Critical 9.1 (shaded `httpclient5` 5.5.1: TLS hostname verification has no effect with the async client; fixed in 5.6.4), CVE-2026-54399 and CVE-2026-54428 High 7.5 (shaded `httpcore5` 5.3.6: HTTP/1.1 parser and HTTP/2 HPACK resource exhaustion), and CVE-2026-64607 Medium. On Maven Central (2026-10-06), 3.7.1 is the latest docker-java and 2.0.5 the latest Testcontainers. The client is shaded, so no version override is possible. Use: test scope only. Testcontainers uses it to talk to the local Docker daemon over a named pipe or Unix socket (no TLS, no remote peer), and it is not in the backend runtime image.
- Options:
  1. (default) Classify the Critical and both Highs as Medium (real vulnerabilities, no reachable exploit path: test scope, local Docker daemon only, not shipped), add a suppression scoped to this jar that references this decision, and re-check for a fixed docker-java release in phase 6.
  2. Drop Testcontainers and run the test database from `docker-compose.yml` instead. That changes `tech-stack.md` and the `test` row of `environments.md`.
  3. Keep the findings as reported, which blocks the phase 0 gate with no version to move to.
- Human response: none
- Resolution: pending review

## D-09: Human response to D-01 (JDK)
- Timestamp: 2026-10-06T18:54:28Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-01 (host JDK Oracle 21.0.11+9 vs pinned Temurin 21.0.10+7).
- Options: as in D-01.
- Human response: 2026-10-06T18:54Z (received): "approve Oracle JDK 21.0.11 for this run." If the version causes a real failure later, raise a new blocking decision.
- Resolution: D-01 option 2. For this run, Oracle JDK 21.0.11+9 replaces the `java` platform entry on the host. The backend runtime image stays `eclipse-temurin:21.0.10_7-jre-alpine` as pinned.

## D-10: Human response to D-02 (Node.js)
- Timestamp: 2026-10-06T18:54:28Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-02 (host Node 24.10.0 / npm 10.9.4 vs pinned 24.13.0 / 11.6.2).
- Options: as in D-02.
- Human response: 2026-10-06T18:54Z (received): "approve Node 24.10.0 with npm 10.9.4 for this run. If either version causes a real failure later, raise it as a new blocking decision."
- Resolution: D-02 option 2. Node.js 24.10.0 with npm 10.9.4 replaces the `node` platform entry and the `npm audit` tooling version on the host for this run. The frontend build image stays `node:24.13.0-alpine` as pinned.

## D-11: Human response to D-04 (npm vulnerabilities)
- Timestamp: 2026-10-06T18:54:28Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-04.
- Options: as in D-04.
- Human response: 2026-10-06T18:54Z (received): "amend vitest and @vitest/coverage-v8 to 5.0.3 (not 4.1.11), and jscpd to 5.4.0."
- Resolution: D-04 option 2. `vitest` 5.0.3, `@vitest/coverage-v8` 5.0.3 and `jscpd` 5.4.0 replace the `tech-stack.md` tooling entries for this run.

## D-12: Human response to D-07 (testcontainers-postgresql false positive)
- Timestamp: 2026-10-06T18:54:28Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-07.
- Options: as in D-07.
- Human response: 2026-10-06T18:54Z (received): "default, classify as Low with a suppression scoped to this jar."
- Resolution: D-07 option 1. Low (false positive); Dependency-Check suppression scoped to `testcontainers-postgresql` and CPE `cpe:/a:postgresql:postgresql`.

## D-13: Human response to D-08 (docker-java-transport-zerodep)
- Timestamp: 2026-10-06T18:54:28Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-08.
- Options: as in D-08.
- Human response: 2026-10-06T18:54Z (received): "default, classify as Medium (test scope only), scoped suppression, re-check in phase 6."
- Resolution: D-08 option 1. Medium; suppression scoped to `docker-java-transport-zerodep` for CVE-2026-71290, CVE-2026-54399 and CVE-2026-54428 only (CVE-2026-64607 stays reported as Medium); re-check for a fixed release in phase 6.

## D-14: Correction of timestamps in D-07, D-08 and the preflight report
- Timestamp: 2026-10-06T18:55:00Z
- Phase: 0
- Type: non-blocking
- Trigger: D-07 and D-08 carry `2026-10-06T18:55:00Z`, and the preflight report says the human was asked at `18:58Z`. These were estimated, not read from the clock, and are later than the actual events. Commit `f520d1f` (18:48:44Z) bounds them.
- Options:
  1. (default) Treat D-07 and D-08 as raised at about 18:47Z and the question as asked at about 18:49Z; correct the agent-written report and run log, which are not append-only; leave D-07 and D-08 unchanged here (append-only log).
- Human response: none
- Resolution: option 1

## D-15: CVE-2025-7962 reported High (CVSS v3 7.5) on angus-activation 2.0.3
- Timestamp: 2026-10-06T18:58:32Z
- Phase: 0
- Type: blocking
- Trigger: re-run of `dependency-check-maven` 12.1.0 on `02_output/backend/pom.xml` fails at CVSS >= 7 on `org.eclipse.angus:angus-activation` 2.0.3 (runtime, via `spring-boot-starter-mail` 4.1.1): CVE-2025-7962, CVSS v3 7.5 HIGH and CVSS v4 6.0 MEDIUM. Under `severity-scale.md` (CVSS 7.0–8.9) the conservative reading is High. The preflight report listed it as Medium 6.0 because only the v4 score was read; that row is corrected. Evidence (`out/logs/00_bootstrap/backend-dependency-check-report.json`): the CVE is an SMTP injection in Jakarta Mail < 2.0.2 and Angus Mail < 2.0.4 (CPE `eclipse:angus_mail`). The jar flagged is the Angus *activation* framework, not the mail implementation; the tool derived `cpe:2.3:a:eclipse:angus_mail:2.0.3` from it. The mail implementation on the classpath is `angus-mail` 2.0.5, which is outside the vulnerable range and is not flagged. No newer stable `angus-activation` exists (2.0.3 latest; 2.1.0-M1 is a milestone). SR-05 (no header or markup injection in e-mail) is still implemented and tested in our own code.
- Options:
  1. (default) Classify as Low (false positive, wrong artifact matched) and add a suppression scoped to `angus-activation` and CPE `cpe:/a:eclipse:angus_mail`.
  2. Override `angus-activation` to the milestone 2.1.0-M1 (adds an unlisted pin; milestone quality).
  3. Keep it as reported, which blocks the phase 0 gate.
- Human response: none
- Resolution: pending review

## D-16: D-13 suppression scoped by jar path to cover shaded httpcore5-h2
- Timestamp: 2026-10-06T18:58:32Z
- Phase: 0
- Type: non-blocking
- Trigger: applying D-13, the same CVE-2026-54399 and CVE-2026-54428 also match `httpcore5-h2` 5.3.6, shaded in the same `docker-java-transport-zerodep` 3.7.1 jar (CVE-2026-54428 is the HTTP/2 HPACK issue). A suppression by package name did not cover it.
- Options:
  1. (default) Scope the suppression by file path to `docker-java-transport-zerodep-3.7.1.jar` and to the three CVEs approved in D-13. This is the same jar and the same CVEs as the human approved; CVE-2026-64607 stays reported.
- Human response: none
- Resolution: option 1

## D-17: Playwright's bundled Chromium is not installed on the host
- Timestamp: 2026-10-06T19:01:12Z
- Phase: 0
- Type: blocking
- Trigger: tooling `@playwright/test` 1.63.0 ("end-to-end, with its bundled Chromium") is installed and runs (`playwright --version` = 1.63.0), but its browser has not been downloaded. End-to-end tests start in phase 3. Downloading it puts software on the host, which needs approval (`AGENTS.md`).
- Options:
  1. (default) Agent runs `npx playwright install chromium` in `02_output/frontend/`, which fetches the Chromium build pinned by Playwright 1.63.0 into the user's Playwright cache (no system-wide install).
  2. Human installs it.
  3. Run the end-to-end tests in a Playwright container image (an unlisted image, so it would need its own approval).
- Human response: none
- Resolution: pending review

## D-18: Human response to D-15 (angus-activation CVE-2025-7962)
- Timestamp: 2026-10-06T19:07:39Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-15.
- Options: as in D-15.
- Human response: 2026-10-06T19:07:39Z (received): "default, classify CVE-2025-7962 on angus-activation 2.0.3 as a false positive (Low), with a suppression scoped to this jar. Keep the raw report."
- Resolution: D-15 option 1. Low (false positive); suppression scoped to `angus-activation` and CPE `cpe:/a:eclipse:angus_mail`; the unsuppressed raw report stays in `out/logs/00_bootstrap/backend-dependency-check-report.json`.

## D-19: Human response to D-17 (Playwright Chromium)
- Timestamp: 2026-10-06T19:07:39Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-17.
- Options: as in D-17.
- Human response: 2026-10-06T19:07:39Z (received): "default, run npx playwright install chromium (user cache only)."
- Resolution: D-17 option 1.

## D-20: AC4 "an invoice is issued to the payer" conflicts with AR-08
- Timestamp: 2026-10-06T19:10:40Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 AC4 (AC-001-04) requires that an invoice is issued to the payer when a registration completes. AR-08 says invoicing is owned by the accounting team and invoices are produced by a separate process of the accounting system, which is not part of this repository; `environments.md` lists the accounting system as "produces invoices; not part of this repository", with no local substitute and no manual test. Both are project inputs and no rule resolves the conflict (`AGENTS.md`: a conflict that no rule resolves is a decision record).
- Options:
  1. (default, conservative) Do not produce invoices in this repository (respect AR-08). The backend stores every completed registration with the payer data (`payerType`, company name, address, VAT ID) and amounts (`netFee`, `vat`, `grossFee`), which the organizer, and through them the accounting system, can read (`GET /api/registrations/{registrationNumber}`, organizer only; personal data shown to "organizers, accounting" per `security-requirements.md`). AC-001-04 keeps its wording; its tests check that the stored registration carries everything an invoice needs. Release notes list "invoice issued by the accounting system" for manual verification.
  2. Generate and send an invoice (document or e-mail) from the backend; this contradicts AR-08 and needs an architecture change.
  3. Add an export or hand-over interface to the accounting system; no interface is defined in `architecture.md`, so it would invent one.
- Human response: none
- Resolution: pending review (option 1 applied)

## D-21: Whether the 240/300 EUR fee is net or gross
- Timestamp: 2026-10-06T19:10:40Z
- Phase: 1
- Type: non-blocking
- Trigger: AC1/AC2 state "the fee is 240 EUR" / "300 EUR". The API returns `netFee`, `vat` and `grossFee`, and configuration has `APP_FEE_EARLY` 240.00, `APP_FEE_REGULAR` 300.00 and a separate `APP_VAT_RATE` 0.22. The inputs do not say whether the configured fee includes VAT.
- Options:
  1. (default) The configured fee is the net fee: `netFee` 240.00, `vat` = net × rate rounded half-up to 2 decimals (52.80), `grossFee` 292.80. This matches the naming (`APP_FEE_*` next to a separate VAT rate, field `netFee`) and keeps the configured value unchanged in the stored record. The confirmation e-mail shows all three amounts, so the participant sees the total before paying.
  2. The configured fee is gross: `grossFee` 240.00, `netFee` = 240 / 1.22 = 196.72, `vat` 43.28.
- Human response: none
- Resolution: pending review (option 1 applied)

## D-22: Workshop selection rules
- Timestamp: 2026-10-06T19:10:40Z
- Phase: 1
- Type: non-blocking
- Trigger: the fixed API accepts `workshops` (an array of workshop ids) but stores a single `workshop` (id or null); `APP_WORKSHOPS` lists W1, W2, W3. The story says nothing about workshops (choice, limit, fee).
- Options:
  1. (default, conservative) A workshop is optional; at most one may be chosen; it must be a configured id; anything else is rejected (AC-001-08), so no submitted choice is silently dropped; a workshop does not change the fee; no capacity limit.
  2. Accept several and store only the first (silently drops input).
- Human response: none
- Resolution: pending review (option 1 applied)

## D-23: Input validation and rejection
- Timestamp: 2026-10-06T19:10:40Z
- Phase: 1
- Type: non-blocking
- Trigger: the story and business rules define no field rules; SB-01 requires server-side validation, SB-12 collecting only needed data, SR-05 safe e-mail content, NFR-01 Slovenian characters.
- Options:
  1. (default, conservative) The rules in `docs/01_acceptance-criteria.md` ("Required fields and validation"): required names, e-mail and payer type; company fields required for `company` and not accepted for `private`; unknown properties rejected; length limits; no control characters. A rejected request returns 4xx with the invalid field names, stores nothing and sends no e-mail.
  2. Lenient: ignore unexpected or mismatched fields (risks silently dropping input or storing data that is not needed).
- Human response: none
- Resolution: pending review (option 1 applied)

## D-24: Repeated registrations with the same e-mail address
- Timestamp: 2026-10-06T19:10:40Z
- Phase: 1
- Type: non-blocking
- Trigger: the inputs define no uniqueness rule. Rejecting duplicates could lock out legitimate users (for example a shared company address); accepting them may create double registrations.
- Options:
  1. (default) Accept each valid submission as a separate registration with its own number; organizers handle duplicates. No legitimate user is locked out, and nothing is inferred that the story does not state.
  2. Reject a second registration with the same e-mail (409).
- Human response: none
- Resolution: pending review (option 1 applied)

## D-25: Confirmation e-mail content and failure handling
- Timestamp: 2026-10-06T19:10:40Z
- Phase: 1
- Type: non-blocking
- Trigger: AC3 requires a confirmation e-mail with the registration number and the fee when the request finishes. The inputs do not say what happens when the SMTP server does not accept the message.
- Options:
  1. (default, conservative) Registration and e-mail succeed or fail together: the backend stores the registration in a transaction, hands the e-mail to SMTP, and commits only if the hand-over succeeded; otherwise it rolls back and answers 5xx without details (AC-001-09), so the participant knows to retry and no participant holds a registration without a confirmation. The e-mail is plain text and shows the registration number, `netFee`, `vat` and `grossFee` in EUR and the chosen workshop.
  2. Store the registration and answer 2xx even when the e-mail fails; log the failure (without personal data) for the organizers to resend.
- Human response: none
- Resolution: pending review (option 1 applied)

## D-26: Health and readiness on an internal management port
- Timestamp: 2026-10-06T19:12:29Z
- Phase: 2
- Type: non-blocking
- Trigger: NFR-02 and ES-09 require health and readiness information used by container health checks. `security-requirements.md` requires organizer authentication on every endpoint that no requirement makes public, and public endpoints are rate limited (`APP_RATE_LIMIT_PER_HOUR` 100), which frequent health probes would exhaust.
- Options:
  1. (default) Serve Actuator on a separate management port (8081) inside the container, not published by `docker-compose.yml` and not routed by the reverse proxy. It exposes only `health` (with `liveness` and `readiness` groups) without details. It is reachable only from inside the container or its network, so it is not a public endpoint.
  2. Expose health on the public port without authentication, exempt from rate limiting.
  3. Require organizer credentials for health checks (puts the organizer secret into health-check commands).
- Human response: none
- Resolution: pending review (option 1 applied)

## D-27: SR-03 "except on localhost" with the local Docker stack
- Timestamp: 2026-10-06T19:12:29Z
- Phase: 2
- Type: non-blocking
- Trigger: SR-03 forbids accepting organizer credentials over plain HTTP except on localhost. In the local stack (`docker compose`, ports published on 127.0.0.1 only) requests reach the backend from the Docker gateway or the frontend container, not from a loopback address. In production, TLS ends at the external nginx.
- Options:
  1. (default) Organizer credentials are accepted only when the request is secure (directly, or `X-Forwarded-Proto: https` from a trusted proxy, Tomcat `RemoteIpValve` defaults), or the client address is loopback, or, outside the `prod` profile only, a private (site-local) address. Otherwise the backend answers 403 before checking the credentials. The `prod` profile accepts only secure or loopback requests.
  2. Accept only secure or loopback requests in every profile (organizer access does not work in the local Docker stack, and the DoD-06 runtime demonstration of the organizer flow is not possible there).
- Human response: none
- Resolution: pending review (option 1 applied)

## D-28: Public endpoint listing the configured workshops
- Timestamp: 2026-10-06T19:12:29Z
- Phase: 2
- Type: non-blocking
- Trigger: the registration form must offer the workshops (`APP_WORKSHOPS`, AR-04: read from configuration where used). The frontend may talk to the backend only through `/api` (AR-01), and the fixed API has no workshop list.
- Options:
  1. (default) Add `GET /api/workshops` (public, rate limited like every public endpoint) returning `[{id, title}]` from configuration; the fixed endpoints are unchanged.
  2. Duplicate the workshop list in the frontend configuration (two sources of one business value).
- Human response: none
- Resolution: pending review (option 1 applied)

## D-29: POST /api/registrations is public
- Timestamp: 2026-10-06T19:12:29Z
- Phase: 2
- Type: non-blocking
- Trigger: `security-requirements.md`: all endpoints require organizer authentication unless a requirement explicitly makes one public; there are no participant accounts. US-001 has a conference participant (not an organizer) register; the fixed API says only the `GET` is organizer-only.
- Options:
  1. (default) `POST /api/registrations` is public, as US-001 requires, and rate limited per client (SB-06); `GET /api/registrations/{registrationNumber}` and every other endpoint not made public by a decision require organizer authentication.
  2. Require organizer authentication for registration as well (makes US-001 impossible for participants).
- Human response: none
- Resolution: pending review (option 1 applied)
