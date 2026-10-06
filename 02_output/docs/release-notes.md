# Release notes

> Written in: phase 7 · Agent: writes

Release 1.0.0 of the conference registration, run `kyuhi-confreg-C1-r1`, 2026-10-06.

## Delivered

- **US-001 Conference registration**, acceptance criteria AC-001-01 to AC-001-18 (`docs/01_acceptance-criteria.md`), all covered by frozen acceptance and end-to-end tests that pass.
  - Public registration form (`frontend/`) and fixed registration API `POST /api/registrations` (`backend/`).
  - Early-bird or regular fee by the server's time in `Europe/Ljubljana`; fees are gross, with net and VAT split out (D-19).
  - Confirmation e-mail with registration number and fee; if it cannot be sent, nothing is stored and the participant is asked to try again (D-22).
  - Optional single workshop from the configured list (D-20); validation rules of D-21.
  - Organizer read access `GET /api/registrations/{registrationNumber}` (HTTP Basic), which provides the invoice data for the accounting system (AC-001-04, D-18).
- Security controls SB-01 to SB-14 and SR-01 to SR-05 as mapped in `docs/06_verification-report.md`: rate limit, body size limit, security headers, no personal data in logs, plain-text e-mail, test clock refused in production, non-root containers.
- Local stack `docker-compose.yml` (backend, frontend, PostgreSQL 16, Mailpit) with health checks (NFR-02); Flyway schema (AR-06).
- Verification: 203 tests pass (64 acceptance, 108 backend unit, architecture and integration, 27 frontend, 4 end-to-end); backend line coverage 97.2%, branch 90.3%, mutation 85%; frontend line coverage 93.7%, mutation 77.8%; no open Critical or High finding.

## Known limitations

- **No invoices are produced here.** The accounting system creates them from the organizer API (AR-08, D-18); nothing notifies accounting of a new registration.
- **Registration depends on SMTP.** While the SMTP server is down, registrations are refused with 503 (D-22). If the database commit failed after the e-mail was accepted (very rare), the participant would hold a confirmation for a registration that was not stored.
- **Rate limit is in memory and per instance.** It resets on restart and does not span several backend instances. In the local stack all browser requests reach the backend through the frontend container, so they share one client address; production must use the external proxy's `X-Forwarded-For` (profile `prod`).
- **No duplicate detection, capacity or closing date** (D-23).
- **Retention** of personal data is defined by the organizer outside this project; there is no deletion function.
- **Default host ports** 8080, 5173 and 8025 may be taken on a machine; the compose file allows `BACKEND_HOST_PORT`, `FRONTEND_HOST_PORT` and `MAILPIT_HOST_PORT` (this run used 18080, 15173 and 18025, D-35).
- **Dev tooling:** `qs` (Moderate) through Stryker, not shipped (F-06); Stryker uses its command runner because its vitest runner does not work with vitest 5.0.3 (D-36).
- **Git history** keeps a random Spring Security development password printed in a phase 3 test log (F-01, redacted in the current file); it was never used outside that test run.

## Must be tested manually by a human

| Item | Why it is manual |
|---|---|
| Confirmation e-mail delivered to a real mailbox through the production SMTP server with STARTTLS (`APP_MAIL_TLS=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`) | `environments.md`: SMTP is substituted by Mailpit locally |
| TLS termination and the external nginx reverse proxy: HTTPS only, `X-Forwarded-Proto` and `X-Forwarded-For` honoured (organizer access over HTTPS, rate limit per real client) | no TLS or proxy locally (SB-04, SR-03, SB-06) |
| Production start with `SPRING_PROFILES_ACTIVE=prod`: refuses `APP_TEST_CLOCK=enabled`, `APP_MAIL_TLS=false` and `APP_INSECURE_AUTH_ALLOWED=true`; database URL with `sslmode=verify-full` | production configuration (SR-04, SB-04); unit-tested only |
| The accounting system obtains invoice data through `GET /api/registrations/{registrationNumber}` and the invoice reaches the payer | accounting system not part of this repository (AC-001-04, D-18) |
| The registration form in the browsers the organizer supports (only Chromium is automated) | e2e runs Chromium only |

## Decisions pending review

Every decision is in `docs/decisions-log.md`. Decisions answered by the human (D-08 to D-12, D-16, D-17, D-33 to D-35) are resolved. These were chosen by the agent and remain pending review:

| Decision | Choice |
|---|---|
| D-02 | cloc image tag 2.10 reports 1.98; pin kept |
| D-03 | OSS Index analyser of Dependency-Check disabled (no credentials listed) |
| D-10 (part) | jscpd 4.3.0 replaced by 5.4.0 together with the approved vitest change |
| D-15 | CVE-2025-15104 on hibernate-validator treated as a false positive and suppressed |
| D-18, D-24 | no invoice issued by this system; AC-001-04 met by stored invoice data and organizer API (human delegated the choice) |
| D-19, D-24 | configured fee is gross; VAT split out; half-up rounding |
| D-20, D-24 | at most one configured workshop, optional, no price effect, no capacity |
| D-21, D-24 | validation rules (required fields, lengths, company fields by payer type) |
| D-22, D-24 | no registration stored when the e-mail fails; 503 |
| D-23, D-24 | duplicates allowed, no capacity, no closing date |
| D-25 | dev-only contract validation tools added |
| D-26 | setting names, `APP_MAIL_TLS`, `APP_INSECURE_AUTH_ALLOWED` added |
| D-27 | contracts committed in one commit (history kept) |
| D-30 | JDK image of the pinned Temurin release used as backend build stage |
| D-32 | three commit subjects over 72 characters (history kept) |
| D-36 | Stryker command runner instead of its vitest runner |
