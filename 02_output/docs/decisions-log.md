# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host JDK is not the pinned Temurin 21.0.10+7
- Timestamp: 2026-10-06T14:03:31Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 2, platform `java` in `project/00_setup/tech-stack.md` (Eclipse Temurin JDK 21.0.10+7). `java -version` reports Oracle Java SE 21.0.11+9-LTS-211; confirmed a second way with `where.exe java` and a listing of `C:\Program Files\Java` and `C:\Program Files\Eclipse Adoptium`: no Temurin 21.0.10+7 is installed. The pinned platform does not resolve on the host.
- Options:
  1. (proposed default) The human installs Eclipse Temurin JDK 21.0.10+7 (https://adoptium.net) and points `JAVA_HOME`/`PATH` at it for this run; no change to `tech-stack.md`.
  2. The human approves Oracle JDK 21.0.11+9 as the host build JDK for this run (replaces the `java` platform entry); container images stay as pinned.
  3. The human approves building the backend inside a container image `eclipse-temurin:21.0.10_7-jdk-alpine` (same pinned JDK version, new image entry), so nothing is installed on the host.
- Human response: option 2, approve Oracle JDK 21.0.11+9 (2026-10-06T14:07:43Z)
- Resolution: option 2; the human approval replaces the `java` platform entry for this run (Oracle JDK 21.0.11+9 on the host); container images unchanged

## D-02: Host Node.js/npm are not the pinned 24.13.0 / 11.6.2
- Timestamp: 2026-10-06T14:03:31Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 2, platform `node` in `project/00_setup/tech-stack.md` (Node.js 24.13.0 with npm 11.6.2). `node --version` reports v24.10.0 and `npm --version` 10.9.4; confirmed a second way with `where.exe node` and by calling `C:\Program Files\nodejs\node.exe` directly. No nvm/fnm installation present. The pinned image `node:24.13.0-alpine` resolves and reports v24.13.0 / npm 11.6.2.
- Options:
  1. (proposed default) The human installs Node.js 24.13.0 (bundles npm 11.6.2) on the host; no change to `tech-stack.md`.
  2. The human approves host Node.js 24.10.0 / npm 10.9.4 for this run (replaces the `node` platform entry).
  3. The human approves running all frontend npm commands (install, build, test, lint, Playwright) inside the pinned `node:24.13.0-alpine` image (Playwright then also needs its own pinned image, `mcr.microsoft.com/playwright:v1.63.0-noble`, as a new entry).
- Human response: option 2, approve Node.js 24.10.0 / npm 10.9.4 (2026-10-06T14:07:43Z)
- Resolution: option 2; the human approval replaces the `node` platform entry for this run (Node.js 24.10.0, npm 10.9.4 on the host); the `node:24.13.0-alpine` build image is unchanged

## D-03: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-06T14:03:31Z
- Phase: 0
- Type: non-blocking
- Trigger: Preflight step 2, tooling `aldanial/cloc` pinned at `2.10`. The image resolves and runs; `docker run --rm aldanial/cloc:2.10 --version` prints `1.98` (checked twice, the second time after inspecting the image entrypoint `/usr/src/cloc`).
- Options:
  1. (proposed default) Keep the pinned tag `2.10` as authoritative and use it for phase 6 code metrics; note the reported version in the verification report.
  2. Ask for another tag.
- Human response: none
- Resolution: option 1 (tech-stack rule: the listed pin stays authoritative)

## D-04: OWASP Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-06T14:19:28Z
- Phase: 0
- Type: non-blocking
- Trigger: Preflight step 6. The first `dependency-check:check` run (`out/logs/00_dependency-check.log`, first run) reported "An error occurred while analyzing ... (Sonatype OSS Index Analyzer)" for 14 jars; OSS Index needs credentials that `project/00_setup/secrets.env.example` does not list. The NVD analyser (with `NVD_API_KEY`) works.
- Options:
  1. (proposed default) Disable the OSS Index analyser (`ossindexAnalyzerEnabled=false` in `backend/pom.xml`), keep the NVD, RetireJS and other analysers, and re-run.
  2. Ask the human for OSS Index credentials (new secret).
- Human response: none
- Resolution: option 1; re-run completed with 0 analyser errors, 112 dependencies scanned (test scope included)

## D-05: Dependency scans report Critical/High results in test and dev tooling
- Timestamp: 2026-10-06T14:19:28Z
- Phase: 0
- Type: blocking
- Trigger: Preflight step 6 (`general/quality/severity-scale.md`: lowering Critical/High needs written evidence and is blocking). Backend: OWASP Dependency-Check 12.1.0 (NVD), `backend/target/dependency-check-report.json`, log `out/logs/00_dependency-check.log`. Frontend: `npm audit` (npm 10.9.4 per D-02), `out/logs/00_npm-audit.json`. `npm audit --omit=dev` reports 0 vulnerabilities, and none of the jars in (c) is packaged in the backend boot jar (checked with `unzip -l`).
  - (a) `org.testcontainers:testcontainers-postgresql` 2.0.5 (test scope): 31 CVEs, 5 Critical, 21 High (e.g. CVE-2015-0244 9.8, CVE-2026-6473 8.8). Evidence of false positive: matched through CPE `cpe:2.3:a:postgresql:postgresql:2.0.5`, i.e. the PostgreSQL **server** product at "version 2.0.5"; the CVE texts describe the PostgreSQL server; the jar is a Java test helper.
  - (b) `org.eclipse.angus:angus-activation` 2.0.3 (runtime): CVE-2025-7962, tool severity MEDIUM but CVSS v3 7.5. Evidence of false positive: matched through CPE `eclipse:angus_mail`; the CVE affects Jakarta Mail < 2.0.2 / angus_mail < 2.0.4; the resolved `angus-mail` is 2.0.5.
  - (c) `com.github.docker-java:docker-java-transport-zerodep` 3.7.1 (test scope, transitive of Testcontainers 2.0.5), shaded `httpclient5` 5.5.1: CVE-2026-71290 9.1 Critical (TLS hostname verification); shaded `httpcore5` 5.3.6: CVE-2026-54399 and CVE-2026-54428 7.5 High (HTTP/1.1 and HPACK resource exhaustion). Real version match. 3.7.1 is the latest docker-java and 2.0.5 the latest Testcontainers on Maven Central (checked 2026-10-06T14:19:28Z), so no fixed version exists. The transport only talks to the local Docker daemon (Windows named pipe / Unix socket, no TLS, no remote peer) while tests run; it is not shipped.
  - (d) `vitest` 3.2.7 (dev) → `tinypool` 1.1.1: Critical (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr, prototype-pollution gadget in worker options); plus `@vitest/mocker` GHSA-82fw-gwwq-j7x9 Moderate. Fix only in vitest 5.0.3 (major). Test runner only; runs our own tests locally; not in `dist/`.
  - (e) `jscpd` 4.3.0 (dev) → `braces` 3.0.3 via `micromatch`/`fast-glob`: High (GHSA-vfj7-8cjw-p6xm, stack exhaustion on deeply nested glob patterns). Fix only in jscpd 5.4.0 (major). Patterns come only from our own command line.
  - Non-blocking, Medium and below: `qs`/`typed-rest-client` via `@stryker-mutator/core` (Moderate, dev); `hibernate-validator` CVE-2025-15104 (Medium, CPE mismatch with validator.nu); 15 Medium/Low items in (a).
- Options:
  1. (proposed default) Keep all pins. Classify (a) and (b) as false positives (Low) and (c), (d), (e) as Medium ("dependency vulnerability with no reachable exploit path": test/dev scope only, not shipped in the backend jar or frontend `dist/`), each with the evidence above; record them in a Dependency-Check suppression file with this decision id and re-check in phase 6.
  2. As option 1 for (a)–(c) (no backend fix exists), but upgrade dev tooling: `vitest` and `@vitest/coverage-v8` 3.2.7 → 5.0.3, `jscpd` 4.3.0 → 5.4.0 (replaces those tech-stack entries; compatibility with `@stryker-mutator/vitest-runner` 10.0.0 to be checked).
  3. Another set of versions named by the human.
- Human response: option 2, upgrade dev tooling (2026-10-06T14:24:23Z)
- Resolution: option 2. The human approval replaces the tech-stack entries `vitest` and `@vitest/coverage-v8` with 5.0.3 and `jscpd` with 5.4.0 (MIT; peers checked: vite 6.4.x allowed, `@stryker-mutator/vitest-runner` 10.0.0 needs vitest >= 2.0.0; Stryker probe run passed). Re-scan: `npm audit` 0 Critical/High (2 Moderate: `qs` via `@stryker-mutator/core`, dev only). Backend (a), (b) false positives and (c) accepted as Medium, recorded in `backend/dependency-check-suppressions.xml` (also covers the shaded `httpcore5-h2` 5.3.6 in the same jar, same CVE ids), plus the hibernate-validator CPE mismatch; Dependency-Check now fails the build at CVSS >= 7 and reports 0 open of 112 dependencies, 49 suppressed

## D-06: How a student registers for free
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 [3] "Students register for free"; `business-rules.md` "No fields beyond the fixed registration API", and the fixed API (`architecture.md`) has no field that identifies a student; `payerType` is fixed to `private`/`company`.
- Options:
  1. (proposed default) Add one optional boolean `student` (default `false`) to the request and the stored registration; students pay 0.00; `payerType` and its rules still apply; no proof is uploaded; the student flag appears in the confirmation e-mail and the invoicing export so the organizer can check status. Clients that omit the field (fixed API) get a paying registration.
  2. Add a third `payerType` value `student` (changes a fixed API value).
  3. Require proof of student status (upload or organizer approval) before the fee is waived (new workflow nobody stated).
  4. Do not support free student registration (drops US-001 [3]).
- Human response: none
- Resolution: pending review (option 1: the only one that keeps the fixed API names and values and does not invent a workflow)

## D-07: "An invoice is issued" vs. AR-08
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 [5] "An invoice is issued" conflicts with AR-08 ("Invoices are produced by a separate process of the accounting system, which is not part of this repository"). `tech-stack.md` lists `poi-ooxml` for "Excel export". AR-08 is an explicit architecture constraint; no precedence rule orders the two project files.
- Options:
  1. (proposed default) The system does not create invoices. It stores every invoice field (payer, company name/address/VAT ID, amounts) and offers the organizer an authenticated Excel export from which the accounting system issues the invoice; the confirmation e-mail says the invoice follows separately (AC-001-05, AC-001-25).
  2. Generate invoice documents in the backend (violates AR-08).
  3. Push registrations to the accounting system through an API (no interface defined; not in `environments.md`).
- Human response: none
- Resolution: pending review (option 1)

## D-08: How the fee is paid
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 "register for the conference and pay the fee"; no payment provider in `tech-stack.md`, `environments.md` or `secrets.env.example`; invoicing belongs to accounting (AR-08).
- Options:
  1. (proposed default) No online payment. The user pays against the invoice issued by accounting; the system calculates and communicates the amount (AC-001-01, AC-001-25). No payment status is tracked.
  2. Integrate an online payment provider (new service and secret, not approved).
- Human response: none
- Resolution: pending review (option 1)

## D-09: Fee amounts, VAT and rounding
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 [2]; `environments.md` gives `APP_FEE_EARLY`, `APP_FEE_REGULAR` and `APP_VAT_RATE` separately; the stored registration has `netFee`, `vat`, `grossFee`. Not stated: whether the fees are net or gross, whether a company with a VAT ID pays VAT, how to round.
- Options:
  1. (proposed default) The configured fees are net amounts; `vat` = `netFee` × `APP_VAT_RATE` rounded half-up to 0.01; `grossFee` = `netFee` + `vat`; the same VAT applies to every payer, including companies with a VAT ID (admission to an event is taxed where the event takes place; no reverse charge); amounts are computed with decimals, never floating point.
  2. The configured fees are gross amounts and VAT is extracted from them.
  3. Reverse charge (0 VAT) for companies with a foreign EU VAT ID.
- Human response: none
- Resolution: pending review (option 1: matches the field names; option 3 would under-charge VAT if wrong)

## D-10: Early-bird boundary
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 [2] "early bird / regular"; `APP_EARLY_BIRD_DEADLINE` is a date; AR-05 says business dates are interpreted in `APP_CONFERENCE_TZ`. Not stated: inclusive or exclusive, and which instant counts.
- Options:
  1. (proposed default) The registration time (the instant the backend accepts the request, from the clock component) is converted to a date in `APP_CONFERENCE_TZ`; early bird applies when that date is on or before the deadline date (the whole deadline day included).
  2. The deadline date is exclusive (early bird ends at the start of the deadline day).
  3. The time the user opened the form counts.
- Human response: none
- Resolution: pending review (option 1: the usual reading of "until 31 July", and the one that does not charge users more than announced)

## D-11: Workshops per registration
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: The fixed request field is `workshops` (array) but the stored registration has a single `workshop` (id or null); US-001 mentions "the workshops" but not how many, their price or capacity.
- Options:
  1. (proposed default) Zero or one workshop per registration: an empty or missing array stores `null`, one known id is stored, more than one id or an unknown id is rejected (never silently dropped); the workshop is included in the fee; no capacity limit.
  2. Accept several workshops and store only the first (silently drops input).
  3. Change the stored registration to a list (changes the fixed API).
- Human response: none
- Resolution: pending review (option 1)

## D-12: Repeated registration with the same e-mail address
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 does not say whether one person (e-mail) may register more than once.
- Options:
  1. (proposed default) One registration per e-mail address, compared case-insensitively after trimming; a second one is answered 409 with a message, nothing is stored and no e-mail is sent. This prevents double invoices from double submissions.
  2. Allow any number of registrations per address.
- Human response: none
- Resolution: pending review (option 1)

## D-13: Required fields per payer type
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 [1] "fills in the registration form"; `business-rules.md` lists no field rules.
- Options:
  1. (proposed default) Always required: `firstName`, `lastName`, `email` (valid address), `payerType`. For `company`: `companyName` and `companyAddress` required, `companyVatId` optional (not every company payer has one; requiring it would lock out legitimate payers). For `private`: company fields must be blank or absent; non-blank ones are rejected rather than silently dropped. Values are trimmed; maximum lengths are set in the specification.
  2. Also require `companyVatId` for companies.
  3. Ignore company fields for private payers.
- Human response: none
- Resolution: pending review (option 1)

## D-14: Registration when the confirmation e-mail cannot be sent
- Timestamp: 2026-10-06T14:26:04Z
- Phase: 1
- Type: non-blocking
- Trigger: US-001 [4] "receives a confirmation e-mail"; not stated what happens when SMTP fails.
- Options:
  1. (proposed default) The registration is stored and answered 201 regardless; the e-mail is sent after the registration is committed, and a failed send is retried by the backend until it succeeds, so the registration is never lost and the e-mail is not sent twice for one registration.
  2. Fail the whole registration when the e-mail cannot be sent (user must retry; risk of lost registrations).
- Human response: none
- Resolution: pending review (option 1)

## D-15: Formats of APP_WORKSHOPS and the registration number
- Timestamp: 2026-10-06T14:31:28Z
- Phase: 2
- Type: non-blocking
- Trigger: `environments.md` gives the `APP_WORKSHOPS` default as prose ("`W1` Requirements engineering ...; `W2` ...") without a machine format; `architecture.md` fixes the field `registrationNumber` but not its format.
- Options:
  1. (proposed default) `APP_WORKSHOPS` = `id=title` pairs separated by `;` (default `W1=Requirements engineering for AI coding agents;W2=Data spaces in practice;W3=Secure software supply chains`); registration number `CR-` plus a 6-digit number from a database sequence (`CR-000001`).
  2. JSON in `APP_WORKSHOPS`; random (non-sequential) registration numbers.
- Human response: none
- Resolution: pending review (option 1: readable in an environment variable; organizer-only access makes sequential numbers acceptable)

## D-16: Public endpoints, rate-limit client and "localhost" for SR-03
- Timestamp: 2026-10-06T14:31:28Z
- Phase: 2
- Type: non-blocking
- Trigger: `security-requirements.md`: every endpoint needs organizer authentication "unless a requirement explicitly makes an endpoint public"; public endpoints rate limited "per client"; SR-03 allows plain-HTTP credentials "on localhost". US-001 (anonymous participants, no accounts) requires public registration; AC-001-20/22 require workshops and the current price on the public page; ES-09/NFR-02 require health checks used by containers.
- Options:
  1. (proposed default) Public: `POST /api/registrations`, `GET /api/registration-options`, `/actuator/health` (+ liveness/readiness, no details). All else organizer-only. "Client" = remote IP address (the `X-Forwarded-For` address only in profile `prod`, behind the trusted proxy); separate hourly buckets for registration, options and failed organizer logins, each `APP_RATE_LIMIT_PER_HOUR`. "Localhost" = loopback, plus private addresses outside `prod` (Docker forwards 127.0.0.1-published ports from its bridge gateway); in `prod` only HTTPS via the proxy or loopback.
  2. Hard-code workshops and prices in the frontend and keep only `POST` public (violates AR-04 and AR-01's single source).
  3. One shared bucket per client for all public endpoints (price refreshes on the form could use up the registration allowance).
- Human response: none
- Resolution: pending review (option 1)

## D-17: JDK image for the backend container build stage
- Timestamp: 2026-10-06T15:08:00Z
- Phase: 4
- Type: non-blocking
- Trigger: `tech-stack.md` lists only the runtime image `eclipse-temurin:21.0.10_7-jre-alpine`; `docker compose up` (environments.md "local") must build the backend jar inside Docker, which needs a JDK. Rule in `tech-stack.md`: a tool not listed may be added with an exact version and a non-blocking decision.
- Options:
  1. (proposed default) Add build-only image `eclipse-temurin:21.0.10_7-jdk-alpine` (same Temurin version as the pinned JRE; GPL-2.0-with-classpath-exception, not shipped: only the JRE stage is the runtime image) as the first stage of `backend/Dockerfile`; scanned in phase 6 like any other entry.
  2. Build the jar on the host first and copy it into the JRE image (compose would depend on a manual step).
- Human response: none
- Resolution: option 1

## D-18: Frozen test AC-001-12 counts unrelated e-mails for the input "@example.org"
- Timestamp: 2026-10-06T15:08:00Z
- Phase: 4
- Type: blocking
- Trigger: First phase 4 run (`logs/04_backend-test-run1.log`): 43 of 44 backend acceptance tests pass; `ValidationAcceptanceTest.ac001_12_invalidEmailAddressIsRejected` fails at line 30 with "expected: 0 but was: 30". The API answers 400 for `"@example.org"` and stores nothing (the assertions before line 30 pass). Line 30 then counts mails "to" that address via the Mailpit search `to:"@example.org"`, which matches by substring, so it counts every confirmation that other tests correctly sent to `p-<uuid>@example.org`. No implementation can make the count 0 while the other acceptance tests pass; the test is frozen (`docs/03_acceptance-manifest.sha256`).
- Options:
  1. (proposed default) Change only the test input `"@example.org"` to `"@missing-local-part.invalid"` (still an address without local part, and no other test sends to that domain), then re-hash `backend/.../ValidationAcceptanceTest.java` in the manifest. The human updates the manifest (or authorizes the agent to do so as a recorded human action).
  2. In `ValidationAcceptanceTest.assertRejected`, skip the mail check for addresses without a local part.
  3. Leave the test unchanged and accept it as a known failing frozen test (the phase 4 gate "All frozen acceptance tests pass" then fails).
- Human response: option 1, change the input and re-hash; the agent is authorized to make both edits (2026-10-06T16:09:09Z)
- Resolution: option 1, applied by the agent on the human's authorization: input `"@example.org"` → `"@missing-local-part.invalid"` in `ac001_12_invalidEmailAddressIsRejected` only (google-java-format wrapped the line), hash of `ValidationAcceptanceTest.java` updated in `docs/03_acceptance-manifest.sha256`; all other hashes unchanged and verified. Re-run: 44/44 backend acceptance tests pass (`logs/04_backend-test-run2.log`)
