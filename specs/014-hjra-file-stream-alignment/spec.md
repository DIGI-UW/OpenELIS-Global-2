# Feature Specification: File Stream Alignment — GenericFile Coordination

**Feature Branch**: `spec/014-hjra-file-stream-alignment`  
**Created**: 2026-03-10  
**Status**: Draft  
**Epic**: OGC-304 (Madagascar Analyzer Work)  
**Input**: Coordinate file import work across OGC-324, OGC-329, OGC-344,
OGC-348, OGC-350, OGC-351, OGC-417, OGC-418

---

## Purpose

This is a **coordination specification**, not a single-feature spec. It
establishes:

1. Stream boundaries for the GenericFile work (what belongs here vs. the ASTM
   lane)
2. Issue-bundle sequencing and dependency ordering
3. How OGC-324 (Upload/Review UI) and OGC-329 (File Config + Watcher) couple as
   a foundation pair
4. The parser boundary for a GenericFile plugin vs. the current partial
   file-import path
5. Which analyzers are implementable now vs. blocked by missing real export
   files
6. Branch recommendations for implementation

---

## Critical Design Decision: What Is "FILE"?

### The Problem

`constants.js` currently maps `FILE` → `ASTM_LIS2_A2`, with the comment "FILE is
transport, default message format is ASTM." The backend `ProtocolVersion` enum
has no `FILE` value — only `ASTM_LIS2_A2`, `HL7_V2_3_1`, `HL7_V2_5`.

But the **actual file import path** (`FileAnalyzerReader`) uses Apache Commons
CSV — it parses delimited text with configurable columns, headers, and
delimiters. There is zero ASTM parsing in this path. The
`FileImportConfiguration` entity has CSV-specific fields: `delimiter`,
`hasHeader`, `columnMappings`.

This creates a semantic contradiction:

- The UI says FILE is "ASTM in a file"
- The backend file path is a CSV/delimited-text parser with no ASTM awareness
- QuantStudio exports Excel workbooks (.xls/.xlsx), not ASTM
- Wondfo exports 40-column CSV with its own structure, not ASTM
- ELISA readers (Tecan, Multiskan) export plate grids, not ASTM

### The Resolution

**GenericFile MUST support true non-ASTM file formats.** The `FILE` →
`ASTM_LIS2_A2` mapping in `constants.js` is incorrect for the GenericFile use
case and must be decoupled.

Evidence:

- `FileAnalyzerReader` already parses CSV natively — it never touches ASTM
  framing
- None of the 6 pending analyzer instruments (QuantStudio, Wondfo CSV, Attune,
  FluoroCycler, Tecan, Multiskan) produce ASTM-formatted files
- The Wondfo Finecare _does_ speak ASTM over its serial/TCP interface (OGC-345),
  but its CSV export (OGC-344) is a flat 40-column file — those are two separate
  integration paths

**Concrete changes needed:**

1. `ProtocolVersion` stays message-format-only (ASTM, HL7). Do NOT add file
   format values to it.
2. File-import analyzers carry a **file format config** on the analyzer profile
   (e.g., `fileFormat: CSV | TSV | EXCEL`). This drives parser selection and
   upload validation — it exists because business logic needs it, not as an
   abstract taxonomy.
3. Remove the `FILE` → `ASTM_LIS2_A2` default in `constants.js`. File-import
   analyzers don't have a meaningful `ProtocolVersion`; the field can be
   null/hidden for them.
4. `FileAnalyzerReader` handles delimited formats; a new `ExcelAnalyzerReader`
   handles workbooks. Format config selects which reader is used.
5. Each file-import plugin declares which file format(s) it accepts.

### What This Means for Wondfo

Wondfo has **two integration paths** that must not be conflated:

- **OGC-344** (this lane): CSV file import via USB export — flat 40-column
  `history.csv`
- **OGC-345** (ASTM lane): Real-time ASTM LIS2-A2 over TCP/serial

OGC-344 belongs in the FILE stream. OGC-345 belongs in the ASTM stream. They
share test mappings but not parsers.

---

## Clarifications

### Session 2026-03-10

