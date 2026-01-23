# Implementation Plan: Medical Laboratory Workflow

**Branch**: `001-medical-lab-workflow` | **Date**: 2024-12-14 | **Updated**:
2026-01-07 | **Spec**: [spec.md](spec.md) **SRS Version**: Medical Laboratory
Workflow Documentation v1.0 (January 2026)

## Summary

This feature implements a comprehensive Medical Laboratory workflow system with
an **order-driven architecture** where lab orders drive all downstream sample
collection, processing, and testing activities. The system follows a 10-stage
workflow from patient registration through disposal/archiving.

**Core Architecture: Order-Driven Workflow**

The system follows an order-centric model where:

- **Orders drive sample collection** - Lab orders specify required container
  types, volumes, and handling requirements
- **Orders enable QC validation** - Samples without corresponding orders are
  rejected
- **Orders determine routing** - Sample-to-test allocation and department
  routing based on ordered tests
- **Orders generate worklists** - Electronic work lists for analyzers generated
  from orders

**Technical Approach**: Build on existing OpenELIS infrastructure
(PatientService, SampleService, StorageService, AnalyzerService). Extend
LabOrder/TestRequest to support order-driven architecture with container/volume
specifications. Create new service modules for QC management (with order
validation), transport packaging, environmental monitoring, result validation,
ALCOA+ compliance, sample retrieval, disposal tracking, and analytics
dashboards. Implement React frontend with Carbon Design System. Use Liquibase
for all schema changes, React Intl for internationalization.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) **Primary Dependencies**:
Spring Framework 6.2.2, Hibernate 6.x, HAPI FHIR R4 6.6.2, React 17, Carbon
Design System v1.15 **Storage**: PostgreSQL 14+, Liquibase 4.8.0 for migrations
**Testing**: JUnit 4 (NOT JUnit 5), Mockito 2.21.0, Cypress 12.17.3 **Target
Platform**: Linux server (Docker), Tomcat 10 WAR deployment **Project Type**:
Web application (Java backend + React frontend) **Performance Goals**: Single
sample operations <3s, bulk operations (100 samples) <30s, dashboard refresh
<30s **Constraints**: Support 20 concurrent users, twice-daily temperature
logging, <60s report generation **Scale/Scope**: 19 UI pages, 165+ functional
requirements, 6 laboratory departments, 12+ entity types

**Compliance Requirements**:

- **ALCOA+ Data Handling**: Attributable, Legible, Contemporaneous, Original,
  Accurate, Complete, Consistent, Enduring, Available
- **Accreditation Standards**: ISO 15189, SLIPTA, CAP, CLIA
- **Order-Driven Validation**: Samples without orders must be rejected at QC

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

| Principle                          | Requirement                                          | Status | Notes                                                               |
| ---------------------------------- | ---------------------------------------------------- | ------ | ------------------------------------------------------------------- |
| I. Configuration-Driven Variation  | Country customizations via config, not code          | PASS   | Quality criteria, reference ranges, retention policies configurable |
| II. Carbon Design System First     | All UI uses @carbon/react exclusively                | PASS   | DataTable, Modal, Form, Grid, Charts components planned             |
| III. FHIR/IHE Standards Compliance | FHIR R4 for external data                            | PASS   | Patient, Sample, DiagnosticReport, Observation FHIR resources       |
| IV. Layered Architecture           | 5-layer: Valueholder->DAO->Service->Controller->Form | PASS   | All new entities follow pattern                                     |
| V. Test-Driven Development         | TDD, >70% coverage, individual E2E tests             | PASS   | JUnit 4 unit tests, Cypress E2E per page                            |
| VI. Database Schema Management     | Liquibase only, no direct DDL                        | PASS   | All tables via Liquibase changesets                                 |
| VII. Internationalization First    | React Intl for all strings                           | PASS   | ~500 new keys for en.json and fr.json                               |
| VIII. Security & Compliance        | RBAC, audit trail, input validation, ALCOA+          | PASS   | Department-based access, audit trail, ALCOA+ data handling          |
| IX. Spec-Driven Iteration          | Milestones for >3 day efforts                        | PASS   | 12 milestones covering 10-stage workflow + ALCOA+ + Retrieval       |

