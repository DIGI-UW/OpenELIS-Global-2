# Feature Specification: Analytical Laboratory Workflow

**Feature Branch**: `281-analytical-lab-workflow`
**Created**: 2025-12-14
**Status**: Draft
**Input**: User description: Analytical Laboratory with bioanalytical and pharmaceutical analysis units

## Executive Summary

The Analytical Laboratory handles samples from multiple sources: processed biological samples from the medical laboratory at AHRI clinical sites, medicines, and samples from external researchers/clients for pharmaceutical analysis or bioanalytical testing. This laboratory operates two specialized units:

1. **Bioanalytical Unit**: Processes biological samples using techniques like HPLC, UV-Vis, and LC-MS/MS
2. **Pharmaceutical Analysis Unit** (Physicochemical and R&D labs): Tests pharmaceutical products (APIs, tablets, capsules) for assay, dissolution, disintegration, friability, hardness, and identity

The workflow differs from the Immunology Laboratory (OGC-51) in several key ways:
- **Multi-source sample intake**: Clinical, research, and external client samples
- **Different test types**: Focus on physicochemical properties rather than immunological markers
- **Regulatory compliance**: Pharmaceutical testing follows USP methods and standards
- **Retention requirements**: Samples stored for stability studies or legal hold periods

**Target Users**: Chemical Analysts, Pharmacists, Researchers, Laboratory Supervisors, Quality Managers

**Expected Impact**:
- Standardize workflow across bioanalytical and pharmaceutical analysis units
- Track samples from diverse sources with consistent identification
- Automate test assignment based on test category and methodology
- Ensure complete traceability from reception to post-analysis storage

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Sample Reception & Registration (Priority: P1)

Laboratory staff receive samples from multiple sources (medical laboratory, external clients, researchers) and register them with appropriate identifiers and metadata. Samples may arrive with existing IDs (from medical lab) or need new IDs assigned.

**Why this priority**: This is the entry point for all samples. Without proper reception and registration, no downstream processing can occur. Must support both existing sample linking and new sample creation.

**Independent Test**: Register 5 samples from a manifest CSV with sample type "tablet", requested tests "Assay, Dissolution", verify all samples appear in the notebook with correct metadata.

**Acceptance Scenarios**:

1. **Given** the "Sample Reception" page is open, **When** a user selects "Link from Medical Lab", **Then** they can search for existing samples by accession number and link them to the analytical notebook
2. **Given** the "Sample Reception" page is open, **When** a user selects "Register New Samples" and uploads a manifest CSV, **Then** new sample records are created with:
   - Sample type (API, tablet, capsule, injection, syrup, cream, etc.)
   - Requested test(s) selected from available options
   - Storage condition prior to testing
   - Client/source information
3. **Given** a sample is registered, **When** it already has an external identifier from the source, **Then** that identifier is retained and stored alongside the new accession number
4. **Given** samples are registered from an external client, **When** the registration is complete, **Then** the client name, project reference, and submission date are recorded
5. **Given** a manifest CSV contains invalid sample types, **When** import is attempted, **Then** invalid rows are flagged with specific error messages
6. **Given** all samples in a batch are registered, **When** the user clicks "Mark Page Complete", **Then** the page shows as completed and samples advance to Test Assignment

---

### User Story 2 - Test Assignment & Preparation (Priority: P1)

Laboratory supervisors assign responsibility for tests to appropriate personnel (Chemical Analysts, Pharmacists, or Researchers) based on test category and select the analytical methodology to be used.

**Why this priority**: Test assignment determines who performs what analysis and with which methodology. This is the critical planning step before any analysis begins.

**Independent Test**: Assign 10 samples to a Chemical Analyst for HPLC assay testing, verify assignments are recorded and appear in the analyst's work queue.

**Acceptance Scenarios**:

1. **Given** the "Test Assignment" page is open, **When** viewing unassigned samples, **Then** each sample shows its requested test(s) and sample type
2. **Given** samples require Dissolution testing, **When** the supervisor selects samples and assigns to a Pharmacist, **Then** the assignment is recorded with:
   - Assigned analyst name
   - Methodology selected (e.g., USP I or USP II)
   - Expected completion date
3. **Given** samples require HPLC analysis, **When** the supervisor assigns methodology, **Then** they can select from:
   - HPLC (various column/detector configurations)
   - UV-Vis spectrophotometry
   - LC-MS/MS
