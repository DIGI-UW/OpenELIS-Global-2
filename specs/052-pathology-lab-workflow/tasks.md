# Tasks: Pathology Laboratory Workflow

**Input**: Design documents from `/specs/052-pathology-lab-workflow/`
**Prerequisites**: plan.md, spec.md, data-model.md, research.md, quickstart.md

**Tests**: Included per Constitution Principle V (TDD required, >70% coverage
goal)

**Organization**: Tasks grouped by user story (10 stories from spec.md)

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1-US10) this task belongs to

## Path Conventions

- **Backend**: `src/main/java/org/openelisglobal/pathology/`
- **Tests**: `src/test/java/org/openelisglobal/pathology/`
- **Frontend**: `frontend/src/components/pathology/`
- **Migrations**: `src/main/resources/liquibase/pathology/`
- **i18n**: `frontend/src/languages/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, package structure, Liquibase registration

- [ ] T001 Create pathology package structure:
      `src/main/java/org/openelisglobal/pathology/{valueholder,dao,service,controller/rest,form}/`
- [ ] T002 Create test package structure:
      `src/test/java/org/openelisglobal/pathology/{service,controller,dao}/`
- [ ] T003 [P] Create frontend component directories:
      `frontend/src/components/pathology/{pages,components,__tests__}/`
- [ ] T004 [P] Create Liquibase directory:
      `src/main/resources/liquibase/pathology/`
- [ ] T005 Register pathology Liquibase files in master changelog:
      `src/main/resources/liquibase/liquibase-changeLog.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities and schema that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Schema (Liquibase)

- [ ] T006 Create Liquibase changeset for enumerations seed data:
      `src/main/resources/liquibase/pathology/pathology-000-enums-seed.xml`
- [ ] T007 [P] Create PathologySampleRegistration table:
      `src/main/resources/liquibase/pathology/pathology-001-sample-registration.xml`
- [ ] T008 [P] Create QualityControlRecord table:
      `src/main/resources/liquibase/pathology/pathology-002-quality-control.xml`
- [ ] T009 [P] Create ProcessingLogEntry table:
      `src/main/resources/liquibase/pathology/pathology-003-processing-log.xml`
- [ ] T010 [P] Create TestResultRecord table:
      `src/main/resources/liquibase/pathology/pathology-004-test-result.xml`
- [ ] T011 [P] Create StorageEnvironmentLog table + StorageDevice temp columns:
      `src/main/resources/liquibase/pathology/pathology-005-storage-environment.xml`
- [ ] T012 [P] Create ReferenceDocument table:
      `src/main/resources/liquibase/pathology/pathology-006-reference-document.xml`
- [ ] T013 [P] Create ProjectAccess table:
      `src/main/resources/liquibase/pathology/pathology-007-project-access.xml`
- [ ] T014 Create seed data (sample sources, QC criteria templates):
      `src/main/resources/liquibase/pathology/pathology-008-seed-data.xml`

### Enumerations

- [ ] T015 [P] Create SampleCategory enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/SampleCategory.java`
- [ ] T016 [P] Create QCType enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/QCType.java`
- [ ] T017 [P] Create QCStatus enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/QCStatus.java`
- [ ] T018 [P] Create QCAction enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/QCAction.java`
- [ ] T019 [P] Create TestType enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/TestType.java`
- [ ] T020 [P] Create StainQuality enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/StainQuality.java`
- [ ] T021 [P] Create ControlStatus enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/ControlStatus.java`
- [ ] T022 [P] Create DocumentType enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/DocumentType.java`
- [ ] T023 [P] Create AccessRole enum:
      `src/main/java/org/openelisglobal/pathology/valueholder/AccessRole.java`

### JPA Entities

- [ ] T024 [P] Create PathologySampleRegistration entity:
      `src/main/java/org/openelisglobal/pathology/valueholder/PathologySampleRegistration.java`
- [ ] T025 [P] Create QualityControlRecord entity:
      `src/main/java/org/openelisglobal/pathology/valueholder/QualityControlRecord.java`
- [ ] T026 [P] Create ProcessingLogEntry entity:
      `src/main/java/org/openelisglobal/pathology/valueholder/ProcessingLogEntry.java`
