# Implementation Plan: OGC-782 M4 WHONET Manual Export

## Technical Context

M4 stacks on the completed M3 reference-data foundation. The repo already
contains the legacy Reports-to-WHONET path,
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
3. Export eligibility requires a finalized bacteriology case, while reporting-
   period membership uses the specimen collection timestamp in a half-open date
   interval. Final close time does not determine period membership.
4. The local seven-day policy sorts by specimen collection time and keeps the
   first isolate for each patient and organism within each rolling seven-day
   window. It is explicitly not labeled WHO/CLSI-certified. A later configured
   window-basis option may change this de-duplication chronology without
   changing reporting-period membership.
5. Missing organism or antibiotic codes are warnings and exclude affected rows.
   Readings without a final S/I/R interpretation are also excluded. Zero
   remaining rows is a generation blocker.
6. A new export-run entity stores generation metadata and a SHA-256 fingerprint,
   not the PHI-bearing file body. Exact re-download is therefore deferred.
7. The write controller derives the actor from the authenticated request. The
   request contract has no actor field.
8. The frontend route is `/Microbiology/whonet` with canonical reporting,
   population, workflow-step, paging, and optional AST-worklist provenance query
   state.
9. Mapping repair uses the M3 admin `edit` query state to open the exact organism
   or antibiotic record. Browser history returns to the preview.
10. The config-backed Reports menu owns the single navigation entry for the
    canonical export page. The stable `/Microbiology/whonet` route is retained
    for links and saved previews, while the competing Microbiology menu entry is
    removed; no database migration is used for navigation. Removing the
    remaining legacy report implementation is separately tracked in
    [GitHub #3983](https://github.com/DIGI-UW/OpenELIS-Global-2/issues/3983)
    because direct callers may still exist.
11. Advanced first-isolate requests retain the existing `NONE` and seven-day
    policy identifiers and add 14- and 30-day identifiers. Separate normalized
    values carry date basis, specimen-source scope, contaminant-first handling,
    and susceptibility-profile sensitivity through preview and generation.
12. Reporting-period selection continues to query finalized cases by specimen
    collection time. De-duplication uses either that collection timestamp or
    the case's final close timestamp, depending on the selected chronology.
13. Same-source mode extends the patient-organism comparison key with the
    authoritative sample-type identifier. Profile-sensitive mode extends it
    with a stable, sorted profile of antibiotic identifier plus final reviewed
    S/I/R interpretation from reportable AST runs.
14. The existing export-run `dedup_policy` stores the selected window identifier.
    The existing JSON population selection records the remaining normalized
    policy fields, so this slice requires no database migration and remains
    backward-compatible with earlier audit rows.
15. Episode-based selection is not accepted by the API or shown in the UI until
    its episode boundary is defined. Repeat rows remain dropped; adding the
    `FIRST_OR_REPEAT` marker is part of the versioned, import-qualified output
    contract rather than an isolated mutation of the legacy 15-column CSV.

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

- JUnit 4/Mockito: collection-date boundaries, final-case eligibility,
  significance,
  seven-day de-duplication, mapping warnings, multiple AST readings, CSV
  escaping, blocked generation, and authenticated actor.
- Controller: worklist and export query binding, attachment headers,
  authorization, and actor spoof resistance through absence of client actor
  input.
- ORM/Liquibase: entity registration plus update and rollback.
- Vitest/RTL: worklist and generator canonical query state, reporting presets,
  Carbon interactions by role/label, transferred-scope notice and clear action,
  preview counts, exact mapping links, disabled/enabled generation, and download.
- Playwright `core-app`: authenticated AST-worklist filtering, export handoff,
  editable transferred scope, clear behavior, preview, generation, and
  downloaded CSV assertions. No arbitrary waits.
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

## R9 Engineering Addendum - Population And Culture Purpose

Research distinguishes routine AMR surveillance of clinical diagnostic
specimens from active screening intended to detect colonization or carriage.
Program selection, patient origin, specimen type, organism, requesting location,
and the test-level AMR flag do not reliably encode that order-specific intent.

R9 is therefore divided into two coherent behavior slices:

1. Filter by data already captured authoritatively: specimen, organism, patient
   origin, and significance. Apply the same normalized filters on the server to
   preview and generation, preserve them in the canonical URL, move the single
   navigation entry to Reports, and compact the touched page toward the M-09
   operational layout.
2. Add Culture purpose to microbiology order context with Clinical
   diagnosis/treatment and Active screening/carriage choices. New orders default
   visibly to clinical purpose; historical missing values remain Unspecified.
   The case displays the value, corrections before final release are audited,
   and export excludes screening and unspecified records by default unless the
   user explicitly includes them.

The second slice is an actual data-model change and therefore receives one
Liquibase migration with rollback. It extends the existing microbiology order
detail and case workflow rather than introducing a separate surveillance record.
FHIR projection, when implemented, should carry the order-specific context in
the laboratory ServiceRequest; it is not required to complete these local
workflow slices.

## R12 Engineering Addendum - Reporting Scope Handoff

R12 resolves the source gap between M-07 and M-09. The AST worklist gains only
the structured surveillance filters that have direct WHONET meaning: reporting
period, specimen type, patient origin, organism, and isolate significance. Its
operational controls remain independent and are never translated into export
criteria.

One canonical scope representation is shared by the worklist handoff and the
generator. Direct Reports entry defaults to the previous complete month; AST-
worklist entry without an active period defaults to the full current calendar
month. The generator retains the stable Reports-owned route, identifies
the transferred scope, permits editing, and clears back to direct-entry defaults.

The backend changes period membership and default first-isolate ordering from
final close time to specimen collection time while retaining final release as an
eligibility gate. No data-model migration is required because collection time
and every transferable filter value already have authoritative sources.