**Additional Compliance Verification**:

| Requirement                  | Status | Notes                                                    |
| ---------------------------- | ------ | -------------------------------------------------------- |
| Order-Driven Architecture    | PASS   | Orders drive collection, validation, routing, worklists  |
| Order Validation at QC       | PASS   | Samples without orders rejected per FR-021               |
| ALCOA+ Data Handling         | PASS   | FR-140 to FR-150 cover all ALCOA+ requirements           |
| SLIPTA Accreditation Support | PASS   | Added to FR-131 alongside ISO 15189, CAP, CLIA           |
| Delta Checks                 | PASS   | FR-150 requires comparison with previous patient results |
| Sample Retrieval & MTAs      | PASS   | FR-160 to FR-165 cover retrieval and transfer protocols  |

## Project Structure

### Documentation (this feature)

```text
specs/001-medical-lab-workflow/
├── plan.md              # This file
├── research.md          # Phase 0 research output
├── data-model.md        # Entity relationship documentation
├── quickstart.md        # Step-by-step developer guide
├── contracts/           # API contracts
│   ├── patient-registration.yaml
│   ├── sample-collection.yaml
│   ├── sample-reception-qc.yaml
│   ├── transport-packaging.yaml
│   ├── sample-allocation.yaml
│   ├── storage-management.yaml
│   ├── environmental-monitoring.yaml
│   ├── sample-processing.yaml
│   ├── testing-integration.yaml
│   ├── quality-control.yaml
│   ├── result-entry-validation.yaml
│   ├── reporting-dashboard.yaml
│   └── disposal-archiving.yaml
└── tasks.md             # Phase 2 task breakdown (via /speckit.tasks)
```

### Source Code (repository root)