4. **Given** samples require physical testing, **When** methodology is selected, **Then** options include:
   - Dissolution (USP Apparatus I - Basket, USP Apparatus II - Paddle)
   - Hardness testing
   - Friability testing
   - Disintegration testing
   - Identity test
5. **Given** a sample has multiple requested tests, **When** assignments are made, **Then** different analysts can be assigned to different tests on the same sample
6. **Given** assignments are complete for all samples, **When** viewing the assignment summary, **Then** workload distribution across analysts is displayed

---

### User Story 3 - Analysis Execution & Result Entry (Priority: P1)

Analysts conduct the assigned tests and record results. Results can be entered manually or imported from analyzer equipment.

**Why this priority**: This is where the core laboratory work happens. Accurate result capture is essential for reporting.

**Independent Test**: Import HPLC results for 20 samples from a CSV export, verify results are correctly matched to samples and stored.

**Acceptance Scenarios**:

1. **Given** the "Analysis Execution" page is open, **When** an analyst views their assigned samples, **Then** only their assigned tests are shown
2. **Given** an analyst completes a Dissolution test, **When** entering results manually, **Then** they can record:
   - Time points (e.g., 15, 30, 45, 60 minutes)
   - Percentage dissolved at each time point
   - Pass/fail against specification
   - Any observations or deviations
3. **Given** HPLC analysis is complete, **When** the analyst uploads an instrument export file, **Then** the system matches results to samples by sequence ID or sample identifier
4. **Given** a Hardness test is performed, **When** results are entered, **Then** the system records:
   - Individual tablet hardness values
   - Mean hardness
   - Standard deviation
   - Specification limits and pass/fail status
5. **Given** an Identity test is performed, **When** results are entered, **Then** the analyst can select result (Positive/Negative) and record the method used (IR, UV, HPLC comparison)
6. **Given** a result is outside specification limits, **When** it is recorded, **Then** the system flags it as Out of Specification (OOS) for review
7. **Given** all tests for a sample are complete, **When** viewing the sample, **Then** aggregate status shows (all tests passed, some failed, pending review)

---

### User Story 4 - Reporting & Release (Priority: P2)

Laboratory staff compile analytical result reports and submit them to the requesting unit or researchers.

**Why this priority**: Reporting is the deliverable that clients and researchers receive. Must be accurate and complete.

**Independent Test**: Generate an analytical report for 10 samples with multiple test types, export as PDF, verify all results and metadata are included.

**Acceptance Scenarios**:

1. **Given** the "Reporting" page is open, **When** selecting samples for reporting, **Then** only samples with completed analysis are available
2. **Given** samples are selected for reporting, **When** the user clicks "Generate Report", **Then** the system compiles:
   - Sample identification and source information
   - Test methods and specifications used
   - All results with pass/fail status
   - Any OOS results with disposition
   - Analyst signatures (electronic)
   - Report date and approval status
3. **Given** a report is generated, **When** the user reviews it, **Then** they can approve, reject, or request corrections
4. **Given** a report is approved, **When** the user clicks "Release to Client", **Then** the system records:
   - Release date and time
   - Recipient (requesting unit, researcher, or external client)
   - Delivery method (email, portal, physical)
5. **Given** results are released to an external client, **When** viewing the notebook, **Then** the release status and client acknowledgment are visible
6. **Given** a report needs revision, **When** the user creates a corrected report, **Then** both versions are retained with version history

---

### User Story 5 - Post-Test Sample & Data Handling (Priority: P2)

Laboratory staff transfer selected samples to the Biorepository and store retention quantities for defined stability or legal hold periods.

**Why this priority**: Proper sample retention is required for regulatory compliance and potential retesting.

**Independent Test**: Transfer 5 retention samples to biorepository storage with 2-year retention period, verify storage assignments and retention dates are recorded.

**Acceptance Scenarios**:

1. **Given** the "Post-Test Handling" page is open, **When** viewing analyzed samples, **Then** remaining sample quantity is displayed
2. **Given** samples require retention storage, **When** the user selects "Transfer to Biorepository", **Then** they specify:
   - Retention quantity
   - Storage condition (temperature, humidity)
   - Retention period (stability study duration or legal hold requirement)
   - Biorepository location
3. **Given** retention parameters are set, **When** samples are transferred, **Then** SampleStorageAssignment and SampleStorageMovement records are created
4. **Given** a sample has legal hold requirements, **When** stored, **Then** the hold reason and required retention end date are recorded
5. **Given** samples are transferred to biorepository, **When** viewing the notebook, **Then** complete chain of custody is visible from reception to final storage
6. **Given** the retention period expires, **When** viewing the sample, **Then** it is flagged for disposition review (destroy, extend retention, or release)

