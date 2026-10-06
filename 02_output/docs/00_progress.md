# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven, acceptance tests first (`general/phases.md`); run `tanej-confreg-C1-r1`, starting commit `9deb956`
- Current phase: 4 (build), started 2026-10-06T19:34:51Z
- Last gate result: phase 4 gate not passed: 50/52 backend acceptance tests pass; the 2 AC-001-09 tests fail because of a frozen-harness defect (D-32); 4/4 e2e pass; checks clean
- Next step: after D-32 is answered, apply the approved one-line fix and manifest update in one commit, re-run the full backend suite, then close phase 4 and start phase 5 (unit and integration tests)
- Waiting for the human on: D-32 (asked 2026-10-06T19:56Z)

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

- Phase 3: 52 backend acceptance tests and 4 e2e tests, all failing on the skeleton for behavioural reasons except 3 vacuous ArchUnit passes; D-30, D-31; frozen at `05ef03e`; gate passed 2026-10-06T19:34:51Z

- Phase 4: backend (domain, application, persistence, mail, clock, security, config, api) and frontend form implemented; Dockerfiles and `docker-compose.yml`; stack healthy; e2e 4/4; AC-001-09 verified at runtime (`out/logs/04_build/runtime-mail-failure-check.log`); D-32 raised

## Interruptions

- 2026-10-06 18:20Z (20:20 local): the human's internet connection dropped and the agent's in-flight request failed. Resumed at 18:45Z on the human's request; nothing else changed. The background Dependency-Check run finished in the meantime.