```text
# Backend (Java)
src/main/java/org/openelisglobal/
├── medlab/                              # NEW MODULE: Medical Lab Workflow
│   ├── valueholder/
│   │   ├── QualityCheck.java            # Sample quality check results
│   │   ├── TransportPackaging.java      # IATA PI650 packaging tracking
│   │   ├── EnvironmentalReading.java    # Temperature monitoring
│   │   ├── ProcessingRecord.java        # Sample processing steps
│   │   ├── QCResult.java                # QC control results (Levey-Jennings)
│   │   ├── ValidationRecord.java        # Result approval workflow
│   │   ├── DisposalRecord.java          # Disposal documentation
│   │   ├── EquipmentUsageLog.java       # Instrument tracking
│   │   └── SampleAllocation.java        # Department routing
│   ├── dao/
│   │   ├── QualityCheckDAO.java
│   │   ├── TransportPackagingDAO.java
│   │   ├── EnvironmentalReadingDAO.java
│   │   ├── ProcessingRecordDAO.java
│   │   ├── QCResultDAO.java
│   │   ├── ValidationRecordDAO.java
│   │   ├── DisposalRecordDAO.java
│   │   └── EquipmentUsageLogDAO.java
│   ├── service/
│   │   ├── SampleReceptionService.java        # QC at reception
│   │   ├── TransportPackagingService.java     # IATA compliance
│   │   ├── EnvironmentalMonitoringService.java # Temperature tracking
│   │   ├── SampleProcessingService.java       # Department processing
│   │   ├── QualityControlService.java         # Levey-Jennings, EQA
│   │   ├── ResultValidationService.java       # Approval workflow
│   │   ├── DisposalService.java               # Disposal compliance
│   │   ├── LabDashboardService.java           # Analytics/KPIs
│   │   └── SampleAllocationService.java       # Department routing
│   ├── controller/rest/
│   │   ├── SampleReceptionController.java
│   │   ├── TransportPackagingController.java
│   │   ├── EnvironmentalMonitoringController.java
│   │   ├── SampleProcessingController.java
│   │   ├── QualityControlController.java
│   │   ├── ResultValidationController.java
│   │   ├── DisposalController.java
│   │   └── LabDashboardController.java
│   └── form/
│       ├── QualityCheckForm.java
│       ├── TransportPackagingForm.java
│       ├── EnvironmentalReadingForm.java
│       └── ValidationForm.java

# Extended existing modules
├── patient/                             # EXTEND: Add participant support
│   └── service/
│       └── ParticipantService.java      # NEW: Research participant management
├── sample/                              # EXTEND: Medical lab attributes
│   └── valueholder/
│       └── SampleItem.java              # UPDATE: Add medlab fields
└── result/                              # EXTEND: Validation workflow
    └── service/
        └── ResultValidationService.java # NEW: Department-specific approval

src/main/resources/liquibase/
└── 3.x.x/
    ├── 001-quality-check.xml
    ├── 002-transport-packaging.xml
    ├── 003-environmental-reading.xml
    ├── 004-processing-record.xml
    ├── 005-qc-result.xml
    ├── 006-validation-record.xml
    ├── 007-disposal-record.xml
    ├── 008-equipment-usage-log.xml
    └── 009-sample-allocation.xml

# Frontend (React) - Reuse-First Approach
# Medical Lab workflow is implemented as a Notebook with embedded pages.
# All pages display within the Notebook container - no standalone navigation.

frontend/src/components/
├── notebook/pages/                      # EXTEND: Add medlab-specific pages
│   ├── PatientOrderEntryPage.js         # NEW: Patient reg + lab order (Page 1)
│   ├── SampleCollectionPage.js          # NEW: Specimen collection (Page 2)
│   ├── SampleReceptionPage.js           # EXTEND: Add medlab QC hooks (Page 3)
│   ├── QualityCheckPage.js              # NEW: Sample-type QC checklist (Page 4)
│   ├── TransportPackagingPage.js        # NEW: IATA PI650 compliance (Page 5, P2)
│   ├── SampleRoutingPage.js             # EXTEND: Dept allocation (Page 6)
│   ├── InitialProcessingPage.js         # EXTEND: Dept-specific methods (Page 7)
│   ├── StorageAssignmentPage.js         # NEW: Embeds StorageHierarchySelector (Page 8)
│   ├── EnvironmentalMonitoringPage.js   # NEW: Embeds FreezerMonitoring (Page 9)
│   ├── AnalysisPage.js                  # EXTEND: Medlab analyzer support (Page 10)
│   ├── QCDashboardPage.js               # NEW: Levey-Jennings charts (Page 11)
│   │   └── LeveyJenningsChart.js        # NEW: Carbon Charts component
│   ├── ResultValidationPage.js          # NEW: Embeds Validation.js (Page 12)
│   ├── ResultCompilationPage.js         # REUSE AS-IS (Page 13)
│   ├── LabDashboardPage.js              # NEW: Embeds Dashboard metrics (Page 14)
│   ├── SampleUtilizationPage.js         # NEW: Usage tracking (Page 15)
│   └── EndOfProjectArchivingPage.js     # REUSE AS-IS (Page 16)
│
├── notebook/workflow/                   # REUSE: Existing workflow components
│   ├── SampleGrid.js                    # REUSE AS-IS: Sample display grid
│   ├── StorageHierarchySelector.js      # REUSE AS-IS: Location picker
│   ├── BoxLayoutViewer.js               # REUSE AS-IS: Box visualization
│   └── NotebookWorkflowTab.js           # REUSE AS-IS: Page navigation
│
├── coldStorage/                         # EXTEND: Environmental monitoring
│   └── FreezerMonitoringDashboard.js    # EXTEND: Add twice-daily schedule
│
├── validation/                          # EXTEND: Result validation
│   └── Validation.js                    # EXTEND: Add department queue filtering
│
└── home/                                # EXTEND: Dashboard metrics
    └── Dashboard.tsx                    # EXTEND: Add medlab-specific tiles

frontend/src/languages/
├── en.json                              # UPDATE: Add ~200 new keys (reduced due to reuse)
└── fr.json                              # UPDATE: Add ~200 new keys

# Tests
src/test/java/org/openelisglobal/medlab/
├── service/SampleReceptionServiceTest.java
├── service/QualityControlServiceTest.java
├── service/ResultValidationServiceTest.java
├── service/EnvironmentalMonitoringServiceTest.java
├── dao/QualityCheckDAOTest.java
└── controller/SampleReceptionControllerTest.java

frontend/cypress/e2e/
├── medlabPatientRegistration.cy.js
├── medlabSampleCollection.cy.js
├── medlabSampleReception.cy.js
├── medlabSampleProcessing.cy.js
├── medlabQCDashboard.cy.js
├── medlabResultValidation.cy.js
├── medlabLabDashboard.cy.js
└── medlabDisposal.cy.js
```

