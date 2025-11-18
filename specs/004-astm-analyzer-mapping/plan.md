# Implementation Plan: ASTM Analyzer Field Mapping

**Branch**: `004-astm-analyzer-mapping` | **Date**: 2025-11-14 | **Spec**:
[spec.md](./spec.md) **Input**: Feature specification from
`/specs/004-astm-analyzer-mapping/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See
`.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Implement comprehensive ASTM analyzer field mapping feature to enable laboratory
administrators to configure field mappings between ASTM analyzer test codes,
units, and qualitative values and OpenELIS tests, analytes, and result fields.
The feature includes three main user workflows: (1) Configure field mappings for
new analyzers (P1), (2) Maintain mappings as instruments change (P2), and (3)
Resolve unmapped or failed analyzer messages (P3). The system provides a
dual-panel mapping interface, analyzer management (CRUD operations), error
dashboard for unmapped messages, and integration with existing ASTM message
processing infrastructure. The left-hand navigation must mirror the latest Figma
hierarchy: a single “Analyzers” parent node that expands to Analyzers Dashboard,
Error Dashboard, Field Mappings (contextual), plus Quality Control placeholders
(main QC dashboard, “QC Alerts & Violations”, “Corrective Actions”) that link
into feature `003-westgard-qc`.

**Technical Approach**: Extend existing OpenELIS analyzer infrastructure (legacy
`Analyzer` entity, `ASTMAnalyzerReader`, `AnalyzerImportController`) with new
annotation-based JPA entities for analyzer field mapping. Create new REST API
endpoints following 5-layer architecture pattern. Build Carbon Design System UI
components for analyzer management, field mapping interface, and error
dashboard. Integrate with existing ASTM message processing to apply mappings
during message interpretation. Support query analyzer functionality to retrieve
available fields from analyzers via ASTM protocol. Integrate navigation with
existing left-hand navigation bar using unified tab-navigation pattern (sub-nav
items function as tabs, backend-driven via `/rest/menu` API, no separate Carbon
Tabs components) while surfacing the future QC routes noted above.

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**:

- Backend: Spring Boot 3.x, Hibernate 6.x, HAPI FHIR R4 (v6.6.2), JPA
  (jakarta.persistence), Liquibase 4.8.0
- Frontend: @carbon/react v1.15.0, React Intl 5.20.12, Formik 2.2.9, SWR 2.0.3,
  React Router DOM 5.2.0
- ASTM Protocol: Existing `ASTMAnalyzerReader` and `AnalyzerImportController`
  infrastructure

**Storage**: PostgreSQL 14+ (existing OpenELIS database)  
**Testing**:

- Backend: JUnit 4 (4.13.1) + Mockito 2.21.0 (unit/integration), ORM validation
  tests (Hibernate SessionFactory build)
- Frontend: Jest + React Testing Library (unit), Cypress 12.17.3 (E2E -
  individual test execution during development)
- FHIR: Resource validation against R4 profiles (if analyzer entities exposed
  externally)

**Target Platform**: Web application (React frontend, Spring Boot backend)  
**Project Type**: Web application (frontend + backend)  
**Performance Goals**:

- Mapping UI: <500ms response time for field queries, <2s for mapping save
  operations
- ASTM message processing: Apply mappings in <100ms per message (non-blocking)
- Error dashboard: Load 1000+ error records with pagination in <1s

**Constraints**:

- MUST use annotation-based JPA mappings (NO XML mapping files per Constitution
  IV)
- MUST follow 5-layer architecture (Valueholder→DAO→Service→Controller→Form)
- MUST use Carbon Design System exclusively (NO Bootstrap/Tailwind)
- MUST internationalize all UI strings via React Intl
- MUST use Liquibase for all schema changes
- MUST maintain backward compatibility with existing analyzer plugin system
- MUST keep all analyzer-related navigation under a single “Analyzers” parent
  node (per Figma), exposing QC placeholder routes alongside the ASTM-specific
  pages so the hierarchy remains consistent with feature `003-westgard-qc`

