# Data Model: Unified Test Catalog Management Editor (OGC-949)

**This document is the translation layer between the FRS and the repository.**
The FRS (v2.4/v2.5, pinned `@f04cce54`) describes an _idealized_ schema; this
file resolves every FRS concept to the **real** schema (canonical sources:
`src/main/resources/hibernate/hbm/*.hbm.xml`, JPA valueholders, existing
Liquibase changesets) and states what M1 actually creates vs. alters vs.
reuses. FRS names are kept as traceability aliases only — **never** as table
names in changesets.

> Revision note (2026-06-11): this file originally restated the FRS's
> aspirational table names (`test_range`, `test_interpretation`,
> `test_select_list_option`, new `unit_of_measure`/`panel_test`/
> `test_localization`). That was wrong — re-grounded against the hbm/liquibase
> sources. See research.md §R9.

## Conventions (grounded)

- Legacy core tables: `numeric(10)` ids via `*_seq` +
  `LIMSStringNumberUserType` (e.g. `TEST`, `RESULT_LIMITS`, `TEST_RESULT`,
  `PANEL`, `UNIT_OF_MEASURE`).
- New OGC-3.5.x tables: `VARCHAR(36)` UUID ids, `lastupdated` +
  `last_updated` columns (precedent: `test_method` 039, `test_amr_config` 040).
- FK columns referencing `TEST` must be `numeric(10)`.
- All DDL via Liquibase under `src/main/resources/liquibase/3.5.x.x/` (041+).

## FRS → repository translation table

