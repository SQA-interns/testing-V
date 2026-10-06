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
