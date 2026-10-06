# Conference registration runs for the paper: instructions

Each of you runs the same case, conference registration (US-001), three times, starting from the same base commit: `main` of this repository (`SQA-interns/testing-V`), which holds the template of `kyuhi/single-agent/sdd/v002` from `SQA-interns/agentic_lab`. The only thing that changes between your three runs is the requirement input:

| Condition | Requirement input | In the paper |
|---|---|---|
| C0 | the story as a typical backlog item | S0 |
| C1 | the story improved to good user-story quality | S1 |
| C2 | the agent-ready requirement file `REQ-REG-01.md`, which the paper proposes; the three requirement files only point to it | REQ-REG-01 |

Everything else stays fixed: the template on `main` (`01_input/00_general`, `AGENTS.md`, its `tech-stack.md`), the project-level files in this package, Opus 5.5 with medium effort, and the same base commit.

## Two branches

| Branch | Content | Where it goes |
|---|---|---|
| `main` | the template only: the exact tree of `agentic_lab` commit `30047fc`, where the v002 run started, with its empty output skeletons. This is the **base commit** for every run. | your run folder; the agent works here |
| `package` (this branch) | these instructions, `C0/`, `C1/`, `C2/` and `oracle/` | a separate folder outside the run folder; the agent never sees it |

Never merge `package` into a run branch and never copy `oracle/` into the run folder.

## Folder contents

```
C0/01_input/   complete project input for condition C0
C1/01_input/   complete project input for condition C1
C2/01_input/   complete project input for condition C2 (includes REQ-REG-01.md)
oracle/        the hidden test suite (never copy it into the run folder)
```

The folders mirror the repository: `C0/01_input/01_project/00_setup`, `01_requirements` and `02_design`. Copying a condition's `01_input` over the repository's `01_input` replaces the project files and nothing else.

- **Same in C0, C1 and C2:** `00_setup/environments.md`, `00_setup/secrets.env.example`, and all of `02_design/`.
- **Different per condition:** only the files in `01_requirements/`.
- **Not included, so they stay as they are on `main`:** `00_setup/tech-stack.md` and `00_setup/run-config.md`, as well as everything in `01_input/00_general/`, `AGENTS.md` and `README.md`.

## 1. Prepare once

1. Get this package into its own folder, outside any run folder:

   ```
   git clone -b package https://github.com/SQA-interns/testing-V.git confreg-package
   ```

2. Note the base commit: the SHA of `main` (`git ls-remote https://github.com/SQA-interns/testing-V main`).

## 2. Each run

Run the conditions in this order (it balances any learning effect between people):

| Person | Order |
|---|---|
| kyuhi | C1, C2, C0 |
| tanej | C2, C0, C1 |
| tjan | C0, C1, C2 |

For each run:

1. Start from the base commit in a fresh clone of `main` only, on a new branch named `<name>/confreg-<condition>`. `--single-branch` keeps the `package` branch (and the oracle) out of the run folder's git data, where the agent could otherwise read it:

   ```
   git clone --single-branch -b main https://github.com/SQA-interns/testing-V.git <name>-confreg-<condition>
   cd <name>-confreg-<condition>
   git switch -c <name>/confreg-<condition>
   ```

   Never run `git fetch --all` or fetch `package` in a run folder.

2. Keep `.env` out of git before you create it: `main` has no `.gitignore` (the agent writes one in phase 0).

   ```
   echo .env >> .git/info/exclude
   ```

