# Frontend: registration form

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

Single React page with the conference registration form (US-001, AC-001-14/15). It talks to the backend only through `/api` (AR-01) and is served by nginx in its container. Contract: [`../docs/02_contracts/registration-form.yaml`](../docs/02_contracts/registration-form.yaml).

## Prerequisites

| Tool                    | Version                                                                   | Notes                                                  |
| ----------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------ |
| Node.js                 | 24 (run used 24.10.0 with npm 10.9.4, D-02; listed: 24.13.0 / npm 11.6.2) |                                                        |
| Docker                  | Engine 29.8.0, Compose 5.5.1                                              | container image and end-to-end tests against the stack |
| Chromium for Playwright | bundled with `@playwright/test` 1.63.0                                    | `npx playwright install chromium` once, if not present |

## Configuration

| Variable          | Default                                                | Used by                                                                                                                                             |
| ----------------- | ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `APP_WORKSHOPS`   | compose default `W1=…;W2=…;W3=…` (same as the backend) | written to `/config.js` at container start (`docker/40-runtime-config.sh`) and by the dev server (`vite.config.ts`); never hard-coded (AR-04, D-14) |
| `E2E_BASE_URL`    | `http://127.0.0.1:8000`                                | Playwright target                                                                                                                                   |
| `E2E_MAILPIT_URL` | `http://127.0.0.1:8025`                                | Playwright mail check                                                                                                                               |

No secrets.

## Commands (ES-05)

Run in `02_output/frontend/` after `npm ci`.

| Purpose     | Command                                                                                                                                                                   |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| build       | `npm run build` (type check, then `dist/`)                                                                                                                                |
| test        | `npm test` (unit and acceptance, Vitest); `npm run test:e2e` (Playwright, needs the running stack)                                                                        |
| check       | `npm run check` (Prettier, ESLint, `tsc --noEmit`)                                                                                                                        |
| run         | `docker compose up --build` in `02_output/` (frontend on http://127.0.0.1:8000); for development `APP_WORKSHOPS="W1=Demo" npm run dev` with the backend on 127.0.0.1:8080 |
| coverage    | `npm run test:coverage`                                                                                                                                                   |
| mutation    | `npm run mutation` (Stryker, command runner, D-17)                                                                                                                        |
| duplication | `npm run duplication`                                                                                                                                                     |

## Troubleshooting

| Symptom                                        | Cause and fix                                                                      |
| ---------------------------------------------- | ---------------------------------------------------------------------------------- |
| The form shows only "No workshop"              | `APP_WORKSHOPS` not set for the dev server or the container                        |
| "Registration failed. Please try again later." | the backend is not reachable through `/api` (dev server proxies to 127.0.0.1:8080) |
| "Too many registrations from your network"     | backend rate limit (`APP_RATE_LIMIT_PER_HOUR`)                                     |
| Playwright cannot find a browser               | `npx playwright install chromium`                                                  |
| e2e fails to find the form                     | the stack is not running; start it as in the root README                           |
