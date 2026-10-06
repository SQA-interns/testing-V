# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

## Run

| Item | Value |
|---|---|
| Run id | kyuhi-confreg-C1-r1 |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.2 |
| Start | 2026-10-06T17:26:13Z, commit `a33129c` |
| End | 2026-10-06T19:51:57Z, final content commit `5f4e610` (followed only by the statistics commit that records this summary) |
| Wall-clock duration | 146 min |

## Timeline

Times are UTC on 2026-10-06. Phase durations include any wait for the human inside the phase; gaps between phases are time waiting for "continue".

| Phase | Name | Start | End | Minutes |
|---|---|---|---|---|
| 0 | preflight-and-bootstrap | 17:26:13 | 17:56:54 | 31 |
| 1 | requirements | 17:57:50 | 18:02:22 | 5 |
| 2 | design | 18:02:22 | 18:07:06 | 5 |
| 3 | test-design | 18:07:06 | 18:15:46 | 9 |
| 4 | build | 18:15:46 | 18:46:00 | 30 |
| 5 | unit-tests | 18:47:04 | 18:58:57 | 12 |
| 6 | verify | 19:04:17 | 19:36:19 | 32 |
| 7 | release | 19:45:04 | 19:51:57 | 7 |

Waiting for answers to decisions: 26 min in 4 interventions (below).

## Outcome

| Measure | Value |
|---|---|
| First complete run (phase 5, all levels) | 200 passed, 1 failed (implementation defect, fixed) |
| Final run (phase 6) | 203 passed, 0 failed |
| Findings (phase 6) | Critical 0, High 0, Medium 3, Low 6 |
| Fix loops | 1 (F-01 to F-05) |
| Backend | 1274 production LOC, 2141 test LOC; line coverage 97.2%, branch 90.3%; mutation 85% |
| Frontend | 441 production LOC, 462 test LOC; line coverage 93.7%, branch 97.3%; mutation 77.8% |
| Duplication | 0% (CPD, jscpd) |
| Complexity | not measured: no tool with purpose complexity is listed in tech-stack.md; PMD design rules ran with no violations |

Frozen acceptance tests: two appeared wrong in phase 4 (D-28, D-29); the human approved the corrections (D-33, D-34) and the manifest was re-frozen once.

## Decisions and human interventions

36 decision records: 22 blocking (questions and the follow-up records holding the answers) and 14 non-blocking. Decisions still pending review are listed in `02_output/docs/release-notes.md`.

| Asked | Answered | Minutes | Reason |
|---|---|---|---|
| 17:39:03 | 17:46:32 | 7 | Phase 0 preflight: D-01 Docker version, D-04 invalid NVD API key, D-05 vulnerable dev tooling, D-06 Playwright Chromium install, D-07 username values equal repository path segments |
| 17:48:42 | 17:55:48 | 7 | Phase 0 re-check: D-13 organizer username still in repository paths, D-14 approve false-positive classification of CVE-2025-7962 |
| 17:59:35 | 18:02:22 | 3 | Phase 1: D-18 US-001 AC4 invoice conflicts with AR-08; confirm D-19 fee includes VAT |
| 18:33:39 | 18:42:24 | 9 | Phase 4: D-28 and D-29 frozen acceptance tests appear wrong; D-31 host ports 8080/5173 in use |

## Not measured by the agent

Cost, tokens, API time and tool calls are added by a human from the harness usage report.
