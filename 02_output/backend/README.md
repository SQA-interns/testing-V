# Backend

> Written in: phase 0 (skeleton), completed in phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Conference registration API (Spring Boot), root package `si.confreg.registration`.

## Commands (ES-05)

Run from `02_output/backend/`.

| Purpose | Command |
|---|---|
| build | `./mvnw -B package -DskipTests` |
| test | `./mvnw -B verify` |
| check (format, lint, static analysis) | `./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check` |
| run | `docker compose up` in `02_output/` (needs PostgreSQL and Mailpit) |
| dependency scan | `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` with `NVD_API_KEY` exported from `.env` |
