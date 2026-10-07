# Usage (human)

> Owner: Experiment lead · Filled: prices before the post-run session, the rest after the run · Agent: reads prices only

## Prices used for `usage.costUsd`

Source and date: Anthropic first-party API list prices, from the Claude Code `claude-api` skill model table (cached 2026-09-25); cache-write multipliers (1.25× for 5-minute TTL, 2× for 1-hour TTL) from the same skill's prompt-caching reference. All run calls used `claude-opus-5-5`, and every cache write was 1-hour TTL.

| Model | Input / MTok | Output / MTok | Cache read / MTok | Cache write / MTok |
|---|---|---|---|---|
| claude-opus-5-5 | $4.00 | $20.00 | $0.20 | $5.00 (5 min) · $8.00 (1 h, used) |

## From the harness usage panel

Panel captured 2026-10-07 in the post-run session. The run used a subscription (Claude Code "using your subscription"), so the panel shows plan-limit percentages, not a dollar cost, and covers all local sessions, not only this run.

| Item | Value |
|---|---|
| API time | not shown by the panel |
| Approval prompts | not shown by the panel |
| Cost shown | no USD cost shown (subscription). Panel: current session 13% used; current week (all models) 87% used; last 24h 740 requests / 9 sessions; last 7d 3125 requests / 16 sessions; model breakdown opus 100%; cache hit 98% |
| Difference to `usage.costUsd` | not computable (no USD cost shown). Cross-check: transcript cache-read share of input-side tokens is 98.3%, matching the panel's 98% cache hit |
