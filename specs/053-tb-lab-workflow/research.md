# Research - Tuberculosis Laboratory Workflow

**Updated**: 2025-12-18

## Items and Decisions

- **Frontend page duplication strategy**

  - **Decision**: Duplicate TB-specific pages from existing workflows
    (Immunology/MNTD, Pharma) rather than sharing/extending them. Reuse only
    shared components (`SampleGrid`, `PageNavigation`,
    `StorageHierarchySelector`).
  - **Rationale**: Prevents merge conflicts during parallel workflow
    development. Each workflow team can iterate independently. Pages will be
    harmonized in a future refactoring milestone after all workflows stabilize.
  - **Alternatives**: Shared component library (rejected: too early for
    abstraction, workflows still evolving, merge conflicts likely).

- **Current implementation status**

  - **Page 1 (Sample Creation)**: ✅ Implemented
    - `TBSampleCreationPage.js` - Full metadata capture with manifest import
    - `TBManifestImportModal.js` - CSV import functionality
    - `TBWorkflowTab.js` - Container with 6 default pages (5 are placeholders)
  - **Pages 2-6**: Placeholders in TBWorkflowTab.js, need implementation
  - **Backend**: TB entities not yet created, need Liquibase + valueholders

- **GeneXpert integration**

  - **Decision**: Treat GeneXpert and other molecular results as manual entry
    flows now; expose a future-safe hook (service method + REST endpoint flag)
    to attach instrument payloads but do not build analyzer polling in this
    milestone.
  - **Rationale**: Spec scopes manual entry only; avoids blocking on instrument
    APIs while leaving integration point.
  - **Alternatives**: Direct instrument listener now (rejected: out-of-scope,
    adds infra/testing risk).

- **REDCap export path**

  - **Decision**: Export CSV/Excel via existing DataExport pattern; include
    REDCap-ready column order + statuses. Track `exported_to_redcap_at` + user
    on sample/report to satisfy audit.
  - **Rationale**: Spec calls for CSV/Excel; aligns with current DataExport
    submodule and avoids new API contract.
  - **Alternatives**: REDCap API push (rejected now: contract unknown, network
    creds not available). Follow-up to add API client once mapping/credentials
    are provided.

- **TB sample ID scope**

  - **Decision**: One TB ID sequence per installation per calendar year
    (Liquibase-managed sequence `tb_sample_id_seq_<year>`). Persist generated ID
    on `sample_item.tb_sample_id`; enforce uniqueness with index + validation.
    Add config hook to switch to per-site namespace later if required.
  - **Rationale**: Spec requires NNN/YY per year; most deployments are single
    lab instance. Keeps generation deterministic and testable.
  - **Alternatives**: Per-site sequence (needs site identifier in sample
    context) — defer until confirmed.

- **Culture tracking performance**

  - **Decision**: Reuse notebook grid virtualization + server-side paging;
    prefetch culture readings with `JOIN FETCH` by sample and order by week;
    index `(sample_item_id, week_number)` for fast lookups and uniqueness guard.
  - **Rationale**: Meets 8-week grid UX while avoiding N+1/slow queries; aligns
    with existing notebook grid patterns.

- **DST data modeling**

  - **Decision**: Store first-line drugs as discrete columns; store second-line
    panels in validated JSONB map with allowed keys; compute `mdr_flag` in
    service when INH-R + RMP-R; persist method enums as strings.
  - **Rationale**: Mirrors spec structure, keeps queries simple for common
    drugs, allows flexible second-line panels without schema churn.

- **QC flag propagation**

  - **Decision**: Keep QC entity as source of truth with `overall_result` +
    `rejection_reason`; propagate a "has_qc_failure" warning via services so
    downstream pages can display alerts and require acknowledgement.
  - **Rationale**: Ensures QC outcomes remain visible through the workflow
    without duplicating flags on every page.

- **TB workflow page structure**

  - **Decision**: Expand from current 6 pages to 11-12 pages to match full TB
    workflow requirements from screenshots:
    1. Sample Creation & Registration ✅
    2. Quality Check (QC) - Raw Sample Integrity
    3. Sample Storage Assignment
    4. Initial Processing (media preparation)
    5. Culture Inoculation & Weekly Monitoring (8-week grid)
    6. Smear Microscopy & AFB Results
    7. Species Identification
    8. GeneXpert Testing
    9. Drug Susceptibility Testing (DST)
    10. Isolate Storage
    11. Result Compilation & Reporting
    12. Data Export (REDCap)
  - **Rationale**: Full TB workflow from screenshots requires dedicated pages
    for each diagnostic step. Culture tracking is unique to TB (8-week
    monitoring).
  - **Implementation**: Update `DEFAULT_TB_WORKFLOW_PAGES` in `TBWorkflowTab.js`

- **Existing workflow patterns to reference**

  - **MNTD (Immunology)**: 10+ pages with similar structure
    - `MNTDSampleIntakePage.js` - Sample intake/registration
    - `MNTDReceptionVerificationPage.js` - QC equivalent
    - `MNTDProcessingQCPage.js` - Processing
    - `MNTDTemporaryStoragePage.js` - Storage
    - `MNTDTestExecutionPage.js` - Test execution
    - `MNTDReportingREDCapPage.js` - REDCap export
  - **Pharma**: 7 pages with similar patterns
    - `PharmaceuticalQualityCheckPage.js` - QC pattern to duplicate
    - `PharmaceuticalStoragePage.js` - Storage pattern to duplicate
  - **Shared components** (reuse, don't duplicate):
    - `SampleGrid.js` - Virtualized sample grid with status filtering
    - `PageNavigation.js` - 9-page navigation with progress indicators
    - `StorageHierarchySelector.js` - Hierarchical storage picker

## Follow-ups

- Confirm REDCap variable mapping and column order with data managers.
- Confirm whether analyzer import hook should emit events or remain manual-only
  for now.
- Confirm if TB sample ID needs site-specific prefix for multi-site deployments.
- Update `DEFAULT_TB_WORKFLOW_PAGES` in `TBWorkflowTab.js` to reflect 11-12
  pages.
- Create TB-specific backend entities in `org.openelisglobal.tb` package.
