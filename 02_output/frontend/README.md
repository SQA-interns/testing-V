# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, `docs/02_specification.md`, ES-05, ES-06 · Agent: writes

The registration form (one page, React). It reads the workshop list from `/config.json` and talks to the backend only through `POST /api/registrations` (AR-01). Form contract: `../docs/02_contracts/registration-form.yaml`.

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2 (`.npmrc` enforces the engine versions and exact saves).
- For end-to-end tests: Playwright's Chromium (`npx playwright install chromium`, once) and a running backend with Mailpit, for example the compose stack.
- Docker for the container image.

## Configuration

| Setting         | Where                                | Purpose                                                                                                           |
| --------------- | ------------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| `APP_WORKSHOPS` | container environment (required)     | written to `/config.json` at container start by `write-config.sh`; same format as the backend (`id=name;id=name`) |
| `APP_WORKSHOPS` | `npm run dev` environment (optional) | the dev server serves `/config.json` from it; without it the form offers only "No workshop"                       |
| `E2E_BASE_URL`  | end-to-end tests                     | frontend URL; default: a Vite dev server on 127.0.0.1:5173 started by Playwright                                  |
| `MAILPIT_URL`   | end-to-end tests                     | Mailpit API, default `http://127.0.0.1:8025`                                                                      |

The frontend has no secrets. In the container, nginx (unprivileged, port 8080) serves the build, sets the security headers and proxies `/api/` to `http://backend:8080`.

## Build

```sh
npm ci
npm run build                        # static files in dist/
docker build -t confreg-frontend .   # nginx image
```

## Run

```sh
npm run dev      # http://127.0.0.1:5173, proxies /api to http://127.0.0.1:8080
```

For the full stack use `../README.md`.

## Test

```sh
npm test                 # unit and component tests (Vitest, jsdom)
npm run test:coverage    # with V8 coverage
npm run test:e2e         # Playwright end-to-end tests (frozen, folder e2e/)
npm run test:mutation    # Stryker (command runner, D-36)
```

End-to-end against the compose stack on moved ports, for example:

```sh
E2E_BASE_URL=http://127.0.0.1:15173 MAILPIT_URL=http://127.0.0.1:18025 npm run test:e2e
```

## Check

```sh
npm run check            # Prettier, ESLint, tsc --noEmit
npm run format           # fix formatting
npm run duplication      # jscpd
npm audit                # dependency scan
```

## Troubleshooting

| Symptom                                      | Cause and fix                                                            |
| -------------------------------------------- | ------------------------------------------------------------------------ |
| "The registration form is not available"     | `/config.json` is missing or invalid; set `APP_WORKSHOPS`                |
| `npm ci` fails with an engine error          | use Node.js 24.13.0 and npm 11.6.2                                       |
| Playwright: "Executable doesn't exist"       | run `npx playwright install chromium`                                    |
| End-to-end tests time out waiting for e-mail | Mailpit is not reachable at `MAILPIT_URL`, or the backend is not running |
| Registration shows "could not be completed"  | the backend answered 5xx (for example SMTP down); try again              |