**Scale/Scope**:

- Entities: Analyzer, AnalyzerField, AnalyzerFieldMapping,
  QualitativeResultMapping, UnitMapping, AnalyzerError
- UI Pages: Analyzers List, Field Mappings (dual-panel), Error Dashboard
- API Endpoints: ~15 REST endpoints for CRUD operations, query analyzer, test
  mapping, reprocess errors
- Integration: Extend existing ASTM message processing pipeline

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: No country-specific code branches planned

  - Analyzer-specific unit preferences and code systems will be handled via
    database configuration, not code branching
  - Mapping validation rules configurable via properties files

- [x] **Carbon Design System**: UI uses @carbon/react exclusively (NO
      Bootstrap/Tailwind)

  - All UI components specified in FR-001 through FR-020 use Carbon components
    (DataTable, ComposedModal, Search, MultiSelect, Tag, SideNavMenu,
    SideNavMenuItem, etc.)
  - **Navigation**: Sub-navigation items function as tabs (NO Carbon
    Tabs/TabList components per FR-020 unified tab-navigation pattern)
  - Field type color coding uses Carbon design tokens ($blue-60, $purple-60,
    etc.)
  - Typography follows Carbon standards ($heading-04, $body-01, etc.)

- [x] **FHIR/IHE Compliance**: External data integrates via FHIR R4 + IHE
      profiles

  - Analyzer entities may be exposed externally (if required by national health
    information exchanges)
  - If exposed, MUST include `fhir_uuid UUID` column and bidirectional transform
  - Use existing `FhirPersistanceService` and `FhirTransformService`
    infrastructure
  - Note: Analyzer mapping configuration itself is internal; only analyzer
    results (if exposed) require FHIR compliance

- [x] **Layered Architecture**: Backend follows 5-layer pattern
      (Valueholder→DAO→Service→Controller→Form)

  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files -
    legacy exempt until refactored)
    - New entities (AnalyzerField, AnalyzerFieldMapping, etc.) will use
      annotation-based mappings
    - Legacy `Analyzer` entity uses XML mappings (exempt until refactored per
      Constitution IV)
  - **Transaction management MUST be in service layer only** - NO
    `@Transactional` annotations on controller methods
  - **Data Compilation Rule**: Services MUST eagerly fetch ALL data needed for
    responses within transaction using JOIN FETCH
  - Controllers MUST NOT traverse entity relationships (prevents
    LazyInitializationException)

- [x] **Test Coverage**: Unit + ORM validation (if applicable) + integration +
      E2E tests planned (>70% coverage goal per Constitution V.4 and V.5)

  - Unit tests: JUnit 4 + Mockito for service layer business logic
  - ORM validation tests: Hibernate SessionFactory build test for all new
    entities (<5s, no database)
  - Integration tests: Spring Test for full-stack API endpoint validation
  - E2E tests: Cypress tests for user workflows (individual test execution
    during development)
  - E2E tests MUST follow Cypress best practices (Constitution V.5):
    - Run tests individually during development (not full suite)
    - Maximum 5-10 test cases per execution during development
    - Browser console logging enabled and reviewed after each run
    - Video recording disabled by default
    - Post-run review of console logs and screenshots required

- [x] **Schema Management**: Database changes via Liquibase changesets only

  - All new tables (analyzer_field, analyzer_field_mapping,
    qualitative_result_mapping, unit_mapping, analyzer_error) via Liquibase
  - Extensions to existing `analyzer` table (if needed) via Liquibase
  - Rollback scripts provided for all structural changes

- [x] **Internationalization**: All UI strings use React Intl (no hardcoded
      text)

  - All labels, tooltips, messages, error text externalized to
    `frontend/src/languages/{locale}.json`
  - Minimum translations: English (en) + French (fr)
  - Date/time formatting via `intl.formatDate()`, `intl.formatTime()`
  - Number formatting via `intl.formatNumber()`

