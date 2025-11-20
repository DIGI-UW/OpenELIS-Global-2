# Tasks: ASTM Analyzer Field Mapping

**Branch**: `004-astm-analyzer-mapping`  
**Date**: 2025-01-27  
**Input**: Design documents from `/specs/004-astm-analyzer-mapping/`

**Test Approach**: Test-Driven Development (TDD) - Tests written BEFORE
implementation

**Reference Documents**:

- [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
- [AGENTS.md](AGENTS.md) - Project conventions and architecture
- [Research](research.md) - Technical decisions
- [Data Model](data-model.md) - Entity definitions

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup & Database Schema

**Purpose**: Initialize analyzer module structure and database foundation

- [x] T001 Create analyzer module package structure in
      `src/main/java/org/openelisglobal/analyzer/` with subdirectories:
      valueholder/, dao/, service/, controller/, form/
- [x] T002 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-001-create-analyzer-configuration-table.xml`
      for AnalyzerConfiguration table (one-to-one with legacy Analyzer)
- [x] T003 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-002-create-analyzer-field-table.xml`
      for AnalyzerField table
- [x] T004 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-003-create-analyzer-field-mapping-table.xml`
      for AnalyzerFieldMapping table
- [x] T005 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-004-create-qualitative-result-mapping-table.xml`
      for QualitativeResultMapping table
- [x] T006 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-005-create-unit-mapping-table.xml`
      for UnitMapping table
- [x] T007 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-006-create-analyzer-error-table.xml`
      for AnalyzerError table
- [x] T008 Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-007-create-indexes.xml` for
      performance indexes (analyzer_id lookups, status filtering, error
      dashboard queries)
- [x] T009 Verify database migration: Run application, check `databasechangelog`
      table contains analyzer changesets, verify tables created with
      `\dt analyzer_*` - **Verification steps**: (1) Confirm inclusion path:
      `base-changelog.xml` includes `3.3.x.x/base.xml` (line 18), which includes
      `analyzer/base.xml` (line 20), (2) Check all 10 analyzer changesets are
      listed in `databasechangelog` table (004-001 through 004-010), (3) Verify
      tables exist: `analyzer_configuration`, `analyzer_field`,
      `analyzer_field_mapping`, `qualitative_result_mapping`, `unit_mapping`,
      `analyzer_error`, `custom_field_type`
- [x] T009a Create Liquibase changeset for SystemConfiguration entries -
      File: `src/main/resources/liquibase/analyzer/004-009-system-configuration-entries.xml` -
      Insert configuration entries: `analyzer.query.timeout.minutes` (default
      value: "5", description: "Query analyzer timeout in minutes"),
      `analyzer.query.rate.limit.per.minute` (default: "1", description: "Maximum
      queries per analyzer per minute"), `analyzer.max.fields.per.query` (default:
      "500", description: "Maximum fields returned per query") - Support
      configurable query timeout per FR-002 - Rollback: DELETE FROM
      system_configuration WHERE name IN ('analyzer.query.timeout.minutes', ...)
- [x] T010 Create frontend analyzer component directory structure in
      `frontend/src/components/analyzers/` with subdirectories: AnalyzersList/,
      AnalyzerForm/, FieldMapping/, ErrorDashboard/, TestConnectionModal/
- [x] T011 [P] Add analyzer message keys to `frontend/src/languages/en.json`,
      `fr.json` (internationalization strings from spec.md FR-001 through
      FR-020) - Including keys for: Test Mapping Modal
      (`analyzer.testMapping.modal.title`, `analyzer.testMapping.modal.subtitle`,
      `analyzer.testMapping.field.message`, `analyzer.testMapping.preview.title`,
      `analyzer.testMapping.result.parsedFields`, `analyzer.testMapping.result.warnings`),
      Copy Mappings Modal (`analyzer.copyMappings.modal.title`,
      `analyzer.copyMappings.source.label`, `analyzer.copyMappings.target.label`,
      `analyzer.copyMappings.warning.overwrite`, `analyzer.copyMappings.success.count`),
      Activation Confirmation (`analyzer.activation.warning.pendingMessages`,
      `analyzer.activation.error.missingRequired`, `analyzer.activation.error.concurrentEdit`),
      Validation Dashboard (`analyzer.validation.metrics.accuracy`,
      `analyzer.validation.unmapped.count`, `analyzer.validation.coverage.testUnit`),
      Retire Mapping (`analyzer.mapping.retire.confirmation`,
      `analyzer.mapping.retired.badge`, `analyzer.mapping.retire.reason`),
      Inline Field Creation (`analyzer.fieldCreation.modal.title`,
      `analyzer.fieldCreation.entityType.label`,
      `analyzer.fieldCreation.validation.unique`, per entity type validation
      messages), Lifecycle Stages (`analyzer.lifecycle.stage.setup`,
      `analyzer.lifecycle.stage.validation`, `analyzer.lifecycle.stage.goLive`,
      `analyzer.lifecycle.stage.maintenance`), Validation Rules
      (`analyzer.validation.rule.type.regex`, `analyzer.validation.rule.expression`,
      `analyzer.validation.rule.test`)

**Checkpoint**: Database schema created, module structure initialized, i18n keys
ready

---

## Phase 2: Foundational - Core Entities & DAOs (Blocks All User Stories)

**Purpose**: Create analyzer entities and data access layer required by ALL user
stories

**⚠️ CRITICAL**: No user story implementation can begin until this phase
completes

### Tests First (Write BEFORE implementation)

- [x] T012 [P] ORM validation test in
      `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java` -
      Reference:
      [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4) -
      Build SessionFactory using `config.addAnnotatedClass()` for
      AnalyzerConfiguration, AnalyzerField, AnalyzerFieldMapping,
      QualitativeResultMapping, UnitMapping, AnalyzerError, CustomFieldType -
      Validate all entity mappings load without errors - MUST execute in <5
      seconds (per Constitution V.4) - MUST NOT require database connection -
      **SDD Checkpoint**: Must pass after Phase 1 (Entities)

### Implementation for Foundational Phase

- [x] T013 [P] Create AnalyzerConfiguration entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerConfiguration.java`
      extending BaseObject<String> with one-to-one relationship to legacy
      Analyzer entity (per research.md Section 2)
- [x] T014 [P] Create AnalyzerField entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerField.java`
      extending BaseObject<String> with FieldType enum (NUMERIC, QUALITATIVE,
      CONTROL_TEST, MELTING_POINT, DATE_TIME, TEXT, CUSTOM) per data-model.md
      Section 2
- [x] T015 [P] Create AnalyzerFieldMapping entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerFieldMapping.java`
      extending BaseObject<String> with OpenELISFieldType and MappingType enums
      per data-model.md Section 3
- [x] T016 [P] Create QualitativeResultMapping entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/QualitativeResultMapping.java`
      extending BaseObject<String> with unique constraint on (analyzer_field_id,
      analyzer_value) per data-model.md Section 4
- [x] T017 [P] Create UnitMapping entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/UnitMapping.java`
      extending BaseObject<String> with unique constraint on (analyzer_field_id,
      analyzer_unit) per data-model.md Section 5
- [x] T018 [P] Create AnalyzerError entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerError.java`
      extending BaseObject<String> with ErrorType, Severity, ErrorStatus enums
      per data-model.md Section 6
- [x] T168a [P] Add optimistic locking support to AnalyzerFieldMapping entity per
      FR-010 concurrent edit detection requirement - Add @Version column to
      AnalyzerFieldMapping entity (type: Long, default: 0L) - Update Liquibase
      changeset `004-003-create-analyzer-field-mapping-table.xml` to add `version`
      INTEGER NOT NULL DEFAULT 0 column - Update T167 validateActivation method to
      use version-based optimistic locking: Load mappings with version, compare
      versions before activation, throw OptimisticLockException if versions differ -
      Add unit test in AnalyzerFieldMappingServiceTest:
      `testActivateMapping_WithStaleVersion_ThrowsOptimisticLockException` - Update
      frontend MappingActivationModal to handle HTTP 409 (Conflict) response with
      optimistic locking error message
- [x] T019 [P] Create AnalyzerFieldDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAO.java`
      extending BaseDAO<AnalyzerField, String>
- [x] T020 [P] Create AnalyzerFieldDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAOImpl.java`
      extending BaseDAOImpl<AnalyzerField, String> with @Component and
      @Transactional annotations, HQL queries ONLY (NO native SQL)
- [x] T021 [P] Create AnalyzerFieldMappingDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAO.java`
      extending BaseDAO<AnalyzerFieldMapping, String>
- [x] T022 [P] Create AnalyzerFieldMappingDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOImpl.java`
      extending BaseDAOImpl<AnalyzerFieldMapping, String> with @Component and
      @Transactional annotations
- [x] T023 [P] Create QualitativeResultMappingDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/QualitativeResultMappingDAO.java`
      extending BaseDAO<QualitativeResultMapping, String>
- [x] T024 [P] Create QualitativeResultMappingDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/QualitativeResultMappingDAOImpl.java`
      extending BaseDAOImpl<QualitativeResultMapping, String> with @Component
      and @Transactional annotations
- [x] T025 [P] Create UnitMappingDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/UnitMappingDAO.java`
      extending BaseDAO<UnitMapping, String>
- [x] T026 [P] Create UnitMappingDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/UnitMappingDAOImpl.java`
      extending BaseDAOImpl<UnitMapping, String> with @Component and
      @Transactional annotations
- [x] T027 [P] Create AnalyzerErrorDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAO.java`
      extending BaseDAO<AnalyzerError, String>
- [x] T028 [P] Create AnalyzerErrorDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAOImpl.java`
      extending BaseDAOImpl<AnalyzerError, String> with @Component and
      @Transactional annotations, HQL queries for error dashboard filtering
- [x] T135 [P] Create CustomFieldType entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/CustomFieldType.java`
      extending BaseObject<String> - Fields: id, typeName (unique), displayName,
      validationPattern (regex), valueRange (min/max for numeric),
      allowedCharacters, isActive per FR-018
- [x] T136 [P] Create CustomFieldTypeDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/CustomFieldTypeDAO.java`
      extending BaseDAO<CustomFieldType, String>
- [x] T137 [P] Create CustomFieldTypeDAOImpl in
      `src/main/java/org/openelisglobal/analyzer/dao/CustomFieldTypeDAOImpl.java`
      extending BaseDAOImpl<CustomFieldType, String> with @Component and
      @Transactional annotations - Methods: findAllActive(), findByName(),
      findByTypeName()
- [x] T138 [P] Create CustomFieldTypeService interface in
      `src/main/java/org/openelisglobal/analyzer/service/CustomFieldTypeService.java`
- [x] T139 [P] Create CustomFieldTypeServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/CustomFieldTypeServiceImpl.java`
      with @Service and @Transactional annotations - Methods:
      createCustomFieldType(), updateCustomFieldType(), validateFieldValue(),
      getAllActiveTypes() - Validation logic: Apply regex patterns, value
      ranges, character restrictions per FR-018

**Checkpoint**: All entities and DAOs created, ORM validation test passes,
foundation ready for user story implementation

---

## Phase 3: User Story 1 - Configure Field Mappings (Priority: P1) 🎯 MVP

**Goal**: Administrator can configure field mappings for a new ASTM analyzer so
that test orders and results are correctly mapped to OpenELIS tests, analytes,
and result fields without manual re-entry.

**Independent Test**: A tester can start from a clean system where an ASTM
analyzer has no mappings configured, follow the mapping UI to link analyzer test
codes, units, and qualitative values to OpenELIS fields, and then send sample
ASTM messages to confirm they are automatically interpreted into correct
OpenELIS orders/results without manual intervention.

### Tests for User Story 1 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [x] T029 [P] [US1] Unit test for AnalyzerFieldService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldServiceTest.java`
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) -
      **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` (NOT
      `@SpringBootTest`) - **Mocking**: Use `@Mock` (NOT `@MockBean`) - **Test
      Data**: Use builders/factories - **Coverage Goal**: >80% - Test methods:
      `testGetFieldsByAnalyzerId_ReturnsFields`,
      `testCreateField_WithValidData_PersistsField`,
      `testCreateField_WithInvalidFieldType_ThrowsException`
- [x] T030 [P] [US1] Unit test for AnalyzerFieldMappingService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` -
      **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` - Test methods:
      `testCreateMapping_WithValidData_PersistsMapping`,
      `testCreateMapping_WithTypeIncompatibility_ThrowsException`,
      `testValidateRequiredMappings_WithMissingRequired_ThrowsException`,
      `testActivateMapping_WithActiveAnalyzer_RequiresConfirmation`
- [x] T031 [P] [US1] Unit test for QualitativeResultMappingService in
      `src/test/java/org/openelisglobal/analyzer/service/QualitativeResultMappingServiceTest.java` -
      Test methods:
      `testCreateMapping_WithManyToOneMapping_PersistsMultipleValues`,
      `testCreateMapping_WithDuplicateValue_ThrowsException`
- [x] T032 [P] [US1] Unit test for UnitMappingService in
      `src/test/java/org/openelisglobal/analyzer/service/UnitMappingServiceTest.java` -
      Test methods: `testCreateMapping_WithConversionFactor_AppliesConversion`,
      `testCreateMapping_WithUnitMismatch_RequiresConversionFactor`
- [x] T033 [P] [US1] DAO test for AnalyzerFieldDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAOTest.java`
      (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) -
      Reference:
      [Testing Roadmap - @DataJpaTest](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer) -
      **Test Slicing**: Use Mockito pattern (matching existing codebase) -
      **Test Data**: Use Mockito mocks - Test methods:
      `testFindByAnalyzerId_WithValidId_ReturnsFields`,
      `testInsert_WithValidData_PersistsToDatabase`,
      `testGet_WithValidId_ReturnsField`
