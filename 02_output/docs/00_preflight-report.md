# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run 1: 2026-10-06, starting commit `8b4fb643b0c36b7a8e36ef9d977dc7595e4ade61` (branch `tanej/confreg-C2`).

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | PASS: model, effort, template version, run id present |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | FAIL: JDK (D-01), Node/npm (D-02). Docker 29.8.0 and Compose 5.5.1 pass; container tools pass (cloc: D-05) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | PASS: Docker engine running; `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1` pulled and run; ports 8080, 8025, 5432 free |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | NOT CHECKED: presence check refused by the permission classifier (D-06). `.env` exists and is git-ignored |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | PASS: 30 Maven coordinates (HTTP 200 on Maven Central `.pom`), 24 npm packages (`npm view`), 8 container images (`docker manifest inspect`) |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | FAIL (frontend): 2 Critical, 5 High (D-03, D-04). Backend scan pending: OWASP Dependency-Check needs the build JDK (D-01) and `NVD_API_KEY` (D-06) |
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
| Maven runtime, test and plugin coordinates (30, incl. `apache-maven` 3.9.9, `pitest-junit5-plugin` 1.2.3) | as listed | yes | pending (backend scan, D-01/D-06) |
| vitest | 3.2.7 | yes | Critical (tinypool GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr; GHSA-82fw-gwwq-j7x9), D-03 |
| @vitest/coverage-v8 | 3.2.7 | yes | Moderate (via vitest), D-03 |
| jscpd | 4.3.0 | yes | High (braces GHSA-vfj7-8cjw-p6xm), D-04 |
| @stryker-mutator/core, vitest-runner | 10.0.0 | yes | Moderate (transitive `qs` via `typed-rest-client`) |
| all other npm entries (19) | as listed | yes | none |
| container images (8) | as listed | yes | not scanned (no image scanner in `tooling`) |

Re-audit with D-03 and D-04 option 1 (vitest and coverage-v8 5.0.3, jscpd 5.4.0): 0 Critical, 0 High, 2 Moderate (`qs`, `typed-rest-client`).

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-06T11:01Z | D-01 JDK, D-02 Node/npm, D-03 vitest, D-04 jscpd, D-06 secret check | pending | pending |