- [x] **Security & Compliance**: RBAC, audit trail, input validation included
  - Role-based access control: LAB_USER (view), LAB_SUPERVISOR (view +
    acknowledge errors), System Administrator (edit + activate mappings)
  - Audit trail: All mapping changes logged with user ID + timestamp
    (BaseObject.sys_user_id, BaseObject.lastupdated)
  - Input validation: Hibernate Validator on entities, Formik validation on
    frontend
  - SQL injection prevention: JPA/Hibernate parameterized queries only (NO
    native SQL)
  - XSS prevention: React Intl escaping, Carbon component sanitization

**Complexity Justification Required If**:

- N/A - No violations identified. All requirements align with constitution
  principles.

## Project Structure

### Documentation (this feature)

```text
specs/004-astm-analyzer-mapping/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

**Structure Decision**: Web application (frontend + backend) - follows existing
OpenELIS structure

```text
# Backend (Java/Spring Boot)
src/main/java/org/openelisglobal/analyzer/
├── valueholder/          # JPA Entities (annotation-based)
│   ├── AnalyzerField.java
│   ├── AnalyzerFieldMapping.java
│   ├── QualitativeResultMapping.java
│   ├── UnitMapping.java
│   └── AnalyzerError.java
├── dao/                  # Data Access Layer
│   ├── AnalyzerFieldDAO.java
│   ├── AnalyzerFieldDAOImpl.java
│   ├── AnalyzerFieldMappingDAO.java
│   ├── AnalyzerFieldMappingDAOImpl.java
│   ├── QualitativeResultMappingDAO.java
│   ├── QualitativeResultMappingDAOImpl.java
│   ├── UnitMappingDAO.java
│   ├── UnitMappingDAOImpl.java
│   ├── AnalyzerErrorDAO.java
│   └── AnalyzerErrorDAOImpl.java
├── service/             # Business Logic Layer
│   ├── AnalyzerFieldService.java
│   ├── AnalyzerFieldServiceImpl.java
│   ├── AnalyzerFieldMappingService.java
│   ├── AnalyzerFieldMappingServiceImpl.java
│   ├── QualitativeResultMappingService.java
│   ├── QualitativeResultMappingServiceImpl.java
│   ├── UnitMappingService.java
│   ├── UnitMappingServiceImpl.java
│   ├── AnalyzerErrorService.java
│   ├── AnalyzerErrorServiceImpl.java
│   ├── AnalyzerQueryService.java
│   └── AnalyzerQueryServiceImpl.java
├── controller/           # REST API Endpoints
│   ├── AnalyzerRestController.java
│   ├── AnalyzerFieldMappingRestController.java
│   └── AnalyzerErrorRestController.java
└── form/                 # DTOs/Forms
    ├── AnalyzerForm.java
    ├── AnalyzerFieldForm.java
    ├── AnalyzerFieldMappingForm.java
    ├── QualitativeResultMappingForm.java
    ├── UnitMappingForm.java
    └── AnalyzerErrorForm.java

src/main/resources/liquibase/analyzer/
├── 004-001-create-analyzer-field-table.xml
├── 004-002-create-analyzer-field-mapping-table.xml
├── 004-003-create-qualitative-result-mapping-table.xml
├── 004-004-create-unit-mapping-table.xml
└── 004-005-create-analyzer-error-table.xml

src/test/java/org/openelisglobal/analyzer/
├── valueholder/          # ORM Validation Tests
│   └── HibernateMappingValidationTest.java
├── service/              # Unit Tests
│   ├── AnalyzerFieldServiceTest.java
│   ├── AnalyzerFieldMappingServiceTest.java
│   └── ...
└── controller/           # Integration Tests
    ├── AnalyzerRestControllerIntegrationTest.java
    └── ...

