# Tasks: Immunology Laboratory Workflow

**Input**: Design documents from `/specs/OGC-51-immunology-workflow/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests ARE REQUIRED per CR-008 (>70% coverage goal, unit +
integration + E2E).

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/org/openelisglobal/`
- **Frontend**: `frontend/src/`
- **Backend Tests**: `src/test/java/org/openelisglobal/`
- **E2E Tests**: `frontend/cypress/e2e/`
- **Liquibase**: `src/main/resources/liquibase/3.4.x.x/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, database schema, and base entity
infrastructure

- [x] T001 Create Liquibase changeset directory
      `src/main/resources/liquibase/3.4.x.x/`
- [x] T002 [P] Create Liquibase changeset for NotebookPageSample table in
      `src/main/resources/liquibase/3.4.x.x/001-notebook-page-sample.xml`
- [x] T003 [P] Create Liquibase changeset for AnalyzerResultImport table in
      `src/main/resources/liquibase/3.4.x.x/002-analyzer-result-import.xml`
- [x] T004 [P] Create Liquibase changeset for SampleRouting table in
      `src/main/resources/liquibase/3.4.x.x/003-sample-routing.xml`
- [x] T005 [P] Create Liquibase changeset to add page_type column to
      notebook_page in
      `src/main/resources/liquibase/3.4.x.x/004-notebook-page-type.xml`
- [x] T006 Add Liquibase include references in master changelog
      `src/main/resources/liquibase/base-changelog.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities and DAOs that MUST be complete before ANY user story
can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

### NotebookPageSample Entity (Core Junction Table)

- [x] T007 Create NotebookPageSample.Status enum in
      `src/main/java/org/openelisglobal/notebook/valueholder/NotebookPageSample.java`
- [x] T008 Create NotebookPageSample valueholder entity with JPA annotations in
      `src/main/java/org/openelisglobal/notebook/valueholder/NotebookPageSample.java`
- [x] T009 Create NotebookPageSampleDAO interface in
      `src/main/java/org/openelisglobal/notebook/dao/NotebookPageSampleDAO.java`
- [x] T010 Create NotebookPageSampleDAOImpl with HQL queries in
      `src/main/java/org/openelisglobal/notebook/dao/NotebookPageSampleDAOImpl.java`
- [x] T011 Create NotebookPageSampleService interface in
      `src/main/java/org/openelisglobal/notebook/service/NotebookPageSampleService.java`
- [x] T012 Create NotebookPageSampleServiceImpl in
      `src/main/java/org/openelisglobal/notebook/service/NotebookPageSampleServiceImpl.java`

### SampleRouting Entity (Destination Tracking)

- [x] T013 [P] Create SampleRouting.DestinationType enum in
      `src/main/java/org/openelisglobal/notebook/valueholder/SampleRouting.java`
- [x] T014 [P] Create SampleRouting valueholder entity in
      `src/main/java/org/openelisglobal/notebook/valueholder/SampleRouting.java`
- [x] T015 [P] Create SampleRoutingDAO interface in
      `src/main/java/org/openelisglobal/notebook/dao/SampleRoutingDAO.java`
- [x] T016 [P] Create SampleRoutingDAOImpl in
      `src/main/java/org/openelisglobal/notebook/dao/SampleRoutingDAOImpl.java`
- [x] T016a Create SampleRoutingService interface in
      `src/main/java/org/openelisglobal/notebook/service/SampleRoutingService.java`
- [x] T016b Create SampleRoutingServiceImpl in
      `src/main/java/org/openelisglobal/notebook/service/SampleRoutingServiceImpl.java`

### AnalyzerResultImport Entity (Import Audit)

- [x] T017 [P] Create AnalyzerResultImport valueholder entity in
      `src/main/java/org/openelisglobal/notebook/valueholder/AnalyzerResultImport.java`
- [x] T018 [P] Create AnalyzerResultImportDAO interface in
      `src/main/java/org/openelisglobal/notebook/dao/AnalyzerResultImportDAO.java`
- [x] T019 [P] Create AnalyzerResultImportDAOImpl in
      `src/main/java/org/openelisglobal/notebook/dao/AnalyzerResultImportDAOImpl.java`
- [x] T019a Create AnalyzerResultImportService interface in
      `src/main/java/org/openelisglobal/notebook/service/AnalyzerResultImportService.java`
- [x] T019b Create AnalyzerResultImportServiceImpl in
      `src/main/java/org/openelisglobal/notebook/service/AnalyzerResultImportServiceImpl.java`

### Form Classes

- [x] T020 [P] Create NotebookPageSampleForm bean in
      `src/main/java/org/openelisglobal/notebook/form/NotebookPageSampleForm.java`
- [x] T021 [P] Create BulkApplyForm bean in
      `src/main/java/org/openelisglobal/notebook/form/BulkApplyForm.java`
