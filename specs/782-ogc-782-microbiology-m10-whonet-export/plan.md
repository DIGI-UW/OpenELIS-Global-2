# Implementation Plan: OGC-782 M4 WHONET Manual Export

## Technical Context

M4 stacks on M3 commit `5c3937e6727ed7902cb354a6c7748caa69d94f84`.
The repo already contains the legacy Reports-to-WHONET path,
`WHONetReportService`, `WHONETCSVRoutineColumnBuilder`, finalized microbiology
cases, service-created UAT fixtures, organism and antibiotic WHONET codes, and
case-level readiness. The new slice reuses the existing renderer and service
boundary rather than introducing a second export implementation.

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
   Readings without a final S/I/R interpretation are also excluded. Zero
   remaining rows is a generation blocker.
6. A new export-run entity stores generation metadata and a SHA-256 fingerprint,
   not the PHI-bearing file body. Exact re-download is therefore deferred.
7. The write controller derives the actor from the authenticated request. The
   request contract has no actor field.
8. The frontend route is `/Microbiology/whonet` with `from`, `to`,
   `significance`, `dedup`, `step`, `page`, and `pageSize` query state.
9. Mapping repair uses the M3 admin `edit` query state to open the exact organism
   or antibiotic record. Browser history returns to the preview.
10. The config-backed Microbiology menu gains the canonical WHONET Export child
    and removes the competing legacy Reports navigation entry; no database
    migration is used for navigation. Removing the remaining legacy report
    implementation is separately tracked in
    [GitHub #3983](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3983)
    because direct callers may still exist.

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

The original Liquibase changeset adds the export-run audit entity. A follow-up
changeset removes one redundant compatibility column and an unneeded index while
preserving the original checksum for environments that already applied M4.
Rollback of the M4 migration file removes the table. Routes, menu config,
fixtures, tests, and UI do not receive migrations.

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

## R8 Engineering Addendum - Specimen Mapping

R8 closes the first remaining vocabulary gap without changing the established
long-format file shape. `TypeOfSample` already owns a nullable WHONET specimen
code and Sample Type Management already carries that value in its REST contract.
The exporter therefore reads the existing field, projects it into the existing
`SPECIMEN_TYPE` column, and links missing mappings to the owning sample-type
editor. No microbiology-specific specimen table or mapping endpoint is added.

The generic sample-type terminology editor currently also offers `WHONET` as a
source even though it is not synchronized with the dedicated specimen code.
That competing authoring option is removed from the touched admin surface; the
single export-relevant value is edited with the sample type itself. Existing
organism and antibiotic repair behavior is unchanged.

The repair URL carries the exact canonical export preview as a validated local
return destination. The sample-type editor focuses the WHONET control on entry
and exposes an explicit return action after save. The service-created WHONET
scenario supplies both mapped and unmapped sample types through existing
services. No migration is added because no data model changes.
