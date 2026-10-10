# Milestone 2: Case visibility and access

The Microbiology landing page lists cases, including cases opened before
collection. Opening a row shows a read-only patient header, current case lab
unit, collected samples, pending specimens and related cases. Results users
can transfer mutable cases to an eligible unit in the same domain when they
hold Results rights in both units.

Ownership uses the existing `MicroCase.labUnitId`. There is no separate
`micro_case_lab_unit` table. Transfer changes this reference and records the
actor, time and source/destination units in `micro_case_activity`. It preserves
the case identifier, Program, requested work, member samples and split links.
It never merges cases. Cancelled, rejected and final cases remain locked,
subject to the existing amendment policy.

Search and direct fetch both require Results or Validation rights in the
current case lab unit. Validation alone grants reading and validation actions,
not ordinary Results writes or transfer. Global administrators and explicit
all-unit assignments use the existing role model. Unit status does not revoke
access to existing work. Related links use shared active sample membership or
a recorded split; sharing an order alone is insufficient. Every returned
related case is checked independently.

## HTTP contract

All endpoints require an authenticated system user and an existing Results,
Validation or administrator role. Controllers return compiled forms and never
traverse entity relationships. The service compiles all response data within
its transaction. Physical specimens load their sample and specimen type with
fetch joins.

| Endpoint | Result |
| --- | --- |
| `GET /rest/microbiology/cases/search` | Paged case summaries and permitted lab-unit filter options |
| `GET /rest/microbiology/cases/by-accession?accessionNumber=…` | Paged summaries for that exact accession; an order can contain several cases |
| `GET /rest/microbiology/cases/{caseId}/shell` | Header, samples, pending specimens, related cases and action permissions |
| `POST /rest/microbiology/cases/{caseId}/transfer` | Updated shell; body contains `labUnitId` |

Search accepts `status` (`ACTIVE`, `CANCELLED`, `REJECTED`), `from` and `to`
(inclusive opened dates in `YYYY-MM-DD`), `q` (accession or patient name),
`patientId`, exact `accessionNumber`, `labUnitId`, `sort` (`newest` or `accession`),
`page` (one-based, default 1), and `pageSize` (1–100, default 20). The database
query applies permitted units before counting and pagination. Unassigned
historical cases remain outside this list until clinical migration assigns a
unit.

A page contains `rows`, `total`, `page`, `pageSize` and `labUnits` (`id`, `value`).
Each summary contains `id`, `accessionNumber`, patient identifier/name/date of
birth/sex, `labUnitId`, `labUnit`, `specimenType`, `status`, `stage`, `priority`
and `createdAt`. The shell adds `samples`, `pendingSamples`, `relatedCases`,
`canWrite`, `canValidate` and `transferLabUnits`. Pending specimens have no
invented collection date or physical sample identifier.

Invalid filters return 400, denied access returns 403, unknown case identifiers
return 404, and a locked transfer returns 409. The client treats HTTP error
status numbers separately from a successful case's string `status`.

## Navigation and retained capabilities

`/Microbiology/worklist` is the case list. Its filter query survives case
navigation and return. `/Microbiology/cases/{id}` is the read-only shell.
Explicit `grain=cultures` or `grain=ast` worklist links retain the existing
culture and susceptibility views. Existing case section links and
`view=workbench` retain the detailed workbench. Their server reads and writes
are checked against current case ownership, including child-resource routes.
No retired workflow or protocol controls are restored.

## Validation

Focused backend checks cover persisted unit assignments, count/page scoping,
HTTP binding and denied direct access, pending requests, related membership,
transfer rights, transfer audit and final locks, plus affected existing services.
Frontend checks cover navigation/filter retention, pending samples, transfer
visibility, failures and stale transfer responses after navigation. The browser
spec `microbiology-case-access.spec.ts` provisions its own data through the
property-gated scenario service and checks the worklist-to-case journey at
both desktop and narrow widths.

These checks are implementation evidence. Full local CI, GitHub checkpoints
and owner acceptance are separate gates; consult the PR for their current state.

The focused run on 2026-10-09 passed 102 backend tests (including 12
PostgreSQL-backed case workspace tests), 66 frontend tests, the production
frontend build, repository formatting and translation plural checks. The
source-stack browser journey and fresh administrator authentication passed;
desktop worklist/shell and 390-pixel shell screenshots were inspected. The
shell check asserts that the document does not overflow the viewport.

The browser journey was run with `--no-deps` after the administrator setup,
because the unrelated participant setup still targets the old fixed database
container name. This is focused browser evidence, not full suite evidence.
