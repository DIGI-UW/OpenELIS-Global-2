# Implementation Plan: Westgard Rules Quality Control Compliance

**Branch**: `003-westgard-qc` | **Date**: 2025-11-19 | **Spec**:
[spec.md](spec.md) **Input**: Feature specification from
`/specs/003-westgard-qc/spec.md`

## Summary

Implement a comprehensive Westgard QC system for OpenELIS Global that
automatically evaluates quality control results against 8 standard Westgard
rules (1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ, 7ₜ), provides real-time compliance
visualization via Levey-Jennings charts, delivers automated alerts for rule
violations, and manages corrective action workflows to meet SLIPTA and ISO 15189
requirements.

**Technical Approach**: Greenfield implementation with service-oriented
architecture using Strategy pattern for statistics calculation, Chain of
Responsibility for rule evaluation. Feature 003 exposes QCResultService.createQCResult() method that feature 004 calls after parsing ASTM Q-segments (per 004:FR-021 and 004:plan.md QC Result Processing Integration section). Carbon Design System for
dashboard/charting UI, and comprehensive TDD workflow with >80% backend coverage
and >70% frontend coverage.

## Technical Context

**Language/Version**:

- Backend: Java 21 LTS (OpenJDK/Temurin)
- Frontend: JavaScript ES6+ (React 17)

**Primary Dependencies**:

- Backend: Spring Boot 3.x, Hibernate 6.x, PostgreSQL 14+, Jakarta EE 9, HAPI
  FHIR R4
- Frontend: @carbon/react v1.15.0, @carbon/charts-react v1.5.2, React Intl
  5.20.12, SWR 2.0.3, Formik 2.2.9

**Storage**: PostgreSQL 14+ (production database)

**Testing**:

- Backend: JUnit 4 (4.13.1), Mockito 2.21.0, Spring Test
- Frontend: Jest, React Testing Library, Cypress 12.17.3

**Target Platform**: Web application (Linux server + modern browsers)

**Project Type**: Web application (Backend + Frontend)

**Performance Goals**:

- Rule evaluation: <5 seconds per result entry (FR-028)
- Dashboard auto-refresh: 5 minutes (configurable, FR-050)
- Chart rendering: <3 seconds for up to 100 data points (SC-007)
- Support 50+ analyzers without performance degradation (SC-008)
- Process minimum 1,000 QC results per day (SC-014)

**Constraints**:

- <200ms API response time (p95) for dashboard data retrieval
- <100MB memory per analyzer for active QC result caching
- 2-year data retention requirement (SC-015)
- Real-time alert delivery <30 seconds of violation detection (SC-003)
- 99.9% uptime for QC result capture and rule evaluation (SC-006)

**Scale/Scope**:

- Support 50+ analyzers with multiple test configurations
- 8 Westgard rules with configurable enable/disable per test-analyzer
- 3 statistical calculation methods (initial runs, rolling, manufacturer fixed)
- Multi-level QC (Low/Normal/High control levels)
- Multi-language support (en, fr minimum per Constitution VII)
- Role-based access (Results, Biologist, Global Admin per FR-078-081)

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: Rule configurations stored in database
      (`westgard_rule_config` table with test_unit_id, analyzer_id, rule_code,
      enabled flag). No code branching for country-specific QC strategies
      (Constitution I).
- [x] **Carbon Design System**: UI uses @carbon/react v1.15.0 exclusively.
      Components: Tile (compliance cards), DataTable (violation lists),
      LineChart from @carbon/charts-react (Levey-Jennings), ComposedModal
      (modals), Tag (status indicators), OverflowMenu (row actions). NO
      Bootstrap/Tailwind (Constitution II).
- [x] **FHIR/IHE Compliance**: QC results are internal quality control and do
      not require external FHIR exposure per Constitution III (not
      patient-facing data). If future integration with external QC systems is
      needed, FHIR Observation resources with QC profiles will be used.
- [x] **Layered Architecture**: Backend follows strict 5-layer pattern
      (Constitution IV):
  - **Valueholders**: `QCControlLot`, `QCResult`, `QCStatistics`,
    `WestgardRuleConfig`, `QCRuleViolation`, `QCCorrectiveAction`, `QCAlert`
    with JPA annotations (NO XML mappings per Constitution v1.3.0)
  - **DAOs**: `QCControlLotDAO`, `QCResultDAO`, `QCStatisticsDAO`,
    `WestgardRuleConfigDAO`, `QCRuleViolationDAO`, `QCCorrectiveActionDAO`,
    `QCAlertDAO`
  - **Services**: `QCControlLotService`, `QCResultService`,
    `QCStatisticsService`, `WestgardRuleEvaluationService`,
    `QCRuleViolationService`, `QCCorrectiveActionService`, `QCAlertService` with
    `@Transactional` (NO @Transactional in controllers per Constitution v1.5.0)
  - **Controllers**: `QCRestController`, `QCViolationRestController`,
    `QCCorrectiveActionRestController` extending `BaseRestController`
  - **Forms/DTOs**: `QCControlLotForm`, `QCResultForm`, `QCViolationForm`,
    `QCCorrectiveActionForm`
  - **Services compile ALL data within transaction** using JOIN FETCH to prevent
    LazyInitializationException (Constitution v1.4.0)
- [x] **Test Coverage**: Comprehensive testing strategy with >80% backend
      (JaCoCo), >70% frontend (Jest) coverage:
  - Unit tests (JUnit 4 + Mockito) for Westgard rule evaluators (8 separate
    classes)
  - ORM validation tests for entity mappings (Constitution V.4)
  - DAO tests (@DataJpaTest) for persistence layer
  - Controller tests (@WebMvcTest) for HTTP layer
  - Integration tests (@SpringBootTest) for full workflows
  - Frontend unit tests (Jest + React Testing Library)
  - E2E tests (Cypress) following Constitution V.5 best practices:
    - Individual test execution during development
    - Browser console logging enabled and reviewed
    - Video disabled by default, screenshots enabled
    - data-testid selectors preferred
    - cy.session() for login (10-20x faster)
    - API-based test data setup (cy.request())
