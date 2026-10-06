# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

Story: **US-001 Conference registration** (`project/01_requirements/REQ-REG-01.md`). AC-001-01 to AC-001-09 adopt the product owner's AC1 to AC9 without changing their meaning. AC-001-10 onwards are **added** by the agent where the fixed API (`project/02_design/architecture.md`), the security requirements or the UI interface need an observable behaviour the requirement does not state.

Conventions used by every criterion:

- Configured values (`APP_FEE_EARLY`, `APP_FEE_REGULAR`, `APP_VAT_RATE`, `APP_EARLY_BIRD_DEADLINE`, `APP_CONFERENCE_TZ`, `APP_WORKSHOPS`, `APP_RATE_LIMIT_PER_HOUR`) prevail; the amounts quoted are the expected 2026 configuration and serve as test expectations (REQ-REG-01, "Constraints"). Tests read them from configuration, never from literals.
- *Submission time* is the server time at which a valid request is received (OQ-01); in tests it comes from the test clock (`X-Test-Now`), never from the system clock.
- VAT = net fee × `APP_VAT_RATE`, rounded half-up to 0.01; gross fee = net fee + VAT (REQ-REG-01, "Oracle notes").
- A *valid registration* satisfies every rule in AC-001-07 and decision D-10.

## US-001 Conference registration

### AC-001-01 Early-bird fee (AC1)
- Given the submission time is on or before the early-bird deadline
- When a valid registration is submitted
- Then the registration is stored with net fee 240.00 EUR, VAT 52.80 EUR and gross fee 292.80 EUR.

### AC-001-02 Regular fee (AC2)
- Given the submission time is after the early-bird deadline
- When a valid registration is submitted
- Then the registration is stored with net fee 300.00 EUR, VAT 66.00 EUR and gross fee 366.00 EUR.

### AC-001-03 Deadline boundary (AC3)
- Given the conference time zone is Europe/Ljubljana
- When registrations are submitted at 2026-07-31 23:59:59 and at 2026-08-01 00:00:00 local time (2026-07-31T21:59:59Z and 2026-07-31T22:00:00Z)
- Then the first is stored with the early-bird net fee and the second with the regular net fee.

### AC-001-04 Confirmation (AC4)
- Given a registration has been stored
- When the request completes
- Then the API returns 201 with the registration number, and exactly one confirmation e-mail is sent to the participant, stating the registration number, net fee, VAT and gross fee.

### AC-001-05 Payer data without invoice (AC5)
- Given the payer is a company
- When a valid registration is submitted
- Then the company's name, address and VAT ID are stored with the registration.
- Given the payer is the participant as a private person
- When a valid registration is submitted
- Then the registration is stored without company data.
- In both cases no invoice and no payment request are created.

### AC-001-06 VAT for payers from other EU member states (AC6)
- Given the payer is a company from another EU member state
- When a valid registration is submitted
- Then VAT is charged at the Slovenian rate, with the same amounts as in AC-001-01 and AC-001-02.

### AC-001-07 Validation (AC7)
- Given a required field is missing or invalid, or more than one workshop is selected
- When a registration is submitted
- Then the API returns 422 with one error per field, nothing is stored and no e-mail is sent.
- Required fields: `firstName`, `lastName` and `email` of the participant; `payerType` (`private` or `company`); for a company payer also `companyName`, `companyAddress` and `companyVatId`.
- What counts as invalid: decision D-10.

### AC-001-08 Workshop selection (AC8)
- Given the participant selects one workshop or none
- When a valid registration is submitted
- Then the registration is stored with that workshop or with no workshop, and no capacity is checked.

### AC-001-09 Repeated registration (AC9, decision OQ-02)
- Given a registration with the same participant e-mail already exists
- When a valid registration is submitted
- Then a second registration is stored with its own registration number and fee, and the existing registration is left unchanged.

### AC-001-10 Organizer reads a registration (added)
Source: fixed registration API (`architecture.md`), story "so that … the organizer can invoice".
- Given a registration has been stored
- When the organizer requests `GET /api/registrations/{registrationNumber}` with valid HTTP Basic credentials (`ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`)
- Then the API returns 200 with the stored registration: `registrationNumber`, `firstName`, `lastName`, `email`, `payerType`, `companyName`, `companyAddress`, `companyVatId`, `workshop` (id or null), `netFee`, `vat`, `grossFee`, each equal to what was stored.

