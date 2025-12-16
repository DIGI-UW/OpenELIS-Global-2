# Tasks: Medical Laboratory Workflow

**Input**: Design documents from `/specs/001-medical-lab-workflow/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/, research.md,
quickstart.md

**Tests**: TDD workflow required per constitution V. Tests included for each
user story.

**Organization**: Tasks grouped by user story to enable independent
implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend (new module)**: `src/main/java/org/openelisglobal/medlab/`
- **Frontend (notebook pages)**: `frontend/src/components/notebook/pages/`
- **Frontend (workflow)**: `frontend/src/components/notebook/workflow/`
- **Tests (Backend)**: `src/test/java/org/openelisglobal/medlab/`
- **Tests (Frontend)**: `frontend/cypress/e2e/`
- **Liquibase**: `src/main/resources/liquibase/3.x.x/`
- **i18n**: `frontend/src/languages/`

## Reuse Strategy

This implementation follows a **reuse-first approach**:

- **NEW**: Only create genuinely new components (8 frontend pages, 4 backend
  entities)
- **EXTEND**: Modify existing components to add medlab-specific features
- **EMBED**: Wrap existing components within notebook pages
- **REUSE**: Use existing components as-is

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create medlab module structure and notebook template configuration

- [ ] T001 Create medlab module package structure at
      src/main/java/org/openelisglobal/medlab/ with subdirectories:
      valueholder/, dao/, service/, controller/rest/, form/
- [ ] T002 [P] Create MedLab notebook template configuration in NoteBookService
      defining 16-page workflow sequence
- [ ] T003 [P] Add medlab notebook route in frontend/src/App.js pointing to
      existing notebook framework
- [ ] T004 [P] Create base i18n keys structure for medlab in
      frontend/src/languages/en.json (~200 keys)
- [ ] T005 [P] Create base i18n keys structure for medlab in
      frontend/src/languages/fr.json (~200 keys)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can
be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

**Note**: Patient and SampleItem entities are REUSED AS-IS - no schema changes
needed

- [ ] T006 [P] Create QualityCheck entity Liquibase changelog in
      src/main/resources/liquibase/3.x.x/001-quality-check.xml
- [ ] T007 [P] Create TransportPackaging entity Liquibase changelog in
      src/main/resources/liquibase/3.x.x/002-transport-packaging.xml
- [ ] T008 [P] Create QCResult entity Liquibase changelog in
      src/main/resources/liquibase/3.x.x/003-qc-result.xml
- [ ] T009 [P] Create EquipmentUsageLog entity Liquibase changelog in
      src/main/resources/liquibase/3.x.x/004-equipment-usage-log.xml
- [ ] T010 Update master Liquibase changelog to include all new changesets
- [ ] T011 Run database migration and verify schema creation

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Patient Registration and Lab Orders (Priority: P1) 🎯 MVP

**Goal**: Register patients and create lab orders - entry point for all
workflows

**Strategy**: REUSE existing PatientService and LabOrder. Create NEW
PatientOrderEntryPage.js as simplified inline form for notebook Page 1.

> **TODO (Future Enhancement)**: Add "participant" support for research studies
> with enrollment status, study ID, and scheduled collection dates. This would
> extend PatientService with ParticipantService wrapper. Track in separate spec.

**Independent Test**: Register a new patient with demographic data, create a lab
order for CBC and Chemistry panel, verify patient and order appear in the
system.

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T012 [P] [US1] Create Cypress E2E test for patient registration in
      frontend/cypress/e2e/medlabPatientRegistration.cy.js

### Implementation for User Story 1

**Backend (REUSE - verify only):**

- [ ] T013 [US1] Verify existing PatientService meets medlab requirements
- [ ] T014 [US1] Verify existing LabOrder/TestRequest integration supports
      medlab workflow

**Frontend (NEW - Page 1):**

- [ ] T015 [P] [US1] Create PatientOrderEntryPage.js in
      frontend/src/components/notebook/pages/PatientOrderEntryPage.js - Inline
      patient search (reuse patient search logic) - Minimal patient form (name,
      sex, age, contact) - Lab order section with test checkboxes - Link to
      CreatePatientForm.js for "Advanced Registration"
- [ ] T016 [US1] Add i18n keys for US1 (patient registration, lab orders) to
      en.json and fr.json
- [ ] T017 [US1] Register PatientOrderEntryPage as Page 1 in MedLab notebook
      template

**Checkpoint**: User Story 1 complete - patients can be registered and lab
orders created within notebook workflow

---

## Phase 4: User Story 2 - Sample Collection and Labeling (Priority: P1)

**Goal**: Collect samples from patients, label them, and record collection
details

**Strategy**: REUSE existing SampleService and SampleItemService. Create NEW
SampleCollectionPage.js for notebook Page 2.

**Independent Test**: Create a sample collection for an existing lab order,
enter sample type (Whole Blood/EDTA), apply label, verify sample appears in
tracking queue.

### Tests for User Story 2 ⚠️

- [ ] T018 [P] [US2] Create Cypress E2E test for sample collection in
      frontend/cypress/e2e/medlabSampleCollection.cy.js

### Implementation for User Story 2

**Backend (REUSE - verify only):**

- [ ] T019 [US2] Verify existing SampleService and SampleItemService meet medlab
      requirements
- [ ] T020 [US2] Verify barcode generation service exists
      (BarcodeParsingService)

**Frontend (NEW - Page 2):**

- [ ] T021 [P] [US2] Create SampleCollectionPage.js in
      frontend/src/components/notebook/pages/SampleCollectionPage.js - Display
      patient context from Page 1 (workflowContext) - Sample creation form
      linked to lab order - Container type selection (vacutainer, cryovial,
      urine cup, etc.) - Free-text label field - Collection time and collector
      ID
- [ ] T022 [US2] Add i18n keys for US2 (sample collection, labeling) to en.json
      and fr.json
- [ ] T023 [US2] Register SampleCollectionPage as Page 2 in MedLab notebook
      template

**Checkpoint**: User Story 2 complete - samples can be collected and labeled

---

## Phase 5: User Story 3 - Sample Reception and Quality Control (Priority: P1)

**Goal**: Receive samples, perform QC checks, accept or reject based on
sample-type-specific criteria

**Strategy**: EXTEND existing SampleReceptionPage.js to add medlab QC hooks.
Create NEW QualityCheck entity and QualityCheckPage.js.

### Tests for User Story 3 ⚠️

- [ ] T024 [P] [US3] Create QualityCheckServiceTest in
      src/test/java/org/openelisglobal/medlab/service/QualityCheckServiceTest.java
- [ ] T025 [P] [US3] Create Cypress E2E test for sample reception in
      frontend/cypress/e2e/medlabSampleReception.cy.js

### Implementation for User Story 3

**Backend (NEW - QualityCheck entity):**

- [ ] T026 [P] [US3] Create QualityCheck valueholder in
      src/main/java/org/openelisglobal/medlab/valueholder/QualityCheck.java
- [ ] T027 [P] [US3] Create QualityCheckDAO + impl in medlab/dao/
- [ ] T028 [US3] Create QualityCheckService with sample-type-specific criteria
      (Chemistry: hemolysis/lipemia; Hematology: clotting; Stool: delay >30min)
- [ ] T029 [US3] Create QualityCheckController REST endpoints

**Frontend (EXTEND Page 3 + NEW Page 4):**

- [ ] T030 [US3] EXTEND SampleReceptionPage.js - Add medlab QC hooks for
      notebook integration
- [ ] T031 [P] [US3] Create QualityCheckPage.js in notebook/pages/ with
      sample-type-specific QC checklist
- [ ] T032 [US3] Add i18n keys for US3 to en.json and fr.json

**Checkpoint**: US3 complete - samples received with QC validation

---

## Phase 6: User Story 5 - Sample Allocation to Departments (Priority: P1)

**Goal**: Allocate accepted samples to testing departments based on ordered
tests

**Independent Test**: Allocate a multi-test order (CBC + LFT + Urinalysis) to
three departments, verify samples appear in each department's worklist.

### Tests for User Story 5 ⚠️

- [ ] T052 [P] [US5] Create SampleAllocationServiceTest in
      src/test/java/org/openelisglobal/medlab/service/SampleAllocationServiceTest.java
- [ ] T053 [P] [US5] Create Cypress E2E test for sample allocation in
      frontend/cypress/e2e/medlabSampleAllocation.cy.js

### Implementation for User Story 5

- [ ] T054 [P] [US5] Create SampleAllocation valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/SampleAllocation.java
- [ ] T055 [P] [US5] Create SampleAllocationDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/SampleAllocationDAO.java
- [ ] T056 [US5] Create SampleAllocationDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/SampleAllocationDAOImpl.java
- [ ] T057 [US5] Create SampleAllocationService with auto-allocation logic in
      src/main/java/org/openelisglobal/medlab/service/SampleAllocationService.java
- [ ] T058 [US5] Create SampleAllocationServiceImpl with department routing
      rules in
      src/main/java/org/openelisglobal/medlab/service/SampleAllocationServiceImpl.java
- [ ] T059 [US5] Create SampleAllocationController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/SampleAllocationController.java
- [ ] T060 [P] [US5] Create SampleAllocation.js React component with department
      worklists in frontend/src/components/medlab/SampleAllocation.js
- [ ] T061 [US5] Add i18n keys for US5 (allocation, departments) to en.json and
      fr.json

**Checkpoint**: User Story 5 complete - samples route to correct departments

---

## Phase 7: User Story 8 - Sample Processing by Department (Priority: P1)

**Goal**: Process samples per department protocols (centrifugation, smears,
staining) and create aliquots

**Independent Test**: Process a Chemistry sample (centrifuge, separate serum),
create 2 aliquots, verify parent-child relationship maintained.

### Tests for User Story 8 ⚠️

- [ ] T062 [P] [US8] Create SampleProcessingServiceTest in
      src/test/java/org/openelisglobal/medlab/service/SampleProcessingServiceTest.java
- [ ] T063 [P] [US8] Create Cypress E2E test for sample processing in
      frontend/cypress/e2e/medlabSampleProcessing.cy.js

### Implementation for User Story 8

- [ ] T064 [P] [US8] Create ProcessingRecord valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/ProcessingRecord.java
- [ ] T065 [P] [US8] Create ProcessingRecordDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/ProcessingRecordDAO.java
- [ ] T066 [US8] Create ProcessingRecordDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/ProcessingRecordDAOImpl.java
- [ ] T067 [US8] Create SampleProcessingService with department-specific methods
      in
      src/main/java/org/openelisglobal/medlab/service/SampleProcessingService.java
- [ ] T068 [US8] Create SampleProcessingServiceImpl with aliquot creation logic
      in
      src/main/java/org/openelisglobal/medlab/service/SampleProcessingServiceImpl.java
- [ ] T069 [US8] Create SampleProcessingController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/SampleProcessingController.java
- [ ] T070 [P] [US8] Create SampleProcessing.js React component with method
      selection in frontend/src/components/medlab/SampleProcessing.js
- [ ] T071 [US8] Add i18n keys for US8 (processing methods, stains, aliquots) to
      en.json and fr.json

**Checkpoint**: User Story 8 complete - samples can be processed and aliquoted

---

## Phase 8: User Story 9 - Testing and Instrument Integration (Priority: P1)

**Goal**: Perform tests with analyzer worklist generation and automatic result
upload with flagging

**Independent Test**: Generate worklist for Chemistry analyzer, run 10 samples,
verify results auto-upload with reference range flags.

### Tests for User Story 9 ⚠️

- [ ] T072 [P] [US9] Create TestingServiceTest in
      src/test/java/org/openelisglobal/medlab/service/TestingServiceTest.java
- [ ] T073 [P] [US9] Create EquipmentUsageLogDAOTest in
      src/test/java/org/openelisglobal/medlab/dao/EquipmentUsageLogDAOTest.java

### Implementation for User Story 9

- [ ] T074 [P] [US9] Create EquipmentUsageLog valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/EquipmentUsageLog.java
- [ ] T075 [P] [US9] Create EquipmentUsageLogDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/EquipmentUsageLogDAO.java
- [ ] T076 [US9] Create EquipmentUsageLogDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/EquipmentUsageLogDAOImpl.java
- [ ] T077 [US9] Create TestingService extending AnalyzerService for worklist
      generation in
      src/main/java/org/openelisglobal/medlab/service/TestingService.java
- [ ] T078 [US9] Implement automatic reference range flagging (H/L/C) in
      TestingService
- [ ] T079 [US9] Create TestingController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/TestingController.java
- [ ] T080 [P] [US9] Create TestingWorklist.js React component in
      frontend/src/components/medlab/TestingWorklist.js
- [ ] T081 [US9] Add i18n keys for US9 (worklist, testing, flags) to en.json and
      fr.json

**Checkpoint**: User Story 9 complete - analyzer worklists and result upload
working

---

## Phase 9: User Story 11 - Result Entry (Manual and Automated) (Priority: P1)

**Goal**: Enter results manually or verify auto-uploaded results

**Independent Test**: Enter manual results for urinalysis microscopy (WBC count,
RBC count), verify results saved to sample record.

### Tests for User Story 11 ⚠️

- [ ] T082 [P] [US11] Create ResultEntryServiceTest in
      src/test/java/org/openelisglobal/medlab/service/ResultEntryServiceTest.java

### Implementation for User Story 11

- [ ] T083 [US11] Create ResultEntryService extending ResultService in
      src/main/java/org/openelisglobal/medlab/service/ResultEntryService.java
- [ ] T084 [US11] Implement critical/panic value alerts in ResultEntryService
- [ ] T085 [US11] Create ResultEntryController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/ResultEntryController.java
- [ ] T086 [P] [US11] Create ResultEntry.js React component with multi-test view
      in frontend/src/components/medlab/ResultEntry.js
- [ ] T087 [US11] Add i18n keys for US11 (result entry, alerts) to en.json and
      fr.json

**Checkpoint**: User Story 11 complete - results can be entered and verified

---

## Phase 10: User Story 12 - Result Validation and Approval (Priority: P1)

**Goal**: Validate and approve results with department-specific queues

**Independent Test**: Submit 5 results for validation, approve 4, reject 1 with
reason, verify approved results are ready for reporting.

### Tests for User Story 12 ⚠️

- [ ] T088 [P] [US12] Create ResultValidationServiceTest in
      src/test/java/org/openelisglobal/medlab/service/ResultValidationServiceTest.java
- [ ] T089 [P] [US12] Create Cypress E2E test for result validation in
      frontend/cypress/e2e/medlabResultValidation.cy.js

### Implementation for User Story 12

- [ ] T090 [P] [US12] Create ValidationRecord valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/ValidationRecord.java
- [ ] T091 [P] [US12] Create ValidationRecordDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/ValidationRecordDAO.java
- [ ] T092 [US12] Create ValidationRecordDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/ValidationRecordDAOImpl.java
- [ ] T093 [P] [US12] Create ValidationForm in
      src/main/java/org/openelisglobal/medlab/form/ValidationForm.java
- [ ] T094 [US12] Create ResultValidationService with department queues in
      src/main/java/org/openelisglobal/medlab/service/ResultValidationService.java
- [ ] T095 [US12] Create ResultValidationServiceImpl with approve/reject/retest
      workflow in
      src/main/java/org/openelisglobal/medlab/service/ResultValidationServiceImpl.java
- [ ] T096 [US12] Create ResultValidationController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/ResultValidationController.java
- [ ] T097 [P] [US12] Create ResultValidation.js React component with department
      queues in frontend/src/components/medlab/ResultValidation.js
- [ ] T098 [US12] Add i18n keys for US12 (validation, approval, rejection) to
      en.json and fr.json

**Checkpoint**: User Story 12 complete - results go through validation workflow

---

## Phase 11: User Story 13 - Reporting and Result Delivery (Priority: P1)

**Goal**: Generate and print result reports after validation approval

**Independent Test**: Generate PDF report for approved results, verify report
includes patient info, test results, reference ranges, and validation signature.

### Tests for User Story 13 ⚠️

- [ ] T099 [P] [US13] Create ReportGenerationServiceTest in
      src/test/java/org/openelisglobal/medlab/service/ReportGenerationServiceTest.java

### Implementation for User Story 13

- [ ] T100 [US13] Create ReportGenerationService with PDF/Excel/CSV support in
      src/main/java/org/openelisglobal/medlab/service/ReportGenerationService.java
- [ ] T101 [US13] Implement result blocking until validation in
      ReportGenerationService
- [ ] T102 [US13] Create ReportController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/ReportController.java
- [ ] T103 [P] [US13] Create ResultReporting.js React component with print
      controls in frontend/src/components/medlab/ResultReporting.js
- [ ] T104 [US13] Add i18n keys for US13 (reporting, delivery) to en.json and
      fr.json

**Checkpoint**: User Story 13 complete - reports can be generated for approved
results

---

## Phase 12: User Story 4 - Transport Packaging Validation (Priority: P2)

**Goal**: Document and validate IATA PI650 compliant transport packaging

**Independent Test**: Record transport packaging details for incoming shipment,
validate IATA PI650 compliance, document condition on arrival.

### Tests for User Story 4 ⚠️

- [ ] T105 [P] [US4] Create TransportPackagingServiceTest in
      src/test/java/org/openelisglobal/medlab/service/TransportPackagingServiceTest.java

### Implementation for User Story 4

- [ ] T106 [P] [US4] Create TransportPackaging valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/TransportPackaging.java
- [ ] T107 [P] [US4] Create TransportPackagingDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/TransportPackagingDAO.java
- [ ] T108 [US4] Create TransportPackagingDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/TransportPackagingDAOImpl.java
- [ ] T109 [P] [US4] Create TransportPackagingForm in
      src/main/java/org/openelisglobal/medlab/form/TransportPackagingForm.java
- [ ] T110 [US4] Create TransportPackagingService with IATA PI650 compliance
      calculation in
      src/main/java/org/openelisglobal/medlab/service/TransportPackagingService.java
- [ ] T111 [US4] Create TransportPackagingController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/TransportPackagingController.java
- [ ] T112 [P] [US4] Create TransportPackagingForm.js React component in
      frontend/src/components/medlab/TransportPackagingForm.js
- [ ] T113 [US4] Add i18n keys for US4 (packaging levels, compliance) to en.json
      and fr.json

**Checkpoint**: User Story 4 complete - transport packaging can be validated

---

## Phase 13: User Story 6 - Hierarchical Storage Management (Priority: P2)

**Goal**: Store samples in hierarchical storage with cryo-box well mapping

**Independent Test**: Store 10 aliquots in a cryo-box, assign positions 1-10 in
wells, verify box map shows occupied positions.

### Tests for User Story 6 ⚠️

- [ ] T114 [P] [US6] Create StorageManagementServiceTest in
      src/test/java/org/openelisglobal/medlab/service/StorageManagementServiceTest.java

### Implementation for User Story 6

- [ ] T115 [US6] Extend StorageLocationService for cryo-box well mapping (81
      wells) in
      src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java
- [ ] T116 [US6] Create StorageManagementService with location lookup in
      src/main/java/org/openelisglobal/medlab/service/StorageManagementService.java
- [ ] T117 [US6] Create StorageManagementController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/StorageManagementController.java
- [ ] T118 [P] [US6] Create StorageManagement.js React component with box grid
      visualization in frontend/src/components/medlab/StorageManagement.js
- [ ] T119 [US6] Add i18n keys for US6 (storage, cryo-box, wells) to en.json and
      fr.json

**Checkpoint**: User Story 6 complete - samples can be stored with location
tracking

---

## Phase 14: User Story 7 - Environmental Monitoring (Priority: P2)

**Goal**: Record and monitor temperature for storage locations with alerts

**Independent Test**: Record AM and PM temperatures for a -80C freezer for 3
days, verify readings appear in dashboard, trigger alert when out of range.

### Tests for User Story 7 ⚠️

- [ ] T120 [P] [US7] Create EnvironmentalMonitoringServiceTest in
      src/test/java/org/openelisglobal/medlab/service/EnvironmentalMonitoringServiceTest.java

### Implementation for User Story 7

- [ ] T121 [P] [US7] Create EnvironmentalReading valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/EnvironmentalReading.java
- [ ] T122 [P] [US7] Create EnvironmentalReadingDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/EnvironmentalReadingDAO.java
- [ ] T123 [US7] Create EnvironmentalReadingDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/EnvironmentalReadingDAOImpl.java
- [ ] T124 [P] [US7] Create EnvironmentalReadingForm in
      src/main/java/org/openelisglobal/medlab/form/EnvironmentalReadingForm.java
- [ ] T125 [US7] Create EnvironmentalMonitoringService with twice-daily
      recording and alerts in
      src/main/java/org/openelisglobal/medlab/service/EnvironmentalMonitoringService.java
- [ ] T126 [US7] Create EnvironmentalMonitoringController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/EnvironmentalMonitoringController.java
- [ ] T127 [P] [US7] Create EnvironmentalMonitoring.js React component with
      temperature charts in
      frontend/src/components/medlab/EnvironmentalMonitoring.js
- [ ] T128 [US7] Add i18n keys for US7 (temperature, excursion, alerts) to
      en.json and fr.json

**Checkpoint**: User Story 7 complete - environmental monitoring operational

---

## Phase 15: User Story 10 - Quality Control and Calibration (Priority: P2)

**Goal**: Run QC controls, track calibration, monitor with Levey-Jennings charts

**Independent Test**: Run normal and pathologic QC for Chemistry, record
results, verify Levey-Jennings chart updates, flag when out of control.

### Tests for User Story 10 ⚠️

- [ ] T129 [P] [US10] Create QualityControlServiceTest in
      src/test/java/org/openelisglobal/medlab/service/QualityControlServiceTest.java
- [ ] T130 [P] [US10] Create Cypress E2E test for QC dashboard in
      frontend/cypress/e2e/medlabQCDashboard.cy.js

### Implementation for User Story 10

- [ ] T131 [P] [US10] Create QCResult valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/QCResult.java
- [ ] T132 [P] [US10] Create QCResultDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/QCResultDAO.java
- [ ] T133 [US10] Create QCResultDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/QCResultDAOImpl.java
- [ ] T134 [US10] Create QualityControlService with Westgard rule validation in
      src/main/java/org/openelisglobal/medlab/service/QualityControlService.java
- [ ] T135 [US10] Implement Levey-Jennings chart data generation in
      QualityControlService
- [ ] T136 [US10] Create QualityControlController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/QualityControlController.java
- [ ] T137 [P] [US10] Create QCDashboard.js React component in
      frontend/src/components/medlab/QCDashboard.js
- [ ] T138 [P] [US10] Create LeveyJenningsChart.js component using Carbon Charts
      in frontend/src/components/medlab/QCDashboard/LeveyJenningsChart.js
- [ ] T139 [P] [US10] Create QCEntryForm.js component in
      frontend/src/components/medlab/QCDashboard/QCEntryForm.js
- [ ] T140 [US10] Add i18n keys for US10 (QC, Westgard, calibration) to en.json
      and fr.json

**Checkpoint**: User Story 10 complete - QC monitoring and Levey-Jennings
charting working

---

## Phase 16: User Story 14 - Laboratory Performance Dashboards (Priority: P2)

**Goal**: Display KPI dashboards for TAT, acceptance rates, QC pass rates,
equipment usage

**Independent Test**: View TAT dashboard showing average time from collection to
result for each test type, verify data updates in real-time.

### Tests for User Story 14 ⚠️

- [ ] T141 [P] [US14] Create LabDashboardServiceTest in
      src/test/java/org/openelisglobal/medlab/service/LabDashboardServiceTest.java
- [ ] T142 [P] [US14] Create Cypress E2E test for lab dashboard in
      frontend/cypress/e2e/medlabLabDashboard.cy.js

### Implementation for User Story 14

- [ ] T143 [US14] Create LabDashboardService with KPI calculations in
      src/main/java/org/openelisglobal/medlab/service/LabDashboardService.java
- [ ] T144 [US14] Implement TAT calculation by test type in LabDashboardService
- [ ] T145 [US14] Implement sample acceptance rate tracking in
      LabDashboardService
- [ ] T146 [US14] Create LabDashboardController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/LabDashboardController.java
- [ ] T147 [P] [US14] Create LabDashboard.js React component in
      frontend/src/components/medlab/LabDashboard.js
- [ ] T148 [P] [US14] Create TATMetrics.js component in
      frontend/src/components/medlab/LabDashboard/TATMetrics.js
- [ ] T149 [P] [US14] Create AcceptanceRateChart.js component in
      frontend/src/components/medlab/LabDashboard/AcceptanceRateChart.js
- [ ] T150 [P] [US14] Create EquipmentUsageReport.js component in
      frontend/src/components/medlab/LabDashboard/EquipmentUsageReport.js
- [ ] T151 [US14] Add i18n keys for US14 (dashboard, metrics, charts) to en.json
      and fr.json

**Checkpoint**: User Story 14 complete - dashboards display laboratory KPIs

---

## Phase 17: User Story 15 - Sample Utilization and Depletion Tracking (Priority: P2)

**Goal**: Track sample usage and mark depleted samples

**Independent Test**: Mark a sample as partially used (2ml of 5ml), later mark
as depleted, verify status update and audit trail.

### Tests for User Story 15 ⚠️

- [ ] T152 [P] [US15] Create SampleUtilizationServiceTest in
      src/test/java/org/openelisglobal/medlab/service/SampleUtilizationServiceTest.java

### Implementation for User Story 15

- [ ] T153 [US15] Create SampleUtilizationService with usage tracking in
      src/main/java/org/openelisglobal/medlab/service/SampleUtilizationService.java
- [ ] T154 [US15] Create SampleUtilizationController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/SampleUtilizationController.java
- [ ] T155 [P] [US15] Create SampleUtilization.js React component in
      frontend/src/components/medlab/SampleUtilization.js
- [ ] T156 [US15] Add i18n keys for US15 (utilization, depletion) to en.json and
      fr.json

**Checkpoint**: User Story 15 complete - sample utilization tracking working

---

## Phase 18: User Story 17 - Sample Disposal (Priority: P2)

**Goal**: Dispose samples following international guidelines with compliance
documentation

**Independent Test**: Dispose 10 blood samples by incineration, record disposal
details, verify audit trail complete.

### Tests for User Story 17 ⚠️

- [ ] T157 [P] [US17] Create DisposalServiceTest in
      src/test/java/org/openelisglobal/medlab/service/DisposalServiceTest.java
- [ ] T158 [P] [US17] Create Cypress E2E test for disposal in
      frontend/cypress/e2e/medlabDisposal.cy.js

### Implementation for User Story 17

- [ ] T159 [P] [US17] Create DisposalRecord valueholder entity in
      src/main/java/org/openelisglobal/medlab/valueholder/DisposalRecord.java
- [ ] T160 [P] [US17] Create DisposalRecordDAO interface in
      src/main/java/org/openelisglobal/medlab/dao/DisposalRecordDAO.java
- [ ] T161 [US17] Create DisposalRecordDAOImpl in
      src/main/java/org/openelisglobal/medlab/dao/DisposalRecordDAOImpl.java
- [ ] T162 [US17] Create DisposalService with method enforcement by sample type
      in src/main/java/org/openelisglobal/medlab/service/DisposalService.java
- [ ] T163 [US17] Create DisposalController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/DisposalController.java
- [ ] T164 [P] [US17] Create DisposalManagement.js React component in
      frontend/src/components/medlab/DisposalManagement.js
- [ ] T165 [US17] Add i18n keys for US17 (disposal, methods, compliance) to
      en.json and fr.json

**Checkpoint**: User Story 17 complete - sample disposal with compliance
verification

---

## Phase 19: User Story 16 - Bioequivalence Study Support (Priority: P3)

**Goal**: Support BE studies with deep freezer storage and bioanalytical lab
transfer

**Independent Test**: Store BE sample with 2-year retention flag, transfer to
bioanalytical lab, verify chain of custody recorded.

### Implementation for User Story 16

- [ ] T166 [US16] Extend StorageManagementService for BE retention policies in
      src/main/java/org/openelisglobal/medlab/service/StorageManagementService.java
- [ ] T167 [US16] Create BiobankTransferService in
      src/main/java/org/openelisglobal/medlab/service/BiobankTransferService.java
- [ ] T168 [US16] Create BiobankTransferController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/BiobankTransferController.java
- [ ] T169 [US16] Add i18n keys for US16 (BE study, retention, transfer) to
      en.json and fr.json

**Checkpoint**: User Story 16 complete - BE study support operational

---

## Phase 20: User Story 18 - Data Archiving and Biobanking Transfer (Priority: P3)

**Goal**: Archive data and transfer samples to biobank

**Independent Test**: Archive complete results for a closed study, transfer
remaining samples to biobank, verify all data accessible in archive.

### Implementation for User Story 18

- [ ] T170 [P] [US18] Create ArchiveBiobank.js React component in
      frontend/src/components/medlab/ArchiveBiobank.js
- [ ] T171 [US18] Create ArchivingService with retention policies in
      src/main/java/org/openelisglobal/medlab/service/ArchivingService.java
- [ ] T172 [US18] Create ArchivingController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/ArchivingController.java
- [ ] T173 [US18] Add i18n keys for US18 (archiving, biobank) to en.json and
      fr.json

**Checkpoint**: User Story 18 complete - archiving and biobank transfer working

---

## Phase 21: User Story 19 - Accreditation Support (Priority: P3)

**Goal**: Support laboratory accreditation (ISO 15189, CAP, CLIA) with audit
trails and compliance reports

**Independent Test**: Generate audit trail report for a sample from collection
to result, verify all actions documented with user, timestamp, and action type.

### Implementation for User Story 19

- [ ] T174 [US19] Create AccreditationReportService with compliance report
      generation in
      src/main/java/org/openelisglobal/medlab/service/AccreditationReportService.java
- [ ] T175 [US19] Create AccreditationController REST endpoints in
      src/main/java/org/openelisglobal/medlab/controller/rest/AccreditationController.java
- [ ] T176 [US19] Verify audit trail completeness for all medlab operations
- [ ] T177 [US19] Add i18n keys for US19 (accreditation, compliance, audit) to
      en.json and fr.json

**Checkpoint**: User Story 19 complete - accreditation support operational

---

## Phase 22: Polish & Cross-Cutting Concerns

**Purpose**: Improvements affecting multiple user stories

- [ ] T178 [P] Update quickstart.md with implementation details in
      specs/001-medical-lab-workflow/quickstart.md
- [ ] T179 [P] Update data-model.md with any schema changes in
      specs/001-medical-lab-workflow/data-model.md
- [ ] T180 Code cleanup and refactoring across medlab module
- [ ] T181 Performance optimization for bulk operations (<30s for 100 samples)
- [ ] T182 [P] Security hardening - verify RBAC for all endpoints
- [ ] T183 [P] Verify all i18n keys have both en.json and fr.json translations
- [ ] T184 Run final Cypress E2E test suite
- [ ] T185 Format all code: mvn spotless:apply && cd frontend && npm run format
- [ ] T186 Run quickstart.md validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - BLOCKS all user stories
- **User Stories (Phase 3-21)**: All depend on Foundational phase completion
  - P1 stories should complete before P2 stories
  - P2 stories should complete before P3 stories
- **Polish (Phase 22)**: Depends on desired user stories being complete

### User Story Dependencies

| Story                          | Priority | Dependencies | Can Start After |
| ------------------------------ | -------- | ------------ | --------------- |
| US1 - Patient/Lab Orders       | P1       | None         | Foundational    |
| US2 - Sample Collection        | P1       | US1          | US1             |
| US3 - Sample Reception QC      | P1       | US2          | US2             |
| US5 - Sample Allocation        | P1       | US3          | US3             |
| US8 - Sample Processing        | P1       | US5          | US5             |
| US9 - Testing Integration      | P1       | US8          | US8             |
| US11 - Result Entry            | P1       | US9          | US9             |
| US12 - Result Validation       | P1       | US11         | US11            |
| US13 - Reporting               | P1       | US12         | US12            |
| US4 - Transport Packaging      | P2       | US3          | Foundational    |
| US6 - Storage Management       | P2       | US3          | Foundational    |
| US7 - Environmental Monitoring | P2       | US6          | US6             |
| US10 - QC/Calibration          | P2       | US9          | US9             |
| US14 - Dashboards              | P2       | US12         | US12            |
| US15 - Sample Utilization      | P2       | US8          | US8             |
| US17 - Disposal                | P2       | US12         | US12            |
| US16 - BE Study Support        | P3       | US6          | US6             |
| US18 - Archiving/Biobank       | P3       | US17         | US17            |
| US19 - Accreditation           | P3       | US14         | US14            |

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Valueholders (entities) before DAOs
- DAOs before Services
- Services before Controllers
- Controllers before Frontend components
- Frontend components before i18n integration

### Parallel Opportunities

Tasks marked [P] can run in parallel if they affect different files:

- All Liquibase changesets in Phase 2 can run in parallel
- Within a story: Entity valueholders, DAO interfaces, and forms can run in
  parallel
- Frontend components within a story can run in parallel
- Test files can be written in parallel

---

## Parallel Example: User Story 3

```bash
# Launch tests in parallel:
Task: "Create SampleReceptionServiceTest in src/test/java/.../SampleReceptionServiceTest.java"
Task: "Create QualityCheckDAOTest in src/test/java/.../QualityCheckDAOTest.java"
Task: "Create Cypress E2E test in frontend/cypress/e2e/medlabSampleReception.cy.js"