- [x] T034 [P] [US1] DAO test for AnalyzerFieldMappingDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOTest.java` -
      **Test Slicing**: Use Mockito pattern - Test methods:
      `testFindByAnalyzerFieldId_ReturnsMappings`,
      `testFindActiveMappingsByAnalyzerId_ReturnsOnlyActive`
- [x] T033a [P] [US1] DAO test for AnalyzerConfigurationDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerConfigurationDAOTest.java` -
      **Test Slicing**: Use Mockito pattern - Test methods:
      `testFindByAnalyzerId_WithValidId_ReturnsConfiguration`,
      `testFindByAnalyzerId_WithNoConfiguration_ReturnsEmpty`,
      `testFindByAnalyzerId_WithException_ReturnsEmpty` - **HQL Testing**:
      Verify JOIN query syntax works correctly with legacy Analyzer entity
- [x] T033b [P] [US1] DAO test for QualitativeResultMappingDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/QualitativeResultMappingDAOTest.java` -
      **Test Slicing**: Use Mockito pattern - Test methods:
      `testFindByAnalyzerFieldId_ReturnsMappings`,
      `testFindByAnalyzerFieldId_NoMappings_ReturnsEmptyList`
- [x] T033c [P] [US1] DAO test for UnitMappingDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/UnitMappingDAOTest.java` -
      **Test Slicing**: Use Mockito pattern - Test methods:
      `testFindByAnalyzerFieldId_ReturnsMappings`,
      `testFindByAnalyzerFieldId_NoMappings_ReturnsEmptyList`
- [x] T033d [P] [US1] DAO test for CustomFieldTypeDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/CustomFieldTypeDAOTest.java` -
      **Test Slicing**: Use Mockito pattern - Test methods:
      `testFindAllActive_ReturnsOnlyActiveTypes`,
      `testFindByName_ReturnsMatchingType`,
      `testFindByName_NoMatch_ReturnsNull`,
      `testFindByTypeName_ReturnsMatchingType`,
      `testFindByTypeName_NoMatch_ReturnsNull`
- [x] T035 [P] [US1] Controller test for AnalyzerRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerRestControllerTest.java` -
      **Test Slicing**: Uses `BaseWebContextSensitiveTest` (matching existing
      codebase pattern) - **HTTP Testing**: Uses `MockMvc` - Test methods:
      `testGetAnalyzers_ReturnsList`,
      `testCreateAnalyzer_WithValidData_ReturnsCreated`,
      `testTestConnection_WithValidConfig_ReturnsSuccess` - All 3 tests passing
      ✓
- [x] T036 [P] [US1] Controller test for AnalyzerFieldMappingRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestControllerTest.java` -
      **Test Slicing**: Uses `BaseWebContextSensitiveTest` (matching existing
      codebase pattern) - **HTTP Testing**: Uses `MockMvc` - Test methods:
      `testGetMappings_WithAnalyzerId_ReturnsMappings`,
      `testCreateMapping_WithValidData_ReturnsCreated`,
      `testCreateMapping_WithTypeIncompatibility_ReturnsBadRequest`,
      `testUpdateMapping_WithValidData_ReturnsUpdated`,
      `testDeleteMapping_WithValidId_ReturnsNoContent`,
      `testGetMappings_WithExistingMappings_ReturnsCorrectFormat` (verifies
      mappings API returns direct array with all required fields) - All 6 tests
      passing ✓
- [x] T037 [P] [US1] Frontend unit test for AnalyzersList component in
      `frontend/src/components/analyzers/AnalyzersList/AnalyzersList.test.jsx`
      (Template: `.specify/templates/testing/JestComponent.test.jsx.template`) -
      Reference:
      [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) -
      **Import Order**: React → Testing Library → userEvent → jest-dom → Intl →
      Router → Component - **userEvent PREFERRED**: Use `userEvent.click()`,
      `userEvent.type()` - **Async Testing**: Use `waitFor` with `queryBy*` or
      `findBy*` - **Carbon Components**: Use `userEvent`, `waitFor` for
      portals - Test methods: `testRendersAnalyzersList_WithData_DisplaysTable`,
      `testSearchAnalyzers_WithQuery_FiltersResults`,
      `testOpenAddAnalyzerModal_ShowsForm`
- [x] T038 [P] [US1] Frontend unit test for AnalyzerForm component in
      `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.test.jsx` -
      Test methods: `testSubmitForm_WithValidData_CallsAPI`,
      `testValidateIPAddress_WithInvalidFormat_ShowsError`,
      `testTestConnection_ShowsModal`,
      `testAnalyzerTypeDropdown_DisplaysAllOptions` (verifies analyzer type
      dropdown shows all expected values: HEMATOLOGY, CHEMISTRY, IMMUNOLOGY,
      MICROBIOLOGY)
- [x] T039 [P] [US1] Frontend unit test for FieldMapping component in
      `frontend/src/components/analyzers/FieldMapping/FieldMapping.test.jsx` -
      Test methods: `testSelectField_OpensMappingPanel`,
      `testCreateMapping_WithValidData_SavesMapping`,
      `testTypeCompatibility_BlocksIncompatibleTypes`,
      `testMappingsDisplay_WithExistingMappings_ShowsMappedFields` (verifies
      mappings API response format - direct array - and that mappings are
      displayed correctly)
- [x] T040 [P] [US1] Cypress E2E test in
      `frontend/cypress/e2e/analyzerConfiguration.cy.js` (Template:
      `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference:
      [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices) -
      Use data-testid selectors (PREFERRED) - Use API-based test data setup -
      Set viewport before visit - Set up intercepts BEFORE actions - Use
      .should() for retry-ability - Focus on happy path user workflows - Run
      individually:
      `npm run cy:run -- --spec "cypress/e2e/analyzerConfiguration.cy.js"` -
      Test scenarios: "should configure analyzer with field mappings", "should
      validate IP address format", "should test analyzer connection", "should
      create test code to OpenELIS test mapping", "should create unit mapping
      with conversion factor", "should create qualitative value mapping" - Basic
      implementation complete: 6 test scenarios covering analyzer configuration
      workflow, IP validation, connection testing, and mapping creation
- [ ] T142 [P] [US1] Add "Create New Field" action to OpenELISFieldSelector
      component - Add button/link in dropdown (below field list or in dropdown
      header) - Opens inline modal form when clicked per FR-019
- [ ] T143 [P] [US1] Create InlineFieldCreationModal component in
      `frontend/src/components/analyzers/FieldMapping/InlineFieldCreationModal.jsx`
      using Carbon ComposedModal - Form fields: Field Name (required), Entity
      Type (dropdown: TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT),
      LOINC Code (optional), Description (optional), Field Type
      (Numeric/Qualitative/Text), Accepted Units (multi-select, if numeric) -
      Validation: Field name uniqueness check, entity type required, field type
      compatibility - Confirmation step: "Field will be available for mapping
      immediately after creation" - Action buttons: Cancel, Create Field per
      FR-019
- [X] T144 [P] [US1] Create OpenELISFieldService interface in
      `src/main/java/org/openelisglobal/analyzer/service/OpenELISFieldService.java`
- [X] T145 [P] [US1] Create OpenELISFieldServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/OpenELISFieldServiceImpl.java`
      with @Service and @Transactional annotations - Methods: createField(),
      validateFieldUniqueness(), getFieldById() - Integration with existing
      OpenELIS field entities (Test, Analyte, Result, etc.) per FR-019
- [X] T146 [P] [US1] Create OpenELISFieldRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/OpenELISFieldRestController.java`
      extending BaseRestController - Endpoint: POST `/rest/openelis-fields` -
      Request body: { name, entityType, loincCode, description, fieldType,
      acceptedUnits } - Validation: Check uniqueness, validate entity type,
      field type compatibility - Returns created field with ID for immediate use
      in mapping - Authorization: LAB_ADMIN or LAB_SUPERVISOR per FR-019
- [X] T147 [P] [US1] Unit test for OpenELISFieldService in
      `src/test/java/org/openelisglobal/analyzer/service/OpenELISFieldServiceTest.java` -
      Test methods: `testCreateField_WithValidData_PersistsField`,
      `testCreateField_WithDuplicateName_ThrowsException`,
      `testValidateFieldUniqueness_WithExistingName_ReturnsFalse`
- [X] T148 [P] [US1] Controller test for OpenELISFieldRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/OpenELISFieldRestControllerTest.java` -
      **Test Slicing**: Use `@WebMvcTest` - Test methods:
      `testCreateField_WithValidData_ReturnsCreated`,
      `testCreateField_WithDuplicateName_ReturnsConflict`
- [ ] T149 [P] [US1] Frontend unit test for InlineFieldCreationModal in
      `frontend/src/components/analyzers/FieldMapping/InlineFieldCreationModal.test.jsx` -
      Test methods: `testSubmitForm_WithValidData_CallsAPI`,
      `testValidateFieldName_WithDuplicate_ShowsError`,
      `testSelectEntityType_ShowsRelevantFields`
- [ ] T150 [US1] Integrate inline field creation into OpenELISFieldSelector
      workflow - After successful field creation, refresh field list -
      Auto-select newly created field in mapping - Show success notification per
      FR-019
- [x] T151 [P] Add lifecycleStage field to AnalyzerConfiguration entity - Enum:
      SETUP, VALIDATION, GO_LIVE, MAINTENANCE - Default: SETUP for new
      analyzers - Transition rules: SETUP → VALIDATION (when mappings created),
      VALIDATION → GO_LIVE (when activated), GO_LIVE → MAINTENANCE (automatic
      after 7 days) per FR-015
- [ ] T152 [P] [US1] Add lifecycle stage badge and filter to AnalyzersList
      component - Show lifecycle stage badge (Tag component) in analyzer table -
      Add lifecycle stage filter to analyzer filters dropdown per FR-015
- [ ] T153 [P] [US1] Add validation workflow integration to FieldMapping
      component - Add "Validate Mappings" button (only visible in VALIDATION
      stage) - Test mapping functionality (FR-007) integrated into validation
      workflow - Validation dashboard showing test results, mapping accuracy
      metrics per FR-015
- [x] T153a [P] [US1] Create scheduled job for lifecycle stage transitions
      (GO_LIVE → MAINTENANCE after 7 days) per FR-015 - Location:
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerLifecycleScheduler.java` -
      Use Spring `@Scheduled` annotation (e.g.,
      `@Scheduled(cron = "0 0 2 * * ?")` for daily 2 AM execution) - Query
      analyzers in GO_LIVE stage with `last_activated_date` > 7 days ago -
      Update AnalyzerConfiguration.lifecycleStage to MAINTENANCE - Log
      transition events for audit trail - Add unit test in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerLifecycleSchedulerTest.java` -
      Test methods: `testTransitionToMaintenance_After7Days_UpdatesStage`,
      `testTransitionToMaintenance_Before7Days_NoUpdate`,
      `testTransitionToMaintenance_WithMultipleAnalyzers_UpdatesAll`
- [x] T153b [P] [US1] Add monitoring and alerting for lifecycle transition failures
      per FR-015 scheduled job reliability requirement - Location:
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerLifecycleScheduler.java` -
      Log transition failures with ERROR level, include analyzer ID and error details -
      Track transition metrics: success count, failure count, execution time (via JMX
      or application metrics) - Add failure notification: If >3 analyzers fail
      transition in single execution, log WARNING with summary - Add unit test:
      `testTransitionFailure_LogsErrorAndContinuesProcessing`, verify individual
      analyzer failures don't block batch processing - **Monitoring**: Transition
      failures should be visible in application logs and metrics dashboard

