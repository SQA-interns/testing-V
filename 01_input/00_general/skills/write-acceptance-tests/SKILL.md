---
name: write-acceptance-tests
description: Write and freeze the black-box acceptance tests from the acceptance criteria and contracts, before any production code exists (phase 3).
---

> Owner: QA lead · Read in: phase 3 · Agent: read-only

- Reads: `docs/01_acceptance-criteria.md`, `docs/02_contracts/`, `general/quality/test-strategy.md`, `general/phases.md`
- Writes: acceptance and end-to-end tests, `docs/03_acceptance-manifest.sha256`, `docs/03_test-strategy.md`
- Does not read: production source code

## Procedure

1. For every AC, write at least one test through the component's public interface that checks the observable outcome the AC states. For a rejection AC, also check that nothing changed.
2. Put the AC id in each test name.
3. Run the suite. Every test must fail because the behaviour is missing, not because of a build, configuration or harness error; a test that passes only on bootstrap code is listed in step 4. Fix the harness until that holds.
4. Record the run (pass/fail counts and a one-line reason per group of failures) in `docs/03_test-strategy.md`.
5. Format and lint the tests now; after freezing they cannot change.
6. Commit the tests story by story as you finish them. Then write the manifest (normalised line endings) and commit it alone as the freeze commit.