- [x] T022 [P] Create SampleRoutingForm bean in
      `src/main/java/org/openelisglobal/notebook/form/SampleRoutingForm.java`
- [x] T023 [P] Create AnalyzerImportForm bean in
      `src/main/java/org/openelisglobal/notebook/form/AnalyzerImportForm.java`
- [x] T024 [P] Create ManifestImportForm bean in
      `src/main/java/org/openelisglobal/notebook/form/ManifestImportForm.java`

### Foundational Tests

- [x] T025 [P] Create NotebookPageSampleDAOTest in
      `src/test/java/org/openelisglobal/notebook/dao/NotebookPageSampleDAOTest.java`
- [x] T026 [P] Create NotebookPageSampleServiceTest in
      `src/test/java/org/openelisglobal/notebook/service/NotebookPageSampleServiceTest.java`
- [x] T027 Verify ORM mapping loads successfully (run build with
      -DskipTests=false for single test)

**Checkpoint**: Foundation ready - user story implementation can now begin in
parallel

---

## Phase 3: User Story 1 - Create Notebook Instance from Template (Priority: P0)

**Goal**: Laboratory supervisors can create a new workflow instance from an
Immunology template and link samples for tracking.

**Independent Test**: Create notebook instance from template, add 5 samples by
accession number, verify all samples appear in notebook's sample list and all 9
pages show "0/5 samples completed".

### Tests for User Story 1

- [x] T028 [P] [US1] Create unit test for notebook instance creation in
      `src/test/java/org/openelisglobal/notebook/service/NotebookInstanceCreationTest.java`
- [ ] T029 [P] [US1] Create integration test for sample linking in
      `src/test/java/org/openelisglobal/notebook/controller/NotebookSampleEntryControllerTest.java`
- [ ] T030 [P] [US1] Create Cypress E2E test for notebook creation workflow in
      `frontend/cypress/e2e/notebookWorkflowCreation.cy.js`

### Implementation for User Story 1

- [x] T031 [US1] Create NotebookSampleEntryService interface in
      `src/main/java/org/openelisglobal/notebook/service/NotebookSampleEntryService.java`
- [x] T032 [US1] Implement searchSamples method in
      NotebookSampleEntryServiceImpl in
      `src/main/java/org/openelisglobal/notebook/service/NotebookSampleEntryServiceImpl.java`
- [x] T033 [US1] Implement linkSamplesToNotebook method in
      NotebookSampleEntryServiceImpl creating NotebookPageSample for all pages
- [x] T034 [US1] Create NotebookSampleEntryController REST endpoints in
      `src/main/java/org/openelisglobal/notebook/controller/rest/NotebookSampleEntryController.java`
- [x] T035 [US1] Implement GET /notebook/{id}/samples/search endpoint
- [x] T036 [US1] Implement POST /notebook/{id}/samples/link endpoint
- [x] T037 [US1] Extend NoteBookService to create notebook instances from
      templates with all pages copied
- [x] T038 [US1] Add progress calculation method getPageProgress() to
      NotebookPageSampleService

### Frontend for User Story 1

- [x] T039 [P] [US1] Create NotebookWorkflowTab container component in
      `frontend/src/components/notebook/workflow/NotebookWorkflowTab.js`
- [x] T040 [P] [US1] Create PageNavigation component (9 pages with progress
      indicators) in
      `frontend/src/components/notebook/workflow/PageNavigation.js`
- [x] T041 [US1] Add internationalization keys for notebook workflow to
      `frontend/src/languages/en.json` (~50 keys)
- [x] T042 [US1] Add French translations to `frontend/src/languages/fr.json`

**Checkpoint**: User Story 1 complete - notebook instances can be created and
samples linked

---

## Phase 4: User Story 2 - Sample Reception & Creation (Priority: P1)

**Goal**: Laboratory technicians can link existing samples OR create new project
samples from a manifest CSV for bulk sample intake.

**Independent Test**: Import manifest CSV with 4 rows (num_of_samples: 10, 15,
20, 5), verify 50 total SampleItem records created with correct external_id
pattern (GRP-001-001 through GRP-001-010 for first row, etc.).

### Tests for User Story 2

- [x] T043 [P] [US2] Create unit test for manifest CSV parsing in
      `src/test/java/org/openelisglobal/notebook/service/ManifestImportServiceTest.java`
- [ ] T044 [P] [US2] Create integration test for sample creation endpoint in
      `src/test/java/org/openelisglobal/notebook/controller/ManifestImportControllerTest.java`
- [ ] T045 [P] [US2] Create Cypress E2E test for sample reception workflow in
      `frontend/cypress/e2e/notebookWorkflowSampleReception.cy.js`

### Implementation for User Story 2

- [x] T046 [US2] Implement manifest CSV parsing in ManifestImportServiceImpl
      (num_of_samples handling)
