# Working rules

> Owner: Team lead · Read in: every phase · Agent: read-only

## Ask the human (blocking)

Record a decision in `docs/decisions-log.md` (format under "Decision record"), ask, and wait when:

- anything checked in phase 0 is missing or wrong and only a human can fix it;
- a technology, version or service from `project/00_setup/tech-stack.md` does not work or needs changing;
- a frozen acceptance test appears wrong (only a human may update the manifest afterwards);
- a Critical or High finding would be downgraded or accepted.

Ask all open questions of a phase in one message. While waiting, finish work that does not depend on the answer.

## Decide and record (non-blocking)

When a requirement allows two behaviours a user would notice, choose the more conservative one, record it as "pending review", and continue.

Also non-blocking, record and continue: a pinned tool that runs but reports another version; a scanner analyser disabled for lack of credentials; dev-only tooling added under the `tech-stack.md` rule.

## Decision record

Append each decision to `docs/decisions-log.md` in this format:

```
## D-nn: <short title>
- Timestamp:
- Phase:
- Type: blocking | non-blocking
- Trigger: <what happened; cite IDs and files>
- Options: <numbered alternatives; mark the proposed default>
- Human response: <answer and time, or "none">
- Resolution: <chosen option> | pending review
```

- Never delete or rewrite a record; add a follow-up record instead.
- The phase 7 gate fails while any record lacks a resolution.

## Identifiers

| Prefix | Meaning | Defined in |
|---|---|---|
| `US-nnn` / `AC-nnn-nn` | User story / acceptance criterion | `project/01_requirements/user-stories.md` / `docs/01_acceptance-criteria.md` |
| `BR-nn` / `OQ-nn` | Business rule / open question | `project/01_requirements/` |
| `AR-nn` | Architecture constraint | `project/02_design/architecture.md` |
| `SB-nn` / `SR-nn` | Security baseline / project security requirement | `general/security/` / `project/02_design/security-requirements.md` |
| `ES-nn` | Engineering standard | `general/engineering-standards.md` |
| `NFR-nn` | Non-functional requirement | `project/02_design/quality-requirements.md` |
| `DoD-nn` / `DoD-Pnn` | Done criterion, general / project | `general/quality/` / `project/02_design/quality-requirements.md` |
| `F-nn` / `D-nn` | Finding / decision | `docs/06_verification-report.md` / `docs/decisions-log.md` |

Tests, commits and findings reference these IDs.

## Commits

Commit as a human developer would: small, finished steps, never half a project.

- One commit = one logical change that can be reverted alone. Commit when it is done and its checks pass; do not batch.
- At most about 15 files or 400 changed lines, not counting logs, lock files and generated files; otherwise split, or state in the message why it cannot be. Never a whole phase or component.
- Keep code, tests, documents, dependency or build changes, and each fix (`F-nn`) in separate commits.
- The build passes at every commit and no previously passing test fails. Frozen acceptance and end-to-end tests of behaviour not yet built may fail.
- Message: `<type>: <summary> (IDs)`; type `feat`, `test`, `fix`, `refactor`, `docs`, `build` or `chore`; imperative, at most 72 characters.
- Units per phase: `general/phases.md`. Working tree clean at every gate.
- Do not squash, rebase or rewrite history.

## Progress

Update `docs/00_progress.md` at every gate and before any stop, so a fresh session can resume.

## Output

- Write command output longer than 50 lines to `out/logs/`; report only a summary and the path.
- Use only the severity levels in `general/quality/severity-scale.md`.

## Secrets (`.env`)

You never need to see a secret value; the tools that run need it, not you.

- Do not read `.env` with a file tool, and do not print, copy or log its values: no `cat`, `echo`, `env`, `printenv` or `docker compose config` on it, in commands, files, logs, commit messages or the conversation.
- Pass values to tools without showing them: `docker compose --env-file .env`, or `export NAME="$(sed -n 's/^NAME=//p' .env | tr -d '\r')"` inside the command that needs it.
- Check presence and emptiness only, as in `general/skills/preflight`.
- Before saving output from a command that may print an environment, filter it so no value is written.
- The phase 6 secret-leak check (`general/skills/verify-release`) must find nothing.

## Statistics

At the start and end of each phase, and for every fix loop, human intervention and decision, update `03_statistics/run-log.json` as defined in `03_statistics/metrics.md`, and write `03_statistics/run-summary.md` in phase 7. Do nothing else in `03_statistics/`.
