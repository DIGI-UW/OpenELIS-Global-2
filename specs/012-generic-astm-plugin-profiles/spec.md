# Feature Specification: Generic ASTM Plugin v1.2 — Plugin Config Foundation

**Feature Branch**: `spec/012-ogc-337-generic-astm-plugin-profiles`  
**Created**: 2026-02-27  
**Status**: Draft  
**Jira**: OGC-337

**Extends**: `004-astm-analyzer-mapping` (v1.0 complete)  
**Relationship to 011**: Independent but complementary to
`011-madagascar-analyzer-integration`

## Executive Summary

This feature closes the remaining gaps in the generic ASTM plugin so OpenELIS
can replace per-instrument compiled parser logic with reusable configuration and
portable JSON profiles.

For MVP, profiles are filesystem-based templates in
`projects/analyzer-profiles/`. Runtime instance behavior is stored in the
database using a protocol-agnostic JSONB table (`analyzer_plugin_config`) plus a
pending-code queue table (`analyzer_pending_code`).

The unified v1.2 MVP scope includes:

- **Core configuration surface (FR-014 through FR-021)**: connection role, QC
  identification rules, value transforms, field extraction overrides, result
  aggregation mode, abnormal flag mapping, simulator preview, and passive
  auto-detect of unmapped codes.
- **Profile apply workflow (FR-022 through FR-024 simplified)**: file-based
  profile selection and snapshot-on-apply defaults.
- **Bidirectional GeneXpert ASTM (FR-026)**: all 4 pathways validated via mock +
  real device.

The following are explicitly deferred: DB-backed profile library/import-export
sharing and lab-unit assignment model.

---

## Clarifications

### Session 2026-02-27

- Q: Should the specification retain legacy version references? -> A: No. Use
  unified v1.2 terminology across this spec.
- Q: Should analyzers remain live-linked to profile updates after creation? ->
  A: No. Snapshot-on-apply with strict separation of instance-specific config
  from profile defaults.
- Q: Should this release include dedicated profile reapply for existing
  analyzers? -> A: No. Out of scope.

### Session 2026-02-28

- Q: Should profiles be stored in a DB library for MVP? -> A: No. Profiles are
  filesystem-based only for MVP.
- Q: Should ASTM config use normalized ASTM-specific tables? -> A: No. Use
  protocol-agnostic JSONB in `analyzer_plugin_config`.
- Q: What RBAC scope ships in MVP? -> A: `GLOBAL_ADMIN` for new endpoints;
  tiered LAB_USER/LAB_SUPERVISOR/LAB_ADMIN is deferred.
- Q: Is profile import/export sharing (US6) in MVP? -> A: No. Deferred entirely.
- Q: Directory naming? -> A: Use `projects/analyzer-profiles/` naming only; no
  legacy fallback.

---

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Configure an analyzer from profile or scratch (Priority: P1)

As a laboratory administrator, I want to add/edit an analyzer by selecting a
file-based profile or starting from scratch, so onboarding is fast and still
allows full site-level customization.

**Independent Test**: Create one analyzer from a built-in profile and one from
"None (Start from Scratch)", save both, and verify defaults/editability.

**Acceptance Scenarios**:

1. **Given** I open Add Analyzer, **When** I select a profile from built-in
   filesystem profiles, **Then** protocol and connection defaults pre-fill while
   remaining editable.
2. **Given** I select "None (Start from Scratch)", **When** I save, **Then**
   analyzer is created without profile-derived defaults.
3. **Given** a selected profile has `configDefaults`, **When** analyzer is
   created, **Then** defaults are copied to `analyzer_plugin_config` for that
   analyzer.

### User Story 2 - Complete core mapping configuration (Priority: P1)

As a laboratory administrator, I want to configure transforms, extraction
fields, aggregation mode, and flag mappings so one generic parser can correctly
interpret analyzer-specific messages.

**Independent Test**: Configure each core mapping section for a generic
analyzer, save, reload, and verify values are persisted and reflected in
simulator preview output.

**Acceptance Scenarios**:

1. **Given** analyzer plugin config is open, **When** I define extraction
   overrides and transform rules, **Then** save succeeds only for valid
   transform payloads.
2. **Given** aggregation mode is `BY_SESSION`, **When** window value is out of
   range, **Then** save is rejected with validation error.
