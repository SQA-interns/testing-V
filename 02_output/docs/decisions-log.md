# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Docker Engine reports 29.8.1, pinned 29.8.0
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `docker` pins Docker Engine 29.8.0; `docker version` and `docker info` both report server 29.8.1 (client 29.8.1). The agent may not install or downgrade host software.
- Options: 1. (proposed default) Accept the installed 29.8.1 patch release as the `docker` platform for this run; nothing else changes. 2. The human installs Docker Engine 29.8.0 and the check is re-run.
- Human response: none
- Resolution: pending review

## D-02: cloc image tag 2.10 reports cloc 1.98
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: non-blocking
- Trigger: tooling `aldanial/cloc:2.10` pulls and runs, but `cloc --version` prints 1.98 (image built 2023-08-19). The preflight skill (step 2) and `tech-stack.md` rules make this non-blocking.
- Options: 1. (chosen) Keep the listed pin `aldanial/cloc:2.10` as authoritative and use it for code metrics. 2. Ask for a different image.
- Human response: none
- Resolution: option 1, pending review

## D-03: OSS Index analyser disabled in OWASP Dependency-Check
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: non-blocking
- Trigger: the Sonatype OSS Index analyser of `dependency-check-maven` needs credentials that `secrets.env.example` does not list (preflight step 6).
- Options: 1. (chosen) Run with `-DossindexAnalyzerEnabled=false`; the NVD, CISA KEV and other default analysers stay enabled. 2. Add OSS Index credentials (would change `secrets.env.example`, human only).
- Human response: none
- Resolution: option 1, pending review

## D-04: NVD API rejects NVD_API_KEY from .env
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: `dependency-check-maven` 12.1.0 fails with "Error updating the NVD Data" (log `out/logs/p0-backend-depcheck.log`). Second check: a direct request to the NVD CVE API 2.0 returns HTTP 404 with the key in the `apiKey` header and HTTP 200 without it, which is NVD's answer to an invalid key. The value is non-empty but is not in NVD's 36-character UUID key format (checked by length and pattern only; value never displayed). The backend dependency scan (preflight step 6) cannot complete.
- Options: 1. (proposed default) The human puts a valid NVD API key into `NVD_API_KEY` in `.env`; the agent re-runs the scan. 2. Run Dependency-Check without a key (allowed by NVD, but the first database download is heavily rate-limited and can take hours).
- Human response: none
- Resolution: pending review

