# Tasks: Analytical Laboratory Workflow

**Input**: Design documents from `/specs/281-analytical-lab-workflow/`
**Prerequisites**: plan.md (required), spec.md (required), research.md,
data-model.md, contracts/, quickstart.md

**Tests**: Include targeted unit/integration/Jest/Cypress tasks where needed to
satisfy acceptance criteria and constitution testing gate.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US5)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish analytical module scaffolding and migration entry points.

- [ ] T001 Add analytical changelog include to
      `src/main/resources/liquibase/base-changelog.xml`
- [ ] T002 Create `src/main/resources/liquibase/3.5.x.x/base.xml` with includes
      for analytical changesets
- [ ] T003 Create backend package scaffold under
      `src/main/java/org/openelisglobal/analytical/` (valueholder, dao, service,
      controller, form)
- [ ] T004 Create frontend scaffolding directories
      `frontend/src/pages/analytical/`, `frontend/src/components/analytical/`,
      `frontend/src/services/analytical/`, and
      `frontend/cypress/e2e/analytical/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before any user story.

- [ ] T005 Add analytical RBAC permissions/roles changelog
      `src/main/resources/liquibase/3.5.x.x/281-analytical-permissions.xml`
- [ ] T006 [P] Add notebook page types (TEST_ASSIGNMENT, ANALYSIS_EXECUTION,
      REPORTING, RETENTION_HANDLING) changelog
      `src/main/resources/liquibase/3.5.x.x/281-analytical-page-types.xml`
- [ ] T007 [P] Extend SampleItem fields (sample_type_detail, client metadata,
      external_identifier) changelog
      `src/main/resources/liquibase/3.5.x.x/281-sampleitem-analytical-fields.xml`
- [ ] T008 [P] Add analytical enums/constants (test types, methodologies) in
      `src/main/java/org/openelisglobal/analytical/valueholder/AnalyticalEnums.java`
- [ ] T009 Wire analytical route placeholders into `frontend/src/App.js`
      navigation
- [ ] T010 Seed base i18n stubs for analytical nav/labels in
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`

---

## Phase 3: User Story 1 - Sample Reception & Registration (Priority: P1) 🎯 MVP

**Goal**: Receive/link samples from medical lab or external clients; import
manifest CSV with validation; record metadata and completion per notebook page.

**Independent Test**: Register 5 samples from a manifest CSV with sample type
"tablet" and requested tests "Assay, Dissolution"; verify entries appear in
notebook with correct metadata and page completion moves to Test Assignment.

### Implementation & Tests

- [ ] T011 [P] [US1] Create manifest DTO/form
      `src/main/java/org/openelisglobal/analytical/form/AnalyticalManifestRowForm.java`
- [ ] T012 [P] [US1] Implement reception service (validate/import via bulk
      utilities)
      `src/main/java/org/openelisglobal/analytical/service/AnalyticalReceptionService.java`
- [ ] T013 [US1] Add reception REST controller
      `/rest/analytical/reception/manifest/*`
      `src/main/java/org/openelisglobal/analytical/controller/rest/AnalyticalReceptionController.java`
- [ ] T014 [US1] Add manifest mapping/validation helper
      `src/main/java/org/openelisglobal/analytical/service/mapper/AnalyticalManifestMapper.java`
- [ ] T015 [US1] Persist external identifiers and client metadata on SampleItem
      `src/main/java/org/openelisglobal/sample/valueholder/SampleItem.java`
- [ ] T016 [P] [US1] Build Reception page UI with link/import modes
      `frontend/src/pages/analytical/ReceptionPage.jsx`
- [ ] T017 [P] [US1] Build manifest upload panel with row-level errors
      `frontend/src/components/analytical/ManifestUploadPanel.jsx`
- [ ] T018 [US1] Add reception API hooks (validate/import, mark page complete)
      `frontend/src/services/analytical/useReceptionApi.js`
