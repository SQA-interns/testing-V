# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run 1: 2026-10-06, starting commit `8b4fb643b0c36b7a8e36ef9d977dc7595e4ade61` (branch `tanej/confreg-C2`).

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | PASS: model, effort, template version, run id present |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Run 1 FAIL: JDK (D-01), Node/npm (D-02). Run 2 PASS as amended: Oracle JDK 21.0.11 (D-01), Node 24.10.0 / npm 10.9.4 (D-02); every Maven and npm tool runs (see "Bootstrap") |
| Local environments and services running or reachable | `project/00_setup/environments.md` | PASS: Docker engine running; `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1` pulled and run; ports 8080, 8025, 5432 free |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Run 1 NOT CHECKED (D-06). Run 2 PASS: skill step 4 command; `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `NVD_API_KEY` set; `SMTP_*` not needed locally. Second method: OWASP scan authenticated to NVD with `NVD_API_KEY`. Organizer password length not inspected by the agent; human confirmed ≥ 16 characters (2026-10-06T11:17Z) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | PASS: 30 Maven coordinates (HTTP 200 on Maven Central `.pom`), 24 npm packages (`npm view`), 8 container images (`docker manifest inspect`) |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Run 1 FAIL (frontend: D-03, D-04). Run 2 frontend PASS: 0 Critical/High, 2 Moderate. Backend: OWASP Dependency-Check (NVD; OSS Index disabled, D-07): Run 3 PASS: CVE-2025-7962 classified Low (false positive, D-08) and suppressed for `angus-activation` 2.0.3 only; 1 Medium false positive (CVE-2025-15104) remains; raw report `out/logs/00_backend-depcheck-raw.json` |
| Clean working tree on the starting commit | repository | PASS: `git status --porcelain` empty except ignored `.env`; HEAD = starting commit, which contains the filled inputs |
| Input manifest written | `docs/00_input-manifest.sha256` | PASS: 26 files (21 under `01_input/`, 5 protected root files), LF-normalised |

## Platform and tool results

| id | Listed | Method 1 | Method 2 | Result |
|---|---|---|---|---|
| java | Temurin 21.0.10+7 | `java -version`: Oracle 21.0.11+9 | `which -a java`, `JAVA_HOME`, install folders: only Oracle 21.0.11, JRE 8 | FAIL (D-01) |
| node | 24.13.0 (npm 11.6.2) | `node --version`: 24.10.0; `npm --version`: 10.9.4 | `which -a node npm`; no version manager | FAIL (D-02) |
| docker | 29.8.0 | `docker version`: server and client 29.8.0 | – | PASS |
| compose | 5.5.1 | `docker compose version`: v5.5.1 | – | PASS |
| semgrep/semgrep | 1.177.0 | `semgrep --version`: 1.177.0 | – | PASS |
| zricethezav/gitleaks | v8.30.1 | `gitleaks version`: v8.30.1 | – | PASS |
| aldanial/cloc | 2.10 | `cloc --version`: 1.98 | re-run, image inspect | PASS with non-blocking D-05 |
| Maven and npm tooling | see `tech-stack.md` | resolve (below) | – | runs checked at bootstrap, after D-01/D-02 |

## Dependency results

Frontend scan: `npm audit` (npm 11.6.2 inside `node:24.13.0-alpine`) over a lock file generated from the exact pinned frontend dependencies and npm tooling.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| Maven runtime and test dependencies (66 jars scanned) | as listed | yes | High by CVSS v3 (CVE-2025-7962 on transitive `angus-activation` 2.0.3, false positive, D-08); Medium (CVE-2025-15104 on transitive `hibernate-validator` 9.1.3.Final: Nu Html Checker CVE, CPE false positive); others none |
| vitest | 3.2.7 → 5.0.3 (D-03) | yes | Critical (tinypool GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr; GHSA-82fw-gwwq-j7x9), D-03 |
| @vitest/coverage-v8 | 3.2.7 → 5.0.3 (D-03) | yes | Moderate (via vitest), D-03 |
| jscpd | 4.3.0 → 5.4.0 (D-04) | yes | High (braces GHSA-vfj7-8cjw-p6xm), D-04 |
| @stryker-mutator/core, vitest-runner | 10.0.0 | yes | Moderate (transitive `qs` via `typed-rest-client`) |
| all other npm entries (19) | as listed | yes | none |
| container images (8) | as listed | yes | not scanned (no image scanner in `tooling`) |

Re-audit with D-03 and D-04 option 1 (vitest and coverage-v8 5.0.3, jscpd 5.4.0): 0 Critical, 0 High, 2 Moderate (`qs`, `typed-rest-client`).

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-06T11:01Z | D-01 JDK, D-02 Node/npm, D-03 vitest, D-04 jscpd, D-06 secret check | 2026-10-06T11:06Z: D-01 opt 2, D-02 opt 2, D-03 opt 1, D-04 opt 1, D-06 opt 1 | run 2: all re-checked, pass |
| 2026-10-06T11:14Z | D-08 downgrade of CVE-2025-7962 | 2026-10-06T11:17Z: option 1; organizer password ≥ 16 characters confirmed | run 3: scan passes |

## Bootstrap (run 2)

| Component | Command | Result | Log |
|---|---|---|---|
| backend | `./mvnw -v` | Maven 3.9.9, Oracle JDK 21.0.11 | – |
| backend | `./mvnw verify` (build, test, JaCoCo) | BUILD SUCCESS | `out/logs/00_backend-verify.log` |
| backend | `./mvnw dependency:list` | every pin resolved exactly, incl. Boot-managed Spring 7.0.9, Security 7.1.1, JUnit 6.0.3, Mockito 5.23.0 | `out/logs/00_backend-deps.txt` |
| backend | `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | pass, 0 bugs | `out/logs/00_backend-check.log` |
| backend | `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage` | runs; skipped (no tests yet) | `out/logs/00_backend-pitest.log` |
| backend | `./mvnw org.owasp:dependency-check-maven:check` | runs (NVD), see D-07, D-08 | `out/logs/00_backend-depcheck.log` |
| frontend | `npm install` | exact versions installed, lock committed | `out/logs/00_frontend-install.log` |
| frontend | `npm run build`, `npm run check` (Prettier, ESLint, tsc) | pass | `out/logs/00_frontend-tools2.log` |
| frontend | `npm test`, `npm run test:coverage`, `npm run duplication`, `npm run mutation` | run (no tests yet) | `out/logs/00_frontend-tools.log`, `00_frontend-tools2.log` |
| frontend | `npx playwright --version` | 1.63.0; Chromium v1243 present | – |
| frontend | `npm audit` | 0 Critical, 0 High, 2 Moderate | – |
| all | gitleaks v8.30.1 `dir 02_output` | no leaks | – |

## Gate (2026-10-06T11:18Z)

Phase 0 gate PASSED: every preflight check passes (as amended by D-01 to D-04, D-07, D-08); both components build; `pom.xml` and `package.json`/`package-lock.json` match `tech-stack.md` as amended; every listed tool runs; input manifest written and re-verified (26/26 hashes match).

Component commands (ES-05; `architecture.md` leaves them empty):

| Component | build | test | check | run |
|---|---|---|---|---|
| backend | `./mvnw -DskipTests package` | `./mvnw verify` | `./mvnw compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` (or `docker compose up` in `02_output/`, phase 4) |
| frontend | `npm run build` | `npm test` (unit/acceptance), `npm run test:e2e` | `npm run check` | `npm run dev` |