- [x] T047 [US2] Implement SampleItem creation with external_id pattern
      (groupId + sequential suffix)
- [x] T048 [US2] Implement sample type validation against TypeOfSample records
- [x] T049 [US2] Implement POST /notebook/{id}/samples/create-from-manifest
      endpoint
- [x] T050 [US2] Add error handling for invalid sample types with row-level
      error reporting

### Frontend for User Story 2

- [x] T051 [P] [US2] Create ManifestImportModal component in
      `frontend/src/components/notebook/workflow/ManifestImportModal.js`
- [x] T052 [P] [US2] Create SampleGrid component with Carbon DataTable in
      `frontend/src/components/notebook/workflow/SampleGrid.js`
- [x] T053 [US2] Create SampleReceptionPage component (Page 1) in
      `frontend/src/components/notebook/pages/SampleReceptionPage.js`
- [x] T054 [US2] Add column mapping UI for CSV import in ManifestImportModal
- [x] T055 [US2] Add sample verification status UI with "Mark Verified" action

**Checkpoint**: User Story 2 complete - samples can be created from manifest CSV

---

## Phase 5: User Story 3 - Bulk Data Entry for Initial Processing (Priority: P1)

**Goal**: Laboratory technicians can record volume, cell count, and isolation
method with bulk apply functionality for multiple samples.

**Independent Test**: Select 10 samples, apply common values (Volume: 5.0mL,
Method: Ficoll), verify all 10 samples updated in database with JSONB data field
containing applied values.

### Tests for User Story 3

- [ ] T056 [P] [US3] Create unit test for bulk apply operation in
      `src/test/java/org/openelisglobal/notebook/service/NotebookBulkOperationServiceTest.java`
- [ ] T057 [P] [US3] Create integration test for bulk endpoints in
      `src/test/java/org/openelisglobal/notebook/controller/NotebookBulkOperationControllerTest.java`
- [ ] T058 [P] [US3] Create Cypress E2E test for bulk operations in
      `frontend/cypress/e2e/notebookWorkflowBulkOperations.cy.js`

### Implementation for User Story 3

- [x] T059 [US3] Create NotebookBulkOperationService interface in
      `src/main/java/org/openelisglobal/notebook/service/NotebookBulkOperationService.java`
- [x] T060 [US3] Implement batch processing (50 samples per batch) in
      NotebookBulkOperationServiceImpl in
      `src/main/java/org/openelisglobal/notebook/service/NotebookBulkOperationServiceImpl.java`
- [x] T061 [US3] Implement bulkApplyValues method updating JSONB data field
- [x] T062 [US3] Implement bulkUpdateStatus method for status transitions
- [x] T063 [US3] Create NotebookBulkOperationController in
      `src/main/java/org/openelisglobal/notebook/controller/rest/NotebookBulkOperationController.java`
- [x] T064 [US3] Implement POST /notebook/bulk/page/{id}/samples/apply endpoint
- [x] T065 [US3] Implement POST /notebook/bulk/page/{id}/samples/status endpoint
- [x] T066 [US3] Implement GET /notebook/bulk/page/{id}/progress endpoint
- [x] T067 [US3] Implement GET /notebook/bulk/page/{id}/samples endpoint with
      pagination

### Frontend for User Story 3

- [x] T068 [P] [US3] Create BulkApplyForm component in
      `frontend/src/components/notebook/workflow/BulkApplyForm.js`
- [x] T069 [US3] Add virtualized rendering support to SampleGrid (react-window
      integration)
- [x] T070 [US3] Create InitialProcessingPage component (Page 2) in
      `frontend/src/components/notebook/pages/InitialProcessingPage.js`
- [x] T071 [US3] Implement inline editing in SampleGrid for individual sample
      values
- [x] T072 [US3] Add filter by status (Pending/Completed/All) to SampleGrid
- [x] T072a [US3] Implement column sorting in SampleGrid component (FR-043)
- [x] T073 [US3] Add "Mark Page Complete" functionality with progress validation

**Checkpoint**: User Story 3 complete - bulk data entry working for 200+ samples

---

## Phase 5a: User Story 3a - Assay Recording (Priority: P1)

**Goal**: Laboratory technicians can record assay data (tests run on samples)
with bulk apply functionality.

**Independent Test**: Select 5 samples, record assay data (test type, kit lot,
results), verify status transitions to IN_PROGRESS.

### Implementation for User Story 3a

- [x] T059a [US3a] Create AssaysPage component (Page 3) in
      `frontend/src/components/notebook/pages/AssaysPage.js`
- [x] T059b [US3a] Add assay recording form fields (test type, kit lot, results)
- [x] T059c [US3a] Integrate with bulk status update endpoint for IN_PROGRESS
      transitions
- [x] T059d [US3a] Add gridId prop to SampleGrid for unique checkbox handling

**Checkpoint**: User Story 3a complete - assay data can be recorded

---