- [ ] T027 [P] Create TestResultRecord entity:
      `src/main/java/org/openelisglobal/pathology/valueholder/TestResultRecord.java`
- [ ] T028 [P] Create StorageEnvironmentLog entity (@Immutable):
      `src/main/java/org/openelisglobal/pathology/valueholder/StorageEnvironmentLog.java`
- [ ] T029 [P] Create ReferenceDocument entity:
      `src/main/java/org/openelisglobal/pathology/valueholder/ReferenceDocument.java`
- [ ] T030 [P] Create ProjectAccess entity:
      `src/main/java/org/openelisglobal/pathology/valueholder/ProjectAccess.java`

### ORM Validation

- [ ] T031 Create HibernateMappingValidationTest for all pathology entities:
      `src/test/java/org/openelisglobal/pathology/HibernateMappingValidationTest.java`
- [ ] T032 Run ORM validation test, fix any mapping errors

### Base DAOs

- [ ] T033 [P] Create PathologySampleRegistrationDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/PathologySampleRegistrationDAO.java`
- [ ] T034 [P] Create PathologySampleRegistrationDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/PathologySampleRegistrationDAOImpl.java`
- [ ] T035 [P] Create QualityControlRecordDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/QualityControlRecordDAO.java`
- [ ] T036 [P] Create QualityControlRecordDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/QualityControlRecordDAOImpl.java`
- [ ] T037 [P] Create ProcessingLogEntryDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/ProcessingLogEntryDAO.java`
- [ ] T038 [P] Create ProcessingLogEntryDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/ProcessingLogEntryDAOImpl.java`
- [ ] T039 [P] Create TestResultRecordDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/TestResultRecordDAO.java`
- [ ] T040 [P] Create TestResultRecordDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/TestResultRecordDAOImpl.java`
- [ ] T041 [P] Create StorageEnvironmentLogDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/StorageEnvironmentLogDAO.java`
- [ ] T042 [P] Create StorageEnvironmentLogDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/StorageEnvironmentLogDAOImpl.java`
- [ ] T043 [P] Create ReferenceDocumentDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/ReferenceDocumentDAO.java`
- [ ] T044 [P] Create ReferenceDocumentDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/ReferenceDocumentDAOImpl.java`
- [ ] T045 [P] Create ProjectAccessDAO interface:
      `src/main/java/org/openelisglobal/pathology/dao/ProjectAccessDAO.java`
- [ ] T046 [P] Create ProjectAccessDAOImpl:
      `src/main/java/org/openelisglobal/pathology/dao/ProjectAccessDAOImpl.java`

### Frontend Foundation

- [ ] T047 Create PathologyDashboard component:
      `frontend/src/components/pathology/PathologyDashboard.js`
- [ ] T048 [P] Add pathology route to main router
- [ ] T049 [P] Add base pathology i18n keys (pathology.dashboard.\*) to:
      `frontend/src/languages/en.json`
- [ ] T050 [P] Add base pathology i18n keys (pathology.dashboard.\*) to:
      `frontend/src/languages/fr.json`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Sample Reception & Registration (Priority: P0) 🎯 MVP

**Goal**: Data clerks can receive samples from Alert Hospital or research
projects, register with metadata, get unique accession numbers

**Independent Test**: Register 5 clinical + 5 research samples, verify accession
numbers and metadata

### Tests for User Story 1

- [ ] T051 [P] [US1] Write unit test for PathologySampleRegistrationService:
      `src/test/java/org/openelisglobal/pathology/service/PathologySampleRegistrationServiceTest.java`
- [ ] T052 [P] [US1] Write integration test for PathologySampleController:
      `src/test/java/org/openelisglobal/pathology/controller/PathologySampleControllerTest.java`
- [ ] T053 [P] [US1] Write Jest test for SampleReceptionPage:
      `frontend/src/components/pathology/__tests__/SampleReceptionPage.test.jsx`

### Implementation for User Story 1

- [ ] T054 [US1] Create PathologySampleRegistrationService interface:
      `src/main/java/org/openelisglobal/pathology/service/PathologySampleRegistrationService.java`