### AC-001-11 Organizer access is protected (added)
Source: SB-02, `security-requirements.md` ("Authentication and authorization").
- Given a registration has been stored
- When `GET /api/registrations/{registrationNumber}` is requested without credentials or with wrong credentials
- Then the API returns 401 and the response contains no registration data.

### AC-001-12 Unknown registration number (added)
Source: fixed registration API.
- Given no registration has the requested registration number
- When the organizer requests it with valid credentials
- Then the API returns 404 and the response contains no registration data.

### AC-001-13 Rate limit on registration (added)
Source: REQ-REG-01 "Constraints" (authorization), `security-requirements.md`, SB-06; details in decision D-11.
- Given one client has already sent `APP_RATE_LIMIT_PER_HOUR` registration requests within the last hour
- When the same client submits another registration
- Then the API returns 429, nothing is stored and no e-mail is sent.

### AC-001-14 Registration through the form (added)
Source: interface "Registration form" (`architecture.md`), AR-01.
- Given the participant opens the registration form page
- When they enter their name and e-mail, choose a private or company payer (company name, address and VAT ID are asked for only for a company payer), select one of the configured workshops or none, and submit
- Then the page shows the registration number, net fee, VAT and gross fee of the stored registration.

### AC-001-15 Form shows validation errors (added)
Source: AC-001-07 seen from the form; interface "Registration form".
- Given the participant submits the form with a required field missing or invalid
- When the API rejects the registration with 422
- Then the page shows an error next to each rejected field, keeps the entered values, and shows no registration number.

## Open questions and assumptions

| ID | Status | Applied as |
|---|---|---|
| OQ-01 (Q1) | resolved | Submission time determines the fee (conventions above, AC-001-01 to 03). |
| OQ-02 (Q2) | resolved | Every registration is kept (AC-001-09). |
| OQ-03 (Q3) | assumed | Each registration stores its own fee and receives its own confirmation (AC-001-04, AC-001-09); choosing the invoiced fee belongs to invoicing (NG3). |
| OQ-04 (Q4) | assumed | No privacy acknowledgment field; personal data handled per `security-requirements.md` (SB-12 to SB-14, SR-01). |
| Gaps found in phase 1 | decided, pending review | D-10 validation details; D-11 rate-limit counting; D-12 behaviour when the confirmation e-mail cannot be sent. |

Non-goals NG1 to NG5 (REQ-REG-01) are out of scope: no student category, no cancellation or change, no payment or invoice, no duplicate detection, no change of configured values.

## Traceability

| AC | Story | Requirement source | Business rules and decisions |
|---|---|---|---|
| AC-001-01 | US-001 | REQ-REG-01 AC1 | glossary: net fee, VAT, early-bird deadline; OQ-01 |
| AC-001-02 | US-001 | REQ-REG-01 AC2 | glossary: net fee, VAT; OQ-01 |
| AC-001-03 | US-001 | REQ-REG-01 AC3 | oracle notes (time zone); AR-05 |
| AC-001-04 | US-001 | REQ-REG-01 AC4 | OQ-03; D-12 |
| AC-001-05 | US-001 | REQ-REG-01 AC5 | glossary: payer; NG3 |
| AC-001-06 | US-001 | REQ-REG-01 AC6 | glossary: VAT |
| AC-001-07 | US-001 | REQ-REG-01 AC7 | D-10 |
| AC-001-08 | US-001 | REQ-REG-01 AC8 | glossary: workshop |
| AC-001-09 | US-001 | REQ-REG-01 AC9 | OQ-02, OQ-03; NG4 |
| AC-001-10 | US-001 | fixed registration API (added) | – |
| AC-001-11 | US-001 | security requirements, SB-02 (added) | – |
| AC-001-12 | US-001 | fixed registration API (added) | – |
| AC-001-13 | US-001 | REQ-REG-01 constraints, SB-06 (added) | D-11 |
| AC-001-14 | US-001 | interface "Registration form", AR-01 (added) | – |
| AC-001-15 | US-001 | interface "Registration form", AC7 (added) | D-10 |
