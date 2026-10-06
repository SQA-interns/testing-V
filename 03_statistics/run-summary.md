# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

## Run

| Field | Value |
|---|---|
| Run id | `tanej-confreg-C1-r1` |
| Model / effort | claude-opus-5-5 / medium |
| Template version | 1.2 |
| Start commit | `9deb9569337af90b061eba8a72b80ee53778f99d` (2026-10-06T17:50:55Z) |
| End commit | see `finalCommit` in `run-log.json` (the statistics commit that follows it records the end time) |
| Transcript | `C:/Users/tanej/.claude/projects/c--Users-tanej-Documents-LAB-tanej-confreg-C1/707941d4-fc8f-4bd6-bc1a-a6c8077ff6ab.jsonl` |

## Timeline

Wall-clock per phase from `run-log.json`. Waiting is the overlap with `humanInterventions` (waiting for an answer, or the connection interruption). Agent time is wall-clock minus waiting.

| Phase | Wall-clock | Waiting for the human | Agent time |
|---|---|---|---|
| 0 Preflight and bootstrap | 1:18:16 | 0:37:24 (incl. 0:25:29 connection interruption) | 0:40:52 |
| 1 Requirements | 0:01:42 | 0 | 0:01:42 |
| 2 Design | 0:05:51 | 0 | 0:05:51 |
| 3 Test design | 0:18:07 | 0:06:29 | 0:11:38 |
| 4 Build | 0:33:01 | 0:06:55 | 0:26:06 |
| 5 Unit tests | 0:14:58 | 0 | 0:14:58 |
| 6 Verify | 0:51:30 | 0:02:29 | 0:49:01 |
| 7 Release | from 2026-10-06T21:14:20Z to `end` in `run-log.json` | 0 | same |

The interruption (18:20Z–18:45Z) uses the human's own estimate of 20:20 local time. The start of the phase 2→3 checkpoint wait (19:17:00Z) is recorded to the minute; the other waits use the times the questions were committed.

## Outcome

| Measure | Value |
|---|---|
| First complete run (phase 5, before fixes) | 200 passed, 2 failed (both defects in non-frozen tests) |
| Final run (phase 6) | 203 passed, 0 failed (52 frozen acceptance, 4 frozen e2e, 129 backend unit/integration, 18 frontend) |
| Frozen tests changed after the freeze | 1 line in 1 file, human-approved (D-32 → D-33), with its manifest hash in the same commit |
| Findings (phase 6) | Critical 0, High 0, Medium 4, Low 6 |
| Fix loops | 2 (F-01 redacted generated password in logs; F-02 stronger filter unit tests) |
| Coverage | backend all levels 96.9 % lines / 90.4 % branches (unit only 78.9 % / 85.1 %); frontend 100 % lines / 97.91 % branches |
| Mutation score | backend 81 % (PIT); frontend not measurable with the pinned tools (D-35) |
| Code size (cloc) | backend 1 361 Java + 28 SQL + 40 properties production lines, 1 997 Java test lines; frontend 345 TypeScript production lines, 497 test lines |
| Duplication | 0 (PMD CPD, jscpd) |
| Complexity | not measured: no tool with this purpose in `tech-stack.md` |
| Clean-checkout check | passed (`02_output/logs/07_release/clone-check.log`) |

## Decisions and human interventions

- 35 decision records: 18 blocking (the questions D-01, D-02, D-04, D-07, D-08, D-15, D-17, D-32 and D-34, and the 9 records of the human's answers) and 17 non-blocking. D-20 to D-29 remain pending review (D-30); all others are resolved.
- 6 human interventions:
  1. The connection interruption.
  2. Phase 0 preflight answers: JDK, Node, npm vulnerabilities, Testcontainers findings.
  3. Phase 0 answers on the Angus false positive and the Playwright browser.
  4. The phase 2→3 checkpoint, where no further information was available.
  5. Approval of the one-line change to a frozen test (D-32).
  6. The frontend mutation score decision (D-34).
- Changes to `tech-stack.md` entries approved by the human, not edited: Oracle JDK 21.0.11, Node 24.10.0 / npm 10.9.4, Vitest 5.0.3, jscpd 5.4.0. One unlisted dev dependency was added with a non-blocking record: `@types/node` 24.10.0 (D-31).
- Corrections recorded during the run: estimated timestamps in D-07/D-08 (D-14); the CVSS v3 score of CVE-2025-7962 first read from v4 only (D-15); a wrong freeze-commit hash given once in a message to the human (the documents always had the right one).

## Not measured by the agent

Cost, tokens, API time and tool calls are added by a human from the harness usage report.
