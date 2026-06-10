# Data Model: Unified Test Catalog Management Editor (OGC-949)

Authoritative schema target for **milestone M1** (OGC-747 schema migrations).
Every v1 section reads from these objects. Source: FRS v2.5 §0.6 (schema
migrations that land in v1), pinned at openelis-work `@f04cce54`. All DDL ships
as Liquibase changesets under `src/main/resources/liquibase/3.5.x.x/` (new
changesets `040+`, following `039-test-method-links.xml`). Column lists are the
design intent; the M1 stories (OGC-936..939) carry field-level acceptance
criteria.

Conventions: PostgreSQL; `clinlims` schema; audit columns (`lastupdated`,
`sys_user_id`) per OE convention; valueholders use JPA/Hibernate annotations
(Constitution IV). New externally-exposed entities carry `fhir_uuid` where they
surface via FHIR.

## v1 — New columns on existing tables

### `test.domain`

- Type: `VARCHAR(20) NOT NULL`, CHECK in (`CLINICAL`,`ENVIRONMENTAL`,`VECTOR`).
- Backfill: all existing rows → `CLINICAL`.
- Drives domain-conditional section visibility (FR-007).

### `test.is_amr_test`

- Type: `BOOLEAN NOT NULL DEFAULT FALSE`.
- Controls WHONET field reveal in Basic Info; toggling off retains
  `test_amr_config` (fix H-04).

### `test_sample_type.display_order`

- Type: `INTEGER`.
- Populated by deployment default (existing order / alphabetical); reordered by
  the Display Order section (M12).

### Deprecated-not-removed (retained one release cycle — FR-002)

- `test.result_type`, `test.unit_of_measure`, `test.significant_digits`,
  `test.default_result`. Values migrate into per-component scope (below) but
  remain readable for external consumers (analyzer interfaces, FHIR sync,
  reports) and as the data-level rollback (no feature flag — FR-D10). Removal is
  a future (v3) migration.

## v1 — New tables

### `test_result_component` _(the core multi-component object)_

One row per labeled value field within a test.

- `id` PK; `test_id` FK → `test(id)`; `code`; `label`; `display_order INTEGER`;
  `result_type`; `unit_of_measure_id` FK → `unit_of_measure(id)`;
  `significant_digits`; `default_result`; `allow_multiple_readings BOOLEAN`;
  `is_active BOOLEAN`.
- Uniqueness: `(test_id, code)`.
- **Migration**: every existing test gets one auto-created PRIMARY component with
  the legacy per-test fields copied in.
- **FHIR**: each component maps to its own `Observation` — never
  `Observation.component[]` (FR-D02 / fix D-02, locked).

### `component_id` FKs (added to three referencing tables)

- `test_range.component_id`, `test_interpretation.component_id`,
  `test_select_list_option.component_id` — each `INTEGER FK → test_result_component(id)`.
- **Backfill**: repoint every existing row to its test's auto-created PRIMARY
  component. Losslessness asserted by the M1 migration test (counts unchanged).

### `unit_of_measure` _(master list — no free text)_

- `id` PK; `code` UNIQUE; `display_name`; `ucum_code`; `description`;
  `is_active BOOLEAN`.
- Seeded with clinical/environmental/vector units (FRS v2.4). Inline-add from
  Sample & Results writes here (FR-011).
- _Note_: an existing `UnitOfMeasure` valueholder exists
  (`org.openelisglobal.unitofmeasure`); M1 reconciles/extends it to the master-list
  shape rather than creating a parallel entity (verify in M1).

### `test_amr_config`

- `id` PK; `test_id` FK; WHONET fields: `antibiotic_code`, `antibiotic_class`,
  `method`, `breakpoint`, `disk_potency`.
- Persists across AMR-flag toggle (fix H-04).

### `whonet_antibiotic_codes` _(seeded reference)_

- `code` PK/UNIQUE; `name`; `class`. Feeds the AMR antibiotic-code typeahead.

### `test_localization`

- `id` PK; `test_id` FK; `language_code`; `test_name`; `reporting_name`;
  `description`.
- Schema created in v1; the fallback function + UI indicators are v2 (M20).

### `test_sample_handling`

- `id` PK; `test_id` FK UNIQUE; `storage_condition`; `storage_duration`;
  `storage_unit`; `stability_notes`; six special-handling boolean flags;
  `disposal_method`; `disposal_timeframe`; `special_instructions`;
  `override_restricted BOOLEAN`; `version`; `created_at`; `updated_at`.
- In-progress orders keep their existing settings; new orders use the locked
  version (fix M-05).

### `test_sample_handling_history` _(created v1, inert until v2 — fix D-09)_

- `id` PK; `test_sample_handling_id` FK; `changed_by` FK; `changed_at`;
  `change_type` (`CREATE`/`UPDATE`); `previous_values JSONB`; `new_values JSONB`.
- No write triggers in v1; they light up in M19 (OGC-766).

### `panel_test` _(junction with position)_

- `panel_id` FK; `test_id` FK; `display_order INTEGER`.
- Replaces/extends the existing panel-test relationship (M1 reconciles with the
  current `TypeOfSamplePanel`/panel-item model — verify and migrate, don't
  duplicate).

### `test_section_assignment` _(multi-section junction)_

- `test_id` FK; `section_id` FK; `is_primary BOOLEAN`.
- Supports a test belonging to multiple sections.

### `test_activation_acknowledgment` _(audit log)_

- `id` PK; `test_id` FK; `user_id` FK; `acknowledged_at TIMESTAMP`;
  `gaps_acknowledged JSONB`.
- One row per activation-with-incomplete-coverage event (FR-012 / fix H-03).
  Re-presented if the gap pattern changes.

## Migration sequencing (within M1)

Mapped to the OGC-747 child stories:

1. **OGC-936** — Domain + AMR + WHONET reference data (`test.domain`,
   `test.is_amr_test`, `test_amr_config`, `whonet_antibiotic_codes`).
2. **OGC-937** — Result Components + `component_id` on the three referencing
   tables (the backfill; the losslessness test lives here).
3. **OGC-938** — Junction tables + Sample Storage + Unit of Measure seed
   (`panel_test`, `test_section_assignment`, `test_sample_handling`,
   `test_sample_type.display_order`, `unit_of_measure` master + seed).
4. **OGC-939** — Localization + Activation Acknowledgment schema
   (`test_localization`, `test_activation_acknowledgment`,
   `test_sample_handling_history` inert).
5. **OGC-940** — Legacy admin decommission + route redirects — **executes at
   M-DC**, not in M1 (see plan.md).

## v2 — Deferred objects (name-only stubs)

Elaborated at v2 kickoff; listed for dependency awareness only.

- `result_reading` (multi-reading capture per component) — M-side detail in the
  result-entry FRS.
- `test_alert_rule` (per-test alert authoring; channels CSV; no template columns;
  `acknowledgment_required BOOLEAN`) — M16 / FR-D03..D06.
- `test_reagent_link` (test↔reagent, usage type, qty) — M13 / FR-D07.
- Label-preset tables (`label_preset`, `label_preset_field`, `test_label_config`,
  `test_label_preset_link`) — **owned by OGC-285** (PR #3676), consumed by M14;
  not created here (R2).
- `get_localized_test_field()` PL/pgSQL fallback function — M20 / FR-D-localization.
- `test_sample_handling_history` write triggers — M19 (table already created in v1).
