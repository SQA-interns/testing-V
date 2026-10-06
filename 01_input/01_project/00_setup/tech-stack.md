# Tech stack

> Owner: Architect · Read in: phases 0, 2, 4, 6 · Agent: read-only

The single source of truth for every platform, dependency and tool version.
Build manifests and lock files in `02_output/` (e.g. `pom.xml`, `package.json`, `build.gradle`, `*.csproj`, `Podfile`) must match this file exactly; the phase 0 gate checks it. Resolvability and vulnerability results are recorded by the agent in `docs/00_preflight-report.md`.

## Rules

- Exact versions only: no ranges, wildcards, `latest` or floating tags.
- Changing any entry below is a blocking decision (`general/working-rules.md`). The human's approval in the decision record replaces the entry; nobody edits this file mid-run.
- A pin that resolves and runs but whose tool reports another version (for example an image tag) is a non-blocking decision; the listed pin stays authoritative. A pin that does not resolve or run is blocking.
- A dependency or tool not listed here may be added only with an exact version and a non-blocking decision record; it is scanned in phase 6 like any other.
- Every dependency's licence must be compatible with the project's licence.
- A `platforms` or `tooling` entry may carry `version_check` (a command whose output contains the version) and, for `tooling`, `options` (flags, analysers or rule sets to enable or disable, for example analysers that need credentials not listed in `secrets.env.example`). Preflight uses them.
- `tooling` must include at least one tool for each purpose required by `general/quality/` and `general/security/`: build, format, lint, type check (if the language has one), test, coverage, mutation, static analysis, dependency scan, secret scan.

Project licence: proprietary. Allowed dependency licences: MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC, PostgreSQL; EPL-2.0 for test scope only; GPL-2.0-with-classpath-exception for the Java runtime image only.

Versions verified to resolve on 2026-09-29. Spring Boot 4.1.1 manages the Spring libraries (Framework 7.0.9, Security 7.1.1); the tomcat, log4j and commons-lang3 entries override Boot-managed versions for known CVEs.

## Platforms

```yaml
platforms:
  - { id: java, name: Eclipse Temurin JDK, version: 21.0.10+7, source: https://adoptium.net }
  - { id: node, name: Node.js (with npm 11.6.2), version: 24.13.0, source: https://nodejs.org }
  - { id: docker, name: Docker Engine, version: 29.8.0, source: https://docs.docker.com/engine/install }
  - { id: compose, name: Docker Compose, version: 5.5.1, source: https://docs.docker.com/compose }
```

## Dependencies

One entry per artifact, written the way the ecosystem writes coordinates.

