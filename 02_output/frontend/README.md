# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, `docs/02_specification.md`, ES-05, ES-06 · Agent: writes

React single-page registration form (AC-001-11). It talks to the backend only through `/api` (AR-01). Element ids: [../docs/02_contracts/registration-form.json](../docs/02_contracts/registration-form.json).

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2 (`engine-strict` is on, so other versions are refused).
- For end-to-end tests: Playwright's Chromium (`npx playwright install chromium`, about 150 MB) and the running backend and Mailpit.
- Exact versions: `01_input/01_project/00_setup/tech-stack.md`; installed versions are fixed by `package-lock.json`.

## Configuration

The page has no business settings of its own; it loads the workshops from `GET /api/workshops`.

| Setting        | Where                              | Default                                              | Purpose                                           |
| -------------- | ---------------------------------- | ---------------------------------------------------- | ------------------------------------------------- |
| `BACKEND_URL`  | container environment              | set to `http://backend:8080` by `docker-compose.yml` | where nginx proxies `/api/`                       |
| dev proxy      | `vite.config.ts`                   | `http://127.0.0.1:8080`                              | where `npm run dev` proxies `/api`                |
| `E2E_BASE_URL` | environment for `npm run test:e2e` | unset: Playwright starts the dev server on 5173      | set `http://127.0.0.1:3000` to test the container |
| `MAILPIT_URL`  | environment for `npm run test:e2e` | `http://127.0.0.1:8025`                              | Mailpit API                                       |

No secrets.

## Build

```sh
npm ci
npm run build
```

Output in `dist/`. The container image (`Dockerfile`) builds in `node:24.13.0-alpine` and serves `dist/` with `nginx:1.30.5-alpine` as a non-root user, with security headers (`nginx/default.conf.template`).

## Run

With the whole local stack: see the [root README](../README.md); the form is at http://127.0.0.1:3000.

For development, with the backend running on 127.0.0.1:8080:

```sh
npm run dev
```

The form is then at http://127.0.0.1:5173.

## Test

```sh
npm test               # unit and component acceptance tests (Vitest, jsdom)
npm run test:coverage  # the same with v8 coverage
npm run test:e2e       # end-to-end (Playwright); needs backend on 8080 and Mailpit on 8025
```

`tests/acceptance/` and `e2e/` are frozen acceptance tests. Mutation testing (`npm run mutation`) does not produce valid results with Stryker 10.0.0 and vitest 5.0.3 (D-16).

## Check

```sh
npm run check   # prettier --check, eslint, tsc --noEmit
```

Format with `npm run format`.

## Troubleshooting

| Symptom                                                                         | Cause and fix                                                                   |
| ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| `npm ci` fails with `EBADENGINE`                                                | Use Node.js 24.13.0 and npm 11.6.2.                                             |
| Form shows "The workshops could not be loaded"                                  | The backend is not reachable through `/api`; start it (dev: port 8080).         |
| `npm run test:e2e` fails with `GET /api/workshops` ≠ 200 or "Mailpit reachable" | Start the local stack (`docker compose --env-file ../.env up` in `02_output/`). |
| Playwright says the browser is missing                                          | Run `npx playwright install chromium`.                                          |
