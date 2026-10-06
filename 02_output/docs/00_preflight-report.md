# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `tanej-confreg-C1-r1`, starting commit `9deb9569337af90b061eba8a72b80ee53778f99d`, checked 2026-10-06 (UTC). Raw outputs: `out/logs/00_preflight/`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. Model, effort, template version and run id present; read from the file. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | **Fail**: `java` (D-01) and `node`/npm (D-02), each confirmed two ways. Docker 29.8.0 and Compose 5.5.1 pass. Container tools pass (semgrep 1.177.0, gitleaks v8.30.1); `aldanial/cloc:2.10` reports 1.98 (D-03, non-blocking). Maven and npm tooling resolve; they run once the platforms pass. |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker daemon 29.8.0 (linux) up; `postgres:16.15-alpine` reports 16.15; `axllent/mailpit:v1.31.1` reports v1.31.1; `nginx:1.30.5-alpine` reports 1.30.5; `eclipse-temurin:21.0.10_7-jre-alpine` reports Temurin-21.0.10+7. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `NVD_API_KEY` non-empty (skill step 4 `sed … \| grep -q .`; second check: key names with masked empty/non-empty flag). `ORGANIZER_PASSWORD` is at least 16 characters (length only). `SMTP_USERNAME`, `SMTP_PASSWORD` empty, which is allowed (marked "not needed"). `.env` is git-ignored. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. 60/60 pins (dependencies and tooling): Maven via HTTP HEAD on the `.pom` at Maven Central, npm via `npm view <id>@<version> version`, containers via `docker manifest inspect`. Also Maven 3.9.9 distribution and `pitest-junit5-plugin` 1.2.3. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Fail**: npm Critical/High (D-04); Maven Critical/High on test-scope Testcontainers artifacts (D-07 false positive, D-08 no fix available). Method: D-05; disabled analysers: D-06. |
| Clean working tree on the starting commit | repository | Pass. `git status --porcelain` showed only the new `03_statistics/run-log.json`; HEAD `9deb956` (`inputs C1`), branch `tanej/confreg-C1`. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 25 files (everything under `01_input/`, `AGENTS.md`, `README.md`, and the `03_statistics/` files in sections 1 and 2), LF-normalised SHA-256. |

## Dependency results

The scan covers the `dependencies` set of `tech-stack.md`. npm: `npm audit` with npm 11.6.2 in `node:24.13.0-alpine` over all npm dependencies and tooling. Maven: `dependency-check-maven` 12.1.0 with Maven 3.9.9 in `eclipse-temurin:21.0.10_7-jre-alpine`, NVD data with `NVD_API_KEY`, test scope included, 107 jars incl. transitives. Container images are not covered by either listed scanner. Rows not listed below resolve with no finding.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| vitest | 3.2.7 | yes | **Critical**: tinypool GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr; @vitest/mocker GHSA-82fw-gwwq-j7x9 (D-04) |
| @vitest/coverage-v8 | 3.2.7 | yes | **Critical** through vitest (D-04) |
| jscpd | 4.3.0 | yes | **High**: braces GHSA-vfj7-8cjw-p6xm, CVSS 7.5 (D-04) |
| @stryker-mutator/core | 10.0.0 | yes | Medium: qs through typed-rest-client (GHSA-q8mj-m7cp-5q26, GHSA-x5fp-wj9c-mxmx, GHSA-4mjr-xmp4-gh2g) |
| org.testcontainers:testcontainers-postgresql | 2.0.5 | yes | **Critical** as reported (CVE-2015-0244 and others); false-positive CPE `postgresql:postgresql:2.0.5` (D-07) |
| org.testcontainers:testcontainers-junit-jupiter | 2.0.5 | yes | **Critical** through docker-java-transport-zerodep 3.7.1 (shaded httpclient5 5.5.1, CVE-2026-71290 9.1); High CVE-2026-54399, CVE-2026-54428 (shaded httpcore5 5.3.6) (D-08) |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 | yes | Medium: hibernate-validator 9.1.3.Final CVE-2025-15104 (6.9) |
| org.springframework.boot:spring-boot-starter-mail | 4.1.1 | yes | Medium: angus-activation 2.0.3 CVE-2025-7962 (6.0) |
| all other dependencies and tooling (51) | as listed | yes | none |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-06T18:58Z | D-01 Temurin JDK 21.0.10+7; D-02 Node.js 24.13.0 / npm 11.6.2; D-04 vitest/jscpd vulnerabilities; D-07, D-08 Testcontainers scan findings | pending | |