---

### Edge Cases

- What happens when a sample arrives without source documentation?
  **System MUST allow registration with "Source Unknown" flag, requiring supervisor approval before analysis can begin.**
- How does the system handle samples requiring multiple incompatible test methods?
  **System MUST support splitting samples with parent-child relationship, each child allocated to different methodology.**
- What happens when an analyst is unavailable after assignment?
  **System MUST allow supervisors to reassign tests to different analysts with audit trail of reassignment.**
- How does the system handle rejected samples?
  **System MUST support sample rejection with reason codes (insufficient quantity, damaged, contaminated) and notification to source.**
- What happens when retention storage is at capacity?
  **System MUST warn but allow assignment to alternative locations, with notification to storage manager.**
- How should OOS results be handled?
  **System MUST require OOS investigation workflow before results can be released - investigation ID, root cause, disposition.**

## Requirements _(mandatory)_

### Functional Requirements

#### Sample Reception & Registration

- **FR-001**: System MUST support sample intake from multiple sources: Medical Laboratory, External Clients, Research Projects
- **FR-002**: System MUST allow linking existing samples (from medical lab) by accession number
- **FR-003**: System MUST support creating new samples from manifest CSV with fields: sample_type, requested_tests, storage_condition, client_info, source_reference
- **FR-004**: System MUST support pharmaceutical sample types: API (Active Pharmaceutical Ingredient), Tablet, Capsule, Injection, Syrup, Cream, Ointment, Suspension
- **FR-005**: System MUST support requested test types: Assay, Dissolution, Disintegration, Friability, Hardness, Identity Test
- **FR-006**: System MUST record storage conditions: Room Temperature (15-25C), Refrigerated (2-8C), Frozen (-20C), Controlled Room Temperature (20-25C)
- **FR-007**: System MUST retain external identifiers from source systems alongside internal accession numbers
- **FR-008**: System MUST record client/source information: name, project reference, submission date, contact details

#### Test Assignment & Preparation

- **FR-010**: System MUST support assignment of tests to personnel roles: Chemical Analyst, Pharmacist, Researcher
- **FR-011**: System MUST support analytical methodology selection:
  - Chromatographic: HPLC, UV-Vis, LC-MS/MS
  - Physical: Dissolution (USP I/II), Hardness, Friability, Disintegration
  - Identification: IR, UV comparison, HPLC comparison, Chemical tests
- **FR-012**: System MUST allow multiple analysts to be assigned different tests on the same sample
- **FR-013**: System MUST record expected completion dates for assignments
- **FR-014**: System MUST support reassignment with audit trail when analysts are unavailable
- **FR-015**: System MUST display workload summary showing assignments per analyst

#### Analysis Execution & Results

- **FR-020**: System MUST support manual result entry for all test types
- **FR-021**: System MUST support bulk result import from HPLC/LC-MS/MS instrument exports (CSV format)
- **FR-022**: System MUST capture dissolution results with multiple time points and percentage dissolved
- **FR-023**: System MUST capture physical test results: hardness (mean, SD), friability (% loss), disintegration (time)
- **FR-024**: System MUST capture identity test results: Positive/Negative with method used
- **FR-025**: System MUST validate results against specification limits and flag OOS results
- **FR-026**: System MUST require OOS investigation before result release (investigation ID, root cause, disposition)
- **FR-027**: System MUST support result approval workflow (analyst entry, reviewer approval)

#### Reporting & Release

- **FR-030**: System MUST generate analytical result reports in PDF and Excel formats
- **FR-031**: System MUST include in reports: sample ID, source, test methods, specifications, all results, OOS notes, analyst signatures, approval status
- **FR-032**: System MUST support report versioning with complete history
- **FR-033**: System MUST record report release: date, recipient, delivery method
- **FR-034**: System MUST support electronic signatures for report approval
- **FR-035**: System MUST track client acknowledgment of released reports

#### Post-Test Sample Handling

- **FR-040**: System MUST support transfer of retention samples to Biorepository
- **FR-041**: System MUST record retention parameters: quantity, storage condition, retention period, location
- **FR-042**: System MUST integrate with existing SampleStorageService for storage assignments
- **FR-043**: System MUST support legal hold designations with hold reason and required end date
- **FR-044**: System MUST create audit trail of sample movements (reception to final storage)
- **FR-045**: System MUST flag samples for disposition review when retention period expires

### Key Entities

#### New Entities

