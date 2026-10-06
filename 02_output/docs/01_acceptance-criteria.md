# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

Derived from `project/01_requirements/user-stories.md` (US-001), `business-rules.md` (no rules recorded) and `scope.md` (no open questions recorded). Business values are named by their setting (`project/00_setup/environments.md`), never by value (AR-04).

- **Adopted**: a product-owner criterion of US-001, kept with its meaning; the number in brackets is its position in the story.
- **Added**: needed to make an adopted criterion testable or to close a gap; each gap is a decision record in `docs/decisions-log.md` (the agent may not add `OQ` entries to `scope.md`).

"Registration time" is the backend's current time when the registration request is accepted (AR-05: the clock component, which honours `X-Test-Now` when `APP_TEST_CLOCK=enabled`). "Rejected" means: the API answers HTTP 400 with a per-field error list, nothing is stored and no e-mail is sent.

## US-001 Conference registration

### Adopted

| ID | Given | When | Then | Source |
|---|---|---|---|---|
| AC-001-01 | the registration page is open | the user fills in the form with valid data and submits it | the registration is stored, the API answers 201 with the stored registration, and the page shows its registration number and the amounts to pay | US-001 [1] |
| AC-001-02 | a non-student registration | it is submitted | `netFee` is `APP_FEE_EARLY` if the registration time, as a date in `APP_CONFERENCE_TZ`, is on or before `APP_EARLY_BIRD_DEADLINE`, otherwise `APP_FEE_REGULAR`; `vat` = `netFee` × `APP_VAT_RATE`; `grossFee` = `netFee` + `vat` | US-001 [2]; D-09, D-10 |
| AC-001-03 | a registration marked as a student | it is submitted, at any registration time | `netFee`, `vat` and `grossFee` are all 0.00 and the registration is stored as a student registration | US-001 [3]; D-06 |
| AC-001-04 | a registration has been stored | the API has answered | one confirmation e-mail is sent to the registration's `email`, containing the registration number, the participant's name, the selected workshop (or none), `netFee`, `vat` and `grossFee` | US-001 [4] |
| AC-001-05 | registrations have been stored | the organizer downloads the invoicing export | the export (Excel) contains one row per registration with every field the accounting system needs to issue the invoice: registration number, registration time, name, e-mail, payer type, company name, address and VAT ID, workshop, student flag, `netFee`, `vat`, `grossFee` | US-001 [5]; AR-08; D-07 |

### Added: price calculation

| ID | Given | When | Then | Source |
|---|---|---|---|---|
| AC-001-06 | a non-student registration | its registration time is the last instant of `APP_EARLY_BIRD_DEADLINE` in `APP_CONFERENCE_TZ` (23:59:59 local) | `netFee` is `APP_FEE_EARLY` | US-001 [2]; D-10 |
| AC-001-07 | a non-student registration | its registration time is the first instant of the day after `APP_EARLY_BIRD_DEADLINE` in `APP_CONFERENCE_TZ` (00:00:00 local), which is still the deadline date in UTC | `netFee` is `APP_FEE_REGULAR` | US-001 [2]; AR-05; D-10 |
| AC-001-08 | any registration | it is stored | `vat` is rounded half-up to two decimals, and `netFee`, `vat` and `grossFee` are returned with exactly two decimals | US-001 [2]; D-09 |
| AC-001-09 | a registration with payer type `company` and a company VAT ID | it is submitted | the same fee and `APP_VAT_RATE` apply as for a private payer | US-001 [2]; D-09 |
| AC-001-10 | a registration selecting a workshop | it is submitted | the fees are the same as without a workshop (the workshop is included) | US-001; D-11 |

### Added: form data and validation