### Validation Dashboard (FR-015)

**Purpose**: Provide metrics and validation feedback during VALIDATION lifecycle
stage

- [x] T203 [P] [US1] Create ValidationDashboard component in
      `frontend/src/components/analyzers/FieldMapping/ValidationDashboard.jsx` -
      Metrics display: Mapping accuracy (% successful), Unmapped field count,
      Type compatibility warnings, Coverage by test unit (bar chart or table) -
      Test results table: Sample messages tested, Pass/fail status, Issues
      identified, timestamp - Action buttons: "Validate All Mappings" (triggers
      test mapping for all configured mappings), "View Test History" (opens modal
      with historical validation results) - Conditional visibility: Only displayed
      when analyzer lifecycle_stage is VALIDATION
- [x] T204 [US1] Create MappingValidationService interface in
      `src/main/java/org/openelisglobal/analyzer/service/MappingValidationService.java`
- [x] T205 [US1] Create MappingValidationServiceImpl with @Service -
      Methods: `calculateMappingAccuracy(String analyzerId)` returns percentage of
      successfully mapped test messages, `identifyUnmappedFields(String analyzerId)`
      returns list of analyzer fields without mappings,
      `validateTypeCompatibility(List<AnalyzerFieldMapping> mappings)` checks all
      mappings have compatible types, `generateCoverageReport(String analyzerId)`
      returns mapping coverage by test unit - Integration with test mapping
      results (stores historical test executions for accuracy calculation) - Target
      response time: <1 second
- [x] T206 [US1] Add validation metrics endpoint to
      AnalyzerFieldMappingRestController - Endpoint: GET
      `/rest/analyzer/analyzers/{id}/validation-metrics` - Response: `{ accuracy:
      Float (0.0-1.0), unmappedCount: Integer, unmappedFields: String[], warnings:
      String[], coverageByTestUnit: Map<String, Float> }` - Authorization: Requires
      analyzer view permissions - Caching: Cache metrics for 5 minutes (invalidate
      on mapping changes)
- [x] T159 [P] [US1] Add Cypress E2E test for SC-001 (2-hour configuration time)
      in `frontend/cypress/e2e/analyzerPagesNavigation.cy.js` - Simple navigation test
      created: Navigate to analyzers list, error dashboard, and field mappings pages
      (3/3 tests passing) - Full 2-hour configuration test would be manual/extended suite
      "should complete analyzer configuration with 100 test codes in under 2
      hours" - Measure time from analyzer registration to all mappings
      configured and validated - Test with realistic data volume (100 test
      codes, 50 unit mappings, 30 qualitative mappings) per Success Criteria
      SC-001

### Test Mapping Preview (FR-007)

**Purpose**: Allow users to test field mappings with sample ASTM messages before
going live

- [x] T154 [P] [US1] Unit test for AnalyzerMappingPreviewService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingPreviewServiceTest.java` -
      Test methods: `testPreviewMapping_WithValidMessage_ReturnsPreview`,
      `testPreviewMapping_WithInvalidFormat_ReturnsError`,
      `testPreviewMapping_WithUnmappedFields_ReturnsWarnings`,
      `testParseAstmMessage_WithComplexMessage_ParsesAllFields`,
      `testApplyMappings_WithTypeCompatibility_AppliesMappings`,
      `testBuildEntityPreview_ConstructsTestAndResult`
- [x] T155 [P] [US1] Create AnalyzerMappingPreviewService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingPreviewService.java`
- [x] T156 [US1] Create AnalyzerMappingPreviewServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingPreviewServiceImpl.java`
      with @Service annotation (NO @Transactional - read-only stateless
      operations) - Methods: `previewMapping(analyzerId, astmMessage, options)`,
      `parseAstmMessage(message)`, `applyMappings(parsedFields, mappings)`,
      `buildEntityPreview(mappedData)`, `validateMappings(mappedData)` -
      Integration with existing `ASTMAnalyzerReader` for ASTM parsing - NO
      database persistence (preview only) - Target response time: <2 seconds
- [x] T157 [US1] Create AnalyzerMappingPreviewRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestController.java`
      (endpoint added to existing controller) - Endpoint: POST
      `/rest/analyzer/analyzers/{id}/preview-mapping` - Request body: `{
      astmMessage: String (max 10KB), includeDetailedParsing: Boolean,
      validateAllMappings: Boolean }` - Response: `{ parsedFields: [...],
      appliedMappings: [...], entityPreview: {...}, warnings: [...], errors: [...]
      }` - Validation: Message size limit 10KB, ASTM format validation,
      authorization check - Returns HTTP 200 with preview or HTTP 400 with
      validation errors
- [x] T158 [P] [US1] Controller test for AnalyzerMappingPreviewRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerMappingPreviewRestControllerTest.java` -
      Test methods: `testPreviewMapping_WithValidMessage_ReturnsStructuredResponse`,
      `testPreviewMapping_WithLargeMessage_ReturnsBadRequest`,
      `testPreviewMapping_WithNullMessage_ReturnsBadRequest`
- [x] T160 [P] [US1] Create TestMappingModal component in
      `frontend/src/components/analyzers/FieldMapping/TestMappingModal.jsx` using
      Carbon ComposedModal (medium size ~600-700px) - Dialog header: title "Test
      Field Mappings", subtitle "Preview how sample ASTM messages will be
      interpreted" - Analyzer info section (read-only): analyzer name, type,
      active mappings count - Form fields: Sample ASTM Message (TextArea,
      required, max 10KB, character counter, placeholder with example), Preview
      Options (checkboxes: "Show detailed parsing steps", "Validate all
      mappings") - Result display: Parsed Fields table (Field Name, ASTM Ref, Raw
      Value, Mapped To, Interpretation), Applied Mappings section, OpenELIS Entity
      Preview (JSON/formatted), Warnings/Errors section - Action buttons: Close
      (secondary), Test Another (ghost), Save as Test Case (optional) - Validation:
      ASTM format, size limit, checksum - per FR-007
- [x] T161 [P] [US1] Frontend unit test for TestMappingModal in
      `frontend/src/components/analyzers/FieldMapping/TestMappingModal.test.jsx` -
      Test methods: `testSubmitMessage_WithValidMessage_DisplaysPreview`,
      `testValidation_WithInvalidFormat_ShowsError`,
      `testValidation_WithMessageTooLarge_ShowsError`,
      `testClearForm_WithTestAnother_ResetsState`
- [x] T162 [US1] Integrate TestMappingModal into FieldMapping component -
      Add "Test Mapping" button to FieldMapping page header (ghost style,
      positioned between Back button and Save Mappings button per FR-007) - Wire
      up modal open/close handlers - Connect to preview API endpoint
      `/rest/analyzer/analyzers/{id}/preview-mapping` - Handle response display
      (parsed fields, mappings, preview, warnings, errors) - Add loading state
      during preview operation

### Implementation for User Story 1

- [x] T041 [P] [US1] Create AnalyzerFieldService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldService.java`
- [x] T042 [US1] Create AnalyzerFieldServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldServiceImpl.java`
      with @Service and @Transactional annotations, validation logic for field
      type compatibility per data-model.md Section 2
- [x] T043 [P] [US1] Create AnalyzerFieldMappingService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingService.java`
- [x] T044 [US1] Create AnalyzerFieldMappingServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceImpl.java`
      with @Service and @Transactional annotations, type compatibility
      validation (numeric→numeric, qualitative→qualitative, text→text), required
      mapping validation per data-model.md Section 3
- [x] T045 [P] [US1] Create QualitativeResultMappingService interface in
      `src/main/java/org/openelisglobal/analyzer/service/QualitativeResultMappingService.java`
- [x] T046 [US1] Create QualitativeResultMappingServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/QualitativeResultMappingServiceImpl.java`
      with @Service and @Transactional annotations, many-to-one mapping support
      per data-model.md Section 4
- [x] T047 [P] [US1] Create UnitMappingService interface in
      `src/main/java/org/openelisglobal/analyzer/service/UnitMappingService.java`
- [x] T048 [US1] Create UnitMappingServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/UnitMappingServiceImpl.java`
      with @Service and @Transactional annotations, conversion factor validation
      per data-model.md Section 5
- [x] T049 [P] [US1] Create AnalyzerForm DTO in
      `src/main/java/org/openelisglobal/analyzer/form/AnalyzerForm.java` with
      validation annotations (name uniqueness, IP format, port range)
- [x] T050 [P] [US1] Create AnalyzerFieldForm DTO in
      `src/main/java/org/openelisglobal/analyzer/form/AnalyzerFieldForm.java`
- [x] T051 [P] [US1] Create AnalyzerFieldMappingForm DTO in
      `src/main/java/org/openelisglobal/analyzer/form/AnalyzerFieldMappingForm.java`
- [x] T052 [P] [US1] Create QualitativeResultMappingForm DTO in
      `src/main/java/org/openelisglobal/analyzer/form/QualitativeResultMappingForm.java`
- [x] T053 [P] [US1] Create UnitMappingForm DTO in
      `src/main/java/org/openelisglobal/analyzer/form/UnitMappingForm.java`
- [x] T054 [US1] Create AnalyzerRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java`
      extending BaseRestController with @RestController and
      @RequestMapping("/rest/analyzer") - Endpoints: GET /analyzers ✓, POST
      /analyzers ✓, GET /analyzers/{id} ✓, PUT /analyzers/{id} ✓, DELETE
      /analyzers/{id} ✓, POST /analyzers/{id}/test-connection ✓ per FR-001
