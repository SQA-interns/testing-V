# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Pinned JDK Temurin 21.0.10+7 not installed on host
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `java` pins Eclipse Temurin JDK 21.0.10+7. `java -version` reports Oracle Java SE 21.0.11+9-LTS; second check (`which -a java`, `JAVA_HOME`, `C:\Program Files\Java`, `C:\Program Files\Eclipse Adoptium`) finds only Oracle `jdk-21.0.11` and a Java 8 JRE. The pin does not resolve on the host.
- Options:
  1. (proposed default) Human installs Eclipse Temurin JDK 21.0.10+7 from https://adoptium.net and points `JAVA_HOME` (and `PATH`) to it; agent re-runs the check.
  2. Approve Oracle JDK 21.0.11+9 as the build JDK, replacing the `java` platform entry (licence of the Oracle JDK must be checked by the human; the runtime image stays `eclipse-temurin:21.0.10_7-jre-alpine`).
  3. Run all Maven commands inside a container `eclipse-temurin:21.0.10_7-jdk-alpine` (a new image entry, needs approval); nothing installed on the host.
- Human response: Option 2, approve Oracle JDK 21.0.11; raise a new blocking decision if it causes a real failure (2026-10-06T11:06:50Z)
- Resolution: 2, Oracle JDK 21.0.11+9 replaces the `java` platform entry for this run

## D-02: Pinned Node.js 24.13.0 / npm 11.6.2 not installed on host
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `node` pins Node.js 24.13.0 with npm 11.6.2. `node --version` reports v24.10.0 and `npm --version` 10.9.4; second check (`which -a node npm`, no nvm/fnm/volta found) confirms only `C:\Program Files\nodejs` exists. The pin does not resolve on the host.
- Options:
  1. (proposed default) Human installs Node.js 24.13.0 (bundles npm 11.6.2) from https://nodejs.org; agent re-runs the check.
  2. Approve Node.js 24.10.0 / npm 10.9.4 as replacement for the `node` platform entry.
  3. Run npm commands inside the listed `node:24.13.0-alpine` image (verified: reports v24.13.0 / 11.6.2). Limitation: Playwright's bundled Chromium does not support Alpine (musl), so end-to-end tests would still need a host Node or another, non-listed image (needs approval).
- Human response: Option 2, approve Node 24.10.0 with npm 10.9.4; raise a new blocking decision if it causes a real failure (2026-10-06T11:06:50Z)
- Resolution: 2, Node.js 24.10.0 / npm 10.9.4 replace the `node` platform entry for this run

## D-03: vitest 3.2.7 has Critical vulnerabilities
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 6, `npm audit` (npm 11.6.2 in `node:24.13.0-alpine`) over the pinned frontend set: Critical in `vitest` 3.2.7 via `tinypool` (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr, prototype pollution to RCE) and `@vitest/mocker` (GHSA-82fw-gwwq-j7x9, path traversal); `@vitest/coverage-v8` 3.2.7 Moderate via `vitest`. Fix only in a major version.
- Options:
  1. (proposed default) Replace `vitest` and `@vitest/coverage-v8` 3.2.7 with 5.0.3. Checked: vitest 5.0.3 peer range accepts vite 6.4.3 and jsdom; `@stryker-mutator/vitest-runner` 10.0.0 accepts vitest >=2.0.0; engines accept Node 24. Re-audit of the set with this change and option 1 of D-04: 0 Critical, 0 High, 2 Moderate (`qs` via `typed-rest-client`).
  2. Keep 3.2.7 and accept the risk as test-only tooling that never ships (lowering Critical needs this written evidence and your approval).
- Human response: Option 1, move vitest and @vitest/coverage-v8 to 5.0.3 (2026-10-06T11:06:50Z)
- Resolution: 1, `vitest` and `@vitest/coverage-v8` 5.0.3 replace 3.2.7