```yaml
dependencies:
  # backend, runtime (Maven Central: https://repo.maven.apache.org/maven2)
  - { id: org.springframework.boot:spring-boot-starter-parent, ecosystem: maven, version: 4.1.1, scope: build, component: backend, source: maven-central, purpose: parent and version management, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-webmvc, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: REST API, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-data-jpa, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: persistence (Hibernate), license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-validation, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: bean validation, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-mail, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: email, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-actuator, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: health and readiness, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-security, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: headers, CORS, organizer access control, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-flyway, ecosystem: maven, version: 4.1.1, scope: runtime, component: backend, source: maven-central, purpose: Flyway auto-configuration, license: Apache-2.0 }
  - { id: org.flywaydb:flyway-core, ecosystem: maven, version: 12.4.0, scope: runtime, component: backend, source: maven-central, purpose: database migrations, license: Apache-2.0 }
  - { id: org.flywaydb:flyway-database-postgresql, ecosystem: maven, version: 12.4.0, scope: runtime, component: backend, source: maven-central, purpose: Flyway PostgreSQL support, license: Apache-2.0 }
  - { id: org.postgresql:postgresql, ecosystem: maven, version: 42.7.13, scope: runtime, component: backend, source: maven-central, purpose: JDBC driver, license: BSD-2-Clause }
  - { id: org.apache.poi:poi-ooxml, ecosystem: maven, version: 5.5.1, scope: runtime, component: backend, source: maven-central, purpose: Excel export, license: Apache-2.0 }
  - { id: org.apache.tomcat.embed:tomcat-embed-core, ecosystem: maven, version: 11.0.26, scope: runtime, component: backend, source: maven-central, purpose: CVE override of the Boot-managed version, license: Apache-2.0 }
  - { id: org.apache.logging.log4j:log4j-api, ecosystem: maven, version: 2.26.1, scope: runtime, component: backend, source: maven-central, purpose: CVE override (pulled in by POI), license: Apache-2.0 }
  - { id: org.apache.commons:commons-lang3, ecosystem: maven, version: 3.20.0, scope: runtime, component: backend, source: maven-central, purpose: CVE override, license: Apache-2.0 }
  # backend, test
  - { id: org.springframework.boot:spring-boot-starter-test, ecosystem: maven, version: 4.1.1, scope: test, component: backend, source: maven-central, purpose: JUnit Jupiter 6.0.3, Mockito 5.23.0, AssertJ, license: Apache-2.0 }
  - { id: org.springframework.boot:spring-boot-starter-webmvc-test, ecosystem: maven, version: 4.1.1, scope: test, component: backend, source: maven-central, purpose: MockMvc auto-configuration, license: Apache-2.0 }
  - { id: org.testcontainers:testcontainers-junit-jupiter, ecosystem: maven, version: 2.0.5, scope: test, component: backend, source: maven-central, purpose: container-based integration tests, license: MIT }
  - { id: org.testcontainers:testcontainers-postgresql, ecosystem: maven, version: 2.0.5, scope: test, component: backend, source: maven-central, purpose: PostgreSQL test container, license: MIT }
  - { id: com.tngtech.archunit:archunit-junit5, ecosystem: maven, version: 1.3.2, scope: test, component: backend, source: maven-central, purpose: architecture rules (AR), license: Apache-2.0 }
  # frontend (npm registry: https://registry.npmjs.org)
  - { id: react, ecosystem: npm, version: 19.3.0, scope: runtime, component: frontend, source: npm, purpose: UI, license: MIT }
  - { id: react-dom, ecosystem: npm, version: 19.3.0, scope: runtime, component: frontend, source: npm, purpose: UI rendering, license: MIT }
  - { id: "@types/react", ecosystem: npm, version: 19.3.0, scope: build, component: frontend, source: npm, purpose: types, license: MIT }
  - { id: "@types/react-dom", ecosystem: npm, version: 19.3.0, scope: build, component: frontend, source: npm, purpose: types, license: MIT }
  - { id: "@vitejs/plugin-react", ecosystem: npm, version: 4.7.0, scope: build, component: frontend, source: npm, purpose: Vite React support, license: MIT }
  - { id: "@testing-library/react", ecosystem: npm, version: 16.3.3, scope: test, component: frontend, source: npm, purpose: component tests, license: MIT }
  - { id: "@testing-library/jest-dom", ecosystem: npm, version: 6.9.1, scope: test, component: frontend, source: npm, purpose: DOM assertions, license: MIT }
  - { id: jsdom, ecosystem: npm, version: 26.1.0, scope: test, component: frontend, source: npm, purpose: test DOM, license: MIT }
  - { id: globals, ecosystem: npm, version: 15.15.0, scope: build, component: frontend, source: npm, purpose: ESLint globals, license: MIT }
  # container images (Docker Hub: https://hub.docker.com)
  - { id: postgres, ecosystem: container, version: 16.15-alpine, scope: runtime, component: all, source: docker-hub, purpose: PostgreSQL 16 database, license: PostgreSQL }
  - { id: eclipse-temurin, ecosystem: container, version: 21.0.10_7-jre-alpine, scope: runtime, component: backend, source: docker-hub, purpose: backend base image, license: GPL-2.0-with-classpath-exception }
  - { id: node, ecosystem: container, version: 24.13.0-alpine, scope: build, component: frontend, source: docker-hub, purpose: frontend build stage, license: MIT }
  - { id: nginx, ecosystem: container, version: 1.30.5-alpine, scope: runtime, component: frontend, source: docker-hub, purpose: serves the frontend, license: BSD-2-Clause }
  - { id: axllent/mailpit, ecosystem: container, version: v1.31.1, scope: test, component: all, source: docker-hub, purpose: local SMTP catcher, license: MIT }
```

