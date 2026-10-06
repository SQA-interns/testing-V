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
    bootstrap: Maven wrapper plus a hand-written pom.xml whose parent is org.springframework.boot:spring-boot-starter-parent; root package si.confreg.registration
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

The local stack (both components, PostgreSQL, Mailpit) is started with `02_output/docker-compose.yml`.

## Constraints

Checkable rules the code must respect. Checked automatically in phase 6 where the stack allows (DoD-04).

| ID | Constraint |
|---|---|
| AR-01 | The frontend talks to the backend only through the REST API under `/api`. The frontend is a single registration form page. |
| AR-02 | The backend's internal architecture is not prescribed: the agent declares and justifies it in `docs/02_specification.md` and then writes ArchUnit rules that check exactly that declaration. |
| AR-03 | No package cycles in the backend (ArchUnit slice check). |
| AR-04 | Business values (fees, deadline, VAT rate, workshops) come from the configuration listed in `project/00_setup/environments.md` and are read where they are used. They are not hard-coded in code or tests. |
| AR-05 | Timestamps are stored in UTC. Every business date and deadline is interpreted in the conference time zone `APP_CONFERENCE_TZ` (Europe/Ljubljana). The backend obtains the current time from one clock component, which honours the test clock. |
| AR-06 | The database schema changes only through Flyway migrations. |
| AR-07 | E-mail is sent only through the backend's mail component to the configured SMTP server. |
| AR-08 | Invoicing is owned by the accounting team. Invoices are produced by a separate process of the accounting system, which is not part of this repository. |

## Interfaces

Every interface is defined as a contract in `docs/02_contracts/` in phase 2. The registration API below is fixed.

| Interface | Between | Style |
|---|---|---|
| Registration form | participant and frontend | UI |
| Registration API | frontend or organizer and backend | REST (OpenAPI) |
| Registration storage | backend and PostgreSQL | SQL (JPA) |
| E-mail | backend and SMTP server | SMTP |

### Fixed registration API

These names are fixed so that the experiment's independent test suite can call every implementation. The behaviour behind them is defined by the requirement files, not here.

- `POST /api/registrations` with a JSON body: `firstName`, `lastName`, `email`, `payerType` (`"private"` or `"company"`), `companyName`, `companyAddress`, `companyVatId`, `workshops` (array of workshop ids). A successful registration answers with a 2xx status and the stored registration.
- `GET /api/registrations/{registrationNumber}` returns the stored registration. Organizer only: HTTP Basic with `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD`.
- Stored registration (JSON): `registrationNumber`, `firstName`, `lastName`, `email`, `payerType`, `companyName`, `companyAddress`, `companyVatId`, `workshop` (id or null), `netFee`, `vat`, `grossFee`. Amounts are JSON numbers or decimal strings with two decimals.
- Test clock: when `APP_TEST_CLOCK=enabled`, the backend uses the instant in the request header `X-Test-Now` (ISO-8601, UTC) as the current time for that request. It is disabled by default, and the production profile refuses to start with it enabled.
