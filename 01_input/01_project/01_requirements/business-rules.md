# Business rules

> Owner: Product owner · Read in: phases 1, 6 · Agent: read-only

## Rules

| ID | Rule |
|---|---|
| BR-01 | There are two registration types: external participant and student. Each has the fixed fields listed under Data; configuration never changes them. |
| BR-02 | Required fields are never empty; leading and trailing whitespace is not significant. |
| BR-03 | Email has a valid email format. Text accepts Unicode, including Slovenian characters (č, š, ž). |
| BR-04 | Options are grouped as workshops, events, meals and other activities. Only active options can be selected; unknown or inactive options are rejected. |
| BR-05 | Mandatory consents must be given and are never preselected. |
| BR-06 | The in-application confirmation is shown only after the registration was accepted. |
| BR-07 | An accepted registration is stored in the database and as a raw JSON copy on persistent storage. |
| BR-08 | Only organizers can obtain the registration list; it is not reachable without organizer access. |

## Data

| Form or entity | Fields (all required) |
|---|---|
| External participant | first name, last name, email, organization / institution |
| Student | first name, last name, email, study institution, study programme, student ID |
| Conference option | stable identifier, display name, category (workshop, event, meal, other), active or inactive |
| Consent | which consents exist and their wording: `OQ-02` |

## Glossary

- Participant: a person who registers, external or student.
- Organizer: conference staff who receive notifications and export registrations.
- Option: a selectable workshop, event, meal or other activity.
- Active option: an option currently offered for selection.
- Raw JSON copy: the registration exactly as accepted, stored as a file for backup.