| ID | Given | When | Then | Source |
|---|---|---|---|---|
| AC-001-11 | a registration request | `firstName`, `lastName`, `email` or `payerType` is missing or blank | it is rejected, with an error for each such field | US-001 [1]; D-13 |
| AC-001-12 | a registration request | `email` is not a valid e-mail address | it is rejected | US-001 [1]; D-13 |
| AC-001-13 | a registration request | `payerType` is neither `private` nor `company` | it is rejected | US-001 [1] |
| AC-001-14 | a registration request with payer type `company` | `companyName` or `companyAddress` is missing or blank | it is rejected; `companyVatId` is optional | US-001 [1]; D-13 |
| AC-001-15 | a registration request with payer type `private` | `companyName`, `companyAddress` or `companyVatId` is not blank | it is rejected (company data is never silently dropped) | US-001 [1]; D-13 |
| AC-001-16 | the workshops configured in `APP_WORKSHOPS` | a registration selects none or exactly one of them | it is stored with `workshop` set to that id, or null when none is selected | US-001; D-11 |
| AC-001-17 | a registration request | `workshops` contains more than one id, or an id not in `APP_WORKSHOPS` | it is rejected | US-001; D-11 |
| AC-001-18 | a registration exists for an e-mail address | another registration is submitted with the same address (ignoring letter case and surrounding spaces) | the API answers 409, nothing is stored and no e-mail is sent | US-001; D-12 |
| AC-001-19 | a field value longer than its limit in the specification | it is submitted | it is rejected | US-001 [1]; SB-01 |

### Added: registration form (UI)

| ID | Given | When | Then | Source |
|---|---|---|---|---|
| AC-001-20 | the registration page is opened | it loads | it shows the fields first name, last name, e-mail, payer type (private / company), a "student" checkbox (not preselected), and the workshops from `APP_WORKSHOPS` as a single choice including "no workshop" | US-001 [1]; D-06, D-11 |
| AC-001-21 | the registration page | payer type `company` is chosen | company name, company address and company VAT ID fields are shown; with `private` they are hidden and not sent | US-001 [1]; D-13 |
| AC-001-22 | the registration page | it loads, or the user changes the student checkbox | the page shows the amounts that apply now (`netFee`, `vat`, `grossFee`), as calculated by the backend for the current time | US-001 [2]; D-06 |
| AC-001-23 | the registration page | the backend rejects a submission | the page shows the error next to each affected field and keeps every entered value | US-001 [1] |
| AC-001-24 | the registration page | the backend answers 409 (duplicate e-mail) or is unreachable | the page shows an understandable message without internal details and keeps the entered values | US-001 [1]; D-12; ES-07 |

### Added: confirmation, payment and invoicing

| ID | Given | When | Then | Source |
|---|---|---|---|---|
| AC-001-25 | a non-student registration has been stored | the confirmation e-mail is sent | it states the gross amount to pay and that the invoice will be sent separately by the organizer's accounting | US-001 [4], [5]; D-07, D-08 |
| AC-001-26 | a student registration has been stored | the confirmation e-mail is sent | it states that participation is free of charge and that student status may be checked by the organizer | US-001 [3], [4]; D-06 |
| AC-001-27 | the SMTP server is unavailable | a valid registration is submitted | the registration is still stored and answered with 201, and the confirmation e-mail is sent once the SMTP server is available again | US-001 [4]; D-14 |

### Added: organizer access

| ID | Given | When | Then | Source |
|---|---|---|---|---|
| AC-001-28 | a stored registration | the organizer requests `GET /api/registrations/{registrationNumber}` with valid HTTP Basic credentials | the API answers 200 with the stored registration | US-001 [5]; architecture "Fixed registration API" |
| AC-001-29 | a stored registration | `GET /api/registrations/{registrationNumber}` or the invoicing export is requested without credentials or with wrong ones | the API answers 401 and returns no registration data | US-001; SB-02 |
| AC-001-30 | the organizer is authenticated | an unknown registration number is requested | the API answers 404 | US-001 |

## Traceability

| Story | Acceptance criteria |
|---|---|
| US-001 | AC-001-01 … AC-001-30 |

Every criterion above traces to US-001; US-001 has 30 criteria.

## Gaps and conflicts

`project/01_requirements/scope.md` records no open questions. The gaps found while deriving these criteria are decided as the more conservative behaviour and recorded as "pending review" (`general/working-rules.md`), so this phase does not wait for them:

| Gap or conflict | Decision |
|---|---|
| How a student is identified; the fixed API has no such field | D-06 |
| US-001 [5] "an invoice is issued" vs. AR-08 (invoices come from the accounting system) | D-07 |
| How the user pays the fee | D-08 |
| Whether fees are net or gross; VAT for companies; rounding | D-09 |
| Which time and boundary decide early bird | D-10 |
| Number of workshops per registration (`workshops` array vs. stored `workshop`); price; capacity | D-11 |
| Repeated registration with the same e-mail address | D-12 |
| Which fields are required for each payer type | D-13 |
| What happens to a registration when the e-mail cannot be sent | D-14 |