## D-04: jscpd 4.3.0 has High vulnerabilities
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 6, same `npm audit` run: High in `jscpd` 4.3.0 via `@jscpd/finder` → `fast-glob` → `micromatch` → `braces` (GHSA-vfj7-8cjw-p6xm, CVSS 7.5, stack-exhaustion DoS). Fix only in a major version.
- Options:
  1. (proposed default) Replace `jscpd` 4.3.0 with 5.4.0 (engines `node >=18`). Re-audit result: see D-03 option 1.
  2. Keep 4.3.0 and accept the risk as a dev-only tool run on the project's own source (lowering High needs your approval).
  3. Drop `jscpd` and measure frontend duplication with PMD CPD (`maven-pmd-plugin`, already listed) in TypeScript mode.
- Human response: Option 1, move jscpd to 5.4.0 (2026-10-06T11:06:50Z)
- Resolution: 1, `jscpd` 5.4.0 replaces 4.3.0

## D-05: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: non-blocking
- Trigger: tooling `aldanial/cloc:2.10` resolves and runs, but `--version` prints 1.98 (image created 2023-08-19, digest sha256:54d1304d…).
- Options:
  1. (proposed default) Keep the listed pin `aldanial/cloc:2.10` as authoritative and use it for code metrics.
- Human response: none
- Resolution: 1, keep the pin (non-blocking rule in `tech-stack.md`)

## D-06: Secret presence check in `.env` could not be run
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 4. The presence check (`sed -n 's/^KEY=//p' .env | tr … | grep -q .`, prints only "set"/"empty") was refused by the Claude Code permission classifier. `.env` exists at the repository root and is git-ignored (`.git/info/exclude`). Values were not read. Needed: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (≥ 16 random characters), `NVD_API_KEY`; `SMTP_USERNAME`/`SMTP_PASSWORD` not needed locally.
- Options:
  1. (proposed default) Human allows the presence-check command (Bash permission rule) and the agent re-runs it.
  2. Human runs the check and confirms in the conversation that the four keys are non-empty and the organizer password has at least 16 characters (never paste values).
- Human response: Option 1, permission mode switched; human approves the check command, run it exactly as written, then the backend dependency scan (2026-10-06T11:06:50Z)
- Resolution: 1, agent re-runs the presence check

## D-07: OSS Index analyser disabled in OWASP Dependency-Check
- Timestamp: 2026-10-06T11:13:58Z
- Phase: 0
- Type: non-blocking
- Trigger: preflight step 6, backend scan. `dependency-check-maven` 12.1.0 failed with HTTP 401 Unauthorized from `https://ossindex.sonatype.org/api/v3/component-report` for every jar (`out/logs/00_backend-depcheck.log`, first run). OSS Index needs credentials that `secrets.env.example` does not list.
- Options:
  1. (proposed default) Disable the OSS Index analyser (`ossindexAnalyzerEnabled=false` in `backend/pom.xml`) and scan with the NVD analyser (and the other local analysers).
- Human response: none
- Resolution: 1, disabled; re-run completed with the NVD CVE analyser (66 dependencies scanned)

## D-08: CVE-2025-7962 (CVSS v3 7.5, High) matched to angus-activation 2.0.3
- Timestamp: 2026-10-06T11:13:58Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 6, OWASP Dependency-Check (NVD) on the backend set reports CVE-2025-7962 on `org.eclipse.angus:angus-activation` 2.0.3 (transitive via `spring-boot-starter-mail`): CVSS v3.1 7.5 HIGH, CVSS v4 6.0 MEDIUM, tool's overall severity MEDIUM. The CVSS v3 score maps to High (`severity-scale.md`). Evidence that it does not apply: the CVE describes SMTP injection in Jakarta Mail versions before 2.0.2; `angus-activation` is the Jakarta Activation implementation, not Jakarta Mail; the mail implementation in use is `org.eclipse.angus:angus-mail` 2.0.5 with `jakarta.mail:jakarta.mail-api` 2.1.5, both outside the affected range and not reported by the scanner. The match comes from the CPE analyser.
- Options:
  1. (proposed default) Classify as a false positive (Low), add a dependency-check suppression for CVE-2025-7962 on `angus-activation` with this evidence, and continue. The design still neutralises CR/LF in any user value written to a mail header (SMTP injection is relevant to US e-mail flows).
  2. Keep it as High; the backend set fails the phase 0 gate until a non-affected version or a human-approved override exists (none needed in practice, since the affected library is not present).