## Phase 6a: User Story 4a - Analysis Preparation (Priority: P2)

**Goal**: Laboratory technicians can prepare routed child samples for analysis,
handling fresh samples, thawed samples from storage, and incubation protocols.

**Prerequisites**: Samples must be routed (US4 complete) before preparation.

**Workflow Options**: a. Fresh analysis - proceed directly, mark as ready b.
Stored samples - log retrieval/thaw from storage, record thaw conditions c.
Incubation required - record incubation duration and conditions

**Independent Test**: Select 10 routed samples, record preparation data (prep
type: fresh/thawed/incubated, conditions, duration), verify status transitions
to PREPARED.

### Implementation for User Story 4a

- [x] T091a [US4a] Create PrepPage component (Page 5) in
      `frontend/src/components/notebook/pages/PrepPage.js`
- [x] T091b [US4a] Add preparation type selector (Fresh/Thawed/Incubated)
- [x] T091c [US4a] Add conditional fields for thawed samples (retrieval date,
      thaw conditions, storage location reference)
- [x] T091d [US4a] Add conditional fields for incubation (duration, temperature,
      conditions)
- [x] T091e [US4a] Add bulk preparation recording with BulkApplyForm integration
- [x] T091f [US4a] Integrate with bulk status update endpoint for PREPARED
      status
- [x] T091g [US4a] Filter grid to show only routed samples (from US4)

**Checkpoint**: User Story 4a complete - sample preparation can be tracked

---

## Phase 6: User Story 4 - Child Sample Creation with Destination Routing (Priority: P1)

**Goal**: Create child samples from parents and route to different destinations
(internal analysis, external lab, storage) with automatic well assignment for
internal analysis.

**Independent Test**: Create 96 child samples, route to internal analysis with
box assignment, verify samples are assigned to wells A1-H12 in row-major
sequential order.

### Tests for User Story 4

- [ ] T074 [P] [US4] Create unit test for child sample creation in
      `src/test/java/org/openelisglobal/notebook/service/ChildSampleCreationServiceTest.java`
- [ ] T075 [P] [US4] Create unit test for well coordinate auto-assignment in
      `src/test/java/org/openelisglobal/notebook/service/SampleRoutingServiceTest.java`
- [ ] T076 [P] [US4] Create Cypress E2E test for routing workflow in
      `frontend/cypress/e2e/notebookWorkflowRouting.cy.js`

### Implementation for User Story 4

- [x] T077 [US4] Implement createChildSamples method in
      NotebookSampleEntryService with parent-child linking
- [x] T078 [US4] Implement POST /notebook/{id}/samples/create-children endpoint
- [x] T079 [US4] Extend SampleRoutingService with destination routing methods
      (routeToInternalAnalysis, routeToExternalLab, routeToStorage)
- [x] T080 [US4] Add well coordinate assignment logic to
      SampleRoutingServiceImpl
- [x] T081 [US4] Implement well coordinate auto-assignment (row-major: A1, A2,
      ..., A12, B1, ...)
- [x] T082 [US4] Integrate with SampleStorageService for storage routing
- [x] T083 [US4] Create NotebookRoutingController in
      `src/main/java/org/openelisglobal/notebook/controller/rest/NotebookRoutingController.java`
- [x] T084 [US4] Implement POST /notebook/{id}/samples/route endpoint
      (INTERNAL_ANALYSIS, EXTERNAL_LAB, STORAGE)
- [x] T085 [US4] Implement GET /notebook/{id}/samples/routing endpoint for
      status
- [x] T086 [US4] Implement GET /notebook/{id}/box/{boxId}/layout endpoint for
      visual grid

### Frontend for User Story 4

- [x] T087 [P] [US4] Create SampleRoutingPage component with destination cards
      in `frontend/src/components/notebook/pages/SampleRoutingPage.js`
- [x] T088 [P] [US4] Create BoxLayoutViewer component (well plate grid) in
      `frontend/src/components/notebook/workflow/BoxLayoutViewer.js`
- [x] T089 [US4] Create ChildSampleCreationPage component (Page 4) in
      `frontend/src/components/notebook/pages/ChildSampleCreationPage.js`
- [x] T090 [US4] Add parent-child navigation links in sample details
- [x] T091 [US4] Add filter by routing status (Routed/Unrouted) to SampleGrid

**Checkpoint**: User Story 4 complete - child samples can be created and routed
with well assignment

---

## Phase 7: User Story 5 - Main Analysis Execution (Priority: P2)

**Goal**: Execute primary assays (ELISA or Flow Cytometry), capture raw data and
machine parameters, import analyzer results with column mapping and well
coordinate matching.

**Workflow Requirements**: a. Conduct ELISA or Flow Cytometry (primary assays)
b. Capture raw data and machine parameters c. Note assay run ID, operator
identity, reagent lot numbers

