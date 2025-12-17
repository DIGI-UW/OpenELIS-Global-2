# Implementation Plan: Immunology Laboratory Workflow

**Branch**: `OGC-51` | **Date**: 2025-12-07 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from
`/specs/OGC-51-immunology-workflow/spec.md`

## Summary

This feature extends the existing OpenELIS Global Notebook/Page architecture to
support a complete 9-step Immunology Laboratory workflow with
per-sample-per-page tracking, bulk operations (200+ samples), branching
workflows (internal analysis, external lab, storage routing), parent-child
sample linking, and analyzer result import with well coordinate matching.

**Technical Approach**: Build on existing NoteBook, NoteBookPage, NoteBookSample
entities. Add new NotebookPageSample junction entity for per-sample-per-page
tracking. Integrate with existing SampleStorageService for storage routing.
Create bulk operation endpoints with batch processing (50 samples per batch).
Implement React frontend with Carbon Design System grid components supporting
virtualized rendering for 200+ samples.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) **Primary Dependencies**:
Spring Framework 6.2.2, Hibernate 6.x, HAPI FHIR R4 6.6.2, React 17, Carbon
Design System v1.15 **Storage**: PostgreSQL 14+, Liquibase 4.8.0 for migrations
**Testing**: JUnit 4 (NOT JUnit 5), Mockito 2.21.0, Cypress 12.17.3 **Target
Platform**: Linux server (Docker), Tomcat 10 WAR deployment **Project Type**:
Web application (Java backend + React frontend) **Performance Goals**: Bulk
operations <30s for 200 samples, Grid rendering <100ms with virtualization
**Constraints**: Max 500 samples per notebook, CSV/Excel up to 10MB, 5
concurrent users per notebook **Scale/Scope**: 200-500 samples per workflow
batch, 9 workflow pages, 15+ API endpoints

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

| Principle                          | Requirement                                          | Status | Notes                                                  |
| ---------------------------------- | ---------------------------------------------------- | ------ | ------------------------------------------------------ |
| I. Configuration-Driven Variation  | Country customizations via config, not code          | PASS   | Workflow pages configurable via template               |
| II. Carbon Design System First     | All UI uses @carbon/react exclusively                | PASS   | Grid, DataTable, Modal, Tag components planned         |
| III. FHIR/IHE Standards Compliance | FHIR R4 for external data                            | PASS   | questionnaire_response_uuid, fhir_uuid fields included |
| IV. Layered Architecture           | 5-layer: Valueholder->DAO->Service->Controller->Form | PASS   | All new entities follow pattern                        |
| V. Test-Driven Development         | TDD, >70% coverage, individual E2E tests             | PASS   | JUnit 4 unit tests, Cypress E2E per page               |
| VI. Database Schema Management     | Liquibase only, no direct DDL                        | PASS   | All tables via Liquibase changesets                    |
| VII. Internationalization First    | React Intl for all strings                           | PASS   | No hardcoded strings                                   |
| VIII. Security & Compliance        | RBAC, audit trail, input validation                  | PASS   | sys_user_id tracking, @SafeHtml validation             |
| IX. Spec-Driven Iteration          | Milestones for >3 day efforts                        | PASS   | 8 milestones planned                                   |

## Project Structure

### Documentation (this feature)

```text
specs/OGC-51-immunology-workflow/
├── plan.md              # This file
├── research.md          # Phase 0 research output
├── data-model.md        # Entity relationship documentation
├── quickstart.md        # Step-by-step developer guide
├── contracts/           # API contracts
│   ├── notebook-sample-entry.yaml
│   ├── notebook-bulk-operations.yaml
│   ├── notebook-routing.yaml
│   └── notebook-analyzer-import.yaml
└── tasks.md             # Phase 2 task breakdown (via /speckit.tasks)
```

### Source Code (repository root)

