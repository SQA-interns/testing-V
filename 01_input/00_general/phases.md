# Phases (spec-driven, acceptance tests first)

> Owner: Team lead · Read in: every phase · Agent: read-only

Run the phases in order. A phase starts only after the previous gate has passed.
Adds to `general/working-rules.md`; does not replace it. Path aliases: see `AGENTS.md`.

| # | Phase | Reads | Writes | Gate (must be true to continue) |
|---|---|---|---|---|
| 0 | Preflight & bootstrap | `project/00_setup/*`, `general/engineering-standards.md`, `general/security/security-baseline.md`, `general/quality/severity-scale.md`, skill `general/skills/preflight` | `docs/00_preflight-report.md`, `docs/00_input-manifest.sha256`, `docs/00_progress.md`, one skeleton per component, repo files (ES-03) | Every preflight check passes; every component builds; manifests and lock files match `tech-stack.md` exactly (as amended by approved decision records); every listed tool runs; input manifest written |
| 1 | Requirements | `project/01_requirements/*` | `docs/01_acceptance-criteria.md` | Every story has ≥ 1 AC; every AC traces to a story; every open question is answered or has a decision record |
| 2 | Design | `docs/01_*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` | `docs/02_specification.md`, `docs/02_contracts/` | Every AC, SR, SB and NFR maps to the spec; contracts validate with a parser |
| 3 | Test design | `docs/01_*`, `docs/02_contracts/`, `general/quality/test-strategy.md`, skill `general/skills/write-acceptance-tests` | acceptance tests, `docs/03_test-strategy.md`, `docs/03_acceptance-manifest.sha256` | Every AC has a test; every test fails for a behavioural reason (no build or setup errors), or passes only because bootstrap code already provides the behaviour, listed in `docs/03_test-strategy.md`; manifest committed last, after all tests, as its own freeze commit |
| 4 | Build | `docs/02_*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/architecture.md`, `general/engineering-standards.md` | source code | All frozen acceptance tests pass; format, lint and type checks are clean; at least one commit per user story |
| 5 | Unit tests | source code, `general/quality/test-strategy.md` | unit tests, `docs/03_test-strategy.md` | First full run recorded and classified before any fix; full suite passes |
| 6 | Verify | all of `01_input/`, all of `docs/`, skill `general/skills/verify-release` | `docs/06_verification-report.md` | Every general and project DoD item has evidence; no open Critical/High finding; manifest hashes match; no secret value found in any file, log or commit message |
| 7 | Release | `general/quality/definition-of-done.md`, `general/engineering-standards.md`, `project/02_design/architecture.md` | `out/README.md`, one README per component, `docs/release-notes.md` | Every README works when followed from a clean checkout; manual-test list present; every decision has a resolution or is listed as pending review |

Any phase may write `docs/decisions-log.md`, `docs/00_progress.md` and `out/logs/`.

## On a failed gate

- Fix within the same phase, then re-check the gate.
- If the gate needs a change to an input or a frozen test, stop and raise a decision record.

## Test levels

| Level | Written in phase | Written from | Frozen | Location |
|---|---|---|---|---|
| Acceptance | 3 | acceptance criteria + contracts | yes, from the freeze commit | path contains `acceptance` |
| End-to-end | 3 | acceptance criteria | yes, from the freeze commit | path contains `e2e` |
| Unit / integration | 5 | implementation | no | any other test path |

## Commit units

| Phase | One commit per |
|---|---|
| 0 | component skeleton; repository files (ES-03); preflight and manifest documents |
| 1, 2 | document (acceptance criteria; specification; each contract) |
| 3 | user story's acceptance and end-to-end tests, then the freeze commit (manifest only) |
| 4 | user story (or coherent group of AC of one story): code, plus only the wiring it needs |
| 5 | component area or layer under test (for example domain, API, UI screen) |
| 6 | fix of one finding, with its re-run evidence; the report in its own commit |
| 7 | README, release notes |

## Constraints

- Do not read or write production source code in phase 3.
- Do not write unit tests before phase 5.
- Do not edit, delete or weaken a test listed in `docs/03_acceptance-manifest.sha256`.

## Evidence (checked in phase 6)

- The phase 3 commits add no production code beyond the bootstrap skeleton; the freeze commit adds the manifest, and every file it lists was committed earlier in phase 3.
- Build has at least one commit per user story, each naming its `US`/`AC` ID; no commit exceeds the size guide in `working-rules.md` without a stated reason.
- Every hash in the manifest still matches.
