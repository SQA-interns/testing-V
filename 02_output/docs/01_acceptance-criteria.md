# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

Derived from `project/01_requirements/user-stories.md`. The product owner's criteria AC1 to AC4 of US-001 are adopted as AC-001-01 to AC-001-04; all others are marked **added**. `business-rules.md` records no rules (no `BR` ids) and `scope.md` records no open questions; every gap found here is a decision record in `docs/decisions-log.md` (D-18 to D-23, resolved by D-24).

## Conventions used by all criteria

- "A registration" is a `POST /api/registrations` request to the fixed registration API (`project/02_design/architecture.md`). "Completed" means the API answers with a 2xx status and the stored registration. "Rejected" means the API answers with a 4xx status, stores nothing and sends no e-mail.
- "Submitted at" is the backend's current time when it receives the request (from its clock component; the test clock where enabled). The participant cannot supply it.
- Business values come from configuration (`project/00_setup/environments.md`): `APP_EARLY_BIRD_DEADLINE`, `APP_FEE_EARLY`, `APP_FEE_REGULAR`, `APP_VAT_RATE`, `APP_WORKSHOPS`, interpreted in `APP_CONFERENCE_TZ`. The defaults are quoted below only as examples.
- The deadline is a calendar date; "on or before the deadline" means up to and including 23:59:59.999 on that date in `APP_CONFERENCE_TZ` (for the defaults: up to 2026-07-31T21:59:59.999Z).
- The fee in AC1 and AC2 is the amount the payer pays, including VAT (D-19).

## US-001 Conference registration

| ID | Criterion | Source | Decisions |
|---|---|---|---|
| AC-001-01 | **Given** a valid registration submitted on or before the early-bird deadline, **when** the registration is completed, **then** the stored registration's `grossFee` equals `APP_FEE_EARLY` (default 240.00 EUR). | US-001 AC1 | D-19 |
| AC-001-02 | **Given** a valid registration submitted after the early-bird deadline, **when** the registration is completed, **then** the stored registration's `grossFee` equals `APP_FEE_REGULAR` (default 300.00 EUR). | US-001 AC2 | D-19 |
| AC-001-03 | **Given** a registration has been completed, **when** the request finishes, **then** one confirmation e-mail has been sent to the participant's `email`, and it contains the registration number and the fee (`grossFee`, with `netFee` and `vat`). | US-001 AC3 | D-22 |
| AC-001-04 | **Given** a registration has been completed, **when** the request finishes, **then** the invoice data for the payer is stored and available to the accounting system through the organizer API (`GET /api/registrations/{registrationNumber}`): for a private payer the participant's name and e-mail, for a company payer `companyName`, `companyAddress` and `companyVatId`, together with `netFee`, `vat` and `grossFee`. This system does not produce the invoice itself (AR-08). | US-001 AC4 | D-18, D-24 |
| AC-001-05 | **added** · **Given** a completed registration, **when** its amounts are read, **then** `netFee` = `grossFee` / (1 + `APP_VAT_RATE`) rounded half-up to 2 decimals, `vat` = `grossFee` − `netFee`, all in EUR with 2 decimals (defaults: 240.00 = 196.72 + 43.28; 300.00 = 245.90 + 54.10). | US-001 AC1, AC2 | D-19 |
| AC-001-06 | **added** · **Given** a valid registration, **when** it is completed, **then** the response contains the stored registration with a `registrationNumber` and every submitted field as stored, and `GET /api/registrations/{registrationNumber}` by an organizer returns the same registration. | US-001 | – |
| AC-001-07 | **added** · **Given** two completed registrations, including two with the same e-mail address, **when** their numbers are compared, **then** each has a different `registrationNumber`. | US-001 | D-23 |
| AC-001-08 | **added** · **Given** a registration with `firstName`, `lastName`, `email` or `payerType` missing or blank, **when** it is submitted, **then** it is rejected. | US-001 | D-21 |
| AC-001-09 | **added** · **Given** a registration whose `email` is not a syntactically valid e-mail address, **when** it is submitted, **then** it is rejected. | US-001 | D-21 |
| AC-001-10 | **added** · **Given** a registration whose `payerType` is neither `private` nor `company`, **when** it is submitted, **then** it is rejected. | US-001 | D-21 |
| AC-001-11 | **added** · **Given** a registration with `payerType` `company` and `companyName`, `companyAddress` or `companyVatId` missing or blank, **when** it is submitted, **then** it is rejected. | US-001 AC4 | D-21 |
| AC-001-12 | **added** · **Given** a registration with `payerType` `private` and any of `companyName`, `companyAddress` or `companyVatId` given and not blank, **when** it is submitted, **then** it is rejected. | US-001 | D-21 |
| AC-001-13 | **added** · **Given** a registration with a field longer than its limit (`firstName`, `lastName` 100; `email` 254; `companyName` 200; `companyAddress` 500; `companyVatId` 30 characters), **when** it is submitted, **then** it is rejected. | US-001 | D-21 |
| AC-001-14 | **added** · **Given** a valid registration with `workshops` containing exactly one id configured in `APP_WORKSHOPS`, **when** it is completed, **then** the stored registration's `workshop` is that id. | US-001 | D-20 |
| AC-001-15 | **added** · **Given** a valid registration with `workshops` absent or empty, **when** it is completed, **then** the stored registration's `workshop` is null. | US-001 | D-20 |
| AC-001-16 | **added** · **Given** a registration with `workshops` containing an id not configured in `APP_WORKSHOPS`, or more than one entry, **when** it is submitted, **then** it is rejected. | US-001 | D-20 |
| AC-001-17 | **added** · **Given** two registrations submitted at the same time, one with a workshop and one without, **when** both are completed, **then** both have the same `grossFee`. | US-001 AC1, AC2 | D-20 |
| AC-001-18 | **added** · **Given** a valid registration while the confirmation e-mail cannot be handed to the SMTP server, **when** it is submitted, **then** the API answers with a 5xx status that tells the participant to try again, and no registration is stored. | US-001 AC3 | D-22 |

## Traceability

| Story | Acceptance criteria |
|---|---|
| US-001 | AC-001-01 to AC-001-18 |

Every story has at least one criterion and every criterion traces to US-001. Non-functional and security requirements (NFR, SR, SB) are mapped in `docs/02_specification.md` (phase 2).
