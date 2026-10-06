# Security requirements

> Owner: Security officer · Read in: phases 2, 6 · Agent: read-only

Adds to `general/security/security-baseline.md` (SB-01 … SB-14); do not repeat those here.

## Verification standard

| Component | Standard | Level | Reason if above the default |
|---|---|---|---|
| backend | OWASP ASVS 5.0 | Level 1 | |
| frontend | OWASP ASVS 5.0 | Level 1 | |

Level 1 is a deliberate choice: the only authentication is one organizer role for one export operation, and the personal data is contact and study data, not sensitive data. Where a requirement below is more specific than ASVS, it applies as well.

## Authentication and authorization

- Participants are anonymous; there are no participant accounts.
- One organizer role may export registrations (BR-08). The mechanism is decided in phase 2, using the organizer credentials from `secrets.env.example`; identity providers are out of scope.
- Everything else (options, registration) is public but protected by SR-01 and SR-03.

## Personal data

| Data item | Purpose | Retention | Shown/exported to |
|---|---|---|---|
| First and last name | identify the participant | `OQ-06` | organizers (email, export) |
| Email | confirmation and contact | `OQ-06` | participant, organizers |
| Organization / institution | attendance records | `OQ-06` | organizers |
| Study institution, programme, student ID | student registration | `OQ-06` | organizers |
| Selected options | plan workshops, events and meals | `OQ-06` | participant (confirmation), organizers |
| Consents given, with timestamp | proof of consent (SB-14) | `OQ-06` | organizers |

## Project requirements

| ID | Requirement |
|---|---|
| SR-01 | Every registration is protected by Google reCAPTCHA v2 and verified on the backend; the frontend check alone is never enough. |
| SR-02 | reCAPTCHA test mode is enabled only by environment configuration, never by default; production refuses to start with test mode on or with empty keys. |
| SR-03 | The registration and export endpoints are rate limited, and request bodies have a size limit. |
| SR-04 | Option identifiers are checked against the active configured set (BR-04). |
| SR-05 | Email content is generated safely: user input cannot inject headers or markup. |
| SR-06 | Organizer credentials are never accepted over plain HTTP, except on localhost. |
| SR-07 | The export and the organizer email expose only registration data, never internal fields or other system data. |

## Overrides

None.