3. **Given** abnormal flag mappings are configured, **When** simulator preview
   runs, **Then** preview output reflects mapped interpretation values.

### User Story 3 - Enforce QC identification before activation (Priority: P1)

As a laboratory supervisor, I want QC identification rules required before
activation so QC samples are reliably separated from patient samples.

**Independent Test**: Attempt analyzer status transition to `ACTIVE` first
without QC rules, then with at least one active valid QC rule.

**Acceptance Scenarios**:

1. **Given** analyzer is in `VALIDATION` and has no active QC rule, **When**
   activation is requested, **Then** transition is blocked with actionable
   error.
2. **Given** analyzer is in `VALIDATION` and has one active valid QC rule,
   **When** activation is requested, **Then** transition to `ACTIVE` succeeds.

### User Story 4 - Simulate and validate parsing safely (Priority: P2)

As a laboratory administrator, I want to paste raw ASTM payloads into simulator
preview so I can validate configuration before live traffic.

**Independent Test**: Submit simulator payload containing mapped and unmapped
values and verify preview shows parsed output without writing clinical/QC rows.

**Acceptance Scenarios**:

1. **Given** valid ASTM payload is submitted to preview, **When** parsing
   completes, **Then** response includes parsed fields, mapping application
   results, and plugin-config snapshot context.
2. **Given** simulator endpoint is used, **When** preview is run repeatedly,
   **Then** no analyzer result/QC persistence side effects occur.

### User Story 5 - Detect and resolve unmapped live test codes (Priority: P2)

As a laboratory administrator, I want passive auto-detect of unknown test codes
with quick mapping workflow so production drift is resolved without parser code
updates.

**Independent Test**: Feed repeated unknown analyzer codes, verify queue
behavior (create/increment/cap/purge), then resolve and ignore entries through
API actions.

**Acceptance Scenarios**:

1. **Given** an unmapped analyzer code is seen, **When** ingestion occurs,
   **Then** pending-code queue stores or increments entry for that analyzer/code
   pair.
2. **Given** queue size reaches cap, **When** additional unknown codes arrive,
   **Then** retention/cap policy is enforced deterministically.
3. **Given** pending code action is `MAP` or `IGNORE`, **When** API action is
   executed, **Then** status transitions and timestamps update accordingly.

### User Story 7 - Bidirectional ASTM communication with GeneXpert (Priority: P1)

As a laboratory administrator, I want OpenELIS to support bidirectional ASTM
communication with GeneXpert analyzers so orders can be sent and results can be
queried.

**Independent Test**: Execute all four pathways (results push, orders pull,
orders push, results pull) against mock analyzer and confirm real-device
verification evidence for release.

**Acceptance Scenarios**:

1. **Given** GeneXpert initiates ASTM communication, **When** OpenELIS receives
   results push payloads, **Then** messages are processed through configured
   mapping flow.
2. **Given** query/order operations are triggered, **When** bidirectional
   endpoints are exercised, **Then** all four pathways succeed with
   protocol-compliant exchange.
3. **Given** release readiness checks run, **When** pathway validations
   complete, **Then** mock and real-device evidence is captured for sign-off.

## Deferred User Stories

### [DEFERRED] User Story 6 - Share analyzer knowledge through profile import/export

Deferred to a future feature. MVP does not include DB-backed profile library,
import/export sharing, or community profile exchange.

---

## Requirements _(mandatory)_

### Functional Requirements

#### Core Configuration Surface (FR-014 to FR-021)

- **FR-014 Connection Role**: Support `SERVER` and `CLIENT` roles with
  conditional fields.
- **FR-015 QC Rules**: Support `FIELD_EQUALS`, `SPECIMEN_ID_PREFIX`,
  `SPECIMEN_ID_PATTERN`, `FIELD_CONTAINS`; OR semantics for rule evaluation.
- **FR-016 Value Transforms**: Support `PASS_THROUGH`, `GREATER_LESS_FLAG`,
  `VALUE_MAP`, `THRESHOLD_CLASSIFY`, `CODED_LOOKUP` with per-type validation.
- **FR-017 Field Extraction Config**: Support editable ASTM field/component
  references with sensible defaults.
