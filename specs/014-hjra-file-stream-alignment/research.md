# Research: File Stream Alignment

**Feature**: 014-hjra-file-stream-alignment  
**Date**: 2026-03-10

## R1: Parser Architecture — How to Support Both CSV and Excel

### Decision

Use a **format-dispatching reader pattern**: `FileImportService` inspects the
file format config on the analyzer profile and delegates to the appropriate
reader. Two readers exist in the app:

- `FileAnalyzerReader` (existing) — handles CSV/TSV via Apache Commons CSV
- New `ExcelAnalyzerReader` — handles .xls/.xlsx via Apache POI

Both readers normalize raw file bytes into structured records
(`List<Map<String, String>>`). The **analyzer-specific interpretation** of those
records belongs to the GenericFile plugin, not the app-side reader.

### Rationale

- `FileAnalyzerReader` already works for CSV. Rewriting it gains nothing.
- Excel workbooks have sheets, not lines. Tab-joining Excel rows is lossy and
  forces plugins to re-parse. Structured records are the natural normalized
  output.
- App-side readers normalize bytes by format; the GenericFile plugin owns the
  meaning of those records per-analyzer via profiles.
- The existing `insert(List<String> lines, ...)` interface on
  `AnalyzerLineInserter` stays untouched.

### Alternatives Considered

- **Single reader for all formats**: Rejected. CSV and Excel parsing are
  fundamentally different libraries (Commons CSV vs. POI). A single reader would
  be a god class.
- **Plugins handle raw InputStream**: Rejected. This pushes format-specific
  parsing into every plugin, duplicating CSV/Excel logic across all future file
  analyzers.
- **Extend AnalyzerXLSLineReader** (existing legacy): Rejected. It's coupled to
  the legacy `AnalyzerReaderFactory` path and doesn't use
  `FileImportConfiguration`.

## R2: GenericFile Plugin — Plugin JAR, Not WAR-Local Parsers

### Decision

QuantStudio and Wondfo CSV are implemented as **profiles for a single
`GenericFile` plugin JAR** in `plugins/analyzers/GenericFile/`, following the
same pattern as `GenericASTM` and `GenericHL7`. They are NOT implemented as
hardcoded Spring components inside the main WAR.

### Rationale

The roadmap explicitly requires this: _"Build toward a GenericFile plugin as a
peer to GenericASTM and GenericHL7"_ and _"File lane: GenericFile + profiles."_
The analyzer matrix lists all file instruments as "No GenericFile yet" — not "no
QuantStudio parser yet."

The GenericFile plugin:

- Implements `AnalyzerImporterPlugin` exactly as GenericASTM and GenericHL7 do
- Registers with `PluginAnalyzerService` at startup via the standard
  `connect()` + `registerAnalyzer()` path
- Loads its per-analyzer behavior from a **profile JSON** in
  `projects/analyzer-profiles/file/` (e.g. `quantstudio.json`,
  `wondfo-csv.json`)
- Uses `AnalyzerPluginConfig` (per-analyzer JSONB config keyed by `analyzer_id`)
  to store profile defaults applied on setup

QuantStudio and Wondfo are the **first two validation profiles**, not a separate
architectural path. All subsequent file analyzers (Tecan, Multiskan, etc.) will
also be profiles once real export files arrive.

### Plugin Submodule Dependency

`plugins/analyzers/GenericFile/` lives in the `plugins` submodule
(`-c5e24819...` as of this branch). The submodule is not checked out in this
worktree. **Implementation of GenericFile plugin code requires coordinating with
or checking out the plugins submodule.** This is a hard dependency for M3.

### Alternatives Considered

- **WAR-local Spring parsers (`src/main/java/.../analyzer/parsers/`)**:
  Rejected. This contradicts the roadmap's plugin-owned parser boundary. It
  creates a special-case code path outside the established plugin model and
  makes future file analyzers diverge from the same pattern.
- **External JARs from day one but one per analyzer**: Rejected. That still
  bypasses GenericFile. Per-instrument JARs are what legacy analyzers do; this
  lane builds a generic approach.