**Structure Decision**: Reuse-first approach using the existing Notebook
workflow framework. Backend creates new `medlab` module under
`org.openelisglobal.medlab` for genuinely new entities (QualityCheck, QCResult,
TransportPackaging). Frontend extends existing notebook pages rather than
duplicating - only 8 new page components needed (vs 19 originally planned). All
pages embedded within Notebook container maintaining patient context throughout
workflow.

## Complexity Tracking

> No violations requiring justification. All patterns follow constitution.

## Implementation Milestones

### Milestone 1: Foundation - Order-Driven Architecture (Backend)

**Branch**: `feat/001-medical-lab-workflow-m1-order-foundation` **Effort**: 3-4
days **Dependencies**: None **User Stories**: US1 (P0)

**Goal**: Establish the order-driven foundation where lab orders drive all
downstream activities. This is the CRITICAL architectural foundation.

**Order Significance** (per SRS Section 3.1.2):

- Orders MUST specify required container types, volumes, and handling
  requirements
- Orders MUST enable QC validation (samples without orders are rejected)
- Orders MUST determine sample-to-test allocation and department routing
- Orders MUST generate electronic work lists for analyzers

Tasks:

- [ ] Verify existing PatientService meets medlab requirements (REUSE AS-IS)
- [ ] Verify existing LabOrder/TestRequest integration (REUSE AS-IS)
- [ ] **Extend LabOrder entity** to include container_type, volume_required,
      handling_requirements per test (FR-006)
- [ ] **Create OrderSampleLink entity** for order-sample relationship tracking
- [ ] Create MedLabNotebook template configuration in NoteBookService
- [ ] Define 16-page workflow template for Medical Lab notebook
- [ ] Implement order-driven container/volume display for collection (FR-007)
- [ ] Add ORM validation tests for order-driven architecture
- [ ] Add unit tests for order significance requirements

### Milestone 2: Sample Collection & Reception QC (Backend)

**Branch**: `feat/001-medical-lab-workflow/m2-collection-reception` **Effort**:
4-5 days **Dependencies**: M1 **User Stories**: US2 (P1), US3 (P1)

**Goal**: Implement sample collection via manifest import, order linking, and QC
with order validation. **Samples without corresponding orders MUST be rejected
at QC stage.**

**Sample Collection Workflow (Two-Step Process)**:

1. **Step 1: Import Samples from Manifest** - Bulk CSV import creates samples in
   system (not yet linked to orders)
2. **Step 2: Link Samples to Orders/Tests** - Associate samples with orders,
   assign tests. Supports anonymous samples (NULL patient).

**Manifest Field Specification** (per FR-010 to FR-014):

| Field              | Required | Maps To                        |
| ------------------ | -------- | ------------------------------ |
| `sampleId`         | Yes      | SampleItem.accessionNumber     |
| `sampleTypeId`     | Yes      | SampleItem.typeOfSample        |
| `containerType`    | Yes      | SampleItem.collectionContainer |
| `customLabel`      | No       | SampleItem.externalId          |
| `quantity`         | Yes      | SampleItem.initialQuantity     |
| `unitOfMeasure`    | Yes      | SampleItem.unitOfMeasure       |
| `collectionSource` | Yes      | Sample.source                  |
| `collector`        | Yes      | SampleItem.collector           |
| `collectionDate`   | Yes      | SampleItem.collectionDate      |
| `collectionTime`   | Yes      | SampleItem.collectionDate      |
| `orderId`          | No       | OrderSampleLink.orderId        |
| `patientId`        | No       | Sample.patientId (NULL=anon)   |
| `notes`            | No       | SampleItem.note                |

