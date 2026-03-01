# Data Model: Generic ASTM Plugin Profiles v1.2

## Design Principles

1. Keep `Analyzer` as the runtime instance aggregate root.
2. Keep plugin-type baseline mappings (`analyzer_test_map`) intact from 011.
3. Add v1.2 configuration entities for analyzer-instance behavior.
4. Keep profile lineage/version semantics explicit: `profile_meta_id` +
   `profile_meta_version`.

## Entities

## `AnalyzerProfile`

Portable profile artifact (Built-in or Site Library entry).

| Field                  | Type           | Notes                            |
| ---------------------- | -------------- | -------------------------------- |
| `id`                   | `VARCHAR(36)`  | PK                               |
| `profile_meta_id`      | `VARCHAR(120)` | Stable lineage/family ID         |
| `profile_meta_version` | `VARCHAR(40)`  | SemVer-like artifact version     |
| `display_name`         | `VARCHAR(255)` | Human-readable name              |
| `source`               | `VARCHAR(20)`  | `BUILT_IN`, `SITE`, `COMMUNITY`  |
| `compat_min_version`   | `VARCHAR(40)`  | Minimum OpenELIS compatibility   |
| `compat_max_version`   | `VARCHAR(40)`  | Maximum OpenELIS compatibility   |
| `is_latest`            | `BOOLEAN`      | Designated latest within lineage |
| `is_mutable`           | `BOOLEAN`      | False for built-ins              |
| `profile_json`         | `JSONB`        | Canonical profile payload        |
| `checksum_sha256`      | `VARCHAR(64)`  | Integrity + duplicate detection  |
| `created_by`           | `VARCHAR(36)`  | Audit                            |
| `created_at`           | `TIMESTAMP`    | Audit                            |
| `updated_by`           | `VARCHAR(36)`  | Audit                            |
| `updated_at`           | `TIMESTAMP`    | Audit                            |

**Constraints**

- Unique: (`profile_meta_id`, `profile_meta_version`)
- One designated latest per lineage (`profile_meta_id`, `is_latest=true`)

## `AnalyzerProfileApplication` (provenance)

Snapshot provenance when profile is applied to analyzer.

| Field                    | Type           | Notes                       |
| ------------------------ | -------------- | --------------------------- |
| `id`                     | `VARCHAR(36)`  | PK                          |
| `analyzer_id`            | `NUMERIC`      | FK -> `analyzer.id`         |
| `source_profile_id`      | `VARCHAR(36)`  | FK -> `analyzer_profile.id` |
| `source_profile_meta_id` | `VARCHAR(120)` | Denormalized lineage        |
| `source_profile_version` | `VARCHAR(40)`  | Denormalized version        |
| `applied_at`             | `TIMESTAMP`    | Application timestamp       |
| `applied_by`             | `VARCHAR(36)`  | User                        |

## `AstmAnalyzerConfig`

Analyzer runtime ASTM configuration that is not plugin-type global.

| Field                        | Type          | Notes                                      |
| ---------------------------- | ------------- | ------------------------------------------ |
| `id`                         | `VARCHAR(36)` | PK                                         |
| `analyzer_id`                | `NUMERIC`     | Unique FK -> `analyzer.id`                 |
| `connection_role`            | `VARCHAR(10)` | `SERVER` or `CLIENT`                       |
| `server_listen_port`         | `INTEGER`     | Used when role is `SERVER`                 |
| `client_target_ip`           | `VARCHAR(64)` | Used when role is `CLIENT`                 |
| `client_target_port`         | `INTEGER`     | Used when role is `CLIENT`                 |
| `aggregation_mode`           | `VARCHAR(20)` | `PER_MESSAGE`, `BY_SPECIMEN`, `BY_SESSION` |
| `aggregation_window_seconds` | `INTEGER`     | Required for `BY_SESSION`                  |
| `created_by`/`created_at`    | audit         |                                            |
| `updated_by`/`updated_at`    | audit         |                                            |

## `AstmFieldExtractionConfig`

Per-analyzer field reference overrides.

| Field             | Type          | Notes                                         |
| ----------------- | ------------- | --------------------------------------------- |
| `id`              | `VARCHAR(36)` | PK                                            |
| `analyzer_id`     | `NUMERIC`     | FK                                            |
| `key`             | `VARCHAR(60)` | e.g. `SPECIMEN_ID_FIELD`, `TEST_ID_COMPONENT` |
| `field_index`     | `INTEGER`     | ASTM 1-indexed field                          |
| `component_index` | `INTEGER`     | ASTM 1-indexed component, nullable            |
| `is_default`      | `BOOLEAN`     | True when unchanged from baseline             |

**Constraints**

- Unique: (`analyzer_id`, `key`)
- Validation: `field_index >= 1`,
  `component_index IS NULL OR component_index >= 1`

**Standard Keys and ASTM Defaults** (FR-017):

| Key                      | Default Record | Default field_index | Default component_index | Description                                     |
| ------------------------ | -------------- | ------------------- | ----------------------- | ----------------------------------------------- |
| `SPECIMEN_ID_FIELD`      | O (Order)      | 3                   | NULL                    | Specimen/sample ID                              |
| `TEST_ID_FIELD`          | R (Result)     | 3                   | NULL                    | Universal Test ID field                         |
| `TEST_ID_COMPONENT`      | R (Result)     | 3                   | 4                       | Component within Test ID (typically assay code) |
| `RESULT_VALUE_FIELD`     | R (Result)     | 4                   | NULL                    | Measurement/observation value                   |
| `RESULT_UNITS_FIELD`     | R (Result)     | 5                   | NULL                    | Units of measurement                            |
| `ABNORMAL_FLAG_FIELD`    | R (Result)     | 7                   | NULL                    | Abnormal/flag indicator                         |
| `RESULT_STATUS_FIELD`    | R (Result)     | 9                   | NULL                    | Result status code                              |
| `RESULT_TIMESTAMP_FIELD` | R (Result)     | 13                  | NULL                    | Date/time of observation                        |
| `SENDER_FIELD`           | H (Header)     | 5                   | NULL                    | Sender/instrument identification                |