- Q: Should file format distinction live inside `ProtocolVersion` or as a
  separate concept? → A: Separate. `ProtocolVersion` stays message-format-only.
  File-import analyzers get a file format config on the analyzer profile,
  required only because it drives business logic (parser selection, validation).

---

## Stream Boundaries

### FILE Stream (this spec coordinates)

| Issue   | Instrument             | File Format             | Real Files?               | Status                          |
| ------- | ---------------------- | ----------------------- | ------------------------- | ------------------------------- |
| OGC-329 | (Foundation)           | N/A                     | N/A                       | Foundation — config + watcher   |
| OGC-324 | (Foundation)           | N/A                     | N/A                       | Foundation — upload + review UI |
| OGC-348 | QuantStudio 5/7 Flex   | Excel .xls/.xlsx        | Yes (QS5 + QS7 from LA2M) | **Ready — first target**        |
| OGC-344 | Wondfo Finecare FS-205 | 40-column CSV           | Yes (validation dataset)  | **Ready — second target**       |
| OGC-350 | Attune CytPix          | FCS only (CSV TBD)      | No                        | **Blocked** — no CSV export     |
| OGC-351 | FluoroCycler XT        | .at (proprietary)       | Yes but unusable          | **Blocked** — need CSV export   |
| OGC-417 | Tecan Infinite F50     | CSV/TSV/XLSX (Magellan) | No                        | **Blocked** — no real exports   |
| OGC-418 | Multiskan FC           | CSV/XLSX (SkanIt)       | No                        | **Blocked** — no real exports   |

### NOT in FILE Stream (belongs elsewhere)

| Issue                      | Why Not Here                                                           |
| -------------------------- | ---------------------------------------------------------------------- |
| OGC-345 (Wondfo ASTM)      | Real-time ASTM over TCP — ASTM lane                                    |
| OGC-325 (HL7 MLLP)         | HL7 listener infrastructure — HL7 lane                                 |
| TB-Profiler custom preview | Custom preview plugin — separate story, depends on OGC-324 slot system |

---

## Issue-Bundle Sequencing

### Phase 0: Foundation (OGC-329 + OGC-324 — coupled)

OGC-329 and OGC-324 are a **coupled foundation pair**. They share:

