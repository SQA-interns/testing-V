# User stories

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## Format

- Stories: `US-nnn`, "As a … I want … so that …", plus in-scope and out-of-scope notes.
- Acceptance criteria are derived in phase 1 as `AC-nnn-nn` (Given/When/Then), one observable behaviour each, each citing its `US` and any `BR`. Where the product owner supplies acceptance criteria, phase 1 adopts them without changing their meaning and adds criteria only where they are needed, marked as added.
- Do not invent behaviour a story does not state; record the gap as an `OQ` in `scope.md`.

## Stories

### US-001 Conference registration

As a conference participant, I want to register for the conference, so that my attendance at the conference is reserved.

Acceptance criteria:

- **AC1** Given a registration is submitted on or before the early-bird deadline, when the registration is completed, then the fee is 240 EUR.
- **AC2** Given a registration is submitted after the early-bird deadline, when the registration is completed, then the fee is 300 EUR.
- **AC3** Given a registration has been completed, when the request finishes, then the participant receives a confirmation e-mail with the registration number and the fee.
- **AC4** Given a registration has been completed, when the request finishes, then an invoice is issued to the payer.