| FRS concept (alias)                                                                                                              | Real counterpart                                                                                                                                                                                           | Mapping source                  | M1 action                                                                                                                                                                                         |
| -------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `test.domain`                                                                                                                    | `TEST.DOMAIN` (new)                                                                                                                                                                                        | Test.hbm.xml                    | ✅ **Done** (changeset 040, OGC-936)                                                                                                                                                              |
| AMR flag (`test.is_amr_test`)                                                                                                    | **`TEST.ANTIMICROBIAL_RESISTANCE` already exists** (`Test.antimicrobialResistance`, liquibase 2.8.x.x) — wired through TestService/validators/TestCatalogBean                                              | Test.hbm.xml                    | **REUSE — no new column.** The originally-added `is_amr_test` was a duplicate, removed (R11). M4 Basic Info toggles the existing field                                                            |
| `test_amr_config`, `whonet_antibiotic_codes`                                                                                     | same (new — per-test WHONET detail + code reference)                                                                                                                                                       | —                               | ✅ **Done** (changeset 040)                                                                                                                                                                       |
| `test_range`                                                                                                                     | **`RESULT_LIMITS`** (ResultLimit.hbm.xml) — already has `TEST_ID`, `gender`, `min_age`/`max_age` (Double, **years**), low/high normal + valid + reporting + **critical** columns, `always_validate`        | ResultLimit.hbm.xml             | **ALTER**: add `component_id` FK (nullable→backfilled). Hour-granular neonatal ranges = fractional-year Doubles today; Coverage Validation (M7) normalizes — no schema change for age units in M1 |
| `test_select_list_option`                                                                                                        | **`TEST_RESULT`** (TestResult.hbm.xml) — one row per selectable result option; `TST_RSLT_TYPE` ('D' = dictionary via `TEST_DICTIONARY`), `VALUE`, `SORT_ORDER`, `SIGNIFICANT_DIGITS`, `is_normal`, `FLAGS` | TestResult.hbm.xml              | **ALTER**: add `component_id` FK (nullable→backfilled)                                                                                                                                            |
| `test_interpretation`                                                                                                            | **No existing table.** Today: `TEST_RESULT.is_normal` boolean + DICTIONARY entries — no per-result interpretation text/severity                                                                            | (absent)                        | **CREATE** `test_result_interpretation` (genuinely new)                                                                                                                                           |
| `test_result_component`                                                                                                          | **No existing table** (the multi-component concept does not exist)                                                                                                                                         | (absent)                        | **CREATE** `test_result_component`; auto-create one PRIMARY per test                                                                                                                              |
| deprecated per-test result fields (`test.result_type`, `test.unit_of_measure`, `test.significant_digits`, `test.default_result`) | Reality differs from FRS: `TEST.UOM_ID` (FK to `UNIT_OF_MEASURE`) is the only per-test field; result type + significant digits live on `TEST_RESULT` rows; `TEST.default_test_result_id` exists            | Test.hbm.xml                    | PRIMARY component copies `TEST.UOM_ID` + the dominant `TEST_RESULT.TST_RSLT_TYPE`/`SIGNIFICANT_DIGITS`. Legacy columns untouched (deprecate-in-place per FR-002)                                  |
| `unit_of_measure` master list                                                                                                    | **`UNIT_OF_MEASURE` already exists** (numeric(10) id, `NAME` 20, `DESCRIPTION` 60; `TEST.UOM_ID` FK)                                                                                                       | UnitOfMeasure.hbm.xml           | **ALTER, not create**: add `code` (unique, nullable), `ucum_code`, `is_active` — FRS's "master list" = harden the existing table                                                                  |
| `panel_test` junction                                                                                                            | **`PANEL_ITEM` already exists** — `PANEL_ID`, `TEST_ID`, **`SORT_ORDER`** (the FRS's display position!), via panel_item_seq                                                                                | PanelItem.hbm.xml               | **REUSE — no new table.** M9 (Panels section) reads/writes `PANEL_ITEM`                                                                                                                           |
| `test_sample_type.display_order`                                                                                                 | Junction is **`SAMPLETYPE_TEST`** (TypeOfSampleTest.hbm.xml): `SAMPLE_TYPE_ID`, `TEST_ID` — **no order column today**                                                                                      | TypeOfSampleTest.hbm.xml        | **ALTER**: add `display_order INTEGER` to `SAMPLETYPE_TEST` (M12 consumer)                                                                                                                        |
| `test_localization`                                                                                                              | **Already covered**: `TEST.name_localization_id` + `TEST.reporting_name_localization_id` → `LOCALIZATION`/`LOCALIZATION_VALUE` (per-locale values, unique (localization_id, locale))                       | Localization.java, Test.hbm.xml | **REUSE — no new table.** Only gap: localized _description_ — defer to M20 (v2 Localization Hardening) as an additional `localization_id` FK if needed                                            |
| `test_section_assignment` (multi-section)                                                                                        | Today 1:1 via `TEST.TEST_SECTION_ID`; `TEST_SECTION` has hierarchy (`PARENT_TEST_SECTION`) but no junction                                                                                                 | TestSection.hbm.xml             | **DEFER decision to M2**: create junction only if the editor actually ships multi-section in v1; flag to Jira (OGC-938 scope) rather than speculatively migrate                                   |
| `test_sample_handling` (+ `_history`)                                                                                            | **No existing counterpart** — `STORAGE_*` tables are physical location hierarchy, not per-test handling requirements (verified)                                                                            | (absent)                        | **CREATE** both (history inert per D-09)                                                                                                                                                          |
| `test_activation_acknowledgment`                                                                                                 | **No existing counterpart**                                                                                                                                                                                | (absent)                        | **CREATE**                                                                                                                                                                                        |
| Terminology mappings                                                                                                             | Today: bare `TEST.LOINC` (VARCHAR 240); no SNOMED/multi-code table                                                                                                                                         | Test.hbm.xml                    | **CREATE** `test_terminology_mapping` (M10 consumer) — source/code/relationship per FRS §2.7                                                                                                      |

## New tables (M1 — the genuinely-new set)

All follow the 039/040 pattern: `VARCHAR(36)` UUID id, `lastupdated TIMESTAMP
DEFAULT now()`, `last_updated TIMESTAMP`, `is_active VARCHAR(2) DEFAULT 'Y'`
where applicable; FKs to `TEST(ID)` as `numeric(10)`.

### `test_result_component` (OGC-937)

- `id` VARCHAR(36) PK · `test_id` numeric(10) FK→TEST NOT NULL · `code`
  VARCHAR(50) NOT NULL · `label` VARCHAR(255) NOT NULL · `display_order`
  INTEGER NOT NULL DEFAULT 0 · `result_type` VARCHAR(1) (mirrors
  `TEST_RESULT.TST_RSLT_TYPE` codes) · `uom_id` numeric(10) FK→UNIT_OF_MEASURE
  (nullable) · `significant_digits` numeric(10) (nullable) · `default_result`
  VARCHAR(80) (nullable) · `allow_multiple_readings` BOOLEAN DEFAULT false ·
  `is_active`.
- Unique `(test_id, code)`.
- **Backfill**: one `PRIMARY` row per existing TEST: `code='PRIMARY'`,
  `label` = test name, `uom_id` = `TEST.UOM_ID`, `result_type` /
  `significant_digits` from the test's `TEST_RESULT` rows (single dominant
  type), `default_result` from `TEST.default_test_result_id`'s value when set.