**Independent Test**: Upload analyzer CSV with 10 results, verify column mapping
UI works, confirm all 10 results imported to correct samples matched by well
coordinate, verify assay run metadata is captured.

### Tests for User Story 5

- [ ] T092 [P] [US5] Create unit test for analyzer file parsing in
      `src/test/java/org/openelisglobal/notebook/service/AnalyzerImportServiceTest.java`
- [ ] T093 [P] [US5] Create integration test for import endpoint in
      `src/test/java/org/openelisglobal/notebook/controller/NotebookAnalyzerImportControllerTest.java`
- [ ] T094 [P] [US5] Create Cypress E2E test for analyzer import in
      `frontend/cypress/e2e/notebookWorkflowAnalyzerImport.cy.js`

### Implementation for User Story 5

- [x] T095 [US5] Extend AnalyzerResultImportService with CSV/Excel parsing
      methods (parseAnalyzerFile, mapColumns, validateResults)
- [x] T096 [US5] Add file format detection (CSV vs Excel) to
      AnalyzerResultImportServiceImpl
- [x] T097 [US5] Implement well coordinate matching (primary) via SampleRouting
      lookup
- [x] T098 [US5] Implement sample external_id fallback matching
- [x] T099 [US5] Implement result storage in NotebookPageSample.data JSONB
- [x] T100 [US5] Create NotebookAnalyzerImportController in
      `src/main/java/org/openelisglobal/notebook/controller/rest/NotebookAnalyzerImportController.java`
- [x] T101 [US5] Implement POST /notebook/bulk/page/{id}/analyzer-import
      endpoint
- [x] T102 [US5] Implement POST /notebook/bulk/page/{id}/analyzer-import/preview
      endpoint
- [x] T103 [US5] Implement GET
      /notebook/bulk/page/{id}/analyzer-import/{importId} endpoint
- [x] T104 [US5] Create AnalyzerResultImport audit record on each import
- [x] T104a [US5] Add assay run metadata fields (assayRunId, operatorId,
      machineParameters) to import record
- [x] T104b [US5] Implement reagent lot number tracking in import data

### Frontend for User Story 5

- [x] T105 [P] [US5] Create AnalyzerImportModal wizard component in
      `frontend/src/components/notebook/workflow/AnalyzerImportModal.js`
- [x] T106 [US5] Create AnalysisPage component (Page 6) in
      `frontend/src/components/notebook/pages/AnalysisPage.js`
- [x] T107 [US5] Implement column mapping UI in AnalyzerImportModal (Step 2)
- [x] T108 [US5] Implement preview table showing matched/unmatched samples
      (Step 3)
- [x] T109 [US5] Add error review modal for failed imports
- [x] T109a [US5] Add assay run metadata form (run ID, operator, machine params)
- [x] T109b [US5] Add reagent lot number input fields
- [x] T109c [US5] Display raw data and machine parameters in results grid

**Checkpoint**: User Story 5 complete - analyzer results can be imported with
assay metadata and well coordinate matching

---

## Phase 8: User Story 6 - Post-Analysis Handling (Priority: P2)

**Goal**: Store processed samples under defined conditions with tracked location
and retention period using existing SampleStorageService.

**Prerequisites**: US5 (Main Analysis Execution) must be complete - samples need
analyzer results before post-analysis storage assignment.

**Workflow Requirements**: a. Store processed samples under defined conditions
(refrigerated, frozen, etc.) b. Track storage location with hierarchical
navigation c. Set and track retention period for each sample

**Independent Test**: Select 10 samples, assign to storage location with
retention period (e.g., "Frozen, -80°C, 5 years"), verify
SampleStorageAssignment and SampleStorageMovement records created with correct
conditions.

### Tests for User Story 6

- [x] T110 [P] [US6] Create unit test for storage assignment in
      `src/test/java/org/openelisglobal/notebook/service/PostAnalysisStorageServiceTest.java`
- [ ] T111 [P] [US6] Create Cypress E2E test for storage assignment in
      `frontend/cypress/e2e/notebookWorkflowStorage.cy.js`

### Implementation for User Story 6

- [x] T112 [US6] Extend SampleRoutingService to handle STORAGE destination with
      SampleStorageService integration
- [x] T113 [US6] Implement bulk storage assignment with SampleStorageMovement
      audit trail
- [x] T114 [US6] Add retention period tracking to routing data
- [x] T114a [US6] Add storage condition types (REFRIGERATED, FROZEN_MINUS20,
      FROZEN_MINUS80, ROOM_TEMP, etc.)
- [x] T114b [US6] Implement retention period calculation and expiry date
      tracking
- [x] T115 [US6] Add storage location display to sample grid

### Frontend for User Story 6

- [x] T116 [P] [US6] Create StorageAssignmentForm component in
      `frontend/src/components/notebook/pages/StoragePage.js` (integrated into
      StoragePage modal)
