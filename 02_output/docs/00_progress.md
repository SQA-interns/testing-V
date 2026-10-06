# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: spec-driven, acceptance tests first (`general/phases.md`); run `tanej-confreg-C2-r1`
- Current phase: 0, preflight & bootstrap
- Last gate result: phase 0 gate not yet passed. Preflight done once (`docs/00_preflight-report.md`); input manifest written; `.gitattributes`/`.gitignore` committed.
- Next step: on the human's answers, re-run only the failed checks (JDK, Node/npm, secrets, frontend audit with the approved versions, backend OWASP scan), then bootstrap the backend and frontend skeletons and check the phase 0 gate.
- Waiting for the human on: D-01, D-02, D-03, D-04, D-06