## Tooling

```yaml
tooling:
  # backend
  - { id: maven-wrapper, ecosystem: maven, version: 3.3.2, purpose: build (downloads Apache Maven 3.9.9), component: backend, source: maven-central }
  - { id: org.springframework.boot:spring-boot-maven-plugin, ecosystem: maven, version: 4.1.1, purpose: build, component: backend, source: maven-central }
  - { id: com.diffplug.spotless:spotless-maven-plugin, ecosystem: maven, version: 2.44.3, purpose: format (google-java-format), component: backend, source: maven-central }
  - { id: com.github.spotbugs:spotbugs-maven-plugin, ecosystem: maven, version: 4.10.4.1, purpose: static-analysis, component: backend, source: maven-central }
  - { id: org.apache.maven.plugins:maven-pmd-plugin, ecosystem: maven, version: 3.26.0, purpose: lint, static-analysis, duplication (CPD), component: backend, source: maven-central }
  - { id: org.jacoco:jacoco-maven-plugin, ecosystem: maven, version: 0.8.12, purpose: coverage, component: backend, source: maven-central }
  - { id: org.pitest:pitest-maven, ecosystem: maven, version: 1.30.0, purpose: mutation (with org.pitest:pitest-junit5-plugin 1.2.3), component: backend, source: maven-central }
  - { id: org.owasp:dependency-check-maven, ecosystem: maven, version: 12.1.0, purpose: dependency-scan (needs NVD_API_KEY), component: backend, source: maven-central }
  # frontend
  - { id: vite, ecosystem: npm, version: 6.4.3, purpose: build, component: frontend, source: npm }
  - { id: typescript, ecosystem: npm, version: 5.9.3, purpose: type-check (tsc --noEmit), component: frontend, source: npm }
  - { id: eslint, ecosystem: npm, version: 9.39.5, purpose: lint, component: frontend, source: npm }
  - { id: "@eslint/js", ecosystem: npm, version: 9.39.5, purpose: lint, component: frontend, source: npm }
  - { id: typescript-eslint, ecosystem: npm, version: 8.70.1, purpose: lint, component: frontend, source: npm }
  - { id: eslint-plugin-react-hooks, ecosystem: npm, version: 5.2.0, purpose: lint, component: frontend, source: npm }
  - { id: eslint-plugin-react-refresh, ecosystem: npm, version: 0.4.26, purpose: lint, component: frontend, source: npm }
  - { id: prettier, ecosystem: npm, version: 3.9.9, purpose: format, component: frontend, source: npm }
  - { id: vitest, ecosystem: npm, version: 3.2.7, purpose: test (unit, component), component: frontend, source: npm }
  - { id: "@vitest/coverage-v8", ecosystem: npm, version: 3.2.7, purpose: coverage, component: frontend, source: npm }
  - { id: "@playwright/test", ecosystem: npm, version: 1.63.0, purpose: test (end-to-end, with its bundled Chromium), component: frontend, source: npm }
  - { id: "@stryker-mutator/core", ecosystem: npm, version: 10.0.0, purpose: mutation, component: frontend, source: npm }
  - { id: "@stryker-mutator/vitest-runner", ecosystem: npm, version: 10.0.0, purpose: mutation, component: frontend, source: npm }
  - { id: jscpd, ecosystem: npm, version: 4.3.0, purpose: duplication, component: frontend, source: npm }
  - { id: npm audit (npm 11.6.2), ecosystem: npm, version: 11.6.2, purpose: dependency-scan, component: frontend, source: npm }
  # all components (run as containers, nothing installed on the host)
  - { id: semgrep/semgrep, ecosystem: container, version: 1.177.0, purpose: static-analysis (security rules), component: all, source: docker-hub }
  - { id: zricethezav/gitleaks, ecosystem: container, version: v8.30.1, purpose: secret-scan, component: all, source: docker-hub }
  - { id: aldanial/cloc, ecosystem: container, version: "2.10", purpose: code-metrics, component: all, source: docker-hub }
```
