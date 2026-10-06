# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

## Run

| Item | Value |
|---|---|
| Run id | `tanej-confreg-C2-r1` |
| Model / effort | claude-opus-5-5 / medium |
| Template version | 1.2 |
| Start | 2026-10-06T10:55:19Z, commit `8b4fb64` |
| End | 2026-10-06T13:28:33Z, final commit in `run-log.json` (`finalCommit`; this summary is committed after it) |
| Transcript | `run-log.json` → `transcript` |

## Timeline

Wall-clock time between phase boundaries, including time spent waiting for the human (agent time only is not observable).

| Phase | Start (UTC) | End (UTC) | Minutes |
|---|---|---|---|
| 0 Preflight & bootstrap | 10:55 | 11:18 | 23 |
| 1 Requirements | 11:18 | 11:20 | 2 |
| 2 Design | 11:20 | 11:28 | 8 |
| 3 Test design | 11:28 | 11:42 | 14 |
| 4 Build | 11:42 | 12:04 | 22 |
| 5 Unit tests | 12:04 | 12:57 | 53 |
| 6 Verify | 12:57 | 13:24 | 28 |
| 7 Release | 13:24 | 13:29 | 4 |
| **Total** | | | **153** |

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 46: backend acceptance 38, frontend acceptance 6, end-to-end 2; all failed for behavioural reasons before the build |
| First complete run (phase 5, before any fix) | 158 passed, 0 failed |
| Final run (phase 6) | 177 passed, 0 failed |
| Coverage, all tests | backend 99.2 % lines / 95.9 % branches; frontend 95.9 % / 97.0 % |
| Mutation score | backend 94 % (PIT, 196 mutants); frontend 82.7 % (Stryker, 249 mutants) |
| Findings as found | Critical 0, High 1, Medium 5, Low 4 |
| Findings after triage | no open Critical or High; F-02 lowered to Low by the human (D-18) |
| Fix loops | 2 (F-01; F-04 and F-05) |
| Code (cloc) | backend 1365 production / 2276 test lines; frontend 407 / 507 |
| Duplication | backend 0 (CPD, 50 tokens); frontend 2.9 % (jscpd) |

## Decisions and human interventions

| | Count | Ids |
|---|---|---|
| Decisions | 18 | blocking: D-01, D-02, D-03, D-04, D-06, D-08, D-18; non-blocking: D-05, D-07, D-09..D-17 |
| Resolved | 11 | D-01..D-09, D-13, D-18 |
| Pending review (non-blocking) | 7 | D-10, D-11, D-12, D-14, D-15, D-16, D-17 |
| Human interventions | 3 | phase 0 preflight (JDK, Node/npm, vitest, jscpd, secret check), 6 min; phase 0 CVE-2025-7962 downgrade, 3 min; phase 6 Semgrep High downgrade, 3 min |

## Not measured by the agent

Cost, tokens, API time and tool calls are added by a human from the harness usage report.
