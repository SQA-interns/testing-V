# Frontend

> Written in: phase 0 (skeleton), completed in phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Single-page registration form (React, Vite, TypeScript). Talks to the backend only through `/api` (AR-01).

## Commands (ES-05)

Run from `02_output/frontend/` after `npm ci`.

| Purpose                          | Command                                                                                                        |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| build                            | `npm run build`                                                                                                |
| test                             | `npm test`                                                                                                     |
| check (format, lint, type check) | `npm run check`                                                                                                |
| run                              | `npm run dev` (development server, proxies `/api` to `127.0.0.1:8080`), or `docker compose up` in `02_output/` |
| dependency scan                  | `npm audit`                                                                                                    |