# Launch valueholders and forms in parallel:
Task: "Create QualityCheck valueholder in src/main/java/.../QualityCheck.java"
Task: "Create QualityCheckForm in src/main/java/.../QualityCheckForm.java"
Task: "Create QualityCheckDAO interface in src/main/java/.../QualityCheckDAO.java"
```

---

## Implementation Strategy

### MVP First (P1 User Stories Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phases 3-11: P1 User Stories
   (US1→US2→US3→US5→US8→US9→US11→US12→US13)
4. **STOP and VALIDATE**: Test complete sample lifecycle
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 (Patient/Orders) → Test independently → First MVP
3. US2+US3 (Collection/Reception) → Test sample tracking
4. US5+US8 (Allocation/Processing) → Test department routing
5. US9+US11+US12+US13 (Testing/Results) → Complete P1 workflow
6. Add P2 stories incrementally (US4, US6, US7, US10, US14, US15, US17)
7. Add P3 stories last (US16, US18, US19)

### Summary

| Category             | Count |
| -------------------- | ----- |
| Total Tasks          | 186   |
| Setup Tasks          | 5     |
| Foundational Tasks   | 13    |
| P1 User Story Tasks  | 85    |
| P2 User Story Tasks  | 64    |
| P3 User Story Tasks  | 12    |
| Polish Tasks         | 9     |
| Parallelizable Tasks | 78    |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD per constitution)
- Commit after each task or logical group
- Run `mvn spotless:apply` and `npm run format` before commits
- Stop at any checkpoint to validate story independently
