# Release notes

> Written in: phase 7 · Agent: writes

Release 0.1.0 of the conference registration system, run `tanej-confreg-C1-r1`, 2026-10-06.

## Delivered

- **US-001 Conference registration.** A participant registers on a single page (`frontend`). The backend validates the request, sets the fee by the early-bird deadline in the conference time zone, stores the registration and sends a plain-text confirmation e-mail with the registration number and the fee. Storage and e-mail succeed together or not at all. AC-001-01 to AC-001-10 (`docs/01_acceptance-criteria.md`).
- **Fixed registration API.** `POST /api/registrations` (public, rate limited) and `GET /api/registrations/{registrationNumber}` (organizer, HTTP Basic). Also `GET /api/workshops` (public, D-28). Contract: `docs/02_contracts/registration-api.openapi.yaml`.
- **Configuration.** All business values (fees, deadline, VAT, workshops, rate limit, time zone) come from the environment variables in `environments.md`. Defaults are in `application.properties`.
- **Operations.** Container images for backend and frontend, both running as non-root. A local stack (`docker-compose.yml`) with PostgreSQL and Mailpit. Health and readiness on an internal management port. Flyway migration V1.
- **Security.** Server-side validation, organizer authentication with a BCrypt-hashed password, per-client rate limiting, a 16 KiB body limit, organizer credentials refused over plain HTTP except locally, security headers, no personal data in logs, test clock refused in production. Details: `docs/06_verification-report.md`, "Security baseline and project requirements".
- **Verification.** Final run 203/203 tests (52 frozen acceptance, 4 frozen end-to-end). No open Critical or High finding.

## Known limitations

- **No invoicing (D-20).** AC4 ("an invoice is issued to the payer") conflicts with AR-08. This release stores the payer data and amounts for the accounting system but issues no invoice. The confirmation e-mail says the accounting office sends the invoice.
- **Fee is net (D-21).** 240/300 EUR are net amounts; VAT is added (292.80/366.00 EUR gross with the defaults).
- **Organizers read registrations one at a time,** by number. There is no list, search, export, cancellation or editing. `poi-ooxml` is a listed dependency but no Excel export was required or built.
- **The rate limit is in memory, per instance.** It resets on restart, and each instance counts separately when several run. Behind a proxy it counts per client address only if the proxy is trusted (Tomcat `RemoteIpValve` defaults: private networks).
- **Registration numbers can have gaps.** A number is taken before the e-mail is sent, so a registration rolled back after an SMTP failure (AC-001-09) leaves its number unused.
- **Repeated registrations** with the same e-mail are separate registrations (D-24).
- **Frontend nginx proxy.** The `/api` proxy in the frontend image is for the local stack (fixed host `backend:8080`). In production the external nginx routes `/api` to the backend.
- **Platform deviations of this run.** Built and tested with Oracle JDK 21.0.11 (D-09) and Node.js 24.10.0 / npm 10.9.4 (D-10) on the host. Images use the pinned `eclipse-temurin:21.0.10_7-jre-alpine` and `node:24.13.0-alpine`. Vitest 5.0.3 and jscpd 5.4.0 replace the original pins (D-11).
- **Frontend mutation score is not measurable** with Stryker 10.0.0 and Vitest 5.0.3 (D-35). Backend mutation score: 81 %.
- **Accepted dependency findings.** These are test or dev scope only and have no fix release: CVE-2026-64607 in docker-java's bundled httpclient5 (Medium, F-04); `qs` through Stryker (Medium, F-06). Suppressed with evidence: Testcontainers and Angus false positives (D-12, D-18), docker-java httpclient5/httpcore5 Critical/High (D-13, re-check in the next release).

## Must be tested manually by a human

| # | What | Why it is not covered automatically |
|---|---|---|
| 1 | Confirmation e-mail delivery through the production SMTP server to real mailboxes (several providers), including č, š, ž in names and company data and the `From` address | Tests use Mailpit (`environments.md`: "must be tested manually: yes") |
| 2 | STARTTLS to the production SMTP server (`APP_MAIL_STARTTLS=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`) | Not available locally |
| 3 | HTTPS through the external nginx reverse proxy: certificate, redirect from HTTP, `X-Forwarded-Proto` and `X-Forwarded-For` passed to the backend | `environments.md`: TLS and reverse proxy "must be tested manually: yes" |
| 4 | Behind the production proxy: organizer `GET` works over HTTPS and is refused with 403 over plain HTTP from a public address (SR-03, D-27); the backend is not reachable directly from the internet | Depends on the production network |
| 5 | Behind the production proxy: rate limiting counts per real client address, not per proxy address (SB-06) | Depends on the proxy and trusted-proxy settings |
| 6 | Production profile: start with `SPRING_PROFILES_ACTIVE=prod`; confirm that `APP_TEST_CLOCK=enabled` prevents startup (SR-04) and that the management port 8081 is not exposed | Production deployment is not part of the repository |
| 7 | Invoices are produced by the accounting system from the stored payer data and amounts (AC-001-04, D-20) | The accounting system is outside this repository |
| 8 | The registration form in the browsers the conference supports, on mobile, and with a screen reader (labels, error messages) | Automated end-to-end tests use desktop Chromium only |
| 9 | Retention and deletion of personal data as defined by the organizer (SB-13) | Defined outside this project |
| 10 | The production values of the business settings (fees, deadline, VAT, workshops) | Defaults come from `environments.md`; production may differ |

## Decisions pending review

Applied with the conservative option and marked pending review. D-30 records that no further information was available.

| Decision | Applied option |
|---|---|
| D-20 | No invoice in this repository; payer data and amounts stored for the accounting system |
| D-21 | The configured fee is the net fee; VAT added, half-up to cents |
| D-22 | At most one workshop, a configured id; no fee effect; anything else rejected |
| D-23 | Strict validation (required fields, company fields only for company payers, unknown fields rejected, length limits) |
| D-24 | Repeated registrations with the same e-mail are accepted |
| D-25 | Registration and confirmation e-mail are atomic; 503 when SMTP refuses |
| D-26 | Health and readiness only on the internal management port 8081 |
| D-27 | Organizer credentials over plain HTTP accepted from loopback, and outside `prod` from private addresses |
| D-28 | Public `GET /api/workshops` for the form |
| D-29 | `POST /api/registrations` is public |

All other decisions are resolved. The log is append-only, so the human answers are separate follow-up records: D-01 → D-09, D-02 → D-10, D-04 → D-11, D-07 → D-12, D-08 → D-13, D-15 → D-18, D-17 → D-19, D-32 → D-33, D-34 → D-35. D-03, D-05, D-06, D-14, D-16, D-30 and D-31 were resolved when they were recorded. See `docs/decisions-log.md`.
