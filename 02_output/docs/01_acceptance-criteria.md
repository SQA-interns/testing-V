# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

## Sources and conventions

- Story: US-001 Conference registration, specified in `project/01_requirements/REQ-REG-01.md` (`user-stories.md`, `business-rules.md`, `scope.md` refer to it). No other story is in scope.
- AC-001-01 to AC-001-09 adopt REQ-REG-01 AC1 to AC9 without changing their meaning. AC-001-10 to AC-001-12 are **added**, each with its reason.
- Business rules: REQ-REG-01 defines no `BR-nn` ids; criteria cite its glossary terms, oracle notes and constraints by name (for example "REQ-REG-01 Glossary: net fee").
- Configured values: fees, deadline, VAT rate, workshops, time zone and rate limit come from the settings in `project/00_setup/environments.md` (`APP_*`). The amounts quoted below are the expected 2026 configuration and are the test expectations; tests read them from configuration, never from literals (REQ-REG-01 "Data and fixtures", AR-04). Configured values prevail over quoted ones (REQ-REG-01 "Constraints").
- Time: "submission time" is the server time at which a valid request is received (Q1). In tests it is set through the test clock (`X-Test-Now`, `project/02_design/architecture.md`); no test depends on the system clock.
- "Stored" is observed through the organizer endpoint `GET /api/registrations/{registrationNumber}` (AC-001-10). "No e-mail sent" is observed through the local mail catcher (Mailpit, `project/00_setup/environments.md`).
- "Valid registration" means a request that satisfies the field rules below.

## Field rules (AC-001-07)

Required fields come from REQ-REG-01 AC7. What counts as "invalid" is not defined there; the rules marked D-08 are the conservative choice recorded in `docs/decisions-log.md` (pending review).

| Field | Rule | Source |
|---|---|---|
| `firstName`, `lastName` | required; not blank after trimming; at most 100 characters; no control characters | AC7; limits and characters D-08 |
| `email` | required; one syntactically valid e-mail address (`local@domain`, domain with a dot); at most 254 characters; no control characters | AC7; format D-08 |
| `payerType` | required; exactly `private` or `company` | AC7 |
| `companyName`, `companyAddress`, `companyVatId` | required when `payerType` is `company`: not blank after trimming; at most 200, 300 and 32 characters; no control characters. No country-specific VAT ID format check. Ignored and not stored when `payerType` is `private` | AC7, AC5; limits and private-payer handling D-08 |
| `workshops` | optional; array of workshop ids; empty or absent means no workshop; at most one element; the element must be an id from `APP_WORKSHOPS` | AC7, AC8; unknown id D-08 |

## Criteria

### AC-001-01 Early-bird fee
- Story: US-001 · Source: REQ-REG-01 AC1; Glossary: submission time, early-bird deadline, net fee, VAT, gross fee
- Given the submission time is on or before the early-bird deadline (`APP_EARLY_BIRD_DEADLINE`, inclusive, in `APP_CONFERENCE_TZ`)
- When a valid registration is submitted
- Then the registration is stored with net fee `APP_FEE_EARLY` (240.00 EUR), VAT 52.80 EUR and gross fee 292.80 EUR.

### AC-001-02 Regular fee
- Story: US-001 · Source: REQ-REG-01 AC2; Glossary: net fee, VAT, gross fee
- Given the submission time is after the early-bird deadline
- When a valid registration is submitted
- Then the registration is stored with net fee `APP_FEE_REGULAR` (300.00 EUR), VAT 66.00 EUR and gross fee 366.00 EUR.
- VAT is net fee × `APP_VAT_RATE`, rounded half up to 0.01; gross fee is net fee + VAT (REQ-REG-01 Oracle notes).

### AC-001-03 Deadline boundary
- Story: US-001 · Source: REQ-REG-01 AC3; Oracle notes (CEST/UTC); AR-05
- Given the conference time zone is Europe/Ljubljana
- When registrations are submitted at 2026-07-31 23:59:59 and at 2026-08-01 00:00:00 local time (2026-07-31T21:59:59Z and 2026-07-31T22:00:00Z)
- Then the first is stored with the early-bird net fee and the second with the regular net fee.

### AC-001-04 Confirmation
- Story: US-001 · Source: REQ-REG-01 AC4; Q3
- Given a registration has been stored
- When the request completes
- Then the API returns 201 with the registration number (in the stored registration body, `project/02_design/architecture.md`), and exactly one confirmation e-mail is sent to the participant's e-mail address, stating the registration number, net fee, VAT and gross fee.

### AC-001-05 Payer data without invoice
- Story: US-001 · Source: REQ-REG-01 AC5; NG3; AR-08
- Given the payer is a company
- When a valid registration is submitted
- Then the company's name, address and VAT ID are stored with the registration.
- Given the payer is the participant as a private person
- When a valid registration is submitted
- Then the registration is stored without company data (`companyName`, `companyAddress`, `companyVatId` are null).
- In both cases no invoice and no payment request are created: the response and the stored registration contain no invoice or payment data, the only e-mail sent is the confirmation of AC-001-04, and the system offers no invoice or payment endpoint.