- [x] **Schema Management**: Liquibase changesets for all schema changes:
  - `liquibase/qc/001-create-qc-tables.xml`
  - `liquibase/qc/002-create-westgard-rule-config.xml`
  - `liquibase/qc/003-create-qc-violation-tables.xml`
  - `liquibase/qc/004-create-qc-corrective-action.xml`
  - `liquibase/qc/005-create-qc-alert.xml`
  - Rollback scripts provided for all structural changes
- [x] **Internationalization**: All UI strings use React Intl with message keys
      following convention: `qc.{context}.{element}.{property}`. Translations
      provided for en + fr minimum (Constitution VII).
- [x] **Security & Compliance**:
  - RBAC: Results (view), Biologist (configure/resolve), Global Admin (full
    access) per FR-078-081
  - Audit trail: `sys_user_id` + `lastupdated` in `BaseObject<String>` for all
    entities
  - Input validation: Hibernate Validator + Formik validation
  - SLIPTA/ISO 15189 compliance: Audit trail, corrective action documentation,
    violation retention (Constitution VIII)

**No Complexity Justification Required**: All choices align with Constitution
mandates.

## Testing Strategy

**Reference**:
[OpenELIS Testing Roadmap](../../.specify/guides/testing-roadmap.md)

### Coverage Goals

- **Backend**: >80% code coverage (measured via JaCoCo)
- **Frontend**: >70% code coverage (measured via Jest)
- **Critical Paths**: 100% coverage for:
  - Westgard rule evaluators (all 8 rules)
  - Statistical calculation (z-score, mean, SD)
  - Violation detection and alert generation
  - Corrective action workflows
  - Patient result release blocking

### Test Types

- [x] **Unit Tests**: Service layer business logic (JUnit 4 + Mockito)

  - Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`
  - **Reference**:
    [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](../../.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito)
  - **Reference**:
    [Backend Testing Best Practices](../../.specify/guides/backend-testing-best-practices.md)
  - **Coverage Goal**: >80% (measured via JaCoCo)
  - **SDD Checkpoint**: After Phase 2 (Services), all unit tests MUST pass
  - **TDD Workflow**: Red-Green-Refactor cycle for Westgard rule evaluators
    (complex logic)
  - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` for isolated unit
    tests
  - **Mocking**: Use `@Mock` (NOT `@MockBean`) for DAOs in service tests
  - **Focus Areas**:
    - 8 Westgard rule evaluators (1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ, 4₁ₛ, 10ₓ, 3₁ₛ, 7ₜ) with
      reference datasets
    - Z-score calculation accuracy
    - Statistics calculation methods (initial, rolling, manufacturer)
    - Alert batching logic (max 1 per analyzer per 15 min, except 1₃ₛ
      immediate)
    - Corrective action workflow state transitions

- [x] **DAO Tests**: Persistence layer testing (@DataJpaTest)

  - Template: `.specify/templates/testing/DataJpaTestDao.java.template`
  - **Reference**:
    [Testing Roadmap - @DataJpaTest (DAO/Repository Layer)](../../.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer)
  - **Reference**:
    [Backend Testing Best Practices](../../.specify/guides/backend-testing-best-practices.md)
  - **Test Slicing**: Use `@DataJpaTest` for DAO testing (faster than
    `@SpringBootTest`)
  - **Test Data**: Use `TestEntityManager` (NOT JdbcTemplate)
  - **Transaction Management**: Automatic rollback (no manual cleanup)
  - **Focus Areas**:
    - QC result insertion with control lot associations
    - Historical result retrieval for sequential rules (10ₓ requires 10
      previous)
    - Violation queries with filtering (by severity, date range, resolution
      status)
    - Statistics calculation with date-based queries

