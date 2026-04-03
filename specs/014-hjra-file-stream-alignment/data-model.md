# Data Model: File Stream Alignment

**Feature**: 014-hjra-file-stream-alignment  
**Date**: 2026-03-10

## Entity Changes

### FileImportConfiguration (MODIFY)

Existing entity at
`org.openelisglobal.analyzer.valueholder.FileImportConfiguration`.

**New field:**

| Field        | Type        | Nullable | Default | Description                                                                   |
| ------------ | ----------- | -------- | ------- | ----------------------------------------------------------------------------- |
| `fileFormat` | VARCHAR(20) | NO       | `CSV`   | Expected file format. Values: `CSV`, `TSV`, `EXCEL`. Drives reader selection. |

**Existing fields (unchanged):**

- `id` (PK), `analyzerId` (FK → Analyzer), `importDirectory`, `filePattern`,
  `archiveDirectory`, `errorDirectory`, `columnMappingsJson` (JSONB),
  `delimiter`, `hasHeader`, `active`, `fhirUuid`

**Liquibase changeset:**
`3.4.14.x/001-add-file-format-to-file-import-config.xml` — adds `file_format`
column with default `'CSV'`, NOT NULL after backfill.

### AnalyzerFileUpload (NEW — from OGC-324)

Audit record per file upload/import event. Defined by OGC-324 spec.

| Field            | Type                      | Nullable | Description                                    |
| ---------------- | ------------------------- | -------- | ---------------------------------------------- |
| `id`             | BIGINT (PK)               | NO       | Auto-generated                                 |
| `analyzerId`     | INTEGER (FK → Analyzer)   | NO       | Which analyzer processed this file             |
| `filename`       | VARCHAR(255)              | NO       | Original filename                              |
| `fileHashSha256` | VARCHAR(64)               | NO       | SHA-256 hash for duplicate detection           |
| `fileSize`       | BIGINT                    | YES      | File size in bytes                             |
| `status`         | VARCHAR(20)               | NO       | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `resultCount`    | INTEGER                   | YES      | Number of results parsed                       |
| `errorMessage`   | TEXT                      | YES      | Error detail if FAILED                         |
| `uploadedBy`     | INTEGER (FK → SystemUser) | NO       | User who uploaded/triggered import             |
| `createdAt`      | TIMESTAMP                 | NO       | When file was received                         |
| `completedAt`    | TIMESTAMP                 | YES      | When processing finished                       |

**State transitions:**

```
PENDING → PROCESSING → COMPLETED
                    → FAILED
```

### AnalyzerRun (NEW — created by OGC-324)

New entity created in M2. OGC-324 defines this table to support preview
rendering. Full field list is deferred to M2 implementation; the OGC-324 Jira
description is the source of truth. Key fields shown here:

| Field               | Type    | Description                                     |
| ------------------- | ------- | ----------------------------------------------- |
| `customPreviewData` | JSONB   | Plugin-specific preview data for rich rendering |
| `pluginId`          | VARCHAR | Which plugin processed this run                 |

### ProtocolVersion enum (NO CHANGE)

Stays as-is: `ASTM_LIS2_A2`, `HL7_V2_3_1`, `HL7_V2_5`. File formats are NOT
added here. File-import analyzers use `FileImportConfiguration.fileFormat`
instead. See spec.md § FILE Semantics for rationale.

### constants.js (MODIFY)

Remove `FILE: "ASTM_LIS2_A2"` from `PLUGIN_PROTOCOL_DEFAULTS`. File-import
analyzers don't set a `ProtocolVersion`; the protocol version dropdown is hidden
when a file-import plugin type is selected.

## Relationships

```
Analyzer (1) ──── (0..1) FileImportConfiguration
    │                         │
    │                         └── fileFormat (CSV | TSV | EXCEL)
    │
    └──── (0..*) AnalyzerFileUpload (audit trail)
    │
    └──── (0..*) AnalyzerRun (import batches)
```

### AnalyzerPluginConfig (USE EXISTING — profile config storage)

Existing entity at
`org.openelisglobal.analyzer.valueholder.AnalyzerPluginConfig`.

This table is the mechanism by which the GenericFile plugin's column mapping and
format config is stored per-analyzer-instance. When an admin applies a file
profile (e.g. `quantstudio.json`), the profile's `configDefaults` are written to
`analyzer_plugin_config.config` (JSONB) for that analyzer's ID.

**Used by**: The GenericFile plugin reads `AnalyzerPluginConfig` at import time
to load column mappings, file format, comparison operator handling, and test
mappings.

**No schema changes required** — existing `analyzer_id` (PK/FK) + `config JSONB`
structure is sufficient.

### Profile JSON Files (NOT entities — filesystem assets)

New files in `projects/analyzer-profiles/file/`:

| File               | Purpose                                                                              |
| ------------------ | ------------------------------------------------------------------------------------ |
| `quantstudio.json` | QuantStudio QS5/QS7 profile — column mappings, EXCEL format, HIV-VL test mappings    |
| `wondfo-csv.json`  | Wondfo Finecare FS-205 profile — 40-column CSV, comparison operators, assay mappings |

Profile schema (extends the existing `astm`/`hl7` profile schema):

```json
{
  "$schema": "https://openelis-global.org/schemas/analyzer-defaults/1.0",
  "profileMeta": { "id": "quantstudio", "version": "1.0.0", "displayName": "QuantStudio 5/7 Flex" },
  "protocol": { "name": "FILE", "format": "EXCEL" },
  "supported_extensions": [".xls", ".xlsx"],
  "column_mapping": { "Sample Name": "sampleId", "CT": "result", ... },
  "comparison_operator_handling": false,
  "default_test_mappings": [ { "analyzer_code": "HIV-VL", "loinc": "20447-9", ... } ],
  "configDefaults": { "fileFormat": "EXCEL", "hasHeader": true, "sheetIndex": 0 }
}
```

## Validation Rules

- `fileFormat` MUST be one of: `CSV`, `TSV`, `EXCEL`
- `fileFormat` MUST match the profile's `protocol.format` field when a profile
  is applied
- `importDirectory` paths MUST be within the configured base import directory
  (defense-in-depth, already enforced in current code)
- `filePattern` MUST be a valid glob pattern
- `fileHashSha256` computed at upload time, stored for duplicate detection
- `AnalyzerPluginConfig.config` MUST be valid JSON (existing `@PrePersist` guard
  in place)
