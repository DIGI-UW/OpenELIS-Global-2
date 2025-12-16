# Implementation Plan: Sample Management Menu

**Branch**: `001-sample-management` | **Date**: 2025-11-20 | **Spec**:
[spec.md](spec.md) **Input**: Feature specification from
`/specs/001-sample-management/spec.md`

## Summary

This feature implements a comprehensive sample management menu that enables
laboratory technicians to search for sample items, add tests (including bulk
operations), and create aliquots with volume tracking. The system supports
unlimited nested aliquoting with parent-child relationship tracking, preventing
over-dispensing through validation of remaining quantities. Key capabilities
include searching by accession number, adding multiple tests to single or
multiple samples, creating aliquots with automatic external ID generation
(SAMPLE001.1, SAMPLE001.1.1, etc.), and maintaining full lineage tracking. The
technical approach follows OpenELIS Global 3.0 Constitution with 5-layer
architecture, Carbon Design System UI, FHIR R4 compliance for external data
integration, and comprehensive TDD workflow.

## Technical Context

**Language/Version**: Java 21 LTS (OpenJDK/Temurin) - MANDATORY **Primary
Dependencies**: Spring Boot 3.x, Hibernate 6.x, React 17, Carbon Design System
v1.15 **Storage**: PostgreSQL 14+ with Liquibase 4.8.0 for schema migrations
**Testing**: JUnit 4 (4.13.1) + Mockito 2.21.0 (backend), Jest + React Testing
Library (frontend), Cypress 12.17.3 (E2E) **Target Platform**: Web application
(Docker + Docker Compose deployment, Ubuntu 20.04+ host) **Project Type**: Web
(frontend + backend) - OpenELIS Global monorepo with separate frontend/ and
backend/ structures **Performance Goals**: Search results in <2 seconds for 100K
samples, test ordering <30 seconds for 5 tests, aliquot creation <1 second
**Constraints**: <200ms p95 for API responses, transactional consistency for
concurrent aliquoting operations, 3 decimal precision for quantities
**Scale/Scope**: Support 100K+ sample items, unlimited aliquot hierarchy depth,
10+ concurrent users performing aliquoting operations

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: No country-specific code branches planned
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (NO
      Bootstrap/Tailwind)
- [x] **FHIR/IHE Compliance**: External data integrates via FHIR R4 + IHE
      profiles for SampleItem and aliquot relationships
- [x] **Layered Architecture**: Backend follows 5-layer pattern
      (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files -
    legacy exempt until refactored)
  - **Transaction management MUST be in service layer only** - NO
    `@Transactional` annotations on controller methods
  - **Services compile all data within transaction** to prevent
    LazyInitializationException when loading hierarchies
- [x] **Test Coverage**: Unit + ORM validation + integration + E2E tests planned
      (>80% backend, >70% frontend coverage goal per Constitution V)
  - E2E tests MUST follow Cypress best practices (Constitution V.5):
    - Run tests individually during development (not full suite)
    - Browser console logging enabled and reviewed after each run
    - Video recording disabled by default
    - Post-run review of console logs and screenshots required
    - Use data-testid selectors (PREFERRED)
    - Use cy.session() for login state (10-20x faster)
    - Use API-based test data setup (10x faster than UI)
    - See
      [Testing Roadmap](.specify/guides/testing-roadmap.md#cypress-e2e-testing)
      for comprehensive Cypress guidance
- [x] **Schema Management**: Database changes via Liquibase changesets only (add
      columns to sample_item table, create sample_item_aliquot_relationship
      table)
- [x] **Internationalization**: All UI strings use React Intl (no hardcoded
      text)
- [x] **Security & Compliance**: RBAC for sample management operations, audit
      trail (sys_user_id + lastupdated), input validation for quantities/IDs,
      optimistic concurrency control for aliquoting

**Complexity Justification Required If**: N/A - All requirements align with
constitution

## Testing Strategy

**Reference**: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)

**MANDATORY**: Every plan MUST include a complete testing strategy that
references the Testing Roadmap and documents test coverage goals, test types,
data management, and checkpoint validations.

### Coverage Goals

- **Backend**: >80% code coverage (measured via JaCoCo)
- **Frontend**: >70% code coverage (measured via Jest)
- **Critical Paths**: 100% coverage (aliquot creation with volume validation,
  test ordering, search functionality)

### Test Types

Document which test types will be used for this feature:

