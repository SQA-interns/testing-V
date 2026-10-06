# Project template

> Owner: Team lead · Agent: read-only

Template version: **1.2**. Increase it whenever a file in section 2 changes; each run records it in `run-config.md`.

Humans start here; agents start at `AGENTS.md`.

## Folders

| Folder | Content | Phase |
|---|---|---|
| `01_input/00_general/` | Rules, phases, skills, quality and security baselines for every project | all |
| `01_input/01_project/00_setup/` | Run configuration, secrets list, tech stack, environments | 0 |
| `01_input/01_project/01_requirements/` | Stories, business rules, scope, open questions | 1 |
| `01_input/01_project/02_design/` | Architecture, security requirements, quality requirements | 2 |
| `02_output/` | Everything the agent produces (docs, code, logs) | 0-7 |
| `03_statistics/` | Experiment measurement (not part of the project) | all |

Project folder numbers match the phase that first uses them; later phases (3-7) use inputs from these folders and the general rules.

## 1. Fill in (human, for each project or run)

| File | What goes in it |
|---|---|
| `01_input/01_project/00_setup/run-config.md` | model, effort, run id (every run) |
| `01_input/01_project/00_setup/secrets.env.example` | every secret the project needs (names only); copy to `.env` and fill the values there |
| `01_input/01_project/00_setup/tech-stack.md` | every platform, dependency and tool with exact versions |
| `01_input/01_project/00_setup/environments.md` | environments, settings, external services |
| `01_input/01_project/01_requirements/user-stories.md` | user stories |
| `01_input/01_project/01_requirements/business-rules.md` | business rules, data fields, glossary |
| `01_input/01_project/01_requirements/scope.md` | in/out of scope, priorities, open questions |
| `01_input/01_project/02_design/architecture.md` | components, constraints, interfaces |
| `01_input/01_project/02_design/security-requirements.md` | security level, authentication, personal data, project requirements |
| `01_input/01_project/02_design/quality-requirements.md` | non-functional requirements, thresholds, extra done criteria |
| `03_statistics/usage.md` | prices before the post-run session; usage-panel values after the run |

Order: fill `01_requirements/` and `02_design/`, then `00_setup/`, then start the agent. It checks everything else in phase 0 and asks for whatever is missing.

## 2. Do not touch (change only when revising the template itself)

- `AGENTS.md`, `README.md`
- everything in `01_input/00_general/`:
  - `working-rules.md`, `phases.md`, `engineering-standards.md`
  - `quality/definition-of-done.md`, `quality/severity-scale.md`, `quality/test-strategy.md`
  - `security/security-baseline.md`
  - `skills/preflight/SKILL.md`, `skills/write-acceptance-tests/SKILL.md`, `skills/verify-release/SKILL.md`
- `03_statistics/metrics.md`, `03_statistics/run-log.template.json`

## 3. Written by the agent (read, do not edit)

- `02_output/README.md` and one README per component
- `02_output/docs/`: `00_preflight-report.md`, `00_input-manifest.sha256` (frozen once written), `00_progress.md`, `01_acceptance-criteria.md`, `02_specification.md`, `02_contracts/`, `03_acceptance-manifest.sha256`, `03_test-strategy.md`, `06_verification-report.md`, `decisions-log.md`, `release-notes.md`
- `02_output/logs/`, all source code and tests in `02_output/`
- repository files required by ES-03 at the repository root: `.gitattributes`, `.gitignore`
- `03_statistics/run-log.json`, `03_statistics/run-summary.md`

Any change to sections 1 and 2 during a run is detected in phase 6 by comparing `00_input-manifest.sha256`.

## Rules for editing this template

- Start every Markdown file with `> Owner: … · Read in: … · Agent: …` (after the front matter in a `SKILL.md`; agent-written skeletons use `> Written in: … · Source: … · Agent: writes`) and add it to the right list above.
- State each rule once; link to it elsewhere. Write constraints ("Do not …"); procedures go in skills.
- One topic per file, no fixed length: split a file when it covers two topics or a reader needs only half of it. Versions appear only in `tech-stack.md`.
