# Implementation Plan: OGC-782 M4 WHONET Manual Export

## Technical Context

M4 stacks on M3 commit `5c3937e6727ed7902cb354a6c7748caa69d94f84`.
The repo already contains the Reports-to-WHONET path,
`WHONetReportService`, `WHONETCSVRoutineColumnBuilder`, finalized microbiology
cases, service-created UAT fixtures, organism and antibiotic WHONET codes, and
case-level readiness. The new slice extends that path; it does not introduce a
second exporter.

## Decisions

1. `WHONetReportService` remains the export boundary. A microbiology dataset
   compiler supplies finalized case, isolate, and AST rows to that service.
2. The existing fifteen-column CSV contract remains the first downloadable
   format. A verified future wide-column WHONET contract can replace or version
   it without changing this page workflow.
3. Export selection uses final close time in a half-open date interval and the
   bacteriology workflow only.
4. The local seven-day policy sorts by final close time and keeps the first
   isolate for each patient and organism within each rolling seven-day window.
   It is explicitly not labeled WHO/CLSI-certified.
5. Missing organism or antibiotic codes are warnings and exclude affected rows.
   Zero remaining rows is a generation blocker.
6. A new export-run entity stores generation metadata and a SHA-256 fingerprint,
   not the PHI-bearing file body. Exact re-download is therefore deferred.
7. The write controller derives the actor from the authenticated request. The
   request contract has no actor field.
8. The frontend route is `/Microbiology/whonet` with `from`, `to`,
   `significance`, `dedup`, `step`, `page`, and `pageSize` query state.
9. Mapping repair uses the M3 admin `edit` query state to open the exact organism
   or antibiotic record. Browser history returns to the preview.
10. The config-backed Microbiology menu gains a WHONET Export child; no database
    migration is used for navigation.

## Data Flow

1. The page canonicalizes query state and requests preview.
2. The report service compiles finalized cases and groups isolates, reportable
   reviewed runs, readings, organism codes, antibiotic codes, and specimen and
   patient context inside a read transaction.
3. The compiler applies significance and de-duplication, then emits eligible
   rows and exact mapping warnings.
4. Generate recompiles the same request, rejects a blocked dataset, renders the
   existing CSV contract, hashes the bytes, records the authenticated run, and
   returns the attachment.

## Database Change

One Liquibase changeset adds the export-run audit entity and indexes generation
time. Rollback removes that table. Routes, menu config, fixtures, tests, and UI
do not receive migrations.

## Test Strategy

- JUnit 4/Mockito: date boundaries, final-case selection, significance,
  seven-day de-duplication, mapping warnings, multiple AST readings, CSV
  escaping, blocked generation, and authenticated actor.
- Controller: query binding, attachment headers, authorization, and actor
  spoof resistance through absence of client actor input.
- ORM/Liquibase: entity registration plus update and rollback.
- Vitest/RTL: canonical query state, Carbon interactions by role/label,
  preview counts, exact mapping links, disabled/enabled generation, and download.
- Playwright `core-app`: authenticated navigation, seeded final case, preview,
  mapping repair path, generation, and downloaded CSV assertions. No arbitrary
  waits.
- Visual evidence: desktop and mobile screenshots compared with M-09 Configure
  and Preview states; deviations recorded as intentional.

## Explicit Non-Goals

Scheduled delivery, SFTP/email, TXT, profile packaging, exact re-download, TB,
phenotypes, expert rules, national aggregation, GLASS submission, and new
specimen/origin/patient/department mapping masters.