- [x] **Unit Tests**: Service layer business logic (JUnit 4 + Mockito)
  - Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`
  - **Reference**:
    [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Coverage Goal**: >80% (measured via JaCoCo)
  - **SDD Checkpoint**: After Phase 2 (Services), all unit tests MUST pass
  - **TDD Workflow**: Red-Green-Refactor cycle for complex logic (aliquot
    numbering, volume calculations, hierarchy traversal)
  - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` for isolated unit
    tests (NOT `@SpringBootTest`)
  - **Mocking**: Use `@Mock` (NOT `@MockBean`) for isolated unit tests
  - **Focus Areas**:
    - SampleManagementService: aliquot creation, volume validation, nested
      hierarchy logic
    - SampleItemService: external ID generation with suffix (SAMPLE001.1.1.1)
    - TestOrderService: duplicate test detection, bulk test addition
- [x] **DAO Tests**: Persistence layer testing (@DataJpaTest)
  - Template: `.specify/templates/testing/DataJpaTestDao.java.template`
  - **Reference**:
    [Testing Roadmap - @DataJpaTest (DAO/Repository Layer)](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Test Slicing**: Use `@DataJpaTest` for DAO testing (NOT
    `@SpringBootTest` - faster execution)
  - **Test Data**: Use `TestEntityManager` (NOT JdbcTemplate) for test data
    setup
  - **Transaction Management**: Automatic rollback (no manual cleanup needed)
  - **Focus Areas**:
    - SampleItemDAO: query by accession number, parent-child relationship
      queries, recursive lineage queries
    - SampleItemAliquotRelationshipDAO: parent-child mapping CRUD, sequence
      number tracking
    - TestOrderDAO: duplicate detection queries, bulk insert operations
- [x] **Controller Tests**: REST API endpoints (@WebMvcTest)
  - Template: `.specify/templates/testing/WebMvcTestController.java.template`
  - **Reference**:
    [Testing Roadmap - @WebMvcTest (Controller Layer)](.specify/guides/testing-roadmap.md#webmvctest-controller-layer)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Test Slicing**: Use `@WebMvcTest` for controller testing (NOT
    `@SpringBootTest` - faster execution)
  - **Mocking**: Use `@MockBean` (NOT `@Mock`) for Spring context mocking
  - **HTTP Testing**: Use `MockMvc` for HTTP request/response testing
  - **Focus Areas**:
    - SampleManagementRestController: search, add tests, create aliquot
      endpoints
    - Request validation, error handling, JSON serialization
- [x] **ORM Validation Tests**: Entity mapping validation (Constitution V.4)
  - **Reference**:
    [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4)
    for detailed patterns
  - **SDD Checkpoint**: After Phase 1 (Entities), ORM validation tests MUST pass
  - **Requirements**: MUST execute in <5 seconds, MUST NOT require database
    connection
  - **Focus Areas**:
    - SampleItem entity: verify annotations for originalQuantity,
      remainingQuantity, parentSampleItem
    - SampleItemAliquotRelationship entity: verify JPA annotations, relationship
      mappings
    - TestOrder entity: verify annotations and relationships to SampleItem
- [x] **Integration Tests**: Full workflow testing (@SpringBootTest)
  - **Reference**:
    [Testing Roadmap - @SpringBootTest (Full Integration)](.specify/guides/testing-roadmap.md#springboottest-full-integration)
    for detailed patterns
  - **Reference**:
    [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md)
    for quick reference
  - **Test Slicing**: Use `@SpringBootTest` only when full application context
    is required
  - **Transaction Management**: Use `@Transactional` for automatic rollback
    (preferred)
  - **SDD Checkpoint**: After Phase 3 (Controllers), integration tests MUST pass
  - **Focus Areas**:
    - Complete aliquot creation workflow (search → create aliquot → verify
      parent/child state)
    - Bulk test ordering workflow
    - Concurrent aliquoting scenario with optimistic locking
- [x] **Frontend Unit Tests**: React component logic (Jest + React Testing
      Library)
  - Template: `.specify/templates/testing/JestComponent.test.jsx.template`
  - **Reference**:
    [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests)
    for detailed patterns
  - **Reference**: [Jest Best Practices](.specify/guides/jest-best-practices.md)
    for quick reference
  - **Coverage Goal**: >70% (measured via Jest)
  - **SDD Checkpoint**: After Phase 4 (Frontend), all unit tests MUST pass
  - **TDD Workflow**: Red-Green-Refactor cycle for complex logic
  - **Focus Areas**:
    - SampleSearch component: search input, results display
    - AliquotForm component: quantity validation, external ID display
    - TestSelector component: multi-select logic, duplicate detection
- [x] **E2E Tests**: Critical user workflows (Cypress)
  - Template: `.specify/templates/testing/CypressE2E.cy.js.template`
  - **Reference**:
    [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
    for functional requirements
  - **Reference**:
    [Testing Roadmap - Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing)
    for detailed patterns
  - **Reference**:
    [Cypress Best Practices](.specify/guides/cypress-best-practices.md) for
    quick reference
  - **Individual Test Execution**: Run tests individually during development
    (NOT full suite)
  - **Focus Areas**:
    - User Story P1: Search sample by accession number
    - User Story P1: Add multiple tests to sample
    - User Story P2: Create aliquot with volume tracking
    - User Story P3: Bulk test addition to aliquots

### Test Data Management

Document how test data will be created and cleaned up:

- **Backend**:

  - **Unit Tests (JUnit 4 + Mockito)**:
    - [x] Use builders/factories for test data (SampleItemBuilder,
          TestOrderBuilder)
    - [x] Use mock data builders/factories for reusable test data
    - [x] Test edge cases (null quantities, zero remaining quantity, invalid
          parent references)
  - **DAO Tests (@DataJpaTest)**:
    - [x] Use `TestEntityManager` for test data setup (NOT JdbcTemplate)
    - [x] Use builders/factories for test entities (create parent sample, create
          child aliquots)
    - [x] Automatic transaction rollback (no manual cleanup needed)
  - **Controller Tests (@WebMvcTest)**:
    - [x] Use builders/factories for test data
    - [x] Mock service layer (use `@MockBean`)
  - **Integration Tests (@SpringBootTest)**:
    - [x] Use builders/factories for test data
    - [x] Use `@Transactional` for automatic rollback (preferred)
    - [x] Create hierarchical test data (parent → child → grandchild aliquots)
    - [x] Use `@Sql` scripts for complex data setup (if needed for large
          hierarchies)

- **Frontend**:
  - **E2E Tests (Cypress)**:
    - [x] Use API-based setup via `cy.request()` (NOT slow UI interactions) -
          10x faster
    - [x] Use fixtures with `cy.intercept()` for consistent test data
    - [x] Use `cy.session()` for login state (10-20x faster than per-test login)
    - [x] Create parent samples, aliquots, and test orders via API before test
          execution
    - [x] Use custom Cypress commands for reusable setup/cleanup
          (`cy.createSample()`, `cy.createAliquot()`)
  - **Unit Tests (Jest)**:
    - [x] Use mock data builders/factories (per Medium article - use generic
          cases)
    - [x] Use `setupApiMocks()` helper for consistent API mocking
    - [x] Test edge cases (empty search results, validation errors)
    - [x] Use `renderWithIntl()` helper for consistent component rendering

### Checkpoint Validations

Document which tests must pass at each SDD phase checkpoint:

- [x] **After Phase 1 (Entities)**: ORM validation tests must pass (SampleItem,
      SampleItemAliquotRelationship, TestOrder entities)
- [x] **After Phase 2 (Services)**: Backend unit tests must pass
      (SampleManagementService, SampleItemService, TestOrderService)
- [x] **After Phase 3 (Controllers)**: Integration tests must pass (full aliquot
      creation workflow, bulk test ordering)
- [x] **After Phase 4 (Frontend)**: Frontend unit tests (Jest) AND E2E tests
      (Cypress) must pass (all 4 user stories)

### TDD Workflow

- [x] **TDD Mandatory**: Red-Green-Refactor cycle for complex logic (aliquot
      numbering, hierarchy traversal, volume calculations)
- [x] **Test Tasks First**: Test tasks MUST appear before implementation tasks
      in tasks.md
- [x] **Checkpoint Enforcement**: Tests must pass before proceeding to next
      phase

## Project Structure

### Documentation (this feature)

```text
specs/001-sample-management/
├── spec.md              # Feature specification (/speckit.specify output)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   ├── search-samples.openapi.yml
│   ├── add-tests.openapi.yml
│   └── create-aliquot.openapi.yml
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

**Structure Decision**: Web application (frontend + backend monorepo)

```text
backend/
└── src/
    ├── main/
    │   ├── java/org/openelisglobal/
    │   │   ├── samplemanagement/
    │   │   │   ├── valueholder/
    │   │   │   │   └── SampleItemAliquotRelationship.java
    │   │   │   ├── dao/
    │   │   │   │   ├── SampleItemAliquotRelationshipDAO.java
    │   │   │   │   └── SampleItemAliquotRelationshipDAOImpl.java
    │   │   │   ├── service/
    │   │   │   │   ├── SampleManagementService.java
    │   │   │   │   └── SampleManagementServiceImpl.java
    │   │   │   ├── controller/
    │   │   │   │   └── SampleManagementRestController.java
    │   │   │   └── form/
    │   │   │       ├── SampleSearchForm.java
    │   │   │       ├── AddTestsForm.java
    │   │   │       └── CreateAliquotForm.java
    │   │   ├── sampleitem/
    │   │   │   └── valueholder/
    │   │   │       └── SampleItem.java (MODIFY: add originalQuantity, remainingQuantity, parentSampleItem)
    │   │   └── test/
    │   │       └── valueholder/
    │   │           └── TestOrder.java (NEW or MODIFY existing Analysis entity)
    │   └── resources/
    │       └── liquibase/
    │           └── samplemanagement/
    │               └── 001-sample-management-schema.xml
    └── test/
        └── java/org/openelisglobal/
            ├── samplemanagement/
            │   ├── service/
            │   │   └── SampleManagementServiceTest.java
            │   ├── dao/
            │   │   └── SampleItemAliquotRelationshipDAOTest.java
            │   └── controller/
            │       └── SampleManagementRestControllerTest.java
            └── orm/
                └── SampleManagementOrmValidationTest.java

frontend/
└── src/
    ├── components/
    │   └── sampleManagement/
    │       ├── SampleSearch.js
    │       ├── SampleSearchResults.js
    │       ├── TestSelector.js
    │       ├── AliquotForm.js
    │       └── SampleHierarchyTree.js
    ├── languages/
    │   ├── en.json (ADD sample management keys)
    │   └── fr.json (ADD sample management keys)
    └── tests/
        └── components/
            └── sampleManagement/
                ├── SampleSearch.test.js
                ├── TestSelector.test.js
                └── AliquotForm.test.js

frontend/cypress/
└── e2e/
    └── sampleManagement.cy.js
```

**Note**: Existing entities will be modified (SampleItem), new entities created
(SampleItemAliquotRelationship, TestOrder if not exists), following 5-layer
architecture pattern per constitution.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations - all requirements align with constitution.

---

## Phase 0: Research & Discovery

**Purpose**: Resolve all NEEDS CLARIFICATION items from Technical Context above
by researching existing codebase patterns, best practices, and technical
approaches.

### Research Tasks

1. **Sample Item Entity Analysis**

   - Research current SampleItem entity structure in
     `org.openelisglobal.sampleitem.valueholder.SampleItem`
   - Identify existing quantity field (if any) and how it's currently used
   - Determine if parent-child relationship already exists or needs to be added
   - Document existing FHIR UUID usage pattern for external integration

2. **Test Ordering System Analysis**

   - Research how tests are currently ordered for samples (Analysis entity, Test
     entity)
   - Identify if TestOrder entity exists or if Analysis entity serves this
     purpose
   - Determine duplicate test detection logic (if exists)
   - Document bulk test ordering patterns (if any exist in codebase)

3. **Accession Number Search Pattern**

   - Research existing accession number search implementation
   - Identify service/DAO methods used for sample search
   - Document search UI patterns in existing React components
   - Determine if autocomplete exists for partial matches

4. **Nested Aliquoting Technical Approach**

   - Research best practices for recursive parent-child relationships in
     JPA/Hibernate
   - Identify efficient query patterns for hierarchical data (JOIN FETCH,
     recursive CTEs)
   - Document external ID generation patterns in existing codebase (sequence
     management)
   - Research optimistic locking patterns for concurrent aliquot operations

5. **FHIR R4 Specimen Mapping**

   - Research FHIR R4 Specimen resource structure for aliquot relationships
   - Identify IHE Lab profile requirements for specimen hierarchies
   - Document existing FhirTransformService patterns for entity-to-FHIR
     conversion
   - Determine parent-child specimen representation in FHIR

6. **Carbon Design System Components**

   - Identify Carbon components for search interface (Search, DataTable)
   - Research multi-select patterns (MultiSelect, Dropdown)
   - Document modal patterns for aliquot creation form
   - Identify tree/hierarchy display components for lineage visualization

7. **Concurrent Access Patterns**
   - Research Hibernate optimistic locking implementation (@Version annotation)
   - Document transaction isolation levels in OpenELIS configuration
   - Identify concurrency test patterns in existing test suite
   - Determine retry logic patterns for optimistic locking failures

### Research Output Location

`research.md` - Consolidated findings document with:

- Decision: [what was chosen]
- Rationale: [why chosen]
- Alternatives considered: [what else evaluated]
- Code references: [link to existing patterns in codebase]

---

## Phase 1: Design & Data Model

**Prerequisites**: research.md complete

### Data Model Design

**Output**: `data-model.md`

#### Entity Modifications

1. **SampleItem** (MODIFY existing entity in
   `org.openelisglobal.sampleitem.valueholder.SampleItem`)

   - Add fields:
     - `Double originalQuantity` - Initial volume when sample created
     - `Double remainingQuantity` - Current available volume for aliquoting
     - `@ManyToOne SampleItem parentSampleItem` - Parent reference for aliquots
       (nullable)
     - `@OneToMany List<SampleItem> childAliquots` - Children references (lazy
       loaded)
   - Validation:
     - remainingQuantity <= originalQuantity
     - originalQuantity > 0
     - Decimal precision: 3 places (e.g., 0.333)

2. **SampleItemAliquotRelationship** (NEW entity in
   `org.openelisglobal.samplemanagement.valueholder.SampleItemAliquotRelationship`)

   - Fields:
     - `String id` (PK, generated)
     - `@ManyToOne SampleItem parentSampleItem` (FK to sample_item)
     - `@ManyToOne SampleItem childSampleItem` (FK to sample_item)
     - `Integer sequenceNumber` - Aliquot number (1, 2, 3...)
     - `Double quantityTransferred` - Amount taken from parent
     - `UUID fhirUuid` - FHIR R4 mapping
     - `String sysUserId` - Audit trail (who created)
     - `Timestamp lastupdated` - Audit trail (when created)
   - Constraints:
     - Unique constraint on (parentSampleItem, sequenceNumber)
     - Check: quantityTransferred > 0

3. **TestOrder** (NEW or use existing Analysis entity)
   - Fields:
     - `String id` (PK)
     - `@ManyToOne SampleItem sampleItem` (FK)
     - `@ManyToOne Test test` (FK to test catalog)
     - `String status` (ORDERED, IN_PROGRESS, COMPLETED)
     - `UUID fhirUuid` - FHIR ServiceRequest mapping
     - `String sysUserId`, `Timestamp lastupdated` - Audit trail
   - Constraints:
     - Unique constraint on (sampleItem, test) - prevent duplicates

#### Liquibase Changesets

**Output**:
`src/main/resources/liquibase/samplemanagement/001-sample-management-schema.xml`

```xml
<changeSet id="sample-mgmt-001-add-quantity-columns" author="dev-team">
  <addColumn tableName="sample_item">
    <column name="original_quantity" type="DECIMAL(10,3)">
      <constraints nullable="true"/> <!-- Nullable for existing rows -->
    </column>
    <column name="remaining_quantity" type="DECIMAL(10,3)">
      <constraints nullable="true"/>
    </column>
    <column name="parent_sample_item_id" type="VARCHAR(36)">
      <constraints nullable="true" foreignKeyName="fk_sample_item_parent"
                   references="sample_item(id)" deleteCascade="false"/>
    </column>
  </addColumn>
</changeSet>

<changeSet id="sample-mgmt-002-create-aliquot-relationship-table" author="dev-team">
  <createTable tableName="sample_item_aliquot_relationship">
    <column name="id" type="VARCHAR(36)"><constraints primaryKey="true"/></column>
    <column name="parent_sample_item_id" type="VARCHAR(36)"><constraints nullable="false" foreignKeyName="fk_aliquot_parent" references="sample_item(id)"/></column>
    <column name="child_sample_item_id" type="VARCHAR(36)"><constraints nullable="false" foreignKeyName="fk_aliquot_child" references="sample_item(id)"/></column>
    <column name="sequence_number" type="INT"><constraints nullable="false"/></column>
    <column name="quantity_transferred" type="DECIMAL(10,3)"><constraints nullable="false"/></column>
    <column name="fhir_uuid" type="UUID"><constraints nullable="false" unique="true"/></column>
    <column name="sys_user_id" type="INT"><constraints nullable="false"/></column>
    <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
  </createTable>
  <addUniqueConstraint tableName="sample_item_aliquot_relationship"
                       columnNames="parent_sample_item_id,sequence_number"
                       constraintName="uk_parent_sequence"/>
</changeSet>
```

### API Contract Design

**Output**: `contracts/` directory with OpenAPI 3.0 specifications

#### Endpoint: Search Sample Items

**File**: `contracts/search-samples.openapi.yml`

```yaml
GET /rest/sample-management/search?accessionNumber={accessionNumber}

Response 200:
{
  "samples": [
    {
      "id": "uuid",
      "externalId": "SAMPLE001",
      "sampleType": "Blood",
      "collectionDate": "2025-11-20T10:00:00Z",
      "originalQuantity": 10.0,
      "remainingQuantity": 7.0,
      "status": "ACTIVE",
      "parentSampleId": null,
      "childAliquots": [
        {"id": "uuid2", "externalId": "SAMPLE001.1", "remainingQuantity": 3.0}
      ],
      "associatedTests": [
        {"testId": "uuid-test", "testName": "CBC", "status": "ORDERED"}
      ]
    }
  ]
}
```

#### Endpoint: Add Tests to Samples

**File**: `contracts/add-tests.openapi.yml`

```yaml
POST /rest/sample-management/add-tests

Request:
{
  "sampleItemIds": ["uuid1", "uuid2"],
  "testIds": ["test-uuid-1", "test-uuid-2"]
}

Response 200:
{
  "success": true,
  "testsAdded": 4,
  "skipped": [
    {"sampleItemId": "uuid1", "testId": "test-uuid-1", "reason": "Duplicate test"}
  ]
}
```

#### Endpoint: Create Aliquot

**File**: `contracts/create-aliquot.openapi.yml`

```yaml
POST /rest/sample-management/create-aliquot

Request:
{
  "parentSampleItemId": "uuid-parent",
  "quantityToTransfer": 3.5
}

Response 200:
{
  "success": true,
  "childSampleItem": {
    "id": "uuid-child",
    "externalId": "SAMPLE001.1",
    "originalQuantity": 3.5,
    "remainingQuantity": 3.5,
    "parentSampleId": "uuid-parent"
  },
  "updatedParentRemainingQuantity": 6.5
}

Response 400 (validation error):
{
  "error": "INSUFFICIENT_QUANTITY",
  "message": "Cannot aliquot: requested volume (5.0mL) exceeds remaining volume (2.0mL)"
}
```

### FHIR R4 Mapping Design

**Entities to FHIR Resources**:

1. **SampleItem → Specimen**

   - Specimen.identifier = external ID (SAMPLE001.1)
   - Specimen.parent = parent Specimen reference
   - Specimen.container.specimenQuantity = originalQuantity
   - Custom extension for remainingQuantity

2. **SampleItemAliquotRelationship → Specimen relationship**

   - Child Specimen.parent references parent Specimen
   - Provenance resource tracks aliquot creation event

3. **TestOrder → ServiceRequest**
   - ServiceRequest.specimen references Specimen
   - ServiceRequest.code = test definition

### Quickstart Guide

**Output**: `quickstart.md` - Developer onboarding document with:

- Local setup instructions
- Database schema overview
- API endpoint examples with curl commands
- Example aliquot creation workflow
- Test execution commands

---

## Phase 2: Implementation Tasks

**Prerequisites**: Phase 1 complete (data-model.md, contracts/)

**Note**: This phase generates the task breakdown using `/speckit.tasks`
command. Tasks will follow TDD workflow with test-first approach and will be
dependency-ordered per SDD methodology.

**Expected Task Categories**:

1. **Phase 1 Tasks**: Entity creation and ORM validation
2. **Phase 2 Tasks**: DAO and Service layer with unit tests
3. **Phase 3 Tasks**: Controllers and integration tests
4. **Phase 4 Tasks**: Frontend components and E2E tests

**Task File Location**: `tasks.md` (generated by `/speckit.tasks`, NOT by
`/speckit.plan`)

---

## Next Steps

1. ✅ **Completed**: Implementation plan created (this file)
2. ⏭️ **Next**: Run `/speckit.plan` research phase to populate `research.md`
   - After research complete, data model and contracts will be generated
     automatically
   - Once Phase 1 artifacts exist, run `/speckit.tasks` to generate
     dependency-ordered task breakdown
3. 🔄 **Then**: Execute `/speckit.implement` to begin TDD implementation
   workflow