- [x] **Controller Tests**: REST API endpoints (@WebMvcTest)

  - Template: `.specify/templates/testing/WebMvcTestController.java.template`
  - **Reference**:
    [Testing Roadmap - @WebMvcTest (Controller Layer)](../../.specify/guides/testing-roadmap.md#webmvctest-controller-layer)
  - **Reference**:
    [Backend Testing Best Practices](../../.specify/guides/backend-testing-best-practices.md)
  - **Test Slicing**: Use `@WebMvcTest` for controller testing
  - **Mocking**: Use `@MockBean` (NOT `@Mock`) for Spring context mocking
  - **HTTP Testing**: Use `MockMvc` for request/response testing
  - **Focus Areas**:
    - GET `/rest/qc/dashboard/{analyzerId}` - dashboard data
    - POST `/rest/qc/results` - QC result entry with auto-evaluation
    - GET `/rest/qc/violations?analyzerId=&status=` - violation listing
    - POST `/rest/qc/violations/{id}/resolve` - violation resolution
    - POST `/rest/qc/corrective-actions` - corrective action creation

- [x] **ORM Validation Tests**: Entity mapping validation (Constitution V.4)

  - **Reference**:
    [Testing Roadmap - ORM Validation Tests](../../.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4)
  - **SDD Checkpoint**: After Phase 1 (Entities), ORM validation tests MUST pass
  - **Requirements**: MUST execute in <5 seconds, MUST NOT require database
  - **Pattern**:

    ```java
    @Test
    public void testQCHibernateMappingsLoadSuccessfully() {
        Configuration config = new Configuration();
        config.addAnnotatedClass(QCControlLot.class);
        config.addAnnotatedClass(QCResult.class);
        config.addAnnotatedClass(QCStatistics.class);
        config.addAnnotatedClass(WestgardRuleConfig.class);
        config.addAnnotatedClass(QCRuleViolation.class);
        config.addAnnotatedClass(QCCorrectiveAction.class);
        config.addAnnotatedClass(QCAlert.class);
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        SessionFactory sf = config.buildSessionFactory();
        assertNotNull("All QC mappings should load", sf);
        sf.close();
    }
    ```

- [x] **Integration Tests**: Full workflow testing (@SpringBootTest)

  - **Reference**:
    [Testing Roadmap - @SpringBootTest (Full Integration)](../../.specify/guides/testing-roadmap.md#springboottest-full-integration)
  - **Reference**:
    [Backend Testing Best Practices](../../.specify/guides/backend-testing-best-practices.md)
  - **Test Slicing**: Use `@SpringBootTest` only when full context required
  - **Transaction Management**: Use `@Transactional` for automatic rollback
  - **SDD Checkpoint**: After Phase 3 (Controllers), integration tests MUST pass
  - **Focus Areas**:
    - End-to-end QC result capture → rule evaluation → violation creation →
      alert generation
    - ASTM interface result capture triggering auto-evaluation
    - Corrective action workflow from creation → assignment → completion →
      resolution
    - Patient result release blocking when rejection violation exists

- [x] **Frontend Unit Tests**: React component logic (Jest + React Testing
      Library)

  - Template: `.specify/templates/testing/JestComponent.test.jsx.template`
  - **Reference**:
    [Testing Roadmap - Jest + React Testing Library](../../.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests)
  - **Reference**:
    [Jest Best Practices](../../.specify/guides/jest-best-practices.md)
  - **Coverage Goal**: >70% (measured via Jest)
  - **SDD Checkpoint**: After Phase 4 (Frontend), all unit tests MUST pass
  - **TDD Workflow**: Red-Green-Refactor cycle for complex logic
  - **Focus Areas**:
    - QC Dashboard component with compliance status tiles
    - Levey-Jennings chart component with SD lines and violation highlighting
    - Control lot setup form with statistics calculation method selection
    - Rule configuration component with enable/disable toggles
    - Violation list filtering and sorting

- [x] **E2E Tests**: Critical user workflows (Cypress)
  - Template: `.specify/templates/testing/CypressE2E.cy.js.template`
  - **Reference**:
    [Constitution Section V.5](../../.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices)
  - **Reference**:
    [Testing Roadmap - Cypress E2E Testing](../../.specify/guides/testing-roadmap.md#cypress-e2e-testing)
  - **Reference**:
    [Cypress Best Practices](../../.specify/guides/cypress-best-practices.md)
  - **Execution**: Individual test files during development (max 5-10 test cases
    per run)
  - **Selectors**: data-testid preferred (e.g.,
    `data-testid="qc-dashboard-compliance-tile"`)
  - **Session Management**: Use `cy.session()` for login (10-20x faster)
  - **Test Data**: API-based setup via `cy.request()` (10x faster than UI)
  - **Focus Areas**:
    - User Story 1: View analyzer compliance status (dashboard interaction)
    - User Story 2: Monitor QC data with control charts (chart filtering,
      tooltip)
    - User Story 3: Receive automated alerts (notification display, navigation)
    - User Story 4: Manage corrective actions (create, assign, complete
      workflow)

### Test Data Management

**Backend**:

- **Unit Tests (JUnit 4 + Mockito)**:
  - [x] Use builders:
        `QCResultBuilder.create().withValue(150.5).withZScore(2.1).build()`
  - [x] Mock data for reference datasets (Westgard literature test cases)
  - [x] Test edge cases: null values, insufficient data, out-of-range z-scores
- **DAO Tests (@DataJpaTest)**:
  - [x] Use `TestEntityManager` for entity persistence
  - [x] Automatic transaction rollback (no manual cleanup)
  - [x] Test data: Control lots with 20+ results for statistics establishment
- **Controller Tests (@WebMvcTest)**:
  - [x] Mock service layer with `@MockBean`
  - [x] Use builders for form/DTO creation
- **Integration Tests (@SpringBootTest)**:
  - [x] Use `@Transactional` for automatic rollback
  - [x] Use builders for complete entity graphs (control lot → results →
        violations)

**Frontend**:

- **E2E Tests (Cypress)**:
  - [x] Use `cy.request()` for API-based setup (create control lots, results)
  - [x] Use `cy.session()` for login state caching
  - [x] Custom commands: `cy.createQCControlLot()`, `cy.createQCResult()`,
        `cy.cleanupQCData()`
  - [x] Fixtures with `cy.intercept()` for consistent dashboard data
- **Unit Tests (Jest)**:
  - [x] Mock data factories: `createMockQCResult({ value: 100, zScore: 1.5 })`
  - [x] `setupApiMocks()` helper for consistent API responses
  - [x] `renderWithIntl()` helper for component rendering
  - [x] Test edge cases: null data, empty arrays, boundary values

### Checkpoint Validations

- [x] **After Phase 1 (Entities)**: ORM validation tests must pass (all 7
      entities load without errors)
- [x] **After Phase 2 (Services)**: Backend unit tests must pass (8 rule
      evaluators, 3 statistics methods, alert batching)
- [x] **After Phase 3 (Controllers)**: Integration tests must pass (full
      workflows end-to-end)
- [x] **After Phase 4 (Frontend)**: Frontend unit tests (Jest) AND E2E tests
      (Cypress) must pass

### TDD Workflow

- [x] **TDD Mandatory**: Red-Green-Refactor cycle for:
  - Westgard rule evaluators (8 classes with reference datasets)
  - Statistical calculation methods (z-score accuracy)
  - Alert batching logic (timing + exception handling)
  - Corrective action state machine
- [x] **Test Tasks First**: Test tasks appear before implementation tasks in
      tasks.md
- [x] **Checkpoint Enforcement**: Tests must pass before proceeding to next
      phase

## Project Structure

### Documentation (this feature)

```text
specs/003-westgard-qc/
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 research findings
├── data-model.md        # Phase 1 entity relationship documentation
├── quickstart.md        # Phase 1 step-by-step developer guide
├── contracts/           # Phase 1 API contracts (OpenAPI specs)
│   ├── qc-api.yaml              # QC dashboard + result entry endpoints
│   ├── violation-api.yaml       # Violation management endpoints
│   └── corrective-action-api.yaml  # Corrective action endpoints
└── tasks.md             # Phase 2 (/speckit.tasks output - NOT created by /speckit.plan)
```

### Source Code (repository root)

**Web Application Structure** (Backend + Frontend):

```text
backend/
├── src/main/java/org/openelisglobal/qc/
│   ├── valueholder/                    # JPA Entities (Layer 1)
│   │   ├── QCControlLot.java
│   │   ├── QCResult.java
│   │   ├── QCStatistics.java
│   │   ├── WestgardRuleConfig.java
│   │   ├── QCRuleViolation.java
│   │   ├── QCCorrectiveAction.java
│   │   └── QCAlert.java
│   ├── dao/                            # Data Access Objects (Layer 2)
│   │   ├── QCControlLotDAO.java
│   │   ├── QCControlLotDAOImpl.java
│   │   ├── QCResultDAO.java
│   │   ├── QCResultDAOImpl.java
│   │   ├── QCStatisticsDAO.java
│   │   ├── QCStatisticsDAOImpl.java
│   │   ├── WestgardRuleConfigDAO.java
│   │   ├── WestgardRuleConfigDAOImpl.java
│   │   ├── QCRuleViolationDAO.java
│   │   ├── QCRuleViolationDAOImpl.java
│   │   ├── QCCorrectiveActionDAO.java
│   │   ├── QCCorrectiveActionDAOImpl.java
│   │   ├── QCAlertDAO.java
│   │   └── QCAlertDAOImpl.java
│   ├── service/                        # Business Logic (Layer 3)
│   │   ├── QCControlLotService.java
│   │   ├── QCControlLotServiceImpl.java
│   │   ├── QCResultService.java
│   │   ├── QCResultServiceImpl.java
│   │   ├── QCStatisticsService.java
│   │   ├── QCStatisticsServiceImpl.java
│   │   ├── WestgardRuleEvaluationService.java      # Coordinates rule evaluation
│   │   ├── WestgardRuleEvaluationServiceImpl.java
│   │   ├── evaluator/                              # Strategy pattern for rules
│   │   │   ├── WestgardRuleEvaluator.java (interface)
│   │   │   ├── Rule1_2sEvaluator.java              # Warning: ±2SD
│   │   │   ├── Rule1_3sEvaluator.java              # Rejection: ±3SD
│   │   │   ├── Rule2_2sEvaluator.java              # Rejection: consecutive ±2SD
│   │   │   ├── RuleR_4sEvaluator.java              # Rejection: range 4SD
│   │   │   ├── Rule4_1sEvaluator.java              # Rejection: 4 consecutive ±1SD
│   │   │   ├── Rule10_xEvaluator.java              # Rejection: 10 same side
│   │   │   ├── Rule3_1sEvaluator.java              # Warning: 3 consecutive ±1SD
│   │   │   └── Rule7_tEvaluator.java               # Warning: 7 trend
│   │   ├── calculator/                             # Strategy pattern for statistics
│   │   │   ├── StatisticsCalculator.java (interface)
│   │   │   ├── InitialRunsCalculator.java          # First N runs (default 20)
│   │   │   ├── RollingCalculator.java              # Moving window
│   │   │   └── ManufacturerFixedCalculator.java    # Fixed mean/SD
│   │   ├── QCRuleViolationService.java
│   │   ├── QCRuleViolationServiceImpl.java
│   │   ├── QCCorrectiveActionService.java
│   │   ├── QCCorrectiveActionServiceImpl.java
│   │   ├── QCAlertService.java
│   │   └── QCAlertServiceImpl.java
│   ├── controller/                     # REST Endpoints (Layer 4)
│   │   ├── QCRestController.java               # Dashboard + result entry
│   │   ├── QCViolationRestController.java      # Violation management
│   │   └── QCCorrectiveActionRestController.java  # Corrective actions
│   └── form/                           # DTOs (Layer 5)
│       ├── QCControlLotForm.java
│       ├── QCResultForm.java
│       ├── QCViolationForm.java
│       └── QCCorrectiveActionForm.java
├── src/main/resources/liquibase/qc/
│   ├── 001-create-qc-tables.xml
│   ├── 002-create-westgard-rule-config.xml
│   ├── 003-create-qc-violation-tables.xml
│   ├── 004-create-qc-corrective-action.xml
│   └── 005-create-qc-alert.xml
└── src/test/java/org/openelisglobal/qc/
    ├── HibernateMappingValidationTest.java     # ORM validation
    ├── service/
    │   ├── QCResultServiceTest.java            # Unit tests
    │   ├── evaluator/
    │   │   ├── Rule1_2sEvaluatorTest.java      # Rule unit tests with reference datasets
    │   │   ├── Rule1_3sEvaluatorTest.java
    │   │   ├── Rule2_2sEvaluatorTest.java
    │   │   ├── RuleR_4sEvaluatorTest.java
    │   │   ├── Rule4_1sEvaluatorTest.java
    │   │   ├── Rule10_xEvaluatorTest.java
    │   │   ├── Rule3_1sEvaluatorTest.java
    │   │   └── Rule7_tEvaluatorTest.java
    │   └── calculator/
    │       ├── InitialRunsCalculatorTest.java
    │       ├── RollingCalculatorTest.java
    │       └── ManufacturerFixedCalculatorTest.java
    ├── dao/
    │   ├── QCResultDAOTest.java                # DAO integration tests (@DataJpaTest)
    │   └── QCRuleViolationDAOTest.java
    └── controller/
        ├── QCRestControllerTest.java           # Controller tests (@WebMvcTest)
        └── QCViolationRestControllerTest.java

frontend/
├── src/components/qc/
│   ├── dashboard/
│   │   ├── QCDashboard.jsx                     # Main dashboard (compliance cards + analyzer list)
│   │   ├── QCDashboard.test.jsx
│   │   ├── ComplianceStatusTile.jsx            # Green/Yellow/Red indicator tile
│   │   └── ComplianceStatusTile.test.jsx
│   ├── charts/
│   │   ├── LeveyJenningsChart.jsx              # @carbon/charts-react LineChart
│   │   ├── LeveyJenningsChart.test.jsx
│   │   ├── ControlChartDetail.jsx              # Chart with date filters + tooltips
│   │   └── ControlChartDetail.test.jsx
│   ├── violations/
│   │   ├── ViolationList.jsx                   # DataTable with filtering
│   │   ├── ViolationList.test.jsx
│   │   ├── ViolationDetailModal.jsx            # ComposedModal with violation details
│   │   └── ViolationDetailModal.test.jsx
│   ├── correctiveActions/
│   │   ├── CorrectiveActionForm.jsx            # Create/Edit corrective action
│   │   ├── CorrectiveActionForm.test.jsx
│   │   ├── CorrectiveActionList.jsx            # Task list view
│   │   └── CorrectiveActionList.test.jsx
│   ├── controlLots/
│   │   ├── ControlLotSetup.jsx                 # Control lot configuration
│   │   ├── ControlLotSetup.test.jsx
│   │   ├── StatisticsConfigModal.jsx           # Statistics method selection
│   │   └── StatisticsConfigModal.test.jsx
│   ├── alerts/
│   │   ├── AlertFeed.jsx                       # Real-time alert display
│   │   └── AlertFeed.test.jsx
│   └── ruleConfig/
│       ├── RuleConfigPanel.jsx                 # Enable/disable rules per analyzer
│       └── RuleConfigPanel.test.jsx
└── cypress/e2e/qc/
    ├── qcDashboard.cy.js                       # User Story 1: View compliance
    ├── controlCharts.cy.js                     # User Story 2: Monitor QC data
    ├── qcAlerts.cy.js                          # User Story 3: Receive alerts
    └── correctiveActions.cy.js                 # User Story 4: Manage corrective actions
```

**Structure Decision**: Web application structure selected due to Java backend +
React frontend architecture. QC feature follows established OpenELIS patterns
with `/org/openelisglobal/qc/` backend package and `/components/qc/` frontend
directory. Navigation integration places QC pages under `/analyzers/qc` route
per feature 004 analyzer menu structure (FR-020).

## Phases

### Phase 0: Research

**Goal**: Resolve all unknowns before design. No code changes.

**Research Tasks** (Completed by Plan agent):

- [x] Westgard rule algorithms (found authoritative reference:
      `.dev-docs/OGC-41/westgard_rules_implementation.md`)
- [x] OpenELIS analyzer integration (feature 004 spec reviewed, navigation
      structure documented)
- [x] Existing notification system (located `EmailNotification` infrastructure)
- [x] Existing corrective action workflow (adapted from `NcEvent` pattern)
- [x] Carbon Design System charting (@carbon/charts-react `LineChart` confirmed)
- [x] Testing strategy (roadmap reviewed, templates located)
- [x] React Intl localization patterns (message key conventions documented)

**Output**: [research.md](research.md) with findings, decisions, and technical
choices.

### Phase 1: Design & Contracts

**Goal**: Define data model, API contracts, quickstart guide. Update agent
context.

**Prerequisites**: Phase 0 research complete.

**Tasks**:

1. **Generate data-model.md**:

   - Entity relationship diagrams
   - Table schemas with columns, types, constraints
   - Relationship cardinalities (1:1, 1:N, N:N)
   - Index specifications for performance
   - Validation rules per entity
   - State transitions (control lot status, corrective action workflow)

2. **Generate API contracts** (`contracts/` directory):

   - `qc-api.yaml`: OpenAPI spec for dashboard, result entry, control lots
   - `violation-api.yaml`: OpenAPI spec for violation management
   - `corrective-action-api.yaml`: OpenAPI spec for corrective action workflows
   - Request/response schemas
   - Error responses with internationalization keys
   - Authentication/authorization requirements

3. **Generate quickstart.md**:

   - Step-by-step developer guide
   - Local development setup
   - Database migration execution
   - Test execution commands
   - Common troubleshooting

4. **Update agent context**:
   - Run `.specify/scripts/bash/update-agent-context.sh claude`
   - Preserve manual additions between markers
   - Add only new technology from this plan

**Outputs**:

- [data-model.md](data-model.md)
- [contracts/qc-api.yaml](contracts/qc-api.yaml)
- [contracts/violation-api.yaml](contracts/violation-api.yaml)
- [contracts/corrective-action-api.yaml](contracts/corrective-action-api.yaml)
- [quickstart.md](quickstart.md)
- Updated CLAUDE.md with QC-specific context

**Post-Phase 1 Constitution Check**:

- [x] All entities use JPA annotations (NO XML mappings)
- [x] All entities extend `BaseObject<String>` with `sys_user_id` +
      `lastupdated`
- [x] Control lot entity includes `fhir_uuid UUID` for future FHIR integration
      (if needed)
- [x] Database schema follows PostgreSQL 14+ conventions
- [x] Liquibase changesets provided with rollback scripts

### Phase 2: Task Generation

**Goal**: Break down implementation into actionable, dependency-ordered tasks.

**Prerequisites**: Phase 1 complete (data model + contracts defined).

**Process**: Run `/speckit.tasks` command (NOT created by `/speckit.plan`).

**Output**: [tasks.md](tasks.md) with:

- Task breakdown by phase (Entities → Services → Controllers → Frontend → E2E)
- Dependency ordering (e.g., `QCResultService` depends on `QCControlLotService`)
- Test tasks before implementation tasks (TDD enforcement)
- Parallel task indicators `[P]` where tasks can run concurrently
- Checkpoint validations after each phase

**Example Task Structure**:

```markdown
## Phase 1: Entities (QC Control Lots + Results)

### Tests for Phase 1 (MANDATORY - TDD)

- [ ] T001 [P] [ORM] ORM validation test for all 7 QC entities
- [ ] T002 [P] [DAO] DAO test for QCControlLotDAO
- [ ] T003 [P] [DAO] DAO test for QCResultDAO

### Implementation for Phase 1

- [ ] T004 [ENT] Create QCControlLot entity (depends on T001 passing)
- [ ] T005 [ENT] Create QCResult entity (depends on T001 passing)
- [ ] T006 [DAO] Create QCControlLotDAO (depends on T002 passing)
- [ ] T007 [DAO] Create QCResultDAO (depends on T003 passing)
- [ ] T008 [LIQ] Create Liquibase changeset 001-create-qc-tables.xml

**Checkpoint**: ORM validation tests MUST pass before proceeding to Phase 2.
```

### Phase 3+: Implementation

**Goal**: Execute tasks.md in dependency order using TDD workflow.

**Prerequisites**: Phase 2 tasks.md generated.

**Process**: Use `/speckit.implement` command to execute tasks.md.

**TDD Workflow** (per task):

1. **Red**: Write failing test first
2. **Green**: Write minimal implementation to pass test
3. **Refactor**: Improve code quality while keeping tests green
4. Mark task as complete only when test passes

**Checkpoint Validations**:

- After Phase 1 (Entities): ORM validation tests pass
- After Phase 2 (Services): Unit tests pass (rule evaluators, statistics
  calculators)
- After Phase 3 (Controllers): Integration tests pass (full workflows)
- After Phase 4 (Frontend): Jest unit tests + Cypress E2E tests pass

## Navigation Integration (Feature 004 Analyzer Menu)

**Dependency**: Feature 003's QC pages are nested UNDER feature 004's /analyzers parent menu per 004:FR-020. Feature 004 MANAGES the navigation hierarchy via `/rest/menu` API; feature 003 implements ONLY the page components.

**Routes Provided by 003**:
- `/analyzers/qc` - Main QC Dashboard (Quality Control overview)
- `/analyzers/qc/alerts` - QC Alerts & Violations (violation management)
- `/analyzers/qc/corrective-actions` - Corrective Actions (workflow management)

**Navigation Structure** (004's responsibility):

```
Analyzers (parent, managed by 004)
├── Analyzers Dashboard (/analyzers) [004]
├── Error Dashboard (/analyzers/errors) [004]
└── Quality Control (group)
    ├── QC Dashboard (/analyzers/qc) [003 - this feature]
    ├── QC Alerts & Violations (/analyzers/qc/alerts) [003 - this feature]
    └── Corrective Actions (/analyzers/qc/corrective-actions) [003 - this feature]
```

**Implementation Details**:

- Menu items added to database menu structure
- Backend `/rest/menu` API exposes QC routes
- Frontend renders menu dynamically based on API response
- Role-based visibility: QC routes hidden if user lacks quality-control
  permissions
- Active highlighting: `currentPath.startsWith("/analyzers/qc")` highlights QC
  items
- State preservation: Filters + pagination in URL query params, scroll position
  in sessionStorage
- NO Carbon Tabs/TabList on QC pages - navigation via left-hand sub-menu only

**Route Configuration**:

```javascript
// frontend/src/App.js
<Route path="/analyzers/qc" element={<QCDashboard />} />
<Route path="/analyzers/qc/alerts" element={<QCViolationList />} />
<Route path="/analyzers/qc/corrective-actions" element={<QCCorrectiveActionList />} />
<Route path="/analyzers/qc/charts/:instrumentId" element={<ControlChartDetail />} />
```

**Menu Database Entry** (example):

```sql
INSERT INTO menu (id, parent_id, menu_key, display_key, route, active_route_prefix, role_required)
VALUES
  ('MENU_QC', 'MENU_ANALYZERS', 'qc', 'analyzer.menu.qc', '/analyzers/qc', '/analyzers/qc', 'BIOLOGIST'),
  ('MENU_QC_ALERTS', 'MENU_QC', 'qc.alerts', 'analyzer.menu.qc.alerts', '/analyzers/qc/alerts', '/analyzers/qc/alerts', 'BIOLOGIST'),
  ('MENU_QC_ACTIONS', 'MENU_QC', 'qc.correctiveActions', 'analyzer.menu.qc.correctiveActions', '/analyzers/qc/corrective-actions', '/analyzers/qc/corrective-actions', 'BIOLOGIST');
```

## Key Technical Decisions

### 1. Westgard Rule Evaluation Architecture

**Pattern**: Strategy + Chain of Responsibility

**Rationale**:

- Strategy pattern for rule evaluators enables independent unit testing per rule
- Chain of Responsibility allows sequential evaluation with early termination
- Each evaluator is stateless and independently testable with reference datasets

**Implementation**:

```java
public interface WestgardRuleEvaluator {
    boolean canEvaluate(WestgardRuleConfig config);
    RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults, QCStatistics statistics);
}

public class WestgardRuleEvaluationServiceImpl {
    @Autowired private List<WestgardRuleEvaluator> evaluators;

    public List<RuleEvaluationResult> evaluateAll(QCResult result) {
        List<WestgardRuleConfig> enabledRules = ruleConfigDAO.findEnabledByInstrument(result.getInstrumentId());
        List<QCResult> history = resultDAO.findHistoricalForRule(result.getControlLotId(), 10);
        QCStatistics stats = statisticsDAO.findLatestByControlLot(result.getControlLotId());

        return enabledRules.stream()
            .map(config -> evaluators.stream()
                .filter(evaluator -> evaluator.canEvaluate(config))
                .findFirst()
                .map(evaluator -> evaluator.evaluate(result, history, stats))
                .orElseThrow())
            .collect(Collectors.toList());
    }
}
```

### 2. Statistics Calculation

**Pattern**: Strategy pattern with caching

**Rationale**:

- Different calculation methods (initial, rolling, manufacturer) require
  different algorithms
- Statistics change infrequently (only when new results added or lot
  transitions)
- Caching in `qc_statistics` table prevents repeated calculations

**Implementation**:

```java
public interface StatisticsCalculator {
    boolean supports(CalculationMethod method);
    QCStatistics calculate(QCControlLot lot, List<QCResult> results);
}

public class QCStatisticsServiceImpl {
    @Autowired private List<StatisticsCalculator> calculators;

    @Transactional
    public QCStatistics getOrCalculateStatistics(QCControlLot lot) {
        QCStatistics cached = statisticsDAO.findLatestByControlLot(lot.getId());
        if (cached != null && !shouldRecalculate(lot, cached)) {
            return cached;
        }

        List<QCResult> results = resultDAO.findByControlLot(lot.getId());
        StatisticsCalculator calculator = calculators.stream()
            .filter(c -> c.supports(lot.getCalculationMethod()))
            .findFirst()
            .orElseThrow();

        QCStatistics newStats = calculator.calculate(lot, results);
        statisticsDAO.insert(newStats);
        return newStats;
    }
}
```

### 3. Alert Batching

**Pattern**: Service-layer batching with exception handling

**Rationale**:

- Prevent alert fatigue (max 1 email per analyzer per 15 min)
- Exception for 1₃ₛ (critical severity requires immediate notification per
  FR-073)
- Batching logic in service layer maintains testability

**Implementation**:

```java
public class QCAlertServiceImpl {
    @Transactional
    public void sendAlertForViolation(QCRuleViolation violation) {
        // Check for 1₃ₛ immediate alert exception
        if ("1_3s".equals(violation.getRuleCode())) {
            sendImmediateAlert(violation);
            return;
        }

        // Batch other alerts
        LocalDateTime now = LocalDateTime.now();
        QCAlert recentAlert = alertDAO.findMostRecentByInstrument(violation.getInstrumentId());

        if (recentAlert != null &&
            ChronoUnit.MINUTES.between(recentAlert.getSentDateTime(), now) < 15) {
            // Within batching window - skip alert
            logEvent("Alert batched for analyzer " + violation.getAnalyzerId());
            return;
        }

        sendBatchedAlert(violation);
    }
}
```

### 4. ASTM Result Auto-Evaluation

**Pattern**: Event listener with async processing

**Rationale**:

- ASTM interface result capture should not block analyzer communication
- Rule evaluation happens asynchronously after result persisted
- Transaction boundaries ensure result saved before evaluation starts

**Implementation**:

```java
@Service
public class QCResultServiceImpl {
    @Autowired private WestgardRuleEvaluationService ruleEvaluationService;
    @Autowired private ApplicationEventPublisher eventPublisher;

    @Transactional
    public String insert(QCResult result) {
        String id = qcResultDAO.insert(result);

        // Publish event for async evaluation (transaction boundary respected)
        eventPublisher.publishEvent(new QCResultCreatedEvent(id));

        return id;
    }
}

@Component
public class QCResultCreatedEventListener {
    @Async
    @EventListener
    @Transactional
    public void handleQCResultCreated(QCResultCreatedEvent event) {
        QCResult result = qcResultDAO.get(event.getResultId());
        List<RuleEvaluationResult> violations = ruleEvaluationService.evaluateAll(result);

        violations.stream()
            .filter(RuleEvaluationResult::isViolated)
            .forEach(v -> qcRuleViolationService.createViolation(v));
    }
}
```

### 5. Corrective Action Workflow

**Pattern**: State machine with validation guards

**Rationale**:

- Enforces proper workflow: Create → Assign → In Progress → Completed → Resolved
- Prevents invalid state transitions (e.g., cannot complete without assignment)
- Audit trail preserved at each state change

**States**:

- `PENDING`: Created, not yet assigned
- `ASSIGNED`: Assigned to user, waiting for work to start
- `IN_PROGRESS`: User actively working on corrective action
- `COMPLETED`: Work finished, awaiting verification
- `RESOLVED`: Verified and closed, violation resolved

**Transitions**:

```
PENDING → ASSIGNED (assign action)
ASSIGNED → IN_PROGRESS (start work)
IN_PROGRESS → COMPLETED (finish work with resolution notes)
COMPLETED → RESOLVED (verify and close, auto-resolves linked violation)
```

**Validation Guards**:

- Cannot assign to user without BIOLOGIST role
- Cannot mark completed without resolution notes
- Cannot resolve violation until all linked corrective actions completed

### 6. Carbon Charts Configuration

**Component**: `@carbon/charts-react` `LineChart`

**Customizations**:

- Horizontal reference lines for mean, ±1SD, ±2SD, ±3SD (via `grid` option)
- Point styling based on violation status (color + size via `points` option)
- Tooltip with result value, z-score, date/time, violations (via
  `tooltip.customHTML`)
- Date range filtering (via `axes.bottom.domain`)

**Example**:

```tsx
const chartOptions = {
  axes: {
    bottom: {
      title: "Run Number",
      mapsTo: "date",
      scaleType: "time",
    },
    left: {
      title: "Result Value",
      mapsTo: "value",
      scaleType: "linear",
    },
  },
  grid: {
    y: {
      enabled: true,
      values: [
        mean,
        mean + sd,
        mean + 2 * sd,
        mean + 3 * sd,
        mean - sd,
        mean - 2 * sd,
        mean - 3 * sd,
      ],
    },
  },
  points: {
    radius: (dataPoint) => (dataPoint.violated ? 6 : 4),
    filled: true,
    color: (dataPoint) => (dataPoint.violated ? "#da1e28" : "#0f62fe"), // Red vs blue
  },
  tooltip: {
    customHTML: ([dataPoint]) => `
      <div>
        <strong>Value:</strong> ${dataPoint.value}<br/>
        <strong>Z-score:</strong> ${dataPoint.zScore}<br/>
        <strong>Date:</strong> ${format(
          dataPoint.date,
          "MM/dd/yyyy HH:mm"
        )}<br/>
        ${
          dataPoint.violations
            ? `<strong>Violations:</strong> ${dataPoint.violations.join(", ")}`
            : ""
        }
      </div>
    `,
  },
};
```

## Risk Mitigation

**Risk 1: Rule Evaluation Performance Degradation**

- **Likelihood**: Medium (sequential rule evaluation with database queries)
- **Impact**: High (FR-028 requires <5s evaluation)
- **Mitigation**:
  - Cache statistics in `qc_statistics` table (avoid recalculation per result)
  - Use single query to fetch historical results (JOIN FETCH in DAO)
  - Index `qc_result` on `control_lot_id` + `run_date_time` for historical
    queries
  - Monitor p95 latency, optimize HQL if >2s

**Risk 2: Alert Fatigue from Excessive Notifications**

- **Likelihood**: High (frequent warning-level violations)
- **Impact**: Medium (users ignore important alerts)
- **Mitigation**:
  - Implement batching: max 1 email per analyzer per 15 min
  - Exception for 1₃ₛ: immediate alerts for critical rejections
  - In-system notification feed for all alerts (no email spam)
  - User preferences to customize alert channels per severity

**Risk 3: Incorrect Westgard Rule Implementation**

- **Likelihood**: Low (authoritative reference available)
- **Impact**: Critical (incorrect QC decisions affect patient safety)
- **Mitigation**:
  - Use reference datasets from Westgard literature for unit tests
  - Implement all 8 rules with TDD (write test with expected violation first)
  - Code review by QC subject matter expert before merge
  - Validate against manual calculations in acceptance testing

**Risk 4: ASTM Interface Result Capture Failures**

- **Likelihood**: Medium (network issues, analyzer downtime)
- **Impact**: High (QC results not evaluated, compliance gaps)
- **Mitigation**:
  - Manual entry fallback when interface unavailable (FR-013)
  - Async event processing (result saved even if evaluation fails)
  - Error dashboard for unmapped/failed results (reprocessing capability)
  - Monitoring with alerts for interface downtime

**Risk 5: Cypress E2E Test Flakiness**

- **Likelihood**: Medium (Carbon component async rendering, network timing)
- **Impact**: Medium (false negatives block PRs)
- **Mitigation**:
  - Use data-testid selectors (most stable, per Constitution V.5)
  - Use `cy.intercept()` with aliases for reliable API waiting
  - Use `.should()` assertions for retry-ability (no arbitrary waits)
  - Run tests individually during development (max 5-10 test cases per run)
  - Review browser console logs after each run (catch hidden errors)

## Success Criteria

**From spec.md SC-001 to SC-015**:

- SC-001: Technicians identify non-compliant analyzers within 10 seconds
- SC-002: Auto-capture + evaluation complete within 5 seconds (FR-028)
- SC-003: Rejection alerts delivered within 30 seconds (FR-065, FR-073)
- SC-004: 95% rule evaluations complete without errors
- SC-005: Rule configuration completes in <5 minutes
- SC-006: 99.9% uptime for QC capture + evaluation
- SC-007: Charts render in <3 seconds for 100 data points
- SC-008: Dashboard supports 50+ analyzers without degradation
- SC-009: <1% false positive/negative rate vs validated datasets
- SC-010: 100% corrective actions traceable to violations
- SC-011: Compliance reports export in <30 seconds
- SC-012: Patient result release blocked for 100% of unresolved rejections
- SC-013: Alert batching reduces volume by 50%
- SC-014: 1,000+ QC results per day processed successfully
- SC-015: 2-year data retention requirement met

**Additional Acceptance Criteria**:

- User Story 1-8 acceptance scenarios pass E2E tests
- > 80% backend coverage (JaCoCo), >70% frontend coverage (Jest)
- All constitution checks pass (no violations)
- ISO 15189 audit trail complete (all changes logged with user + timestamp)

---

**Next Steps**: Run `/speckit.tasks` to generate actionable task breakdown for
implementation.
