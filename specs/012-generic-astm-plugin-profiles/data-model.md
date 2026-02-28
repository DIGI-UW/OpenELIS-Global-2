# Data Model: Generic ASTM Plugin Profiles v1.2 (Simplified)

## Design Principles

1. Keep `Analyzer` as the runtime instance aggregate root.
2. Keep plugin-type baseline mappings in `analyzer_test_map`.
3. Store v1.2 per-instance behavior in one protocol-agnostic JSONB config table.
4. Keep pending unmapped-code lifecycle in a dedicated queue table.
5. Keep analyzer-type profile configuration in filesystem JSON templates; no DB profile library in MVP.

## Prerequisite Migration

### `009-decouple-test-mappings.xml` (mandatory before v1.2 tables)

`develop` currently keys `analyzer_test_map` by `(analyzer_id, analyzer_test_name)`.  
v1.2 foundations require `(analyzer_type_id, analyzer_test_name)` to map by plugin type.

Migration responsibilities:
- add `analyzer_type_id` to `analyzer_test_map`
- backfill from `analyzer.analyzer_type_id`
- deduplicate collisions
- replace old PK/FK with analyzer-type-based key
- remove stale phantom analyzer rows created by legacy connect paths

This migration is a hard dependency for profile-default mapping behavior.

## Existing Entities Retained

- **Analyzer**: existing instance entity with connection/runtime fields.
- **AnalyzerTestMapping** (`analyzer_test_map`): existing type-level analyzer test code -> OpenELIS test mapping.
- **AnalyzerField** and **AnalyzerFieldMapping**: existing per-analyzer field discovery/mapping artifacts.
- **AnalyzerError**: existing error/failure records (separate from pending-code queue).

## New Entities

## `AnalyzerPluginConfig` (table: `analyzer_plugin_config`)

One row per analyzer instance. Holds protocol-agnostic v1.2 config in JSONB.

| Field | Type | Notes |
|---|---|---|
| `analyzer_id` | `NUMERIC` | PK, FK -> `analyzer.id` |
| `config` | `JSONB` | NOT NULL, default `{}` |
| `sys_user_id` | `INT` | audit |
| `last_updated` | `TIMESTAMP` | audit |

### `config` JSONB structure

```json
{
  "connectionRole": "SERVER",
  "serverListenPort": 9100,
  "clientTargetIp": null,
  "clientTargetPort": null,
  "aggregationMode": "PER_MESSAGE",
  "aggregationWindowSeconds": null,
  "qcRules": [
    {
      "id": "uuid",
      "ruleType": "SPECIMEN_ID_PREFIX",
      "targetField": "SPECIMEN_ID_FIELD",
      "operand": "QC",
      "isActive": true,
      "sortOrder": 1
    }
  ],
  "extractionOverrides": {
    "SPECIMEN_ID_FIELD": { "fieldIndex": 3, "componentIndex": null },
    "TEST_ID_COMPONENT": { "fieldIndex": 3, "componentIndex": 4 }
  },
  "flagMappings": {
    "H": "HIGH",
    "L": "LOW",
    "N": "NORMAL"
  },
  "transforms": {
    "MTB-RIF": { "type": "PASS_THROUGH" }
  }
}
```

## `AnalyzerPendingCode` (table: `analyzer_pending_code`)

Observed unmapped analyzer codes queue with lifecycle independent from config.

| Field | Type | Notes |
|---|---|---|
| `id` | `VARCHAR(36)` | PK |
| `analyzer_id` | `NUMERIC` | FK -> `analyzer.id` |
| `analyzer_test_name` | `VARCHAR(120)` | Unmapped analyzer code |
| `first_seen_at` | `TIMESTAMP` | first observation |
| `last_seen_at` | `TIMESTAMP` | last observation |
| `seen_count` | `INTEGER` | occurrence count |
| `sample_payload` | `TEXT` | optional truncated evidence |
| `status` | `VARCHAR(20)` | `PENDING`, `RESOLVED`, `IGNORED` |
| `sys_user_id` | `INT` | audit |
| `last_updated` | `TIMESTAMP` | audit |

Rules:
- max active pending entries: 100 per analyzer
- purge pending entries older than 30 days

## Profile JSON Schema (MVP)

Profiles are filesystem templates in `projects/analyzer-profiles/{astm,hl7}/`.

### Required v1.2 metadata

```json
{
  "profileMeta": {
    "id": "genexpert-cepheid-astm",
    "version": "1.2.0",
    "displayName": "Cepheid GeneXpert (ASTM)"
  }
}
```

### Optional instance defaults

```json
{
  "configDefaults": {
    "connectionRole": "SERVER",
    "aggregationMode": "PER_MESSAGE",
    "qcRules": [],
    "extractionOverrides": {},
    "flagMappings": {},
    "transforms": {}
  }
}
```

Profiles without `configDefaults` remain valid and produce empty `{}` plugin config at apply time.

## Type-Level vs Instance-Level Ownership

### Stays in profile JSON (type-level)

- `profileMeta`
- analyzer identity/type descriptors (`analyzer_name`, `manufacturer`, `category`)
- protocol and transport descriptors
- notes and static profile documentation

### Applied to DB on profile selection (instance defaults)

| Profile JSON field | DB destination |
|---|---|
| `identifier_pattern` | `analyzer.identifier_pattern` |
| `protocol.name` | `analyzer.protocol_version` |
| `default_test_mappings[].analyzer_code/loinc` | `analyzer_test_map` rows |
| `configDefaults.*` | `analyzer_plugin_config.config` |

After create/save, DB state is authoritative for the instance.

## Validation and State Rules

1. **Activation gate**: cannot transition analyzer to `ACTIVE` without at least one active QC rule.
2. **Role validation**:
   - `SERVER` requires `serverListenPort`
   - `CLIENT` requires `clientTargetIp` + `clientTargetPort`
3. **Aggregation validation**:
   - `BY_SESSION` requires `aggregationWindowSeconds` in `[5, 300]`
4. **Pending-code lifecycle**:
   - cap and purge constraints are enforced per analyzer

## Migration Notes

1. Apply 009 migration before any 010/011 changesets.
2. Add new changesets:
   - `010-create-analyzer-plugin-config.xml`
   - `011-create-analyzer-pending-code.xml`
3. No DB profile bootstrap is needed for MVP; built-in profiles are read from `projects/analyzer-profiles/`.
4. DB-backed profile library (`analyzer_profile*`) and lab-unit model are deferred.
