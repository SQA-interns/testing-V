# Quality requirements

> Owner: QA lead · Read in: phases 2, 6, 7 · Agent: read-only

Adds to `general/quality/` (definition of done, test strategy, severity scale); do not repeat those here.

## Non-functional requirements

| ID | Category (performance, availability, accessibility, localisation, usability, …) | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | Localisation | Names and other text with Slovenian characters (č, š, ž) survive the form, database, JSON copy, emails and Excel export unchanged. | an end-to-end test with such input |
| NFR-02 | Reliability | Stored registrations and JSON copies survive recreation of every container. | runtime demonstration: `docker compose down` then `up`, data still present |
| NFR-03 | Usability | The frontend validates fields before submission and shows the backend's field errors next to the fields. | component tests |
| NFR-04 | Availability | The backend reports health and readiness, and the containers use them in health checks. | runtime demonstration |

## Thresholds

| Measure | Threshold or "record only" |
|---|---|
| Line coverage | record only |
| Branch coverage | record only |
| Mutation score | record only; a surviving mutant that a test should plausibly have caught is a finding |

## Additional done criteria

| ID | Criterion | Evidence |
|---|---|---|
| DoD-P01 | Against the running local stack: one external and one student registration succeed. | runtime demonstration log |
| DoD-P02 | Each registration is in PostgreSQL and has its JSON copy. | database query, file listing |
| DoD-P03 | The participant email and the organizer email (with the JSON attachment) arrive in Mailpit. | Mailpit API output |
| DoD-P04 | The organizer downloads a valid Excel workbook; the same request without organizer access is refused. | response codes, opened workbook |
| DoD-P05 | The production reCAPTCHA code path is tested against a mocked verification endpoint, including a rejected token. | test report |

## Overrides

None.
