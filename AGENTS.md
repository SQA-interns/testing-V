# AGENTS.md

> Owner: Team lead · Read in: every phase · Agent: read-only

Humans start at `README.md`.

## Read first, in this order

1. `01_input/01_project/00_setup/run-config.md`: model, effort, run id
2. `01_input/00_general/phases.md`: phase order, inputs, outputs, gates, test levels, commit units
3. `01_input/00_general/working-rules.md`

If `02_output/docs/00_progress.md` shows work in progress, resume from it instead of starting over.

## Paths and precedence

- Aliases used in all files: `general/` = `01_input/00_general/`, `project/` = `01_input/01_project/`, `out/` = `02_output/`, `docs/` = `02_output/docs/`.
- Precedence: `AGENTS.md` > `general/working-rules.md` > `general/phases.md` > other `general/` files > `project/` files, except for explicit overrides.
- A project file adds to the general file on the same topic and never repeats it; it overrides a general item only with a line `Overrides: <ID>, reason`.
- A human approval recorded in a decision record (`Human response`) replaces the input entry it changes, for that item only. Never edit `01_input/` to apply it.
- A conflict that no rule resolves is a decision record, never a silent choice.

## Never

- Do not create, modify or delete anything under `01_input/`, or any file in sections 1 and 2 of `README.md`.
- Write only the files in section 3 of `README.md`.
- Do not edit a file listed in `02_output/docs/03_acceptance-manifest.sha256`; raise a decision record instead.
- Do not change `02_output/docs/00_input-manifest.sha256` after phase 0.
- Do not change a technology, version, service or secret named in `01_project/00_setup/tech-stack.md` or `01_project/00_setup/secrets.env.example` without human approval.
- Do not install or upgrade software on the host without human approval.
- Do not commit secrets or environment-specific values.
- Do not read `.env` with a file tool, and do not print, copy or log its values (see "Secrets" in `working-rules.md`).
- Do not ask the human to paste a secret into the conversation; ask them to put it in `.env`.
- Do not start a phase before the previous phase's gate has passed.
- Do not finish while a Critical or High finding is open.
- Do not touch `03_statistics/` except as stated in `working-rules.md`.
