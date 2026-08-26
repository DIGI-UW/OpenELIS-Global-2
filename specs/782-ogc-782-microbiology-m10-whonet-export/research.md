# Research And Artifact Crosswalk

## Pre-M4 Baseline

- `WHONetReportServiceImpl` selects legacy AMR analyses and emits the existing
  fifteen-column long CSV contract.
- `WHONETCSVRoutineColumnBuilder` owns quoting and the established header.
- `MicroWhonetReadinessServiceImpl` checks one case at a time and only organism
  and antibiotic mapping.
- M3 provides server-backed organism and antibiotic administration with exact
  `edit=<id>` URL state.
- Final microbiology release records `FINAL_RELEASED` and `closedAt`; repeat AST
  already identifies the reportable run.
- No microbiology export-run history or generated-file audit exists.

## M4 As-Built State

M4 adds a canonical Microbiology export page, scoped used-set readiness,
service-compiled preview data, manual CSV generation through the existing
15-column OpenELIS row builder, and an immutable audit summary. It does not yet
prove import into a current WHONET release or implement the mock's full wide
format, profile packaging, scheduling, or delivery model.

## Artifact Health

| Artifact | Classification | Issue | M4 ruling |
| --- | --- | --- | --- |
| OGC-794 | Implementation leakage | Names a table, eight pages, formats, column families, scheduler, and delivery mechanisms as scope | Keep the user outcome; carry technical choices in `plan.md`; split later delivery modes |
| OGC-878 | Engineering decision needed | Calls the de-dup policy WHO/GLASS-aligned while verification is still listed as a dependency | Implement and label a deterministic local seven-day option; do not claim certification |
| OGC-879 | Real contradiction | Jira is Done/superseded but repo has readiness only | Treat generation as unimplemented until tests prove it |
| OGC-880 | Product breadth mismatch | Requires eight mapping vocabularies although current export data and ownership are settled only for organism and antibiotic | Provide exact repair for currently used mappings; defer unsettled vocabularies |
| OGC-881 | Real contradiction | Jira is Done/superseded but no repo history exists | Add generation audit metadata; defer profile packaging and exact re-download |
| M-09 FRS | Implementation leakage | Prescribes routes, schemas, services, columns, scheduler, SFTP, and profile packaging | Use workflow intent and mock hierarchy only |
| M-09 mock | Product guidance | Combines mapping, export, scheduling, FHIR, and standards claims in one prototype | Match compact Configure/Preview/Generate behavior; omit unavailable downstream capabilities |

## Visual Baseline

The authoritative painless mock establishes:

- a compact report page rather than a dashboard of decorative cards;
- previous-month and clinically-significant defaults;
- visible Configure, Preview, Generate progression;
- counts before and after filtering/de-duplication;
- mapping gaps as contextual warnings with direct repair;
- advanced and automated delivery options visually subordinate to the routine
  manual path.

M4 intentionally replaces prototype-only HTML controls with Carbon components,
uses the current application breadcrumb and navigation patterns, and does not
render unavailable scheduling or FHIR controls.

## Remaining Clarifications

- Verify the current authoritative WHONET column and method-suffix contract
  through a real import before claiming compatibility or full wide-format
  parity.
- Verify WHO/CLSI first-isolate guidance before labeling the local seven-day
  policy standards-compliant.
- Decide ownership and source vocabularies for specimen, origin, patient type,
  department, breakpoint-standard, and phenotype mappings.
- Decide retention and security policy before storing PHI-bearing export bytes
  for exact re-download.
