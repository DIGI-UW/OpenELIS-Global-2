# Research: Analytical Laboratory Workflow

## Decisions and Clarifications

- **Manifest CSV structure**

  - **Decision**: Reuse OGC-51 manifest importer with analytical-specific
    required columns: `sample_type`, `requested_tests` (semicolon-delimited),
    `storage_condition`, `client_name`, `project_reference`, `external_id`,
    `submission_date`. Optional: `source_reference`, `notes`.
  - **Rationale**: Aligns with existing bulk import flow and validation surfaces
    row-level errors (per spec). Minimal changes while supporting external
    clients.
  - **Alternatives considered**: New importer dedicated to analytical; rejected
    to avoid duplicate validation and UI patterns.

- **Test methodology vocabulary**

  - **Decision**: Enumerate methodologies in configuration with categories
    `chromatographic` (HPLC, UV-Vis, LC-MS/MS), `physical` (Dissolution USP
    I/II, Hardness, Friability, Disintegration), and `identity` (IR, UV
    comparison, HPLC comparison, chemical tests). Store as coded values to allow
    i18n.
  - **Rationale**: Matches acceptance criteria and supports workload summary by
    method type.
  - **Alternatives considered**: Free-text methods; rejected for reporting
    consistency and validation.

- **Role-based assignment rules**

  - **Decision**: Use existing RBAC roles extended with new permissions
    `ANALYTICAL_ASSIGNMENT_MANAGE`, `ANALYTICAL_RESULT_ENTER`,
    `ANALYTICAL_REPORT_RELEASE`, `ANALYTICAL_RETENTION_MANAGE`. Supervisors can
    assign; analysts/pharmacists/researchers receive queues.
  - **Rationale**: Keeps security centralized; satisfies constitution security
    gate.
  - **Alternatives considered**: Hardcoded role lists per page; rejected because
    it violates configuration-driven variation.

- **OOS investigation workflow**

  - **Decision**: Add OOS fields on `AnalyticalResult` (flag +
    `oos_investigation_id`, `root_cause`, `disposition`). Block report release
    when any linked result has unresolved OOS flag.
  - **Rationale**: Meets CR-007 and acceptance criteria requiring investigation
    before release.
  - **Alternatives considered**: Separate workflow service; deferred to later
    milestone to keep scope manageable.

- **Instrument CSV import mapping**

  - **Decision**: Support column mapping UI using existing bulk import
    framework; match samples by `sequence_id` or `sample_identifier` with
    preview and row-level errors. Expected columns: `sequence_id`,
    `sample_identifier`, `analyte`, `result_value`, `units`, `spec_low`,
    `spec_high`, `retention_time`, `area`, `status`.
  - **Rationale**: Reuses proven import pipeline; keeps instrument-specific
    parsing out of scope while enabling HPLC/LC-MS exports.
  - **Alternatives considered**: Custom parser per instrument; deferred per
    out-of-scope (direct API integrations).

- **Retention handling & biorepository integration**

  - **Decision**: Use existing `SampleStorageService` to create
    `SampleStorageAssignment` and `SampleStorageMovement` for retention
    transfers. Add `RetentionSample` entity referencing storage assignment and
    retention dates; enforce expiry flagging via scheduled job hook (reuse
    storage expiry checker where available).
  - **Rationale**: Aligns with reuse list and avoids duplicate storage logic.
  - **Alternatives considered**: New retention storage tables; rejected to keep
    single source of truth for locations.

- **Reporting format and signature**
  - **Decision**: Generate PDF/Excel using existing report attachment service;
    include electronic signature metadata (user id, timestamp). Versioning via
    `AnalyticalReport.version_number` and status (`DRAFT`, `APPROVED`,
    `RELEASED`, `SUPERSEDED`).
  - **Rationale**: Leverages current notebook report infrastructure; satisfies
    versioning and release audit requirements.
  - **Alternatives considered**: New reporting microservice; deferred as scope
    creep.