- **AnalyticalTestAssignment**: Tracks test assignments to analysts. Key attributes: sample_item_id, test_type, assigned_to (user), methodology, expected_completion_date, actual_completion_date, status (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)

- **AnalyticalResult**: Stores test results. Key attributes: test_assignment_id, result_value, result_unit, specification_limit_low, specification_limit_high, pass_fail_status, oos_flag, oos_investigation_id, result_date, entered_by (user), approved_by (user), approval_date

- **DissolutionResult**: Specialized result for dissolution testing. Key attributes: analytical_result_id, time_point_minutes, percent_dissolved, medium, apparatus_type, rpm, temperature

- **AnalyticalReport**: Tracks generated reports. Key attributes: notebook_id, report_date, version_number, status (DRAFT, APPROVED, RELEASED, SUPERSEDED), approved_by (user), approval_date, release_date, recipient, delivery_method

- **RetentionSample**: Tracks samples held for stability/legal purposes. Key attributes: sample_item_id, retention_quantity, retention_unit, retention_reason (STABILITY, LEGAL_HOLD, REFERENCE), retention_start_date, retention_end_date, storage_assignment_id, disposition_status

#### Extended Entities

- **NoteBookPage**: Reuse page_type enum, add new types for analytical workflow: TEST_ASSIGNMENT, ANALYSIS_EXECUTION, REPORTING, RETENTION_HANDLING

- **SampleItem**: Add fields if not present: sample_type_detail (for pharmaceutical forms), client_id (for external clients), project_reference

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react)
- **CR-002**: All UI strings MUST be internationalized via React Intl
- **CR-003**: Backend MUST follow 5-layer architecture (Valueholder-DAO-Service-Controller-Form)
- **CR-004**: Database changes MUST use Liquibase changesets
- **CR-005**: @Transactional annotations MUST only be used in service layer
- **CR-006**: Services MUST compile all data within transaction boundaries
- **CR-007**: Security: RBAC for analytical operations, audit trail for all changes
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage goal)

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Laboratory staff can register 50 samples from a manifest in under 10 minutes
- **SC-002**: Test assignments for a 50-sample batch can be completed in under 15 minutes
- **SC-003**: Bulk result import from HPLC for 100 samples completes in under 3 minutes with zero transcription errors
- **SC-004**: 100% of samples have complete traceability from reception to retention storage
- **SC-005**: Analytical reports can be generated for 50 samples in under 5 minutes
- **SC-006**: OOS results require investigation completion before release (100% enforcement)
- **SC-007**: Retention samples have documented storage location and expiry date (100% compliance)
- **SC-008**: System supports concurrent work by 5+ analysts without performance degradation

### Quality Gates

- **QG-001**: All sample operations complete within 30 seconds for 50-sample batches
- **QG-002**: Result import handles files up to 10MB without timeout
- **QG-003**: Report generation completes within 60 seconds for 100 samples
- **QG-004**: All database operations use appropriate indexes
- **QG-005**: Audit trail captures all changes with user, timestamp, and previous value

## Reusable Components from OGC-51

The following backend services and patterns from the Immunology Laboratory workflow (OGC-51) can be reused:

1. **NotebookPageSample entity and service**: Per-sample-per-page tracking with status (PENDING, IN_PROGRESS, COMPLETED, SKIPPED)
2. **Bulk Operations framework**: Select All, Apply to Selected, batch processing
3. **SampleStorageService integration**: For retention sample storage assignments
4. **Manifest CSV import logic**: Column mapping interface and validation
5. **Sample grid UI patterns**: Pagination, filtering, inline editing, virtualization
6. **Progress tracking**: Page completion indicators, sample counts
7. **Report attachment service**: Saving generated reports to notebook

## Dependencies

- **Existing**: NoteBook, NoteBookPage, NoteBookSample entities and services
- **Existing**: SampleStorageService for biorepository integration
- **Existing**: SampleItem, Sample entities
- **Existing from OGC-51**: NotebookPageSample entity, bulk operation endpoints
- **New**: AnalyticalTestAssignment entity and services
- **New**: AnalyticalResult and DissolutionResult entities
- **New**: AnalyticalReport entity and generation service
- **New**: RetentionSample entity for legal hold tracking

## Out of Scope

- Instrument integration via direct API connections (use file export/import)
- Automated stability study scheduling
- Certificate of Analysis (CoA) generation with regulatory formatting
- Client portal for external submission and result retrieval
- Barcode/QR code printing for retention samples
- Integration with LIMS systems