- [ ] T055 [US1] Implement PathologySampleRegistrationServiceImpl with
      registerSample(), searchSamples():
      `src/main/java/org/openelisglobal/pathology/service/PathologySampleRegistrationServiceImpl.java`
- [ ] T056 [US1] Create PathologySampleForm DTO:
      `src/main/java/org/openelisglobal/pathology/form/PathologySampleForm.java`
- [ ] T057 [US1] Create PathologySampleController with POST
      /rest/pathology/samples, GET /search:
      `src/main/java/org/openelisglobal/pathology/controller/rest/PathologySampleController.java`
- [ ] T058 [P] [US1] Create ClinicalSpecimenForm component:
      `frontend/src/components/pathology/components/ClinicalSpecimenForm.js`
- [ ] T059 [P] [US1] Create ResearchSpecimenForm component:
      `frontend/src/components/pathology/components/ResearchSpecimenForm.js`
- [ ] T060 [US1] Create SampleReceptionPage with category toggle, form fields,
      "Copy from Previous":
      `frontend/src/components/pathology/pages/SampleReceptionPage.js`
- [ ] T061 [P] [US1] Add sample reception i18n keys (pathology.reception._,
      pathology.category._) to en.json
- [ ] T062 [P] [US1] Add sample reception i18n keys to fr.json
- [ ] T063 [US1] Create Cypress E2E test for sample reception:
      `frontend/cypress/e2e/pathologySampleReception.cy.js`

**Checkpoint**: Users can register clinical/research samples with "Alert
Hospital" default and unique accession numbers

---

## Phase 4: User Story 2 - Sample Quality Control (Initial Inspection) (Priority: P1)

**Goal**: Lab technicians perform QC inspection with specimen-type-specific
criteria, Pass/Fail status

**Independent Test**: Process 10 samples through QC, 8 Pass, 2 Fail with
documented reasons

### Tests for User Story 2

- [ ] T064 [P] [US2] Write unit test for QualityControlService:
      `src/test/java/org/openelisglobal/pathology/service/QualityControlServiceTest.java`
- [ ] T065 [P] [US2] Write integration test for QualityControlController:
      `src/test/java/org/openelisglobal/pathology/controller/QualityControlControllerTest.java`
- [ ] T066 [P] [US2] Write Jest test for QualityControlPage:
      `frontend/src/components/pathology/__tests__/QualityControlPage.test.jsx`

### Implementation for User Story 2

- [ ] T067 [US2] Create QualityControlService interface:
      `src/main/java/org/openelisglobal/pathology/service/QualityControlService.java`
- [ ] T068 [US2] Implement QualityControlServiceImpl with recordQC(),
      getQCBySample():
      `src/main/java/org/openelisglobal/pathology/service/QualityControlServiceImpl.java`
- [ ] T069 [US2] Create QualityControlForm DTO:
      `src/main/java/org/openelisglobal/pathology/form/QualityControlForm.java`
- [ ] T070 [US2] Create QualityControlController with POST/GET for QC records:
      `src/main/java/org/openelisglobal/pathology/controller/rest/QualityControlController.java`
- [ ] T071 [P] [US2] Create QCChecklist component (specimen-type-specific
      criteria): `frontend/src/components/pathology/components/QCChecklist.js`
- [ ] T072 [US2] Create QualityControlPage with Pass/Fail, failure reason,
      action taken:
      `frontend/src/components/pathology/pages/QualityControlPage.js`
- [ ] T073 [P] [US2] Add QC i18n keys (pathology.qc.\*) to en.json
- [ ] T074 [P] [US2] Add QC i18n keys to fr.json
- [ ] T075 [US2] Create Cypress E2E test for QC:
      `frontend/cypress/e2e/pathologyQualityControl.cy.js`

**Checkpoint**: Lab technicians can perform QC with dynamic checklists, record
Pass/Fail with reasons

---

## Phase 5: User Story 3 - Tissue Block QC (Post-Embedding) (Priority: P1)

**Goal**: QC for embedded tissue blocks - surface, depth, orientation, paraffin

**Independent Test**: Evaluate 10 blocks, 8 Pass, 2 Fail with remediation
actions

### Tests for User Story 3