- [ ] T019 [US1] Add i18n strings for reception/import errors
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
- [ ] T020 [US1] JUnit test for manifest validation/import
      `src/test/java/org/openelisglobal/analytical/service/AnalyticalReceptionServiceTest.java`
- [ ] T021 [US1] Controller integration test for reception endpoints
      `src/test/java/org/openelisglobal/analytical/controller/AnalyticalReceptionControllerTest.java`
- [ ] T022 [US1] Jest test for reception page CSV handling
      `frontend/src/pages/analytical/__tests__/ReceptionPage.test.jsx`
- [ ] T023 [US1] Cypress scenario for manifest upload and page completion
      `frontend/cypress/e2e/analytical/reception.cy.js`

---

## Phase 4: User Story 2 - Test Assignment & Preparation (Priority: P1)

**Goal**: Assign tests to analysts with selected methodology and expected
completion; show workload summary; allow reassignment with audit.

**Independent Test**: Assign 10 samples to a Chemical Analyst for HPLC assay
testing and verify assignments appear in the analyst queue with methodology and
due date.

### Implementation & Tests

- [ ] T024 [P] [US2] Create AnalyticalTestAssignment entity + DAO
      `src/main/java/org/openelisglobal/analytical/valueholder/AnalyticalTestAssignment.java`
- [ ] T025 [US2] Add assignments table/indexes changelog
      `src/main/resources/liquibase/3.5.x.x/281-analytical-assignments.xml`
- [ ] T026 [US2] Implement assignment service (create/reassign/audit)
      `src/main/java/org/openelisglobal/analytical/service/AnalyticalAssignmentService.java`
- [ ] T027 [US2] Add assignment REST controller `/rest/analytical/assignments`
      `src/main/java/org/openelisglobal/analytical/controller/rest/AnalyticalAssignmentController.java`
- [ ] T028 [US2] Implement workload summary helper
      `src/main/java/org/openelisglobal/analytical/service/AnalyticalWorkloadService.java`
- [ ] T029 [P] [US2] Build Test Assignment page UI
      `frontend/src/pages/analytical/TestAssignmentPage.jsx`
- [ ] T030 [P] [US2] Build assignment grid/dialog component
      `frontend/src/components/analytical/AssignmentPanel.jsx`
- [ ] T031 [US2] Add assignment API hooks
      `frontend/src/services/analytical/useAssignmentsApi.js`
- [ ] T032 [US2] Add i18n strings for assignments/methodologies
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
- [ ] T033 [US2] JUnit test for assignment service (reassignment audit)
      `src/test/java/org/openelisglobal/analytical/service/AnalyticalAssignmentServiceTest.java`
- [ ] T034 [US2] Controller integration test for assignment endpoints
      `src/test/java/org/openelisglobal/analytical/controller/AnalyticalAssignmentControllerTest.java`
- [ ] T035 [US2] Jest test for workload summary and multi-test assignment
      `frontend/src/pages/analytical/__tests__/TestAssignmentPage.test.jsx`
- [ ] T036 [US2] Cypress scenario for assigning samples to analysts
      `frontend/cypress/e2e/analytical/assignment.cy.js`

---

## Phase 5: User Story 3 - Analysis Execution & Result Entry (Priority: P1)

**Goal**: Analysts view their assigned tests, enter results (manual or CSV
import), validate against specs, and flag OOS requiring investigation.

**Independent Test**: Import HPLC results for 20 samples from CSV; verify
matches by sequence ID, pass/fail calculation, and OOS flags block release.

### Implementation & Tests

- [ ] T037 [P] [US3] Create AnalyticalResult entity + DAO
      `src/main/java/org/openelisglobal/analytical/valueholder/AnalyticalResult.java`
- [ ] T038 [P] [US3] Create DissolutionResult entity + DAO
      `src/main/java/org/openelisglobal/analytical/valueholder/DissolutionResult.java`
- [ ] T039 [US3] Add results/dissolution tables changelog
      `src/main/resources/liquibase/3.5.x.x/281-analytical-results.xml`