- Human response: Option 1; treat as false positive (Low) with the stated evidence, suppress for that one artifact only, keep the raw report, re-run the scan (2026-10-06T11:17:25Z)
- Resolution: 1, Low (false positive); suppression in `backend/dependency-check-suppressions.xml` limited to `pkg:maven/org.eclipse.angus/angus-activation@2.0.3`; raw report kept in `out/logs/00_backend-depcheck-raw.json`

## D-09: Playwright and Vite config files left out of the TypeScript type-check scope
- Timestamp: 2026-10-06T11:17:25Z
- Phase: 0
- Type: non-blocking
- Trigger: frontend bootstrap, `tsc --noEmit` failed with TS2591 (`process` unknown) in `frontend/playwright.config.ts` because `@types/node` is not in `tech-stack.md` (`out/logs/00_frontend-tools.log`).
- Options:
  1. (proposed default) Limit `tsconfig.json` `include` to `src` and `tests`; the config files are still linted by ESLint and loaded by their tools.
  2. Add `@types/node` as an unlisted dev dependency with an exact version and a non-blocking record.
- Human response: none (human asked at 2026-10-06T11:17:25Z that this be recorded)
- Resolution: 1, applied in commit 061b6e6

## D-10: What counts as an invalid registration field (AC-001-07)
- Timestamp: 2026-10-06T11:19:59Z
- Phase: 1
- Type: non-blocking
- Trigger: REQ-REG-01 AC7 says "missing or invalid" but does not define invalid; the choice is visible to users (which inputs are rejected with 422).
- Options:
  1. (proposed default, conservative: rejects only clearly unusable data, never a legitimate name, address or foreign VAT ID) A text field is missing when absent, null or blank after trimming. `email` is invalid unless it is one syntactically valid address (local part @ domain with a dot), at most 254 characters. `payerType` is invalid unless exactly `private` or `company`. `workshops` is invalid when it holds more than one entry or an id not in `APP_WORKSHOPS`; absent, null or empty means no workshop. Names at most 100, company name 200, company address 500, VAT ID 30 characters; any field containing a control character (CR, LF, etc.) is invalid (SR-05). No country-specific VAT ID format check. For a private payer, company fields sent by the client are ignored and not stored (AC-001-05). A body that is not a JSON object answers 400.
  2. Stricter: country-specific VAT ID patterns and 422 for company fields sent with a private payer.
- Human response: none
- Resolution: 1, pending review

## D-11: How the registration rate limit counts (AC-001-13)
- Timestamp: 2026-10-06T11:19:59Z
- Phase: 1
- Type: non-blocking
- Trigger: `security-requirements.md` and REQ-REG-01 require rate limiting per client at `APP_RATE_LIMIT_PER_HOUR` but do not say what a client is, which requests count, or which window.
- Options:
  1. (proposed default, conservative: also limits floods of invalid requests) Client = remote IP address as seen by the backend (in production, the reverse proxy's forwarded address, configured in phase 2); every `POST /api/registrations` counts, valid or not; sliding one-hour window; over the limit: 429 with `Retry-After`, nothing stored, no e-mail.
  2. Count only accepted registrations.
- Human response: none
- Resolution: 1, pending review

## D-12: Behaviour when the confirmation e-mail cannot be sent (AC-001-04)
- Timestamp: 2026-10-06T11:19:59Z
- Phase: 1
- Type: non-blocking
- Trigger: AC4 requires exactly one confirmation e-mail per stored registration but does not say what happens when the SMTP server is unavailable.
- Options:
  1. (proposed default, conservative: no accepted registration is ever lost and no duplicate e-mail is sent) The registration is stored and the API answers 201 regardless; the confirmation is recorded as pending in the same transaction and sent by the backend's mail component, which retries pending confirmations until each is sent once. The failure is logged with the registration number only (SR-01).
  2. Roll back and answer 503 so the participant retries; registrations are impossible while SMTP is down.
- Human response: none
- Resolution: 1, pending review
