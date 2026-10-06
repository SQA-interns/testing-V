# Frontend

> Written in: phase 0 (skeleton), completed in phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

The single registration page (React, Vite, TypeScript). It talks to the backend only through `/api` (AR-01). The form follows `docs/02_contracts/registration-form.yaml`. In the container, nginx serves the built files as a non-root user, adds the security headers and, in the local stack, proxies `/api` to the backend.

## Prerequisites

Exact versions are in `01_input/01_project/00_setup/tech-stack.md`, as amended in `docs/decisions-log.md`.

- Node.js 24 with npm (this run: Node 24.10.0 with npm 10.9.4, approved in D-10; the pin is 24.13.0 with npm 11.6.2). The container build uses `node:24.13.0-alpine`.
- For the end-to-end tests: the local stack running (root README) and Playwright's Chromium (`npx playwright install chromium`, once per machine).

## Configuration

The frontend has no settings of its own. Business values (workshops) come from `GET /api/workshops` (D-28).

| Variable                                   | Default                                                            | Used by                                                                      |
| ------------------------------------------ | ------------------------------------------------------------------ | ---------------------------------------------------------------------------- |
| `E2E_BASE_URL`                             | none (Playwright starts the dev server on `http://127.0.0.1:5173`) | end-to-end tests; set to `http://127.0.0.1:3000` to test the built container |
| `E2E_API_URL`                              | `http://127.0.0.1:8080`                                            | end-to-end tests (organizer checks)                                          |
| `E2E_MAILPIT_URL`                          | `http://127.0.0.1:8025`                                            | end-to-end tests (e-mail checks)                                             |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none (secrets from `.env`)                                         | end-to-end tests                                                             |

## Commands (ES-05)

Run from `02_output/frontend/` after `npm ci`.

| Purpose                          | Command                                                                                                                                                                                                                              |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| build                            | `npm run build` (type check, then `dist/`)                                                                                                                                                                                           |
| test                             | `npm test` (unit and component tests); `npm run coverage` for coverage                                                                                                                                                               |
| check (format, lint, type check) | `npm run check`                                                                                                                                                                                                                      |
| format                           | `npm run format`                                                                                                                                                                                                                     |
| run                              | `npm run dev` (development server on 5173; proxies `/api` to `127.0.0.1:8080`), or `docker compose --env-file ../.env up --build` in `02_output/`                                                                                    |
| end-to-end                       | with the stack running: `export ORGANIZER_USERNAME="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../../.env \| tr -d '\r')" ORGANIZER_PASSWORD="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../../.env \| tr -d '\r')"`, then `npx playwright test` |
| duplication                      | `npx jscpd src`                                                                                                                                                                                                                      |
| dependency scan                  | `npm audit`                                                                                                                                                                                                                          |

Test levels: `e2e/` and `playwright.config.ts` are the frozen end-to-end tests (hashes in `docs/03_acceptance-manifest.sha256`; do not edit). `src/**/*.test.*` are unit and component tests. Mutation testing with Stryker (`stryker.config.json`) does not produce a valid score with Vitest 5.0.3 (D-34, D-35).

## Troubleshooting

| Symptom                                                                                                        | Cause and fix                                                                                                                   |
| -------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------- |
| The workshop list stays empty or registration shows "not possible right now"                                   | The backend is not reachable at `127.0.0.1:8080`, or the rate limit is reached. Start the stack and check `docker compose ps`.  |
| `npx playwright test` fails at `readRegistration` with "ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be set" | Export both from `.env` as shown above.                                                                                         |
| Playwright reports a missing browser executable                                                                | Run `npx playwright install chromium`.                                                                                          |
| `npm ci` fails on an internal npm error                                                                        | Delete `node_modules` and run `npm ci` again.                                                                                   |
| The browser console shows CSP errors after a change                                                            | The nginx CSP allows only same-origin resources (`default-src 'self'`). Bundle assets instead of loading them from other hosts. |