- **FR-018 Aggregation Mode**: Support `PER_MESSAGE`, `BY_SPECIMEN`,
  `BY_SESSION`; session mode requires window.
- **FR-019 Abnormal Flag Mapping**: Editable analyzer-flag to OpenELIS
  interpretation mapping.
- **FR-020 Simulator**: Preview-only parse of raw ASTM with no persistence.
- **FR-021 Auto-Detect Codes**: Detect/store unmapped codes and expose API
  workflow to resolve or ignore pending entries.

#### Bidirectional ASTM Communication (FR-026)

- **FR-026**: Support 4 pathways for GeneXpert:
  - Results Push (Analyzer -> OpenELIS)
  - Orders Pull (Analyzer queries OpenELIS)
  - Orders Push (OpenELIS -> Analyzer)
  - Results Pull (OpenELIS queries Analyzer)
- MVP validation is service/API + harness driven; no dedicated bidirectional UI
  required.

#### Profile Apply Surface (FR-022 to FR-024, simplified)

- **FR-022 Profile Format**:
  - Profile JSON MUST include `profileMeta` (`id`, `version`, `displayName`).
  - Profile JSON MAY include `configDefaults` for instance-level defaults.
  - Profile files live in `projects/analyzer-profiles/{astm,hl7}/`.
- **FR-023 Profile Source (MVP)**:
  - MVP source is filesystem-only built-in profiles.
  - No DB profile library, no community import pipeline in MVP.
  - Built-in catalog includes 11 files (6 ASTM + 5 HL7) validated in repository.
  - ASTM filenames: `genexpert-astm`, `horiba-micros60`, `horiba-pentra60`,
    `mindray-ba88a`, `stago-start4`, `sysmex-xn`.
  - HL7 filenames: `abbott-architect`, `genexpert-hl7`, `mindray-bc2000`,
    `mindray-bc5380`, `mindray-bs360e`.
- **FR-024 Profile Selection**:
  - Add Analyzer MUST allow built-in profile selection and "None (Start from
    Scratch)".
  - Selecting profile pre-fills analyzer fields and applies `configDefaults`
    snapshot to DB.
  - No live inheritance and no reapply workflow in MVP.

#### Deferred Requirement

- **FR-025 Lab Unit Assignment**: Deferred to future feature.

### Business Rules

- **BR-11 Port Conflict Validation**: Active listeners must not conflict on
  host/port.
- **BR-12 QC Required for Active**: Activation requires at least one valid QC
  rule.
- **BR-13 Transform Validation**: Transform config must match selected type.
- **BR-14 Aggregation Window Range**: `BY_SESSION` window range is 5-300
  seconds.
- **BR-15 Simulator Preview-Only**: Simulator output never persists clinical/QC
  records.
- **BR-16 Pending Code Limits**: Queue cap 100 per analyzer; retention purge
  after 30 days.
- **BR-17 ASTM Indexing**: ASTM field/component references are 1-indexed.
- **BR-18 Profile Schema Validation**: Files must satisfy schema and required
  v1.2 keys.
- **BR-19 Profile Identity (MVP)**: `profileMeta` identifies profile
  lineage/version; no DB-designated latest tracking in MVP.
- **BR-20 Built-in Immutability**: Filesystem profiles are read-only runtime
  templates.
- **BR-21 Export Sanitization**: Deferred (no profile export in MVP).
- **BR-22 Lab Unit Organizational Scope**: Deferred with FR-025.

### API Surface Requirements

All new endpoints enforce role authorization per CR-007.

#### Core APIs (new/extended)

- Plugin-config CRUD per analyzer (JSONB-backed).
- QC/extraction/transform/flag/aggregation operations through plugin-config
  surface.
- Extended mapping preview endpoint for simulator outputs.
- Pending code query/action/cleanup APIs.
- Profile file read endpoint(s) under analyzer defaults/profiles surface.

Pending-code action semantics for MVP:

- `MAP`: mark pending entry as mapped and associate mapping workflow completion
  metadata.
- `IGNORE`: mark pending entry as ignored without creating a test mapping.
- `PENDING`: default state for newly detected unmapped codes.
- Invalid status transitions return validation error payloads and do not mutate
  state.

#### Deferred APIs

- Profile library CRUD/import/export APIs.
- Lab unit assignment APIs.

