# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

## Run

| Item | Value |
|---|---|
| Run id | tanej-confreg-C0-r1 |
| Model / effort | claude-opus-5-5 / medium |
| Template version | 1.2 |
| Start commit | dc8b146309f2597ec60bcbc235616a3865ec3a8a (2026-10-06T14:00:16Z) |
| Final commit | 247ecd5620e978e2f59c3f5640d400c15841befb (the commit before this summary; 2026-10-06T17:13:18Z) |

## Timeline

| Phase | Start (UTC) | End (UTC) | Duration | Note |
|---|---|---|---|---|
| 0 Preflight & bootstrap | 14:00 | 14:25 | 25 min | includes two human answers (D-01/D-02, D-05) |
| 1 Requirements | 14:25 | 14:28 | 3 min | |
| 2 Design | 14:28 | 14:32 | 4 min | |
| 3 Test design | 14:32 | 14:42 | 10 min | |
| 4 Build | 14:42 | 16:10 | 88 min | includes the wait for the human on D-18 (15:08 – 16:09) |
| 5 Unit tests | 16:10 | 16:26 | 16 min | |
| 6 Verify | 16:26 | 17:06 | 40 min | includes a 15-minute Stryker re-run |
| 7 Release | 17:06 | 17:13 | | |

Durations are wall-clock times between phase boundaries, including waiting for the human.

## Outcome

| Item | Value |
|---|---|
| First full test run (phase 5, before fixes) | 173 passed, 5 failed (all defects in new non-frozen tests) |
| Final test run | 178 passed, 0 failed (backend 154, frontend 22, e2e 2) |
| Findings (phase 6) | Critical 0, High 0, Medium 6, Low 5 |
| Fix loops | 3 (F-01 nginx Host header, F-02 redacted log, F-06 two mutation test gaps) |
| Coverage, full suite (line / branch) | backend 96.9 % / 91.7 %, frontend 95.2 % / 89.5 % |
| Mutation score | backend 67 % (PIT, unit tests only); frontend not measurable (F-11) |
| Code (production / test lines) | backend 1938 / 2411, frontend 533 / 564; duplication 0 % / 0.73 % |
| Commits | 57 since the start commit |

## Decisions and human interventions

- 18 decisions: blocking D-01, D-02, D-05 (phase 0) and D-18 (phase 4), all answered by the human; non-blocking D-03, D-04, D-17 resolved; D-06 … D-16 (requirement and design gaps) pending review by the product owner.
- 3 human interventions: 14:04 – 14:08 (D-01 host JDK, D-02 host Node.js approved as installed), 14:19 – 14:24 (D-05: upgrade vitest/jscpd, classify backend scan results), 15:08 – 16:09 (D-18: authorized fix of one frozen-test input and manifest re-hash).

## Not measured by the agent

Cost, tokens, API time and tool calls are added by a human from the harness usage report.