```text
# Backend (Java)
src/main/java/org/openelisglobal/
├── notebook/
│   ├── valueholder/
│   │   ├── NotebookPageSample.java      # NEW: Per-sample-per-page tracking
│   │   ├── AnalyzerResultImport.java    # NEW: Analyzer import audit
│   │   └── SampleRouting.java           # NEW: Destination routing
│   ├── dao/
│   │   ├── NotebookPageSampleDAO.java   # NEW
│   │   ├── AnalyzerResultImportDAO.java # NEW
│   │   └── SampleRoutingDAO.java        # NEW
│   ├── service/
│   │   ├── NotebookPageSampleService.java    # NEW
│   │   ├── NotebookBulkOperationService.java # NEW
│   │   ├── SampleRoutingService.java         # NEW
│   │   └── AnalyzerImportService.java        # NEW
│   ├── controller/rest/
│   │   ├── NotebookSampleEntryController.java    # NEW: Sample entry endpoints
│   │   ├── NotebookBulkOperationController.java  # NEW: Bulk operation endpoints
│   │   ├── NotebookRoutingController.java        # NEW: Routing endpoints
│   │   └── NotebookAnalyzerImportController.java # NEW: Analyzer import
│   └── form/
│       ├── NotebookPageSampleForm.java  # NEW
│       └── AnalyzerImportForm.java      # NEW

src/main/resources/liquibase/
└── 3.4.x.x/
    ├── 001-notebook-page-sample.xml     # NEW: Per-sample tracking table
    ├── 002-analyzer-result-import.xml   # NEW: Import audit table
    └── 003-sample-routing.xml           # NEW: Routing table

# Frontend (React)
frontend/src/
├── components/
│   └── notebook/
│       ├── workflow/
│       │   ├── NotebookWorkflowTab.js       # NEW: Main workflow container
│       │   ├── PageNavigation.js            # NEW: 9-page navigation
│       │   ├── SampleGrid.js                # NEW: Virtualized sample grid
│       │   ├── BulkApplyForm.js             # NEW: Bulk value application
│       │   ├── SampleRoutingPanel.js        # NEW: Destination routing UI
│       │   ├── AnalyzerImportModal.js       # NEW: Import wizard
│       │   ├── BoxLayoutViewer.js           # NEW: Well plate visualization
│       │   └── ManifestImportModal.js       # NEW: CSV sample creation
│       └── pages/
│           ├── SampleReceptionPage.js       # NEW: Page 1
│           ├── InitialProcessingPage.js     # NEW: Page 2
│           ├── ChildSampleCreationPage.js   # NEW: Page 4
│           ├── AnalyzerResultsPage.js       # NEW: Page 6
│           └── ResultCompilationPage.js     # NEW: Page 8
└── languages/
    ├── en.json                              # UPDATE: Add ~150 new keys
    └── fr.json                              # UPDATE: Add ~150 new keys

# Tests
src/test/java/org/openelisglobal/notebook/
├── service/NotebookPageSampleServiceTest.java
├── service/NotebookBulkOperationServiceTest.java
├── dao/NotebookPageSampleDAOTest.java
└── controller/NotebookBulkOperationControllerTest.java

frontend/cypress/e2e/
├── notebookWorkflowSampleReception.cy.js
├── notebookWorkflowBulkOperations.cy.js
├── notebookWorkflowRouting.cy.js
└── notebookWorkflowAnalyzerImport.cy.js
```

**Structure Decision**: Web application pattern selected. Backend extends
existing notebook module (`org.openelisglobal.notebook`). Frontend extends
existing `components/notebook/` directory with new `workflow/` and `pages/`
subdirectories.

## Complexity Tracking

> No violations requiring justification. All patterns follow constitution.

## Implementation Milestones

### Milestone 1: NotebookPageSample Entity & DAO (Backend Foundation)

**Branch**: `feat/OGC-51-immunology-workflow/m1-page-sample-entity` **Effort**:
2-3 days **Dependencies**: None

Tasks:

- [ ] Create NotebookPageSample valueholder with JPA annotations
- [ ] Create Liquibase changeset for notebook_page_sample table
- [ ] Create NotebookPageSampleDAO interface and implementation
- [ ] Add ORM validation test
- [ ] Add unit tests for DAO

### Milestone 2: Sample Entry & Linking Service (Backend)

**Branch**: `feat/OGC-51-immunology-workflow/m2-sample-entry-service`
**Effort**: 3-4 days **Dependencies**: M1

Tasks:

- [ ] Create NotebookSampleEntryService for sample linking
- [ ] Implement manifest CSV parsing and sample creation
- [ ] Implement sample search by accession number
- [ ] Create REST endpoints for sample entry (POST /notebook/{id}/samples/\*)
- [ ] Add unit and integration tests

### Milestone 3: Bulk Operations Service (Backend)

