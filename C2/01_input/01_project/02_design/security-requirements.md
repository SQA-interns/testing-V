# Security requirements

> Owner: Security officer · Read in: phases 2, 6 · Agent: read-only

Adds to the security baseline of the general rules (SB-01 … SB-14); do not repeat those here.

## Verification standard

| Component | Standard | Level | Reason if above the default |
|---|---|---|---|
| backend | OWASP ASVS 5.0 | Level 1 | |
| frontend | OWASP ASVS 5.0 | Level 1 | |

## Authentication and authorization

- All endpoints require organizer authentication (HTTP Basic with `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD`) unless a requirement explicitly makes an endpoint public.
- Public endpoints are rate limited per client (`APP_RATE_LIMIT_PER_HOUR`).
- There are no participant accounts.

## Personal data

| Data item | Purpose | Retention | Shown/exported to |
|---|---|---|---|
| First and last name, e-mail | identify and contact the participant | defined by the organizer outside this project | participant, organizers |
| Company name, address, VAT ID | invoicing by the accounting system | defined by the organizer outside this project | organizers, accounting |

## Project requirements

| ID | Requirement |
|---|---|
| SR-01 | Personal data (names, e-mail addresses, postal addresses, VAT IDs) is never written to logs or standard output. |
| SR-02 | Request bodies have a size limit. |
| SR-03 | Organizer credentials are never accepted over plain HTTP, except on localhost. |
| SR-04 | The test clock can never be active in production. |
| SR-05 | E-mail content is generated safely: user input cannot inject headers or markup. |

## Overrides

None.
