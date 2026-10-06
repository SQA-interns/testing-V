# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `kyuhi-confreg-C1-r1`, starting commit `a33129ca3ce82866e3fa4652ad8894707ab85a9b`, checked 2026-10-06 (UTC). Logs are in `out/logs/p0-*`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass: model, effort, template version and run id present (read the file) |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass after re-check: Docker 29.8.1 accepted (D-08), Playwright Chromium installed (D-11); non-blocking D-02. Details below |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass: Docker daemon answers (`docker info`); Docker Hub, Maven Central and the npm registry reachable; `postgres` and `axllent/mailpit` images pulled and run |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass: all 6 keys non-empty (POSTGRES_PASSWORD, ORGANIZER_USERNAME, ORGANIZER_PASSWORD, SMTP_USERNAME, SMTP_PASSWORD, NVD_API_KEY). Method 1: `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .`; method 2: key names listed with values masked. The SMTP keys are marked "not needed". NVD_API_KEY was first rejected by NVD (D-04); after the human updated it (D-09) the NVD API answers HTTP 200 with it. **ORGANIZER_USERNAME still occurs in repository paths (D-13)** |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass: 20 Maven dependency poms plus 10 build or tool poms return HTTP 200 from Maven Central; 24 npm packages resolve with `npm view` (licences as listed); 8 container images pulled. Second method: backend `./mvnw dependency:list` and frontend `npm ls --depth=0` show exactly the pinned versions |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Open (D-14).** Frontend after D-10: 0 Critical, 0 High, 2 Moderate (`npm audit`). Backend (Dependency-Check, OSS Index off per D-03): CVE-2025-7962, CVSS v3 7.5, on `angus-activation` 2.0.3, evidence of a false positive awaiting approval (D-14); CVE-2025-15104 Medium false positive on `hibernate-validator` (D-15) |
| Clean working tree on the starting commit | repository | Pass: `git status --porcelain --ignored` at start showed only the ignored `.env`; HEAD = starting commit |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass: 25 files (all of `01_input/`, `AGENTS.md`, `README.md`, `03_statistics/metrics.md`, `run-log.template.json`, `usage.md`), SHA-256 after removing CR |

## Platforms and tools

| id | Pinned | Reported | Method | Result |
|---|---|---|---|---|
| java | 21.0.10+7 | Temurin-21.0.10+7 | `java -version` | pass |
| node | 24.13.0 (npm 11.6.2) | v24.13.0, npm 11.6.2 | `node --version`, `npm --version` | pass |
| docker | 29.8.0 | server 29.8.1, client 29.8.1 | `docker version`, `docker info` | pass: 29.8.1 accepted by the human (D-08) |
| compose | 5.5.1 | v5.5.1 | `docker compose version` | pass |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | wrapper 3.3.2 only-script, Maven 3.9.9 | `./mvnw -B package` | pass |
| spring-boot-maven-plugin | 4.1.1 | 4.1.1 | `repackage` in build log | pass |
| spotless-maven-plugin | 2.44.3 | 2.44.3 | `spotless:check` | pass |
| spotbugs-maven-plugin | 4.10.4.1 | 4.10.4.1 | `spotbugs:check` | pass |
| maven-pmd-plugin | 3.26.0 | 3.26.0 | `pmd:check`, `pmd:cpd-check` | pass |
| jacoco-maven-plugin | 0.8.12 | 0.8.12 | `verify` (prepare-agent, report) | pass |
| pitest-maven (+ junit5 plugin 1.2.3) | 1.30.0 | 1.30.0 | `pitest:help`; junit5 plugin pom resolves | pass |
| dependency-check-maven | 12.1.0 | 12.1.0 runs | `check` | pass after D-09: NVD data updated, 67 dependencies scanned |
| vite | 6.4.3 | 6.4.3 | `vite --version` | pass |
| typescript | 5.9.3 | 5.9.3 | `tsc --version` | pass |
| eslint, @eslint/js | 9.39.5 | 9.39.5 | `eslint --version` | pass |
| typescript-eslint | 8.70.1 | 8.70.1 | package version | pass |
| eslint-plugin-react-hooks | 5.2.0 | 5.2.0 | package version | pass |
| eslint-plugin-react-refresh | 0.4.26 | 0.4.26 | package version | pass |
| prettier | 3.9.9 | 3.9.9 | `prettier --version` | pass |
| vitest, @vitest/coverage-v8 | 5.0.3 (D-10; was 3.2.7) | 5.0.3 | `vitest --version`, `npm ls` | pass |
| @playwright/test | 1.63.0 | 1.63.0 | `playwright --version` | pass: Chromium chromium-1243 installed with `npx playwright install chromium` (D-11) |
| @stryker-mutator/core, vitest-runner | 10.0.0 | 10.0.0 | `stryker --version` | pass |
| jscpd | 5.4.0 (D-10; was 4.3.0) | 5.4.0 | `jscpd --version` | pass |
| npm audit | 11.6.2 | 11.6.2 | `npm --version` | pass |
| semgrep/semgrep | 1.177.0 | 1.177.0 | `semgrep --version` in container | pass |
| zricethezav/gitleaks | v8.30.1 | v8.30.1 | `gitleaks version` in container | pass |
| aldanial/cloc | 2.10 | 1.98 | `cloc --version` in container | runs, other version (D-02, non-blocking) |