- [ ] T076 [P] [US3] Write unit test for block QC methods in
      QualityControlServiceTest
- [ ] T077 [P] [US3] Write Jest test for BlockQCPage:
      `frontend/src/components/pathology/__tests__/BlockQCPage.test.jsx`

### Implementation for User Story 3

- [ ] T078 [US3] Extend QualityControlServiceImpl with recordBlockQC(),
      getBlockQCHistory():
      `src/main/java/org/openelisglobal/pathology/service/QualityControlServiceImpl.java`
- [ ] T079 [P] [US3] Create BlockQCForm component (block-specific criteria):
      `frontend/src/components/pathology/components/BlockQCForm.js`
- [ ] T080 [US3] Create BlockQCPage with remediation options (Re-embed, Melt and
      re-embed): `frontend/src/components/pathology/pages/BlockQCPage.js`
- [ ] T081 [P] [US3] Add block QC i18n keys (pathology.blockqc.\*) to en.json
- [ ] T082 [P] [US3] Add block QC i18n keys to fr.json
- [ ] T083 [US3] Extend Cypress E2E test for block QC:
      `frontend/cypress/e2e/pathologyQualityControl.cy.js`

**Checkpoint**: Tissue blocks can be QC'd with block-specific criteria and
remediation workflow

---

## Phase 6: User Story 4 - Sample Processing (Grossing & Aliquoting) (Priority: P1)

**Goal**: Grossing, aliquoting, parent-child sample hierarchy tracking

**Independent Test**: Gross 5 tissue samples into blocks, aliquot 5 fluids,
verify parent-child relationships

### Tests for User Story 4

- [ ] T084 [P] [US4] Write unit test for ProcessingLogService:
      `src/test/java/org/openelisglobal/pathology/service/ProcessingLogServiceTest.java`
- [ ] T085 [P] [US4] Write integration test for ProcessingLogController:
      `src/test/java/org/openelisglobal/pathology/controller/ProcessingLogControllerTest.java`
- [ ] T086 [P] [US4] Write Jest test for ProcessingPage:
      `frontend/src/components/pathology/__tests__/ProcessingPage.test.jsx`

### Implementation for User Story 4

- [ ] T087 [US4] Create ProcessingLogService interface:
      `src/main/java/org/openelisglobal/pathology/service/ProcessingLogService.java`
- [ ] T088 [US4] Implement ProcessingLogServiceImpl with recordStep(),
      createChildSample():
      `src/main/java/org/openelisglobal/pathology/service/ProcessingLogServiceImpl.java`
- [ ] T089 [US4] Create ProcessingLogForm DTO:
      `src/main/java/org/openelisglobal/pathology/form/ProcessingLogForm.java`
- [ ] T090 [US4] Create ProcessingLogController:
      `src/main/java/org/openelisglobal/pathology/controller/rest/ProcessingLogController.java`
- [ ] T091 [P] [US4] Create GrossingForm component:
      `frontend/src/components/pathology/components/GrossingForm.js`
- [ ] T092 [P] [US4] Create AliquotForm component:
      `frontend/src/components/pathology/components/AliquotForm.js`
- [ ] T093 [US4] Create ProcessingPage with grossing/aliquoting tabs,
      parent-child display:
      `frontend/src/components/pathology/pages/ProcessingPage.js`
- [ ] T094 [P] [US4] Add processing i18n keys (pathology.processing.\*) to
      en.json
- [ ] T095 [P] [US4] Add processing i18n keys to fr.json
- [ ] T096 [US4] Create Cypress E2E test for processing:
      `frontend/cypress/e2e/pathologyProcessing.cy.js`

**Checkpoint**: Samples can be processed with full parent-child hierarchy
tracking

---

## Phase 7: User Story 5 - Testing & Microscopy (Priority: P1)

**Goal**: Staining procedures, control validation, pathologist sign-off

**Independent Test**: H&E on 10 slides, IHC on 5 with controls, verify results
and sign-off

### Tests for User Story 5

- [ ] T097 [P] [US5] Write unit test for TestResultService:
      `src/test/java/org/openelisglobal/pathology/service/TestResultServiceTest.java`