- [x] T117 [US6] Create StoragePage component (Page 7) in
      `frontend/src/components/notebook/pages/StoragePage.js`
- [x] T118 [US6] Add storage location picker with hierarchical navigation
- [x] T118a [US6] Add storage condition selector (Refrigerated/Frozen/-80°C)
- [x] T118b [US6] Add retention period input with date calculation
- [x] T118c [US6] Display current storage status for each sample

**Checkpoint**: User Story 6 complete - samples can be assigned to storage
locations with conditions and retention tracking

---

## Phase 9: User Story 7 - Result Compilation & Dissemination (Priority: P2)

**Goal**: Compile analysis outputs into structured result files or database
records, deliver results to Data Management Team or designated recipients, and
flag invalid or inconclusive results for review.

**Workflow Requirements**: a. Compile outputs into structured result files or
database records b. Deliver results to the Data Management Team or designated
recipients c. Flag invalid or inconclusive results if applicable

**Independent Test**: Flag 3 samples as invalid with reasons, generate Excel
report, verify flagged samples are marked in report with status
(VALID/INVALID/INCONCLUSIVE) and delivery confirmation.

### Tests for User Story 7

- [x] T119 [P] [US7] Create unit test for result compilation in
      `src/test/java/org/openelisglobal/notebook/service/ResultCompilationServiceTest.java`
- [ ] T120 [P] [US7] Create Cypress E2E test for export workflow in
      `frontend/cypress/e2e/notebookWorkflowExport.cy.js`

### Implementation for User Story 7

- [x] T121 [US7] Create ResultCompilationService for statistics calculation in
      `src/main/java/org/openelisglobal/notebook/service/ResultCompilationService.java`
- [x] T122 [US7] Implement sample flagging (VALID, INVALID, INCONCLUSIVE) with
      reason storage in NotebookPageSample.data JSONB
- [x] T122a [US7] Add result validation status enum (VALID, INVALID,
      INCONCLUSIVE) in `ValidationStatus.java`
- [x] T122b [US7] Implement bulk flagging with reason for each sample
- [x] T123 [US7] Implement Excel report generation using Apache POI with
      structured output format
- [x] T123a [US7] Include all result data: sample ID, assay results, validation
      status, flags
- [x] T124 [US7] Implement report attachment to NoteBookFile for audit trail
- [x] T125 [US7] Add export endpoints to NotebookBulkOperationController
- [x] T125a [US7] Implement GET /notebook/bulk/notebook/{id}/export/excel
      endpoint
- [x] T125b [US7] Implement POST /notebook/bulk/notebook/{id}/deliver endpoint
      for recipient delivery
- [x] T125c [US7] Add recipient tracking (Data Management Team, external
      recipients)

### Frontend for User Story 7

- [x] T126 [P] [US7] Create FlagSampleModal component in
      `frontend/src/components/notebook/workflow/FlagSampleModal.js`
- [x] T126a [US7] Add validation status selector (Valid/Invalid/Inconclusive)
- [x] T126b [US7] Add reason text field for invalid/inconclusive samples
- [x] T127 [US7] Create ResultCompilationPage component (Page 8) in
      `frontend/src/components/notebook/pages/ResultCompilationPage.js`
- [x] T128 [US7] Add summary statistics dashboard (total, valid, invalid,
      inconclusive counts)
- [x] T128a [US7] Add visual indicators for flagged samples in grid
- [x] T129 [US7] Add export format selection (Excel, PDF, CSV)
- [x] T129a [US7] Add recipient selection for result delivery
- [x] T129b [US7] Add delivery confirmation tracking and display

**Checkpoint**: User Story 7 complete - results can be compiled, flagged,
delivered to recipients, and exported

---

## Phase 10: User Story 8 - End of Project Archiving (Priority: P3)

**Goal**: Once project concludes, transfer remaining Parent and Child Samples to
Biorepository Laboratory with permanent storage logged and complete traceability
links verified.

**Workflow Requirements**: a. Once project concludes: i. Transfer remaining
Parent and Child Samples to Biorepository Laboratory ii. Ensure permanent
storage is logged with complete traceability links

**Independent Test**: Transfer 10 samples (both parent and child) to
biorepository location, verify traceability checklist items (parent-child links,
movement history, storage assignments), confirm permanent storage is logged,
mark page complete, notebook status changes to FINALIZED.

### Tests for User Story 8

- [x] T130 [P] [US8] Create unit test for archiving workflow in
      `src/test/java/org/openelisglobal/notebook/service/ArchivingServiceTest.java`
- [x] T131 [P] [US8] Create Cypress E2E test for archiving in
      `frontend/cypress/e2e/notebookWorkflowArchiving.cy.js`

### Implementation for User Story 8

- [x] T132 [US8] Implement bulk transfer to Biorepository Laboratory with
      SampleStorageMovement records
