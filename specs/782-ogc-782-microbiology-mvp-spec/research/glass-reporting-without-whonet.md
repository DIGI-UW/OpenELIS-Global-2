# Research: GLASS Reporting Without a WHONET Dependency

**Status**: Research and engineering direction. This document is not roadmap
status, a user acceptance record, or a claim that the described system exists.

**Evidence boundary**: Repository findings were checked against the AMR demo
code snapshot `b1c692ba930decc371a67cd7c534c663a2932471`. External output
contracts must be rechecked against the active World Health Organization GLASS
release before implementation.

## Purpose

Define how OpenELIS can support Global Antimicrobial Resistance and Use
Surveillance System (GLASS) reporting without requiring WHONET or BacLink.

The target reporting chain is:

`source data -> open mapping profile -> canonical line-level records -> national aggregation -> GLASS RIS and SAMPLE files -> authorized upload`

WHONET isolate files may remain an optional migration input. WHONET software,
configuration files, code tables, and internal data structures must not be
required by the target system.

This conclusion does not depend on whether WHONET is free to use. It avoids a
runtime and data-model dependency and allows the reporting behavior to be
tested, versioned, and maintained directly.

## Sources Of Authority

- OpenELIS Work defines functional workflow and visual intent:
  <https://digi-uw.github.io/openelis-work/designs/microbiology/glass-submission-console.html>
- The World Health Organization defines GLASS output meaning and submission
  rules:
  <https://iris.who.int/bitstream/handle/10665/372741/9789240076600-eng.pdf>
- WHONET documentation explains the behavior that must be replaced:
  <https://whonet.org/WebDocs/BacLink%202.Excel%2C%20text%20files%2C%20other%20applications.html>
- The detailed BacLink conversion and code-mapping guide is available at:
  <https://whonet.org/WebDocs/BacLink.9_BacLink_and_data_conversion_and_code_mapping.pdf>
- The WHONET GLASS workflow is described at:
  <https://whonet.org/WebDocs/WHONET_for_GLASS.English.pdf>
- FHIR R4 defines the report and atomic result structures:
  <https://hl7.org/fhir/R4/diagnosticreport.html> and
  <https://hl7.org/fhir/R4/observation.html>
- The HL7 Europe microbiology example demonstrates structured culture,
  organism, and susceptibility relationships:
  <https://hl7.eu/fhir/laboratory/Bundle-BundleLabResultMicroCultureSusc.html>
- CLSI and EUCAST are the clinical breakpoint authorities:
  <https://clsi.org/shop/standards/m100/> and
  <https://www.eucast.org/bacteria/clinical-breakpoints-and-interpretation/clinical-breakpoint-tables/>

## Confirmed Current Workflow

1. BacLink is configured against a concrete source layout such as a CSV,
   spreadsheet, or laboratory-system export.
2. The configuration records parsing rules, dates, source columns, row shape,
   antimicrobial layout, and local-code mappings.
3. BacLink saves those choices as a reusable `.cfg` file.
4. Later files with the same layout are converted into WHONET's line-level
   isolate representation.
5. WHONET applies scope and repeat-isolate rules and produces aggregate GLASS
   files.

The replacement must cover every stage. Generating a WHONET-compatible
intermediate file alone would preserve the dependency rather than replace it.

## GLASS Output Meaning

The aggregated reporting workflow requires two related datasets:

- **RIS** contains counts of resistant, intermediate, susceptible,
  uninterpretable, and unreported antimicrobial results grouped by the required
  country, year, specimen, pathogen, sex, patient origin, age, and
  antimicrobial dimensions.
- **SAMPLE** contains patient and specimen denominators for the same reporting
  scope. It includes target-pathogen positives, other positive cultures, and
  negative or no-growth specimens.

Both outputs must come from the same frozen source cohort. Negative cultures
cannot be discarded merely because they have no isolate or susceptibility
reading. A national reviewer remains responsible for final review and upload.

The current official templates, data dictionary, missing-value conventions,
and validation examples are required implementation inputs. Old WHONET output
or mock labels must not be treated as a substitute for the active contract.

