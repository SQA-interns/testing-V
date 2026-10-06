# User stories

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## Format

- Stories: `US-nnn`, "As a … I want … so that …", plus in-scope and out-of-scope notes.
- Acceptance criteria are derived in phase 1 as `AC-nnn-nn` (Given/When/Then), one observable behaviour each, each citing its `US` and any `BR`.
- Do not invent behaviour a story does not state; record the gap as an `OQ` in `scope.md`.

## Stories

Epic: as a conference organizer, I want participants to register online, so that registrations are collected and managed reliably.

### US-001 External participant registration

As an external participant, I want to register using the external participant form, so that I can attend the conference and the activities I select.
In scope: the external form (BR-01), selecting active options (BR-04). Out of scope: payment, accounts, editing a submitted registration.

### US-002 Student registration

As a student, I want to register using the student form, so that I can attend the conference and the activities available to students.
In scope: the student form (BR-01), selecting active options (BR-04). Out of scope: verifying student status externally, accounts.

### US-003 Configurable conference options

As an organizer, I want workshops, events, meals and other optional activities to be configurable, so that the system can be reused when the programme changes.
Out of scope: an administration UI for options.

### US-004 Registration confirmation

As a participant, I want a clear confirmation in the application after my registration is processed, so that I know it was received (BR-06).

### US-005 Reliable registration storage

As an organizer, I want every accepted registration stored reliably, so that none is lost and each can be recovered (BR-07).

### US-006 Participant email confirmation

As a participant, I want an email confirming my registration, so that I have a record of it.

### US-007 Organizer notification

As an organizer, I want an email when someone registers, containing the submitted data with the raw registration JSON attached, so that I can monitor incoming registrations.

### US-008 Registration export

As an organizer, I want to export the current registrations as an Excel workbook, so that I can process them outside the system (BR-08).
