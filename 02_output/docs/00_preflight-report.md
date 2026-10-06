# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `tanej-confreg-C0-r1`, starting commit `dc8b146309f2597ec60bcbc235616a3865ec3a8a`, first pass 2026-10-06T14:00Z–14:04Z.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | pass: model, effort, template version and run id present |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | pass after D-01/D-02 (human approved host Oracle JDK 21.0.11+9 and Node.js 24.10.0/npm 10.9.4); docker, compose, container tools, Maven plugins and npm tools run (`out/logs/00_backend-check.log`, `00_backend-test-tools.log`, `00_frontend-tools.log`); cloc reports another version (D-03, non-blocking) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | pass: Docker Engine 29.8.0 (linux) running; Docker Hub reachable; Mailpit and PostgreSQL images pull |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | pass: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `NVD_API_KEY` non-empty; `SMTP_USERNAME`, `SMTP_PASSWORD` marked not needed (empty); `.env` is git-ignored |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | pass: 31 Maven, 24 npm and 8 container coordinates resolve (`out/logs/00_resolve.log`) |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | pass after D-05: `npm audit` 0 Critical/High (vitest, coverage-v8 → 5.0.3, jscpd → 5.4.0); Dependency-Check 0 open of 112 (49 suppressed with evidence, D-05); OSS Index analyser disabled (D-04) |
| Clean working tree on the starting commit | repository | pass: HEAD `dc8b146`, only untracked `03_statistics/run-log.json` |
| Input manifest written | `docs/00_input-manifest.sha256` | pass: 25 files, LF-normalised |

## Platform and tool results

| id | pinned | reported | method (first / second) | Result |
|---|---|---|---|---|
| java | Temurin 21.0.10+7 | Oracle 21.0.11+9-LTS | `java -version` / `where.exe java` + install-folder listing | fail, then approved (D-01) |
| node | 24.13.0, npm 11.6.2 | 24.10.0, npm 10.9.4 | `node --version`, `npm --version` / `where.exe node`, direct call of `node.exe` | fail, then approved (D-02) |
| docker | 29.8.0 | 29.8.0 | `docker version` | pass |
| compose | 5.5.1 | v5.5.1 | `docker compose version` | pass |
| semgrep/semgrep | 1.177.0 | 1.177.0 | `semgrep --version` in image | pass |
| zricethezav/gitleaks | v8.30.1 | v8.30.1 | `gitleaks version` in image | pass |
| aldanial/cloc | 2.10 | 1.98 | `--version` in image / image inspect + re-run | pass with D-03 |
| axllent/mailpit | v1.31.1 | v1.31.1 | `mailpit version` in image | pass |
| node image | 24.13.0-alpine | v24.13.0, npm 11.6.2 | `node --version; npm --version` in image | pass |
| Maven plugins (wrapper 3.3.2 → Maven 3.9.9, spring-boot, spotless, spotbugs, pmd/cpd, jacoco, pitest, dependency-check) | as pinned | as pinned | goals run on the skeleton; `mvnw -v` reports 3.9.9 | pass |
| npm tools (vite, tsc, eslint, prettier, vitest, coverage-v8, playwright, stryker, jscpd) | as pinned; vitest/coverage-v8 5.0.3, jscpd 5.4.0 per D-05 | as pinned; Playwright Chromium v1243 present | `--version` of each; `npm ls`; build, check, test, coverage, jscpd run | pass |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| all 31 Maven coordinates (dependencies and plugins) | as pinned; `dependency:list` matches (`out/logs/00_backend-deps.txt`) | yes (HTTP 200 on the `.pom` at Maven Central) | none open; 49 suppressed with evidence (D-05 a–c, CPE mismatch on hibernate-validator) |
| all 24 npm packages | as pinned; `package-lock.json` committed | yes (HTTP 200 on registry version document) | runtime: none; dev: 2 Moderate (`qs` via Stryker) after the D-05 upgrade |
| all 8 container images | as pinned | yes (`docker manifest inspect`) | scanned in phase 6 (no image scanner listed in `tech-stack.md`) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-06T14:04Z | D-01 pinned JDK; D-02 pinned Node.js/npm | 14:07Z: approve installed Oracle JDK 21.0.11+9 and Node.js 24.10.0 / npm 10.9.4 | 14:10Z: backend and frontend build and all tools run |
| 2026-10-06T14:19Z | D-05 Critical/High scan results in test/dev tooling | 14:24Z: upgrade vitest/coverage-v8/jscpd, classify backend items with evidence | 14:24Z: both scans pass |

## Component commands (ES-05)

`project/02_design/architecture.md` leaves them empty; these are the commands verified in phase 0.

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B verify` | `./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` (or `docker compose up` in `02_output/`) |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` (or `docker compose up` in `02_output/`) |

Also verified: `./mvnw -B dependency-check:check` (needs `NVD_API_KEY`), `./mvnw org.pitest:pitest-maven:mutationCoverage`, `npm run coverage`, `npm run mutation`, `npm run duplication`, `npm audit`.

## Gate

Passed 2026-10-06T14:25Z: every check passes (with D-01, D-02, D-05 approved by the human and D-03, D-04 non-blocking); both skeletons build; manifests and lock files match `tech-stack.md` as amended by D-01, D-02, D-05; every listed tool runs; input manifest written.
