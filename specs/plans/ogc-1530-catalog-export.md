# Catalog export in the import format (OGC-1530 slice 5)

Source requirements: openelis-work `designs/admin-config/reference-data-config-frs.md`
(FR-4.1, FR-4.3 localization columns, FR-4.4, FR-8.3) and
`reference-data-config-breakdown.md` slice 5. Grounded in develop `13337b74c3`
and the Microbiology V3 stack as of 2026-11-10.

## Goal

A lab administrator can download any configuration area as a CSV in the exact
format its loader reads, edit it in a spreadsheet, and load it back. An
unedited download loaded back changes nothing.

## Acceptance

1. An unedited export, imported back into the server it came from, changes
   nothing: a second export is byte-identical to the first, and no row's
   `lastupdated` or audit history moves.
2. An export imported into a database that lacks those records creates them with
   no rejected row (within one area's own dependencies).
3. `-9.4705` and `+675 325 1190` survive the round trip; a text value starting
   with `=` is escaped on export and restored on import.
4. The export carries a `localization:xx` column for every active locale.
5. The AST reporting rules export (PR #4832) is served by the same hook once
   both are on develop.

## What exists on develop

- `AbstractCatalogCsvHandler` reads columns ad hoc with `row.get("name")`. Only
  `requiredColumns()` is declared; there is no column list to reuse for export.
- `tests`, `test-sections`, `sample-types`, `panels` support dry run but carry
  their own header parsing.
- One export: `LocationsImportServiceImpl.export` (own `quote`, no formula
  guard).
- `StringUtil.csvEscape` prefixes `'` before `= + - @` unconditionally, which
  corrupts negative numbers and phone numbers (FR-4.4 forbids that).
- `CsvLoadSummary.Outcome` has CREATED, UPDATED, SKIPPED. A re-import of
  identical data reports every row as UPDATED and writes it.
- Readers skip `#` lines after the header only.

## Overlap with the Microbiology V3 stack (OGC-1528, OGC-1607)

23 open PRs by one author: a trunk of 17 from #4823, then five tips. The trunk
is 59 commits behind develop and #4823 waits on review. The stack changes 247
files; these are shared with this work:

| File | What V3 changes | In PR | Effect here |
|---|---|---|---|
| `LoadedRow`, `CsvLoadSummary` | Adds `UNCHANGED`, `REMOVED`, `WARNING` outcomes and `LoadedRow.unchanged` | #4998 | This work needs `UNCHANGED`. It must not add a second one. |
| `AbstractCatalogCsvHandler` | Adds `rowError` (line number on every bad row) | #4998 | Different hunk from the reader rule; low conflict |
| `CatalogImportRestController`, `UnresolvedReference`, `CatalogReferenceResolver` | Organism and antibiotic decision candidates | #4998 | Export goes in its own controller, so no shared hunk |
| `CatalogImportServiceImpl` | Removes the folder a failed apply created | #4998 | Not touched here |
| `CatalogImport.jsx` and its test | Neutral "unchanged" rows, warnings | #4998 | Export panel is its own component, mounted with one line |
| `MicroOrganism`, `MicroAntimicrobial`, `MicroAstPanel` handlers | Organism groups, panel defaults, stricter errors | #4958, #4994, #4995, #4998 | Their exports wait until V3 is on develop |
| New: `MicroOrganismGroup`, `MicroQcRange`, `MicroReportingRule` handlers | Three new areas; reporting rules has its own export | #4832, #4845, #4994 | Adopt the hook after V3 lands |
| `en.json`, `liquibase/3.5.x.x/base.xml` | Many keys; changesets | all | Routine merge conflicts |

Consequence: the shared outcome model (`UNCHANGED`, `REMOVED`, `WARNING`) is
the one real collision. It sits near the top of the V3 stack, so it will reach
develop late. Proposed route, in order of preference (not yet raised with the
V3 owner):

1. The V3 owner lands the outcome-model commits (`6f2353d70c`, `4593c38679`,
   `6cd80b312f`, `a73535968e`) as a small PR against develop. Both stacks then
   build on it.
2. If that does not happen before PR 2 here is ready, PR 2 carries the same
   enum values and factory names so the later merge is textual, not semantic.

PR 1 here does not touch `LoadedRow` or `CsvLoadSummary`.

## How AMR for R data fits

Today (OGC-1533, #4801 merged; AMR fork PR #1 and PNG distro PR #11 open):

```
DIGI-UW/AMR data-raw/openelis/export.R + build_package.py
  -> immutable versioned package on GHCR (organisms, antimicrobials,
     breakpoints as ordinary CSV + provenance, coverage, comparison cases)
  -> distro pins URL + SHA-256 (PNG: configs/configuration-packages/*.json)
  -> scripts/prepare-configuration-packages.py verifies and copies the CSVs
     into configuration/backend/microbiology-*/
  -> OpenELIS loader areas at start-up or reload
```

OpenELIS never runs R and never reads the archive. The package is GPL-2.0 data
kept out of the MPL-2.0 source tree, as V3 FR-R21.2 requires.

This is the "pack" of the Reference Data design, already working for one
publisher. It is import-only by nature. Rules for this work:

- **Package-owned rows are not exported for editing.** Breakpoint editions
  loaded from a package reject changed content under the same identity, so an
  edited re-import would fail by design. Breakpoints are excluded from export.
- **Lab-owned microbiology areas are exported:** AST panels, reporting rules,
  organism groups, QC ranges, settings. These are what a lab copies between
  servers.
- **Organisms and antimicrobials** carry `source` + `code` from the package.
  That pair is the matching identifier the Reference Data design wants on every
  record (FR-3.1), so their export writes it and a re-import matches on it. The
  export covers what the lab set locally (display name, active, WHONET code),
  not a second copy of the package.
- **Scale.** V3 seeds the whole package list (about 103,000 taxa, FR-R13.1).
  The writer streams rows; UNCHANGED detection must stay linear (develop
  already fixed this for breakpoint reloads in `33abcd1a8c`).

What V3 still wants from the package and the pipeline does not yet give
(owned by OGC-1528 and OGC-1531, listed so this work does not block it):
taxonomy, synonyms and group membership (R-12, R-13); antibiotic ATC, LOINC and
synonyms (R-19); breakpoints in the package's native columns with indication
(R-14, R-18); comparison cases run as an import gate (FR-R18.3); starter
reporting rules generated from the package (R-08); and "Keep local" or "Use
pack value" on a new package version (FR-R25.5), which needs Import Review.

## Metadata issues raised on 2026-11-09 (AMR call)

Checked against the DIGI-UW/AMR fork (`c/ogc-1533-reference-package`) and
develop. None of them changes the export design; two change what is loaded.

| Raised | Finding | Owner |
|---|---|---|
| Genus entries ("Klebsiella sp.") have no WHONET code | The data is there: `microorganisms.codes` holds 6,293 codes, 1,753 pointing at a genus. `data-raw/openelis/export.R:222-228` keeps a code only when its source text equals `WHONET (<fullname>)`. For a genus the source text is `WHONET (Klebsiella sp.)` and the fullname is `Klebsiella`, so 444 genera lose their code and 4 keep one. | Exporter fix in the AMR fork (OGC-1533) |
| Users expect "Klebsiella sp.", not a bare genus | The same source text carries the WHONET name. The exporter writes only `displayName = fullname`. | Exporter: add the WHONET name; display choice is V3's |
| Organisms missing from the list (about 443 loaded) | By design of the first package: breakpoint targets plus species with `prevalence == 1` and a WHONET mapping (`export.R:18-33`). The source has 103,222 taxa. V3 FR-R13.1 asks for the whole list. | Package scope (OGC-1533) and V3 R-13 |
| "Most common organisms" for panel setup | The source has a `prevalence` column (1, 1.15, 1.25, 1.5, 2); V3 FR-R13.1a ranks pickers by it. No WHONET-curated list is published. | V3 R-13 |
| AST panels cannot be linked to organisms | Linking exists as a `defaultFor` column on `microbiology-ast-panels` (develop) but has no screen there. V3 PRs #4995 and #5009 add the matching and the panels page control. | V3 stack |
| Entering S, I or R directly when no measurement or breakpoint applies | No requirement found in the V3 FRS (searched; R-30 keeps the measurement and reports "No breakpoint"). | Product decision |
| Antibiogram and GLASS submission | Deferred to a separate surveillance epic (D-340). | After V3 |

What it means for this work: panel-to-organism links and the lab's organism
shortlist are lab-owned data, so they belong in the PR 4 exports. The exporter
fix adds a column to `organisms.csv`; the organisms loader and its export must
carry it.

## Design

1. **Export hook on the handler contract.** `DomainConfigurationHandler` gains
   `default boolean supportsExport()` (false) and
   `default CatalogExport export()` returning the ordered header and a stream
   of rows. The handler that reads a format is the one that writes it.
2. **One writer.** `CatalogCsvWriter` turns a `CatalogExport` into CSV: RFC 4180
   quoting, the FR-4.4 formula guard (text starting with `= + - @`, tab or CR
   gets a leading `'`, except a plain number or a phone number), and the
   optional leading `#` filter line.
3. **One reader rule.** The shared reader strips one leading `'` before those
   characters and skips `#` lines before the header. It applies to
   `AbstractCatalogCsvHandler` and the four handlers with private parsing.
4. **Unchanged rows write nothing**, using the V3 outcome model (see overlap).
   Each exporting handler compares before saving.
5. **Own controller.** `CatalogExportRestController`:
   `GET /rest/configuration/export?domain=<area>` and
   `GET /rest/configuration/export/domains` (ADMIN, as import is today).
6. **UI.** An `ExportPanel` component on Admin > Import Catalog: "What to
   export" and "Download CSV". New keys in `en.json` only.
7. **Round-trip harness.** One parameterized integration test: load the area's
   fixture, export, import the export, export again; assert the two exports are
   equal. Each area added is one line. Each handler also gets one "edit one
   cell, the record changes" test.

## Stack

One `gh stack` from develop, each PR reviewable alone.

| PR | Contents | Touches V3's files? |
|---|---|---|
| 1 | Hook, writer, reader rule, controller, harness; `sample-types` and `test-sections` exports. Acceptance 1 is asserted by export equality. | `AbstractCatalogCsvHandler` (other hunk) |
| 2 | `tests` and `panels` exports; unchanged rows write nothing for the four areas so far; `ExportPanel` | `LoadedRow`, `CsvLoadSummary` via the proposed route; `CatalogImport.jsx` one line |
| 3 | `result-components`, `result-limits` (adds `lowReporting`, `highReporting`), `terminology`, `answer-terminology`, `sample-handling`, `reflex-rules` | no |
| 4 | After V3 is on develop: `microbiology-ast-panels`, `-organism-groups`, `-qc-ranges`, `-settings`, local fields of organisms and antimicrobials; the reporting rules export moves onto the hook | yes, by then merged |

`dictionaries` and `test-results` follow OGC-1530 slices 1 and 2.

## Not in this slice

- The Excel workbook and "export everything" (FR-4.2, FR-7; phase P2).
- Hidden matching-identifier and row-signature columns (P2, after slice 7).
- List filters on catalog exports. The writer supports the `#` filter line;
  Replace reading it back is Import Review slice 13.
- Fields the loaders cannot read today (test description separate from name,
  holding time, QC thresholds, methods). An export is a faithful copy of what a
  file can set.
- Package-owned breakpoint editions.

## Risks

- Stripping a leading `'` on import changes behaviour for a value that
  genuinely starts with `'=`. The FRS accepts this.
- UNCHANGED detection must compare every column the handler writes; a missed
  column makes a real edit look unchanged.
- V3 lands later than PR 2 is ready and the outcome model has to be carried
  twice (route 2 above).

## Status

| Step | State |
|---|---|
| Plan and overlap analysis | done 2026-11-10 |
| Outcome-model route agreed with the V3 owner | not started |
| PR 1 | not started |
| PR 2 | not started |
| PR 3 | not started |
| PR 4 | waits for V3 on develop |