- [ ] T098 [P] [US5] Write integration test for TestResultController:
      `src/test/java/org/openelisglobal/pathology/controller/TestResultControllerTest.java`
- [ ] T099 [P] [US5] Write Jest test for TestingPage:
      `frontend/src/components/pathology/__tests__/TestingPage.test.jsx`

### Implementation for User Story 5

- [ ] T100 [US5] Create TestResultService interface:
      `src/main/java/org/openelisglobal/pathology/service/TestResultService.java`
- [ ] T101 [US5] Implement TestResultServiceImpl with recordResult(),
      validateControls(), signOff():
      `src/main/java/org/openelisglobal/pathology/service/TestResultServiceImpl.java`
- [ ] T102 [US5] Create TestResultForm DTO:
      `src/main/java/org/openelisglobal/pathology/form/TestResultForm.java`
- [ ] T103 [US5] Create TestResultController:
      `src/main/java/org/openelisglobal/pathology/controller/rest/TestResultController.java`
- [ ] T104 [P] [US5] Create TestResultForm component (test-type-specific
      fields): `frontend/src/components/pathology/components/TestResultForm.js`
- [ ] T105 [P] [US5] Create ControlValidation component (IHC control checker):
      `frontend/src/components/pathology/components/ControlValidation.js`
- [ ] T106 [US5] Create TestingPage with stain selection, controls, results,
      sign-off: `frontend/src/components/pathology/pages/TestingPage.js`
- [ ] T107 [P] [US5] Add testing i18n keys (pathology.testing.\*) to en.json
- [ ] T108 [P] [US5] Add testing i18n keys to fr.json
- [ ] T109 [US5] Create Cypress E2E test for testing:
      `frontend/cypress/e2e/pathologyTesting.cy.js`

**Checkpoint**: Tests can be recorded with control validation and pathologist
sign-off

---

## Phase 8: User Story 6 - Storage & Inventory Management (Priority: P2)

**Goal**: Sample storage with temp suggestions, location tracking, movements

**Independent Test**: Store 10 samples across temps, retrieve 2, verify location
logbook

### Tests for User Story 6

- [ ] T110 [P] [US6] Write unit test for StorageEnvironmentService:
      `src/test/java/org/openelisglobal/pathology/service/StorageEnvironmentServiceTest.java`
- [ ] T111 [P] [US6] Write integration test for StorageEnvironmentController:
      `src/test/java/org/openelisglobal/pathology/controller/StorageEnvironmentControllerTest.java`
- [ ] T112 [P] [US6] Write Jest test for StoragePage:
      `frontend/src/components/pathology/__tests__/StoragePage.test.jsx`

### Implementation for User Story 6

- [ ] T113 [US6] Create StorageEnvironmentService interface:
      `src/main/java/org/openelisglobal/pathology/service/StorageEnvironmentService.java`
- [ ] T114 [US6] Implement StorageEnvironmentServiceImpl with
      recordTemperature(), detectExcursion():
      `src/main/java/org/openelisglobal/pathology/service/StorageEnvironmentServiceImpl.java`
- [ ] T115 [US6] Create StorageEnvironmentController:
      `src/main/java/org/openelisglobal/pathology/controller/rest/StorageEnvironmentController.java`
- [ ] T116 [P] [US6] Create TemperatureLogTable component:
      `frontend/src/components/pathology/components/TemperatureLogTable.js`
- [ ] T117 [US6] Create StoragePage with location assignment, temp logging,
      excursion alerts: `frontend/src/components/pathology/pages/StoragePage.js`
- [ ] T118 [P] [US6] Add storage i18n keys (pathology.storage.\*) to en.json
- [ ] T119 [P] [US6] Add storage i18n keys to fr.json
- [ ] T120 [US6] Create Cypress E2E test for storage:
      `frontend/cypress/e2e/pathologyStorage.cy.js`

**Checkpoint**: Samples can be stored with temp monitoring and excursion
detection

---

## Phase 9: User Story 7 - Tracking & Performance Monitoring (Priority: P2)

**Goal**: Monthly reports with volume, TAT, rejection rate, assay success rate

**Independent Test**: Generate monthly report for 100 samples, verify all
metrics

### Tests for User Story 7

