# Environments

> Owner: Architect · Read in: phases 0, 2, 4, 6, 7 · Agent: read-only

## Environments

| Environment | Purpose | How it runs |
|---|---|---|
| test | automated tests | Testcontainers PostgreSQL, in-process or container mail catcher; test clock enabled |
| local | development and the phase 6 runtime demonstration | `docker compose up` in `02_output/`: backend, frontend, PostgreSQL, Mailpit; plain HTTP on 127.0.0.1 only. Backend on port 8080, Mailpit web and API on port 8025. |
| production | the public registration site | backend and frontend containers behind an external nginx reverse proxy with HTTPS |

## Configuration

Every setting can be overridden by an environment variable of the same name; `02_output/docker-compose.yml` passes them through to the backend. Secrets are listed in `project/00_setup/secrets.env.example`, not here.

| Setting | Default | Environments | Default allowed? |
|---|---|---|---|
| `APP_CONFERENCE_TZ` | Europe/Ljubljana | all | yes |
| `APP_EARLY_BIRD_DEADLINE` | 2026-07-31 | all | yes |
| `APP_FEE_EARLY` | 240.00 | all | yes |
| `APP_FEE_REGULAR` | 300.00 | all | yes |
| `APP_VAT_RATE` | 0.22 | all | yes |
| `APP_WORKSHOPS` | `W1` Requirements engineering for AI coding agents; `W2` Data spaces in practice; `W3` Secure software supply chains | all | yes |
| `APP_RATE_LIMIT_PER_HOUR` | 100 | all | yes |
| `APP_TEST_CLOCK` | disabled | test and local only | production refuses to start if enabled |
| Database URL and user | – | all | local and test only |
| SMTP host, port, TLS | – | all | local and test only (Mailpit) |
| Sender address | – | all | yes |

## External services

| Service | Production | Local/test substitute | Must be tested manually by a human? |
|---|---|---|---|
| SMTP | external SMTP server | Mailpit (`axllent/mailpit` from `tech-stack.md`) | yes: delivery to a real mailbox |
| TLS and reverse proxy | external nginx | none (plain HTTP on localhost) | yes |
| Accounting system | produces invoices; not part of this repository | none | no |