## `AstmQcRule`

QC sample identification rules.

| Field          | Type           | Notes                                                                         |
| -------------- | -------------- | ----------------------------------------------------------------------------- |
| `id`           | `VARCHAR(36)`  | PK                                                                            |
| `analyzer_id`  | `NUMERIC`      | FK                                                                            |
| `rule_type`    | `VARCHAR(40)`  | `FIELD_EQUALS`, `SPECIMEN_ID_PREFIX`, `SPECIMEN_ID_PATTERN`, `FIELD_CONTAINS` |
| `target_field` | `VARCHAR(60)`  | Which ASTM-extracted field to evaluate                                        |
| `operand`      | `VARCHAR(255)` | Prefix/regex/value                                                            |
| `is_active`    | `BOOLEAN`      |                                                                               |
| `sort_order`   | `INTEGER`      | UI/order stability                                                            |

## `AstmTestMappingConfig` (table: `astm_test_mapping_config`)

Analyzer-instance transform overlay by analyzer code. Supplements the
plugin-type-level `analyzer_test_map`; does not replace it.

| Field                | Type           | Notes                                                                                  |
| -------------------- | -------------- | -------------------------------------------------------------------------------------- |
| `id`                 | `VARCHAR(36)`  | PK                                                                                     |
| `analyzer_id`        | `NUMERIC`      | FK                                                                                     |
| `analyzer_test_name` | `VARCHAR(120)` | Analyzer code                                                                          |
| `transform_type`     | `VARCHAR(40)`  | `PASS_THROUGH`, `GREATER_LESS_FLAG`, `VALUE_MAP`, `THRESHOLD_CLASSIFY`, `CODED_LOOKUP` |
| `transform_config`   | `JSONB`        | Type-specific validated payload                                                        |
| `is_active`          | `BOOLEAN`      |                                                                                        |

**Note**: This supplements, not replaces, plugin-type `analyzer_test_map`.

## `AstmFlagMapping`

Analyzer abnormal flag to OpenELIS interpretation mapping.

| Field           | Type          | Notes                     |
| --------------- | ------------- | ------------------------- |
| `id`            | `VARCHAR(36)` | PK                        |
| `analyzer_id`   | `NUMERIC`     | FK                        |
| `analyzer_flag` | `VARCHAR(30)` | Raw flag                  |
| `openelis_flag` | `VARCHAR(30)` | Interpreted code          |
| `is_custom`     | `BOOLEAN`     | Tracks user-defined flags |

## `AstmPendingCode`

Observed unmapped analyzer codes queue.

| Field                | Type           | Notes                            |
| -------------------- | -------------- | -------------------------------- |
| `id`                 | `VARCHAR(36)`  | PK                               |
| `analyzer_id`        | `NUMERIC`      | FK                               |
| `analyzer_test_name` | `VARCHAR(120)` | Unmapped code                    |
| `first_seen_at`      | `TIMESTAMP`    |                                  |
| `last_seen_at`       | `TIMESTAMP`    |                                  |
| `seen_count`         | `INTEGER`      |                                  |
| `sample_payload`     | `TEXT`         | Optional truncated evidence      |
| `status`             | `VARCHAR(20)`  | `PENDING`, `RESOLVED`, `IGNORED` |

**Rules**

- Max active pending entries: 100 per analyzer.
- Purge pending entries older than 30 days.

## `AnalyzerLabUnit`

Many-to-many analyzer assignment to lab units.

| Field         | Type      | Notes                                        |
| ------------- | --------- | -------------------------------------------- |
| `analyzer_id` | `NUMERIC` | FK -> `analyzer.id`                          |
| `lab_unit_id` | `VARCHAR` | References existing lab unit source of truth |

**Constraints**

- Unique composite PK: (`analyzer_id`, `lab_unit_id`)

## State and Validation Rules

1. **Activation Gate**: Analyzer cannot transition to `ACTIVE` without at least
   one active QC rule.
2. **Role Validation**:
   - `SERVER` requires `server_listen_port`.
   - `CLIENT` requires `client_target_ip` + `client_target_port`.
3. **Aggregation Validation**:
   - `BY_SESSION` requires `aggregation_window_seconds` in `[5, 300]`.
4. **Profile Import Validation**:
   - Reject duplicate (`profile_meta_id`, `profile_meta_version`).
   - Accept same lineage with new version.
   - Resolve designated latest by highest valid SemVer unless admin override.

## Migration Notes

1. Keep existing `analyzer.test_unit_ids` during transition; new junction table
   becomes source of truth for v1.2 UI/API.
2. No live rewrite of historical analyzers required; profile application
   provenance starts when v1.2 is used.
3. Backfill built-in profiles from `projects/analyzer-profiles/{astm,hl7}` at
   startup/bootstrap (renamed from `projects/analyzer-defaults/` per plan.md
   Profiles-as-Config-Files Alignment section).
