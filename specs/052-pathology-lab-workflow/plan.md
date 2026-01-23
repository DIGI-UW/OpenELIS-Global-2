# Implementation Plan: Pathology Laboratory Workflow

**Branch**: `052-pathology-lab-workflow` | **Date**: 2025-12-14 | **Spec**:
[spec.md](spec.md) **Input**: Feature specification from
`/specs/052-pathology-lab-workflow/spec.md`

## Summary

Implement a comprehensive Pathology Laboratory Workflow module for OpenELIS
Global, extending the existing Notebook/Page architecture to support
pathology-specific sample processing. The feature includes:

- **Sample Reception & Registration**: Clinical and research specimen intake
  with category-specific metadata
- **Quality Control**: Initial inspection and post-embedding tissue block QC
  with specimen-type-specific criteria
- **Sample Processing**: Grossing, aliquoting, and parent-child sample tracking
- **Testing & Microscopy**: Staining procedures (H&E, IHC, special stains) with
  control validation
- **Storage & Inventory**: Multi-temperature storage with environmental
  monitoring
- **Performance Monitoring**: TAT, rejection rates, and monthly reporting
- **Disposal & Archiving**: Retention policy enforcement and audit-ready
  archiving
- **Reference Module**: SOP management with version control
- **Access Control**: Project-based restrictions for research samples

This feature reuses patterns from OGC-51 Immunology Workflow
(Notebook/NoteBookPage/NotebookEntry, SampleItem parent-child,
SampleStorageAssignment) while adding pathology-specific entities and workflows.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) **Primary Dependencies**:
Spring Framework 6.2.2 (Traditional Spring MVC), Hibernate ORM 5.6.15.Final,
HAPI FHIR R4 6.6.2 **Storage**: PostgreSQL 14+ **Testing**: JUnit 4.13.1 +
Mockito 2.21.0 (backend), Cypress 12.17.3 (E2E), Jest + React Testing Library
(frontend) **Target Platform**: Docker containers (Tomcat 10 WAR deployment),
Ubuntu 20.04+ host **Project Type**: Web application (Java backend + React
frontend) **Performance Goals**: Sample registration <5s, QC operations <5s,
storage search <2s for 10,000 samples **Constraints**: Constitution compliance,
Carbon Design System UI, React Intl i18n, Liquibase migrations **Scale/Scope**:
~1000 samples/month, 5 concurrent users, 10 new pages/forms

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

### Pre-Design Constitution Compliance

| Principle                          | Status | Compliance Approach                                                                                                                                   |
| ---------------------------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| I. Configuration-Driven Variation  | PASS   | Sample types, QC criteria, retention policies via database configuration. No country-specific code branches.                                          |
| II. Carbon Design System First     | PASS   | All UI components use @carbon/react v1.15+. Forms use Carbon TextInput, Dropdown, RadioButtonGroup, DataTable.                                        |
| III. FHIR/IHE Standards Compliance | PASS   | PathologySampleRegistration, QualityControlRecord include fhir_uuid for FHIR sync. TestResultRecord maps to DiagnosticReport/Observation.             |
| IV. Layered Architecture Pattern   | PASS   | Strict 5-layer: Valueholder → DAO → Service → Controller → Form. @Transactional in services only. Services compile all data within transaction.       |
| V. Test-Driven Development         | PASS   | TDD for all services. Unit tests (JUnit 4), ORM validation tests, integration tests (BaseWebContextSensitiveTest), E2E (Cypress). >70% coverage goal. |
| VI. Database Schema Management     | PASS   | All tables via Liquibase changesets in src/main/resources/liquibase/pathology/. Rollback scripts provided.                                            |
| VII. Internationalization First    | PASS   | All UI strings via React Intl. New keys in en.json, fr.json. No hardcoded strings.                                                                    |
| VIII. Security & Compliance        | PASS   | RBAC for project access control. Audit trail (sys_user_id + lastupdated). Input validation via Hibernate Validator.                                   |
| IX. Spec-Driven Iteration          | PASS   | Feature >3 days. Will use milestone branches: m1-entities, m2-backend-services, m3-controllers, m4-frontend.                                          |

**Gate Result**: PASS - No constitution violations. Proceed to Phase 0.

## Project Structure

### Documentation (this feature)

