# Release notes

> Written in: phase 7 · Agent: writes

Release 0.1.0 of the conference registration (run `tanej-confreg-C0-r1`, 2026-10-06).

## Delivered

US-001 Conference registration, acceptance criteria AC-001-01 … AC-001-30 (`docs/01_acceptance-criteria.md`):

- Public registration page: name, e-mail, payer (private person or company with name, address and optional VAT ID), "I am a student", at most one workshop, live price (net, VAT, total), field errors from the server, confirmation with registration number and amounts.
- Pricing from configuration: early-bird fee up to and including `APP_EARLY_BIRD_DEADLINE` in `APP_CONFERENCE_TZ`, regular fee afterwards, VAT added and rounded half-up, students free.
- Fixed registration API: `POST /api/registrations`, `GET /api/registrations/{registrationNumber}` (organizer, HTTP Basic), plus `GET /api/registrations/export` (organizer, Excel for the accounting system) and `GET /api/registration-options` (public).
- One registration per e-mail address; plain-text confirmation e-mail, retried every 30 s while the SMTP server is unavailable.
- Security: server-side validation, organizer-only access by default, per-client rate limits, 16 KiB body limit, organizer credentials only over HTTPS (except local), security headers, non-root containers, no personal data in logs, test clock refused in production.
- Health and readiness endpoints, used by the container health checks; local stack with `docker compose` (backend, frontend, PostgreSQL, Mailpit).
- Tests: 178 passing (backend 154 incl. 44 frozen acceptance/architecture tests, frontend 22 incl. 9 frozen acceptance tests, 2 end-to-end tests).

## Known limitations

- No invoices are created and no payment is taken in the system: the accounting system issues invoices from the Excel export (D-07, D-08), and no payment status is tracked.
- Student status is self-declared (`student` flag) and not verified by the system; the export marks students so the organizer can check (D-06).
- At most one workshop per registration; workshops have no capacity limit (D-11).
- Rate limits and the mail retry run in memory: the backend is designed for a single instance; with several instances limits are per instance.
- Locally all browser requests reach the backend through nginx and share one rate-limit bucket; in production the client address comes from the trusted proxy (F-07).
- The confirmation e-mail is sent at least once; if the process stops between SMTP acceptance and recording it, the mail can be sent twice.
- Container images are not scanned for vulnerabilities: `tech-stack.md` lists no image scanner (F-08).
- The frontend mutation score cannot be measured with the pinned Stryker vitest runner 10.0.0 and vitest 5.0.3 (F-11).
- `.npmrc` sets no `min-release-age`, which npm 10.9.4 does not support; versions are pinned with a lock file instead (F-03).
- Development dependencies carry Moderate advisories (`qs` via Stryker) and accepted test-scope advisories (`docker-java-transport-zerodep`, D-05); none ships in the production images.
- The run used Oracle JDK 21.0.11 and Node.js 24.10.0 / npm 10.9.4 on the host instead of the pinned Temurin 21.0.10+7 and Node.js 24.13.0 / npm 11.6.2 (approved, D-01, D-02); the container images use the pinned versions. `vitest`, `@vitest/coverage-v8` (5.0.3) and `jscpd` (5.4.0) replace the listed versions (D-05); the build-only image `eclipse-temurin:21.0.10_7-jdk-alpine` was added (D-17).
- Retention of personal data is defined by the organizer outside this project (security requirements); the system deletes nothing.

## Must be tested manually by a human

| What | Why | How |
|---|---|---|
| Delivery of the confirmation e-mail to a real mailbox through the production SMTP server, with `APP_SMTP_TLS=true` and `SMTP_USERNAME`/`SMTP_PASSWORD` | automated tests use Mailpit only (environments.md) | register with a real address on the production stack; check inbox, sender, subject, UTF-8 characters (č, š, ž) and that it is not marked as spam |
| HTTPS and the external nginx reverse proxy in production | no local substitute (environments.md) | open the site over HTTPS; check the certificate, that HTTP redirects to HTTPS, that the proxy sends `X-Forwarded-For` and `X-Forwarded-Proto`, and that the backend runs with `SPRING_PROFILES_ACTIVE=prod` |
| Organizer access over HTTPS in production, and refusal over plain HTTP (SR-03) | depends on the real proxy | `curl -u … https://…/api/registrations/export` succeeds; the same over plain HTTP to the backend from another host answers 403 |
| Rate limit per real client behind the proxy (SB-06) | client address comes from the proxy only in production | from one client, send more than `APP_RATE_LIMIT_PER_HOUR` registrations; expect 429 with `Retry-After`; another client is unaffected |
| Production start-up guards (SR-04, SB-04) | production configuration | start with `prod` and `APP_TEST_CLOCK=enabled`, then with `APP_SMTP_TLS=false`: both must refuse to start |
| Import of the Excel export into the accounting system | accounting system is outside this repository (AR-08) | download `/api/registrations/export` and import or check it with the accounting team |
| Production business values | defaults in environments.md may change per edition | check `APP_FEE_*`, `APP_VAT_RATE`, `APP_EARLY_BIRD_DEADLINE`, `APP_WORKSHOPS` on the production stack via `GET /api/registration-options` |
| Usability and accessibility of the page with a screen reader and on mobile | not covered by automated tests | register once with a screen reader and once on a phone |

## Decisions pending review

The product owner should confirm or change these behaviours, chosen as the more conservative option (`docs/decisions-log.md`):

| Decision | Chosen behaviour |
|---|---|
| D-06 | students tick "I am a student" (added `student` field); free; no proof in the system |
| D-07 | invoices come from the accounting system via the Excel export, not from this system |
| D-08 | no online payment; payment against the invoice |
| D-09 | configured fees are net; 22 % VAT for every payer, including companies with a VAT ID; half-up rounding |
| D-10 | early bird includes the whole deadline day in the conference time zone, at the time the backend accepts the registration |
| D-11 | zero or one workshop; included in the fee; no capacity limit |
| D-12 | one registration per e-mail address (409 on repeat) |
| D-13 | company name and address required for companies, VAT ID optional; company data from a private payer is rejected |
| D-14 | registration kept when e-mail fails; mail retried |
| D-15 | `APP_WORKSHOPS` format `id=title;…`; registration numbers `CR-000001` |
| D-16 | public endpoints: registration, options, health; rate-limit client = IP; "localhost" includes private addresses outside production |