### Data Model Requirements

- Prerequisite migration: `009-decouple-test-mappings.xml` re-keys
  `analyzer_test_map` to `(analyzer_type_id, analyzer_test_name)`.
- Core instance config persists in:
  - `analyzer_plugin_config` (JSONB config by analyzer)
  - `analyzer_pending_code` (unmapped-code queue)
- Existing assets retained/extended:
  - `analyzer_test_map`
  - `AnalyzerField`
  - `AnalyzerFieldMapping`
- Not part of MVP:
  - `analyzer_profile`
  - `analyzer_profile_application`
  - `analyzer_lab_unit`
  - `astm_analyzer_config`
  - `astm_field_extraction_config`
  - `astm_qc_rule`
  - `astm_test_mapping_config`
  - `astm_flag_mapping`
  - `astm_pending_code`

### Constitution Compliance Requirements (OpenELIS Global)

- **CR-001**: Carbon Design System components only.
- **CR-002**: React Intl for all UI strings.
- **CR-003**: 5-layer backend architecture (Valueholder -> DAO -> Service ->
  Controller -> Form).
- **CR-004**: Liquibase-only schema management.
- **CR-006**: Configuration-driven behavior.
- **CR-007 (MVP)**: New config endpoints require `GLOBAL_ADMIN`.
- **CR-008**: Unit/integration/E2E tests required.

### Key Entities

- **Analyzer**: Existing analyzer instance aggregate root.
- **AnalyzerPluginConfig**: New per-analyzer JSONB config row.
- **AnalyzerPendingCode**: New per-analyzer unmapped-code queue entries.
- **AnalyzerTestMapping**: Existing mapping table with analyzer-type keying via
  009 migration.

---

## Acceptance Criteria Traceability

### Core Configuration (FR-014 to FR-021)

- Connection role fields available and validated.
- QC rule types and activation gate enforced.
- Transform types validated and applied in preview.
- Extraction overrides and aggregation options persisted.
- Abnormal flag mappings configurable.
- Simulator outputs preview data only.
- Pending code queue cap and retention behavior enforced.

### Profile Apply (FR-022 to FR-024 simplified)

- Profile selector shows filesystem built-ins plus "None".
- Selected profile pre-fills analyzer defaults.
- `configDefaults` is copied into `analyzer_plugin_config`.
- Snapshot-on-apply semantics are enforced.

### Deferred

- Profile library/import/export ACs (previous AC-92 to AC-99 family) are
  deferred.
- Lab unit ACs (previous AC-100 through AC-105) are deferred.

---

## Success Criteria _(mandatory)_

- **SC-001**: Built-in profile onboarding path can be completed in under 10
  minutes, measured from opening Add Analyzer to successful save using a
  standard built-in profile on local dev environment with seeded baseline data.
- **SC-002**: Profile-based analyzer creation deterministically applies
  defaults.
- **SC-003**: Activation blocked when QC rules are missing/invalid.
- **SC-004**: Simulator preview does not persist records.
- **SC-005**: Pending-code queue enforces cap and purge behavior.
- **SC-006**: Deferred (profile export not in MVP).
- **SC-007**: Deferred (lab unit filtering not in MVP).
- **SC-008**: New UI strings localized (minimum EN/FR).

---

## Assumptions & Constraints

### Assumptions

1. FR-014 through FR-021 behavior follows OGC-337 and current analyzer mapping
   UX.
2. Existing analyzer model from 011 remains authoritative.
3. Existing preview-mapping endpoint remains the simulator backend mechanism.

### Constraints

1. Use Carbon React components.
2. Keep profile payload portable and environment-agnostic.
3. Version DB changes via Liquibase in project versioned directories.
4. Exclude profile library sharing/import-export and lab-unit assignment from
   MVP.
5. Exclude dedicated profile reapply workflow from MVP.

---

## References

- `specs/012-generic-astm-plugin-profiles/docs/analyzer-mapping-templates-mockup.jsx`
- `specs/012-generic-astm-plugin-profiles/docs/astm-analyzer-mapping-addendum-v1_2.md`
- `specs/004-astm-analyzer-mapping/spec.md`
- `specs/011-madagascar-analyzer-integration/spec.md`
- `.specify/memory/constitution.md`