**Sample-Test Relationship**:

- One SampleItem → Many Analysis records (multiple tests per sample)
- Aliquoting is manual (Stage 6) via SampleManagementService - NOT automatic

**Common QC Criteria** (per SRS Section 3.3.1):

- Mislabeling or unlabeled specimens
- Inappropriate container or test tube type
- **Without corresponding test request/order** ← MANDATORY rejection
- Storage temperature at collection validation

Tasks:

- [ ] Create manifest import service (SampleManifestImportService)
- [ ] Implement CSV parsing with field validation per manifest spec
- [ ] Create sample-to-order linking service (OrderSampleLinkService)
- [ ] Support anonymous samples (patientId = NULL, display as "Participant")
- [ ] Create QualityCheck valueholder with sample-type-specific criteria
- [ ] Create QualityCheckDAO and SampleReceptionService
- [ ] **Implement order validation in QC** - reject samples without orders
      (FR-021, FR-025)
- [ ] Implement sample-type-specific quality validation (Chemistry, Hematology,
      Stool, Urine, Microbiology)
- [ ] Create SampleAllocation entity for department routing
- [ ] **Implement order-driven department routing** (FR-008)
- [ ] Implement corrective action workflow (recollection, return to submitter)
- [ ] Create REST endpoints: POST /rest/medlab/samples/import (manifest upload)
- [ ] Create REST endpoints: POST /rest/medlab/samples/{id}/link-order
- [ ] Add Liquibase changesets
- [ ] Add unit and integration tests for manifest import and order linking

### Milestone 3: Transport Packaging Validation (Backend)

**Branch**: `feat/001-medical-lab-workflow/m3-transport-packaging` **Effort**:
2-3 days **Dependencies**: M2

Tasks:

- [ ] Create TransportPackaging valueholder with primary/secondary/tertiary
      levels
- [ ] Create TransportPackagingDAO and TransportPackagingService
- [ ] Implement IATA PI650 compliance calculation
- [ ] Create REST endpoints for packaging validation
- [ ] Add Liquibase changesets
- [ ] Add unit tests

### Milestone 4: Storage & Environmental Monitoring (Backend)

**Branch**: `feat/001-medical-lab-workflow/m4-storage-monitoring` **Effort**:
3-4 days **Dependencies**: M2

Tasks:

- [ ] Create EnvironmentalReading valueholder for temperature logging
- [ ] Create EnvironmentalMonitoringService with twice-daily recording
- [ ] Implement temperature excursion alerts
- [ ] Extend existing StorageLocation for cryo-box well mapping (81 wells)
- [ ] Create REST endpoints for environmental monitoring
- [ ] Add Liquibase changesets
- [ ] Add unit and integration tests

### Milestone 5: Sample Processing & Aliquoting (Backend)

**Branch**: `feat/001-medical-lab-workflow/m5-processing` **Effort**: 3-4 days
**Dependencies**: M2

Tasks:

- [ ] Create ProcessingRecord valueholder with department-specific methods
- [ ] Create SampleProcessingService for processing workflows
- [ ] Implement parent-child sample relationships for aliquots
- [ ] Extend SampleItem with derived sample support
- [ ] Create REST endpoints for processing
- [ ] Add Liquibase changesets
- [ ] Add unit tests

### Milestone 6: Testing & Instrument Integration (Backend)

**Branch**: `feat/001-medical-lab-workflow/m6-testing-integration` **Effort**:
4-5 days **Dependencies**: M5

Tasks:

- [ ] Create EquipmentUsageLog valueholder for instrument tracking
- [ ] Extend existing analyzer integration for worklist generation
- [ ] Implement automatic reference range flagging (H/L/C)
- [ ] Create TestingService for worklist management
- [ ] Create REST endpoints for testing worklist
- [ ] Add Liquibase changesets
- [ ] Add unit and integration tests

### Milestone 7: Quality Control & Calibration (Backend)

**Branch**: `feat/001-medical-lab-workflow/m7-quality-control` **Effort**: 4-5
days **Dependencies**: M6

