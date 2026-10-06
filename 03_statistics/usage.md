# Usage (human)

> Owner: Experiment lead · Filled: prices before the post-run session, the rest after the run · Agent: reads prices only

## Prices used for `usage.costUsd`

Source and date: Anthropic first-party API prices (as used in the agentic_lab tanej template, price table date 2026-09-25); 1-hour cache writes.

| Model | Input / MTok | Output / MTok | Cache read / MTok | Cache write / MTok |
|---|---|---|---|---|
| claude-opus-5-5 | 4.00 | 20.00 | 0.20 | 8.00 |

## From the harness usage panel

| Item | Value |
|---|---|
| API time | not shown: subscription plan; the usage panel shows only plan-limit percentages (transcript estimate: 26 to 51 minutes of model time) |
| Approval prompts | not counted (ask-first permission mode). Decision answers by the human: 4 (D-01, D-02, D-05, D-18), all through the agent's question form |
| Cost shown | not shown: subscription plan, no per-session cost in the usage panel |
| Difference to `usage.costUsd` | n/a (no panel cost; `usage.costUsd` is the API-price equivalent from the transcript) |