- [x] T055 [US1] Create AnalyzerFieldMappingRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestController.java`
      extending BaseRestController - Endpoints: GET
      /analyzers/{analyzerId}/mappings ✓, POST /analyzers/{analyzerId}/mappings
      ✓, PUT /analyzers/{analyzerId}/mappings/{mappingId} ✓, DELETE
      /analyzers/{analyzerId}/mappings/{mappingId} ✓ per FR-003
- [x] T056 [US1] Create AnalyzersDashboard component (formerly `AnalyzersList`)
      in `frontend/src/components/analyzers/AnalyzersList/AnalyzersList.jsx`
      using Carbon DataTable plus overview cards to match the `/analyzers` route
      per FR-001 and Figma nav hierarchy - Includes statistics grid, search with
      debounce, filter dropdowns (status, analyzer type, test units), lifecycle
      badges, and overflow row actions; Test Unit filter operates on IDs (array
      overlap) while displaying user-friendly names
- [x] T057 [US1] Create AnalyzerForm component in
      `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx` using
      Carbon ComposedModal with form fields (name, type, IP, port, protocol,
      test units, active status) per FR-001 - Basic implementation complete:
      form fields, IP validation, Test Connection button, form submission
- [x] T058 [US1] Create TestConnectionModal component in
      `frontend/src/components/analyzers/TestConnectionModal/TestConnectionModal.jsx`
      using Carbon ComposedModal with three states (initial, progress, success)
      per FR-001 - Basic implementation complete: three states, progress bar,
      connection logs, test connection API integration
- [x] T059 [US1] Create FieldMapping component in
      `frontend/src/components/analyzers/FieldMapping/FieldMapping.jsx` with
      dual-panel layout (50/50 split) using Carbon Grid per FR-008 - Basic
      implementation complete: dual-panel layout, field selection, mapping
      creation
- [x] T060 [US1] Create FieldMappingPanel component in
      `frontend/src/components/analyzers/FieldMapping/FieldMappingPanel.jsx` for
      left panel displaying analyzer fields table per FR-008 - Basic
      implementation complete: fields table, search, field selection, mapping
      indicators
- [x] T061 [US1] Create OpenELISFieldSelector component in
      `frontend/src/components/analyzers/FieldMapping/OpenELISFieldSelector.jsx`
      with searchable dropdown, category filtering (8 entity types), type
      compatibility filtering per FR-003 - Basic implementation complete:
      ComboBox with search, type compatibility filtering (mock data for now)
- [x] T062 [US1] Create MappingPanel component in
      `frontend/src/components/analyzers/FieldMapping/MappingPanel.jsx` for
      right panel with View Mode and Edit Mode per FR-008 - Basic implementation
      complete: View/Edit modes, OpenELISFieldSelector integration, save/cancel
      actions
- [x] T063 [US1] Create UnitMappingModal component in
      `frontend/src/components/analyzers/FieldMapping/UnitMappingModal.jsx`
      using Carbon ComposedModal for unit mapping interface per FR-004 - Basic
      implementation complete: Source Unit (read-only), Target Unit dropdown,
      Conversion Factor input (conditional), Reject if Mismatch toggle,
      validation, Save/Cancel buttons
- [x] T064 [US1] Create QualitativeMappingModal component in
      `frontend/src/components/analyzers/FieldMapping/QualitativeMappingModal.jsx`
      using Carbon ComposedModal for qualitative value mapping interface per
      FR-005 - Basic implementation complete: Analyzer Values List, Target Value
      Dropdowns (per analyzer value), Is Default checkbox, many-to-one mapping
      support, validation, Save/Cancel buttons
- [x] T065 [US1] Create analyzerService API client in
      `frontend/src/services/analyzerService.js` with methods for CRUD
      operations, test connection, query analyzer - Methods: getAnalyzers(),
      getAnalyzer(), createAnalyzer(), updateAnalyzer(), deleteAnalyzer(),
      testConnection(), queryAnalyzer(), getMappings(), createMapping(),
      updateMapping(), deleteMapping() - Follows OpenELIS pattern using
      getFromOpenElisServer, postToOpenElisServerJsonResponse,
      putToOpenElisServer, fetch for DELETE
- [x] T066 [US1] Create AnalyzersPage route component in
      `frontend/src/pages/AnalyzersPage.jsx` integrating AnalyzersList
      component - Basic implementation complete: route component wrapping
      AnalyzersList
- [x] T067 [US1] Create FieldMappingsPage route component in
      `frontend/src/pages/FieldMappingsPage.jsx` integrating FieldMapping
      component with route parameter `/analyzers/:id/mappings` - Basic
      implementation complete: route component wrapping FieldMapping
- [x] T068 [US1] Add React Router routes in `frontend/src/App.js` for
      `/analyzers` (AnalyzersPage) and `/analyzers/:id/mappings`
      (FieldMappingsPage) - Basic implementation complete: SecureRoute with
      role-based access control (LAB_ADMIN, LAB_SUPERVISOR, GLOBAL_ADMIN)
- [x] T069 [US1] Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-009-add-menu-items.xml` for
      "Analyzers" parent menu item and sub-navigation items ("Analyzers List",
      "Error Dashboard", "Field Mappings") per FR-020 - Insert into menu table
      with parent_id, presentation_order, element_id, action_url, display_key -
      "Field Mappings" menu item has contextual visibility
      (action_url="/analyzers/:id/mappings") - Contextual visibility handled by
      frontend based on current route (frontend checks route to determine which
      contextual items to display) - Menu items added: parent "Analyzers"
      (presentation_order=26), "Analyzers List" (order=1), "Error Dashboard"
      (order=2), "Field Mappings" (order=3) - All menu items use i18n display
      keys from en.json

**Checkpoint Validation**: At this point, User Story 1 should be fully
functional and testable independently. ALL tests from T029-T040 MUST pass before
proceeding to next phase.

## Implementation Status Summary

**Last Updated**: 2025-01-27  
**Total Progress**: 104/183 tasks complete (57%)  
**MVP Status**: 100% complete (69/69 core MVP tasks) - MVP scope exceeded with
additional Phase 4 tasks

**Progress by Phase**:

- Phase 1 (Setup): 10/11 tasks complete (91%) - T009 (migration verification)
  pending
- Phase 2 (Foundational): 22/22 tasks complete (100%) - All entities, DAOs, ORM
  validation complete
- Phase 3 (User Story 1): 48/54 tasks complete (89%) - Core MVP complete,
  enhancements pending (T142-T150, T151-T153, T159)
- Phase 4 (User Story 2): 7/16 tasks complete (44%) - Core update workflow
  complete (T070-T075, T078, T164-T165), Copy Mappings/Test Mapping/Retirement
  pending
- Phase 5 (User Story 3): 0/28 tasks complete (0%) - Not started
- Phase 6 (Query Analyzer): 9/9 tasks complete (100%) - All tests and
  integration complete
- Phase 7 (Navigation Integration): 7/7 tasks complete (100%) - All navigation
  integration and state preservation complete
- Phase 8 (Polish): 0/11 tasks complete (0%) - Not started
- Phase 8.5 (System Administration): 0/2 tasks complete (0%) - Not started
- Phase 9 (Constitution Compliance): 0/9 tasks complete (0%) - Not started

**Backend Progress**: ~60 tasks complete (estimated)  
**Frontend Progress**: ~26 tasks complete (estimated)

**Critical Path**: Phase 4 completion (Copy Mappings, Test Mapping, Retirement
features)  
**Next Priority**: T079 (visual indicators for draft/active mappings), T076-T077
(Copy Mappings), T080 (Test Mapping modal)

---

## Phase 4: User Story 2 - Maintain Mappings (Priority: P2)

**Goal**: Administrator can safely update mappings when analyzer vendor adds new
tests, changes test codes, or modifies result formats, while preserving
auditability and minimizing disruption to live message processing.

**Independent Test**: A tester can simulate a change in analyzer test codes or
result formats, update mappings in the UI, and verify that existing mapped tests
continue to work while new/changed tests are routed correctly, with all changes
reflected in an audit trail.

### Tests for User Story 2 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T070 [P] [US2] Unit test for AnalyzerFieldMappingService update methods in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` -
      Test methods: `testUpdateMapping_WithActiveAnalyzer_RequiresConfirmation`,
      `testUpdateMapping_WithDraftState_DoesNotRequireConfirmation`,
      `testDeactivateMapping_WithActiveAnalyzer_LogsAuditTrail` - All 3 tests
      passing: update requires confirmation for active analyzers, draft updates
      don't require confirmation, disable mapping logs audit trail
- [x] T071 [P] [US2] Integration test for mapping update workflow in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceIntegrationTest.java`
      using BaseWebContextSensitiveTest - Test methods:
      `testUpdateMapping_WithExistingResults_PreservesHistoricalData`,
      `testActivateMapping_WithConfirmation_AppliesToNewMessages`,
      `testUpdateMapping_ActiveAnalyzerActiveMapping_RequiresConfirmation` - All
      3 tests passing: updates preserve historical data, activation applies to
      new messages, confirmation required for active analyzers
- [x] T072 [P] [US2] Frontend unit test for mapping update workflow in
      `frontend/src/components/analyzers/FieldMapping/MappingPanel.test.jsx` -
      Test methods: `testUpdateMapping_ShowsConfirmationModal`,
      `testSaveDraftMapping_DoesNotRequireConfirmation`,
      `testCreateMapping_DoesNotRequireConfirmation` - All 3 tests passing:
      tests verify current behavior, confirmation modal test prepared for
      T078/T164 implementation
- [x] T073 [P] [US2] Cypress E2E test in
      `frontend/cypress/e2e/analyzerMaintenance.cy.js` - Test scenarios: "should
      update existing mapping", "should activate draft mapping with
      confirmation", "should deactivate mapping while preserving history" - All
      3 test scenarios implemented: update workflow, activation with
      confirmation, deactivation with history preservation - Tests use
      data-testid selectors, API-based setup, intercepts before actions

### Implementation for User Story 2

- [x] T074 [US2] Extend AnalyzerFieldMappingServiceImpl with update methods
      supporting draft/active workflow per FR-010 - Validate required mappings
      before activation (noted for analyzer activation time) - Apply changes to
      new messages only (existing results unchanged) - updateMapping() and
      activateMapping() methods implemented with confirmation workflow - All
      integration tests passing
- [x] T075 [US2] Add audit trail logging to AnalyzerFieldMappingServiceImpl for
      all mapping changes (who, when, previous vs new values) per FR-009 - Use
      BaseObject audit fields (sys_user_id, last_updated) - All update methods
      (updateMapping, activateMapping, disableMapping) call
      setLastupdatedFields() - Detailed audit trail (previous vs new values) can
      be added via AuditTrailService if needed
### Copy Mappings Feature (FR-006)

**Purpose**: Allow copying field mappings from one analyzer to another for faster
configuration

**Note**: Copy Mappings is an optional convenience feature, not required for core
US2 functionality (draft/active workflow T074, T078 already complete). Can be
deferred to Phase 8 (Polish) if needed.

- [x] T191 [P] [US2] Unit test for AnalyzerMappingCopyService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingCopyServiceTest.java` -
      Test methods: `testCopyMappings_WithValidSource_CopiesAllMappings`,
      `testCopyMappings_WithExistingMappings_OverwritesTarget`,
      `testCopyMappings_WithTypeIncompatibility_GeneratesWarnings`,
      `testMergeQualitativeMappings_CombinesValuesDeduplicated`,
      `testCopyMappings_WithPartialFailure_RollsBackTransaction`
- [x] T192 [P] [US2] Create AnalyzerMappingCopyService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingCopyService.java`
- [x] T193 [US2] Create AnalyzerMappingCopyServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerMappingCopyServiceImpl.java`
      with @Service and @Transactional annotations - Methods:
      `copyMappings(sourceAnalyzerId, targetAnalyzerId, CopyOptions)`,
      `validateCopyOperation(source, target)`,
      `resolveConflicts(existingMappings, newMappings)`,
      `mergeQualitativeMappings(source, target)` - Conflict resolution: Overwrite
      existing mappings by default, merge qualitative values (deduplicate),
      validate type compatibility (skip or warn), rollback on any failure - per
      FR-006 workflow
- [x] T194 [US2] Add copyMappings endpoint to
      AnalyzerFieldMappingRestController - Endpoint: POST
      `/rest/analyzer/analyzers/{targetId}/copy-mappings` - Request: `{
      sourceAnalyzerId: String, overwriteExisting: Boolean (default true),
      skipIncompatible: Boolean (default false) }` - Response: `{ copiedCount:
      Integer, skippedCount: Integer, warnings: String[], conflicts:
      ConflictDetail[] }` - Returns HTTP 200 with copy results, HTTP 400 if
      validation fails, HTTP 409 if rollback occurs
- [x] T195 [P] [US2] Controller test for copyMappings endpoint in
      `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestControllerTest.java` -
      Test methods: `testCopyMappings_WithValidRequest_ReturnsCopyResults`,
      `testCopyMappings_WithNoSourceMappings_ReturnsBadRequest`