Tasks:

- [ ] Create QCResult valueholder with dual-level (Normal/Pathologic) support
- [ ] Create QualityControlService with Westgard rule validation
- [ ] Implement Levey-Jennings chart data generation
- [ ] Add calibration tracking support
- [ ] Add EQA participation recording
- [ ] Create REST endpoints for QC management
- [ ] Add Liquibase changesets
- [ ] Add unit tests

### Milestone 8: Result Validation & Reporting (Backend)

**Branch**: `feat/001-medical-lab-workflow/m8-validation-reporting` **Effort**:
4-5 days **Dependencies**: M6

Tasks:

- [ ] Create ValidationRecord valueholder for approval workflow
- [ ] Create ResultValidationService with department-specific queues
- [ ] Implement result blocking until validation
- [ ] Create report generation service (PDF/Excel/CSV)
- [ ] Implement TAT calculation and sample acceptance rate tracking
- [ ] Create REST endpoints for validation and reporting
- [ ] Add Liquibase changesets
- [ ] Add unit and integration tests

### Milestone 9: Disposal & Archiving (Backend)

**Branch**: `feat/001-medical-lab-workflow/m9-disposal-archiving` **Effort**:
2-3 days **Dependencies**: M8

Tasks:

- [ ] Create DisposalRecord valueholder with method tracking
- [ ] Create DisposalService with compliance verification
- [ ] Implement disposal method enforcement by sample type
- [ ] Add biobank transfer support
- [ ] Create REST endpoints for disposal
- [ ] Add Liquibase changesets
- [ ] Add unit tests

### Milestone 10: Frontend Implementation (React) - Reuse-First

**Branch**: `feat/001-medical-lab-workflow/m10-frontend` **Effort**: 6-8 days
**Dependencies**: M1-M9 (can start partial implementation after M2)

The frontend uses the existing Notebook workflow framework. All 16 pages are
embedded within the Notebook container - users never navigate away.

**Stage 2 (SampleCollectionPage) Component Analysis**:

| Component               | Strategy   | Notes                                             |
| ----------------------- | ---------- | ------------------------------------------------- |
| SampleCollectionPage.js | **EXTEND** | Already exists with manifest import, bulk actions |
| ManifestImportModal.js  | **FORK**   | Create MedLabManifestImportModal with new fields  |
| SampleGrid.js           | **KEEP**   | Reusable sample display grid                      |
| LinkPatientModal.js     | **KEEP**   | Patient search & linking                          |
| LinkOrderModal.js       | **NEW**    | Link samples to orders + assign tests             |

**ManifestImportModal Column Mapping Changes**:

| Current Field        | Action | New Field              |
| -------------------- | ------ | ---------------------- |
| groupIdColumn        | REMOVE | -                      |
| sampleTypeColumn     | KEEP   | sampleTypeColumn       |
| collectionDateColumn | KEEP   | collectionDateColumn   |
| volumeColumn         | RENAME | quantityColumn         |
| numOfSamplesColumn   | REMOVE | -                      |
| notesColumn          | KEEP   | notesColumn            |
| -                    | ADD    | sampleIdColumn         |
| -                    | ADD    | containerTypeColumn    |
| -                    | ADD    | customLabelColumn      |
| -                    | ADD    | unitOfMeasureColumn    |
| -                    | ADD    | collectionSourceColumn |
| -                    | ADD    | collectorColumn        |
| -                    | ADD    | collectionTimeColumn   |
| -                    | ADD    | orderIdColumn          |
| -                    | ADD    | patientIdColumn        |

Tasks (NEW pages only - 8 components):

- [ ] Create PatientOrderEntryPage.js - Patient reg + lab order (Page 1)
- [ ] **EXTEND** SampleCollectionPage.js - Two-step workflow (Page 2): - Add
      step indicator (Import → Link to Orders) - Add "Link to Order"
      button/action alongside "Link to Patient" - Display "Participant" for
      anonymous samples (patientId = NULL) - Show linked order info in sample
      grid (orderId, tests)
