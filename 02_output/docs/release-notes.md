# Release notes

> Written in: phase 7 · Agent: writes

Release 0.1.0 of the conference registration (run `tanej-confreg-C2-r1`, 2026-10-06).

## Delivered

- **US-001 Conference registration**, acceptance criteria AC-001-01 to AC-001-15 (`docs/01_acceptance-criteria.md`):
  - public registration form (React, served by nginx) and `POST /api/registrations`;
  - fee by submission time in the conference time zone: early-bird on or before `APP_EARLY_BIRD_DEADLINE`, otherwise regular; Slovenian VAT for every payer, rounded half-up; net, VAT and gross stored;
  - private or company payer (company name, address, VAT ID stored); no invoice or payment request is created;
  - zero or one workshop, no capacity limit; repeated registrations with the same e-mail are all kept;
  - validation with one error per field (422), shown next to each field in the form;
  - one plain-text confirmation e-mail per registration, retried while the SMTP server is unavailable;
  - organizer read access `GET /api/registrations/{registrationNumber}` with HTTP Basic, accepted only over HTTPS or on localhost;
  - rate limiting per client for registrations and failed organizer logins; 16 KiB body limit; security headers; non-root containers; health and readiness for the container health checks.
- Local stack: `02_output/docker-compose.yml` (backend, frontend, PostgreSQL 16, Mailpit).
- Verification: 177 automated tests pass (46 of them frozen acceptance and end-to-end tests); scanners show no open Critical or High finding (`docs/06_verification-report.md`).

## Known limitations

- **Rate limiting is in memory per backend instance.** Several backend instances would each count separately, and a restart resets the counts.
- **Client identity for rate limiting** is the address forwarded by trusted internal proxies. The external nginx must set `X-Forwarded-For` and `X-Forwarded-Proto`.
- **A confirmation that still fails after `APP_MAIL_MAX_ATTEMPTS`** is marked `failed`. The backend log then says "manual follow-up needed" with the registration number. There is no automatic notification to the organizer.
- **The organizer can read one registration by number.** There is no list, search or export.
- **The form and the e-mail are in English** (D-16).
- **Build platforms:** the backend was built and tested with Oracle JDK 21.0.11 (D-01) and the frontend with Node.js 24.10.0 / npm 10.9.4 (D-02). The listed Temurin 21.0.10+7 and Node.js 24.13.0 were not used on the host. The frontend image build uses the listed `node:24.13.0-alpine`.
- **The backend container image is built from a jar compiled on the host** (`./mvnw -DskipTests package` first), because no JDK image is listed.
- **Container images were not scanned for vulnerabilities**, because no image scanner is listed. The dependencies inside them were scanned.
- **Accepted findings** (`docs/06_verification-report.md`):
  - F-02: HTTP Basic, required by the design (D-18);
  - F-03: no npm minimum release age, not supported by the npm used;
  - F-06: a false-positive CVE;
  - F-07: dev-only `qs` advisories;
  - F-08 and F-09.

## Must be tested manually by a human

| # | What | Why |
|---|---|---|
| 1 | Confirmation e-mail delivery to real mailboxes through the production SMTP server, with `SMTP_TLS=true` and `SMTP_AUTH`/credentials set. Check that č, š, ž arrive unchanged and the message is not marked as spam. | Only Mailpit was used in tests (`environments.md`). |
| 2 | HTTPS through the external nginx reverse proxy. Check the certificate, the redirect from HTTP, and that `X-Forwarded-For` and `X-Forwarded-Proto` are set. Check that organizer requests over HTTPS succeed and over plain HTTP to the public host are refused (401). | TLS and the reverse proxy have no local substitute. |
| 3 | Rate limiting behind the production proxy. Check that different clients are counted separately, not all as the proxy's address. | It depends on the proxy configuration. |
| 4 | Production start with `SPRING_PROFILES_ACTIVE=prod`. Check that it refuses to start with `APP_TEST_CLOCK=enabled` or without `SMTP_TLS=true`, and starts otherwise. | Covered by unit tests only. |
| 5 | SMTP outage. Stop the SMTP server, register, restart it. Check that exactly one confirmation arrives within `APP_MAIL_RETRY_INTERVAL`. | The retry path (D-12) is covered by unit tests only. |
| 6 | The configured 2026 values (fees, deadline 2026-07-31, VAT 22 %, workshops W1–W3) in the production environment. | Configured values prevail over defaults. |
| 7 | Database connection with `sslmode=require` in production. | Operator configuration. |

## Decisions pending review

Non-blocking decisions taken with the conservative option; please confirm or change:

| Decision | Choice made |
|---|---|
| D-10 | What counts as invalid: e-mail syntax with a dotted domain, length limits, no control characters, no country-specific VAT ID format. Company fields sent with a private payer are ignored. |
| D-11 | The rate limit counts every registration request per client IP in a sliding hour. |
| D-12 | If the e-mail cannot be sent, the registration is kept, the API still answers 201, and the e-mail is retried. |
| D-14 | Workshops reach the form through runtime configuration (`/config.js`), not a new public endpoint. |
| D-15 | Health and readiness are on an unpublished management port, without authentication. |
| D-16 | The form and the e-mail are in English. |
| D-17 | Frontend mutation testing uses Stryker's command runner, because the vitest runner doesn't work with vitest 5.0.3. |

All other decisions (D-01 to D-09, D-13, D-18) are resolved (`docs/decisions-log.md`).
