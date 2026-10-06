# Test strategy

> Owner: QA lead · Read in: every phase that writes or runs tests · Agent: read-only

When each test level is written and whether it is frozen is defined in `general/phases.md`. Project thresholds are in `project/02_design/quality-requirements.md`.

## Constraints

- Do not test acceptance behaviour through internals; use the component's public interface (API, UI, CLI, messages).
- Do not depend on live external services in automated tests; use the local substitutes in `project/00_setup/environments.md` and list the real services in the release notes for manual testing.
- Do not leave flaky, skipped or disabled tests; fix them or record a decision.
- Do not weaken an assertion to make an incorrect implementation pass.
- Test names contain the AC id they verify.

## First complete run

Record the result of the first full run before fixing anything, then classify each failure:

| Class | Action |
|---|---|
| Implementation defect | fix the code |
| Defect in a non-frozen test | fix the test and say so in `docs/03_test-strategy.md` |
| Frozen test appears wrong | blocking decision; do not touch the test |

## Measures (recorded in every project)

| Measure | Scope |
|---|---|
| Line and branch coverage | per component, unit and integration separately |
| Mutation score | validation, security, persistence and business-rule code |
| Test counts | per level, passed and failed |

## Manifest

`docs/03_acceptance-manifest.sha256` uses `sha256sum` format with paths relative to `02_output/`, hashed after LF normalisation.