- [ ] T040 [US3] Implement result service (spec validation, OOS flagging)
      `src/main/java/org/openelisglobal/analytical/service/AnalyticalResultService.java`
- [ ] T041 [US3] Implement instrument CSV import handler
      `src/main/java/org/openelisglobal/analytical/service/importer/AnalyticalResultImportService.java`
- [ ] T042 [US3] Add results REST controller `/rest/analytical/results`
      `src/main/java/org/openelisglobal/analytical/controller/rest/AnalyticalResultController.java`
- [ ] T043 [P] [US3] Build Analysis Execution page UI (assigned tests view)
      `frontend/src/pages/analytical/AnalysisExecutionPage.jsx`
- [ ] T044 [P] [US3] Build result entry/import components
      `frontend/src/components/analytical/ResultEntryPanel.jsx`
- [ ] T045 [US3] Add results API hooks (list/import/approval/OOS)
      `frontend/src/services/analytical/useResultsApi.js`
- [ ] T046 [US3] Add i18n strings for analysis/OOS messaging
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
- [ ] T047 [US3] JUnit test for result validation/OOS blocking
      `src/test/java/org/openelisglobal/analytical/service/AnalyticalResultServiceTest.java`
- [ ] T048 [US3] Controller integration test for results/import endpoints
      `src/test/java/org/openelisglobal/analytical/controller/AnalyticalResultControllerTest.java`
- [ ] T049 [US3] Jest test for result entry/import UI
      `frontend/src/pages/analytical/__tests__/AnalysisExecutionPage.test.jsx`
- [ ] T050 [US3] Cypress scenario for CSV import and OOS flagging
      `frontend/cypress/e2e/analytical/analysis.cy.js`

---

## Phase 6: User Story 4 - Reporting & Release (Priority: P2)

**Goal**: Generate analytical reports with results, specs, OOS dispositions,
signatures; approve and release to recipients with version history.

**Independent Test**: Generate a report for 10 samples with mixed tests; approve
and release to client; verify version saved and release metadata recorded.

### Implementation & Tests

- [ ] T051 [P] [US4] Create AnalyticalReport entity + DAO
      `src/main/java/org/openelisglobal/analytical/valueholder/AnalyticalReport.java`
- [ ] T052 [US4] Add reports table/versioning changelog
      `src/main/resources/liquibase/3.5.x.x/281-analytical-reports.xml`
- [ ] T053 [US4] Implement report generation/attachment service
      `src/main/java/org/openelisglobal/analytical/service/AnalyticalReportService.java`
- [ ] T054 [US4] Add reports REST controller `/rest/analytical/reports`
      `src/main/java/org/openelisglobal/analytical/controller/rest/AnalyticalReportController.java`
- [ ] T055 [P] [US4] Build Reporting page UI (approve/release)
      `frontend/src/pages/analytical/ReportingPage.jsx`
- [ ] T056 [P] [US4] Build report review/approval component
      `frontend/src/components/analytical/ReportReviewPanel.jsx`
- [ ] T057 [US4] Add report API hooks
      `frontend/src/services/analytical/useReportsApi.js`
- [ ] T058 [US4] Add i18n strings for reporting/release
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
- [ ] T059 [US4] JUnit test for report generation blocking OOS
      `src/test/java/org/openelisglobal/analytical/service/AnalyticalReportServiceTest.java`
- [ ] T060 [US4] Controller integration test for report release flow
      `src/test/java/org/openelisglobal/analytical/controller/AnalyticalReportControllerTest.java`
- [ ] T061 [US4] Jest test for reporting page approval/release
      `frontend/src/pages/analytical/__tests__/ReportingPage.test.jsx`
- [ ] T062 [US4] Cypress scenario for report generation and release
      `frontend/cypress/e2e/analytical/reporting.cy.js`

---

## Phase 7: User Story 5 - Post-Test Sample & Data Handling (Priority: P2)

