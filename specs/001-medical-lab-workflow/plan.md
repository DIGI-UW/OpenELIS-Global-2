# Implementation Plan: Medical Laboratory Workflow

**Branch**: `001-medical-lab-workflow` | **Date**: 2024-12-14 | **Spec**:
[spec.md](spec.md) **Input**: Feature specification from
`/specs/001-medical-lab-workflow/spec.md`

## Summary

This feature implements a comprehensive Medical Laboratory workflow system
covering the complete sample lifecycle from patient registration through
disposal/archiving. The system extends existing OpenELIS Global services
(Patient, Sample, Storage) with new modules for quality control, sample
tracking, transport packaging validation, environmental monitoring, testing with
instrument integration, result validation, and compliance reporting.

**Technical Approach**: Build on existing OpenELIS infrastructure
(PatientService, SampleService, StorageService, AnalyzerService). Create new
service modules for QC management, transport packaging, environmental
monitoring, result validation, disposal tracking, and analytics dashboards.
Implement React frontend with Carbon Design System for 19 workflow pages. Use
Liquibase for all schema changes, React Intl for internationalization.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) **Primary Dependencies**:
Spring Framework 6.2.2, Hibernate 6.x, HAPI FHIR R4 6.6.2, React 17, Carbon
Design System v1.15 **Storage**: PostgreSQL 14+, Liquibase 4.8.0 for migrations
**Testing**: JUnit 4 (NOT JUnit 5), Mockito 2.21.0, Cypress 12.17.3 **Target
Platform**: Linux server (Docker), Tomcat 10 WAR deployment **Project Type**:
Web application (Java backend + React frontend) **Performance Goals**: Single
sample operations <3s, bulk operations (100 samples) <30s, dashboard refresh
<30s **Constraints**: Support 20 concurrent users, twice-daily temperature
logging, <60s report generation **Scale/Scope**: 19 UI pages, 132+ functional
requirements, 6 laboratory departments, 10+ entity types

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
| VIII. Security & Compliance        | RBAC, audit trail, input validation                  | PASS   | Department-based access control, complete audit trail               |
| IX. Spec-Driven Iteration          | Milestones for >3 day efforts                        | PASS   | 10 milestones planned covering all 10 workflow phases               |

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

### Milestone 1: Foundation - Patient & Lab Orders (Backend)

**Branch**: `feat/001-medical-lab-workflow-m1-patient-orders` **Effort**: 2-3
days **Dependencies**: None

Tasks:

- [ ] Verify existing PatientService meets medlab requirements (REUSE AS-IS)
- [ ] Verify existing LabOrder/TestRequest integration (REUSE AS-IS)
- [ ] Create MedLabNotebook template configuration in NoteBookService
- [ ] Define 16-page workflow template for Medical Lab notebook
- [ ] Add ORM validation tests for notebook integration
- [ ] Add unit tests for notebook template creation

### Milestone 2: Sample Collection & Reception QC (Backend)

**Branch**: `feat/001-medical-lab-workflow/m2-collection-reception` **Effort**:
4-5 days **Dependencies**: M1

Tasks:

- [ ] Create QualityCheck valueholder with sample-type-specific criteria
- [ ] Create QualityCheckDAO and SampleReceptionService
- [ ] Implement sample-type-specific quality validation (Chemistry, Hematology,
      Stool, Urine, Microbiology)
- [ ] Create SampleAllocation entity for department routing
- [ ] Implement corrective action workflow (recollection, return to submitter)
- [ ] Create REST endpoints for sample reception
- [ ] Add Liquibase changesets
- [ ] Add unit and integration tests

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

Tasks (NEW pages only - 8 components):

- [ ] Create PatientOrderEntryPage.js - Patient reg + lab order (Page 1)
- [ ] Create SampleCollectionPage.js - Specimen collection (Page 2)
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
| Lab Orders            | POST/GET /rest/laborder                         | 4      |
| Sample Collection     | POST/GET /rest/sample/collection                | 4      |
| Sample Reception      | POST/GET/PUT /rest/medlab/reception             | 6      |
| Transport Packaging   | POST/GET /rest/medlab/transport                 | 4      |
| Sample Allocation     | POST/GET /rest/medlab/allocation                | 4      |
| Storage/Environmental | POST/GET /rest/medlab/environmental             | 6      |
| Sample Processing     | POST/GET /rest/medlab/processing                | 4      |
| Testing/Worklist      | POST/GET /rest/medlab/worklist                  | 4      |
| Quality Control       | POST/GET /rest/medlab/qc                        | 6      |
| Result Validation     | POST/GET /rest/medlab/validation                | 6      |
| Dashboard/Reporting   | GET /rest/medlab/dashboard, /rest/medlab/report | 6      |
| Disposal              | POST/GET /rest/medlab/disposal                  | 4      |
| **Total**             |                                                 | **64** |

## Risk Assessment

| Risk                              | Mitigation                                         |
| --------------------------------- | -------------------------------------------------- |
| Large scope (132 requirements)    | Prioritize P1 requirements first, iterate on P2/P3 |
| Analyzer integration complexity   | Leverage existing AnalyzerService patterns         |
| Performance with concurrent users | Implement proper indexing, batch operations        |
| QC charting (Levey-Jennings)      | Use Carbon Charts library for visualization        |
| Compliance requirements           | Document audit trail coverage, test thoroughly     |

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