# Frontend (React/Carbon)
frontend/src/
├── components/analyzers/
│   ├── AnalyzersList/
│   │   ├── AnalyzersList.js
│   │   ├── AnalyzersList.test.js
│   │   └── AnalyzersList.scss
│   ├── AnalyzerForm/
│   │   ├── AnalyzerForm.js
│   │   └── AnalyzerForm.test.js
│   ├── FieldMapping/
│   │   ├── FieldMapping.js
│   │   ├── FieldMappingPanel.js
│   │   ├── OpenELISFieldSelector.js
│   │   └── ...
│   ├── ErrorDashboard/
│   │   ├── ErrorDashboard.js
│   │   └── ErrorDetailsModal.js
│   └── TestConnectionModal/
│       └── TestConnectionModal.js
├── pages/
│   ├── AnalyzersPage.js
│   ├── FieldMappingsPage.js
│   └── ErrorDashboardPage.js
└── services/
    └── analyzerService.js

frontend/src/languages/
├── en.json               # English translations
└── fr.json               # French translations

frontend/cypress/e2e/
├── analyzerConfiguration.cy.js    # User Story 1 (P1)
├── analyzerMaintenance.cy.js     # User Story 2 (P2)
└── errorResolution.cy.js         # User Story 3 (P3)
```

## Phase 0: Outline & Research

**Status**: Complete  
**Objective**: Resolve all technical unknowns and research decisions needed for
implementation

### Research Tasks

1. **ASTM Protocol Integration**

   - Research: How to query analyzers via ASTM protocol to retrieve available
     fields
   - Research: ASTM LIS2-A2 segment/field structure and parsing requirements
   - Research: Integration points with existing `ASTMAnalyzerReader` and
     `AnalyzerImportController`

2. **Legacy Analyzer Entity Integration**

   - Research: How to extend or work alongside legacy `Analyzer` entity (XML
     mappings)
   - Research: Migration strategy for analyzer configuration (IP/Port,
     connection settings)
   - Research: Backward compatibility requirements with existing analyzer plugin
     system

3. **Field Mapping Architecture**

   - Research: Best practices for many-to-one mapping patterns (multiple
     analyzer values → single OpenELIS code)
   - Research: Unit conversion patterns and validation rules
   - Research: Type compatibility validation (numeric vs qualitative vs text)

4. **Error Queue and Reprocessing**

   - Research: Message queue patterns for holding failed/unmapped messages
   - Research: Reprocessing workflow and state management
   - Research: Integration with existing ASTM message processing pipeline

5. **Carbon Design System Components**

   - Research: Dual-panel layout patterns using Carbon Grid
   - Research: Visual connection lines between mapped fields (Carbon design
     tokens)
   - Research: OpenELIS Field Selector component patterns (searchable,
     categorized dropdown)
   - Research: Navigation integration patterns (unified tab-navigation using
     sub-nav items)

6. **Navigation Integration**
   - Research: Backend-driven menu system (`/rest/menu` API) integration
     patterns
   - Research: Unified tab-navigation pattern (sub-nav items as tabs, no
     separate tab components)
   - Research: Active tab/page state tracking via route-based highlighting
   - Research: Navigation visibility control (pages requiring nav to be
     visible/expanded)

**Output**: `research.md` with all technical decisions documented

## Phase 1: Design & Contracts

**Status**: Complete  
**Objective**: Generate data model, API contracts, and quickstart guide

### Deliverables

1. **Data Model** (`data-model.md`)

   - Entity definitions: AnalyzerField, AnalyzerFieldMapping,
     QualitativeResultMapping, UnitMapping, AnalyzerError
   - Relationships and foreign keys
   - Validation rules and constraints
   - State transitions (draft → active mappings)

2. **API Contracts** (`contracts/`)

   - REST API endpoint specifications (OpenAPI/Swagger format)
   - Request/response schemas
   - Error response formats
   - Authentication/authorization requirements

3. **Quickstart Guide** (`quickstart.md`)

   - Step-by-step developer setup instructions
   - Database migration steps
   - API testing examples
   - UI component usage examples

4. **Agent Context Update**
   - Run `.specify/scripts/bash/update-agent-context.sh cursor-agent`
   - Add new technology decisions to agent-specific context file

**Output**: `data-model.md`, `contracts/*.json`, `quickstart.md`, updated agent
context