```text
specs/052-pathology-lab-workflow/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# Backend (Java)
src/main/java/org/openelisglobal/pathology/
├── valueholder/                    # JPA Entities
│   ├── PathologySampleRegistration.java
│   ├── QualityControlRecord.java
│   ├── ProcessingLogEntry.java
│   ├── TestResultRecord.java
│   ├── StorageEnvironmentLog.java
│   ├── ReferenceDocument.java
│   └── ProjectAccess.java
├── dao/                            # Data Access Objects
│   ├── PathologySampleRegistrationDAO.java
│   ├── PathologySampleRegistrationDAOImpl.java
│   ├── QualityControlRecordDAO.java
│   ├── QualityControlRecordDAOImpl.java
│   ├── ProcessingLogEntryDAO.java
│   ├── ProcessingLogEntryDAOImpl.java
│   ├── TestResultRecordDAO.java
│   ├── TestResultRecordDAOImpl.java
│   ├── StorageEnvironmentLogDAO.java
│   ├── StorageEnvironmentLogDAOImpl.java
│   ├── ReferenceDocumentDAO.java
│   ├── ReferenceDocumentDAOImpl.java
│   ├── ProjectAccessDAO.java
│   └── ProjectAccessDAOImpl.java
├── service/                        # Business Logic
│   ├── PathologySampleRegistrationService.java
│   ├── PathologySampleRegistrationServiceImpl.java
│   ├── QualityControlService.java
│   ├── QualityControlServiceImpl.java
│   ├── ProcessingLogService.java
│   ├── ProcessingLogServiceImpl.java
│   ├── TestResultService.java
│   ├── TestResultServiceImpl.java
│   ├── StorageEnvironmentService.java
│   ├── StorageEnvironmentServiceImpl.java
│   ├── ReferenceDocumentService.java
│   ├── ReferenceDocumentServiceImpl.java
│   ├── ProjectAccessService.java
│   ├── ProjectAccessServiceImpl.java
│   └── PathologyReportingService.java  # Performance metrics
├── controller/rest/                # REST Controllers
│   ├── PathologySampleController.java
│   ├── QualityControlController.java
│   ├── ProcessingLogController.java
│   ├── TestResultController.java
│   ├── StorageEnvironmentController.java
│   ├── ReferenceDocumentController.java
│   └── ProjectAccessController.java
└── form/                           # DTOs
    ├── PathologySampleForm.java
    ├── QualityControlForm.java
    ├── ProcessingLogForm.java
    ├── TestResultForm.java
    └── ReferenceDocumentForm.java

# Database Migrations
src/main/resources/liquibase/pathology/
├── pathology-001-sample-registration.xml
├── pathology-002-quality-control.xml
├── pathology-003-processing-log.xml
├── pathology-004-test-result.xml
├── pathology-005-storage-environment.xml
├── pathology-006-reference-document.xml
├── pathology-007-project-access.xml
└── pathology-008-seed-data.xml

# Backend Tests
src/test/java/org/openelisglobal/pathology/
├── HibernateMappingValidationTest.java   # ORM validation
├── service/                               # Unit tests
│   ├── PathologySampleRegistrationServiceTest.java
│   ├── QualityControlServiceTest.java
│   ├── ProcessingLogServiceTest.java
│   ├── TestResultServiceTest.java
│   └── ...
├── controller/                            # Integration tests
│   ├── PathologySampleControllerTest.java
│   ├── QualityControlControllerTest.java
│   └── ...
└── dao/                                   # DAO tests
    ├── PathologySampleRegistrationDAOTest.java
    └── ...

# Frontend (React)
frontend/src/components/pathology/
├── PathologyDashboard.js               # Main entry point
├── pages/
│   ├── SampleReceptionPage.js          # Sample registration form
│   ├── QualityControlPage.js           # QC inspection
│   ├── BlockQCPage.js                  # Tissue block QC
│   ├── ProcessingPage.js               # Grossing/aliquoting
│   ├── TestingPage.js                  # Staining & results
│   ├── StoragePage.js                  # Storage assignment
│   ├── PerformancePage.js              # Metrics dashboard
│   ├── DisposalPage.js                 # Disposal workflow
│   └── ReferenceModulePage.js          # SOP management
├── components/
│   ├── ClinicalSpecimenForm.js         # Clinical sample fields
│   ├── ResearchSpecimenForm.js         # Research sample fields
│   ├── QCChecklist.js                  # Dynamic QC criteria
│   ├── BlockQCForm.js                  # Block QC criteria
│   ├── GrossingForm.js                 # Grossing entry
│   ├── AliquotForm.js                  # Aliquot creation
│   ├── TestResultForm.js               # Test result entry
│   ├── ControlValidation.js            # IHC control checker
│   ├── TemperatureLogTable.js          # Environment monitoring
│   ├── DocumentUploader.js             # SOP upload
│   └── DocumentVersionHistory.js       # Version tracking
└── __tests__/
    ├── SampleReceptionPage.test.jsx
    ├── QualityControlPage.test.jsx
    └── ...

# E2E Tests
frontend/cypress/e2e/
├── pathologySampleReception.cy.js
├── pathologyQualityControl.cy.js
├── pathologyProcessing.cy.js
├── pathologyTesting.cy.js
├── pathologyStorage.cy.js
├── pathologyReports.cy.js
├── pathologyDisposal.cy.js
└── pathologyReferenceModule.cy.js

# Internationalization
frontend/src/languages/
├── en.json                            # Add pathology.* keys
└── fr.json                            # French translations
```

