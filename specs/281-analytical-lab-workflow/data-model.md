# Data Model: Analytical Laboratory Workflow

## Core Entities

### AnalyticalTestAssignment

- **Purpose**: Track per-sample test assignments to analysts with methodology
  and schedule.
- **Fields**: `id`, `sample_item_id` (FK SampleItem), `test_type` (ASSAY,
  DISSOLUTION, DISINTEGRATION, FRIABILITY, HARDNESS, IDENTITY), `methodology`
  (coded value, see research), `assigned_to_user_id`,
  `expected_completion_date`, `actual_completion_date`, `status` (PENDING,
  IN_PROGRESS, COMPLETED, CANCELLED, REASSIGNED), `reassignment_reason`,
  `created_by`, `created_date`, `last_updated_date`.
- **Relationships**: many-to-one SampleItem; one-to-many AnalyticalResult; audit
  via existing change tracking.

### AnalyticalResult

- **Purpose**: Store analytical outcomes for a specific assignment/test.
- **Fields**: `id`, `test_assignment_id` (FK AnalyticalTestAssignment),
  `analyte` (optional), `result_value`, `result_unit`,
  `specification_limit_low`, `specification_limit_high`, `pass_fail_status`
  (PASS/FAIL/PENDING), `oos_flag` (boolean), `oos_investigation_id`,
  `oos_root_cause`, `oos_disposition`, `result_date`, `entered_by_user_id`,
  `approved_by_user_id`, `approval_date`, `remarks`.
- **Relationships**: many-to-one AnalyticalTestAssignment; one-to-many
  DissolutionResult (when applicable).

### DissolutionResult

- **Purpose**: Capture time-series dissolution data tied to an AnalyticalResult.
- **Fields**: `id`, `analytical_result_id` (FK), `time_point_minutes`,
  `percent_dissolved`, `medium`, `apparatus_type` (USP_I, USP_II), `rpm`,
  `temperature_c`.
- **Relationships**: many-to-one AnalyticalResult.

### AnalyticalReport

- **Purpose**: Versioned report container for notebook outputs.
- **Fields**: `id`, `notebook_id` (FK NoteBook), `version_number`, `status`
  (DRAFT, APPROVED, RELEASED, SUPERSEDED), `report_date`, `approved_by_user_id`,
  `approval_date`, `release_date`, `recipient`, `delivery_method`,
  `attachment_id` (link to report storage), `oos_present` (boolean),
  `release_notes`.
- **Relationships**: many-to-one NoteBook; may reference generated attachment
  via existing report service.

### RetentionSample

- **Purpose**: Track retention/legal hold inventory after analysis.
- **Fields**: `id`, `sample_item_id` (FK), `retention_quantity`,
  `retention_unit`, `retention_reason` (STABILITY, LEGAL_HOLD, REFERENCE),
  `retention_start_date`, `retention_end_date`, `storage_assignment_id` (FK
  SampleStorageAssignment), `disposition_status` (ACTIVE, DUE_REVIEW, DISPOSED,
  EXTENDED), `hold_reason_text`.
- **Relationships**: many-to-one SampleItem; references storage assignment for
  chain of custody.

## Extended / Reused Entities

- **NoteBookPage**: Add page types `TEST_ASSIGNMENT`, `ANALYSIS_EXECUTION`,
  `REPORTING`, `RETENTION_HANDLING` for navigation/progress. Reuse existing
  status fields.
- **SampleItem**: Add `sample_type_detail` (pharmaceutical form), `client_id`
  (or client_name for external), `project_reference`, `external_identifier`
  fields where not present.
- **NotebookPageSample**: Reuse for per-sample page state; add status columns if
  needed for analytical phases.

## Enumerations

- **TestType**: ASSAY, DISSOLUTION, DISINTEGRATION, FRIABILITY, HARDNESS,
  IDENTITY.
- **Methodology** (config-driven coded values): HPLC, UV_VIS, LC_MS_MS,
  DISSOLUTION_USP_I, DISSOLUTION_USP_II, HARDNESS, FRIABILITY, DISINTEGRATION,
  IDENTITY_IR, IDENTITY_UV_COMPARISON, IDENTITY_HPLC_COMPARISON,
  IDENTITY_CHEMICAL.
- **ReportStatus**: DRAFT, APPROVED, RELEASED, SUPERSEDED.
- **RetentionReason**: STABILITY, LEGAL_HOLD, REFERENCE.
- **DispositionStatus**: ACTIVE, DUE_REVIEW, DISPOSED, EXTENDED.

## Data Integrity & Rules

- Results cannot be approved if `oos_flag` is true and `oos_disposition` is
  empty.
- Report release blocked if any linked AnalyticalResult has `oos_flag = true`
  without disposition.
- Retention expiry checker flags records where `retention_end_date <= today` and
  `disposition_status = ACTIVE` → set to DUE_REVIEW.
- Deletions of SampleItem must cascade/validate absence of retention and report
  artifacts.

## Storage & Migration Plan

- All new tables/columns added via Liquibase changesets under
  `src/main/resources/liquibase/analytical/` with rollback entries.
- Indexes: `AnalyticalTestAssignment(sample_item_id, status)`,
  `AnalyticalResult(test_assignment_id)`,
  `DissolutionResult(analytical_result_id)`,
  `RetentionSample(retention_end_date, disposition_status)`, plus FK indexes.