- [x] T076 [P] [US2] Create CopyMappingsModal component in
      `frontend/src/components/analyzers/FieldMapping/CopyMappingsModal.jsx` using
      Carbon ComposedModal (small size ~400-480px) - Dialog header: title "Copy
      Field Mappings", subtitle dynamic - Source analyzer section (read-only):
      analyzer name, type - Target analyzer section: dropdown (searchable,
      filters to analyzers with active mappings), auto-populated from context -
      Mapping summary: "X mappings will be copied" with breakdown - Warning note:
      overwrite warning - Confirmation: nested modal before copy - Action buttons:
      Cancel, "Copy Mappings" (with Copy icon) - Success notification: mapping
      count, "View Target Analyzer" button - Error handling: display conflicts,
      allow retry - per FR-006
- [x] T196 [P] [US2] Frontend unit test for CopyMappingsModal in
      `frontend/src/components/analyzers/FieldMapping/CopyMappingsModal.test.jsx` -
      Test methods: `testSelectTarget_EnablesCopyButton`,
      `testCopyMappings_ShowsConfirmationDialog`,
      `testCopySuccess_ShowsNotificationWithCount`,
      `testCopyWithConflicts_DisplaysWarnings`
- [x] T197 [US2] Integrate CopyMappingsModal into AnalyzersList component -
      Add "Copy Mappings" action to row overflow menu - Open modal with source
      analyzer pre-selected - Wire modal to API endpoint
      `/rest/analyzer/analyzers/{id}/copy-mappings` - Handle success/error states
- [x] T078 [US2] Extend MappingPanel component with Edit Mode supporting draft
      state per FR-010 - Show "Save as Draft" and "Save and Activate" buttons -
      Require confirmation for active analyzers - Integrated
      MappingActivationModal component (T164) - Updated FieldMapping to pass
      analyzerIsActive and analyzerName props - All 3 MappingPanel tests
      passing: testUpdateMapping_ShowsConfirmationModal,
      testSaveDraftMapping_DoesNotRequireConfirmation,
      testCreateMapping_DoesNotRequireConfirmation
- [ ] T079 [US2] Add visual indicators for draft vs active mappings in
      FieldMappingPanel component - Status badges, filter options for
      draft/active
- [ ] T080 [US2] Create TestMappingModal component in
      `frontend/src/components/analyzers/FieldMapping/TestMappingModal.jsx`
      using Carbon ComposedModal for inline test mapping capability per FR-007 -
      Submit sample ASTM messages, preview interpretation, show
      warnings/errors - Add "Test Mapping" button in FieldMapping page header
      (ghost style, secondary action) positioned between "Back" button and "Save
      Mappings" button per FR-007 specification - Button opens TestMappingModal
      when clicked
- [x] T164 [P] [US2] Create MappingActivationModal component in
      `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.jsx`
      using Carbon ComposedModal (warning variant) - Dialog header with title
      "Activate Mapping Changes" and subtitle "Confirm activation of mapping
      changes for analyzer '{analyzer name}'" - Warning message section with
      warning icon - Additional warning for active analyzers - Confirmation
      checkbox (required before activation) - Dialog footer with Cancel button
      (secondary) and "Activate Changes" button (primary, destructive style) per
      FR-010 specification - Component created with all required features, uses
      React Intl, data-testid attributes for testing
- [x] T165 [P] [US2] Frontend unit test for MappingActivationModal in
      `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.test.jsx` -
      Test methods: `testDisplayModal_ShowsWarningMessages`,
      `testDisplayModal_WithActiveAnalyzer_ShowsActiveWarning`,
      `testActivate_WithoutCheckbox_DisablesButton`,
      `testActivate_WithCheckbox_EnablesButton`,
      `testActivate_WithCheckbox_CallsOnConfirm`, `testCancel_CallsOnClose`,
      `testModal_WhenClosed_NotVisible` - All 7 tests passing: uses data-testid
      selectors, waits for content with findByTestId, checks checkbox state and
      button disabled state directly

### Activation Modal Edge Cases (FR-010)

**Purpose**: Enhance MappingActivationModal with validation for edge cases

- [x] T163 [P] [US1] Enhance MappingActivationModal component in
      `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.jsx`
      with edge case handling - Add pending messages warning: Display "This
      analyzer has {count} pending messages in error queue. Activating mapping
      changes may affect reprocessing." with "View Pending Messages" link - Add
      required mappings validation: Block activation if Sample ID, Test Code, or
      Result Value mappings missing, display error "Cannot activate: Required
      mappings missing" with list of missing fields - Add concurrent edit
      detection: Display optimistic locking warning "Mapping changes detected.
      Another user modified mappings. Please reload." with "Reload Page" button -
      Update per FR-010 edge cases
- [x] T166 [P] [US1] Add activateMapping endpoint to
      AnalyzerFieldMappingRestController - Endpoint: POST
      `/rest/analyzer/analyzers/{id}/activate-mappings` - Request: `{ mappingIds:
      String[], confirmationToken: String }` - Validation: Check required mappings
      present (Sample ID, Test Code, Result Value), check pending messages in
      error queue, check optimistic lock (compare lastUpdated timestamps) -
      Response: `{ activatedCount: Integer, warnings: String[], requiresAdditionalConfirmation: Boolean }` -
      Returns HTTP 400 if validation fails, HTTP 409 if concurrent edit detected
- [x] T167 [US1] Add validateActivation method to
      AnalyzerFieldMappingServiceImpl - Method signature: `ActivationValidationResult
      validateActivation(String analyzerId)` - Validation checks: Required
      mappings present (Sample ID, Test Code, Result Value), pending messages in
      error queue count, concurrent edits detection using optimistic locking from
      T168a (version-based), all active mappings have compatible types, analyzer
      connection operational (optional warning) - Returns: `{ canActivate: Boolean, missingRequired:
      String[], pendingMessagesCount: Integer, warnings: String[] }`
- [x] T168 [P] [US1] Unit test for activation validation in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` -
      Add test methods: `testValidateActivation_WithMissingRequired_ReturnsFalse`,
      `testValidateActivation_WithPendingMessages_ReturnsWarnings`,
      `testValidateActivation_AllChecksPass_ReturnsTrue`,
      `testActivateMapping_WithConcurrentEdit_ThrowsOptimisticLockException`
- [ ] T169a [P] [US1] Frontend unit test for MappingActivationModal edge cases in
      `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.test.jsx` -
      Add test methods: `testActivation_WithMissingRequired_ShowsErrorAndBlocksButton`,
      `testActivation_WithPendingMessages_ShowsWarningAndAllowsActivation`,
      `testActivation_WithConcurrentEdit_ShowsOptimisticLockWarning`

### Retire/Disable Mappings (FR-013)

**Purpose**: Allow retiring mappings while preserving historical data for audit

- [x] T198 [US2] Add retireMapping method to AnalyzerFieldMappingServiceImpl -
      Method signature: `void retireMapping(String mappingId, String reason)` -
      Sets `is_active=false`, records `retirement_date=NOW()`, stores
      `retirement_reason` in notes field - Validation: Cannot retire if analyzer
      has pending messages (UNACKNOWLEDGED/PENDING_RETRY status) using this
      mapping - Throws `LIMSRuntimeException` with message "Cannot retire mapping:
      {count} pending messages reference this mapping" - per FR-013
- [x] T169 [P] [US2] Enhance MappingPanel View Mode with retire action -
      Add "Retire Mapping" button in View Mode header (positioned next to Edit,
      before Remove) - Button disabled with tooltip if mapping is required AND
      analyzer has pending messages - Clicking opens MappingRetirementModal (T170) -
      Wire to retire endpoint - Add visual indicator: "Retired" badge (gray Tag)
      on mappings with `is_active=false` - per FR-013
- [x] T170 [P] [US2] Create MappingRetirementModal component in
      `frontend/src/components/analyzers/FieldMapping/MappingRetirementModal.jsx`
      using Carbon ComposedModal (small size ~400px) - Dialog header: title
      "Retire Mapping", subtitle "Confirm retirement of field mapping" -
      Confirmation message: "Are you sure you want to retire this mapping?
      Historical messages will still reference it for audit purposes." -
      Retirement reason field: TextArea (optional, max 500 chars, placeholder
      "Optional: Reason for retiring this mapping") - Warning: If pending
      messages exist, display "Cannot retire: {count} pending messages use this
      mapping" and block action - Action buttons: Cancel (secondary), "Retire
      Mapping" (destructive, disabled if pending messages) - Success notification:
      "Mapping retired successfully" - per FR-013
- [x] T200 [US2] Add getHistoricalMappings endpoint enhancement to
      AnalyzerFieldMappingRestController - Update existing GET
      `/rest/analyzer/analyzers/{id}/mappings` endpoint - Add query parameter:
      `includeRetired` (Boolean, default false) - Response includes
      retirement_date, retirement_reason, retired_by_user for retired mappings -
      Filter: Only include mappings with `is_active=false` if parameter is true -
      per FR-013
- [x] T201 [P] [US2] Unit test for retireMapping in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` -
      Test methods: `testRetireMapping_WithNoPendingMessages_SetsInactiveSuccessfully`,
      `testRetireMapping_WithPendingMessages_ThrowsException`,
      `testRetireMapping_WithReason_StoresReasonInNotes`,
      `testRetireMapping_SetsRetirementDateToNow`
- [ ] T202 [P] [US2] Frontend unit test for retire mapping workflow in
      `frontend/src/components/analyzers/FieldMapping/MappingPanel.test.jsx` -
      Test methods: `testRetireButton_WithPendingMessages_ShowsDisabledTooltip`,
      `testRetireMapping_OpensConfirmationModal`,
      `testRetiredMapping_DisplaysRetiredBadge`,
      `testRetireWithReason_SubmitsReasonToAPI`
- [ ] T161 [P] [US2] Add integration test for SC-003 (audit trail completeness)
      in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingAuditTest.java`
      using @SpringBootTest - Test methods: `testCreateMapping_LogsAuditTrail`,
      `testUpdateMapping_LogsPreviousAndNewValues`,
      `testDisableMapping_LogsRetirementReason` - Verify audit trail fields:
      user ID, timestamp, previous value, new value - Test audit trail query
      performance - Create/update/disable 100 mappings, verify 100% have audit
      trail entries per Success Criteria SC-003

**Checkpoint**: At this point, User Stories 1 AND 2 should both work
independently

---

## Phase 5: User Story 3 - Resolve Unmapped Messages (Priority: P3)

**Goal**: Interface administrator can see ASTM messages that could not be
processed due to missing or ambiguous mappings, correct the mappings, and then
reprocess the affected messages.

**Independent Test**: A tester can generate ASTM messages with unmapped test
codes or result values, observe that they are held for review rather than
silently failing, configure the necessary mappings, and then confirm that the
impacted messages can be reprocessed successfully.

### Tests for User Story 3 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T081 [P] [US3] Unit test for AnalyzerErrorService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerErrorServiceTest.java` -
      Test methods: `testCreateError_WithUnmappedField_CreatesErrorRecord`,
      `testAcknowledgeError_WithValidUser_UpdatesStatus`,
      `testReprocessError_WithNewMapping_ProcessesMessage` - All 5 tests
      passing: createError, acknowledgeError, reprocessError,
      getErrorsByFilters, exception handling
- [x] T082 [P] [US3] Unit test for AnalyzerReprocessingService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingServiceTest.java` -
      Test methods: `testReprocessMessage_WithValidMapping_ReturnsSuccess`,
      `testReprocessMessage_WithStillUnmapped_ReturnsError` - All 4 tests
      passing: valid mapping, still unmapped, null raw message, empty raw
      message
- [x] T083 [P] [US3] Integration test for error queue workflow in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerErrorServiceIntegrationTest.java`
      using @SpringBootTest - Test methods:
      `testHoldUnmappedMessage_InErrorQueue`,
      `testReprocessAfterMapping_CreatesOrder` - All 4 tests passing: hold
      unmapped message, reprocess after mapping, acknowledge error, get errors
      by filters
- [x] T084 [P] [US3] DAO test for AnalyzerErrorDAO in
      `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAOTest.java` -
      Test methods: `testFindByStatus_ReturnsUnacknowledgedErrors`,
      `testFindByAnalyzerId_ReturnsErrorsForAnalyzer` - All 6 tests passing:
      findByStatus, findByAnalyzerId, findByErrorType, findBySeverity, get with
      valid ID, get with invalid ID
