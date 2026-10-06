# Security baseline

> Owner: Security officer · Read in: phases 0, 2, 6 · Agent: read-only

Applies to every project. Project requirements (`SR-nn`) and the chosen standard level are in `project/02_design/security-requirements.md`.

## Verification standard by component type

| Component type | Standard | Default level |
|---|---|---|
| Web app, API, service | OWASP ASVS (latest) | Level 1; Level 2 when handling authentication, payments or sensitive personal data |
| Mobile app | OWASP MASVS (latest) | L1 |
| Desktop app, CLI, library | Applicable OWASP ASVS chapters | Level 1 |

## Controls for every project

| ID | Control |
|---|---|
| SB-01 | All input is validated on the trusted side (server or core), whatever the client does. |
| SB-02 | Every non-public operation checks authentication and authorization. |
| SB-03 | Credentials are stored only as salted, slow hashes; secrets come from configuration (ES-01, ES-02). |
| SB-04 | Data in transit uses TLS outside the local machine. |
| SB-05 | Output is encoded for its context; queries are parameterised. |
| SB-06 | Public and authentication endpoints are rate limited. |
| SB-07 | Errors and logs expose no internals, secrets or personal data (ES-07). |
| SB-08 | Dependencies are scanned; no known Critical or High vulnerability ships. |
| SB-09 | Source is scanned with static analysis and a secret scanner. |
| SB-10 | Web responses carry security headers (content security policy, framing, content-type options). |
| SB-11 | Deployed processes run with least privilege (e.g. non-root containers). |

## Personal data (privacy)

| ID | Control |
|---|---|
| SB-12 | Collect only the personal data a requirement needs; the project lists it. |
| SB-13 | Each personal-data item has a stated purpose and retention period. |
| SB-14 | Consent, where required, is explicit and never preselected. |
