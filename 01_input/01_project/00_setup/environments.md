# Environments

> Owner: Architect · Read in: phases 0, 2, 4, 6, 7 · Agent: read-only

## Environments

| Environment | Purpose | How it runs |
|---|---|---|
| test | automated tests | Testcontainers PostgreSQL, in-process or container mail catcher, reCAPTCHA test mode; no network calls to Google |
| local | development and the phase 6 runtime demonstration | `docker compose up` in `02_output/`: backend, frontend, PostgreSQL, Mailpit; plain HTTP on 127.0.0.1 only |
| production | the public registration site | backend and frontend containers with HTTP inside; an external nginx reverse proxy terminates HTTPS (Let's Encrypt), serves `/` from the frontend and forwards `/api` to the backend |

Persistent data (database and JSON copies) lives on named volumes and survives container recreation in local and production.

## Configuration

Settings that differ between environments (ES-01). Secrets are listed in `project/00_setup/secrets.env.example`, not here.

| Setting | Environments | Default allowed? |
|---|---|---|
| Database URL and user | all | local and test only |
| SMTP host, port, TLS on or off | all | local and test only (Mailpit) |
| Sender address | all | yes |
| Conference name shown in emails | all | yes |
| Conference options source | all | local and test only |
| JSON copy directory | all | yes (a path on a persistent volume) |
| reCAPTCHA test mode | all | off by default; on only in test and local; production refuses to start if it is on or if the keys are empty |
| Organizer HTTPS-only access | all | on by default; may be off only on local plain HTTP |
| Allowed frontend origin (CORS) | all | local only |
| Rate limits and request-size limit | all | yes |

## External services

| Service | Production | Local/test substitute | Must be tested manually by a human? |
|---|---|---|---|
| Google reCAPTCHA v2 | live verification with real keys | deterministic test mode plus a mocked verification endpoint for the production code path | yes: one real submission with production keys |
| SMTP | external SMTP server | Mailpit (`axllent/mailpit` from `tech-stack.md`) | yes: delivery to a real mailbox |
| TLS and reverse proxy | external nginx with Let's Encrypt | none (plain HTTP on localhost) | yes: HTTPS, redirects and `/api` routing |
| NVD vulnerability data | not used at runtime | Dependency-Check with `NVD_API_KEY` | no |