## Component bootstrap

| Component | Build | Test | Check | Run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package` | `./mvnw -B verify` | `./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` (end-to-end: `npm run test:e2e`) | `npm run check` | `npm run dev` |

Both skeletons build, test and check cleanly (logs `p0-backend-*.log`, `p0-frontend-*.log`).

## Dependency results

Maven artifacts resolve from Maven Central and npm packages from registry.npmjs.org.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-data-jpa | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-mail | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-actuator | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-security | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-flyway | 4.1.1 | yes | none |
| org.flywaydb:flyway-core | 12.4.0 | yes | none |
| org.flywaydb:flyway-database-postgresql | 12.4.0 | yes | none |
| org.postgresql:postgresql | 42.7.13 | yes | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes | none |
| org.springframework.boot:spring-boot-starter-test | 4.1.1 | yes | none |
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
| vitest (tooling) | 5.0.3 (D-10) | yes | none |
| @vitest/coverage-v8 (tooling) | 5.0.3 (D-10) | yes | none |
| jscpd (tooling) | 5.4.0 (D-10) | yes | none |
| transitive: org.eclipse.angus:angus-activation | 2.0.3 (Boot-managed) | yes | CVSS v3 7.5, CVE-2025-7962, false positive pending (D-14) |
| transitive: org.hibernate.validator:hibernate-validator | 9.1.3.Final (Boot-managed) | yes | Medium, CVE-2025-15104, false positive (D-15) |
| other npm tooling | as listed | yes | none, apart from Moderate `qs` and `typed-rest-client` (transitive) |
| postgres | 16.15-alpine | yes (pulled) | not scanned (no container scanner listed) |
| eclipse-temurin | 21.0.10_7-jre-alpine | yes (pulled) | not scanned (no container scanner listed) |
| node | 24.13.0-alpine | yes (pulled) | not scanned (no container scanner listed) |
| nginx | 1.30.5-alpine | yes (pulled) | not scanned (no container scanner listed) |
| axllent/mailpit | v1.31.1 | yes (pulled) | not scanned (no container scanner listed) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-06T17:39:03Z | D-01 Docker Engine version; D-04 valid NVD API key; D-05 vulnerable dev tooling; D-06 Playwright Chromium install; D-07 username values | D-01 accept 29.8.1; D-04 key updated; D-05 vitest and coverage-v8 5.0.3; D-06 install Chromium; D-07 credentials updated (D-08 to D-12, 2026-10-06T17:46:32Z) | D-01, D-04, D-05, D-06 pass; D-07 partly: ORGANIZER_USERNAME unchanged (D-13) |
| 2026-10-06T17:48:42Z | D-13 organizer username; D-14 approval of the CVE-2025-7962 false positive | pending | pending |
