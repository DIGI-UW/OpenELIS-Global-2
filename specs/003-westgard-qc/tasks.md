# Tasks: Westgard Rules Quality Control Compliance

**Input**: Design documents from `/specs/003-westgard-qc/` **Prerequisites**:
plan.md, spec.md

**Tests**: Following TDD approach per AGENTS.md and testing-roadmap.md. Test
tasks appear BEFORE implementation tasks to enforce Red-Green-Refactor workflow.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/org/openelisglobal/qc/`
- **Backend Tests**: `src/test/java/org/openelisglobal/qc/`
- **Frontend**: `frontend/src/components/qc/`
- **E2E Tests**: `frontend/cypress/e2e/qc/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 Create QC package structure in backend:
      `src/main/java/org/openelisglobal/qc/` with subdirectories: valueholder/,
      dao/, service/, controller/, form/
- [x] T002 [P] Create QC test package structure in
      `src/test/java/org/openelisglobal/qc/` with subdirectories: service/,
      dao/, controller/, service/evaluator/, service/calculator/
- [x] T003 [P] Create QC frontend component structure in
      `frontend/src/components/qc/` with subdirectories: dashboard/, charts/,
      violations/, correctiveActions/, controlLots/, alerts/, ruleConfig/
- [x] T004 [P] Create Cypress E2E test structure in `frontend/cypress/e2e/qc/`
      for QC feature tests
- [x] T005 [P] Add QC internationalization keys to
      `frontend/src/languages/en.json` (base structure: qc.dashboard._,
      qc.charts._, qc.violations._, qc.correctiveActions._, qc.alerts._,
      qc.rules._)
- [x] T006 [P] Add QC internationalization keys to
      `frontend/src/languages/fr.json` (French translations for all en.json
      keys)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can
be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Liquibase Schema Setup

- [x] T007 Create Liquibase changeset
      `src/main/resources/liquibase/qc/001-create-qc-tables.xml` with tables:
      qc_control_lot, qc_result, qc_statistics (with rollback support)
- [x] T008 Create Liquibase changeset
      `src/main/resources/liquibase/qc/002-create-westgard-rule-config.xml` with
      table: westgard_rule_config and default rule definitions (with rollback
      support)
- [x] T009 Create Liquibase changeset
      `src/main/resources/liquibase/qc/003-create-qc-violation-tables.xml` with
      table: qc_rule_violation (with rollback support)
- [x] T010 Create Liquibase changeset
      `src/main/resources/liquibase/qc/004-create-qc-corrective-action.xml` with
      table: qc_corrective_action (with rollback support)
- [x] T011 Create Liquibase changeset
      `src/main/resources/liquibase/qc/005-create-qc-alert.xml` with table:
      qc_alert (with rollback support)

### Core Entities (Layer 1 - Valueholders)

- [x] T012 [P] Create QCControlLot entity in
      `src/main/java/org/openelisglobal/qc/valueholder/QCControlLot.java`
      extending BaseObject<String> with JPA annotations
- [x] T013 [P] Create QCResult entity in
      `src/main/java/org/openelisglobal/qc/valueholder/QCResult.java` extending
      BaseObject<String> with JPA annotations
- [x] T014 [P] Create QCStatistics entity in
      `src/main/java/org/openelisglobal/qc/valueholder/QCStatistics.java`
      extending BaseObject<String> with JPA annotations
- [x] T015 [P] Create WestgardRuleConfig entity in
      `src/main/java/org/openelisglobal/qc/valueholder/WestgardRuleConfig.java`
      extending BaseObject<String> with JPA annotations
- [x] T016 [P] Create QCRuleViolation entity in
      `src/main/java/org/openelisglobal/qc/valueholder/QCRuleViolation.java`
      extending BaseObject<String> with JPA annotations
- [x] T017 [P] Create QCCorrectiveAction entity in
      `src/main/java/org/openelisglobal/qc/valueholder/QCCorrectiveAction.java`
      extending BaseObject<String> with JPA annotations
- [x] T018 [P] Create QCAlert entity in
      `src/main/java/org/openelisglobal/qc/valueholder/QCAlert.java` extending
      BaseObject<String> with JPA annotations

### ORM Validation Test (Constitution V.4 - TDD Checkpoint)

- [x] T019 ORM validation test in
      `src/test/java/org/openelisglobal/qc/QCHibernateMappingValidationTest.java` -
      Build SessionFactory with all 7 QC entities (QCControlLot, QCResult,
      QCStatistics, WestgardRuleConfig, QCRuleViolation, QCCorrectiveAction,
      QCAlert) - Validate all mappings load without errors - MUST execute in <5
      seconds (per Constitution V.4) - MUST NOT require database connection -
      **SDD Checkpoint**: Must pass before proceeding to DAO layer

### Core DAOs (Layer 2 - Data Access)

- [ ] T020 [P] Create QCControlLotDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/QCControlLotDAO.java` and `QCControlLotDAOImpl.java` extending BaseDAOImpl
- [ ] T021 [P] Create QCResultDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/QCResultDAO.java` and `QCResultDAOImpl.java` with methods: findByControlLot, findHistoricalForRule, findByAnalyzerAndDateRange
- [ ] T022 [P] Create QCStatisticsDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/QCStatisticsDAO.java` and `QCStatisticsDAOImpl.java` with methods: findLatestByControlLot, findByCalculationMethod
- [ ] T023 [P] Create WestgardRuleConfigDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/WestgardRuleConfigDAO.java` and `WestgardRuleConfigDAOImpl.java` with methods: findEnabledByInstrument, findByTestAndInstrument
- [ ] T024 [P] Create QCRuleViolationDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/QCRuleViolationDAO.java` and `QCRuleViolationDAOImpl.java` with methods: findByAnalyzer, findUnresolved, findBySeverity
- [ ] T025 [P] Create QCCorrectiveActionDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/QCCorrectiveActionDAO.java` and `QCCorrectiveActionDAOImpl.java` with methods: findByViolation, findByAssignedUser, findByStatus
- [ ] T026 [P] Create QCAlertDAO interface and implementation in `src/main/java/org/openelisglobal/qc/dao/QCAlertDAO.java` and `QCAlertDAOImpl.java` with methods: findMostRecentByInstrument, findUnread, findByRecipient

### Test Data Builders (for all subsequent tests)

- [x] T027 [P] Create QCControlLotBuilder in
      `src/test/java/org/openelisglobal/qc/builder/QCControlLotBuilder.java`
      with fluent API for test data creation
- [x] T028 [P] Create QCResultBuilder in
      `src/test/java/org/openelisglobal/qc/builder/QCResultBuilder.java` with
      fluent API for test data creation
- [x] T029 [P] Create QCStatisticsBuilder in
      `src/test/java/org/openelisglobal/qc/builder/QCStatisticsBuilder.java`
      with fluent API for test data creation
- [x] T030 [P] Create WestgardRuleConfigBuilder in
      `src/test/java/org/openelisglobal/qc/builder/WestgardRuleConfigBuilder.java`
      with fluent API for test data creation
- [x] T031 [P] Create QCRuleViolationBuilder in
      `src/test/java/org/openelisglobal/qc/builder/QCRuleViolationBuilder.java`
      with fluent API for test data creation

**Checkpoint**: Foundation ready - user story implementation can now begin in
parallel

---

## Phase 3: User Story 6 - Manage QC Control Lots (Priority: P2) 🏗️ FOUNDATIONAL FOR OTHER STORIES

**Goal**: Enable lab supervisors to set up QC control lots with statistical
parameters so the system can evaluate QC results accurately.

**Why First**: Control lot management is foundational - all other stories
require control lots to exist before QC results can be captured and evaluated.

**Independent Test**: Create a new control lot with manufacturer fixed values,
verify it's immediately active and ready for QC result evaluation. Create
another lot with initial establishment method, verify it enters "establishment"
status and doesn't evaluate rules until sufficient results collected.