- [ ] T121 [P] [US7] Write unit test for PathologyReportingService:
      `src/test/java/org/openelisglobal/pathology/service/PathologyReportingServiceTest.java`
- [ ] T122 [P] [US7] Write Jest test for PerformancePage:
      `frontend/src/components/pathology/__tests__/PerformancePage.test.jsx`

### Implementation for User Story 7

- [ ] T123 [US7] Create PathologyReportingService interface:
      `src/main/java/org/openelisglobal/pathology/service/PathologyReportingService.java`
- [ ] T124 [US7] Implement PathologyReportingServiceImpl with
      calculateMonthlyMetrics(), exportReport():
      `src/main/java/org/openelisglobal/pathology/service/PathologyReportingServiceImpl.java`
- [ ] T125 [US7] Add reporting endpoint to PathologySampleController or create
      PathologyReportController
- [ ] T126 [US7] Create PerformancePage with date range, metrics display,
      export: `frontend/src/components/pathology/pages/PerformancePage.js`
- [ ] T127 [P] [US7] Add reporting i18n keys (pathology.reports.\*) to en.json
- [ ] T128 [P] [US7] Add reporting i18n keys to fr.json
- [ ] T129 [US7] Create Cypress E2E test for reports:
      `frontend/cypress/e2e/pathologyReports.cy.js`

**Checkpoint**: Supervisors can generate monthly performance reports with all
metrics

---

## Phase 10: User Story 8 - Disposal & Archiving (Priority: P3)

**Goal**: Retention policy enforcement, disposal logging, archiving

**Independent Test**: Mark 5 clinical samples for disposal, archive logbooks

### Tests for User Story 8

- [ ] T130 [P] [US8] Write unit test for disposal methods in
      PathologySampleRegistrationServiceTest
- [ ] T131 [P] [US8] Write Jest test for DisposalPage:
      `frontend/src/components/pathology/__tests__/DisposalPage.test.jsx`

### Implementation for User Story 8

- [ ] T132 [US8] Extend PathologySampleRegistrationServiceImpl with
      markForDisposal(), recordDisposal():
      `src/main/java/org/openelisglobal/pathology/service/PathologySampleRegistrationServiceImpl.java`
- [ ] T133 [US8] Add disposal endpoints to PathologySampleController
- [ ] T134 [US8] Create DisposalPage with retention check, disposal form,
      archive recording:
      `frontend/src/components/pathology/pages/DisposalPage.js`
- [ ] T135 [P] [US8] Add disposal i18n keys (pathology.disposal.\*) to en.json
- [ ] T136 [P] [US8] Add disposal i18n keys to fr.json
- [ ] T137 [US8] Create Cypress E2E test for disposal:
      `frontend/cypress/e2e/pathologyDisposal.cy.js`

**Checkpoint**: Samples can be disposed with retention policy enforcement and
audit trail

---

## Phase 11: User Story 9 - Reference Module & SOP Management (Priority: P2)

**Goal**: SOP viewing, upload (authorized users), version history

**Independent Test**: Upload SOP, verify all users view, upload new version,
verify history

### Tests for User Story 9

- [ ] T138 [P] [US9] Write unit test for ReferenceDocumentService:
      `src/test/java/org/openelisglobal/pathology/service/ReferenceDocumentServiceTest.java`
- [ ] T139 [P] [US9] Write integration test for ReferenceDocumentController:
      `src/test/java/org/openelisglobal/pathology/controller/ReferenceDocumentControllerTest.java`
- [ ] T140 [P] [US9] Write Jest test for ReferenceModulePage:
      `frontend/src/components/pathology/__tests__/ReferenceModulePage.test.jsx`

### Implementation for User Story 9

- [ ] T141 [US9] Create ReferenceDocumentService interface:
      `src/main/java/org/openelisglobal/pathology/service/ReferenceDocumentService.java`
- [ ] T142 [US9] Implement ReferenceDocumentServiceImpl with uploadDocument(),
      getVersionHistory():
      `src/main/java/org/openelisglobal/pathology/service/ReferenceDocumentServiceImpl.java`
- [ ] T143 [US9] Create ReferenceDocumentForm DTO:
      `src/main/java/org/openelisglobal/pathology/form/ReferenceDocumentForm.java`