- [x] T132a [US8] Add BIOREPOSITORY destination type to SampleRouting
- [x] T132b [US8] Implement transfer of both Parent and Child samples together
- [x] T133 [US8] Implement traceability verification (parent-child links,
      movement history, storage chain)
- [x] T133a [US8] Generate traceability report for all sample lineages
- [x] T133b [US8] Verify complete storage chain from reception to archive
- [x] T134 [US8] Implement permanent storage logging with immutable audit trail
- [x] T134a [US8] Mark samples as ARCHIVED with archive timestamp
- [x] T135 [US8] Implement notebook status transition to FINALIZED
- [x] T135a [US8] Prevent modifications to FINALIZED notebooks
- [x] T136 [US8] Add archiving endpoints to NotebookArchivingController
- [x] T136a [US8] Implement POST /notebook/{id}/archive/transfer endpoint
- [x] T136b [US8] Implement POST /notebook/{id}/archive/verify-traceability
      endpoint
- [x] T136c [US8] Implement POST /notebook/{id}/archive/finalize endpoint

### Frontend for User Story 8

- [x] T137 [P] [US8] Create TraceabilityChecklist component in
      `frontend/src/components/notebook/workflow/TraceabilityChecklist.js`
- [x] T137a [US8] Display parent-child relationship verification status
- [x] T137b [US8] Display movement history completeness status
- [x] T137c [US8] Display storage assignment verification status
- [x] T138 [US8] Create EndOfProjectArchivingPage component (Page 9) in
      `frontend/src/components/notebook/pages/EndOfProjectArchivingPage.js`
- [x] T138a [US8] Add Biorepository transfer form with location selection
- [x] T138b [US8] Add sample selection for parent and child samples
- [x] T139 [US8] Add "Submit for Final Review" button with status transition
- [x] T139a [US8] Add confirmation modal for finalization (irreversible action)
- [x] T139b [US8] Display finalization summary with all traceability data

**Checkpoint**: User Story 8 complete - notebooks can be finalized with samples
transferred to biorepository and complete traceability verified

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

### Bug Fixes (HIGH PRIORITY)

- [x] T149 [P] Fix page progress percentages with color-coded ranges in
      `PageNavigation.js` - Current: Only shows green (100%), blue
      (in-progress), gray (0%) - Required: Color-coded percentage ranges (e.g.,
      red <25%, orange 25-50%, yellow 50-75%, green 75-100%) - Affects:
      `frontend/src/components/notebook/workflow/PageNavigation.js` - Update
      `getStatusTag()` function to use percentage-based colors - Add CSS classes
      for new color ranges in `NotebookWorkflow.css` - **IMPLEMENTED**: Updated
      `getPercentageTagType()` function with ranges: 0%=gray, 1-24%=red,
      25-49%=orange, 50-74%=yellow, 75-99%=teal, 100%=green

- [x] T150 [P] Fix sample availability on proceeding pages after previous page
      completion - Current: Samples only appear on next page if linked to that
      page's NotebookPageSample - Required: Samples marked COMPLETED on previous
      page should automatically be available on the next page - Affects:
      Backend - need to check previous page completion status when loading
      samples - Option A: Auto-create NotebookPageSample records when sample
      completes on previous page - Option B: Query samples by checking previous
      page status in `/rest/notebook/bulk/page/{id}/samples` - Files:
      `NotebookBulkOperationController.java`, `NotebookPageSampleService.java` -
      **IMPLEMENTED**: Option A - Auto-create records in
      `NotebookPageSampleServiceImpl.bulkUpdateStatus()` when status=COMPLETED

### Polish Tasks

- [ ] T139 [P] Add remaining internationalization keys for all components (~100
      additional keys)
- [ ] T140 [P] Add @SafeHtml validation to all form text inputs
- [ ] T141 Performance optimization: verify bulk operations complete in <30s for
      200 samples
- [ ] T142 Performance optimization: verify grid rendering <100ms with
      virtualization