### `component_id` FKs (OGC-937)

- `RESULT_LIMITS.component_id` VARCHAR(36) FK→test_result_component (nullable,
  then backfilled to the test's PRIMARY; left nullable in v1 — NOT NULL
  tightening deferred to v2 once all writers populate it).
- `TEST_RESULT.component_id` — same treatment.
- **Losslessness invariant (the M1 test)**: row counts of `TEST`,
  `RESULT_LIMITS`, `TEST_RESULT` unchanged pre/post; every `RESULT_LIMITS` and
  `TEST_RESULT` row has `component_id` = its test's PRIMARY component;
  exactly one PRIMARY component per test.

### `test_result_interpretation` (OGC-937; M5 consumer)

- `id` VARCHAR(36) PK · `component_id` VARCHAR(36) FK NOT NULL ·
  `value_match` VARCHAR(80) · `interpretation_text` VARCHAR(255) · `severity`
  VARCHAR(20) (NORMAL/ABNORMAL/CRITICAL) · `color` VARCHAR(20) ·
  `display_order` INTEGER · `is_active`.
- No backfill (no legacy counterpart; `TEST_RESULT.is_normal` stays
  authoritative for legacy flows until M5).

### `test_sample_handling` + `test_sample_handling_history` (OGC-938)

- Per FRS v2.4 "Sample Storage Tab" (grounded — **3** special-handling
  checkboxes, not 6): `test_id` numeric(10) FK UNIQUE · `storage_condition` ·
  `storage_condition_custom` · `storage_duration` + `storage_duration_unit` ·
  `stability_notes` · `protect_from_light` / `do_not_freeze` /
  `do_not_refrigerate` (booleans) · `disposal_method` · `disposal_timeframe` +
  `disposal_unit` · `special_instructions` · `override_restricted` BOOLEAN ·
  `version` INTEGER. (Shipped, changeset 042.)
- History table created **inert** (no triggers until M19/v2 — D-09):
  `test_sample_handling_id` FK · `changed_by` numeric(10) (system_user) ·
  `changed_at` · `change_type` · `previous_values JSONB` · `new_values JSONB`.

### `test_activation_acknowledgment` (OGC-939; M7 consumer)

- `id` VARCHAR(36) PK · `test_id` numeric(10) FK NOT NULL · `user_id`
  numeric(10) FK→system_user NOT NULL · `acknowledged_at TIMESTAMP` ·
  `gaps_acknowledged JSONB`.

### `test_terminology_mapping` (OGC-939 alt / M10 consumer)

- `id` VARCHAR(36) PK · `test_id` numeric(10) FK NOT NULL · `source`
  VARCHAR(20) (LOINC/SNOMED/CIEL/OCL) · `code` VARCHAR(80) · `relationship`
  VARCHAR(20) · `is_active`. Unique `(test_id, source, code)`.