- The `FileImportConfiguration` entity and its admin UI
- The file watcher service and its directory structure
- The preview/review flow (OGC-324 uploads feed into the same review path that
  OGC-329's watcher populates)
- API endpoints for analyzer plugin discovery

**Sequencing within the pair:**

1. **OGC-329 first** — admin config, plugin API, watcher service, directory
   management
2. **OGC-324 second** — upload UI, preview slot system, review mode

OGC-329 must land before OGC-324 because the upload screen needs the plugin
discovery API, file configuration entity, and directory conventions that OGC-329
establishes.

### Phase 1: First GenericFile Profile — QuantStudio (OGC-348)

QuantStudio is the first GenericFile profile target because:

- Real export files exist from LA2M Madagascar (both QS5 and QS7)
- Excel format requires building the app-side `ExcelAnalyzerReader`, which
  validates the file-format abstraction early
- HIV-1 Viral Load is a critical assay for Madagascar
- Multi-sheet workbook parsing exercises the GenericFile plugin's profile-driven
  column mapping beyond simple CSV
- Validates the complete GenericFile path: profile JSON → `AnalyzerPluginConfig`
  → GenericFile plugin → `ExcelAnalyzerReader` → `AnalyzerResults`

### Phase 2: Second GenericFile Profile — Wondfo CSV (OGC-344)

Wondfo CSV is the second GenericFile profile target because:

- Real export files exist (validation dataset with 4 records)
- Simple 40-column CSV validates the CSV path via GenericFile independently of
  Excel
- Comparison operator handling (`<2`, `>100`) is a profile-level config concern
- Field mapping companion guide is complete
- Validates the watcher-triggered import path with a real GenericFile-backed
  analyzer

### Phase 3: Blocked Analyzers (deferred until export files arrive)

| Issue                     | Blocker                                             | What's Needed                                                                            |
| ------------------------- | --------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| OGC-350 (Attune CytPix)   | No CSV export — instrument outputs FCS only         | Confirm if Attune Cytometric Software can export CSV; otherwise need middleware pipeline |
| OGC-351 (FluoroCycler XT) | `.at` files are proprietary Java serialized objects | Need CSV/PDF export from FluoroSoftware; cannot parse .at format                         |
| OGC-417 (Tecan F50)       | No real Magellan exports from target site           | Need ASCII/Excel export from operational Tecan at Madagascar site                        |
| OGC-418 (Multiskan FC)    | No real SkanIt exports from target site             | Need Excel/CSV export from operational Multiskan at Madagascar site                      |

These can begin spec work and stub plugins once export files arrive, but
**parser implementation cannot start without real sample files to validate
against**.

---

## User Scenarios & Testing

### User Story 1 — Admin Configures a File-Based Analyzer (Priority: P1)

A lab administrator adds a new file-based analyzer (e.g., QuantStudio 5) to
OpenELIS. They select the File Import protocol, choose the QuantStudio plugin
from the available plugins dropdown, configure the subdirectory name, and save.
The system auto-creates the directory structure (incoming/, processed/, errors/)
and the analyzer is ready to receive files.

**Why this priority**: Without admin configuration, no file-based analyzer can
operate. This is the gate for all downstream work.

**Independent Test**: Can be fully tested by creating a file-import analyzer
configuration and verifying directory creation, plugin association, and
configuration persistence.

**Acceptance Scenarios**:

1. **Given** the GenericFile plugin is deployed, **When** admin creates a new
   analyzer with File Import protocol, selects the GenericFile plugin type,
   applies the QuantStudio profile, and saves, **Then** the system saves the
   configuration with correct directory paths, `fileFormat=EXCEL`, and the
   QuantStudio column mappings from the profile.
2. **Given** an existing file-import analyzer configuration, **When** admin
   edits the polling interval or toggles the file watcher, **Then** the watcher
   behavior updates without restart.
3. **Given** no file-import plugins deployed, **When** admin selects File Import
   protocol, **Then** the plugin dropdown shows an empty-state message ("No file
   import plugins installed").

---

### User Story 2 — Tech Uploads QuantStudio Results via File (Priority: P1)

A lab technician navigates to Results → Upload Analyzer File, selects the
QuantStudio analyzer, and uploads an Excel workbook exported from QuantStudio
Design & Analysis Software. The system parses the file, shows a preview table
with sample IDs, test results, and validation status. The tech reviews, resolves
any warnings (e.g., accession not found), and submits to the import queue.

**Why this priority**: End-to-end file upload is the core user journey.
QuantStudio is the first real instrument, so this validates the entire path.

**Independent Test**: Can be tested with a real QS5 or QS7 .xls file, verifying
parse → preview → submit → results appear in Analyzer Results.

**Acceptance Scenarios**:

1. **Given** a configured QuantStudio analyzer, **When** tech uploads a QS7 .xls
   file, **Then** the system parses the workbook and displays a preview with
   sample IDs, CT values, and VL results.
2. **Given** a parsed preview with one ACCESSION*NOT_FOUND warning, **When**
   tech edits the lab number inline (editable cell in the preview table),
   **Then** the warning resolves and the row becomes valid. *(Deferred post-M2:
   inline edit of lab number; M2 delivers preview and submit only; editing may
   be added in a follow-up.)\*
3. **Given** a preview with all valid rows, **When** tech clicks Submit,
   **Then** results are queued and the tech is redirected to Analyzer Results
   for QC-first review.
4. **Given** a QS5 .xls file (different column order than QS7), **When**
   uploaded to the same QuantStudio analyzer, **Then** the parser correctly maps
   columns despite ordering differences.

---

### User Story 3 — File Watcher Auto-Imports Wondfo CSV Results (Priority: P2)

A Wondfo Finecare FS-205 exports its `history.csv` via USB to a shared
directory. The file watcher detects the new file, parses it using the
**GenericFile plugin with the Wondfo CSV profile**, and creates an analyzer run.
The tech later reviews results in the standard Analyzer Results page.

**Why this priority**: Watcher-based import is the hands-free path for
instruments with direct file export. Wondfo CSV is the simplest format to
validate this flow.

**Independent Test**: Can be tested by placing a `history.csv` in the configured
incoming directory and verifying results appear in Analyzer Results within the
polling interval.

**Acceptance Scenarios**:

1. **Given** a configured Wondfo analyzer with file watcher enabled, **When**
   `history.csv` is placed in the incoming directory, **Then** the watcher
   detects, parses, and imports results within one polling cycle.
2. **Given** a successfully parsed file, **When** import completes, **Then** the
   file is moved to the processed directory.
3. **Given** a file with comparison operators (e.g., `<2` for CRP), **When**
   parsed, **Then** the result value preserves the operator and is stored
   correctly.
4. **Given** a malformed CSV, **When** the watcher attempts to parse it,
   **Then** the file is moved to the errors directory with an error log entry.

---

### User Story 4 — Blocked Analyzer Awareness (Priority: P3)

A project manager or implementer reviews the analyzer configuration page and can
see which analyzers are "Design To Do" — lacking real export files for parser
validation. The system clearly communicates that parsers for Attune CytPix,
FluoroCycler XT, Tecan F50, and Multiskan FC cannot be completed until sample
files are provided.

**Why this priority**: Prevents wasted implementation effort and sets
expectations for downstream delivery.

**Independent Test**: Can be verified by checking that blocked analyzers are
documented in the spec and that plugin stubs (if created) are clearly marked as
incomplete.

**Acceptance Scenarios**:

1. **Given** no Attune CytPix plugin deployed, **When** an admin attempts to
   configure one, **Then** the system does not show an Attune option in the
   plugin dropdown.
2. **Given** spec artifacts for all 8 issues, **When** a developer reads the
   coordination spec, **Then** they can determine which analyzers are ready to
   implement and which are blocked.

---

### Edge Cases

- What happens when a file matches multiple parser plugins? → Plugin selection
  uses `FileImportConfiguration.analyzerId` first (deterministic); fallback
  `isTargetAnalyzer()` scan only when no config match.
- How does the system handle an Excel file uploaded to a CSV-configured
  analyzer? → File format mismatch detected at parse time; error reported with
  clear message.
- What if the same file is uploaded twice? → Duplicate detection via SHA-256
  hash in `analyzer_file_upload` audit table; warn but allow re-import.
- What if the watcher's polling finds a file still being written? → Use file-age
  threshold (e.g., file must be >2 seconds old) or OS-level lock detection.
- What if a plugin JAR is removed while an analyzer references it? → Show
  "Plugin not loaded" warning in admin UI; watcher skips this analyzer; upload
  rejects with clear error.

---

## Requirements

### Functional Requirements

- **FR-001**: System MUST support file-based analyzer import as a distinct
  protocol type, separate from ASTM and HL7.
- **FR-002**: System MUST support at minimum two file formats: delimited text
  (CSV/TSV) and Excel workbooks (.xls/.xlsx).
- **FR-003**: Each file-import plugin MUST declare its expected file format(s)
  and supported extensions.
- **FR-004**: The admin UI MUST allow configuration of file-import analyzers
  including plugin selection, directory paths, watcher toggle, and polling
  interval.
- **FR-005**: The file watcher service MUST poll configured directories and
  auto-import matching files.
- **FR-006**: The upload UI MUST allow manual file upload with preview,
  validation, and submit-to-queue workflow.
- **FR-007**: The GenericFile plugin MUST interpret the QuantStudio profile to
  handle both QS5 and QS7 Excel workbook layouts via profile-driven column
  mapping.
- **FR-008**: The GenericFile plugin MUST interpret the Wondfo CSV profile to
  handle the 40-column `history.csv` format, including comparison operator
  handling (`<2`, `>100`).
- **FR-009**: The system MUST move successfully processed files to an archive
  directory and failed files to an error directory.
- **FR-010**: The system MUST record each file upload/import as an audit event
  with file hash, timestamp, and user ID.
- **FR-011**: The system MUST NOT conflate Wondfo CSV import (OGC-344) with
  Wondfo ASTM real-time (OGC-345).
- **FR-012**: The `constants.js` FILE → ASTM_LIS2_A2 mapping MUST be removed.
  File-import analyzers use a file format config on the analyzer profile instead
  of `ProtocolVersion`.

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) — NO
  custom CSS frameworks.
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text).
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form).
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct
  DDL/DML).