## Current OpenELIS Findings

### Reusable capabilities

- `RegisterFhirHooksTask` supports configurable FHIR subscriptions and includes
  site name and site code headers.
- `FhirTransformServiceImpl` already transforms Patient, Specimen,
  ServiceRequest, Result-as-Observation, and finalized
  Analysis-as-DiagnosticReport data.
- The microbiology implementation already records cases, isolates, reviewed
  susceptibility readings, overrides, release states, warnings, and audited
  export runs.

### Gaps and dependency leaks

- `MicroReportProjectionServiceImpl` flattens microbiology content into a short
  text Result. The exported Observation consequently loses organism/isolate
  relationships, antimicrobial identity, raw measurement, method,
  interpretation, and breakpoint provenance.
- `MicroWhonetDatasetServiceImpl` emits one row for each reviewed reportable
  susceptibility reading. Its 15 columns are not either aggregate GLASS
  dataset.
- That export starts from isolates and susceptibility readings, so finalized
  no-growth cases are absent from the denominator.
- Facility-level first-isolate filtering cannot correctly remove repeats across
  multiple sites.
- `MicroBreakpointImportServiceImpl` identifies antimicrobials through a
  WHONET code. Canonical antimicrobial identity must be independent of WHONET;
  WHONET codes can be optional external aliases.

The facility laboratory system should publish complete, finalized line-level
meaning. National scope, cross-site patient matching, repeat-isolate selection,
and GLASS aggregation belong in the national reporting application.

## OpenELIS Work Alignment

The GLASS submission console correctly establishes these product behaviors:

- A separate national reporting console receives data from multiple sites.
- OpenELIS sites can contribute through FHIR.
- Other sites can contribute through uploaded files.
- Unresolved mappings are held for review rather than silently dropped.
- Repeat-isolate selection happens nationally.
- RIS and SAMPLE are generated separately and reviewed before manual upload.

Three corrections are required:

1. Replace "map local code to WHONET code" with "map local code to a canonical
   concept and then to the versioned GLASS code." A WHONET alias is optional.
2. Make general CSV, TSV, and spreadsheet import with an open mapping profile
   the primary file workflow. WHONET input is compatibility, not a prerequisite.
3. Describe RIS records as aggregate counts. The interface may separately show
   how many line-level isolates contributed to each count.

## Target System

### Input adapters

- Structured OpenELIS FHIR microbiology data.
- CSV, TSV, and spreadsheet files from non-OpenELIS sites.
- Optional WHONET isolate files for migration and interoperability.

### Open mapping profile

The mapping workbench creates an openly documented, versioned JSON or YAML
profile for a concrete source layout. The profile is reusable only while later
files match that layout.

The profile must describe:

- Sheet, header row, delimiter, quoting, encoding, decimal, and date rules.
- One-isolate-per-row, one-antimicrobial-per-row, horizontal, vertical, fixed,
  or variable antimicrobial layouts.
- Patient, specimen, isolate, and susceptibility grouping keys.
- Source columns for demographic, origin, location, culture-purpose, specimen,
  organism, antimicrobial, method, measurement, unit, interpretation, and
  guideline fields.
- Local-to-canonical code mappings, explicit ignored values with reasons, and
  blocked unresolved values.
- Representative test examples and expected canonical records.

Before import, the system must show raw, parsed, and canonical previews. Header
or layout drift must produce an exact difference and block processing. Unknown
codes must remain quarantined, countable, traceable, and eligible for reprocess
after mapping.

### Canonical line-level records

The system needs its own representation of:

- Import run and source lineage.
- Facility and source system.
- Privacy-safe patient token and national match key.
- Encounter and patient-origin context.
- Specimen and culture purpose.
- Positive, other-positive, or no-growth culture outcome.
- One or more isolates.
- One or more susceptibility runs and readings.
- Clinical standard reference and terminology aliases.

