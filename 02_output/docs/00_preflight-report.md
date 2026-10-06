# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `kyuhi-confreg-C2-r1`, starting commit `2bab314fc372c581a8b5b8d7f148355ec7722849`, checked 2026-10-06 (UTC).

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | pass: model, effort, template version and run id present |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | pass with D-01, D-02 (see "Platforms and tools") |
| Local environments and services running or reachable | `project/00_setup/environments.md` | pass: Docker daemon up (`docker info`); Docker Hub, Maven Central, npm registry reachable; `postgres` and `axllent/mailpit` images pulled and their binaries run in a container |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | pass: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `NVD_API_KEY` non-empty; `ORGANIZER_PASSWORD` ≥ 16 characters; `SMTP_USERNAME`, `SMTP_PASSWORD` marked "not needed" (also non-empty) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | pass: all resolve at the exact version (see "Dependency results") |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | pass after D-04, D-05: backend no result with CVSS ≥ 7 (CVE-2025-7962 suppressed for `angus-activation` only); frontend 0 Critical, 0 High; remaining Medium/Moderate listed below |
| Clean working tree on the starting commit | repository | pass: `git status --porcelain` empty apart from ignored `.env`, HEAD = starting commit |
| Input manifest written | `docs/00_input-manifest.sha256` | pass: 26 files (all of `01_input/`, `AGENTS.md`, `README.md`, `03_statistics/metrics.md`, `03_statistics/run-log.template.json`, `03_statistics/usage.md`), LF-normalised |

## Methods

- Secrets: `sed -n 's/^KEY=//p' .env | tr -d ' \r"'"'"'' | grep -q .` per key; length checked inside one command. No value displayed or logged.
- Versions: each tool's version command; second check for any mismatch (D-01: `docker version` and `docker info`; D-02: `--version` and a shell inside the image).
- Images: `docker manifest inspect` (resolves), then `docker pull` and the tool's version command in the container (runs).
- Maven: backend skeleton `./mvnw clean verify` and `./mvnw dependency:list -DincludeScope=test` (`02_output/logs/00_backend-build.log`, `00_backend-deps.log`).
- npm: `npm install` from the hand-written `package.json`, then `npm ls --depth=0` (`02_output/logs/00_frontend-install.log`).
- Scans: OWASP Dependency-Check 12.1.0 with `NVD_API_KEY` passed from `.env` (`02_output/logs/00_backend-dependency-check.log`, analysers per D-03); `npm audit` and `npm audit --omit=dev` (`02_output/logs/00_frontend-npm-audit.log`, `.json`).

## Platforms and tools