- **CR-006**: Configuration-driven variation — analyzer parsers are plugins, not
  hardcoded branches.
- **CR-007**: Security: RBAC, audit trail (sys_user_id + lastupdated), input
  validation.
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal).

### Key Entities

- **GenericFile plugin** (`plugins/analyzers/GenericFile/`): Plugin JAR that
  implements `AnalyzerImporterPlugin`. Owns analyzer-specific file
  interpretation via profile-driven column mapping. Peer to GenericASTM and
  GenericHL7. Registers with `PluginAnalyzerService` at startup.
- **Analyzer Profile** (`projects/analyzer-profiles/file/`): Per-instrument JSON
  file (e.g. `quantstudio.json`, `wondfo-csv.json`). Declares `file_format`,
  `supported_extensions`, `column_mapping`, `default_test_mappings`,
  `comparison_operator_handling`, and `configDefaults`. Applied to an analyzer
  instance via the admin setup flow.
- **FileImportConfiguration**: Per-analyzer file transport settings — directory
  paths, file pattern, `fileFormat`, delimiter, watcher toggle, polling
  interval. Links to an Analyzer entity. Drives app-side reader selection, not
  analyzer-specific interpretation.
- **AnalyzerPluginConfig**: Per-analyzer JSONB config (`analyzer_plugin_config`
  table). Stores profile defaults applied on setup — column mappings, file
  format, sheet name, etc. The GenericFile plugin reads this at import time.