**Goal**: Transfer retention samples to biorepository with retention metadata;
track chain of custody and flag expirations for disposition.

**Independent Test**: Transfer 5 retention samples with 2-year retention; verify
storage assignments, retention dates, and expiry flagging for review.

### Implementation & Tests

- [ ] T063 [P] [US5] Create RetentionSample entity + DAO
      `src/main/java/org/openelisglobal/analytical/valueholder/RetentionSample.java`
- [ ] T064 [US5] Add retention tables/indexes changelog
      `src/main/resources/liquibase/3.5.x.x/281-analytical-retention.xml`
- [ ] T065 [US5] Implement retention service (SampleStorageService integration)
      `src/main/java/org/openelisglobal/analytical/service/RetentionSampleService.java`
- [ ] T066 [US5] Add retention REST controller `/rest/analytical/retention`
      `src/main/java/org/openelisglobal/analytical/controller/rest/RetentionSampleController.java`
- [ ] T067 [P] [US5] Build Retention handling page UI
      `frontend/src/pages/analytical/RetentionPage.jsx`
- [ ] T068 [P] [US5] Build retention transfer form component
      `frontend/src/components/analytical/RetentionTransferPanel.jsx`
- [ ] T069 [US5] Add retention API hooks
      `frontend/src/services/analytical/useRetentionApi.js`
- [ ] T070 [US5] Add i18n strings for retention flow
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
- [ ] T071 [US5] JUnit test for retention expiry/disposition logic
      `src/test/java/org/openelisglobal/analytical/service/RetentionSampleServiceTest.java`
- [ ] T072 [US5] Controller integration test for retention transfer/disposition
      `src/test/java/org/openelisglobal/analytical/controller/RetentionSampleControllerTest.java`
- [ ] T073 [US5] Jest test for retention page flow
      `frontend/src/pages/analytical/__tests__/RetentionPage.test.jsx`
- [ ] T074 [US5] Cypress scenario for retention transfer and expiry review
      `frontend/cypress/e2e/analytical/retention.cy.js`

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Finalize quality, performance, and compliance across stories.

- [ ] T075 [P] Ensure en/fr Intl coverage for all analytical keys
      `frontend/src/languages/en.json` and `frontend/src/languages/fr.json`
- [ ] T076 Enforce formatting hooks per `pom.xml` and `frontend/package.json`
      (run spotless/prettier after changes)
- [ ] T077 Add audit logging hooks for analytical controllers
      `src/main/java/org/openelisglobal/analytical/controller/rest/`
- [ ] T078 Add index/optimizer changelog for analytical tables
      `src/main/resources/liquibase/3.5.x.x/281-analytical-indexes.xml`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1) → Foundational (Phase 2) → User Stories → Polish
- User Stories depend on Foundational completion.

### User Story Dependencies

- US1 (Reception, P1) → US2 (Assignment, P1) → US3 (Analysis, P1)
- US4 (Reporting, P2) depends on US3 results; US5 (Retention, P2) depends on US3
  completion data.

### Within Each User Story

- Tests should be authored before implementation tasks they cover.
- Models/entities before services; services before controllers; backend before
  UI hooks/pages; UI components before Cypress.

### Parallel Opportunities

- Tasks marked [P] can run in parallel (separate files): package scaffolds,
  enums, UI builds, entity creations, parallel backend/frontend per story.
- Different user stories can progress in parallel after Foundational if teams
  are distinct, but honor data dependencies noted above.

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Setup + Foundational (T001–T010).
2. Deliver US1 (T011–T023); verify manifest upload + page completion.
3. Demo MVP before expanding to assignments/results.

### Incremental Delivery

- After MVP, add US2 → US3 → US4/US5 in order, validating each story
  independently via listed tests.

### Parallel Team Strategy

- After Foundational, split teams: US1/US2 frontend vs backend in parallel;
  another team on US3 backend import logic; stagger US4/US5 once US3 stabilizes.