| id | pin | reported | Result |
|---|---|---|---|
| java | 21.0.10+7 | Temurin-21.0.10+7 | pass |
| node | 24.13.0 (npm 11.6.2) | v24.13.0, npm 11.6.2 | pass |
| docker | 29.8.0 | 29.8.1 | runs; D-01 |
| compose | 5.5.1 | v5.5.1 | pass |
| maven-wrapper | 3.3.2 | 3.3.2 (Apache Maven 3.9.9) | pass |
| spring-boot-maven-plugin | 4.1.1 | 4.1.1 (repackage ran) | pass |
| spotless-maven-plugin | 2.44.3 | 2.44.3 (`spotless:check` clean) | pass |
| spotbugs-maven-plugin | 4.10.4.1 | 4.10.4.1 (`spotbugs:check` 0 bugs) | pass |
| maven-pmd-plugin | 3.26.0 | 3.26.0 (`pmd:check`, `pmd:cpd-check` clean) | pass |
| jacoco-maven-plugin | 0.8.12 | 0.8.12 | pass |
| pitest-maven (+ junit5 plugin 1.2.3) | 1.30.0 | 1.30.0 (`mutationCoverage` ran) | pass |
| dependency-check-maven | 12.1.0 | 12.1.0 (scan completed) | pass; D-03 |
| vite | 6.4.3 | vite/6.4.3 | pass |
| typescript | 5.9.3 | 5.9.3 | pass |
| eslint, @eslint/js | 9.39.5 | v9.39.5 | pass |
| typescript-eslint | 8.70.1 | 8.70.1 | pass |
| eslint-plugin-react-hooks | 5.2.0 | 5.2.0 | pass |
| eslint-plugin-react-refresh | 0.4.26 | 0.4.26 | pass |
| prettier | 3.9.9 | 3.9.9 | pass |
| vitest, @vitest/coverage-v8 | 5.0.3 (D-05; was 3.2.7) | vitest/5.0.3 | pass |
| @playwright/test | 1.63.0 | 1.63.0; Chromium build 1243 already present on host | pass |
| @stryker-mutator/core, vitest-runner | 10.0.0 | 10.0.0 | pass |
| jscpd | 5.4.0 (D-05; was 4.3.0) | 5.4.0 (ran on `src`) | pass |
| npm audit | 11.6.2 | npm 11.6.2 | pass |
| semgrep/semgrep | 1.177.0 | 1.177.0 | pass |
| zricethezav/gitleaks | v8.30.1 | v8.30.1 | pass |
| aldanial/cloc | 2.10 | 1.98 | runs; D-02 |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 | yes | none (Tomcat overridden to 11.0.26) |
| org.springframework.boot:spring-boot-starter-data-jpa | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 | yes | Medium: CVE-2025-15104 (5.3) on transitive `hibernate-validator` 9.1.3; CPE `validator:validator` is Nu Html Checker, likely false positive |
| org.springframework.boot:spring-boot-starter-mail | 4.1.1 | yes | none after D-04: CVE-2025-7962 (7.5) on transitive `angus-activation` 2.0.3 classified false positive (Low) and suppressed for that artifact only |
| org.springframework.boot:spring-boot-starter-actuator | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-security | 4.1.1 | yes | none (Security 7.1.1) |
| org.springframework.boot:spring-boot-starter-flyway | 4.1.1 | yes | none |
| org.flywaydb:flyway-core | 12.4.0 | yes | none |
| org.flywaydb:flyway-database-postgresql | 12.4.0 | yes | none |
| org.postgresql:postgresql | 42.7.13 | yes | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes | none |
| org.springframework.boot:spring-boot-starter-test | 4.1.1 | yes (JUnit Jupiter 6.0.3, Mockito 5.23.0) | none |
| org.springframework.boot:spring-boot-starter-webmvc-test | 4.1.1 | yes | none |
| org.testcontainers:testcontainers-junit-jupiter | 2.0.5 | yes | none |
| org.testcontainers:testcontainers-postgresql | 2.0.5 | yes | none |
| com.tngtech.archunit:archunit-junit5 | 1.3.2 | yes | none |
| react | 19.3.0 | yes | none |
| react-dom | 19.3.0 | yes | none |
| @types/react | 19.3.0 | yes | none |
| @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react | 16.3.3 | yes | none |
| @testing-library/jest-dom | 6.9.1 | yes | none |
| jsdom | 26.1.0 | yes | none |
| globals | 15.15.0 | yes | none |
| vitest, @vitest/coverage-v8 (tooling) | 5.0.3 (D-05) | yes | none (3.2.7 had Critical via `tinypool`) |
| jscpd (tooling) | 5.4.0 (D-05) | yes | none (4.3.0 had High via `braces`) |
| @stryker-mutator/core (tooling) | 10.0.0 | yes | Moderate: `qs` via `typed-rest-client` (2 advisories, dev-only, not blocking) |
| postgres (image) | 16.15-alpine | yes, PostgreSQL 16.15 | not scanned (no image scanner listed) |
| eclipse-temurin (image) | 21.0.10_7-jre-alpine | yes, 21.0.10+7 | not scanned |
| node (image) | 24.13.0-alpine | yes, v24.13.0 | not scanned |
| nginx (image) | 1.30.5-alpine | yes, nginx/1.30.5 | not scanned |
| axllent/mailpit (image) | v1.31.1 | yes, v1.31.1 | not scanned |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-06 (phase 0) | D-04: accept CVE-2025-7962 as false positive; D-05: accept or upgrade vulnerable frontend dev tooling | D-04 option 1, D-05 option 2 (2026-10-06T22:43Z) | Dependency-Check re-run: CVE suppressed, nothing ≥ 7; `npm install`, `npm run check`/`build`/`test` pass; `npm audit` 0 Critical/High, `--omit=dev` 0 |

## Notes

- The `ORGANIZER_USERNAME` value is also a substring of the run id and the repository path, so it appears in files that contain those (run log, Maven logs). This is not a leak of the secret; the phase 6 secret-leak check must account for it.