## D-05: npm audit reports Critical and High in vitest 3.2.7 and jscpd 4.3.0
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` on the frontend dependency set (log `out/logs/p0-frontend-audit.json`) reports 2 Critical, 5 High and 4 Moderate. Critical: `vitest` 3.2.7 via `tinypool` <=2.1.0 (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr) and `@vitest/mocker` <4.1.11 (GHSA-82fw-gwwq-j7x9). High: `jscpd` 4.3.0 via `@jscpd/finder`, `fast-glob`, `micromatch` and `braces` <=3.0.3 (GHSA-vfj7-8cjw-p6xm). Moderate: `@vitest/coverage-v8` (via vitest), `qs` and `typed-rest-client`. All are dev and test tools; the runtime bundle only contains `react` and `react-dom`, which have no findings. Fixing them changes `tech-stack.md` pins, and accepting them would lower a Critical or High; both need the human.
- Options: 1. (proposed default) Replace `vitest` and `@vitest/coverage-v8` 3.2.7 with 4.1.11, and `jscpd` 4.3.0 with 5.4.0, all MIT. A scratch install with these versions gives 0 Critical, 0 High and 2 Moderate (`qs` via `typed-rest-client`); `vite` 6.4.3 meets the vitest 4.1.11 peer range, `@stryker-mutator/vitest-runner` 10.0.0 needs vitest >=2.0.0, and `tsc` and `vitest run` pass. 2. Same, but with vitest and coverage-v8 5.0.3 (a larger jump; needs node ^24, which is met). 3. Keep the pins and accept the findings as dev-only; this lowers a Critical or High, so it needs written human approval.
- Human response: none
- Resolution: pending review

## D-06: Playwright Chromium browser not installed on the host
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: tooling `@playwright/test` 1.63.0 runs ("Version 1.63.0"), but its bundled Chromium (chromium-1243) is not present in `%LOCALAPPDATA%\ms-playwright`. Downloading it is a host install, which needs human approval (`AGENTS.md`). End-to-end tests (phase 3) need it.
- Options: 1. (proposed default) Approve `npx playwright install chromium` from `02_output/frontend` (per-user download, no admin rights, no version change). 2. The human installs it. 3. Run Playwright in the `mcr.microsoft.com/playwright:v1.63.0` container (a new tool, recorded under the `tech-stack.md` rule).
- Human response: none
- Resolution: pending review

## D-07: ORGANIZER_USERNAME and SMTP_USERNAME values occur in repository paths
- Timestamp: 2026-10-06T17:39:03Z
- Phase: 0
- Type: blocking
- Trigger: the secret-leak check (value search, values never displayed) finds the 5-character values of `ORGANIZER_USERNAME` and `SMTP_USERNAME` in 27 places. Every match is part of the run id or the repository path (for example in `docs/00_preflight-report.md`, `03_statistics/run-log.json` and the build logs), not a copy of `.env`. The phase 6 check (`general/skills/verify-release`) must find no secret value, and an organizer login equal to a public name is also easy to guess (SB-02).
- Options: 1. (proposed default) The human sets `ORGANIZER_USERNAME` (and `SMTP_USERNAME`, if it is used) in `.env` to a value that does not occur in the repository or its paths. 2. Keep the values and accept these matches as false positives in phase 6.
- Human response: none
- Resolution: pending review

## D-08: Human response to D-01 (Docker Engine version)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-01; the human answered.
- Options: as in D-01.
- Human response: "go with already installed" (2026-10-06T17:46:32Z)
- Resolution: D-01 option 1. Docker Engine 29.8.1 (installed) replaces the 29.8.0 pin of platform `docker` for this run; `tech-stack.md` is not edited.

## D-09: Human response to D-04 (NVD API key)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-04; the human answered.
- Options: as in D-04.
- Human response: "NVD API key is updated" (2026-10-06T17:46:32Z)
- Resolution: D-04 option 1. The human updated `NVD_API_KEY` in `.env`; the agent re-runs the NVD check and the backend dependency scan.

## D-10: Human response to D-05 (vulnerable dev tooling)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-05; the human answered.
- Options: as in D-05.
- Human response: "Move vitest and @vitest/coverage-v8 to 5.0.3." (2026-10-06T17:46:32Z)
- Resolution: D-05 option 2. `vitest` and `@vitest/coverage-v8` 5.0.3 replace the 3.2.7 pins. Option 2 also includes `jscpd` 5.4.0 in place of 4.3.0, which the answer did not name. The agent applies it because it is the only way to clear the High findings, and it is pending review. `tech-stack.md` is not edited.

## D-11: Human response to D-06 (Playwright Chromium)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-06; the human answered.
- Options: as in D-06.
- Human response: "go ahead and run npx playwright install chromium" (2026-10-06T17:46:32Z)
- Resolution: D-06 option 1. The agent runs `npx playwright install chromium` in `02_output/frontend`.

## D-12: Human response to D-07 (username values)
- Timestamp: 2026-10-06T17:46:32Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-07; the human answered.
- Options: as in D-07.
- Human response: "I have updated the credentials to be more secure" (2026-10-06T17:46:32Z)
- Resolution: D-07 option 1. The human changed the credentials in `.env`; the agent re-runs the presence check and the secret-leak value search.

## D-13: ORGANIZER_USERNAME still occurs in repository paths after D-12
- Timestamp: 2026-10-06T17:48:42Z
- Phase: 0
- Type: blocking
- Trigger: re-check after D-12. `SMTP_USERNAME` and all passwords now match nothing. `ORGANIZER_USERNAME` is still 5 characters and still occurs in the repository path and in the run id `Run ID` of `project/00_setup/run-config.md`, so it is found in 8 tracked files (value search; value never displayed).
- Options: 1. (proposed default) The human sets `ORGANIZER_USERNAME` in `.env` to a value that does not occur in the repository or its paths. 2. Keep it and accept these matches as false positives in the phase 6 secret check (the human accepts that the organizer login is guessable).
- Human response: none
- Resolution: pending review

## D-14: Dependency-Check reports CVE-2025-7962 (CVSS v3 7.5) on angus-activation 2.0.3
- Timestamp: 2026-10-06T17:48:42Z
- Phase: 0
- Type: blocking
- Trigger: backend scan with the updated key (log `out/logs/p0-backend-depcheck.log`, report `out/logs/p0-backend-depcheck-report.json`, 67 dependencies) fails the CVSS 7 threshold. Finding: `org.eclipse.angus:angus-activation:2.0.3` matched to CPE `eclipse:angus_mail:2.0.3` for CVE-2025-7962 (SMTP injection with CR/LF). Scores: CVSS v3 7.5 (High under the severity scale), v4 6.0; the tool's severity is MEDIUM. Evidence that it is a false positive: NVD lists as affected only `eclipse:jakarta_mail` < 1.6.8 or 2.0.0 to < 2.0.2, and `eclipse:angus_mail` < 2.0.4. The resolved mail artifacts are `jakarta.mail:jakarta.mail-api` 2.1.5 and `org.eclipse.angus:angus-mail` 2.0.5, both outside these ranges. `angus-activation` is the Jakarta Activation implementation, not Angus Mail; it matched only by vendor name and an equal version number. Lowering a High needs the human (`general/quality/severity-scale.md`). SR-05 (header injection) will still be enforced and tested in the application.
- Options: 1. (proposed default) Classify as a false positive (Low) and add a Dependency-Check suppression for CVE-2025-7962 on `angus-activation` only, with this record as the reason; `angus-mail` stays scanned. 2. Keep it as High and ask for a different dependency set (would change `tech-stack.md`).
- Human response: none
- Resolution: pending review

## D-15: Dependency-Check reports CVE-2025-15104 (Medium) on hibernate-validator 9.1.3.Final
- Timestamp: 2026-10-06T17:48:42Z
- Phase: 0
- Type: non-blocking
- Trigger: `hibernate-validator` 9.1.3.Final (managed by Spring Boot 4.1.1) matched CPE `validator:validator` for CVE-2025-15104, a flaw in the Nu Html Checker (validator.nu). Scores: CVSS v4 6.9, v3 5.3, so Medium. The product is unrelated to Hibernate Validator and is not used. A Medium does not block (`general/quality/severity-scale.md`).
- Options: 1. (chosen) Classify as a false positive (Low), record it here, and suppress it on `hibernate-validator` together with D-14 if D-14 option 1 is approved; otherwise list it in the phase 6 findings. 2. Leave it as Medium.
- Human response: none
- Resolution: option 1, pending review
