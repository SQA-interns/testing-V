# Metrics

> Owner: Experiment lead · Read in: every phase and post-run · Agent: read-only

Experiment measurement only. Record observed values; never estimate. An unobservable value stays `null` with a `<field>Reason`.

## 1. Agent, during the run → `run-log.json`

Phase 0, first action: copy `run-log.template.json` to `run-log.json`.

| Field | When | How |
|---|---|---|
| `runId`, `model`, `effort`, `templateVersion` | phase 0 | copied from `project/00_setup/run-config.md` |
| `startCommit`, `start` | phase 0, first action | git HEAD, UTC timestamp |
| `transcript` | phase 0 | path of this session's transcript file, if the harness keeps one (Claude Code: newest `*.jsonl` in `~/.claude/projects/<escaped project path>/`) |
| `phases[].start`, `phases[].end` | each phase boundary | UTC timestamp |
| `firstTestRun`, `finalTestRun` | first full run before fixes; last full run | passed and failed counts, all levels together |
| `findings` | end of phase 6 | counts by severity, as found |
| `fixLoops[]` | each verify → fix → re-verify loop | `{loop, findings, change, start, end}` |
| `decisions[]` | each decision record | `{id, type, start, end}`; `end` = when resolved |
| `humanInterventions[]` | each time the human is asked or acts | `{start, end, reason}` |
| `codeMetrics` | phase 6 | per component: production and test lines of code (tool with purpose `code-metrics`), complexity, duplication |
| `commits[]` | not recorded by the agent | filled after the run from `git log`, so frequent commits cost no extra calls |
| `finalCommit`, `end` | last action | git HEAD, UTC timestamp |

## 2. Agent, post-run session → `run-log.json` → `usage`

Started after the run ends, in a new session whose only task is: "Fill section 2 of `03_statistics/metrics.md`." It does not change anything else.

| Field | How |
|---|---|
| `usage.inputTokens`, `outputTokens`, `cacheReadTokens`, `cacheWriteTokens` | sum over every model call in `transcript`, each call counted once |
| `usage.modelCalls` | number of distinct model calls |
| `usage.toolCalls` | total and per tool name, from the tool-use entries |
| `usage.costUsd` | tokens × the prices in `usage.md`; record `usage.priceTableDate` |

If no transcript exists (another harness), leave these `null` with a reason.

## 3. Human → `usage.md`

API time, approval prompts, the cost shown by the harness usage panel (cross-check for `usage.costUsd`), and the price table used in section 2.
