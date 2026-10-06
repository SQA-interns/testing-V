# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

Derived from `project/01_requirements/user-stories.md`. AC-001-01 to AC-001-04 are the product owner's AC1 to AC4, adopted verbatim. Criteria marked **added** cover behaviour that the fixed registration API (`project/02_design/architecture.md`) requires and the story leaves open. Each added criterion cites the decision that chose the behaviour (`docs/decisions-log.md`). No business rules (`BR`) are recorded; `scope.md` lists no open questions, so gaps are recorded as decisions.

Common interpretation, from the inputs, not a change of meaning:

- "Submitted" means the backend's current time when it receives `POST /api/registrations`, read from the single clock component (AR-05; the test clock `X-Test-Now` in test and local).
- The early-bird deadline `APP_EARLY_BIRD_DEADLINE` is a date in `APP_CONFERENCE_TZ` (Europe/Ljubljana); "on or before" includes the whole deadline day up to 23:59:59.999 local time (AR-05).
- "Fee" amounts, VAT rate and workshops come from configuration (AR-04). The figures below are the defaults from `project/00_setup/environments.md`.
- "Registration is completed" means `POST /api/registrations` answered with a 2xx status and the stored registration.

## US-001 Conference registration

As a conference participant, I want to register for the conference, so that my attendance at the conference is reserved.

| ID | Source | Given | When | Then | Decisions |
|---|---|---|---|---|---|
| AC-001-01 | PO AC1 | a registration is submitted on or before the early-bird deadline | the registration is completed | the fee is 240 EUR (`APP_FEE_EARLY`) | D-21 (net fee) |
| AC-001-02 | PO AC2 | a registration is submitted after the early-bird deadline | the registration is completed | the fee is 300 EUR (`APP_FEE_REGULAR`) | D-21 (net fee) |
| AC-001-03 | PO AC3 | a registration has been completed | the request finishes | the participant receives a confirmation e-mail, at the submitted address, with the registration number and the fee | D-21, D-25 |
| AC-001-04 | PO AC4 | a registration has been completed | the request finishes | an invoice is issued to the payer | D-20 (invoice produced by the accounting system, AR-08) |
| AC-001-05 | added | a valid registration is submitted | the registration is completed | the response contains the stored registration with a new unique `registrationNumber` and the submitted values; an organizer retrieving `GET /api/registrations/{registrationNumber}` gets the same registration | – |
| AC-001-06 | added | a registration has been completed | its amounts are returned | `netFee` is the fee of AC-001-01 or AC-001-02, `vat` is `netFee` × `APP_VAT_RATE` rounded half-up to 2 decimals, and `grossFee` = `netFee` + `vat` (defaults: 240.00 / 52.80 / 292.80 and 300.00 / 66.00 / 366.00) | D-21 |
| AC-001-07 | added | a registration lists no workshop or exactly one configured workshop id | the registration is completed | the stored `workshop` is that id, or null when none is listed; the fee does not change | D-22 |
| AC-001-08 | added | a registration is missing a required field, has an invalid value, lists more than one or an unknown workshop, or has company fields that do not match `payerType` | it is submitted | the request is rejected with a 4xx status naming the invalid fields; no registration is stored and no e-mail is sent | D-23 |
| AC-001-09 | added | the confirmation e-mail cannot be handed to the SMTP server | a valid registration is submitted | the request fails with a 5xx status without internal details, and no registration is stored | D-25 |
| AC-001-10 | added | a participant submits a second valid registration with the same e-mail address | it is submitted | it is completed as a separate registration with its own registration number | D-24 |

## Required fields and validation (AC-001-08, D-23)

| Field | Rule |
|---|---|
| `firstName`, `lastName` | required; 1–100 characters after trimming; letters of any script including č, š, ž (NFR-01), spaces, hyphen, apostrophe, period; no control characters |
| `email` | required; a single syntactically valid address, at most 254 characters; no CR, LF or other control characters (SR-05) |
| `payerType` | required; `"private"` or `"company"` |
| `companyName`, `companyAddress`, `companyVatId` | `company`: all three required (name 1–200, address 1–500 characters, VAT ID 2–20 letters and digits); `private`: all three absent, null or empty |
| `workshops` | optional; absent, null or empty means none; at most one element, which must be a configured workshop id (`APP_WORKSHOPS`) |
| any other field | rejected (unknown properties are not stored, SB-12) |

## Traceability

| Story | Acceptance criteria |
|---|---|
| US-001 | AC-001-01 … AC-001-10 |
