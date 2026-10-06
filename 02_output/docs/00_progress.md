# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven, acceptance tests first (`general/phases.md`); run `tanej-confreg-C1-r1`, starting commit `9deb956`
- Current phase: 0 (preflight and bootstrap), started 2026-10-06T17:50:55Z
- Last gate result: phase 0 gate not passed: Maven dependency scan reports CVE-2025-7962 (D-15); Playwright browser not installed (D-17). All other checks pass.
- Next step: apply the answers to D-15 and D-17, re-run Dependency-Check on the backend, re-check the gate, then start phase 1 (raise AC4 (invoice) vs AR-08 (invoicing outside this repo) as a decision)
- Waiting for the human on: D-15, D-17 (asked 2026-10-06T19:01Z)

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

## Interruptions

- 2026-10-06 18:20Z (20:20 local): the human's internet connection dropped and the agent's in-flight request failed. Resumed at 18:45Z on the human's request; nothing else changed. The background Dependency-Check run finished in the meantime.