- **AnalyzerFileUpload**: Audit record per file upload/import — analyzer ID,
  filename, SHA-256 hash, status, timestamps, user ID.
- **AnalyzerRun**: Represents a single import execution (batch of results from
  one file). Holds `custom_preview_data` (JSONB) for rich preview rendering.
- **ProtocolVersion**: Backend enum for message formats (ASTM, HL7). NOT
  extended for file formats — stays as-is. File-import analyzers leave this
  null/unused.
- **FileFormat** (new config field on `FileImportConfiguration`): Declares the
  expected file format (CSV, TSV, EXCEL). Drives app-side reader selection. Only
  present for file-import analyzers.

---

## Parser Boundary

### App-Side: Format Normalization

The app owns **format normalization** — converting raw file bytes into
structured records keyed by column name:

- `FileAnalyzerReader` (existing) handles CSV/TSV
- `ExcelAnalyzerReader` (new, M3) handles .xls/.xlsx via Apache POI

Both readers produce `List<Map<String, String>>` — format-normalized, not
analyzer-specific. Reader selection is driven by
`FileImportConfiguration.fileFormat`.

### Plugin-Side: Analyzer-Specific Interpretation

The **GenericFile plugin** owns analyzer-specific interpretation. Given a set of
structured records and a loaded profile/config:

- Maps columns from the file to OpenELIS test result fields via profile-driven
  column mapping
- Applies comparison operator handling (e.g. `<2` for quantitative thresholds)
- Determines which OpenELIS test IDs to associate via `default_test_mappings`
- Produces `AnalyzerResults` for persistence

This is exactly how GenericASTM and GenericHL7 work: the app-side reader handles
protocol/format parsing, the plugin handles analyzer-specific semantic mapping.

### Boundary Constraint

The existing
`AnalyzerLineInserter.insert(List<String> lines, String systemUserId)` interface
stays as-is. The GenericFile plugin's inserter receives structured records from
the app service layer. Existing file-import configurations and any plugins
already using the current interface must not break.

---

## Branch Recommendations