- **Skip the plugin interface entirely**: Rejected. Would create a special-case
  file-import path that diverges from the established plugin model, making the
  system harder to maintain.

## R3: Legacy Upload Endpoint Coexistence

### Decision

The new OGC-324 upload endpoint handles file-import protocol analyzers only. The
legacy `/importAnalyzer` endpoint continues to serve existing ASTM/HL7 file
drops unchanged.

### Rationale

- The legacy endpoint uses `AnalyzerReaderFactory` which has its own reader
  selection logic. Modifying it risks breaking existing workflows.
- The new endpoint is purpose-built for the `FileImportConfiguration` +
  format-dispatching reader + GenericFile plugin path.
- Users are not affected because the two endpoints serve different analyzer
  types.

### Alternatives Considered

- **Replace legacy entirely**: Rejected. Scope creep; legacy upload has existing
  users.
- **Route both through new endpoint**: Rejected. Would require migrating all
  legacy readers into the new framework.

## R4: FileFormat Config — Where It Lives

### Decision

Add a `fileFormat` field to `FileImportConfiguration` (not to the `Analyzer`
entity directly). Values: `CSV`, `TSV`, `EXCEL`. This field drives reader
selection in `FileImportService`.

### Rationale

- `FileImportConfiguration` already holds all file-import-specific transport
  settings (directory, pattern, delimiter, column mappings). File format belongs
  with them.
- The `Analyzer` entity is shared across ASTM/HL7/FILE. Adding file-specific
  fields to it violates separation of concerns.
- The file format config is only relevant when `FileImportConfiguration` exists
  — they have the same lifecycle.

### Alternatives Considered

- **Field on Analyzer entity**: Rejected. Pollutes the shared entity with
  file-specific concerns.
- **Infer format from file extension**: Rejected. Unreliable (some instruments
  produce .csv files that are actually TSV, or .xls that are really HTML).

## R5: Apache POI Dependency

### Decision

Use Apache POI for Excel parsing in the app-side `ExcelAnalyzerReader`. `poi`
5.4.0 is already a direct dependency in `pom.xml`. `poi-ooxml` (needed for
.xlsx/XSSF) must be added explicitly.

### Rationale

- POI handles both .xls (HSSF) and .xlsx (XSSF/SAX) formats.
- `poi` is already declared; `poi-ooxml` is a sibling artifact at the same
  version.
- Well-established library for Java Excel handling.

### Action Required

Add `poi-ooxml` to `pom.xml` during M3 (QuantStudio needs .xlsx support). Match
the existing `poi` version (5.4.0).

## R6: GenericFile Mirrors GenericASTM/GenericHL7

### Decision

`GenericFile` follows the established generic plugin pattern exactly. It is a
first-class peer to `GenericASTM` and `GenericHL7`, not a special case.

### How GenericASTM/GenericHL7 Work (Reference Baseline)

The existing generic plugins follow this pattern:

1. **Plugin JAR** in `plugins/analyzers/GenericASTM/` (or GenericHL7/)
2. **Plugin class** implements `AnalyzerImporterPlugin` with:
   - `isGenericPlugin()` → `true`
   - `isTargetAnalyzer(List<String> lines)` → identifier-based matching (ASTM
     H-segment, HL7 MSH-3)
   - `getAnalyzerLineInserter()` → returns a mapping-aware inserter that reads
     per-analyzer config
3. **`plugin.xml` descriptor** inside the JAR — declares the extension point and
   plugin class path
4. **`connect()` method** calls
   `PluginAnalyzerService.getInstance().registerAnalyzer(this)` at startup
5. **`PluginRegistryService`** auto-discovers the loaded plugin and creates an
   `AnalyzerType` row if missing
6. **Profiles** in `projects/analyzer-profiles/{astm,hl7}/` — JSON files with
   `default_test_mappings`, `configDefaults`, `identifier_pattern`, etc.
7. **`AnalyzerPluginConfig`** table — per-analyzer-instance JSONB config applied
   when admin selects a profile