**Structure Decision**: Web application structure following existing OpenELIS
patterns. Backend in `src/main/java/org/openelisglobal/pathology/`, frontend in
`frontend/src/components/pathology/`. Reuses existing Notebook infrastructure
from OGC-51.

## Reusable Components from OGC-51 Immunology Workflow

Based on OGC-51 spec analysis, the following components can be directly reused:

### Backend Services (Existing)

- `NoteBookService` / `NoteBookServiceImpl` - Template and instance management
- `NoteBookPageService` - Page navigation and completion tracking
- `NotebookSampleEntryService` - Sample linking to notebooks
- `SampleStorageAssignmentService` - Storage location assignment
- `SampleStorageMovementService` - Movement audit trail
- `ResultCompilationService` - Report generation patterns

### Frontend Components (Existing)

- `NoteBookDashBoard.js` - Notebook navigation
- `NotebookWorkflowNav.js` - Page progress indicators
- Carbon DataTable patterns for sample grids
- Bulk operation patterns (Select All, Apply to Selected)

### Patterns to Extend

- `NotebookPageSample` entity pattern for per-sample-per-page tracking
- Parent-child sample relationships via `SampleItemAliquotRelationship`
- File attachment patterns from notebook files

## Complexity Tracking

> **No violations to justify** - All constitution principles pass.

## Implementation Milestones (IX. Spec-Driven Iteration)

Based on Constitution Principle IX, this feature (~3+ weeks effort) is broken
into validation milestones:

| Milestone | Branch                                           | Description                          | Dependencies |
| --------- | ------------------------------------------------ | ------------------------------------ | ------------ |
| M1        | `feat/052-pathology-lab-workflow/m1-entities`    | JPA entities + Liquibase + ORM tests | None         |
| M2        | `feat/052-pathology-lab-workflow/m2-services`    | Service layer + unit tests           | M1           |
| M3        | `feat/052-pathology-lab-workflow/m3-controllers` | REST controllers + integration tests | M2           |
| M4 [P]    | `feat/052-pathology-lab-workflow/m4-frontend`    | React pages + Jest tests             | M3           |
| M5 [P]    | `feat/052-pathology-lab-workflow/m5-e2e`         | Cypress E2E tests                    | M4           |

[P] = Parallel milestones (can be developed simultaneously after M3)

## Risk Assessment

| Risk                      | Likelihood | Impact | Mitigation                                                                           |
| ------------------------- | ---------- | ------ | ------------------------------------------------------------------------------------ |
| QC criteria complexity    | Medium     | Medium | Use JSONB for flexible criteria storage; configuration-driven                        |
| Test result variety       | Medium     | Medium | Generic TestResultRecord with JSONB result_data; type-specific validation in service |
| Performance (10k samples) | Low        | High   | Index notebook_page_id, sample_item_id; pagination on all queries                    |
| SOP file storage          | Low        | Medium | Reuse existing file attachment patterns from notebook files                          |
| Access control complexity | Medium     | High   | Clear ProjectAccess junction table; filter at service layer                          |

## Next Steps

1. **Phase 0**: Generate `research.md` - Research existing OGC-51 patterns, FHIR
   mappings for pathology
2. **Phase 1**: Generate `data-model.md` - Entity definitions with relationships
3. **Phase 1**: Generate `contracts/` - OpenAPI specifications for REST
   endpoints
4. **Phase 1**: Generate `quickstart.md` - Developer onboarding guide
5. **Phase 2** (via `/speckit.tasks`): Generate `tasks.md` - Dependency-ordered
   implementation tasks