**Branch**: `feat/OGC-51-immunology-workflow/m3-bulk-operations` **Effort**: 3-4
days **Dependencies**: M1

Tasks:

- [ ] Create NotebookBulkOperationService with batch processing (50 samples)
- [ ] Implement status update endpoint (POST
      /notebook/bulk/page/{id}/samples/status)
- [ ] Implement bulk apply endpoint (POST
      /notebook/bulk/page/{id}/samples/apply)
- [ ] Implement progress endpoint (GET /notebook/bulk/page/{id}/progress)
- [ ] Add unit and integration tests

### Milestone 4: Sample Routing & Storage Integration (Backend)

**Branch**: `feat/OGC-51-immunology-workflow/m4-routing` **Effort**: 3-4 days
**Dependencies**: M1

Tasks:

- [ ] Create SampleRouting valueholder and Liquibase changeset
- [ ] Create SampleRoutingService integrating with SampleStorageService
- [ ] Implement well coordinate auto-assignment (row-major A1, A2...B1)
- [ ] Create routing endpoints (POST /notebook/{id}/samples/route)
- [ ] Create box layout endpoint (GET /notebook/{id}/box/{boxId}/layout)
- [ ] Add unit and integration tests

### Milestone 5: Analyzer Result Import (Backend)

**Branch**: `feat/OGC-51-immunology-workflow/m5-analyzer-import` **Effort**: 3-4
days **Dependencies**: M4

Tasks:

- [ ] Create AnalyzerResultImport valueholder and Liquibase changeset
- [ ] Create AnalyzerImportService with CSV/Excel parsing
- [ ] Implement well coordinate matching
- [ ] Implement sample ID fallback matching
- [ ] Create import endpoint (POST /notebook/bulk/page/{id}/analyzer-import)
- [ ] Add unit and integration tests

### Milestone 6: Workflow UI Foundation (Frontend)

**Branch**: `feat/OGC-51-immunology-workflow/m6-workflow-ui` **Effort**: 4-5
days **Dependencies**: M2, M3

Tasks:

- [ ] Create NotebookWorkflowTab container component
- [ ] Create PageNavigation component (9 pages with progress)
- [ ] Create SampleGrid with Carbon DataTable and virtualization
- [ ] Create BulkApplyForm component
- [ ] Add internationalization keys (en.json, fr.json)
- [ ] Add Jest unit tests

### Milestone 7: Routing & Analyzer UI (Frontend)

**Branch**: `feat/OGC-51-immunology-workflow/m7-routing-analyzer-ui` **Effort**:
4-5 days **Dependencies**: M4, M5, M6

Tasks:

- [ ] Create SampleRoutingPanel with destination cards
- [ ] Create BoxLayoutViewer for well plate visualization
- [ ] Create ManifestImportModal for CSV sample creation
- [ ] Create AnalyzerImportModal wizard (upload, map, preview, import)
- [ ] Add internationalization keys
- [ ] Add Jest unit tests

### Milestone 8: E2E Tests & Integration

**Branch**: `feat/OGC-51-immunology-workflow/m8-e2e-integration` **Effort**: 3-4
days **Dependencies**: M6, M7

Tasks:

- [ ] Create Cypress E2E tests for sample reception workflow
- [ ] Create Cypress E2E tests for bulk operations
- [ ] Create Cypress E2E tests for routing workflow
- [ ] Create Cypress E2E tests for analyzer import
- [ ] Verify test data fixtures
- [ ] Performance validation (200 samples, <30s operations)

## Risk Assessment

| Risk                              | Impact | Mitigation                                                  |
| --------------------------------- | ------ | ----------------------------------------------------------- |
| Grid performance with 500 samples | High   | Use react-virtualized or react-window for rendering         |
| Bulk operation timeouts           | High   | Batch processing (50/batch), async progress tracking        |
| Analyzer file format variations   | Medium | Support multiple CSV dialects, provide clear error messages |
| Concurrent edit conflicts         | Medium | Optimistic locking on notebook_page_sample                  |
| Storage capacity edge cases       | Low    | Soft warnings (existing behavior), allow over-assignment    |

## Research Completed

See [research.md](research.md) for detailed findings on:

- Existing NoteBook, NoteBookPage, NoteBookSample entity structure
- SampleStorageAssignment and well coordinate system
- SampleItem parent-child aliquoting relationships
- Frontend patterns for DataTable, bulk operations, CSV import