- [ ] Create MedLabManifestImportModal.js - MedLab-specific manifest fields: -
      sampleId, sampleTypeId, containerType, customLabel - quantity,
      unitOfMeasure, collectionSource, collector - collectionDate,
      collectionTime, orderId, patientId, notes
- [ ] Create LinkOrderModal.js - Link samples to existing orders: - Search
      orders by labNo, patientName - Display order tests - Assign tests to
      sample (one sample → many Analysis)
- [ ] Create QualityCheckPage.js - Sample-type QC checklist (Page 4)
- [ ] Create TransportPackagingPage.js - IATA PI650 compliance (Page 5)
- [ ] Create StorageAssignmentPage.js - Embeds StorageHierarchySelector (Page 8)
- [ ] Create EnvironmentalMonitoringPage.js - Embeds FreezerMonitoring (Page 9)
- [ ] Create QCDashboardPage.js with LeveyJenningsChart.js (Page 11)
- [ ] Create LabDashboardPage.js - Embeds Dashboard metrics (Page 14)

Tasks (EXTEND existing pages - 5 components):

- [ ] Extend SampleReceptionPage.js - Add medlab QC hooks (Page 3)
- [ ] Extend SampleRoutingPage.js - Department allocation (Page 6)
- [ ] Extend InitialProcessingPage.js - Dept-specific methods (Page 7)
- [ ] Extend AnalysisPage.js - Medlab analyzer support (Page 10)
- [ ] Extend SampleGrid.js - Add utilization actions (Page 15)

Tasks (EMBED existing components - 2 wrappers):

- [ ] Create ResultValidationPage.js - Wraps Validation.js (Page 12)
- [ ] Create SampleUtilizationPage.js - SampleGrid + actions (Page 15)

Tasks (REUSE AS-IS - no changes needed):

- ResultCompilationPage.js (Page 13)
- EndOfProjectArchivingPage.js (Page 16)

Common tasks:

- [ ] Add i18n keys (~200 en/fr translations - reduced due to reuse)
- [ ] Create Cypress E2E tests for medlab workflow

### Milestone 11: ALCOA+ Data Handling & Delta Checks (Backend)

**Branch**: `feat/001-medical-lab-workflow/m11-alcoa-compliance` **Effort**: 2-3
days **Dependencies**: M8 **User Stories**: Cross-cutting (FR-140 to FR-150)

**Goal**: Implement ALCOA+ compliant data handling and Delta checks for result
anomaly detection.

**ALCOA+ Requirements** (per SRS Section 5.3):

- **Attributable**: User identification for all entries
- **Legible**: Clear and readable data
- **Contemporaneous**: Real-time entry timestamps
- **Original**: Original data or certified copy
- **Accurate**: Verified data
- **Complete**: All required data captured
- **Consistent**: Uniform data practices
- **Enduring**: Preserved for required retention period
- **Available**: Accessible when needed

Tasks:

- [ ] Audit existing AuditTrailService for ALCOA+ compliance
- [ ] Implement attributable data validation (user ID on all entries)
- [ ] Implement contemporaneous timestamp validation
- [ ] **Create DeltaCheckService** for previous result comparison (FR-150)
- [ ] Configure Delta check thresholds per test type
- [ ] Create alerts for significant Delta deviations
- [ ] Add unit tests for ALCOA+ compliance validation
- [ ] Add unit tests for Delta check calculations

### Milestone 12: Sample Retrieval & Distribution (Backend)

**Branch**: `feat/001-medical-lab-workflow/m12-sample-retrieval` **Effort**: 2-3
days **Dependencies**: M4 **User Stories**: FR-160 to FR-165

**Goal**: Implement sample retrieval requests, inter-lab transfers, and Material
Transfer Agreements.

Tasks:

- [ ] **Create SampleRetrievalRequest entity** with authorization tracking
- [ ] Create SampleRetrievalDAO and SampleRetrievalService
- [ ] Implement supervisor authorization for external requests (FR-160)
- [ ] Implement retrieval documentation (date/time, personnel, purpose,
      condition) (FR-161)