### AC-001-06 VAT for payers from other EU member states
- Story: US-001 · Source: REQ-REG-01 AC6; Glossary: VAT
- Given the payer is a company from another EU member state (fixture: Beispiel GmbH, VAT ID ATU00000001)
- When a valid registration is submitted
- Then VAT is charged at the Slovenian rate, with the same amounts as in AC-001-01 and AC-001-02.

### AC-001-07 Validation
- Story: US-001 · Source: REQ-REG-01 AC7; SB-01; field rules above
- Given a required field is missing or invalid, or more than one workshop is selected
- When a registration is submitted
- Then the API returns 422 with one error per invalid field, nothing is stored and no e-mail is sent.
- Required fields: `firstName`, `lastName` and `email` of the participant; `payerType` (`private` or `company`); for a company payer also `companyName`, `companyAddress` and `companyVatId`.

### AC-001-08 Workshop selection
- Story: US-001 · Source: REQ-REG-01 AC8; Glossary: workshop
- Given the participant selects one workshop or none
- When a valid registration is submitted
- Then the registration is stored with that workshop (`workshop` = its id) or with no workshop (`workshop` = null), and no capacity is checked.

### AC-001-09 Repeated registration
- Story: US-001 · Source: REQ-REG-01 AC9; Q2, Q3; NG4; Oracle notes
- Given a registration with the same participant e-mail already exists
- When a valid registration is submitted
- Then a second registration is stored with its own registration number and fee, and the existing registration is left unchanged.

### AC-001-10 Organizer reads a stored registration (added)
- Story: US-001 ("so that … the organizer can invoice the fee") · Source: `project/02_design/architecture.md` fixed registration API; `project/02_design/security-requirements.md` (organizer authentication); SB-02
- Reason added: AC-001-01 to AC-001-09 require that data is "stored"; the fixed API defines this endpoint as the way to read it, and access control is a project requirement.
- Given a stored registration
- When the organizer requests `GET /api/registrations/{registrationNumber}` with HTTP Basic credentials `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD`
- Then the API returns 200 with the stored registration: `registrationNumber`, `firstName`, `lastName`, `email`, `payerType`, `companyName`, `companyAddress`, `companyVatId`, `workshop` (id or null), `netFee`, `vat`, `grossFee` (amounts with two decimals).
- Given the request has no credentials or wrong credentials
- Then the API returns 401 and no registration data.
- Given an authenticated request for a registration number that does not exist
- Then the API returns 404.

### AC-001-11 Registration form (added)
- Story: US-001 ("I want to submit a registration") · Source: AR-01 (single registration form page, REST under `/api`); AC-001-04, AC-001-07; behaviour on the page D-07
- Reason added: the participant reaches the API through the frontend; the story needs a form that submits the fields of AC-001-07.
- Given the participant opens the registration page
- When they enter valid participant and payer data, optionally choose one workshop from the configured list, and submit
- Then the page sends one `POST /api/registrations` and shows the registration number, net fee, VAT and gross fee from the response.
- Given the participant chooses payer type "company", then the company name, address and VAT ID fields are shown and required; for "private" they are hidden and not sent.
- Given the API answers 422
- Then the page shows each field's error next to that field, keeps the entered values, and shows no registration number.

### AC-001-12 Rate limit on public registration (added)
- Story: US-001 · Source: REQ-REG-01 "Constraints" (authorization: public endpoint, rate limiting `APP_RATE_LIMIT_PER_HOUR` applies); `project/02_design/security-requirements.md`; SB-06
- Reason added: the constraint is observable behaviour of the public endpoint.
- Given one client has sent `APP_RATE_LIMIT_PER_HOUR` registration requests within the last hour
- When the same client sends another registration request within that hour
- Then the API returns 429, nothing is stored and no e-mail is sent; requests from other clients are not affected.

## Open questions

| ID | Status | Applied as |
|---|---|---|
| OQ-01 (REQ-REG-01 Q1) | resolved | submission time determines the fee: AC-001-01 to AC-001-03 |
| OQ-02 (REQ-REG-01 Q2) | resolved | every registration is kept: AC-001-09 |
| OQ-03 (REQ-REG-01 Q3) | assumed | each registration stores its own fee and gets its own confirmation; choosing the invoiced fee belongs to invoicing (NG3): AC-001-04, AC-001-09 |
| OQ-04 (REQ-REG-01 Q4) | assumed | no privacy acknowledgment field; personal data as in `security-requirements.md`: no criterion adds such a field |
| New gaps | recorded | D-06 (decision rule), D-07 (form behaviour), D-08 (field validity), D-09 (e-mail failure) in `docs/decisions-log.md` |

## Non-goals (no criterion may require them)

NG1 student registration; NG2 cancellation or change; NG3 check-in, payment, invoicing; NG4 detecting or merging repeated registrations; NG5 changing configured values.

## Traceability

| Story | Criteria |
|---|---|
| US-001 | AC-001-01, AC-001-02, AC-001-03, AC-001-04, AC-001-05, AC-001-06, AC-001-07, AC-001-08, AC-001-09, AC-001-10, AC-001-11, AC-001-12 |

API-level tests are required for AC-001-04 and AC-001-07 (REQ-REG-01 "Standard of verification").