- [ ] T144 [US9] Create ReferenceDocumentController with upload, list, download:
      `src/main/java/org/openelisglobal/pathology/controller/rest/ReferenceDocumentController.java`
- [ ] T145 [P] [US9] Create DocumentUploader component:
      `frontend/src/components/pathology/components/DocumentUploader.js`
- [ ] T146 [P] [US9] Create DocumentVersionHistory component:
      `frontend/src/components/pathology/components/DocumentVersionHistory.js`
- [ ] T147 [US9] Create ReferenceModulePage with document list, upload, version
      history: `frontend/src/components/pathology/pages/ReferenceModulePage.js`
- [ ] T148 [P] [US9] Add reference module i18n keys (pathology.reference.\*) to
      en.json
- [ ] T149 [P] [US9] Add reference module i18n keys to fr.json
- [ ] T150 [US9] Create Cypress E2E test for reference module:
      `frontend/cypress/e2e/pathologyReferenceModule.cy.js`

**Checkpoint**: SOPs can be uploaded with version control and viewed by all
users

---

## Phase 12: User Story 10 - Project-Based Access Control (Priority: P2)

**Goal**: Restrict research sample access to designated personnel

**Independent Test**: Create project with 10 samples, assign 3 users, verify
others blocked

### Tests for User Story 10

- [ ] T151 [P] [US10] Write unit test for ProjectAccessService:
      `src/test/java/org/openelisglobal/pathology/service/ProjectAccessServiceTest.java`
- [ ] T152 [P] [US10] Write integration test for ProjectAccessController:
      `src/test/java/org/openelisglobal/pathology/controller/ProjectAccessControllerTest.java`
- [ ] T153 [P] [US10] Write Jest test for project access management

### Implementation for User Story 10

- [ ] T154 [US10] Create ProjectAccessService interface:
      `src/main/java/org/openelisglobal/pathology/service/ProjectAccessService.java`
- [ ] T155 [US10] Implement ProjectAccessServiceImpl with grantAccess(),
      revokeAccess(), hasAccess():
      `src/main/java/org/openelisglobal/pathology/service/ProjectAccessServiceImpl.java`
- [ ] T156 [US10] Create ProjectAccessController:
      `src/main/java/org/openelisglobal/pathology/controller/rest/ProjectAccessController.java`
- [ ] T157 [US10] Integrate access filtering into
      PathologySampleRegistrationServiceImpl.searchSamples()
- [ ] T158 [US10] Add project access management UI to PathologyDashboard or
      create dedicated admin page
- [ ] T159 [P] [US10] Add access control i18n keys (pathology.access.\*) to
      en.json
- [ ] T160 [P] [US10] Add access control i18n keys to fr.json
- [ ] T161 [US10] Create Cypress E2E test for access control (verify filtering)

**Checkpoint**: Research samples are only visible to authorized project members

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Improvements affecting multiple user stories

- [ ] T162 [P] Run mvn spotless:apply for backend formatting
- [ ] T163 [P] Run npm run format for frontend formatting
- [ ] T164 [P] Review and consolidate i18n keys for consistency
- [ ] T165 Run full test suite (mvn test)
- [ ] T166 Run frontend tests (npm test)
- [ ] T167 Run full E2E suite:
      `npm run cy:run -- --spec "cypress/e2e/pathology*.cy.js"`
- [ ] T168 Performance validation: verify sample registration <5s, QC <5s,
      storage search <2s
- [ ] T169 Security review: RBAC, input validation, audit trail
- [ ] T170 Run quickstart.md validation with fresh clone

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) ──────────────────────────────────────────┐
                                                          ▼
Phase 2 (Foundational) ◄── BLOCKS ALL USER STORIES ──────┤
           │                                              │
           ▼                                              │
┌──────────┼──────────┬──────────┬──────────┬───────────┐ │
│          │          │          │          │           │ │
▼          ▼          ▼          ▼          ▼           ▼ │
Phase 3    Phase 4    Phase 5    Phase 6    Phase 7   ... │ (User Stories)
US1 (P0)   US2 (P1)   US3 (P1)   US4 (P1)   US5 (P1)     │
   │          │          │          │          │          │
   └──────────┴──────────┴──────────┴──────────┘          │
                         │                                 │
                         ▼                                 │
                  Phase 13 (Polish) ◄─────────────────────┘
