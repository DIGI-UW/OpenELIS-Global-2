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
hierarchy: a single "Analyzers" parent node that expands to Analyzers Dashboard,
Error Dashboard, plus Quality Control placeholders (main QC dashboard, "QC
Alerts & Violations", "Corrective Actions") that link into feature
`003-westgard-qc`. Field Mappings page is accessed via analyzer row actions in
the Analyzers Dashboard and does not appear in the navigation menu.

**Technical Approach**: Extend existing OpenELIS analyzer infrastructure (legacy
`Analyzer` entity, `ASTMAnalyzerReader`, `AnalyzerImportController`) with new
annotation-based JPA entities for analyzer field mapping. Create new REST API
endpoints following 5-layer architecture pattern. Build Carbon Design System UI
components for analyzer management, field mapping interface, and error
dashboard. Integrate with existing ASTM message processing to apply mappings
during message interpretation. Support query analyzer functionality to retrieve
available fields from analyzers via HTTP requests to ASTM-HTTP Bridge (bridge
handles ASTM protocol translation). Integrate navigation with existing left-hand
navigation bar using unified tab-navigation pattern (sub-nav items function as
tabs, backend-driven via `/rest/menu` API, no separate Carbon Tabs components)
while surfacing the future QC routes noted above.

**QC Result Processing Integration**: Feature 004 MUST handle QC result
processing as part of ASTM message ingestion. When ASTM messages contain
Q-segments (Quality Control result segments), the system MUST: (1) Parse
Q-segments using extended ASTMAnalyzerReader, (2) Extract QC data (instrument ID
from message header, test code, control lot/level, result value, timestamp), (3)
Apply configured QC field mappings (per FR-019) to map ASTM codes to OpenELIS
entities, (4) Persist QC results via direct service call to QCResultService
(003's service layer). Integration Pattern: Direct service call from 004's
message processing service to 003's QCResultService.createQCResult() method.
This ensures immediate consistency and follows the 5-layer architecture pattern
(004's service calls 003's service). Error Handling: Unmapped QC messages are
queued in 004's Error Dashboard (per FR-011). When mappings are resolved, queued
QC messages are reprocessed automatically.

## Milestone Plan

**Constitution Principle IX Compliance**: This feature exceeds 3 days effort and
MUST be broken into Validation Milestones. Each milestone corresponds to a Pull
Request targeting the specs branch (`spec/OGC-49-astm-analyzer-mapping`).

| Milestone | Description             | User Stories  | Task Ranges                                             | Estimated Days | Dependencies | PR    |
| --------- | ----------------------- | ------------- | ------------------------------------------------------- | -------------- | ------------ | ----- |
| M1        | Backend core + database | US1, US2, US3 | T001-T011, T012-T047, T089-T103, T151a-T153c, T182-T196 | ~15            | None         | #2429 |
| M2        | Frontend analyzers      | US1, US2, US3 | T010, T048-T088, T140, T174-T181                        | ~12            | M1           | #2430 |
| M3        | Query bridge work       | FR-002        | T104-T105, T106-T107, T108-T109                         | ~5             | M1           | #2431 |

**Milestone Details**:

- **M1 (Backend core + database)**: Database schema (Liquibase changesets), JPA
  entities, DAOs, services, REST controllers, QC result processing integration,
  unified status field migration. Includes all backend infrastructure required
  for analyzer field mapping, error handling, and message processing.

- **M2 (Frontend analyzers)**: React components (AnalyzersList, AnalyzerForm,
  FieldMapping, ErrorDashboard), navigation integration, custom field type
  management UI. Includes all user-facing interfaces for configuring and
  managing analyzer mappings.

- **M3 (Query bridge work)**: AnalyzerQueryService implementation, HTTP
  communication with ASTM-HTTP Bridge, query job management, field parsing from
  ASTM responses. Enables FR-002 (Query Analyzer functionality) to retrieve
  available fields from analyzers via bi-directional bridge communication.

**Branch Naming**: `feat/OGC-49-astm-analyzer-mapping/m{N}-{desc}` per
Constitution Principle IX.

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**:

- Backend: Spring Boot 3.x, Hibernate 6.x, HAPI FHIR R4 (v6.6.2), JPA
  (jakarta.persistence), Liquibase 4.8.0, RestTemplate/WebClient (HTTP client
  for ASTM-HTTP Bridge communication)
- Frontend: @carbon/react v1.15.0, React Intl 5.20.12, Formik 2.2.9, SWR 2.0.3,
  React Router DOM 5.2.0
- ASTM Protocol: ASTM-HTTP Bridge (bi-directional protocol translator), Existing
  `ASTMAnalyzerReader` and `AnalyzerImportController` infrastructure

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
- UI Pages: Analyzers Dashboard, Field Mappings (dual-panel, accessed via
  analyzer actions), Error Dashboard
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
  - **Navigation**: Sub-navigation items function as tabs - SideNavMenuItem
    components MUST be used exclusively. Carbon Tabs/TabList/TabPanels
    components MUST NOT be used on analyzer pages (explicit anti-pattern per
    FR-020 unified tab-navigation pattern). This ensures consistent navigation
    behavior and prevents duplicate tab affordances.
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

  - **Valueholders use XML mappings for analyzer entities** (matching legacy
    `Analyzer` entity pattern) - AnalyzerField, AnalyzerFieldMapping,
    QualitativeResultMapping, UnitMapping use XML-only mappings (`*.hbm.xml`) to
    avoid Hibernate relationship resolution issues when XML-mapped entities
    reference annotation-based entities. This is an exception to the general
    annotation-based approach, documented in `ANALYZER_XML_MAPPING_ANALYSIS.md`.
    Legacy entities are exempt until refactored per Constitution IV.
    - **NEW ENTITIES**: All new entities (AnalyzerField, AnalyzerFieldMapping,
      etc.) MUST use annotation-based mappings exclusively
    - **LEGACY EXCEPTION**: Legacy `Analyzer` entity uses XML mappings
      (`src/main/resources/hibernate/hbm/Analyzer.hbm.xml`) and is exempt from
      this rule until refactored per Constitution IV
    - **IMPLICATIONS**: Queries that traverse relationships through XML-mapped
      entities may require special patterns (see research.md "Querying Through
      XML-Mapped Entities")
    - **MIGRATION PATH**: Future refactoring of `Analyzer` entity to annotations
      will eliminate this exception
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

## Technical Approach Details

### Unified Status Field Management

**Implementation Pattern**: Event-driven automatic status transitions with
manual override capability

**Unified Status Field**:

The system uses a single `status` field (replacing separate `active` boolean and
`lifecycle_stage` enum) that combines lifecycle stage and operational status.
This simplifies the data model and UI while capturing all necessary states.

**Status Values**:

- `INACTIVE`: Manually set by user (overrides all other criteria)
- `SETUP`: Analyzer added but no mappings configured yet
- `VALIDATION`: Mappings being created/tested, not all required mappings
  activated
- `ACTIVE`: All required mappings configured and activated, auto-processing
  results
- `ERROR_PENDING`: Active analyzer with unacknowledged errors in error queue
- `OFFLINE`: Connection test failed, analyzer unreachable

**Automatic Status Transitions**:

Status transitions occur automatically based on analyzer state:

- **SETUP → VALIDATION**: Triggered when first mapping is created (event
  listener on AnalyzerFieldMapping creation)
- **VALIDATION → ACTIVE**: Triggered when all required mappings are activated
  (event listener on mapping activation, validates required mappings present)
- **ACTIVE → ERROR_PENDING**: Triggered when unacknowledged errors are detected
  (event listener on AnalyzerError creation with status UNACKNOWLEDGED)
- **ACTIVE → OFFLINE**: Triggered when connection test fails (event listener on
  connection test failure)
- **ERROR_PENDING → ACTIVE**: Triggered when all errors are acknowledged (event
  listener on error acknowledgment, checks if all errors acknowledged)
- **OFFLINE → ACTIVE**: Triggered when connection test succeeds (event listener
  on successful connection test)

**Manual Override**:

- Users can manually set status to `INACTIVE` at any time via:
  - Analyzer edit form (status dropdown)
  - Status dropdown in analyzer table row (inline edit)
- Manual override to INACTIVE is always available regardless of current status
- Other manual status changes are restricted (only automatic transitions allowed
  except INACTIVE override)
- All status changes (automatic and manual) MUST be logged in audit trail

**Implementation Notes**:

- Status transitions are implemented using Spring event listeners
  (@EventListener) to decouple transition logic from business operations
- Transition validation: Each transition checks prerequisites (e.g., VALIDATION
  → ACTIVE requires all required mappings activated)
- Connection monitoring: Periodic connection tests (configurable interval,
  default 5 minutes) automatically update status to OFFLINE if connection fails
- Error monitoring: Error dashboard queries check for unacknowledged errors and
  trigger ERROR_PENDING status if found
- Status change events are published to audit service for logging

**Database Columns**:

- `status` VARCHAR(20) NOT NULL DEFAULT 'SETUP' (replaces `lifecycle_stage` and
  `active` boolean)
- `last_activated_date` TIMESTAMP NULL (populated when status transitions to
  ACTIVE)

### Test Mapping Preview Architecture

**Implementation Pattern**: Stateless synchronous preview service

**Service Architecture**:

- **Class**:
  `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingPreviewService`
- **Pattern**: @Service annotation, NO @Transactional (read-only operations)
- **ASTM Parser Integration**: Reuse existing `ASTMAnalyzerReader` for message
  parsing
- **Mapping Application**: Apply current field mappings to parsed data without
  persistence
- **Entity Preview**: Construct Test/Result/Sample entities in memory only

**Service Methods**:

1. `previewMapping(String analyzerId, String astmMessage, PreviewOptions options)` -
   Main preview method
2. `parseAstmMessage(String message)` - Parse ASTM message into field/value
   pairs
3. `applyMappings(List<ParsedField> fields, List<AnalyzerFieldMapping> mappings)` -
   Apply mappings to parsed data
4. `buildEntityPreview(Map<String, Object> mappedData)` - Construct OpenELIS
   entities (Test, Result, Sample)
5. `validateMappings(Map<String, Object> mappedData)` - Identify missing
   mappings, type mismatches, validation errors

**Response Format**:

```json
{
  "parsedFields": [
    {
      "fieldName": "GLU",
      "astmRef": "R|1|^^^GLU",
      "rawValue": "105",
      "dataType": "NUMERIC"
    }
  ],
  "appliedMappings": [
    {
      "mappingId": "MAPPING-001",
      "analyzerField": "GLU",
      "openelisField": "Glucose",
      "confidence": "HIGH"
    }
  ],
  "entityPreview": {
    "test": { "testCode": "GLU", "testName": "Glucose" },
    "result": { "value": "105", "unit": "mg/dL" }
  },
  "warnings": [
    {
      "type": "UNIT_MISMATCH",
      "message": "Analyzer unit 'mg/dL' does not match OpenELIS unit 'mmol/L' - conversion applied"
    }
  ],
  "errors": [
    {
      "type": "UNMAPPED_FIELD",
      "message": "Field 'HbA1c' has no mapping configured"
    }
  ]
}
```

**Performance**:

- Target: <2 seconds response time (synchronous operation)
- Max message size: 10KB (validated before processing)
- Caching: No caching (always use current mappings for accuracy)

**Security**:

- No database persistence (preview only)
- User must have analyzer view permissions
- ASTM message content not logged (may contain PHI)

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
