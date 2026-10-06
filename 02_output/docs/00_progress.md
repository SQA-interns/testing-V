# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven phases 0-7 (`general/phases.md`); run `kyuhi-confreg-C1-r1`
- Current phase: 3 (test design), started 2026-10-06T18:07:06Z
- Last gate result: phase 2 passed at 2026-10-06T18:07:06Z. Every AC, SR, SB and NFR maps to `docs/02_specification.md`; all five contracts in `docs/02_contracts/` validate (`tools/contract-check`: `npm run validate`; SQL applied to `postgres:16.15-alpine`; log `out/logs/p2-contract-check.log`). Phase 1 passed at 2026-10-06T18:02:22Z (D-24).
- Next step: phase 3. Write backend acceptance tests (`backend/src/test/java/si/confreg/registration/acceptance/`, Testcontainers PostgreSQL and Mailpit, test clock) and frontend end-to-end tests (`frontend/e2e/`, Playwright) for AC-001-01 to AC-001-18; no production code; run them and record the failures in `docs/03_test-strategy.md`; commit per story, then the freeze commit with `docs/03_acceptance-manifest.sha256` alone.
- Waiting for the human on: nothing
