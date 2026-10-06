# User stories

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## Format

- Stories: `US-nnn`, "As a … I want … so that …", plus in-scope and out-of-scope notes.
- Acceptance criteria are derived in phase 1 as `AC-nnn-nn` (Given/When/Then), one observable behaviour each, each citing its `US` and any `BR`. Where the product owner supplies acceptance criteria, phase 1 adopts them without changing their meaning and adds criteria only where they are needed, marked as added.
- Do not invent behaviour a story does not state; record the gap as an `OQ` in `scope.md`.

## Stories

### US-001 Conference registration

As a user, I want to register for the conference and pay the fee so that I can attend the conference and the workshops.

Acceptance criteria:

1. The user fills in the registration form.
2. The correct price is calculated (early bird / regular).
3. Students register for free.
4. The user receives a confirmation e-mail.
5. An invoice is issued.