- `TEST.LOINC` stays (deprecate-in-place); backfill one LOINC mapping row per
  test with non-null `TEST.LOINC`.

## Alterations to existing tables (M1)

| Table             | Change                                                                                            | Consumer               |
| ----------------- | ------------------------------------------------------------------------------------------------- | ---------------------- |
| `RESULT_LIMITS`   | + `component_id` VARCHAR(36) FK (backfilled)                                                      | M7 Ranges              |
| `TEST_RESULT`     | + `component_id` VARCHAR(36) FK (backfilled)                                                      | M5 Sample & Results    |
| `UNIT_OF_MEASURE` | + `code` VARCHAR(20) UNIQUE nullable, `ucum_code` VARCHAR(40), `is_active` VARCHAR(2) DEFAULT 'Y' | M5 inline-add (FR-011) |
| `SAMPLETYPE_TEST` | + `display_order` INTEGER (deployment-default backfill by current sort)                           | M12 Display Order      |
| `TEST`            | _(040 done)_ `DOMAIN` only — AMR reuses existing `ANTIMICROBIAL_RESISTANCE` (R11)                 | M4 Basic Info          |
| `RESULT_LIMITS`   | _(041 done)_ `component_id` shipped                                                               | M7 Ranges              |
| `TEST_RESULT`     | _(041 done)_ `component_id` shipped                                                               | M5 Sample & Results    |

## Explicitly reused (no new schema)

- **Panels** → `PANEL` + `PANEL_ITEM` (`SORT_ORDER` is the position editor's
  column).
- **Localization of test names** → `LOCALIZATION`/`LOCALIZATION_VALUE` via
  `TEST.name_localization_id` / `reporting_name_localization_id`.
- **Methods** → `test_method` (`039`, ported in M0). **Correction `044`
  (post-merge, R13)**: `039` created the FK columns `test_id`/`method_id` as
  `VARCHAR(36)`, violating the "FK columns referencing `TEST` must be
  `numeric(10)`" convention above; `044` retypes them to `numeric(10,0)` and adds
  the FKs (`test_id→TEST` CASCADE, `method_id→METHOD` RESTRICT). The PK
  `test_method.id` correctly stays `VARCHAR(36)` (UUID).
- **Analyzers (read-only)** → analyzer field-mapping tables (no change).

## Deferred / flagged decisions

- **Multi-section junction** (`test_section_assignment`): not created in M1;
  decide at M2 against actual editor scope. Flagged to Jira (OGC-938).
- **`component_id` NOT NULL tightening**: v2, after all writers populate it.
- **Localized description**: M20 (v2), reusing LOCALIZATION.
- **v2 objects** (name-only): `result_reading`, `test_alert_rule`,
  `test_reagent_link`; label-preset tables **owned by OGC-285** (PR #3676).

## Migration sequencing (M1 changesets)

1. ~~040 — domain + AMR + WHONET~~ ✅ shipped.
2. **041 — result components** (OGC-937): `test_result_component` + PRIMARY
   backfill; `component_id` on `RESULT_LIMITS` + `TEST_RESULT` + backfill;
   `test_result_interpretation`. _(Losslessness test lives here.)_
3. **042 — handling + uom + display order** (OGC-938): `test_sample_handling`
   (+ inert history); `UNIT_OF_MEASURE` hardening; `SAMPLETYPE_TEST.display_order`.
4. **043 — acknowledgment + terminology** (OGC-939): `test_activation_acknowledgment`;
   `test_terminology_mapping` + LOINC backfill.
5. OGC-940 (legacy decommission) → **M-DC**, not M1.
6. **`044` — post-merge remediation** (PR #3714, R13): `test_method`
   `test_id`/`method_id` → `numeric(10,0)` + FKs; unique-active index; drop dead
   `test_method_seq`. Additive to the merged `039` (immutable).
