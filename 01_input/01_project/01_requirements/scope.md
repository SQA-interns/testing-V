# Scope

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## In scope

- Registration forms for external participants and students (US-001, US-002)
- Configurable conference options (US-003)
- In-application confirmation, participant confirmation email, organizer notification email with the JSON attachment (US-004, US-006, US-007)
- Storage in the database plus a raw JSON copy (US-005)
- Excel export for organizers (US-008)
- Anti-automation protection of the registration form

## Out of scope

- Participant accounts and login
- Payment processing
- Editing or cancelling a submitted registration
- Verifying student status externally
- An administration UI or dashboard, including option configuration
- Integration with an identity provider

## Priorities

1. A registration is never lost: storage (BR-07) comes before any notification.
2. Security and privacy (`project/02_design/security-requirements.md`) come before convenience.
3. The participant flow (US-001 to US-006) comes before organizer features (US-007, US-008).

## Open questions

| ID | Question | Owner | Answer |
|---|---|---|---|
| OQ-01 | Are all options available to students, or are some only for external participants? | Product owner | |
| OQ-02 | Which mandatory consents exist, and what is their wording? | Product owner | |
| OQ-03 | What happens when storage succeeds but an email fails? | Product owner | |
| OQ-04 | How many options may a participant select per category? | Product owner | |
| OQ-05 | Is a second registration with the same email allowed? | Product owner | |
| OQ-06 | How long are registrations and JSON copies kept? | Product owner | |

Unanswered questions become decision records in phase 1.
