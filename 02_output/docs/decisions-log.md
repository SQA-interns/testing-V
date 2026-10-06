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
- Human response: none
- Resolution: pending review

## D-02: Pinned Node.js 24.13.0 / npm 11.6.2 not installed on host
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `node` pins Node.js 24.13.0 with npm 11.6.2. `node --version` reports v24.10.0 and `npm --version` 10.9.4; second check (`which -a node npm`, no nvm/fnm/volta found) confirms only `C:\Program Files\nodejs` exists. The pin does not resolve on the host.
- Options:
  1. (proposed default) Human installs Node.js 24.13.0 (bundles npm 11.6.2) from https://nodejs.org; agent re-runs the check.
  2. Approve Node.js 24.10.0 / npm 10.9.4 as replacement for the `node` platform entry.
  3. Run npm commands inside the listed `node:24.13.0-alpine` image (verified: reports v24.13.0 / 11.6.2). Limitation: Playwright's bundled Chromium does not support Alpine (musl), so end-to-end tests would still need a host Node or another, non-listed image (needs approval).
- Human response: none
- Resolution: pending review

## D-03: vitest 3.2.7 has Critical vulnerabilities
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 6, `npm audit` (npm 11.6.2 in `node:24.13.0-alpine`) over the pinned frontend set: Critical in `vitest` 3.2.7 via `tinypool` (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr, prototype pollution to RCE) and `@vitest/mocker` (GHSA-82fw-gwwq-j7x9, path traversal); `@vitest/coverage-v8` 3.2.7 Moderate via `vitest`. Fix only in a major version.
- Options:
  1. (proposed default) Replace `vitest` and `@vitest/coverage-v8` 3.2.7 with 5.0.3. Checked: vitest 5.0.3 peer range accepts vite 6.4.3 and jsdom; `@stryker-mutator/vitest-runner` 10.0.0 accepts vitest >=2.0.0; engines accept Node 24. Re-audit of the set with this change and option 1 of D-04: 0 Critical, 0 High, 2 Moderate (`qs` via `typed-rest-client`).
  2. Keep 3.2.7 and accept the risk as test-only tooling that never ships (lowering Critical needs this written evidence and your approval).
- Human response: none
- Resolution: pending review

## D-04: jscpd 4.3.0 has High vulnerabilities
- Timestamp: 2026-10-06T11:01:08Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 6, same `npm audit` run: High in `jscpd` 4.3.0 via `@jscpd/finder` → `fast-glob` → `micromatch` → `braces` (GHSA-vfj7-8cjw-p6xm, CVSS 7.5, stack-exhaustion DoS). Fix only in a major version.
- Options:
  1. (proposed default) Replace `jscpd` 4.3.0 with 5.4.0 (engines `node >=18`). Re-audit result: see D-03 option 1.
  2. Keep 4.3.0 and accept the risk as a dev-only tool run on the project's own source (lowering High needs your approval).
  3. Drop `jscpd` and measure frontend duplication with PMD CPD (`maven-pmd-plugin`, already listed) in TypeScript mode.
- Human response: none
- Resolution: pending review

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
- Human response: none
- Resolution: pending review