| Branch                                    | Issue   | Base                       | Target  |
| ----------------------------------------- | ------- | -------------------------- | ------- |
| `feat/014-ogc-329-file-config-foundation` | OGC-329 | develop                    | develop |
| `feat/014-ogc-324-upload-review-ui`       | OGC-324 | develop (after 329 merges) | develop |
| `feat/014-ogc-348-quantstudio-import`     | OGC-348 | develop (after 329 merges) | develop |
| `feat/014-ogc-344-wondfo-csv-import`      | OGC-344 | develop (after 348 merges) | develop |
| `feat/014-ogc-350-attune-file-path`       | OGC-350 | develop                    | develop |
| `feat/014-ogc-351-fluorocycler-import`    | OGC-351 | develop                    | develop |
| `feat/014-ogc-417-tecan-f50-import`       | OGC-417 | develop                    | develop |
| `feat/014-ogc-418-multiskan-import`       | OGC-418 | develop                    | develop |

**Dependency graph:**

```
OGC-329 (file config foundation)
  ├── OGC-324 (upload/review UI) — sequential after 329
  ├── OGC-348 (QuantStudio) — parallel with 324, after 329
  │     └── OGC-344 (Wondfo CSV) — after 348 validates the path
  └── [blocked analyzers] — after real exports arrive
        ├── OGC-350 (Attune)
        ├── OGC-351 (FluoroCycler)
        ├── OGC-417 (Tecan)
        └── OGC-418 (Multiskan)
```

OGC-324 and OGC-348 can proceed in **parallel** after OGC-329 lands. OGC-348
exercises the backend parser path; OGC-324 exercises the frontend upload flow.
Both validate different aspects of the foundation.

---

## Success Criteria

### Measurable Outcomes

- **SC-001**: A lab technician can upload a QuantStudio Excel file and see
  parsed results in the review page within 30 seconds of upload.
- **SC-002**: The file watcher correctly imports a Wondfo CSV file placed in the
  configured directory within one polling cycle (default 60 seconds).
- **SC-003**: All file-import analyzer configurations are manageable through the
  admin UI without direct database manipulation.
- **SC-004**: The system correctly rejects files that don't match the configured
  format (e.g., Excel uploaded to a CSV-configured analyzer) with a clear
  user-facing error message.
- **SC-005**: Zero ASTM-specific coupling exists in the GenericFile import path
  — the file reader, configuration, and plugin interface operate independently
  of ASTM framing.
- **SC-006**: Blocked analyzers (Attune, FluoroCycler, Tecan, Multiskan) are
  clearly documented as pending export file availability, with no incomplete
  parser code deployed.

---

## Assumptions & Constraints

### Architectural Assumptions (from Constitution)

- UI uses Carbon Design System v1.15+ exclusively (Principle II)
- All strings internationalized via React Intl for en + fr minimum (Principle
  VII)
- 5-layer architecture: Valueholder → DAO → Service → Controller → Form
  (Principle IV)
- Liquibase for all schema changes (Principle VI)
- Plugin-based architecture — no hardcoded analyzer branches (Principle I)
- TDD workflow with JUnit 4 (Principle V)

### Project Assumptions

- QuantStudio real files from LA2M Madagascar are representative of production
  exports for the QuantStudio profile
- Wondfo CSV validation dataset with 4 records is sufficient for initial Wondfo
  profile validation
- The existing `FileImportConfiguration` entity can be extended (rather than
  replaced) to support the `fileFormat` field
- Apache POI is an acceptable dependency for app-side Excel reading (`poi-ooxml`
  must be added explicitly)
- Blocked analyzers will receive real export files from the Madagascar site
  within 2-4 weeks; their GenericFile profiles cannot be created until real
  files arrive
- The old `/importAnalyzer` endpoint (which uses `AnalyzerReaderFactory`) is a
  legacy path that will NOT be modified
- The `plugins` submodule must be checked out to implement the GenericFile
  plugin code; this is a hard dependency for M3

### Constraints

- No implementation work starts until the FILE semantics design decision
  (Section 2) is reviewed and approved
- Wondfo CSV (OGC-344) must NOT be assigned to the ASTM lane — it belongs to the
  FILE stream
- GenericFile profiles for blocked instruments must not ship without real export
  files to validate against
- The old `FileAnalyzerReader` must remain backward-compatible for any existing
  file-import configurations
- QuantStudio and Wondfo MUST be implemented as GenericFile profiles, NOT as
  WAR-local Spring parsers in `src/main/java/.../analyzer/parsers/` — this would
  contradict the plugin-owned parser boundary mandated by the roadmap