- [ ] Implement inter-lab transfer with chain of custody (FR-162)
- [ ] Implement temperature monitoring during transfer (FR-163)
- [ ] **Create MaterialTransferAgreement entity** for external distribution
      (FR-164)
- [ ] Implement packaging and shipping documentation (FR-165)
- [ ] Create REST endpoints for sample retrieval
- [ ] Add Liquibase changesets
- [ ] Add unit and integration tests

## Reusable Existing Services

The following existing OpenELIS services will be leveraged:

| Service                | Purpose                           | Extends/Integrates             |
| ---------------------- | --------------------------------- | ------------------------------ |
| PatientService         | Patient registration              | Add participant support        |
| SampleService          | Core sample management            | Add medlab-specific attributes |
| SampleItemService      | Sample item tracking              | Add aliquot relationships      |
| StorageLocationService | Hierarchical storage              | Add cryo-box well mapping      |
| AnalyzerService        | Analyzer configuration            | Use for worklist generation    |
| ResultService          | Test result management            | Integrate with validation      |
| AuditTrailService      | Audit logging                     | Use for all operations         |
| UserService            | User authentication/authorization | Use for RBAC                   |

## API Endpoint Summary

| Category              | Endpoints                                       | Count  |
| --------------------- | ----------------------------------------------- | ------ |
| Patient/Participant   | POST/GET/PUT /rest/patient, /rest/participant   | 6      |
| Lab Orders            | POST/GET /rest/laborder (with container/volume) | 4      |
| Order-Sample Link     | POST/GET /rest/medlab/order-sample-link         | 4      |
| Manifest Import       | POST /rest/medlab/samples/import (CSV upload)   | 2      |
| Sample-Order Linking  | POST /rest/medlab/samples/{id}/link-order       | 2      |
| Sample Collection     | POST/GET /rest/sample/collection                | 4      |
| Sample Reception      | POST/GET/PUT /rest/medlab/reception             | 6      |
| Transport Packaging   | POST/GET /rest/medlab/transport                 | 4      |
| Sample Allocation     | POST/GET /rest/medlab/allocation                | 4      |
| Storage/Environmental | POST/GET /rest/medlab/environmental             | 6      |
| Sample Processing     | POST/GET /rest/medlab/processing                | 4      |
| Testing/Worklist      | POST/GET /rest/medlab/worklist                  | 4      |
| Quality Control       | POST/GET /rest/medlab/qc                        | 6      |
| Result Validation     | POST/GET /rest/medlab/validation                | 6      |
| Delta Checks          | GET /rest/medlab/delta-check                    | 2      |
| Dashboard/Reporting   | GET /rest/medlab/dashboard, /rest/medlab/report | 6      |
| Sample Retrieval      | POST/GET /rest/medlab/retrieval                 | 4      |
| Material Transfer     | POST/GET /rest/medlab/mta                       | 4      |
| Disposal              | POST/GET /rest/medlab/disposal                  | 4      |
| **Total**             |                                                 | **82** |

## Risk Assessment

| Risk                              | Mitigation                                             |
| --------------------------------- | ------------------------------------------------------ |
| Large scope (165 requirements)    | Prioritize P0/P1 requirements first, iterate on P2/P3  |
| Order-driven architecture change  | M1 establishes foundation; validate before proceeding  |
| Order validation at QC            | Comprehensive tests for rejection without orders       |
| Analyzer integration complexity   | Leverage existing AnalyzerService patterns             |
| Performance with concurrent users | Implement proper indexing, batch operations            |
| QC charting (Levey-Jennings)      | Use Carbon Charts library for visualization            |
| ALCOA+ compliance                 | Audit existing AuditTrailService, extend as needed     |
| Delta check accuracy              | Configure thresholds per test type, validate with data |
| Sample retrieval authorization    | Implement supervisor approval workflow                 |

## Definition of Done

Each milestone is complete when:

1. All planned tasks completed
2. Unit tests pass with >70% coverage
3. Integration tests pass
4. Code formatted (`mvn spotless:apply`, `npm run format`)
5. No hardcoded strings (React Intl verified)
6. Constitution compliance verified
7. PR reviewed and approved
8. Documentation updated (quickstart.md, data-model.md)