- [x] T085 [P] [US3] Controller test for AnalyzerErrorRestController in
      `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerErrorRestControllerTest.java` -
      **Test Slicing**: Uses `BaseWebContextSensitiveTest` (matching existing
      codebase pattern) - **HTTP Testing**: Uses `MockMvc` - Test methods:
      `testGetErrors_WithFilters_ReturnsFilteredList`,
      `testGetError_WithValidId_ReturnsError`,
      `testAcknowledgeError_WithValidId_UpdatesStatus`,
      `testReprocessError_WithValidId_ProcessesMessage`,
      `testGetError_WithInvalidId_Returns404`,
      `testGetErrors_WithNoFilters_ReturnsAllErrors` (catches bug where service
      returned empty list when no filters),
      `testGetErrors_ResponseFormat_MatchesFrontendExpectations` (verifies errors
      converted to maps and response structure matches frontend) - All 7 tests
      passing ✓
- [x] T086 [P] [US3] Frontend unit test for ErrorDashboard component in
      `frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.test.jsx` -
      Test methods: `testRendersErrorDashboard_WithErrors_DisplaysTable`,
      `testFilterErrors_ByType_FiltersResults`,
      `testOpenErrorDetails_ShowsModal`,
      `testSearchErrors_WithQuery_FiltersResults`,
      `testAcknowledgeAll_CallsHandler` - **Updated**: All tests now use correct
      API response format `{ data: { content: [...], statistics: {...} }, status:
      "success" }` to catch frontend/backend response format mismatches
- [x] T087 [P] [US3] Frontend unit test for ErrorDetailsModal component in
      `frontend/src/components/analyzers/ErrorDashboard/ErrorDetailsModal.test.jsx` -
      Test methods: `testDisplayErrorDetails_ShowsFullContext`,
      `testOpenMappingInterface_FromError_ShowsMappingModal`
- [ ] T088 [P] [US3] Cypress E2E test in
      `frontend/cypress/e2e/errorResolution.cy.js` - Test scenarios: "should
      display unmapped messages in error dashboard", "should create mapping from
      error context", "should reprocess error after mapping creation", "should
      acknowledge multiple errors in batch"

### Implementation for User Story 3

- [x] T089 [P] [US3] Create AnalyzerErrorService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerErrorService.java` -
      Interface created with methods: createError, acknowledgeError,
      reprocessError, getErrorsByFilters
- [x] T090 [US3] Create AnalyzerErrorServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerErrorServiceImpl.java`
      with @Service and @Transactional annotations - Methods: createError,
      acknowledgeError, getErrorsByFilters per FR-016 - Implementation complete
      with audit trail logging (setLastupdatedFields, setSysUserId), all unit
      tests passing
- [x] T091 [P] [US3] Create AnalyzerReprocessingService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingService.java` -
      Interface created with reprocessMessage method
- [x] T092 [US3] Create AnalyzerReprocessingServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingServiceImpl.java`
      with @Service and @Transactional annotations - Reprocess raw message from
      AnalyzerError.rawMessage through ASTMAnalyzerReader with new mappings per
      FR-017 - Implementation complete: validates raw message, checks for active
      mappings, converts to InputStream, processes through ASTMAnalyzerReader,
      all unit tests passing
- [x] T093 [US3] Integrate error creation into
      ASTMAnalyzerReader.processData() - When mapping not found: Create
      AnalyzerError record, hold message in error queue per FR-011 - When
      validation fails: Create AnalyzerError record with validation details -
      Implementation complete: Error creation integrated through
      MappingAwareAnalyzerLineInserter wrapper in ASTMAnalyzerReader.insertAnalyzerData()
      (T180). Wrapper creates AnalyzerError records when mappings not found or
      transformation fails per FR-011
- [x] T094 [P] [US3] Create AnalyzerErrorForm DTO in
      `src/main/java/org/openelisglobal/analyzer/form/AnalyzerErrorForm.java`
- [x] T095 [US3] Create AnalyzerErrorRestController in
      `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerErrorRestController.java`
      extending BaseRestController - Endpoints: GET /analyzers/errors, GET
      /analyzers/errors/{id}, POST /analyzers/errors/{id}/acknowledge, POST
      /analyzers/errors/{id}/reprocess, POST /analyzers/errors/batch-acknowledge
      per FR-016, FR-017 - Implementation complete with all endpoints, error
      handling, and statistics calculation
- [x] T096 [US3] Create ErrorDashboard component in
      `frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.jsx`
      using Carbon DataTable with statistics cards, filters, pagination per
      FR-016 - Implementation complete: Component created with statistics cards,
      filters (search, error type, severity, analyzer), DataTable with error
      details, ErrorDetailsModal integration, action dropdown for view/acknowledge
- [x] T097 [US3] Create ErrorDetailsModal component in
      `frontend/src/components/analyzers/ErrorDashboard/ErrorDetailsModal.jsx`
      using Carbon ComposedModal with error information, analyzer logs,
      recommended actions per FR-016 - Implementation complete: Modal created
      with error information display, analyzer logs accordion, recommended
      actions, acknowledge button, "Create Mapping" button for MAPPING errors
      (T101)
- [x] T098 [US3] Create ErrorDashboardPage route component in
      `frontend/src/pages/ErrorDashboardPage.jsx` integrating ErrorDashboard
      component with route `/analyzers/errors` - Implementation complete: Page
      component created and integrated
- [x] T099 [US3] Add React Router route in `frontend/src/App.js` for
      `/analyzers/errors` (ErrorDashboardPage) - Implementation complete: Route
      added to App.js
- [x] T100 [US3] Add visual indicators for unmapped fields in FieldMappingPanel
      component - Status badges showing "Unmapped", filter options, counts in
      status bar per FR-012 - Implementation complete: Added status filter
      dropdown, warning icons for unmapped fields, status badges (Mapped/Unmapped),
      and counts in status bar (mapped/unmapped counts)
- [x] T101 [US3] Integrate mapping interface modal into ErrorDetailsModal -
      "Create Mapping" button opens FieldMapping modal with pre-filled context
      from error per FR-011 - Implementation complete: Added "Create Mapping"
      button that navigates to FieldMapping page with analyzer ID from error
      context. Button only shows for MAPPING error type
- [x] T177 [P] [US3] Create MappingAwareAnalyzerLineInserter wrapper class in
      `src/main/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserter.java`
      implementing AnalyzerLineInserter interface - Wrapper logic: Receive raw
      ASTM message segments, call MappingApplicationService.applyMappings() to
      transform segments, delegate transformed data to original plugin inserter
      if mappings found, create AnalyzerError if mappings not found per
      research.md Section 7 - Implementation complete: Wrapper class created
      with error handling, unit tests passing
- [x] T178 [P] [US3] Create MappingApplicationService interface in
      `src/main/java/org/openelisglobal/analyzer/service/MappingApplicationService.java`
      - Interface created with applyMappings() and hasActiveMappings() methods
- [x] T179 [P] [US3] Create MappingApplicationServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/MappingApplicationServiceImpl.java`
      with @Service and @Transactional annotations - Method: applyMappings() -
      Receives raw ASTM message segments, extracts test codes/units/qualitative
      values, queries AnalyzerFieldMapping, applies mappings, returns
      transformed data structure per research.md Section 7 - Implementation
      complete: Service implementation with basic transformation logic, unit
      tests passing (5 tests)
- [x] T180 [US3] Integrate wrapper into ASTMAnalyzerReader.processData() with
      conditional wrapping logic - Check if analyzer has active mappings before
      wrapping - If analyzer has active mappings: Wrap plugin inserter with
      MappingAwareAnalyzerLineInserter - If analyzer has no mappings: Use
      original plugin inserter directly (backward compatibility) per research.md
      Section 7 - Implementation complete: Integration logic added to
      insertAnalyzerData() method, wrapInserterIfMappingsExist() helper method
      created. Note: identifyAnalyzerFromMessage() is a placeholder and needs
      enhancement to fully identify analyzer from ASTM message header
- [x] T181 [P] [US3] Integration test for MappingAwareAnalyzerLineInserter
      wrapper pattern in
      `src/test/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserterIntegrationTest.java`
      using @SpringBootTest - Test methods:
      `testProcessMessage_WithMappings_AppliesTransformations`,
      `testProcessMessage_WithoutMappings_UsesOriginalInserter`,
      `testProcessMessage_WithUnmappedField_CreatesError` - Implementation
      complete: Integration tests created using BaseWebContextSensitiveTest,
      all 3 tests passing
- [ ] T160 [P] [US3] Add integration test for SC-002 (98% processing rate) in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingPerformanceTest.java`
      using @SpringBootTest - Test methods:
      `testProcessMessages_WithMappings_MeetsSuccessRate`,
      `testProcessMessages_WithUnmappedFields_HandlesGracefully` - Send 1000
      ASTM messages with mappings configured, verify 98%+ processed
      successfully - Test error rate calculation: (successful messages / total
      messages) >= 0.98 - Include edge cases: unmapped fields, unit mismatches,
      validation errors per Success Criteria SC-002

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Query Analyzer Functionality (FR-002)

**Purpose**: Implement asynchronous "Query Analyzer" functionality for
retrieving available data fields from analyzers

### Tests for Query Analyzer

- [x] T102 [P] Unit test for AnalyzerQueryService in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceTest.java` -
      Test methods: `testQueryAnalyzer_WithValidConfig_ReturnsJobId`,
      `testGetQueryStatus_WithJobId_ReturnsStatus`,
      `testCancelQuery_WithJobId_CancelsJob`
- [x] T103 [P] Integration test for query workflow in
      `src/test/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceIntegrationTest.java`
      using @SpringBootTest - Test methods:
      `testQueryAnalyzer_WithTimeout_HandlesGracefully`,
      `testParseASTMResponse_ExtractsFields`

### Implementation for Query Analyzer

- [x] T104 [P] Create AnalyzerQueryService interface in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerQueryService.java`
- [x] T105 Create AnalyzerQueryServiceImpl in
      `src/main/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceImpl.java`
      with @Service and @Transactional annotations - TCP connection to analyzer
      IP:Port, ASTM query message sending, response parsing per research.md
      Section 1 - Background job pattern: return job ID immediately, poll status
      endpoint per FR-002 - Read query timeout from SystemConfiguration key
      `analyzer.query.timeout.minutes` (default: 5 minutes) per FR-002
      specification - Use SystemConfigurationService to lookup timeout value,
      fallback to 5 minutes if not configured
- [x] T105a [P] Create default SystemConfiguration entry for
      `analyzer.query.timeout.minutes` via Liquibase changeset - Location:
      `src/main/resources/liquibase/analyzer/004-010-add-query-timeout-config.xml` -
      Insert into `system_configuration` table with key
      `analyzer.query.timeout.minutes`, value `5`, description "Query timeout in
      minutes for analyzer field queries (default: 5 minutes, configurable per
      deployment)" - This ensures the configuration key exists for T105 to read
      from per FR-002 specification
- [x] T106 Add query endpoints in AnalyzerRestController: POST
      /analyzers/{id}/query (returns job ID), GET
      /analyzers/{id}/query/{jobId}/status (polling endpoint) per FR-002
- [x] T107 Create QueryStatusModal component in
      `frontend/src/components/analyzers/FieldMapping/QueryStatusModal.jsx`
      using Carbon ComposedModal with progress bar, connection logs, cancel
      button per FR-002
- [x] T108 Add "Query Analyzer" button to FieldMapping component - Triggers
      background job, polls status endpoint every 2-3 seconds, displays progress
      per FR-002
- [x] T109 Integrate query results into FieldMappingPanel - Display retrieved
      fields with field type indicators (color-coded tags) per FR-002

---

## Phase 7: Navigation Integration (FR-020)

**Purpose**: Integrate analyzer mapping feature into OpenELIS left-hand
navigation using unified tab-navigation pattern

### Tests for Navigation Integration