- [ ] T143 Add optimistic locking conflict handling ("Data modified by another
      user")
- [ ] T144 [P] Add additional unit tests for edge cases (coverage >70%)
- [ ] T145 Security review: verify RBAC for notebook operations
- [ ] T146 Run quickstart.md validation end-to-end
- [ ] T147 [P] Run mvn spotless:apply for code formatting
- [ ] T148 [P] Run frontend npm run format for code formatting

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user
  stories
- **User Stories (Phase 3-10)**: All depend on Foundational phase completion
  - US1 (P0): Must complete first - creates notebook instances
  - US2 (P1), US3 (P1), US4 (P1): Can proceed in parallel after US1
  - US5 (P2), US6 (P2), US7 (P2): Can proceed in parallel after US4 (routing
    needed)
  - US8 (P3): Can proceed after US6, US7 (needs storage and results)
- **Polish (Phase 11)**: Depends on all desired user stories being complete

### User Story Dependencies

| Story     | Depends On   | Can Parallel With          |
| --------- | ------------ | -------------------------- |
| US1 (P0)  | Foundational | None - must complete first |
| US2 (P1)  | US1          | US3, US3a, US4             |
| US3 (P1)  | US1          | US2, US3a, US4             |
| US3a (P1) | US3          | US2, US4 (after US3)       |
| US4 (P1)  | US1          | US2, US3, US3a             |
| US4a (P2) | US4          | US5, US6 (after US4)       |
| US5 (P2)  | US4a         | US6 (after US4a)           |
| US6 (P2)  | US5          | None (after US5)           |
| US7 (P2)  | US5          | US6 (after US5)            |
| US8 (P3)  | US6, US7     | None - final step          |

**Workflow Flow**:

1. **Reception** (US1, US2) → Link/create samples
2. **Processing** (US3) → Initial processing data
3. **Assays** (US3a) → Supplementary tests
4. **Child Samples + Routing** (US4) → Create children, route to destinations
5. **Prep** (US4a) → Prepare for analysis (fresh/thaw/incubate)
6. **Analysis** (US5) → Execute ELISA/Flow Cytometry, import results
7. **Storage** (US6) → Post-analysis storage with conditions
8. **Results** (US7) → Compile, flag, deliver to Data Management
9. **Archive** (US8) → Transfer to Biorepository, verify traceability, finalize

### Within Each User Story

1. Tests MUST be written and FAIL before implementation
2. Backend: DAO → Service → Controller (sequential)
3. Frontend: Components → Pages → Integration (can parallel components)
4. Story complete before moving to next priority

### Parallel Opportunities

- All Setup Liquibase tasks (T002-T005) can run in parallel
- All Foundational entity tasks (T013-T019) can run in parallel after T007-T012
- All Form classes (T020-T024) can run in parallel
- All test tasks marked [P] can run in parallel
- Different user stories can be worked on by different developers

---

## Parallel Example: Phase 2 Foundational

```bash
# After T001 (directory creation), launch Liquibase changesets in parallel:
Task: "T002 [P] Create Liquibase changeset for NotebookPageSample..."
Task: "T003 [P] Create Liquibase changeset for AnalyzerResultImport..."
Task: "T004 [P] Create Liquibase changeset for SampleRouting..."
Task: "T005 [P] Create Liquibase changeset to add page_type..."

# After T012 (NotebookPageSampleService), launch other entities in parallel:
Task: "T013 [P] Create SampleRouting.DestinationType enum..."
Task: "T017 [P] Create AnalyzerResultImport valueholder..."

# Launch all Form classes in parallel:
Task: "T020 [P] Create NotebookPageSampleForm..."
Task: "T021 [P] Create BulkApplyForm..."
Task: "T022 [P] Create SampleRoutingForm..."
Task: "T023 [P] Create AnalyzerImportForm..."
Task: "T024 [P] Create ManifestImportForm..."
```

## Parallel Example: User Story 4

```bash
# Launch all tests for User Story 4 together (TDD - must fail first):
Task: "T074 [P] [US4] Create unit test for child sample creation..."
Task: "T075 [P] [US4] Create unit test for well coordinate auto-assignment..."
Task: "T076 [P] [US4] Create Cypress E2E test for routing workflow..."

# Launch parallel frontend components:
Task: "T087 [P] [US4] Create SampleRoutingPanel..."
Task: "T088 [P] [US4] Create BoxLayoutViewer..."
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 Only)

1. Complete Phase 1: Setup (Liquibase changesets)
2. Complete Phase 2: Foundational (entities, DAOs, services)
3. Complete Phase 3: User Story 1 (notebook creation, sample linking)
4. Complete Phase 4: User Story 2 (sample reception, manifest import)
5. **STOP and VALIDATE**: Test notebook creation + sample import end-to-end
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test → Deploy (create notebooks, link samples)
3. Add User Story 2 → Test → Deploy (manifest import)
4. Add User Story 3 → Test → Deploy (bulk data entry)
5. Add User Story 4 → Test → Deploy (routing with well assignment)
6. Add User Story 5 → Test → Deploy (analyzer import)
7. Add User Story 6 + 7 → Test → Deploy (storage + export)
8. Add User Story 8 → Test → Deploy (archiving)
9. Polish phase → Final release

### Parallel Team Strategy

With multiple developers after Foundational is complete:

- Developer A: User Story 2 (sample reception)
- Developer B: User Story 3 (bulk operations)
- Developer C: User Story 4 (routing)

Stories complete and integrate independently.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests FAIL before implementing (TDD)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Run `mvn spotless:apply` before commits (backend)
- Run `npm run format` before commits (frontend)
- Use JUnit 4 (NOT JUnit 5) for tests
- Use jakarta.persistence (NOT javax.persistence)
- Individual E2E tests during development (NOT full suite)
