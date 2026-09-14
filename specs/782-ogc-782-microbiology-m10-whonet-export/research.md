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

## R9 Research - Clinical Diagnostic Versus Active Screening Cultures

### Question

Where should OpenELIS obtain the distinction needed by the M-09 mock's
"Include screening / surveillance cultures" filter?

### Findings

WHO GLASS routine AMR surveillance is based on specimens sent for routine
clinical purposes from patients seeking care. It is surveillance performed over
clinical diagnostic data; "surveillance" does not itself mean that the specimen
was collected to screen an asymptomatic patient.

CDC/NHSN separately defines Active Surveillance Culture/Testing as testing whose
intent is to identify carriage for isolation precautions or to monitor a carrier
state. It explicitly distinguishes that intent from cultures performed for
diagnosis and treatment. Because the distinction is the reason for ordering the
test, it cannot be recovered reliably from organism, specimen, patient location,
or the eventual result.

WHONET's standard laboratory configuration captures patient, location,
specimen, organism, and AST fields and permits laboratories to add coded fields
for local clinical or infection-control needs. WHONET for GLASS likewise treats
the standard clinical dataset and additional surveillance activities as
separate configurable data. This supports sending an explicit order-time value
rather than inventing an export-time inference.

FHIR R4 ServiceRequest provides order-context elements such as `orderDetail` and
`reasonCode`, but does not impose a universal culture-purpose value set. That
supports keeping a controlled OpenELIS choice now and mapping it explicitly when
the laboratory-order FHIR profile is extended.

### Ruling

- Replace the ambiguous mock wording with **Include active screening/carriage
  cultures**. Routine AMR surveillance remains the purpose of the export, not a
  culture-purpose value.
- Capture **Culture purpose** with the microbiology order as Clinical
  diagnosis/treatment or Active screening/carriage. Default visibly to Clinical
  diagnosis/treatment for new routine orders.
- Do not infer purpose from Program, Patient Origin, specimen, organism,
  requesting location, or test-level AMR eligibility.
- Preserve historical missing values as Unspecified. Do not backfill a clinical
  meaning that was never recorded.
- Exclude Active screening/carriage and Unspecified by default; permit explicit
  inclusion and show their counts in preview.

### Sources

| Confidence | Source | Relevance |
| --- | --- | --- |
| High | [WHO GLASS routine data surveillance](https://www.who.int/initiatives/glass/glass-routine-data-surveillance) | Defines the routine AMR population as clinical specimens sent for clinical purposes. |
| High | [CDC/NHSN miscellaneous FAQs](https://www.cdc.gov/nhsn/faqs/faqs-miscellaneous.html) | Defines Active Surveillance Culture/Testing by carriage and infection-control intent and distinguishes it from diagnosis/treatment. |
| High | [WHONET laboratory configuration](https://whonet.org/WebDocs/WHONET%202.Laboratory%20configuration.html) | Documents standard fields and configurable coded clinical or microbiology fields. |
| High | [WHONET for GLASS](https://whonet.org/WebDocs/WHONET%20for%20GLASS.html) | Distinguishes the standard clinical GLASS dataset from additional surveillance activities. |
| High | [HL7 FHIR R4 ServiceRequest](https://www.hl7.org/fhir/R4/servicerequest.html) | Provides order-context structures without prescribing a culture-purpose code set. |