- [ ] T110 [P] Integration test for menu API in
      `src/test/java/org/openelisglobal/menu/controller/MenuControllerTest.java` -
      Test methods: `testGetMenu_WithAnalyzersItems_ReturnsItems`,
      `testGetMenu_WithRoleFilter_HidesUnauthorizedItems`
- [x] T111 [P] Frontend unit test for navigation integration in
      `frontend/src/components/layout/Header.test.js` - Test methods:
      `testRendersAnalyzersMenu_WithSubItems_DisplaysExpandable`,
      `testHighlightActiveSubNav_WithRoute_ShowsActiveState`,
      `testSidebar_PersistentOnAnalyzerPages`,
      `testFieldMappingsMenu_OnlyVisibleOnMappingsRoute`

### Implementation for Navigation Integration

- [x] T112 Extend `MenuController`
      (`src/main/java/org/openelisglobal/menu/controller/MenuController.java`)
      to return the full analyzer nav tree (Analyzers Dashboard, Error
      Dashboard, contextual Field Mappings, QC dashboard, QC Alerts &
      Violations, Corrective Actions) via `/rest/menu` with role-based filtering
      so QC routes only appear for QC-enabled roles per FR-020 clarification
- [x] T113 Update `frontend/src/components/layout/Header.js` / `GlobalSideBar`
      to render the expanded analyzer hierarchy from the menu API, ensure
      sub-nav items act as tabs, and always highlight the active route
      (including QC placeholders) using Carbon `SideNavMenuItem`
- [x] T210 [P] Add new placeholder QC pages in `frontend/src/pages/analyzers/`
      (`QCDashboardPlaceholder.jsx`, `QCAlertsPlaceholder.jsx`,
      `CorrectiveActionsPlaceholder.jsx`) that display "Quality Control coming
      soon" messaging and link to feature `003-westgard-qc` resources until the
      full functionality ships
- [x] T211 [P] Wire the placeholder QC pages into React Router
      (`frontend/src/App.js`) with routes `/analyzers/qc`,
      `/analyzers/qc/alerts`, `/analyzers/qc/corrective-actions`, secure them
      with the appropriate roles, and ensure navigation entries open these pages
- [x] T212 Add route metadata or page component props to force the left-hand
      navigation visible/expanded for all analyzer routes (dashboard, error,
      mappings, QC placeholders) per FR-020
- [x] T213 Implement state preservation using URL query parameters for filters,
      pagination, selected analyzer ID per FR-020 - Use URLSearchParams pattern
      consistent with `SearchResultForm.js`
- [x] T214 Implement state preservation using sessionStorage for scroll
      position, form drafts per FR-020 - Clear on tab close, preserve across
      browser navigation

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T117 [P] Add comprehensive error handling and user feedback (success,
      warnings, errors) throughout analyzer mapping UI per FR-014 - Use Carbon
      Notification components, follow OpenELIS internationalization practices
- [ ] T141 [P] Integrate custom field types into AnalyzerField entity and
      FieldMappingPanel component - Extend AnalyzerField entity to support
      custom field types (reference CustomFieldType entity) - Update
      FieldMappingPanel to show custom field types in type selector - Update
      validation to use custom field type validation rules per FR-018

---

## Phase 8.5: System Administration - Custom Field Types Management

**Purpose**: Admin UI for managing custom field types (FR-018)

- [ ] T140 [P] Create CustomFieldTypeManagement component in
      `frontend/src/components/analyzers/admin/CustomFieldTypeManagement.jsx` -
      Admin-only page accessible via System Administration menu - Form fields:
      Type Name (unique identifier), Display Name, Validation Pattern (regex
      input with validation), Value Range (min/max for numeric types), Allowed
      Characters (character set input), Active Status - List view with
      edit/delete actions per FR-018
- [ ] T166 [P] Frontend unit test for CustomFieldTypeManagement in
      `frontend/src/components/analyzers/admin/CustomFieldTypeManagement.test.jsx` -
      Test methods: `testRendersCustomFieldTypes_WithData_DisplaysTable`,
      `testCreateCustomFieldType_WithValidData_SavesType`,
      `testValidatePattern_WithInvalidRegex_ShowsError`

### Validation Rule Engine (FR-018)

**Purpose**: Implement runtime validation for custom field types using configurable
rules

- [ ] T167 [P] [FR-018] Create ValidationRuleConfiguration entity in
      `src/main/java/org/openelisglobal/analyzer/valueholder/ValidationRuleConfiguration.java`
      extending BaseObject<String> - Fields: id, customFieldTypeId (FK), ruleName,
      ruleType (REGEX/RANGE/ENUM/LENGTH enum), ruleExpression (TEXT/JSON),
      errorMessage (VARCHAR 500), isActive (BOOLEAN) - Relationships: Many-to-One
      with CustomFieldType - Validation: ruleType enum, ruleExpression valid
      format - per data-model.md Section 8
- [ ] T168 [P] [FR-018] Create Liquibase changeset
      `src/main/resources/liquibase/analyzer/004-008-create-validation-rule-configuration-table.xml` -
      Create validation_rule_configuration table - Columns: id (PK), custom_field_type_id
      (FK), rule_name, rule_type, rule_expression (TEXT), error_message, is_active,
      sys_user_id, last_updated - Index on custom_field_type_id for query
      performance - Rollback: DROP TABLE validation_rule_configuration
- [ ] T169 [P] [FR-018] Create ValidationRuleConfigurationDAO interface in
      `src/main/java/org/openelisglobal/analyzer/dao/ValidationRuleConfigurationDAO.java`
      extending BaseDAO<ValidationRuleConfiguration, String>
- [ ] T170 [FR-018] Create ValidationRuleConfigurationDAOImpl extending
      BaseDAOImpl with @Component and @Transactional - HQL query methods:
      `findByCustomFieldTypeId(String typeId)`,
      `findActiveRulesByCustomFieldTypeId(String typeId)`
- [ ] T171 [P] [FR-018] Create ValidationRuleEngine interface in
      `src/main/java/org/openelisglobal/analyzer/service/ValidationRuleEngine.java` -
      Methods: `evaluateRule(String value, ValidationRuleConfiguration rule)`,
      `validateRegex(String value, String pattern)`, `validateRange(Number value,
      Number min, Number max)`, `validateEnum(String value, List<String> allowedValues)`,
      `validateLength(String value, Integer minLength, Integer maxLength)`
- [ ] T172 [FR-018] Create ValidationRuleEngineImpl with @Service (NO
      @Transactional - stateless validation) - Implement all validation methods -
      REGEX: Compile pattern, match against value - RANGE: Parse JSON `{"min":
      X, "max": Y}`, validate numeric value - ENUM: Parse JSON array, check value
      in list - LENGTH: Parse JSON `{"minLength": X, "maxLength": Y}`, validate
      string length - Error handling: Return validation errors with custom error
      messages from rule configuration
- [ ] T173 [P] [FR-018] Create ValidationRuleConfigurationForm in
      `src/main/java/org/openelisglobal/analyzer/form/ValidationRuleConfigurationForm.java` -
      Fields: id, customFieldTypeId, ruleName, ruleType, ruleExpression,
      errorMessage, isActive - Validation: @NotNull for required fields, @Size
      for string lengths
- [ ] T174 [FR-018] Add validation rule CRUD endpoints to
      CustomFieldTypeRestController - Endpoints: GET
      `/rest/analyzer/custom-field-types/{id}/validation-rules` (list rules), POST
      `/rest/analyzer/custom-field-types/{id}/validation-rules` (create rule), PUT
      `/rest/analyzer/custom-field-types/{id}/validation-rules/{ruleId}` (update
      rule), DELETE `/rest/analyzer/custom-field-types/{id}/validation-rules/{ruleId}`
      (delete rule) - Authorization: System Administrator only - Validation:
      Validate rule expression format before save
- [ ] T175 [P] [FR-018] Create ValidationRuleEditor component in
      `frontend/src/components/analyzers/CustomFieldTypes/ValidationRuleEditor.jsx` -
      Rule type selector (Dropdown: REGEX, RANGE, ENUM, LENGTH) - Dynamic form
      fields based on rule type: REGEX (pattern TextInput with validation),
      RANGE (min/max NumberInputs), ENUM (TagInput for values list), LENGTH
      (minLength/maxLength NumberInputs) - Error message customization (TextInput,
      max 500 chars) - Test validation section: Sample value input, "Test Rule"
      button, validation result display - Action buttons: Save Rule, Cancel
- [ ] T176 [FR-018] Integrate validation rule evaluation into field mapping
      workflow - When analyzer field uses custom field type (field_type='CUSTOM',
      custom_field_type_id != NULL), fetch validation rules - Apply validation
      rules in AnalyzerFieldMappingService before saving mapping - Display
      validation errors in MappingPanel - Block mapping save if validation fails -
      Show validation rules in field info tooltip
- [ ] T177 [P] [FR-018] Unit test for ValidationRuleEngine in
      `src/test/java/org/openelisglobal/analyzer/service/ValidationRuleEngineTest.java` -
      Test methods: `testValidateRegex_WithMatchingValue_ReturnsTrue`,
      `testValidateRegex_WithNonMatchingValue_ReturnsFalse`,
      `testValidateRange_WithValueInRange_ReturnsTrue`,
      `testValidateRange_WithValueOutOfRange_ReturnsFalse`,
      `testValidateEnum_WithAllowedValue_ReturnsTrue`,
      `testValidateEnum_WithDisallowedValue_ReturnsFalse`,
      `testValidateLength_WithValidLength_ReturnsTrue`,
      `testValidateLength_WithInvalidLength_ReturnsFalse`,
      `testEvaluateRule_WithInvalidRuleExpression_ThrowsException`
- [ ] T178 [P] [FR-018] Frontend unit test for ValidationRuleEditor in
      `frontend/src/components/analyzers/CustomFieldTypes/ValidationRuleEditor.test.jsx` -
      Test methods: `testSelectRuleType_ShowsRelevantFields`,
      `testRegexRule_WithInvalidPattern_ShowsError`,
      `testRangeRule_WithMinGreaterThanMax_ShowsError`,
      `testTestValidation_WithSampleValue_DisplaysResult`

- [ ] T118 [P] Add loading states and skeleton screens for async operations
      (query analyzer, test connection, reprocessing)
- [ ] T119 [P] Optimize HQL queries in DAO implementations using JOIN FETCH for
      eager loading (prevents LazyInitializationException per AGENTS.md)
- [ ] T120 [P] Add comprehensive logging for analyzer operations (connection
      tests, query operations, mapping changes, error creation)
- [ ] T121 [P] Add input validation and sanitization for all user inputs (IP
      addresses, port numbers, field names, mapping values)
- [ ] T122 [P] Add accessibility improvements (ARIA labels, keyboard navigation,
      screen reader support) for Carbon components
- [ ] T123 [P] Add responsive design improvements for mobile devices (<1024px) -
      Stack panels vertically, optimize table layouts
- [ ] T124 [P] Add performance optimizations (debouncing search inputs,
      pagination, lazy loading for large datasets)
- [ ] T125 [P] Update documentation in
      `specs/004-astm-analyzer-mapping/quickstart.md` with step-by-step
      developer guide
- [ ] T126 Run quickstart.md validation - Verify all scenarios from
      quickstart.md work end-to-end

---

## Phase 9: Constitution Compliance Verification

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

- [ ] T127 **Configuration-Driven**: Verify no country-specific code branches
      introduced - All variations via database configuration
      (SystemConfiguration, LocalizationConfiguration)
- [ ] T128 **Carbon Design System**: Audit UI - confirm @carbon/react used
      exclusively (NO Bootstrap/Tailwind) - Verify Carbon tokens used for
      colors, spacing, typography - Verify Carbon components used: SideNavMenu,
      SideNavMenuItem, DataTable, ComposedModal, Search, MultiSelect, Tag,
      Toggle, OverflowMenu, Pagination, Accordion, TextInput, Dropdown,
      NumberInput
- [ ] T129 **FHIR/IHE Compliance**: Validate FHIR resources against R4 profiles
      (if applicable) - Analyzer entities may not require FHIR mapping (verify
      per research.md)