### Tests for User Story 6 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference:
> [OpenELIS Testing Roadmap](../../.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T032 [P] [US6] Unit test for QCControlLotService in `src/test/java/org/openelisglobal/qc/service/QCControlLotServiceTest.java` (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) - Reference: [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](../../.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) for detailed patterns - Reference: [Backend Testing Best Practices](../../.specify/guides/backend-testing-best-practices.md) for quick reference - **TDD Workflow**: Write test FIRST (RED), then implement (GREEN), then refactor - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` for isolated unit tests (NOT `@SpringBootTest`) - **Mocking**: Use `@Mock` (NOT `@MockBean`) for DAOs - **Test Cases**: Test lot creation with all 3 calculation methods (initial/rolling/manufacturer), test lot activation/deactivation, test establishment status transitions - **Coverage Goal**: >80% (measured via JaCoCo)
- [ ] T033 [P] [US6] Unit test for QCStatisticsService in `src/test/java/org/openelisglobal/qc/service/QCStatisticsServiceTest.java` (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) - Test statistics calculation caching, test shouldRecalculate logic, test all 3 calculator strategies - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` - **Mocking**: Use `@Mock` for DAOs and calculators
- [ ] T034 [P] [US6] Unit test for InitialRunsCalculator in `src/test/java/org/openelisglobal/qc/service/calculator/InitialRunsCalculatorTest.java` - Test mean/SD calculation with reference datasets (20 results), test insufficient data handling (<20 results), test accuracy against known values - Use builders for test data
- [ ] T035 [P] [US6] Unit test for RollingCalculator in `src/test/java/org/openelisglobal/qc/service/calculator/RollingCalculatorTest.java` - Test rolling window calculation, test window size configuration, test edge cases (insufficient data) - Use builders for test data
- [ ] T036 [P] [US6] Unit test for ManufacturerFixedCalculator in `src/test/java/org/openelisglobal/qc/service/calculator/ManufacturerFixedCalculatorTest.java` - Test fixed value usage (no calculation), test validation of manufacturer values - Use builders for test data
- [ ] T037 [P] [US6] DAO test for QCControlLotDAO in `src/test/java/org/openelisglobal/qc/dao/QCControlLotDAOTest.java` (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) - Reference: [Testing Roadmap - @DataJpaTest](../../.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer) - **Test Slicing**: Use `@DataJpaTest` (NOT `@SpringBootTest` - faster execution) - **Test Data**: Use `TestEntityManager` (NOT JdbcTemplate) - **Transaction Management**: Automatic rollback - Test CRUD operations, test findByAnalyzerAndTest, test active lot queries, test expiration date filtering
- [ ] T038 [P] [US6] DAO test for QCStatisticsDAO in `src/test/java/org/openelisglobal/qc/dao/QCStatisticsDAOTest.java` (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) - Test findLatestByControlLot, test statistics history queries, test calculation method filtering - Use `TestEntityManager` for test data
- [ ] T039 [P] [US6] Controller test for QCControlLotRestController in `src/test/java/org/openelisglobal/qc/controller/QCControlLotRestControllerTest.java` (Template: `.specify/templates/testing/WebMvcTestController.java.template`) - Reference: [Testing Roadmap - @WebMvcTest](../../.specify/guides/testing-roadmap.md#webmvctest-controller-layer) - **Test Slicing**: Use `@WebMvcTest` (NOT `@SpringBootTest`) - **Mocking**: Use `@MockBean` (NOT `@Mock`) for services - **HTTP Testing**: Use `MockMvc` for request/response testing - Test POST /rest/qc/control-lots (create lot), test GET /rest/qc/control-lots/{id}, test PUT /rest/qc/control-lots/{id}/deactivate, test validation errors (400), test not found errors (404)
- [ ] T040 [P] [US6] Frontend unit test for ControlLotSetup in `frontend/src/components/qc/controlLots/ControlLotSetup.test.jsx` (Template: `.specify/templates/testing/JestComponent.test.jsx.template`) - Reference: [Testing Roadmap - Jest + React Testing Library](../../.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) - Reference: [Jest Best Practices](../../.specify/guides/jest-best-practices.md) - **Import Order**: React → Testing Library → userEvent → jest-dom → Intl → Router → Component → Utils → Messages - **userEvent PREFERRED**: Use `userEvent.type()`, `userEvent.click()` (NOT `fireEvent`) - **Async Testing**: Use `waitFor` with `queryBy*` or `findBy*` - Test form rendering, test calculation method selection, test manufacturer value input, test form submission, test validation errors
- [ ] T041 [P] [US6] Frontend unit test for StatisticsConfigModal in `frontend/src/components/qc/controlLots/StatisticsConfigModal.test.jsx` - Test modal opening/closing, test method selection (initial/rolling/manufacturer), test parameter input, test save action - Use `userEvent` and `waitFor`
- [ ] T042 [P] [US6] Cypress E2E test in `frontend/cypress/e2e/qc/controlLotSetup.cy.js` (Template: `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference: [Constitution Section V.5](../../.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices) - Reference: [Testing Roadmap - Cypress](../../.specify/guides/testing-roadmap.md#cypress-e2e-testing) - Use data-testid selectors (PREFERRED), use cy.session() for login, use cy.request() for test data setup - Test AS-001 (create lot with manufacturer fixed values - immediately active), test AS-002 (create lot with initial establishment - enters establishment status), test AS-003 (deactivate expired lot - prompts for replacement)

### Implementation for User Story 6

> **CRITICAL: Implementation tasks depend on test tasks. Tests must pass before
> proceeding to next phase checkpoint.**

- [ ] T043 [US6] Create StatisticsCalculator interface in `src/main/java/org/openelisglobal/qc/service/calculator/StatisticsCalculator.java` with methods: supports(CalculationMethod), calculate(QCControlLot, List<QCResult>)
- [ ] T044 [P] [US6] Create InitialRunsCalculator implementation in `src/main/java/org/openelisglobal/qc/service/calculator/InitialRunsCalculator.java` with @Component annotation - Calculate mean/SD from first N runs (default 20) - Annotate with @Service
- [ ] T045 [P] [US6] Create RollingCalculator implementation in `src/main/java/org/openelisglobal/qc/service/calculator/RollingCalculator.java` with @Component annotation - Calculate mean/SD using moving window - Annotate with @Service
- [ ] T046 [P] [US6] Create ManufacturerFixedCalculator implementation in `src/main/java/org/openelisglobal/qc/service/calculator/ManufacturerFixedCalculator.java` with @Component annotation - Use fixed manufacturer values - Annotate with @Service
- [ ] T047 [US6] Create QCControlLotService interface and implementation in `src/main/java/org/openelisglobal/qc/service/QCControlLotService.java` and `QCControlLotServiceImpl.java` with @Service and @Transactional - Methods: insert, update, deactivate, findByAnalyzerAndTest, validateLotDates
- [ ] T048 [US6] Create QCStatisticsService interface and implementation in `src/main/java/org/openelisglobal/qc/service/QCStatisticsService.java` and `QCStatisticsServiceImpl.java` with @Service and @Transactional - Methods: getOrCalculateStatistics, recalculateStatistics, shouldRecalculate - Inject List<StatisticsCalculator> for strategy pattern - Implement caching logic (depends on T047)
- [ ] T049 [US6] Create QCControlLotForm DTO in `src/main/java/org/openelisglobal/qc/form/QCControlLotForm.java` with validation annotations
- [ ] T050 [US6] Create QCControlLotRestController in `src/main/java/org/openelisglobal/qc/controller/QCControlLotRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/qc/control-lots") - Endpoints: POST /, GET /{id}, PUT /{id}/deactivate, GET /by-analyzer/{analyzerId} - NO @Transactional (belongs in service layer) - Services compile all data within transaction (depends on T047, T048)
- [ ] T051 [US6] Create ControlLotSetup React component in `frontend/src/components/qc/controlLots/ControlLotSetup.jsx` using Carbon Design System - Components: Form, TextInput, ComboBox, DatePicker, Button - Use React Intl for all strings - Use Formik for form management (depends on T050 for API)
- [ ] T052 [US6] Create StatisticsConfigModal React component in `frontend/src/components/qc/controlLots/StatisticsConfigModal.jsx` using Carbon ComposedModal - Components: RadioButtonGroup (method selection), NumberInput (manufacturer values), Button - Use React Intl for all strings
- [ ] T053 [US6] Add QC control lot routes to `frontend/src/App.js`: /analyzers/qc/control-lots, /analyzers/qc/control-lots/new, /analyzers/qc/control-lots/:id/edit
- [ ] T054 [US6] Add QC control lot menu items to database menu structure: MENU_QC_CONTROL_LOTS under MENU_QC parent - Update menu SQL in Liquibase changeset
- [ ] T055 [US6] Run formatting: `mvn spotless:apply` for backend, `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 6 should be fully
functional and testable independently. Lab supervisors can create control lots
with all 3 calculation methods, deactivate lots, and see establishment status.
ALL tests from T032-T042 MUST pass.

---

## Phase 4: Integration with Feature 004

**Goal**: Implement service contract for receiving QC results from feature 004's ASTM message processing.

**Priority**: P1 - Required for automatic QC result capture

**Independent Test**: Mock call to QCResultService.createQCResult() with test data, verify QCResult persisted, z-score calculated, event published, rule evaluation triggered.

**Prerequisites**: Phases 1-3 complete (QC entities, rule evaluation, dashboard), Feature 004 implementation (for end-to-end testing)

**Tasks**:

- [ ] T139 [INT] Unit test for QCResultService.createQCResult() in `src/test/java/org/openelisglobal/qc/service/QCResultServiceTest.java` - Test method accepts parameters (analyzer ID, test ID, control lot ID, control level, result value, unit, timestamp), test z-score calculation, test QCResult persistence, test QCResultCreatedEvent publishing, test validation (result within range) - Use @Mock for DAOs and ApplicationEventPublisher
- [ ] T140 [INT] Implement QCResultService.createQCResult() method in `src/main/java/org/openelisglobal/qc/service/QCResultServiceImpl.java` - Accept parameters from 004, calculate z-score using control lot statistics, persist QCResult entity, publish QCResultCreatedEvent, return created entity - Method signature: `QCResult createQCResult(String analyzerId, String testId, String controlLotId, String controlLevel, BigDecimal resultValue, String unit, LocalDateTime timestamp)` - Annotate with @Transactional
- [ ] T141 [INT] Integration test for 004-to-003 service call in `src/test/java/org/openelisglobal/qc/integration/Feature004IntegrationTest.java` (@SpringBootTest) - Test full workflow: Mock call from 004 → createQCResult() → QCResult persisted → Event published → Rule evaluation triggered - Verify async event processing completes - Use builders for test data
- [ ] T142 [INT] Update QCResultRestController manual entry endpoint in `src/main/java/org/openelisglobal/qc/controller/QCResultRestController.java` - Endpoint POST /rest/qc/results (manual entry) calls QCResultService.createQCResult() - Map form data to service method parameters - NO ASTM-specific endpoints (004 calls service layer directly, not REST API)
- [ ] T143 [INT] Cypress E2E test for manual QC result entry in `frontend/cypress/e2e/qc/manualQCEntry.cy.js` - Test manual entry form submission, verify result saved, verify rule evaluation triggered, verify alert if violation detected - Note: Automatic capture from analyzers is handled by feature 004 (not tested in 003's E2E suite)

---

## Phase 5: User Story 5 - Configure Westgard Rules (Priority: P2)

**Goal**: Enable lab managers to configure which Westgard rules are enabled and
their parameters so the system can customize QC strategy per laboratory needs.

**Why Next**: Rule configuration must be in place before rule evaluation can be
implemented.

**Independent Test**: Configure rule set for test analyzer, enable/disable specific rules, verify only enabled rules are marked for evaluation.

### Tests for User Story 5 (MANDATORY - TDD Enforcement)

- [ ] T066 [P] [US5] Unit test for WestgardRuleConfigService in `src/test/java/org/openelisglobal/qc/service/WestgardRuleConfigServiceTest.java` - Test rule enable/disable, test preset configurations (Basic/Standard/Comprehensive), test validation (at least one rejection rule required) - Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` for DAOs
- [ ] T067 [P] [US5] DAO test for WestgardRuleConfigDAO in `src/test/java/org/openelisglobal/qc/dao/WestgardRuleConfigDAOTest.java` - Test findEnabledByInstrument, test findByTestAndInstrument, test rule enable/disable persistence - Use `@DataJpaTest` and `TestEntityManager`
- [ ] T068 [P] [US5] Controller test for WestgardRuleConfigRestController in `src/test/java/org/openelisglobal/qc/controller/WestgardRuleConfigRestControllerTest.java` - Test GET /rest/qc/rule-config/{analyzerId}, test PUT /rest/qc/rule-config (update), test validation errors - Use `@WebMvcTest`, `@MockBean`, `MockMvc`
- [ ] T069 [P] [US5] Frontend unit test for RuleConfigPanel in `frontend/src/components/qc/ruleConfig/RuleConfigPanel.test.jsx` - Test rule list rendering, test enable/disable toggles, test preset selection, test validation (warning when no rejection rules enabled) - Use `userEvent` and `waitFor`
- [ ] T070 [P] [US5] Cypress E2E test in `frontend/cypress/e2e/qc/ruleConfiguration.cy.js` - Test AS-001 (view all 8 rules with enable/disable), test AS-002 (select preset - rules auto-enabled), test AS-003 (disable all rejection rules - validation error) - Use data-testid selectors, cy.session() for login

### Implementation for User Story 5

- [ ] T071 [US5] Create WestgardRuleConfigService interface and implementation in `src/main/java/org/openelisglobal/qc/service/WestgardRuleConfigService.java` and `WestgardRuleConfigServiceImpl.java` with @Service and @Transactional - Methods: findByAnalyzer, updateRuleConfig, applyPreset, validateRuleConfig
- [ ] T072 [US5] Create WestgardRuleConfigForm DTO in `src/main/java/org/openelisglobal/qc/form/WestgardRuleConfigForm.java` with validation annotations
- [ ] T073 [US5] Create WestgardRuleConfigRestController in `src/main/java/org/openelisglobal/qc/controller/WestgardRuleConfigRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/qc/rule-config") - Endpoints: GET /{analyzerId}, PUT / (update config), GET /presets (list presets) - NO @Transactional (depends on T071)
- [ ] T074 [US5] Create RuleConfigPanel React component in `frontend/src/components/qc/ruleConfig/RuleConfigPanel.jsx` using Carbon Toggle, Tile, Button - Display all 8 rules with severity indicators, enable/disable toggles, preset selection ComboBox - Use React Intl for all strings (depends on T073)
- [ ] T075 [US5] Add rule configuration route to `frontend/src/App.js`: /analyzers/qc/rule-config/:analyzerId
- [ ] T076 [US5] Run formatting: `mvn spotless:apply` for backend, `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 5 should be fully functional. Lab managers can configure which rules are enabled per analyzer, select presets, and validation prevents invalid configurations. ALL tests from T066-T070 MUST pass.

---

## Phase 6: Westgard Rule Evaluators (CRITICAL - Enables US1, US2, US3, US4)

**Goal**: Implement all 8 Westgard rule evaluation algorithms with TDD using
reference datasets.

**Why Next**: Rule evaluators are the core QC logic that powers compliance
monitoring, alerts, and corrective actions.

**Independent Test**: Each rule evaluator can be tested independently with
reference datasets from Westgard literature.

### Tests for Rule Evaluators (MANDATORY - TDD with Reference Datasets)

> **CRITICAL: Use reference datasets from Westgard literature for each rule
> test**

- [ ] T077 [P] [RULES] Unit test for Rule1_2sEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule1_2sEvaluatorTest.java` -
      Test single result exceeding +2SD (violation), test single result
      exceeding -2SD (violation), test result within ±2SD (no violation), test
      edge case (exactly 2.0 SD - should violate), test with reference dataset -
      Use `@RunWith(MockitoJUnitRunner.class)` - Verify severity = WARNING
- [ ] T078 [P] [RULES] Unit test for Rule1_3sEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule1_3sEvaluatorTest.java` -
      Test single result exceeding +3SD (violation), test single result
      exceeding -3SD (violation), test result within ±3SD (no violation), test
      edge case (exactly 3.0 SD - should violate), test with reference dataset -
      Verify severity = REJECTION
- [ ] T079 [P] [RULES] Unit test for Rule2_2sEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule2_2sEvaluatorTest.java` -
      Test two consecutive results exceeding same +2SD (violation), test two
      consecutive results exceeding same -2SD (violation), test two consecutive
      on opposite sides (no violation), test insufficient data (<2 results -
      cannot evaluate), test with reference dataset - Verify severity =
      REJECTION
- [ ] T080 [P] [RULES] Unit test for RuleR_4sEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/RuleR_4sEvaluatorTest.java` -
      Test range between consecutive results >4SD (violation), test range <4SD
      (no violation), test edge case (exactly 4.0 SD range - should violate),
      test insufficient data, test with reference dataset - Verify severity =
      REJECTION
- [ ] T081 [P] [RULES] Unit test for Rule4_1sEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule4_1sEvaluatorTest.java` -
      Test 4 consecutive results exceeding same +1SD (violation), test 4
      consecutive results exceeding same -1SD (violation), test 3 consecutive
      (no violation - need 4), test insufficient data (<4 results), test with
      reference dataset - Verify severity = REJECTION
- [ ] T082 [P] [RULES] Unit test for Rule10_xEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule10_xEvaluatorTest.java` -
      Test 10 consecutive results on same side of mean (violation), test 9
      consecutive (no violation - need 10), test alternating sides (no
      violation), test insufficient data (<10 results), test with reference
      dataset - Verify severity = REJECTION
- [ ] T083 [P] [RULES] Unit test for Rule3_1sEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule3_1sEvaluatorTest.java` -
      Test 3 consecutive results exceeding same +1SD (violation), test 3
      consecutive results exceeding same -1SD (violation), test 2 consecutive
      (no violation - need 3), test with reference dataset - Verify severity =
      WARNING
- [ ] T084 [P] [RULES] Unit test for Rule7_tEvaluator in
      `src/test/java/org/openelisglobal/qc/service/evaluator/Rule7_tEvaluatorTest.java` -
      Test 7 consecutive results showing consistent increasing trend
      (violation), test 7 consecutive results showing consistent decreasing
      trend (violation), test inconsistent trend (no violation), test
      insufficient data (<7 results), test with reference dataset - Verify
      severity = WARNING
- [ ] T085 [US8] Unit test for WestgardRuleEvaluationService in
      `src/test/java/org/openelisglobal/qc/service/WestgardRuleEvaluationServiceTest.java` -
      Test evaluateAll with multiple enabled rules, test rule filtering (only
      enabled), test sequential evaluation, test insufficient data handling
      (skip unevaluable rules), test performance (<5 seconds for full
      evaluation) - Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` for DAOs
      and evaluators
- [ ] T086 [US8] Unit test for QCResultCreatedEventListener in
      `src/test/java/org/openelisglobal/qc/event/QCResultCreatedEventListenerTest.java` -
      Test async event handling, test rule evaluation triggered, test violation
      creation for detected violations, test transaction boundaries - Use
      `@RunWith(MockitoJUnitRunner.class)`

### Implementation for Rule Evaluators

- [ ] T087 [US8] Create WestgardRuleEvaluator interface in
      `src/main/java/org/openelisglobal/qc/service/evaluator/WestgardRuleEvaluator.java`
      with methods: canEvaluate(WestgardRuleConfig), evaluate(QCResult,
      List<QCResult>, QCStatistics) returning RuleEvaluationResult
- [ ] T088 [US8] Create RuleEvaluationResult class in
      `src/main/java/org/openelisglobal/qc/service/evaluator/RuleEvaluationResult.java`
      with fields: ruleCode, isViolated, severity, affectedResults, message
- [ ] T089 [P] [US8] Create Rule1_2sEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule1_2sEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if
      abs(zScore) > 2.0 - Return severity WARNING if violated (depends on T087)
- [ ] T090 [P] [US8] Create Rule1_3sEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule1_3sEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if
      abs(zScore) > 3.0 - Return severity REJECTION if violated (depends on
      T087)
- [ ] T091 [P] [US8] Create Rule2_2sEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule2_2sEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if
      current AND previous both exceed same ±2SD - Return severity REJECTION if
      violated (depends on T087)
- [ ] T092 [P] [US8] Create RuleR_4sEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/RuleR_4sEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if
      range between consecutive results > 4SD - Return severity REJECTION if
      violated (depends on T087)
- [ ] T093 [P] [US8] Create Rule4_1sEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule4_1sEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if 4
      consecutive results exceed same ±1SD - Return severity REJECTION if
      violated (depends on T087)
- [ ] T094 [P] [US8] Create Rule10_xEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule10_xEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if
      10 consecutive results on same side of mean - Return severity REJECTION if
      violated (depends on T087)
- [ ] T095 [P] [US8] Create Rule3_1sEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule3_1sEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if 3
      consecutive results exceed same ±1SD - Return severity WARNING if violated
      (depends on T087)
- [ ] T096 [P] [US8] Create Rule7_tEvaluator in
      `src/main/java/org/openelisglobal/qc/service/evaluator/Rule7_tEvaluator.java`
      implementing WestgardRuleEvaluator with @Component - Algorithm: Check if 7
      consecutive results show consistent trend (all increasing or all
      decreasing) - Return severity WARNING if violated (depends on T087)
- [ ] T097 [US8] Create WestgardRuleEvaluationService interface and
      implementation in
      `src/main/java/org/openelisglobal/qc/service/WestgardRuleEvaluationService.java`
      and `WestgardRuleEvaluationServiceImpl.java` with @Service - Inject
      List<WestgardRuleEvaluator> via @Autowired - Method evaluateAll: Get
      enabled rules, get historical results (up to 10), get statistics, evaluate
      each rule using strategy pattern - Method evaluateAll must complete in <5
      seconds (FR-028) (depends on T089-T096)
- [ ] T098 [US8] Create QCResultCreatedEventListener in
      `src/main/java/org/openelisglobal/qc/event/QCResultCreatedEventListener.java`
      with @Component - Use @EventListener and @Async for async processing - Use
      @Transactional for evaluation workflow - Inject
      WestgardRuleEvaluationService - Handle QCResultCreatedEvent: load result,
      evaluate all rules, create violations for detected violations (depends on
      T097)
- [ ] T099 [US8] Run formatting: `mvn spotless:apply` for backend (MANDATORY
      before commit)

**Checkpoint Validation**: At this point, all 8 Westgard rule evaluators are
implemented and tested. QC results trigger automatic rule evaluation
asynchronously. Violations are detected correctly. ALL tests from T077-T086 MUST
pass with >80% coverage.

---

## Phase 7: User Story 3 - Receive Automated Alerts (Priority: P1)

**Goal**: Enable lab managers to receive automated email and system
notifications when Westgard rules are violated so they can take immediate
corrective actions.

**Why Next**: Alerts enable timely response to QC failures, critical for patient
safety.

**Independent Test**: Trigger a 1₃ₛ violation (result >3SD), verify immediate
email sent to lab managers. Trigger multiple 1₂ₛ violations within 15 min,
verify batched notification.

### Tests for User Story 3 (MANDATORY - TDD Enforcement)

- [ ] T100 [P] [US3] Unit test for QCAlertService in `src/test/java/org/openelisglobal/qc/service/QCAlertServiceTest.java` - Test immediate alert for 1₃ₛ (no batching), test batching logic (max 1 per 15 min), test alert recipient determination, test email formatting - Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` for DAOs and email service
- [ ] T101 [P] [US3] Unit test for QCRuleViolationService in `src/test/java/org/openelisglobal/qc/service/QCRuleViolationServiceTest.java` - Test violation creation with all affected results, test severity classification, test resolution status tracking, test violation-to-alert linking - Use `@RunWith(MockitoJUnitRunner.class)`
- [ ] T102 [P] [US3] DAO test for QCRuleViolationDAO in `src/test/java/org/openelisglobal/qc/dao/QCRuleViolationDAOTest.java` - Test findByAnalyzer, test findUnresolved, test findBySeverity, test violation queries with filtering - Use `@DataJpaTest` and `TestEntityManager`
- [ ] T103 [P] [US3] DAO test for QCAlertDAO in `src/test/java/org/openelisglobal/qc/dao/QCAlertDAOTest.java` - Test findMostRecentByInstrument, test findUnread, test findByRecipient, test read status tracking - Use `@DataJpaTest`
- [ ] T104 [P] [US3] Frontend unit test for AlertFeed in `frontend/src/components/qc/alerts/AlertFeed.test.jsx` - Test alert list rendering, test unread indicators, test mark as read, test alert navigation links - Use `userEvent` and `waitFor`
- [ ] T105 [P] [US3] Integration test for alert workflow in `src/test/java/org/openelisglobal/qc/QCAlertIntegrationTest.java` (@SpringBootTest with @Transactional) - Test full workflow: violation created → alert generated → email sent → alert recorded - Test batching behavior with multiple violations - Use builders for test data
- [ ] T106 [P] [US3] Cypress E2E test in `frontend/cypress/e2e/qc/qcAlerts.cy.js` - Test AS-001 (1₃ₛ violation triggers immediate email), test AS-002 (multiple 1₂ₛ violations batched), test AS-003 (system notification appears in feed), test AS-004 (click notification - navigate to analyzer detail) - Use data-testid selectors, cy.intercept() for email verification

### Implementation for User Story 3

- [ ] T107 [US3] Create QCRuleViolationService interface and implementation in `src/main/java/org/openelisglobal/qc/service/QCRuleViolationService.java` and `QCRuleViolationServiceImpl.java` with @Service and @Transactional - Methods: createViolation(RuleEvaluationResult), findByAnalyzer, findUnresolved, resolveViolation - Inject QCAlertService to trigger alerts after violation creation
- [ ] T108 [US3] Create QCAlertService interface and implementation in `src/main/java/org/openelisglobal/qc/service/QCAlertService.java` and `QCAlertServiceImpl.java` with @Service and @Transactional - Methods: sendAlertForViolation (with batching logic), sendImmediateAlert (for 1₃ₛ), sendBatchedAlert, determineRecipients - Inject EmailNotificationService for email sending - Implement batching: max 1 per analyzer per 15 min, except 1₃ₛ immediate (FR-072, FR-073) (depends on T107)
- [ ] T109 [US3] Update QCResultCreatedEventListener in `src/main/java/org/openelisglobal/qc/event/QCResultCreatedEventListener.java` - After rule evaluation, call QCRuleViolationService.createViolation for each detected violation - Violation creation triggers alert via QCAlertService (depends on T107, T108)
- [ ] T110 [US3] Create QCViolationForm DTO in `src/main/java/org/openelisglobal/qc/form/QCViolationForm.java` with validation annotations
- [ ] T111 [US3] Create QCViolationRestController in `src/main/java/org/openelisglobal/qc/controller/QCViolationRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/qc/violations") - Endpoints: GET / (list with filtering), GET /{id}, POST /{id}/acknowledge (for warnings) - NO @Transactional (depends on T107)
- [ ] T112 [US3] Create AlertFeed React component in `frontend/src/components/qc/alerts/AlertFeed.jsx` using Carbon InlineNotification, Button - Display recent violations with severity indicators, unread badges, navigation links to analyzer detail - Use React Intl for all strings - Use SWR for real-time updates every 30 seconds (depends on T111)
- [ ] T113 [US3] Add AlertFeed to QCDashboard component (placeholder component - will be implemented in US1) - Display in top-right corner of dashboard
- [ ] T114 [US3] Run formatting: `mvn spotless:apply` for backend, `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 3 should be fully
functional. Violations trigger automated alerts via email and system
notifications. Batching prevents alert fatigue. 1₃ₛ violations get immediate
alerts. ALL tests from T100-T106 MUST pass.

---

## Phase 8: User Story 1 - View Analyzer Compliance Status (Priority: P1) 🎯 MVP

**Goal**: Enable laboratory technicians to see a clear compliance overview with visual indicators so they can quickly assess which analyzers require attention.

**Why MVP**: This is the foundational user-facing capability that provides
immediate value by showing compliance at a glance.

**Independent Test**: Display dashboard with multiple analyzers showing color-coded status (green/yellow/red) based on QC data, and verify technicians can identify non-compliant analyzers within 10 seconds.

### Tests for User Story 1 (MANDATORY - TDD Enforcement)

- [ ] T115 [P] [US1] Unit test for QCDashboardService in `src/test/java/org/openelisglobal/qc/service/QCDashboardServiceTest.java` - Test compliance status calculation (green if no violations, yellow if warnings, red if rejections), test analyzer card data compilation (within transaction), test last result timestamp tracking - Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` for DAOs
- [ ] T116 [P] [US1] Controller test for QCRestController in `src/test/java/org/openelisglobal/qc/controller/QCRestControllerTest.java` - Test GET /rest/qc/dashboard (all analyzers), test GET /rest/qc/dashboard/{analyzerId} (single analyzer detail), test data compilation (no lazy loading), test response format - Use `@WebMvcTest`, `@MockBean`, `MockMvc`
- [ ] T117 [P] [US1] Frontend unit test for QCDashboard in `frontend/src/components/qc/dashboard/QCDashboard.test.jsx` - Test dashboard rendering, test analyzer card display, test auto-refresh (5 min interval), test loading states, test empty state (no analyzers) - Use `userEvent` and `waitFor`
- [ ] T118 [P] [US1] Frontend unit test for ComplianceStatusTile in `frontend/src/components/qc/dashboard/ComplianceStatusTile.test.jsx` - Test green tile (compliant), test yellow tile (warning), test red tile (non-compliant), test click navigation to detail view - Use `userEvent` for click interactions
- [ ] T119 [P] [US1] Cypress E2E test in `frontend/cypress/e2e/qc/qcDashboard.cy.js` - Test AS-001 (view dashboard with color-coded analyzer cards), test AS-002 (analyzer card shows status, triggered rules, last result time), test AS-003 (click card - navigate to detail view) - Use data-testid selectors, cy.request() for test data setup, cy.session() for login

### Implementation for User Story 1

- [ ] T120 [US1] Create QCDashboardService interface and implementation in `src/main/java/org/openelisglobal/qc/service/QCDashboardService.java` and `QCDashboardServiceImpl.java` with @Service and @Transactional - Methods: getInstrumentComplianceStatus (compile all data within transaction using JOIN FETCH), calculateComplianceColor, getRecentViolations - Prevent LazyInitializationException by eagerly fetching all data
- [ ] T121 [US1] Create QCRestController in `src/main/java/org/openelisglobal/qc/controller/QCRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/qc") - Endpoints: GET /dashboard (all analyzers), GET /dashboard/{analyzerId} (single analyzer) - NO @Transactional - Services compile all data (depends on T120)
- [ ] T122 [US1] Create ComplianceStatusTile React component in `frontend/src/components/qc/dashboard/ComplianceStatusTile.jsx` using Carbon Tile - Props: status (green/yellow/red), instrumentName, triggeredRules[], lastResultTime - Use Carbon color tokens for status colors (success, warning, danger) - Use React Intl for all strings - Clickable with navigation to detail view
- [ ] T123 [US1] Create QCDashboard React component in `frontend/src/components/qc/dashboard/QCDashboard.jsx` using Carbon Grid, Column - Display ComplianceStatusTile for each analyzer - Add AlertFeed in top-right corner - Use SWR for data fetching with 5-minute refresh interval (FR-050) - Display last update timestamp - Use React Intl for all strings (depends on T121, T122)
- [ ] T124 [US1] Add QC dashboard route to `frontend/src/App.js`: /analyzers/qc (maps to QCDashboard component)
- [ ] T125 [US1] Add QC dashboard menu item to database menu structure: MENU_QC under MENU_ANALYZERS parent with route /analyzers/qc - Update menu SQL in Liquibase changeset
- [ ] T126 [US1] Run formatting: `mvn spotless:apply` for backend, `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 1 should be fully functional and testable independently. Technicians can view compliance dashboard, identify non-compliant analyzers by color within 10 seconds, and navigate to detail views. Dashboard auto-refreshes every 5 minutes. ALL tests from T115-T119 MUST pass. **THIS IS THE MVP - CAN DEPLOY/DEMO NOW.**

---

## Phase 9: User Story 2 - Monitor QC Data with Control Charts (Priority: P1)

**Goal**: Enable laboratory technicians to view interactive Levey-Jennings
charts with Westgard rule overlays so they can visually identify patterns and
violations in QC data.

**Why Next**: Control charts are essential diagnostic tools for understanding QC
violations and determining corrective actions.

**Independent Test**: Display Levey-Jennings chart for single analyzer with plotted QC results, SD lines (±1SD, ±2SD, ±3SD), and highlighted violation points.

### Tests for User Story 2 (MANDATORY - TDD Enforcement)

- [ ] T127 [P] [US2] Controller test for QCChartDataRestController in `src/test/java/org/openelisglobal/qc/controller/QCChartDataRestControllerTest.java` - Test GET /rest/qc/charts/{analyzerId} (chart data with filtering), test date range filtering, test control level filtering, test data compilation (all relationships fetched) - Use `@WebMvcTest`, `@MockBean`, `MockMvc`
- [ ] T128 [P] [US2] Frontend unit test for LeveyJenningsChart in `frontend/src/components/qc/charts/LeveyJenningsChart.test.jsx` - Test chart rendering with Carbon LineChart, test SD reference lines (mean, ±1SD, ±2SD, ±3SD), test violation point highlighting (color, size), test tooltip content (value, z-score, date, violations) - Mock @carbon/charts-react - Use `waitFor` for async chart rendering
- [ ] T129 [P] [US2] Frontend unit test for ControlChartDetail in `frontend/src/components/qc/charts/ControlChartDetail.test.jsx` - Test date range filters, test control level filters, test zoom/pan functionality, test chart export button - Use `userEvent` for filter interactions
- [ ] T130 [P] [US2] Cypress E2E test in `frontend/cypress/e2e/qc/controlCharts.cy.js` - Test AS-001 (view chart with QC values, SD limits displayed), test AS-002 (violation points highlighted with distinct colors), test AS-003 (hover data point - tooltip shows value, z-score, violations), test AS-004 (apply date range filter - chart updates) - Use data-testid selectors, cy.request() for chart data setup

### Implementation for User Story 2

- [ ] T131 [US2] Create QCChartDataRestController in `src/main/java/org/openelisglobal/qc/controller/QCChartDataRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/qc/charts") - Endpoints: GET /{analyzerId} (with date range, control level filters), GET /{analyzerId}/statistics (mean, SD for reference lines) - Use JOIN FETCH to compile all data (results + violations + statistics) - NO @Transactional - Services compile data (depends on T120 QCDashboardService for data compilation pattern)
- [ ] T132 [US2] Create LeveyJenningsChart React component in `frontend/src/components/qc/charts/LeveyJenningsChart.jsx` using @carbon/charts-react LineChart - Configure chart options: SD reference lines (grid.y with mean, ±1/2/3 SD), point styling (color red for violations, size 6 for violations vs 4 normal), tooltip customHTML (value, z-score, date, violations) - Props: chartData[], statistics (mean, sd) - Use React Intl for axis labels
- [ ] T133 [US2] Create ControlChartDetail React component in `frontend/src/components/qc/charts/ControlChartDetail.jsx` using Carbon DatePicker, ComboBox, Button - Display LeveyJenningsChart with date range and control level filters - Add export button (download chart as PNG) - Use SWR for data fetching with filter params - Use React Intl for all strings (depends on T131, T132)
- [ ] T134 [US2] Add control chart route to `frontend/src/App.js`: /analyzers/qc/charts/:analyzerId
- [ ] T135 [US2] Update ComplianceStatusTile click handler to navigate to /analyzers/qc/charts/:analyzerId (link dashboard to chart detail view)
- [ ] T136 [US2] Run formatting: `mvn spotless:apply` for backend, `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 2 should be fully
functional. Technicians can view Levey-Jennings charts with SD lines, see
violation points highlighted, hover for details, filter by date/level, and
export charts. ALL tests from T127-T130 MUST pass.

---

## Phase 10: User Story 4 - Manage Corrective Actions (Priority: P2)

**Goal**: Enable laboratory supervisors to log corrective actions for QC
violations and assign follow-up tasks so they maintain a complete audit trail
and ensure violations are properly resolved.

**Why Next**: Corrective action management is required for regulatory compliance
and operational closure of QC issues.

**Independent Test**: Create corrective action for known violation, assign to
user, track status changes, verify violation resolved when action completed.

### Tests for User Story 4 (MANDATORY - TDD Enforcement)

- [ ] T137 [P] [US4] Unit test for QCCorrectiveActionService in
      `src/test/java/org/openelisglobal/qc/service/QCCorrectiveActionServiceTest.java` -
      Test corrective action state machine (PENDING → ASSIGNED → IN_PROGRESS →
      COMPLETED → RESOLVED), test validation guards (cannot complete without
      resolution notes, cannot assign to non-BIOLOGIST), test violation
      resolution on action completion, test patient result release blocking -
      Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` for DAOs
- [ ] T138 [P] [US4] DAO test for QCCorrectiveActionDAO in
      `src/test/java/org/openelisglobal/qc/dao/QCCorrectiveActionDAOTest.java` -
      Test findByViolation, test findByAssignedUser, test findByStatus, test
      state transition persistence - Use `@DataJpaTest` and `TestEntityManager`
- [ ] T139 [P] [US4] Controller test for QCCorrectiveActionRestController in
      `src/test/java/org/openelisglobal/qc/controller/QCCorrectiveActionRestControllerTest.java` -
      Test POST /rest/qc/corrective-actions (create), test PUT /{id}/assign,
      test PUT /{id}/start, test PUT /{id}/complete, test validation errors -
      Use `@WebMvcTest`, `@MockBean`, `MockMvc`
- [ ] T140 [P] [US4] Frontend unit test for CorrectiveActionForm in
      `frontend/src/components/qc/correctiveActions/CorrectiveActionForm.test.jsx` -
      Test action type selection (Recalibration, Maintenance, Repeat Control,
      Reagent Change, Other), test user assignment, test description input, test
      form submission - Use `userEvent` and `waitFor`
- [ ] T141 [P] [US4] Frontend unit test for CorrectiveActionList in
      `frontend/src/components/qc/correctiveActions/CorrectiveActionList.test.jsx` -
      Test task list rendering, test status filtering, test mark as in progress,
      test mark as completed (with resolution notes modal) - Use `userEvent` for
      interactions
- [ ] T142 [P] [US4] Cypress E2E test in
      `frontend/cypress/e2e/qc/correctiveActions.cy.js` - Test AS-001 (create
      corrective action with type selection, description, user assignment), test
      AS-002 (view task list with pending actions), test AS-003 (complete action
      with resolution notes - violation auto-resolved), test AS-004 (patient
      results held until violation resolved) - Use data-testid selectors,
      cy.request() for test data

### Implementation for User Story 4

- [ ] T143 [US4] Create QCCorrectiveActionService interface and implementation
      in
      `src/main/java/org/openelisglobal/qc/service/QCCorrectiveActionService.java`
      and `QCCorrectiveActionServiceImpl.java` with @Service and
      @Transactional - Methods: createAction, assignAction, startAction,
      completeAction (with state machine validation), findByUser, findByStatus -
      State machine: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED → RESOLVED -
      Validation guards: cannot complete without resolution notes, cannot assign
      to non-BIOLOGIST user, cannot skip states - On completion: annotate QC
      result with corrective action summary, resolve linked violation - Inject
      QCRuleViolationService to resolve violations
- [ ] T144 [US4] Create QCCorrectiveActionForm DTO in
      `src/main/java/org/openelisglobal/qc/form/QCCorrectiveActionForm.java`
      with validation annotations (actionType, description required;
      resolutionNotes required for completion)
- [ ] T145 [US4] Create QCCorrectiveActionRestController in
      `src/main/java/org/openelisglobal/qc/controller/QCCorrectiveActionRestController.java`
      extending BaseRestController with @RestController and
      @RequestMapping("/rest/qc/corrective-actions") - Endpoints: POST /
      (create), GET /{id}, GET /by-user/{userId}, PUT /{id}/assign, PUT
      /{id}/start, PUT /{id}/complete - NO @Transactional (depends on T143)
- [ ] T146 [US4] Create CorrectiveActionForm React component in
      `frontend/src/components/qc/correctiveActions/CorrectiveActionForm.jsx`
      using Carbon ComboBox (action type), TextArea (description), ComboBox
      (user assignment), Button - Use Formik for form management, Yup for
      validation - Use React Intl for all strings (depends on T145)
- [ ] T147 [US4] Create CorrectiveActionList React component in
      `frontend/src/components/qc/correctiveActions/CorrectiveActionList.jsx`
      using Carbon DataTable, Tag (status indicators), OverflowMenu (row
      actions: start, complete) - Display pending actions with priority, due
      status - Modal for completion with resolution notes TextArea - Use SWR for
      data fetching - Use React Intl for all strings (depends on T145)
- [ ] T148 [US4] Add corrective action routes to `frontend/src/App.js`:
      /analyzers/qc/corrective-actions (list),
      /analyzers/qc/corrective-actions/new (create form)
- [ ] T149 [US4] Add corrective action menu item to database menu structure:
      MENU_QC_ACTIONS under MENU_QC parent - Update menu SQL in Liquibase
      changeset
- [ ] T150 [US4] Update QCViolationRestController to add endpoint POST
      /{id}/create-corrective-action (create action from violation detail view)
- [ ] T151 [US4] Run formatting: `mvn spotless:apply` for backend,
      `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 4 should be fully
functional. Supervisors can create corrective actions for violations, assign to
users, track status through workflow, and violations are auto-resolved on
completion. Patient results are held until violations resolved. ALL tests from
T137-T142 MUST pass.

---

## Phase 11: User Story 7 - Analyze Compliance Trends (Priority: P3)

**Goal**: Enable laboratory managers to view trend graphs showing compliance
over time so they can identify recurring issues, track improvements, and prepare
for regulatory audits.

**Why Next**: Trend analysis provides strategic insights for quality
improvement. Implemented after core operational features.

**Independent Test**: Generate trend reports for date range showing violation frequency by analyzer, rule type, and severity, with filtering and export capabilities.

### Tests for User Story 7 (MANDATORY - TDD Enforcement)

- [ ] T152 [P] [US7] Unit test for QCTrendAnalysisService in `src/test/java/org/openelisglobal/qc/service/QCTrendAnalysisServiceTest.java` - Test compliance percentage calculation over time, test violation frequency distribution, test recurring violation detection, test mean time to resolution calculation - Use `@RunWith(MockitoJUnitRunner.class)`, `@Mock` for DAOs
- [ ] T153 [P] [US7] Controller test for QCTrendAnalysisRestController in `src/test/java/org/openelisglobal/qc/controller/QCTrendAnalysisRestControllerTest.java` - Test GET /rest/qc/trends/compliance (compliance over time), test GET /rest/qc/trends/violations (violation distribution), test filtering (date range, analyzer, test, rule type, severity) - Use `@WebMvcTest`, `@MockBean`, `MockMvc`
- [ ] T154 [P] [US7] Frontend unit test for ComplianceTrendChart in `frontend/src/components/qc/trends/ComplianceTrendChart.test.jsx` - Test trend graph rendering, test filtering options, test export functionality - Use `waitFor` for async chart rendering
- [ ] T155 [P] [US7] Frontend unit test for ViolationDistributionChart in `frontend/src/components/qc/trends/ViolationDistributionChart.test.jsx` - Test violation frequency bar chart, test rule type breakdown, test instrument comparison - Mock @carbon/charts-react
- [ ] T156 [P] [US7] Cypress E2E test in `frontend/cypress/e2e/qc/trendAnalysis.cy.js` - Test AS-001 (view compliance percentage over time with date range filter), test AS-002 (view violation frequency distribution by rule type), test AS-003 (export compliance report - PDF download within 30s), test AS-004 (compare multiple analyzers - identify highest violation rates) - Use data-testid selectors, cy.intercept() for export

### Implementation for User Story 7

- [ ] T157 [US7] Create QCTrendAnalysisService interface and implementation in `src/main/java/org/openelisglobal/qc/service/QCTrendAnalysisService.java` and `QCTrendAnalysisServiceImpl.java` with @Service - Methods: calculateComplianceTrend (compliance % over time), getViolationFrequencyDistribution (by rule type, analyzer, time period), identifyRecurringViolations (configurable thresholds), calculateMeanTimeToResolution - Use complex HQL queries with aggregations and date grouping
- [ ] T158 [US7] Create QCTrendAnalysisRestController in `src/main/java/org/openelisglobal/qc/controller/QCTrendAnalysisRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/qc/trends") - Endpoints: GET /compliance (with date range, analyzer, test filters), GET /violations (violation distribution), GET /recurring (analyzers with recurring violations), GET /export (PDF report generation) - NO @Transactional (depends on T157)
- [ ] T159 [US7] Create ComplianceTrendChart React component in `frontend/src/components/qc/trends/ComplianceTrendChart.jsx` using @carbon/charts-react LineChart - Display compliance percentage over time, date range filters, analyzer/test filters - Use SWR for data fetching - Use React Intl for all strings (depends on T158)
- [ ] T160 [US7] Create ViolationDistributionChart React component in `frontend/src/components/qc/trends/ViolationDistributionChart.jsx` using @carbon/charts-react BarChart - Display violation frequency by rule type, analyzer comparison, severity breakdown - Use React Intl for all strings (depends on T158)
- [ ] T161 [US7] Create TrendAnalysisDashboard React component in `frontend/src/components/qc/trends/TrendAnalysisDashboard.jsx` - Combine ComplianceTrendChart and ViolationDistributionChart, add export button (download PDF report) - Use Carbon Grid for layout - Use React Intl for all strings (depends on T159, T160)
- [ ] T162 [US7] Add trend analysis route to `frontend/src/App.js`: /analyzers/qc/trends
- [ ] T163 [US7] Add trend analysis menu item to database menu structure: MENU_QC_TRENDS under MENU_QC parent - Update menu SQL in Liquibase changeset
- [ ] T164 [US7] Run formatting: `mvn spotless:apply` for backend, `npm run format` for frontend (MANDATORY before commit)

**Checkpoint Validation**: At this point, User Story 7 should be fully
functional. Managers can view compliance trends, violation distributions,
identify recurring issues, and export reports for audits. ALL tests from
T152-T156 MUST pass.

---

## Phase 12: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T165 [P] Add comprehensive JavaDoc comments to all public methods in service layer (`src/main/java/org/openelisglobal/qc/service/`)
- [ ] T166 [P] Add JSDoc comments to all exported React components (`frontend/src/components/qc/`)
- [ ] T167 [P] Create quickstart.md in `specs/003-westgard-qc/quickstart.md` with step-by-step developer guide (local setup, database migration, test execution, troubleshooting)
- [ ] T168 [P] Create data-model.md in `specs/003-westgard-qc/data-model.md` with entity relationship diagrams, table schemas, relationship cardinalities
- [ ] T169 Performance optimization: Add database indexes for QC result queries - Index on (control_lot_id, run_date_time) in qc_result table - Index on (analyzer_id, created_at) in qc_rule_violation table - Update Liquibase changeset
- [ ] T170 Security hardening: Add input validation for all QC result entry endpoints - Validate result values within reasonable range (mean ± 10 SD) - Prevent SQL injection in HQL queries (use parameterized queries) - Add CSRF protection for state-changing endpoints
- [ ] T171 [P] Add monitoring metrics for QC evaluation performance - Analyzer rule evaluation duration (track p95 latency) - Alert generation latency (track time from violation to email sent) - Dashboard data retrieval time - Add logging to QC services for performance tracking
- [ ] T172 [P] Update OpenAPI contracts in `specs/003-westgard-qc/contracts/` with actual endpoint documentation (qc-api.yaml, violation-api.yaml, corrective-action-api.yaml)
- [ ] T173 Run full test suite verification - Backend: `mvn test && mvn verify` (verify JaCoCo >80% coverage) - Frontend: `npm test -- --coverage` (verify Jest >70% coverage) - E2E: Run all Cypress tests individually: `npm run cy:run -- --spec "cypress/e2e/qc/*.cy.js"`

---

## Phase 13: Constitution Compliance Verification (OpenELIS Global 3.0)

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

- [ ] T174 **Configuration-Driven**: Verify no country-specific code branches
      introduced - Audit all service classes for hardcoded logic - Confirm rule
      configurations are database-driven (westgard_rule_config table) - Confirm
      calculation methods are configurable (not hardcoded)
- [ ] T175 **Carbon Design System**: Audit UI - confirm @carbon/react used
      exclusively (NO Bootstrap/Tailwind) - Search frontend codebase for
      prohibited imports:
      `grep -r "bootstrap\|tailwind" frontend/src/components/qc/` - Verify all
      components use Carbon: Tile, DataTable, LineChart, ComposedModal, Tag,
      OverflowMenu, Button, TextInput, ComboBox, DatePicker
- [ ] T176 **FHIR/IHE Compliance**: QC results are internal quality control and
      do not require FHIR exposure per Constitution III - No FHIR implementation
      required for this feature - Confirm no fhir_uuid columns added to QC
      entities (not patient-facing data)
- [ ] T177 **Layered Architecture**: Verify 5-layer pattern followed
      (Valueholder→DAO→Service→Controller→Form) - Audit: Confirm NO controllers
      call DAOs directly (must go through services) - Audit: Confirm NO business
      logic in DAOs (only CRUD + HQL queries) - Audit: Confirm NO native SQL in
      Java code (all HQL) - Audit: Confirm NO class-level variables in
      controllers (thread safety violation) - Audit: Confirm NO @Transactional
      in controllers (must be in services only) - Audit: Confirm services
      compile all data within transaction (JOIN FETCH used, no lazy loading in
      controllers)
- [ ] T178 **Test Coverage**: Run coverage report - confirm >80% backend, >70%
      frontend - Backend: `mvn verify` → Check `target/site/jacoco/index.html`
      for >80% - Frontend: `npm test -- --coverage` → Check coverage report
      for >70% - Critical paths MUST have 100% coverage: All 8 rule evaluators,
      statistics calculators, alert batching, corrective action state machine
- [ ] T179 **Schema Management**: Verify ALL database changes use Liquibase
      changesets (NO direct SQL) - Audit: Confirm all tables created via
      Liquibase XML in `src/main/resources/liquibase/qc/` - Audit: Confirm all 5
      changesets have rollback scripts - Verify: Run `mvn liquibase:rollback` to
      test rollback functionality
- [ ] T180 **Internationalization**: Audit UI strings - confirm React Intl used
      for ALL text (no hardcoded strings) - Search for hardcoded strings:
      `grep -r '"[A-Z]' frontend/src/components/qc/` (should only find intl
      message IDs) - Verify: All en.json keys have corresponding fr.json
      translations - Verify: Date/time formatting uses intl.formatDate(),
      intl.formatTime() - Verify: Number formatting uses intl.formatNumber()
- [ ] T181 **Security & Compliance**: Verify RBAC, audit trail (sys_user_id +
      lastupdated), input validation - Audit: Confirm all QC entities extend
      BaseObject<String> (provides sys_user_id + lastupdated) - Audit: Confirm
      role checks in controllers (Results, Biologist, Global Admin roles
      enforced) - Audit: Confirm input validation on all forms (Hibernate
      Validator annotations, Formik validation) - Audit: Confirm audit trail
      captures all violations, corrective actions, status changes - Verify: Test
      patient result release blocking when unresolved violations exist

**Verification Commands**:

```bash
# Backend: Code formatting (MUST run before each commit) + build + tests
mvn spotless:apply && mvn spotless:check && mvn clean install

# Frontend: Formatting (MUST run before each commit) + E2E tests
cd frontend && npm run format
# Run E2E tests individually (per Constitution V.5):
npm run cy:run -- --spec "cypress/e2e/qc/qcDashboard.cy.js"
npm run cy:run -- --spec "cypress/e2e/qc/controlCharts.cy.js"
npm run cy:run -- --spec "cypress/e2e/qc/qcAlerts.cy.js"
npm run cy:run -- --spec "cypress/e2e/qc/correctiveActions.cy.js"
npm run cy:run -- --spec "cypress/e2e/qc/controlLotSetup.cy.js"
npm run cy:run -- --spec "cypress/e2e/qc/ruleConfiguration.cy.js"
npm run cy:run -- --spec "cypress/e2e/qc/trendAnalysis.cy.js"
# Full suite only in CI/CD: npm run cy:run

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 6 (Phase 3)**: Depends on Foundational - FOUNDATIONAL for other stories (control lots required)
- **User Story 8 (Phase 4)**: Depends on US6 (control lots must exist) - Enables automated result capture
- **User Story 5 (Phase 5)**: Depends on Foundational - Rule configuration required before evaluation
- **Rule Evaluators (Phase 6)**: Depends on US5, Phase 4 - CRITICAL, enables US1, US2, US3, US4
- **User Story 3 (Phase 7)**: Depends on Rule Evaluators - Alerts require violations
- **User Story 1 (Phase 8)**: Depends on Rule Evaluators, US3 - Dashboard shows compliance + alerts 🎯 **MVP**
- **User Story 2 (Phase 9)**: Depends on US1, Rule Evaluators - Charts extend dashboard
- **User Story 4 (Phase 10)**: Depends on US3 - Corrective actions address violations
- **User Story 7 (Phase 11)**: Depends on all previous stories - Trend analysis requires historical data
- **Polish (Phase 12)**: Depends on all desired user stories being complete
- **Constitution Verification (Phase 13)**: Final validation

### Critical Path (MVP)

**Minimum Viable Product** = User Story 1 (View Compliance Dashboard)

1. Phase 1: Setup
2. Phase 2: Foundational (entities, DAOs, Liquibase, ORM validation)
3. Phase 3: User Story 6 (Control Lots - required for QC results)
4. Phase 4: Integration with Feature 004 (service contract for QC result reception)
5. Phase 5: User Story 5 (Rule Configuration - required for evaluation)
6. Phase 6: Rule Evaluators (8 rules + evaluation service - CRITICAL)
7. Phase 7: User Story 3 (Alerts - violation notification)
8. Phase 8: User Story 1 (Dashboard - **MVP COMPLETE** 🎯)

**At this point, core QC functionality is operational and valuable to users.**

### User Story Dependencies

- **US6 (Control Lots)**: Independent - Can start after Foundational
- **Phase 4 (Feature 004 Integration)**: Depends on US6 (control lots must exist for QC results)
- **US5 (Rule Config)**: Independent - Can start after Foundational
- **Rule Evaluators**: Depend on US5, Phase 4 (need config + results from 004)
- **US3 (Alerts)**: Depends on Rule Evaluators (violations trigger alerts)
- **US1 (Dashboard)**: Depends on Rule Evaluators, US3 (displays compliance +
  alerts) 🎯 **MVP**
- **US2 (Charts)**: Depends on US1, Rule Evaluators (extends dashboard with
  charts)
- **US4 (Corrective Actions)**: Depends on US3 (actions resolve violations)
- **US7 (Trends)**: Depends on all previous (requires historical data)

### Within Each User Story

- Tests (TDD) MUST be written and FAIL before implementation
- Entities before DAOs
- DAOs before services
- Services before controllers
- Controllers before frontend components
- Core implementation before integration
- Story complete and tests passing before moving to next priority

### Parallel Opportunities

- **Phase 1 (Setup)**: All tasks (T001-T006) can run in parallel
- **Phase 2 (Foundational)**: Liquibase tasks (T007-T011) can run in parallel,
  entities (T012-T018) can run in parallel after Liquibase, DAOs (T020-T026) can
  run in parallel after entities, builders (T027-T031) can run in parallel
- **Within User Stories**: All test tasks marked [P] can run in parallel
  (different test files), entity/model tasks marked [P] can run in parallel
- **User Stories in Parallel** (if team capacity):
  - After Phase 6 (Rule Evaluators) complete: US1, US2, US4 can proceed in
    parallel
  - US3 must complete before US1 (alerts integrated into dashboard)

---

## Parallel Example: User Story 6 (Control Lots)

```bash
# Launch all tests for User Story 6 together (TDD - write these first):
T032: Unit test for QCControlLotService
T033: Unit test for QCStatisticsService
T034: Unit test for InitialRunsCalculator
T035: Unit test for RollingCalculator
T036: Unit test for ManufacturerFixedCalculator
T037: DAO test for QCControlLotDAO
T038: DAO test for QCStatisticsDAO
T039: Controller test for QCControlLotRestController
T040: Frontend unit test for ControlLotSetup
T041: Frontend unit test for StatisticsConfigModal
T042: Cypress E2E test for controlLotSetup

# After tests written and failing (RED), launch parallel implementation:
T044: Create InitialRunsCalculator
T045: Create RollingCalculator
T046: Create ManufacturerFixedCalculator
# Then sequentially:
T043: Create StatisticsCalculator interface (blocking for calculators)
T047: Create QCControlLotService (blocking for T048)
T048: Create QCStatisticsService (depends on T047)
T049: Create QCControlLotForm
T050: Create QCControlLotRestController (depends on T047, T048)
# Then frontend in parallel:
T051: Create ControlLotSetup component
T052: Create StatisticsConfigModal component
```

---

## Implementation Strategy

### MVP First (User Stories 6 + 8 + 5 + Rule Evaluators + 3 + 1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 6 (Control Lots - foundational)
4. Complete Phase 4: Feature 004 Integration (service contract for QC results)
5. Complete Phase 5: User Story 5 (Rule Configuration)
6. Complete Phase 6: Rule Evaluators (8 rules - CRITICAL)
7. Complete Phase 7: User Story 3 (Alerts)
8. Complete Phase 8: User Story 1 (Dashboard) 🎯 **MVP COMPLETE**
9. **STOP and VALIDATE**: Test User Story 1 independently - technicians can view compliance dashboard, identify non-compliant analyzers, receive alerts
10. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US6 (Control Lots) → Test independently → Lab can set up QC control lots
3. Add Phase 4 (Feature 004 Integration) → Test independently → QC result service contract working
4. Add US5 (Rule Config) → Test independently → Lab can configure rules per analyzer
5. Add Rule Evaluators → Test independently → QC results automatically evaluated
6. Add US3 (Alerts) → Test independently → Violations trigger automated alerts
7. Add US1 (Dashboard) → Test independently → **MVP!** Dashboard shows
   compliance status
8. Add US2 (Charts) → Test independently → Levey-Jennings charts available
9. Add US4 (Corrective Actions) → Test independently → Corrective action
   workflow operational
10. Add US7 (Trends) → Test independently → Trend analysis for audits
11. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Developer A: User Story 6 (Control Lots)
3. Developer B: Phase 4 (Feature 004 Integration) - can start after US6 entities created
4. Developer C: User Story 5 (Rule Configuration) - independent
5. After US5, Phase 4 complete: Developer A: Rule Evaluators (critical path)
6. After Rule Evaluators: Developer B: US3 (Alerts), Developer C: US1 (Dashboard)
7. After US1: Developer A: US2 (Charts), Developer B: US4 (Corrective Actions), Developer C: US7 (Trends)
8. Stories complete and integrate independently

---

## Notes

- **[P] tasks** = different files, no dependencies, can run in parallel
- **[Story] label** (US1, US2, etc.) maps task to specific user story for
  traceability
- **Each user story** should be independently completable and testable
- **TDD Workflow**: Verify tests FAIL before implementing (RED), then implement
  (GREEN), then refactor
- **Commit frequently**: After each task or logical group
- **Stop at checkpoints**: Validate story independently before proceeding
- **Avoid**: Vague tasks, same file conflicts, cross-story dependencies that break independence
- **MVP Strategy**: Implement critical path (US6 → Phase 4 → US5 → Rules → US3 → US1) to deliver value quickly
- **Constitution Compliance**: Verify all 8 principles at Phase 13 before declaring feature complete

---

**Total Tasks**: 181 tasks

**Tasks by Phase**:

- Phase 1 (Setup): 6 tasks
- Phase 2 (Foundational): 25 tasks
- Phase 3 (US6 - Control Lots): 23 tasks
- Phase 4 (Feature 004 Integration): 5 tasks
- Phase 5 (US5 - Rule Config): 11 tasks
- Phase 6 (Rule Evaluators): 23 tasks
- Phase 7 (US3 - Alerts): 15 tasks
- Phase 8 (US1 - Dashboard): 12 tasks
- Phase 9 (US2 - Charts): 10 tasks
- Phase 10 (US4 - Corrective Actions): 15 tasks
- Phase 11 (US7 - Trends): 13 tasks
- Phase 12 (Polish): 9 tasks
- Phase 13 (Constitution): 8 tasks

**Test Tasks**: 77 tasks (42.5% of total - strong TDD emphasis)

**Parallel Opportunities**: 98 tasks marked [P] (54% can run in parallel within
constraints)

**MVP Scope**: Phases 1-8 (92 tasks) = Core QC compliance monitoring with
dashboard, alerts, automated evaluation

**Suggested Execution**: Follow critical path for MVP (Phases 1-8), then add
US2, US4, US7 incrementally based on priority.
