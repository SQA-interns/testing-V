# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven, acceptance tests first (`general/phases.md`); run `tanej-confreg-C1-r1`, starting commit `9deb956`
- Current phase: 3 (test design), started 2026-10-06T19:16:44Z
- Last gate result: phase 2 gate passed 2026-10-06T19:16:44Z (traceability complete; 4 contracts valid, `out/logs/02_design/contract-validation.log`)
- Next step: write acceptance tests (backend, through the HTTP API with Testcontainers PostgreSQL and Mailpit) and end-to-end tests (Playwright against the form) for AC-001-01 to AC-001-10 and ArchUnit rules for AR-02/AR-03; no production code; then the freeze commit
- Waiting for the human on: nothing

## Done in phase 0

- `03_statistics/run-log.json` created from the template
- Run configuration complete; platform and tool versions checked (Java and Node mismatched: D-01, D-02; cloc tag: D-03)
- Required secrets present in `.env` (SMTP keys marked not needed)
- All 60 `tech-stack.md` pins resolve from their sources
- `npm audit` run: Critical/High in vitest 3.2.7 and jscpd 4.3.0 (D-04); scan method D-05
- Dependency-Check run 1 failed on OSS Index 401 and missing dotnet (D-06); run 2 passed the run, findings D-07, D-08
- `docs/00_preflight-report.md` written; raw outputs in `out/logs/00_preflight/`
- `docs/00_input-manifest.sha256` written (25 files)
- ES-03 repository files committed (`7868425`)
- Human answered D-01, D-02, D-04, D-07, D-08 at 18:54Z (recorded as D-09 to D-13); timestamp correction D-14
- Backend skeleton builds and checks clean (`e54e530`); frontend skeleton builds, checks clean, lock matches (`d572bb6`)
- Re-run of failed checks: platforms pass (as amended), npm audit passes, Dependency-Check finds CVE-2025-7962 (D-15); D-16 suppression scope

- Human answered D-15 and D-17 at 19:07Z (D-18, D-19); backend scan passes; Playwright Chromium installed; all tools run
- Phase 0 gate passed 2026-10-06T19:09:11Z

- Phase 1: `docs/01_acceptance-criteria.md` (AC-001-01 to AC-001-10); decisions D-20 to D-25 pending review; gate passed 2026-10-06T19:10:53Z

- Phase 2: `docs/02_specification.md`, contracts (OpenAPI, SQL, e-mail, form) and `out/tools/validate_contracts.py`; decisions D-26 to D-29 pending review; gate passed 2026-10-06T19:16:44Z

## Interruptions

- 2026-10-06 18:20Z (20:20 local): the human's internet connection dropped and the agent's in-flight request failed. Resumed at 18:45Z on the human's request; nothing else changed. The background Dependency-Check run finished in the meantime.
