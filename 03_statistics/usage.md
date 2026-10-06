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
| API time | not shown: subscription plan; the usage panel shows only plan-limit percentages (transcript estimate: 31 to 56 minutes of model time) |
| Approval prompts | not counted (Auto permission mode). Decision answers by the human: 9 (D-01, D-02, D-04, D-07, D-08, D-15, D-17, D-32, D-34) in 5 messages, plus 1 standard reply to the business-decision review (D-30) and 1 resume after a network drop |
| Cost shown | not shown: subscription plan, no per-session cost in the usage panel |
| Difference to `usage.costUsd` | n/a (no panel cost; `usage.costUsd` is the API-price equivalent from the transcript) |
