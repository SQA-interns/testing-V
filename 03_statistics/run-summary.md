# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

## Run

| Item | Value |
|---|---|
| Run id | `kyuhi-confreg-C2-r1` |
| Model / effort | `claude-opus-5-5` / medium |
| Template version | 1.2 |
| Start commit | `2bab314` (2026-10-06T22:18:37Z) |
| End commit | recorded in `run-log.json` (`finalCommit`): the commit before the final `run-log.json` commit |
| Transcript | path in `run-log.json` (`transcript`) |

## Timeline

Phase boundaries are UTC timestamps from `run-log.json`. Agent time subtracts the recorded waits for a human answer; the wait for the first human message in phase 0 has no observable timestamps and is not subtracted.

| Phase | Start | End | Elapsed | Waiting for the human | Agent time |
|---|---|---|---|---|---|
| 0 Preflight and bootstrap | 22:18:37 | 22:44:26 | 26 min | 9 min (D-04, D-05) | 17 min |
| 1 Requirements | 22:44:26 | 22:46:12 | 2 min | – | 2 min |
| 2 Design | 22:46:12 | 22:56:01 | 10 min | 5 min (D-10) | 5 min |
| 3 Test design | 22:56:01 | 23:07:27 | 11 min | – | 11 min |
| 4 Build | 23:07:27 | 23:26:05 | 19 min | – | 19 min |
| 5 Unit tests | 23:26:05 | 23:35:05 | 9 min | – | 9 min |
| 6 Verify | 23:35:05 | 06:54:11 (+1 day) | 439 min | 418 min (D-16) | 21 min |
| 7 Release | 06:54:11 | see `run-log.json` `end` | – | – | – |

## Outcome

| Measure | Value |
|---|---|
| First full run (phase 5) | 185 passed, 0 failed |
| Final full run (phase 6) | 190 passed, 0 failed (backend 167, frontend 20, end-to-end 3) |
| Frozen tests | 65 (52 backend acceptance, 10 frontend acceptance, 3 end-to-end); all failed for behavioural reasons at the freeze, all pass now |
| Findings, as reported | Critical 0, High 1, Medium 4, Low 5 |
| Findings after triage | High F-01 lowered to Low with the human (D-17); 2 fixed (F-07, F-08); 1 open item (F-09, D-16); rest accepted with reasons |
| Fix loops | 3 (F-07 fixed, F-08 fixed by tests, F-04 no fix available) |
| Coverage, backend | unit 70.0 % lines / 83.8 % branches; integration and acceptance 88.0 % / 69.7 % |
| Coverage, frontend | unit 80.8 % / 75.4 %; component acceptance 92.3 % / 82.5 % |
| Mutation score | backend 82 % (PIT); frontend not measurable (D-16) |
| Code size (cloc) | backend 1,546 production / 2,487 test lines; frontend 440 / 589; duplication 0 % |
| Clean checkout | READMEs followed from a fresh clone: build, checks, tests, stack and end-to-end tests pass (`02_output/logs/07_clean-checkout.log`) |

## Decisions and human interventions

- 17 decision records: 5 blocking (D-04, D-05, D-10, D-16 answered by the human; D-17 answered in advance), 12 non-blocking, all pending review (D-01 to D-03, D-06 to D-09, D-11 to D-15).
- Human interventions:
  1. Phase 0: a tool call was rejected with an answer, given in advance, to the later HTTP Basic finding; it was applied in phase 6 (D-17).
  2. Phase 0: answers to D-04 (CVE false positive) and D-05 (upgrade vitest, coverage-v8, jscpd).
  3. Phase 2: answer to D-10 (public workshop endpoint).
  4. Phase 6: answer to D-16 (frontend mutation score not measurable).
- Consequence worth noting: the D-05 upgrade to vitest 5.0.3 removed the Critical advisories but made Stryker 10.0.0 ineffective (D-16).

## Not measured by the agent

Cost, tokens, API time and tool calls are added by a human from the harness usage report.