3. Copy `<condition>/01_input` from the package folder over the repository's `01_input` (merge, overwrite existing files).
   - PowerShell: `Copy-Item -Recurse -Force ..\confreg-package\<condition>\01_input\* .\01_input\`
   - Git Bash: `cp -r ../confreg-package/<condition>/01_input/. ./01_input/`
4. Check that `01_input/01_project/01_requirements/` contains only the files of this condition. In C0 and C1 there must be no `REQ-REG-01.md`.
5. In `01_input/01_project/00_setup/run-config.md`, set the Run ID to `<name>-confreg-<condition>-r1`. Keep model `claude-opus-5-5`, effort `medium` and template version `1.2`.
6. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the repository root and fill it (database password, organizer user and password, NVD key).
7. Commit ("inputs <condition>") and push the branch. Write down the SHA. `git status` must not list `.env`.
8. Start a **new agent session** in the run folder and run the full workflow as usual.

The previous project's dependencies stay in `tech-stack.md` (e.g. the Excel library). If the agent records a decision about unused entries, that is fine; it happens the same way in all conditions.

**Rules during the run**

- Answer tooling and environment questions as usual, using the standard answers below so that everyone answers the same way.
- Type at least one line of each answer yourself; the agent asks you to confirm an answer that is only pasted text.
- To any question about **business behaviour** (what the system should do), answer only: *"No further information is available. Choose an option and record it as a decision."* Do not add anything.
- Log every intervention, as the template already requires.
- Do not change the input files during the run.
- Keep the package folder outside the run folder and never mention the oracle to the agent.

## Standard answers to expected tooling questions

These questions came up in the first run (tanej, C2) and come from the fixed template and stack, so they will come up in every condition. Decision numbers (D-nn) may differ in your run; recognise them by their subject.

| Phase | The agent asks about | Answer |
|---|---|---|
| 0 | The `.env` presence check (or a later command that reads `.env`) was refused by the permission classifier | Switch the chat's permission mode from Auto to the default ask-first mode, approve the command when prompted, and answer "allow the command". Switch back to Auto after phase 0. Simpler: start phase 0 in the ask-first mode. |
| 0 | Host JDK or Node/npm differs from the pins (Temurin 21.0.10+7, Node 24.13.0, npm 11.6.2) | Approve the installed versions; if one causes a real failure later, the agent raises a new decision. |
| 0 | `vitest` 3.2.7 has Critical vulnerabilities (via `tinypool`) | Move `vitest` and `@vitest/coverage-v8` to 5.0.3. |
| 0 | `jscpd` 4.3.0 has High vulnerabilities | Move `jscpd` to 5.4.0. |
| 0 | CVE-2025-7962 (High) on `angus-activation` | Treat it as a false positive (Low): the CVE is in the mail library before 2.0.4, and the shipped `angus-mail` is 2.0.5. Suppress it for that one artifact only. |
| 6 | Semgrep "use of basic authentication" (maps to High) on the organizer endpoint | Lower it to Low and accept it: HTTP Basic is required by `architecture.md` and `security-requirements.md`, credentials only over HTTPS or localhost, password only as a BCrypt hash, failed logins rate limited. |

Decisions the agent takes on its own (for example, the OSS Index analyser disabled for lack of credentials, or a scanner false positive rated Medium) need no answer.

## 3. After each run

1. Run the post-run session: "Fill section 2 of `03_statistics/metrics.md`." Then fill `usage.md`, including the cost shown by the tool.
2. Run the hidden test suite against the running local stack. Each phase needs the backend started with different settings; `docker-compose.yml` passes these variables through to the backend.
   - Start the stack the way the run's `02_output/README.md` says (for example with `--env-file ../.env`), adding `--force-recreate`.
   - Write the results outside the repository: `oracle.py` writes `oracle-<phase>.json` into the current folder unless you give `--out`.
   - If the backend or Mailpit is on other ports, set `ORACLE_BASE_URL` and `ORACLE_MAILPIT_URL`. If the agent's compose file does not pass a variable through, add it to the backend's `environment:` section for the test only, and note that you did.

   **Git Bash, Linux or macOS**

   ```bash
   cd 02_output
   export ORGANIZER_USERNAME=... ORGANIZER_PASSWORD=...      # from .env
   export ORACLE_LOG_CMD="docker compose logs backend --no-color"
   O=/path/to/confreg-package/oracle
   R=/path/to/results/<name>-confreg-<condition>-r1          # outside the repository
   mkdir -p "$R"

   APP_TEST_CLOCK=enabled APP_RATE_LIMIT_PER_HOUR=1000 docker compose --env-file ../.env up -d --force-recreate
   python3 $O/oracle.py main --out "$R/oracle-main.json"
   APP_TEST_CLOCK=enabled APP_RATE_LIMIT_PER_HOUR=1000 APP_EARLY_BIRD_DEADLINE=2026-08-15 docker compose --env-file ../.env up -d --force-recreate
   python3 $O/oracle.py deadline --out "$R/oracle-deadline.json"
   APP_TEST_CLOCK=enabled APP_RATE_LIMIT_PER_HOUR=3 docker compose --env-file ../.env up -d --force-recreate
   python3 $O/oracle.py ratelimit --out "$R/oracle-ratelimit.json"
   python3 $O/summarize.py "$R"
   ```

   On Windows Git Bash use `py` instead of `python3` if `python3` is not found.

   **Windows PowerShell**

   ```powershell
   cd 02_output
   $env:ORGANIZER_USERNAME = "..."; $env:ORGANIZER_PASSWORD = "..."   # from .env
   $env:ORACLE_LOG_CMD = "docker compose logs backend --no-color"
   $O = "C:\path\to\confreg-package\oracle"
   $R = "C:\path\to\results\<name>-confreg-<condition>-r1"           # outside the repository
   New-Item -ItemType Directory -Force $R | Out-Null

   $env:APP_TEST_CLOCK = "enabled"; $env:APP_RATE_LIMIT_PER_HOUR = "1000"
   docker compose --env-file ..\.env up -d --force-recreate
   py "$O\oracle.py" main --out "$R\oracle-main.json"

   $env:APP_EARLY_BIRD_DEADLINE = "2026-08-15"
   docker compose --env-file ..\.env up -d --force-recreate
   py "$O\oracle.py" deadline --out "$R\oracle-deadline.json"

   Remove-Item Env:APP_EARLY_BIRD_DEADLINE
   $env:APP_RATE_LIMIT_PER_HOUR = "3"
   docker compose --env-file ..\.env up -d --force-recreate
   py "$O\oracle.py" ratelimit --out "$R\oracle-ratelimit.json"

   py "$O\summarize.py" $R
   ```

   In PowerShell a variable set with `$env:` stays set for the whole window; that is why `APP_EARLY_BIRD_DEADLINE` is removed before the rate-limit phase. Open a new window for the next run.

3. Leave the oracle results (`oracle-*.json`) outside the repository, next to your other results.

## 4. What to send back (per run)

| What | Why |
|---|---|
| The whole repository as a zip, **including `.git`** | code, diff from the base commit, commit history |
| `03_statistics/run-log.json` with the usage section filled | duration, phases, tokens, tests, findings, decisions, interventions |
| `03_statistics/usage.md` | cost and API time from the tool |
| `02_output/docs/decisions-log.md`, `01_acceptance-criteria.md`, `06_verification-report.md` | how each requirement question was decided |
| The session transcript (`.jsonl`) | questions the agent asked, token cross-check |
| `oracle-main.json`, `oracle-deadline.json`, `oracle-ratelimit.json`, `oracle-summary.json` | scenarios SC1–SC12 |
| Base commit SHA and the input commit SHA | to confirm that only the requirement files differ |
| A note on anything unusual (repeated run, usage limit, manual compose change) | threats to validity |

## What the test suite checks

| ID | Situation | Expected |
|---|---|---|
| SC1 | Submitted 31 July 2026, 23:30 local | net fee 240.00 |
| SC2 | Submitted 1 August 2026, 00:10 local (31 July 22:10 UTC) | net fee 300.00 |
| SC3 | Confirmation of an early-bird registration | one e-mail stating 240.00, 52.80, 292.80 |
| SC4 | Company payer from Austria | VAT 52.80, gross 292.80 (no reverse charge) |
| SC5 | Valid company registration | payer data stored; no invoice or payment fields; at most one e-mail |
| SC6 | Private person without company data | accepted |
| SC7 | Same e-mail on 20 July and 5 August | both kept, fees 240.00 and 300.00 |
| SC8 | One, none or two workshops; 60 registrations for one workshop | one and none accepted, two rejected, no capacity limit |
| SC9 | Backend log during a registration | no name, e-mail, address or VAT ID |
| SC10 | Deadline configured as 15 August; submitted 10 August | net fee 240.00 |
| SC11 | Request with a student flag | no discount |
| SC12 | Request without authentication; rate limit 3 | accepted; the 4th request is refused with 429 |

"harness" means the test clock was not implemented, so a time-dependent scenario could not be checked. Report it as it is; do not fix the code.
