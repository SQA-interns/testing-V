# Quality requirements

> Owner: QA lead · Read in: phases 2, 6, 7 · Agent: read-only

Adds to the general quality rules (definition of done, tests, severity scale); do not repeat those here.

## Non-functional requirements

| ID | Category | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | Localisation | Names and addresses with Slovenian characters (č, š, ž) survive storage and e-mail unchanged. | an integration test with such input |
| NFR-02 | Availability | The backend reports health and readiness, and the containers use them in health checks. | runtime demonstration |

## Thresholds

| Measure | Threshold or "record only" |
|---|---|
| Line coverage | record only |
| Branch coverage | record only |
| Mutation score | record only |

## Additional done criteria

| ID | Criterion | Evidence |
|---|---|---|
| DoD-P01 | Against the running local stack, a registration request through the fixed registration API is accepted and stored. | runtime demonstration log |

## Overrides

None.