```

### User Story Dependencies

| Story                  | Priority | Dependencies                      | Notes                                   |
| ---------------------- | -------- | --------------------------------- | --------------------------------------- |
| US1 (Sample Reception) | P0       | Foundational                      | MVP - start here                        |
| US2 (Initial QC)       | P1       | Foundational                      | Depends on samples from US1 for testing |
| US3 (Block QC)         | P1       | Foundational, extends US2 service | Reuses QualityControlService            |
| US4 (Processing)       | P1       | Foundational                      | Uses parent-child sample patterns       |
| US5 (Testing)          | P1       | Foundational                      | May reference processing from US4       |
| US6 (Storage)          | P2       | Foundational                      | Standalone, uses existing storage infra |
| US7 (Reports)          | P2       | US1, US2, US5                     | Needs data from other stories           |
| US8 (Disposal)         | P3       | US1, US6                          | Extends sample and storage              |
| US9 (Reference)        | P2       | Foundational                      | Standalone, no sample deps              |
| US10 (Access)          | P2       | Foundational                      | Integrates with US1 search              |

### Within Each User Story

1. Tests (T0XX) MUST be written FIRST and FAIL before implementation
2. Service interface before implementation
3. Service implementation before controller
4. Controller before frontend page
5. i18n keys before frontend (or parallel)
6. E2E test after page complete

### Parallel Opportunities

**Setup Phase (T001-T005)**: All tasks can run in parallel

**Foundational Phase (T006-T050)**:

- All Liquibase tasks (T006-T014) can run in parallel
- All enum tasks (T015-T023) can run in parallel
- All entity tasks (T024-T030) can run in parallel
- All DAO tasks (T033-T046) can run in parallel
- ORM validation (T031-T032) must wait for entities

**User Story Phases**:

- All test tasks [P] within a story can run in parallel
- Multiple user stories can be worked on by different developers simultaneously
  after Phase 2

---

## Parallel Example: Foundational Phase

```bash
# Launch all entity creations in parallel:
Task: "Create PathologySampleRegistration entity in valueholder/"
Task: "Create QualityControlRecord entity in valueholder/"
Task: "Create ProcessingLogEntry entity in valueholder/"
Task: "Create TestResultRecord entity in valueholder/"
Task: "Create StorageEnvironmentLog entity in valueholder/"
Task: "Create ReferenceDocument entity in valueholder/"
Task: "Create ProjectAccess entity in valueholder/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Sample Reception)
4. **STOP and VALIDATE**: Register samples, verify accession numbers
5. Deploy/demo if ready

### Incremental Delivery by Priority

1. Setup + Foundational → Foundation ready
2. Add US1 (P0) → Demo sample registration ✓
3. Add US2, US3, US4, US5 (P1) → Core workflow complete ✓
4. Add US6, US7, US9, US10 (P2) → Storage, reports, SOPs, access ✓
5. Add US8 (P3) → Disposal/archiving ✓
6. Polish → Production ready

### Parallel Team Strategy

With multiple developers after Phase 2:

- Developer A: US1 → US4 → US7
- Developer B: US2 → US5 → US8
- Developer C: US3 → US6 → US9
- Developer D: US10 (access control - affects search)

---

## Summary

| Metric               | Value                           |
| -------------------- | ------------------------------- |
| Total Tasks          | 170                             |
| Setup Phase          | 5 tasks                         |
| Foundational Phase   | 45 tasks                        |
| User Stories         | 10 stories, 120 tasks           |
| Polish Phase         | 9 tasks                         |
| Parallelizable Tasks | ~90 (53%)                       |
| MVP Scope            | Phase 1-3 (US1 only, ~63 tasks) |

---

## Notes

- [P] tasks = different files, no dependencies within phase
- [Story] label maps task to specific user story for traceability
- Each user story is independently testable after implementation
- Verify tests FAIL before implementing (TDD)
- Run `mvn spotless:apply` and `npm run format` before commits
- Stop at any checkpoint to validate story independently