### How GenericFile Maps to This Pattern

| Aspect                 | GenericASTM/GenericHL7                                          | GenericFile                                                                                        |
| ---------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| Plugin JAR location    | `plugins/analyzers/GenericASTM/`                                | `plugins/analyzers/GenericFile/`                                                                   |
| `isGenericPlugin()`    | `true`                                                          | `true`                                                                                             |
| `isTargetAnalyzer()`   | ASTM H-segment / HL7 MSH-3 pattern                              | Config-driven via `FileImportConfiguration.analyzerId`; content pattern fallback                   |
| Identifier matching    | `identifier_pattern` from profile                               | File pattern / format from `FileImportConfiguration`                                               |
| Line inserter          | `MappingAwareAnalyzerLineInserter`                              | Profile-aware file inserter (reads column mappings from profile + `AnalyzerPluginConfig`)          |
| Profile location       | `projects/analyzer-profiles/astm/`                              | `projects/analyzer-profiles/file/`                                                                 |
| Profile schema         | `identifier_pattern`, `default_test_mappings`, `configDefaults` | Same, plus `file_format`, `column_mapping`, `supported_extensions`, `comparison_operator_handling` |
| `AnalyzerPluginConfig` | Stores applied profile defaults                                 | Same — stores column mappings, format, sheet name, etc.                                            |

### Key Differences from ASTM/HL7

- **No real-time transport**: GenericFile is triggered by file watcher or upload
  endpoint, not by serial/TCP connection. The `getAnalyzerResponder()` returns
  null.
- **Format dispatch in app layer**: Unlike ASTM/HL7 where the reader handles
  protocol parsing, the app-side `FileImportService` dispatches to
  `FileAnalyzerReader` or `ExcelAnalyzerReader` based on `fileFormat`, then
  hands structured records to GenericFile.
- **`isTargetAnalyzer()` behavior**: For file imports,
  `FileImportConfiguration.analyzerId` deterministically identifies the plugin —
  `isTargetAnalyzer()` is a fallback for cases without a config match.

### Handoff Contract: App Service → GenericFile Plugin

The app-side readers produce `List<Map<String, String>>` (structured records).
The `AnalyzerImporterPlugin` interface expects
`AnalyzerLineInserter.insert(List<String> lines, String systemUserId)`. These
two shapes must be bridged.

**Resolution**: `FileImportServiceImpl` converts the structured records into
tab-joined `List<String>` before calling the plugin's
`AnalyzerLineInserter.insert()`. This preserves backward compatibility with the
existing plugin interface. The `GenericFileLineInserter` then reads the
per-analyzer column mapping from `AnalyzerPluginConfig` to interpret the
tab-joined fields. This is the same approach `FileAnalyzerReader.readStream()`
already uses today — it builds tab-joined lines from CSV records and feeds them
to the inserter.

For Excel specifically, the service builds tab-joined lines from the
`ExcelAnalyzerReader` output before handing off. The column order in the
tab-joined line follows a deterministic sequence derived from the profile's
`column_mapping` keys.

This means:

- The `AnalyzerImporterPlugin` interface is unchanged
- The GenericFile inserter receives lines in the same format as any other
  inserter
- Profile-driven column mapping in the inserter maps positional fields to
  `AnalyzerResults`
- No special-case code path is needed for file vs ASTM/HL7 at the plugin
  interface boundary

### Required App-Side Changes to Support GenericFile

- `PluginRegistryService`: Add `GENERIC_FILE_CLASS` constant and
  `getDefaultIdentifierPattern()` branch for FILE protocol.
- `PluginRegistryService.detectProtocol()`: Ensure `GenericFileAnalyzer`
  resolves to `"FILE"` protocol in `AnalyzerType`.
- `FileImportServiceImpl`: After format-dispatching to the appropriate reader,
  convert structured records to tab-joined lines, resolve the GenericFile plugin
  for the configured analyzer via
  `PluginAnalyzerService.getPluginByAnalyzerId()`, and call its inserter.