It must retain negative cultures, multiple isolates, repeat runs, amendments,
raw comparator/value/unit, source interpretation, computed interpretation,
method, authority, version, override, reviewer, and original source record.

For related turnaround-time reporting, each test must retain specimen-received
and biological-validation/result-release timestamps. Turnaround time is the
interval between those two events.

### Clinical standards

CLSI and EUCAST data, not WHONET tables, define breakpoint interpretation.
CLSI M100 is licensed and must not be scraped or bundled without appropriate
rights. EUCAST data must be used under its published terms.

The system must preserve:

- Raw measurement, comparator, and unit.
- Interpretation received from the source laboratory.
- Interpretation computed centrally, when applicable.
- Guideline authority, edition/version, method, and rule inputs.
- Override reason, reviewer, and time.

EUCAST `I` means susceptible with increased exposure. It must not be silently
collapsed into a generic "intermediate" category.

### National compiler and release

The national application applies the active GLASS scope and version, matches
patients across sites using an approved privacy-safe key, applies the repeat
isolate policy once, and compiles RIS and SAMPLE from one immutable cohort.

Every aggregate count must trace back to canonical records and their raw source
rows or FHIR resources. An authorized reviewer validates the output, downloads
the files, uploads them to the World Health Organization, and records the
submission outcome.

## Scope Boundary

Included:

- Replacing BacLink-style source interpretation with an open mapping profile.
- Replacing the WHONET isolate representation with canonical records.
- Replacing WHONET's GLASS aggregation and export behavior.
- Supporting WHONET files as optional compatibility input.

Not automatically included:

- Reimplementation of every WHONET outbreak, trend, and antibiogram feature.
- Silent redistribution of licensed CLSI content.
- National aggregation inside each facility OpenELIS installation.
- Inference of current GLASS columns from historical files.

## Decisions Still Required

1. Define a stable, privacy-safe cross-site patient matching contract.
2. Obtain and approve the active GLASS RIS and SAMPLE templates, data
   dictionary, and validation examples.
3. Approve JSON or YAML as the portable mapping-profile representation and
   define its formal schema.
4. Define precedence when a reviewed site interpretation disagrees with a
   centrally computed interpretation. The recommended default is to preserve
   both and flag the disagreement.
5. Select the repository, deployment, and ownership boundary for the separate
   national reporting application.

## Recommended Build Order

1. Freeze external contracts and representative FHIR, wide-file, long-file,
   spreadsheet, WHONET, CLSI, and EUCAST test fixtures.
2. Define canonical records and source-lineage rules.
3. Define and test the open mapping-profile schema and execution engine.
4. Build the file mapping, preview, quarantine, and reprocessing workbench.
5. Add structured culture, isolate, and susceptibility FHIR output to
   OpenELIS.
6. Add versioned CLSI and EUCAST interpretation support.
7. Add national GLASS scope, patient matching, and repeat-isolate selection.
8. Compile and validate RIS and SAMPLE from one immutable cohort.
9. Qualify review permissions, security, accessibility, performance, backup,
   recovery, and user acceptance behavior.

## Deterministic Acceptance Criteria

- A profile created from a concrete layout processes later matching files.
- Header or structural drift blocks import and displays the exact difference.
- Wide and long susceptibility examples normalize to equivalent records.
- Unknown codes never silently remove rows.
- FHIR and file representations of the same case produce equivalent clinical
  meaning.
- No-growth records contribute to SAMPLE and never fabricate RIS isolates.
- Cross-site repeat selection occurs exactly once at the national layer.
- Every computed interpretation identifies its authority, version, method, and
  inputs.
- RIS and SAMPLE derive from the same immutable cohort.
- Every aggregate count traces to canonical and raw source records.
- Identical data, profile, mappings, standards, and GLASS version produce
  byte-identical output.
- A clean deployment without WHONET software or data tables completes the full
  reporting workflow.

## Companion Visualization

Open `glass-reporting-architecture.html` for the interactive comparison,
mapping examples, canonical data model, current gaps, decisions, and build
order. The visualization is explanatory evidence; this Markdown document is
the durable textual record.
