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
