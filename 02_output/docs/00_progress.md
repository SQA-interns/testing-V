# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven, acceptance tests first (`general/phases.md`); run `tanej-confreg-C1-r1`, starting commit `9deb956`
- Current phase: 6 (verify), started 2026-10-06T20:22:50Z
- Last gate result: phase 5 gate passed 2026-10-06T20:22:50Z (first run 200/202 recorded and classified; full suite 202/202)
- Next step: phase 6 per `general/skills/verify-release`: hashes, full suite with coverage and mutation, runtime demonstration, SB/SR evidence, scanners (Dependency-Check, npm audit, semgrep, gitleaks), secret-leak check, traceability, DoD, `docs/06_verification-report.md`
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

- Phase 3: 52 backend acceptance tests and 4 e2e tests, all failing on the skeleton for behavioural reasons except 3 vacuous ArchUnit passes; D-30, D-31; frozen at `05ef03e`; gate passed 2026-10-06T19:34:51Z

- Phase 4: backend (domain, application, persistence, mail, clock, security, config, api) and frontend form implemented; Dockerfiles and `docker-compose.yml`; stack healthy; e2e 4/4; AC-001-09 verified at runtime (`out/logs/04_build/runtime-mail-failure-check.log`); D-32 raised

- D-32 answered (D-33, 20:03Z): one line added to the frozen AC-001-09 test, manifest hash updated in the same commit; phase 4 gate passed 2026-10-06T20:07:52Z

- Phase 5: 128 backend unit/integration and 18 frontend tests; first run 200 passed / 2 failed (both non-frozen test defects, fixed); full suite 202/202; gate passed 2026-10-06T20:22:50Z

## Interruptions

- 2026-10-06 18:20Z (20:20 local): the human's internet connection dropped and the agent's in-flight request failed. Resumed at 18:45Z on the human's request; nothing else changed. The background Dependency-Check run finished in the meantime.
