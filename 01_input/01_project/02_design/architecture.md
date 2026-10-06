# Architecture

> Owner: Architect · Read in: phases 0, 2, 4, 6, 7 · Agent: read-only

## Components

Each component gets its own folder and README in `02_output/`. Versions come only from `tech-stack.md`; reference its ids, never repeat a version here.

```yaml
components:
  - name: backend
    type: api
    folder: 02_output/backend
    platform: java
    build: maven-wrapper
    bootstrap: Maven wrapper plus a hand-written pom.xml whose parent is org.springframework.boot:spring-boot-starter-parent; root package si.konferenca.registration
    commands:          # ES-05; filled in phase 0 if left empty
      build:
      test:
      check:
      run:
    depends_on: []
    deploys_as: container image (eclipse-temurin base, non-root)
  - name: frontend
    type: web-app
    folder: 02_output/frontend
    platform: node
    build: vite
    bootstrap: hand-written package.json with the exact tech-stack.md versions and a committed package-lock.json (no project generator)
    commands:
      build:
      test:
      check:
      run:
    depends_on: [backend]
    deploys_as: container image (static files served by nginx)
```

The local stack (both components, PostgreSQL, mail catcher) is started with `02_output/docker-compose.yml`.

## Constraints

Checkable rules the code must respect (layering, allowed dependencies, patterns). Checked automatically in phase 6 where the stack allows (DoD-04).

| ID | Constraint |
|---|---|
| AR-01 | The frontend talks to the backend only through the REST API under `/api`. |
| AR-02 | The backend's internal architecture is not prescribed: the agent declares and justifies it in `docs/02_specification.md` and then writes ArchUnit rules that check exactly that declaration. |
| AR-03 | No package cycles in the backend (ArchUnit slice check). |
| AR-04 | Conference options change through configuration only, without code changes and without changing the fixed participant fields (BR-01). The mechanism is decided in phase 2. |
| AR-05 | A success response is sent only after both the database row and the JSON copy are written (BR-06, BR-07). |
| AR-06 | The database schema changes only through Flyway migrations (ES-08). |
| AR-07 | The frontend contains no secret; the reCAPTCHA site key is its only key and comes from configuration. |

## Interfaces

Every interface between components or with external systems is defined as a contract in `docs/02_contracts/` in phase 2.

| Interface | Between | Style (e.g. REST, gRPC, events, file, UI) |
|---|---|---|
| Registration form | participant and frontend | UI |
| Options, registration, export | frontend or organizer and backend | REST (OpenAPI) |
| Registration storage | backend and PostgreSQL | SQL (JPA) |
| JSON copy | backend and persistent volume | file (JSON schema) |
| Conference options | configuration and backend | file or configuration (schema defined in phase 2) |
| Emails | backend and SMTP server | SMTP |
| Anti-automation | frontend, backend and Google reCAPTCHA | JavaScript widget, HTTPS verify call |