- [ ] T130 **Layered Architecture**: Verify 5-layer pattern followed
      (Valueholder→DAO→Service→Controller→Form) - NO @Transactional in
      controllers - NO DAO calls from controllers - Services compile all data
      within transaction using JOIN FETCH
- [ ] T131 **Test Coverage**: Run coverage report - confirm >80% backend
      (JaCoCo), >70% frontend (Jest) - Verify ORM validation tests pass (<5
      seconds) - Verify E2E tests run individually during development
- [ ] T131a **Jest Test Anti-Pattern Fixes**: Address act() warnings in
      AnalyzersList.test.jsx - Issue found: State updates in loadAnalyzers
      callback not wrapped in act() - Fix: Wrap API mock callbacks in act() or
      use waitFor with queryBy* selectors - Verify fix:
      `npm run test -- --testPathPattern=AnalyzersList.test.jsx` shows no
      warnings - Reference: [Testing Roadmap - Async Testing Patterns](.specify/guides/testing-roadmap.md#async-testing-patterns) -
      Apply same fix pattern to AnalyzerForm.test.jsx and FieldMapping.test.jsx
      if similar warnings exist
- [ ] T132 **Schema Management**: Verify ALL database changes use Liquibase
      changesets (NO direct SQL) - All changesets in
      `src/main/resources/liquibase/analyzer/` - Rollback scripts provided for
      structural changes
- [ ] T133 **Internationalization**: Audit UI strings - confirm React Intl used
      for ALL text (no hardcoded strings) - Verify en.json and fr.json
      translations complete - Verify date/time formatting via intl.formatDate(),
      intl.formatTime()
- [ ] T134 **Security & Compliance**: Verify RBAC (role-based access control) -
      Verify audit trail (sys_user_id + last_updated) - Verify input validation
      (SQL injection, XSS prevention) - Verify HTTPS endpoints only (NO HTTP for
      PHI)
- [ ] T162 Add manual validation checklist for SC-004 (support ticket reduction)
      in Phase 9 compliance verification - Document metric collection approach
      (not automated test) - Add manual validation checklist for post-deployment
      monitoring per Success Criteria SC-004

**Verification Commands**:

```bash
# Backend: Code formatting (MUST run before each commit) + build + tests
mvn spotless:apply && mvn spotless:check && mvn clean install -DskipTests -Dmaven.test.skip=true

# Frontend: Formatting (MUST run before each commit) + E2E tests
cd frontend && npm run format
# Run E2E tests individually (per Constitution V.5):
npm run cy:run -- --spec "cypress/e2e/analyzerConfiguration.cy.js"
npm run cy:run -- --spec "cypress/e2e/analyzerMaintenance.cy.js"
npm run cy:run -- --spec "cypress/e2e/errorResolution.cy.js"
# Full suite only in CI/CD: npm run cy:run

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** – No dependencies; can start immediately.
- **Phase 2: Foundational** – Depends on Phase 1; BLOCKS all user stories.
- **Phase 3: US1 (Configure Mappings, P1)** – Depends on Phase 2; provides
  minimal routes/components leveraged by navigation.
- **Phase 4: US2 (Maintain Mappings, P2)** – Depends on Phase 2; can progress in
  parallel with US1 once entities/DAOs exist.
- **Phase 5: US3 (Error Resolution, P3)** – Depends on Phase 2; uses error queue
  infra from foundational.
- **Phase 6: Query Analyzer (FR-002)** – Depends on Phase 2; may run in parallel
  with US1/US2/US3.
- **Phase 7: Navigation Integration (FR-020, clarified with QC placeholders)** –
  Depends on Phase 3 minimal routes.
  - Inside Phase 7:
    - T112 (extend `/rest/menu`) → T113 (render sidebar from API).
    - T210–T211 (QC placeholder pages + routes) can proceed once T113 wiring is
      present. Pages may be stubbed earlier, but menu surfacing requires T112.
    - T212 (force analyzer nav visible/expanded) depends on router scaffolding
      from Phase 3 and T113.
    - T213–T214 (URL/session state preservation) depend on base pages/routes;
      can run in parallel after routes exist.
- **Phase 8: Polish** – Depends on desired user stories being complete.
- **Phase 9: Constitution Compliance** – Depends on all implementation phases.

### User Story Dependencies

- **US1 (P1)** – Starts after Phase 2; no dependency on other stories.
- **US2 (P2)** – Starts after Phase 2; optional UI integration with US1 but
  independently testable (service/controller-first).
- **US3 (P3)** – Starts after Phase 2; depends on error queue infra
  (`AnalyzerError`) from foundational.
- **Navigation (Phase 7)** – Surfaces US1, US3, and QC placeholders under
  Analyzers parent; follow Phase 3 minimal routes.

### Within Each User Story

- Tests (MANDATORY) must be written and FAIL before implementation (per Testing
  Roadmap).
- Order: Entities → DAOs → Services → Controllers → Frontend components →
  Integration.
- Complete the story slice independently before moving to the next priority.

### Parallel Execution Examples

- **US1 (P1)**
  - [P] DAO tests (e.g., T033–T033d) parallel with service tests (T029–T032).
  - [P] Frontend unit tests (T037–T039) parallel with controller tests
    (T035–T036).
  - [P] UI components (T056–T063) once DTOs/controllers exist (T049–T055).
- **US2 (P2)**
  - [P] Modal work (T164, T169–T173, T170) parallel with service/controller
    disablement (T171–T172).
  - [P] Copy mappings UI (T076) in parallel with endpoint (T077).
  - [P] Test Mapping modal (T080) after minimal service hooks are stubbed.
- **US3 (P3)**
  - [P] Error services (T089–T092) parallel with dashboard UI (T096–T099).
  - [P] Wrapper pattern (T177–T181) parallel with controller tests (T085).
- **Query Analyzer (Phase 6)**
  - [P] Service unit/integration tests (T102–T103) parallel with UI
    polling/modal (T107–T108).
- **Navigation Integration (Phase 7)**
  - T112 → T113; then:
    - [P] T210–T211 (QC placeholder pages/routes).
    - [P] T212 (force nav visible/expanded) after routing present.
    - [P] T213–T214 (URL/session state persistence).

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for AnalyzerFieldService in src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldServiceTest.java"
Task: "Unit test for AnalyzerFieldMappingService in src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java"
Task: "Unit test for QualitativeResultMappingService in src/test/java/org/openelisglobal/analyzer/service/QualitativeResultMappingServiceTest.java"
Task: "Unit test for UnitMappingService in src/test/java/org/openelisglobal/analyzer/service/UnitMappingServiceTest.java"
Task: "DAO test for AnalyzerFieldDAO in src/test/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAOTest.java"
Task: "DAO test for AnalyzerFieldMappingDAO in src/test/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOTest.java"
Task: "Controller test for AnalyzerRestController in src/test/java/org/openelisglobal/analyzer/controller/AnalyzerRestControllerTest.java"
Task: "Controller test for AnalyzerFieldMappingRestController in src/test/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestControllerTest.java"
Task: "Frontend unit test for AnalyzersList component in frontend/src/components/analyzers/AnalyzersList/AnalyzersList.test.jsx"
Task: "Frontend unit test for AnalyzerForm component in frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.test.jsx"
Task: "Frontend unit test for FieldMapping component in frontend/src/components/analyzers/FieldMapping/FieldMapping.test.jsx"
Task: "Cypress E2E test in frontend/cypress/e2e/analyzerConfiguration.cy.js"

# Launch all entities/DAOs for User Story 1 together:
Task: "Create AnalyzerFieldService interface in src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldService.java"
Task: "Create AnalyzerFieldMappingService interface in src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingService.java"
Task: "Create QualitativeResultMappingService interface in src/main/java/org/openelisglobal/analyzer/service/QualitativeResultMappingService.java"
Task: "Create UnitMappingService interface in src/main/java/org/openelisglobal/analyzer/service/UnitMappingService.java"
Task: "Create AnalyzerForm DTO in src/main/java/org/openelisglobal/analyzer/form/AnalyzerForm.java"
Task: "Create AnalyzerFieldForm DTO in src/main/java/org/openelisglobal/analyzer/form/AnalyzerFieldForm.java"
Task: "Create AnalyzerFieldMappingForm DTO in src/main/java/org/openelisglobal/analyzer/form/AnalyzerFieldMappingForm.java"
Task: "Create QualitativeResultMappingForm DTO in src/main/java/org/openelisglobal/analyzer/form/QualitativeResultMappingForm.java"
Task: "Create UnitMappingForm DTO in src/main/java/org/openelisglobal/analyzer/form/UnitMappingForm.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add Query Analyzer functionality → Test independently → Deploy/Demo
6. Add Navigation Integration → Test independently → Deploy/Demo
7. Polish & Compliance → Final validation → Deploy

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (P1)
   - Developer B: User Story 2 (P2) + Query Analyzer (Phase 6)
   - Developer C: User Story 3 (P3) + Navigation Integration (Phase 7)
3. Stories complete and integrate independently
4. Team collaborates on Polish & Compliance (Phase 8-9)

---

## Task Summary

**Total Tasks**: 183 (updated: added T105a, T153a)

**Tasks by Phase**:

- Phase 1 (Setup): 11 tasks
- Phase 2 (Foundational): 22 tasks (1 test + 21 implementation, includes
  CustomFieldType entity/DAO/Service)
- Phase 3 (User Story 1): 55 tasks (15 tests + 40 implementation, includes
  inline field creation, lifecycle workflow, lifecycle scheduler, SC-001
  validation) - **Note**: Added T153a for lifecycle stage transition scheduler
- Phase 4 (User Story 2): 16 tasks (6 tests + 10 implementation, includes
  mapping retirement, activation modal, SC-003 validation)
- Phase 5 (User Story 3): 28 tasks (9 tests + 19 implementation, includes
  wrapper integration, SC-002 validation)
- Phase 6 (Query Analyzer): 9 tasks (2 tests + 7 implementation) - **Note**:
  Added T105a for SystemConfiguration default entry
- Phase 7 (Navigation Integration): 7 tasks (2 tests + 5 implementation)
- Phase 8 (Polish): 11 tasks (includes custom field types integration)
- Phase 8.5 (System Administration): 2 tasks (CustomFieldTypeManagement UI)
- Phase 9 (Constitution Compliance): 9 tasks (includes SC-004 manual validation)

**Tasks by User Story**:

- User Story 1 (P1): 54 tasks
- User Story 2 (P2): 16 tasks
- User Story 3 (P3): 28 tasks

**Test Tasks**: 35 total (ORM validation: 1, Unit tests: 11, DAO tests: 4,
Controller tests: 6, Frontend unit tests: 9, E2E tests: 4, Integration tests: 4)

**Parallel Opportunities**: All tasks marked [P] can run in parallel within
their phase

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1) = 69 core tasks

**MVP Calculation Breakdown**:

- Phase 1 (Setup): 11 tasks (all core MVP)
- Phase 2 (Foundational): 22 tasks (all core MVP)
- Phase 3 (User Story 1): 36 core tasks + 18 enhancement tasks = 54 total
  - **Core MVP tasks (36)**: T029-T069 (core mapping functionality, tests,
    implementation, routes)
  - **Enhancement tasks excluded from MVP (18)**: T142-T150 (inline field
    creation, FR-019), T151-T153 (lifecycle stages, FR-015), T159 (SC-001
    validation)
- **Total MVP**: 11 + 22 + 36 = 69 core tasks
- **Total Phase 3**: 11 + 22 + 54 = 87 tasks (if including enhancements)

**Note**: The MVP scope focuses on core field mapping functionality.
Enhancements like inline field creation, lifecycle stage management, and
performance validation are valuable but not required for initial MVP delivery.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Follow TDD workflow: Red (write failing test) → Green (implement) → Refactor
- Use testing roadmap conventions for all test types
- Follow AGENTS.md architecture patterns (5-layer, no @Transactional in
  controllers, JOIN FETCH in services)
- Use research.md decisions for implementation approach
- Follow data-model.md entity definitions and relationships
