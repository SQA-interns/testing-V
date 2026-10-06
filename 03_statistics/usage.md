# Usage (human)

> Owner: Experiment lead · Filled: prices before the post-run session, the rest after the run · Agent: reads prices only

## Prices used for `usage.costUsd`

Source and date: Anthropic first-party API list prices, Claude Code `claude-api` skill model table (cached 2026-09-25); cache-write multipliers 1.25× (5 min TTL) and 2× (1 h TTL) from the same source. All 488,865 cache-write tokens in the run were 1 h TTL.

| Model | Input / MTok | Output / MTok | Cache read / MTok | Cache write / MTok |
|---|---|---|---|---|
| claude-opus-5-5 | $4.00 | $20.00 | $0.20 | $5.00 (5 min) / $8.00 (1 h) |

## From the harness usage panel

| Item | Value |
|---|---|
| API time | not shown by the panel |
| Approval prompts | not shown by the panel |
| Cost shown | none per run: plan is Pro (subscription), so the panel shows only limits (5-hour 58 %, weekly 76 %) and month-to-date extra usage €14.13 of €100.00, read 2026-10-06 ~20:03 UTC |
| Difference to `usage.costUsd` | not computable: no per-run cost shown (`usage.costUsd` = $22.28) |
