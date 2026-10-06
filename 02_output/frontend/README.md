# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

The single registration page (React, Vite, TypeScript; contract `docs/02_contracts/registration-form.json`). It talks to the backend only through `/api` on its own origin (AR-01): nginx proxies `/api/` to the backend in the container, Vite's dev server proxies to `http://127.0.0.1:8080`.

## Prerequisites

- Node.js with npm (this run: Node.js 24.10.0 / npm 10.9.4, approved in D-02; pinned in `tech-stack.md`: 24.13.0 / 11.6.2; the container build uses `node:24.13.0-alpine`).
- For end-to-end tests: Playwright's Chromium (`npx playwright install chromium`) and the running local stack.

## Configuration

No settings of its own: workshops and prices come from the backend (`GET /api/registration-options`). nginx (`nginx.conf`) listens on 8080 as user `nginx`, sets the security headers (SB-10) and limits request bodies to 16 KiB. No secrets.

## Build

```sh
npm ci
npm run build                 # type check + production bundle in dist/
docker build -t confreg-frontend .
```

## Run

```sh
npm run dev                   # http://127.0.0.1:5173, backend expected on 127.0.0.1:8080
docker compose --env-file ../.env up -d --build   # from 02_output/: page on http://127.0.0.1:3000
```

## Test

```sh
npm test                      # unit and acceptance tests (Vitest, jsdom)
npm run coverage              # with V8 coverage
npm run e2e                   # Playwright against the running stack (E2E_BASE_URL, E2E_MAILPIT_URL to override)
npm run mutation              # Stryker (see known limitation F-11)
```

Acceptance tests (frozen): `tests/acceptance/`; end-to-end: `e2e/`.

## Check

```sh
npm run check                 # prettier --check, eslint, tsc --noEmit
npm run format                # reformat
npm run duplication           # jscpd
npm audit                     # dependency scan
```

## Troubleshooting

| Symptom                                                     | Cause and fix                                                                               |
| ----------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Page shows "Registration is not possible right now" on load | the backend is not reachable through `/api`; start it (dev server proxies to port 8080)     |
| `npm run e2e` fails with `ECONNREFUSED 127.0.0.1:3000`      | start the local stack first                                                                 |
| Playwright cannot find a browser                            | `npx playwright install chromium`                                                           |
| `npm ci` fails on the lock file                             | use the npm version above; do not regenerate `package-lock.json` with another major version |
| Stryker reports a very low mutation score                   | known runner issue with vitest 5 (F-11); the score is not valid                             |
