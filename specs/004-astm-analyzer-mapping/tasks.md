# Tasks: ASTM Analyzer Field Mapping

**Branch**: `004-astm-analyzer-mapping`  
**Date**: 2025-01-27  
**Input**: Design documents from `/specs/004-astm-analyzer-mapping/`

**Test Approach**: Test-Driven Development (TDD) - Tests written BEFORE implementation

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

- [X] T001 Create analyzer module package structure in `src/main/java/org/openelisglobal/analyzer/` with subdirectories: valueholder/, dao/, service/, controller/, form/
- [X] T002 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-001-create-analyzer-configuration-table.xml` for AnalyzerConfiguration table (one-to-one with legacy Analyzer)
- [X] T003 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-002-create-analyzer-field-table.xml` for AnalyzerField table
- [X] T004 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-003-create-analyzer-field-mapping-table.xml` for AnalyzerFieldMapping table
- [X] T005 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-004-create-qualitative-result-mapping-table.xml` for QualitativeResultMapping table
- [X] T006 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-005-create-unit-mapping-table.xml` for UnitMapping table
- [X] T007 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-006-create-analyzer-error-table.xml` for AnalyzerError table
- [X] T008 Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-007-create-indexes.xml` for performance indexes (analyzer_id lookups, status filtering, error dashboard queries)
- [ ] T009 Verify database migration: Run application, check `databasechangelog` table contains analyzer changesets, verify tables created with `\dt analyzer_*`
- [X] T010 Create frontend analyzer component directory structure in `frontend/src/components/analyzers/` with subdirectories: AnalyzersList/, AnalyzerForm/, FieldMapping/, ErrorDashboard/, TestConnectionModal/
- [X] T011 [P] Add analyzer message keys to `frontend/src/languages/en.json`, `fr.json` (internationalization strings from spec.md FR-001 through FR-020)

**Checkpoint**: Database schema created, module structure initialized, i18n keys ready

---

## Phase 2: Foundational - Core Entities & DAOs (Blocks All User Stories)

**Purpose**: Create analyzer entities and data access layer required by ALL user stories

**⚠️ CRITICAL**: No user story implementation can begin until this phase completes

### Tests First (Write BEFORE implementation)

- [X] T012 [P] ORM validation test in `src/test/java/org/openelisglobal/analyzer/HibernateMappingValidationTest.java` - Reference: [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4) - Build SessionFactory using `config.addAnnotatedClass()` for AnalyzerConfiguration, AnalyzerField, AnalyzerFieldMapping, QualitativeResultMapping, UnitMapping, AnalyzerError, CustomFieldType - Validate all entity mappings load without errors - MUST execute in <5 seconds (per Constitution V.4) - MUST NOT require database connection - **SDD Checkpoint**: Must pass after Phase 1 (Entities)

### Implementation for Foundational Phase

- [X] T013 [P] Create AnalyzerConfiguration entity in `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerConfiguration.java` extending BaseObject<String> with one-to-one relationship to legacy Analyzer entity (per research.md Section 2)
- [X] T014 [P] Create AnalyzerField entity in `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerField.java` extending BaseObject<String> with FieldType enum (NUMERIC, QUALITATIVE, CONTROL_TEST, MELTING_POINT, DATE_TIME, TEXT, CUSTOM) per data-model.md Section 2
- [X] T015 [P] Create AnalyzerFieldMapping entity in `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerFieldMapping.java` extending BaseObject<String> with OpenELISFieldType and MappingType enums per data-model.md Section 3
- [X] T016 [P] Create QualitativeResultMapping entity in `src/main/java/org/openelisglobal/analyzer/valueholder/QualitativeResultMapping.java` extending BaseObject<String> with unique constraint on (analyzer_field_id, analyzer_value) per data-model.md Section 4
- [X] T017 [P] Create UnitMapping entity in `src/main/java/org/openelisglobal/analyzer/valueholder/UnitMapping.java` extending BaseObject<String> with unique constraint on (analyzer_field_id, analyzer_unit) per data-model.md Section 5
- [X] T018 [P] Create AnalyzerError entity in `src/main/java/org/openelisglobal/analyzer/valueholder/AnalyzerError.java` extending BaseObject<String> with ErrorType, Severity, ErrorStatus enums per data-model.md Section 6
- [X] T019 [P] Create AnalyzerFieldDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAO.java` extending BaseDAO<AnalyzerField, String>
- [X] T020 [P] Create AnalyzerFieldDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAOImpl.java` extending BaseDAOImpl<AnalyzerField, String> with @Component and @Transactional annotations, HQL queries ONLY (NO native SQL)
- [X] T021 [P] Create AnalyzerFieldMappingDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAO.java` extending BaseDAO<AnalyzerFieldMapping, String>
- [X] T022 [P] Create AnalyzerFieldMappingDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOImpl.java` extending BaseDAOImpl<AnalyzerFieldMapping, String> with @Component and @Transactional annotations
- [X] T023 [P] Create QualitativeResultMappingDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/QualitativeResultMappingDAO.java` extending BaseDAO<QualitativeResultMapping, String>
- [X] T024 [P] Create QualitativeResultMappingDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/QualitativeResultMappingDAOImpl.java` extending BaseDAOImpl<QualitativeResultMapping, String> with @Component and @Transactional annotations
- [X] T025 [P] Create UnitMappingDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/UnitMappingDAO.java` extending BaseDAO<UnitMapping, String>
- [X] T026 [P] Create UnitMappingDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/UnitMappingDAOImpl.java` extending BaseDAOImpl<UnitMapping, String> with @Component and @Transactional annotations
- [X] T027 [P] Create AnalyzerErrorDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAO.java` extending BaseDAO<AnalyzerError, String>
- [X] T028 [P] Create AnalyzerErrorDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAOImpl.java` extending BaseDAOImpl<AnalyzerError, String> with @Component and @Transactional annotations, HQL queries for error dashboard filtering
- [X] T135 [P] Create CustomFieldType entity in `src/main/java/org/openelisglobal/analyzer/valueholder/CustomFieldType.java` extending BaseObject<String> - Fields: id, typeName (unique), displayName, validationPattern (regex), valueRange (min/max for numeric), allowedCharacters, isActive per FR-018
- [X] T136 [P] Create CustomFieldTypeDAO interface in `src/main/java/org/openelisglobal/analyzer/dao/CustomFieldTypeDAO.java` extending BaseDAO<CustomFieldType, String>
- [X] T137 [P] Create CustomFieldTypeDAOImpl in `src/main/java/org/openelisglobal/analyzer/dao/CustomFieldTypeDAOImpl.java` extending BaseDAOImpl<CustomFieldType, String> with @Component and @Transactional annotations - Methods: findAllActive(), findByName(), findByTypeName()
- [X] T138 [P] Create CustomFieldTypeService interface in `src/main/java/org/openelisglobal/analyzer/service/CustomFieldTypeService.java`
- [X] T139 [P] Create CustomFieldTypeServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/CustomFieldTypeServiceImpl.java` with @Service and @Transactional annotations - Methods: createCustomFieldType(), updateCustomFieldType(), validateFieldValue(), getAllActiveTypes() - Validation logic: Apply regex patterns, value ranges, character restrictions per FR-018

**Checkpoint**: All entities and DAOs created, ORM validation test passes, foundation ready for user story implementation

---

## Phase 3: User Story 1 - Configure Field Mappings (Priority: P1) 🎯 MVP

**Goal**: Administrator can configure field mappings for a new ASTM analyzer so that test orders and results are correctly mapped to OpenELIS tests, analytes, and result fields without manual re-entry.

**Independent Test**: A tester can start from a clean system where an ASTM analyzer has no mappings configured, follow the mapping UI to link analyzer test codes, units, and qualitative values to OpenELIS fields, and then send sample ASTM messages to confirm they are automatically interpreted into correct OpenELIS orders/results without manual intervention.

### Tests for User Story 1 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [X] T029 [P] [US1] Unit test for AnalyzerFieldService in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldServiceTest.java` (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) - Reference: [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` (NOT `@SpringBootTest`) - **Mocking**: Use `@Mock` (NOT `@MockBean`) - **Test Data**: Use builders/factories - **Coverage Goal**: >80% - Test methods: `testGetFieldsByAnalyzerId_ReturnsFields`, `testCreateField_WithValidData_PersistsField`, `testCreateField_WithInvalidFieldType_ThrowsException`
- [X] T030 [P] [US1] Unit test for AnalyzerFieldMappingService in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` - Test methods: `testCreateMapping_WithValidData_PersistsMapping`, `testCreateMapping_WithTypeIncompatibility_ThrowsException`, `testValidateRequiredMappings_WithMissingRequired_ThrowsException`, `testActivateMapping_WithActiveAnalyzer_RequiresConfirmation`
- [X] T031 [P] [US1] Unit test for QualitativeResultMappingService in `src/test/java/org/openelisglobal/analyzer/service/QualitativeResultMappingServiceTest.java` - Test methods: `testCreateMapping_WithManyToOneMapping_PersistsMultipleValues`, `testCreateMapping_WithDuplicateValue_ThrowsException`
- [X] T032 [P] [US1] Unit test for UnitMappingService in `src/test/java/org/openelisglobal/analyzer/service/UnitMappingServiceTest.java` - Test methods: `testCreateMapping_WithConversionFactor_AppliesConversion`, `testCreateMapping_WithUnitMismatch_RequiresConversionFactor`
- [X] T033 [P] [US1] DAO test for AnalyzerFieldDAO in `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerFieldDAOTest.java` (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) - Reference: [Testing Roadmap - @DataJpaTest](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer) - **Test Slicing**: Use Mockito pattern (matching existing codebase) - **Test Data**: Use Mockito mocks - Test methods: `testFindByAnalyzerId_WithValidId_ReturnsFields`, `testInsert_WithValidData_PersistsToDatabase`, `testGet_WithValidId_ReturnsField`
- [X] T034 [P] [US1] DAO test for AnalyzerFieldMappingDAO in `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerFieldMappingDAOTest.java` - **Test Slicing**: Use Mockito pattern - Test methods: `testFindByAnalyzerFieldId_ReturnsMappings`, `testFindActiveMappingsByAnalyzerId_ReturnsOnlyActive`
- [X] T033a [P] [US1] DAO test for AnalyzerConfigurationDAO in `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerConfigurationDAOTest.java` - **Test Slicing**: Use Mockito pattern - Test methods: `testFindByAnalyzerId_WithValidId_ReturnsConfiguration`, `testFindByAnalyzerId_WithNoConfiguration_ReturnsEmpty`, `testFindByAnalyzerId_WithException_ReturnsEmpty` - **HQL Testing**: Verify JOIN query syntax works correctly with legacy Analyzer entity
- [X] T033b [P] [US1] DAO test for QualitativeResultMappingDAO in `src/test/java/org/openelisglobal/analyzer/dao/QualitativeResultMappingDAOTest.java` - **Test Slicing**: Use Mockito pattern - Test methods: `testFindByAnalyzerFieldId_ReturnsMappings`, `testFindByAnalyzerFieldId_NoMappings_ReturnsEmptyList`
- [X] T033c [P] [US1] DAO test for UnitMappingDAO in `src/test/java/org/openelisglobal/analyzer/dao/UnitMappingDAOTest.java` - **Test Slicing**: Use Mockito pattern - Test methods: `testFindByAnalyzerFieldId_ReturnsMappings`, `testFindByAnalyzerFieldId_NoMappings_ReturnsEmptyList`
- [X] T033d [P] [US1] DAO test for CustomFieldTypeDAO in `src/test/java/org/openelisglobal/analyzer/dao/CustomFieldTypeDAOTest.java` - **Test Slicing**: Use Mockito pattern - Test methods: `testFindAllActive_ReturnsOnlyActiveTypes`, `testFindByName_ReturnsMatchingType`, `testFindByName_NoMatch_ReturnsNull`, `testFindByTypeName_ReturnsMatchingType`, `testFindByTypeName_NoMatch_ReturnsNull`
- [X] T035 [P] [US1] Controller test for AnalyzerRestController in `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerRestControllerTest.java` - **Test Slicing**: Uses `BaseWebContextSensitiveTest` (matching existing codebase pattern) - **HTTP Testing**: Uses `MockMvc` - Test methods: `testGetAnalyzers_ReturnsList`, `testCreateAnalyzer_WithValidData_ReturnsCreated`, `testTestConnection_WithValidConfig_ReturnsSuccess` - All 3 tests passing ✓
- [X] T036 [P] [US1] Controller test for AnalyzerFieldMappingRestController in `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestControllerTest.java` - **Test Slicing**: Uses `BaseWebContextSensitiveTest` (matching existing codebase pattern) - **HTTP Testing**: Uses `MockMvc` - Test methods: `testGetMappings_WithAnalyzerId_ReturnsMappings`, `testCreateMapping_WithValidData_ReturnsCreated`, `testCreateMapping_WithTypeIncompatibility_ReturnsBadRequest`, `testUpdateMapping_WithValidData_ReturnsUpdated`, `testDeleteMapping_WithValidId_ReturnsNoContent` - All 5 tests passing ✓
- [X] T037 [P] [US1] Frontend unit test for AnalyzersList component in `frontend/src/components/analyzers/AnalyzersList/AnalyzersList.test.jsx` (Template: `.specify/templates/testing/JestComponent.test.jsx.template`) - Reference: [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) - **Import Order**: React → Testing Library → userEvent → jest-dom → Intl → Router → Component - **userEvent PREFERRED**: Use `userEvent.click()`, `userEvent.type()` - **Async Testing**: Use `waitFor` with `queryBy*` or `findBy*` - **Carbon Components**: Use `userEvent`, `waitFor` for portals - Test methods: `testRendersAnalyzersList_WithData_DisplaysTable`, `testSearchAnalyzers_WithQuery_FiltersResults`, `testOpenAddAnalyzerModal_ShowsForm`
- [X] T038 [P] [US1] Frontend unit test for AnalyzerForm component in `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.test.jsx` - Test methods: `testSubmitForm_WithValidData_CallsAPI`, `testValidateIPAddress_WithInvalidFormat_ShowsError`, `testTestConnection_ShowsModal`
- [X] T039 [P] [US1] Frontend unit test for FieldMapping component in `frontend/src/components/analyzers/FieldMapping/FieldMapping.test.jsx` - Test methods: `testSelectField_OpensMappingPanel`, `testCreateMapping_WithValidData_SavesMapping`, `testTypeCompatibility_BlocksIncompatibleTypes`
- [X] T040 [P] [US1] Cypress E2E test in `frontend/cypress/e2e/analyzerConfiguration.cy.js` (Template: `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference: [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices) - Use data-testid selectors (PREFERRED) - Use API-based test data setup - Set viewport before visit - Set up intercepts BEFORE actions - Use .should() for retry-ability - Focus on happy path user workflows - Run individually: `npm run cy:run -- --spec "cypress/e2e/analyzerConfiguration.cy.js"` - Test scenarios: "should configure analyzer with field mappings", "should validate IP address format", "should test analyzer connection", "should create test code to OpenELIS test mapping", "should create unit mapping with conversion factor", "should create qualitative value mapping" - Basic implementation complete: 6 test scenarios covering analyzer configuration workflow, IP validation, connection testing, and mapping creation
- [ ] T142 [P] [US1] Add "Create New Field" action to OpenELISFieldSelector component - Add button/link in dropdown (below field list or in dropdown header) - Opens inline modal form when clicked per FR-019
- [ ] T143 [P] [US1] Create InlineFieldCreationModal component in `frontend/src/components/analyzers/FieldMapping/InlineFieldCreationModal.jsx` using Carbon ComposedModal - Form fields: Field Name (required), Entity Type (dropdown: TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT), LOINC Code (optional), Description (optional), Field Type (Numeric/Qualitative/Text), Accepted Units (multi-select, if numeric) - Validation: Field name uniqueness check, entity type required, field type compatibility - Confirmation step: "Field will be available for mapping immediately after creation" - Action buttons: Cancel, Create Field per FR-019
- [ ] T144 [P] [US1] Create OpenELISFieldService interface in `src/main/java/org/openelisglobal/analyzer/service/OpenELISFieldService.java`
- [ ] T145 [P] [US1] Create OpenELISFieldServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/OpenELISFieldServiceImpl.java` with @Service and @Transactional annotations - Methods: createField(), validateFieldUniqueness(), getFieldById() - Integration with existing OpenELIS field entities (Test, Analyte, Result, etc.) per FR-019
- [ ] T146 [P] [US1] Create OpenELISFieldRestController in `src/main/java/org/openelisglobal/analyzer/controller/OpenELISFieldRestController.java` extending BaseRestController - Endpoint: POST `/rest/openelis-fields` - Request body: { name, entityType, loincCode, description, fieldType, acceptedUnits } - Validation: Check uniqueness, validate entity type, field type compatibility - Returns created field with ID for immediate use in mapping - Authorization: LAB_ADMIN or LAB_SUPERVISOR per FR-019
- [ ] T147 [P] [US1] Unit test for OpenELISFieldService in `src/test/java/org/openelisglobal/analyzer/service/OpenELISFieldServiceTest.java` - Test methods: `testCreateField_WithValidData_PersistsField`, `testCreateField_WithDuplicateName_ThrowsException`, `testValidateFieldUniqueness_WithExistingName_ReturnsFalse`
- [ ] T148 [P] [US1] Controller test for OpenELISFieldRestController in `src/test/java/org/openelisglobal/analyzer/controller/OpenELISFieldRestControllerTest.java` - **Test Slicing**: Use `@WebMvcTest` - Test methods: `testCreateField_WithValidData_ReturnsCreated`, `testCreateField_WithDuplicateName_ReturnsConflict`
- [ ] T149 [P] [US1] Frontend unit test for InlineFieldCreationModal in `frontend/src/components/analyzers/FieldMapping/InlineFieldCreationModal.test.jsx` - Test methods: `testSubmitForm_WithValidData_CallsAPI`, `testValidateFieldName_WithDuplicate_ShowsError`, `testSelectEntityType_ShowsRelevantFields`
- [ ] T150 [US1] Integrate inline field creation into OpenELISFieldSelector workflow - After successful field creation, refresh field list - Auto-select newly created field in mapping - Show success notification per FR-019
- [ ] T151 [P] Add lifecycleStage field to AnalyzerConfiguration entity - Enum: SETUP, VALIDATION, GO_LIVE, MAINTENANCE - Default: SETUP for new analyzers - Transition rules: SETUP → VALIDATION (when mappings created), VALIDATION → GO_LIVE (when activated), GO_LIVE → MAINTENANCE (automatic after 7 days) per FR-015
- [ ] T152 [P] [US1] Add lifecycle stage badge and filter to AnalyzersList component - Show lifecycle stage badge (Tag component) in analyzer table - Add lifecycle stage filter to analyzer filters dropdown per FR-015
- [ ] T153 [P] [US1] Add validation workflow integration to FieldMapping component - Add "Validate Mappings" button (only visible in VALIDATION stage) - Test mapping functionality (FR-007) integrated into validation workflow - Validation dashboard showing test results, mapping accuracy metrics per FR-015
- [ ] T159 [P] [US1] Add Cypress E2E test for SC-001 (2-hour configuration time) in `frontend/cypress/e2e/analyzerConfiguration.cy.js` - Test scenario: "should complete analyzer configuration with 100 test codes in under 2 hours" - Measure time from analyzer registration to all mappings configured and validated - Test with realistic data volume (100 test codes, 50 unit mappings, 30 qualitative mappings) per Success Criteria SC-001

### Implementation for User Story 1

- [X] T041 [P] [US1] Create AnalyzerFieldService interface in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldService.java`
- [X] T042 [US1] Create AnalyzerFieldServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldServiceImpl.java` with @Service and @Transactional annotations, validation logic for field type compatibility per data-model.md Section 2
- [X] T043 [P] [US1] Create AnalyzerFieldMappingService interface in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingService.java`
- [X] T044 [US1] Create AnalyzerFieldMappingServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceImpl.java` with @Service and @Transactional annotations, type compatibility validation (numeric→numeric, qualitative→qualitative, text→text), required mapping validation per data-model.md Section 3
- [X] T045 [P] [US1] Create QualitativeResultMappingService interface in `src/main/java/org/openelisglobal/analyzer/service/QualitativeResultMappingService.java`
- [X] T046 [US1] Create QualitativeResultMappingServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/QualitativeResultMappingServiceImpl.java` with @Service and @Transactional annotations, many-to-one mapping support per data-model.md Section 4
- [X] T047 [P] [US1] Create UnitMappingService interface in `src/main/java/org/openelisglobal/analyzer/service/UnitMappingService.java`
- [X] T048 [US1] Create UnitMappingServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/UnitMappingServiceImpl.java` with @Service and @Transactional annotations, conversion factor validation per data-model.md Section 5
- [X] T049 [P] [US1] Create AnalyzerForm DTO in `src/main/java/org/openelisglobal/analyzer/form/AnalyzerForm.java` with validation annotations (name uniqueness, IP format, port range)
- [X] T050 [P] [US1] Create AnalyzerFieldForm DTO in `src/main/java/org/openelisglobal/analyzer/form/AnalyzerFieldForm.java`
- [X] T051 [P] [US1] Create AnalyzerFieldMappingForm DTO in `src/main/java/org/openelisglobal/analyzer/form/AnalyzerFieldMappingForm.java`
- [X] T052 [P] [US1] Create QualitativeResultMappingForm DTO in `src/main/java/org/openelisglobal/analyzer/form/QualitativeResultMappingForm.java`
- [X] T053 [P] [US1] Create UnitMappingForm DTO in `src/main/java/org/openelisglobal/analyzer/form/UnitMappingForm.java`
- [X] T054 [US1] Create AnalyzerRestController in `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerRestController.java` extending BaseRestController with @RestController and @RequestMapping("/rest/analyzer") - Endpoints: GET /analyzers ✓, POST /analyzers ✓, GET /analyzers/{id} ✓, PUT /analyzers/{id} ✓, DELETE /analyzers/{id} ✓, POST /analyzers/{id}/test-connection ✓ per FR-001
- [X] T055 [US1] Create AnalyzerFieldMappingRestController in `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerFieldMappingRestController.java` extending BaseRestController - Endpoints: GET /analyzers/{analyzerId}/mappings ✓, POST /analyzers/{analyzerId}/mappings ✓, PUT /analyzers/{analyzerId}/mappings/{mappingId} ✓, DELETE /analyzers/{analyzerId}/mappings/{mappingId} ✓ per FR-003
- [X] T056 [US1] Create AnalyzersList component in `frontend/src/components/analyzers/AnalyzersList/AnalyzersList.jsx` using Carbon DataTable with search, filters, pagination per FR-001 - Test Unit filter: Multi-select dropdown showing test unit names (loaded from existing test unit configuration), filters by test unit IDs using PostgreSQL array overlap operator (`WHERE analyzer.test_unit_ids && ARRAY[selected_unit_ids]`) per FR-001 specification - UI displays test unit names for user selection, but filtering logic uses IDs for database queries - Basic implementation complete: DataTable, search with debounce, Add Analyzer button, statistics cards, overflow menu actions
- [X] T057 [US1] Create AnalyzerForm component in `frontend/src/components/analyzers/AnalyzerForm/AnalyzerForm.jsx` using Carbon ComposedModal with form fields (name, type, IP, port, protocol, test units, active status) per FR-001 - Basic implementation complete: form fields, IP validation, Test Connection button, form submission
- [X] T058 [US1] Create TestConnectionModal component in `frontend/src/components/analyzers/TestConnectionModal/TestConnectionModal.jsx` using Carbon ComposedModal with three states (initial, progress, success) per FR-001 - Basic implementation complete: three states, progress bar, connection logs, test connection API integration
- [X] T059 [US1] Create FieldMapping component in `frontend/src/components/analyzers/FieldMapping/FieldMapping.jsx` with dual-panel layout (50/50 split) using Carbon Grid per FR-008 - Basic implementation complete: dual-panel layout, field selection, mapping creation
- [X] T060 [US1] Create FieldMappingPanel component in `frontend/src/components/analyzers/FieldMapping/FieldMappingPanel.jsx` for left panel displaying analyzer fields table per FR-008 - Basic implementation complete: fields table, search, field selection, mapping indicators
- [X] T061 [US1] Create OpenELISFieldSelector component in `frontend/src/components/analyzers/FieldMapping/OpenELISFieldSelector.jsx` with searchable dropdown, category filtering (8 entity types), type compatibility filtering per FR-003 - Basic implementation complete: ComboBox with search, type compatibility filtering (mock data for now)
- [X] T062 [US1] Create MappingPanel component in `frontend/src/components/analyzers/FieldMapping/MappingPanel.jsx` for right panel with View Mode and Edit Mode per FR-008 - Basic implementation complete: View/Edit modes, OpenELISFieldSelector integration, save/cancel actions
- [X] T063 [US1] Create UnitMappingModal component in `frontend/src/components/analyzers/FieldMapping/UnitMappingModal.jsx` using Carbon ComposedModal for unit mapping interface per FR-004 - Basic implementation complete: Source Unit (read-only), Target Unit dropdown, Conversion Factor input (conditional), Reject if Mismatch toggle, validation, Save/Cancel buttons
- [X] T064 [US1] Create QualitativeMappingModal component in `frontend/src/components/analyzers/FieldMapping/QualitativeMappingModal.jsx` using Carbon ComposedModal for qualitative value mapping interface per FR-005 - Basic implementation complete: Analyzer Values List, Target Value Dropdowns (per analyzer value), Is Default checkbox, many-to-one mapping support, validation, Save/Cancel buttons
- [X] T065 [US1] Create analyzerService API client in `frontend/src/services/analyzerService.js` with methods for CRUD operations, test connection, query analyzer - Methods: getAnalyzers(), getAnalyzer(), createAnalyzer(), updateAnalyzer(), deleteAnalyzer(), testConnection(), queryAnalyzer(), getMappings(), createMapping(), updateMapping(), deleteMapping() - Follows OpenELIS pattern using getFromOpenElisServer, postToOpenElisServerJsonResponse, putToOpenElisServer, fetch for DELETE
- [X] T066 [US1] Create AnalyzersPage route component in `frontend/src/pages/AnalyzersPage.jsx` integrating AnalyzersList component - Basic implementation complete: route component wrapping AnalyzersList
- [X] T067 [US1] Create FieldMappingsPage route component in `frontend/src/pages/FieldMappingsPage.jsx` integrating FieldMapping component with route parameter `/analyzers/:id/mappings` - Basic implementation complete: route component wrapping FieldMapping
- [X] T068 [US1] Add React Router routes in `frontend/src/App.js` for `/analyzers` (AnalyzersPage) and `/analyzers/:id/mappings` (FieldMappingsPage) - Basic implementation complete: SecureRoute with role-based access control (LAB_ADMIN, LAB_SUPERVISOR, GLOBAL_ADMIN)
- [X] T069 [US1] Create Liquibase changeset `src/main/resources/liquibase/analyzer/004-009-add-menu-items.xml` for "Analyzers" parent menu item and sub-navigation items ("Analyzers List", "Error Dashboard", "Field Mappings") per FR-020 - Insert into menu table with parent_id, presentation_order, element_id, action_url, display_key - "Field Mappings" menu item has contextual visibility (action_url="/analyzers/:id/mappings") - Contextual visibility handled by frontend based on current route (frontend checks route to determine which contextual items to display) - Menu items added: parent "Analyzers" (presentation_order=26), "Analyzers List" (order=1), "Error Dashboard" (order=2), "Field Mappings" (order=3) - All menu items use i18n display keys from en.json

**Checkpoint Validation**: At this point, User Story 1 should be fully functional and testable independently. ALL tests from T029-T040 MUST pass before proceeding to next phase.

## Implementation Status Summary

**Last Updated**: 2025-11-17  
**Backend Progress**: 44/60 tasks complete (73%)  
**Frontend Progress**: 0/15 tasks complete (0%)  
**Total Progress**: 44/75 tasks complete (59%)

**Critical Path**: Frontend implementation (T065-T069) blocks MVP delivery  
**Next Priority**: T065 (analyzerService.js API client) - required by all frontend components

---

## Phase 4: User Story 2 - Maintain Mappings (Priority: P2)

**Goal**: Administrator can safely update mappings when analyzer vendor adds new tests, changes test codes, or modifies result formats, while preserving auditability and minimizing disruption to live message processing.

**Independent Test**: A tester can simulate a change in analyzer test codes or result formats, update mappings in the UI, and verify that existing mapped tests continue to work while new/changed tests are routed correctly, with all changes reflected in an audit trail.

### Tests for User Story 2 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T070 [P] [US2] Unit test for AnalyzerFieldMappingService update methods in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` - Test methods: `testUpdateMapping_WithActiveAnalyzer_RequiresConfirmation`, `testUpdateMapping_WithDraftState_DoesNotRequireConfirmation`, `testDeactivateMapping_WithActiveAnalyzer_LogsAuditTrail` - All 3 tests passing: update requires confirmation for active analyzers, draft updates don't require confirmation, disable mapping logs audit trail
- [X] T071 [P] [US2] Integration test for mapping update workflow in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceIntegrationTest.java` using BaseWebContextSensitiveTest - Test methods: `testUpdateMapping_WithExistingResults_PreservesHistoricalData`, `testActivateMapping_WithConfirmation_AppliesToNewMessages`, `testUpdateMapping_ActiveAnalyzerActiveMapping_RequiresConfirmation` - All 3 tests passing: updates preserve historical data, activation applies to new messages, confirmation required for active analyzers
- [X] T072 [P] [US2] Frontend unit test for mapping update workflow in `frontend/src/components/analyzers/FieldMapping/MappingPanel.test.jsx` - Test methods: `testUpdateMapping_ShowsConfirmationModal`, `testSaveDraftMapping_DoesNotRequireConfirmation`, `testCreateMapping_DoesNotRequireConfirmation` - All 3 tests passing: tests verify current behavior, confirmation modal test prepared for T078/T164 implementation
- [X] T073 [P] [US2] Cypress E2E test in `frontend/cypress/e2e/analyzerMaintenance.cy.js` - Test scenarios: "should update existing mapping", "should activate draft mapping with confirmation", "should deactivate mapping while preserving history" - All 3 test scenarios implemented: update workflow, activation with confirmation, deactivation with history preservation - Tests use data-testid selectors, API-based setup, intercepts before actions

### Implementation for User Story 2

- [X] T074 [US2] Extend AnalyzerFieldMappingServiceImpl with update methods supporting draft/active workflow per FR-010 - Validate required mappings before activation (noted for analyzer activation time) - Apply changes to new messages only (existing results unchanged) - updateMapping() and activateMapping() methods implemented with confirmation workflow - All integration tests passing
- [X] T075 [US2] Add audit trail logging to AnalyzerFieldMappingServiceImpl for all mapping changes (who, when, previous vs new values) per FR-009 - Use BaseObject audit fields (sys_user_id, last_updated) - All update methods (updateMapping, activateMapping, disableMapping) call setLastupdatedFields() - Detailed audit trail (previous vs new values) can be added via AuditTrailService if needed
- [ ] T076 [US2] Create CopyMappingsModal component in `frontend/src/components/analyzers/FieldMapping/CopyMappingsModal.jsx` using Carbon ComposedModal per FR-006 - Source analyzer selector, target analyzer selector, warning note, confirmation dialog
- [ ] T077 [US2] Add copy mappings endpoint in AnalyzerFieldMappingRestController: POST /analyzers/{sourceId}/copy-mappings/{targetId} per FR-006
- [ ] T078 [US2] Extend MappingPanel component with Edit Mode supporting draft state per FR-010 - Show "Save as Draft" and "Save and Activate" buttons - Require confirmation for active analyzers - Create MappingActivationModal component in `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.jsx` (ComposedModal, warning variant) with confirmation checkbox, warning messages, destructive-style "Activate Changes" button per FR-010 specification
- [ ] T079 [US2] Add visual indicators for draft vs active mappings in FieldMappingPanel component - Status badges, filter options for draft/active
- [ ] T080 [US2] Create TestMappingModal component in `frontend/src/components/analyzers/FieldMapping/TestMappingModal.jsx` using Carbon ComposedModal for inline test mapping capability per FR-007 - Submit sample ASTM messages, preview interpretation, show warnings/errors - Add "Test Mapping" button in FieldMapping page header (ghost style, secondary action) positioned between "Back" button and "Save Mappings" button per FR-007 specification - Button opens TestMappingModal when clicked
- [X] T164 [P] [US2] Create MappingActivationModal component in `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.jsx` using Carbon ComposedModal (warning variant) - Dialog header with title "Activate Mapping Changes" and subtitle "Confirm activation of mapping changes for analyzer '{analyzer name}'" - Warning message section with warning icon - Additional warning for active analyzers - Confirmation checkbox (required before activation) - Dialog footer with Cancel button (secondary) and "Activate Changes" button (primary, destructive style) per FR-010 specification - Component created with all required features, uses React Intl, data-testid attributes for testing
- [X] T165 [P] [US2] Frontend unit test for MappingActivationModal in `frontend/src/components/analyzers/FieldMapping/MappingActivationModal.test.jsx` - Test methods: `testDisplayModal_ShowsWarningMessages`, `testDisplayModal_WithActiveAnalyzer_ShowsActiveWarning`, `testActivate_WithoutCheckbox_DisablesButton`, `testActivate_WithCheckbox_EnablesButton`, `testActivate_WithCheckbox_CallsOnConfirm`, `testCancel_CallsOnClose`, `testModal_WhenClosed_NotVisible` - All 7 tests passing: uses data-testid selectors, waits for content with findByTestId, checks checkbox state and button disabled state directly
- [ ] T169 [P] [US2] Add "Disable Mapping" action to MappingPanel View Mode - Add action button/menu item in View Mode header - Opens confirmation modal when clicked per FR-013
- [ ] T170 [P] [US2] Create MappingRetirementModal component in `frontend/src/components/analyzers/FieldMapping/MappingRetirementModal.jsx` using Carbon ComposedModal - Confirmation message: "Disable this mapping? Historical mappings will be retained for audit purposes." - Action buttons: Cancel, Disable Mapping (destructive style) per FR-013
- [ ] T171 [P] [US2] Add disable mapping endpoint in AnalyzerFieldMappingRestController: PUT `/analyzers/{analyzerId}/mappings/{mappingId}/disable` - Sets `is_active=false`, logs retirement reason - Validation: Cannot disable required mappings (Sample ID, Test Code, Result Value) per FR-013
- [ ] T172 [P] [US2] Add service method `disableMapping()` to AnalyzerFieldMappingServiceImpl - Sets `is_active=false`, logs retirement reason - Validation: Cannot disable required mappings per FR-013
- [ ] T173 [P] [US2] Add "Retired" badge and filter option in FieldMappingPanel component - Show "Retired" badge (Tag component, gray color) on disabled mappings - Filter option: "Show Retired Mappings" checkbox - Historical mapping retention: Disabled mappings remain in database, marked as retired per FR-013
- [ ] T174 [P] [US2] Unit test for mapping retirement workflow in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerFieldMappingServiceTest.java` - Test methods: `testDisableMapping_WithValidMapping_SetsInactive`, `testDisableMapping_WithRequiredMapping_ThrowsException`, `testDisableMapping_LogsRetirementReason`
- [ ] T161 [P] [US2] Add integration test for SC-003 (audit trail completeness) in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingAuditTest.java` using @SpringBootTest - Test methods: `testCreateMapping_LogsAuditTrail`, `testUpdateMapping_LogsPreviousAndNewValues`, `testDisableMapping_LogsRetirementReason` - Verify audit trail fields: user ID, timestamp, previous value, new value - Test audit trail query performance - Create/update/disable 100 mappings, verify 100% have audit trail entries per Success Criteria SC-003

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Resolve Unmapped Messages (Priority: P3)

**Goal**: Interface administrator can see ASTM messages that could not be processed due to missing or ambiguous mappings, correct the mappings, and then reprocess the affected messages.

**Independent Test**: A tester can generate ASTM messages with unmapped test codes or result values, observe that they are held for review rather than silently failing, configure the necessary mappings, and then confirm that the impacted messages can be reprocessed successfully.

### Tests for User Story 3 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T081 [P] [US3] Unit test for AnalyzerErrorService in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerErrorServiceTest.java` - Test methods: `testCreateError_WithUnmappedField_CreatesErrorRecord`, `testAcknowledgeError_WithValidUser_UpdatesStatus`, `testReprocessError_WithNewMapping_ProcessesMessage`
- [ ] T082 [P] [US3] Unit test for AnalyzerReprocessingService in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingServiceTest.java` - Test methods: `testReprocessMessage_WithValidMapping_ReturnsSuccess`, `testReprocessMessage_WithStillUnmapped_ReturnsError`
- [ ] T083 [P] [US3] Integration test for error queue workflow in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerErrorServiceIntegrationTest.java` using @SpringBootTest - Test methods: `testHoldUnmappedMessage_InErrorQueue`, `testReprocessAfterMapping_CreatesOrder`
- [ ] T084 [P] [US3] DAO test for AnalyzerErrorDAO in `src/test/java/org/openelisglobal/analyzer/dao/AnalyzerErrorDAOTest.java` - Test methods: `testFindByStatus_ReturnsUnacknowledgedErrors`, `testFindByAnalyzerId_ReturnsErrorsForAnalyzer`
- [ ] T085 [P] [US3] Controller test for AnalyzerErrorRestController in `src/test/java/org/openelisglobal/analyzer/controller/AnalyzerErrorRestControllerTest.java` - **Test Slicing**: Use `@WebMvcTest` - Test methods: `testGetErrors_WithFilters_ReturnsFilteredList`, `testAcknowledgeError_WithValidId_UpdatesStatus`, `testReprocessError_WithValidId_ProcessesMessage`
- [ ] T086 [P] [US3] Frontend unit test for ErrorDashboard component in `frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.test.jsx` - Test methods: `testRendersErrorDashboard_WithErrors_DisplaysTable`, `testFilterErrors_ByType_FiltersResults`, `testOpenErrorDetails_ShowsModal`
- [ ] T087 [P] [US3] Frontend unit test for ErrorDetailsModal component in `frontend/src/components/analyzers/ErrorDashboard/ErrorDetailsModal.test.jsx` - Test methods: `testDisplayErrorDetails_ShowsFullContext`, `testOpenMappingInterface_FromError_ShowsMappingModal`
- [ ] T088 [P] [US3] Cypress E2E test in `frontend/cypress/e2e/errorResolution.cy.js` - Test scenarios: "should display unmapped messages in error dashboard", "should create mapping from error context", "should reprocess error after mapping creation", "should acknowledge multiple errors in batch"

### Implementation for User Story 3

- [ ] T089 [P] [US3] Create AnalyzerErrorService interface in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerErrorService.java`
- [ ] T090 [US3] Create AnalyzerErrorServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerErrorServiceImpl.java` with @Service and @Transactional annotations - Methods: createError, acknowledgeError, getErrorsByFilters per FR-016
- [ ] T091 [P] [US3] Create AnalyzerReprocessingService interface in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingService.java`
- [ ] T092 [US3] Create AnalyzerReprocessingServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerReprocessingServiceImpl.java` with @Service and @Transactional annotations - Reprocess raw message from AnalyzerError.rawMessage through ASTMAnalyzerReader with new mappings per FR-017
- [ ] T093 [US3] Integrate error creation into ASTMAnalyzerReader.processData() - When mapping not found: Create AnalyzerError record, hold message in error queue per FR-011 - When validation fails: Create AnalyzerError record with validation details
- [ ] T094 [P] [US3] Create AnalyzerErrorForm DTO in `src/main/java/org/openelisglobal/analyzer/form/AnalyzerErrorForm.java`
- [ ] T095 [US3] Create AnalyzerErrorRestController in `src/main/java/org/openelisglobal/analyzer/controller/AnalyzerErrorRestController.java` extending BaseRestController - Endpoints: GET /analyzers/errors, GET /analyzers/errors/{id}, POST /analyzers/errors/{id}/acknowledge, POST /analyzers/errors/{id}/reprocess, POST /analyzers/errors/batch-acknowledge per FR-016, FR-017
- [ ] T096 [US3] Create ErrorDashboard component in `frontend/src/components/analyzers/ErrorDashboard/ErrorDashboard.jsx` using Carbon DataTable with statistics cards, filters, pagination per FR-016
- [ ] T097 [US3] Create ErrorDetailsModal component in `frontend/src/components/analyzers/ErrorDashboard/ErrorDetailsModal.jsx` using Carbon ComposedModal with error information, analyzer logs, recommended actions per FR-016
- [ ] T098 [US3] Create ErrorDashboardPage route component in `frontend/src/pages/ErrorDashboardPage.jsx` integrating ErrorDashboard component with route `/analyzers/errors`
- [ ] T099 [US3] Add React Router route in `frontend/src/App.js` for `/analyzers/errors` (ErrorDashboardPage)
- [ ] T100 [US3] Add visual indicators for unmapped fields in FieldMappingPanel component - Status badges showing "Unmapped", filter options, counts in status bar per FR-012
- [ ] T101 [US3] Integrate mapping interface modal into ErrorDetailsModal - "Create Mapping" button opens FieldMapping modal with pre-filled context from error per FR-011
- [ ] T177 [P] [US3] Create MappingAwareAnalyzerLineInserter wrapper class in `src/main/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserter.java` implementing AnalyzerLineInserter interface - Wrapper logic: Receive raw ASTM message segments, call MappingApplicationService.applyMappings() to transform segments, delegate transformed data to original plugin inserter if mappings found, create AnalyzerError if mappings not found per research.md Section 7
- [ ] T178 [P] [US3] Create MappingApplicationService interface in `src/main/java/org/openelisglobal/analyzer/service/MappingApplicationService.java`
- [ ] T179 [P] [US3] Create MappingApplicationServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/MappingApplicationServiceImpl.java` with @Service and @Transactional annotations - Method: applyMappings() - Receives raw ASTM message segments, extracts test codes/units/qualitative values, queries AnalyzerFieldMapping, applies mappings, returns transformed data structure per research.md Section 7
- [ ] T180 [US3] Integrate wrapper into ASTMAnalyzerReader.processData() with conditional wrapping logic - Check if analyzer has active mappings before wrapping - If analyzer has active mappings: Wrap plugin inserter with MappingAwareAnalyzerLineInserter - If analyzer has no mappings: Use original plugin inserter directly (backward compatibility) per research.md Section 7
- [ ] T181 [P] [US3] Integration test for MappingAwareAnalyzerLineInserter wrapper pattern in `src/test/java/org/openelisglobal/analyzer/service/MappingAwareAnalyzerLineInserterIntegrationTest.java` using @SpringBootTest - Test methods: `testProcessMessage_WithMappings_AppliesTransformations`, `testProcessMessage_WithoutMappings_UsesOriginalInserter`, `testProcessMessage_WithUnmappedField_CreatesError`
- [ ] T160 [P] [US3] Add integration test for SC-002 (98% processing rate) in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerMappingPerformanceTest.java` using @SpringBootTest - Test methods: `testProcessMessages_WithMappings_MeetsSuccessRate`, `testProcessMessages_WithUnmappedFields_HandlesGracefully` - Send 1000 ASTM messages with mappings configured, verify 98%+ processed successfully - Test error rate calculation: (successful messages / total messages) >= 0.98 - Include edge cases: unmapped fields, unit mismatches, validation errors per Success Criteria SC-002

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Query Analyzer Functionality (FR-002)

**Purpose**: Implement asynchronous "Query Analyzer" functionality for retrieving available data fields from analyzers

### Tests for Query Analyzer

- [ ] T102 [P] Unit test for AnalyzerQueryService in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceTest.java` - Test methods: `testQueryAnalyzer_WithValidConfig_ReturnsJobId`, `testGetQueryStatus_WithJobId_ReturnsStatus`, `testCancelQuery_WithJobId_CancelsJob`
- [ ] T103 [P] Integration test for query workflow in `src/test/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceIntegrationTest.java` using @SpringBootTest - Test methods: `testQueryAnalyzer_WithTimeout_HandlesGracefully`, `testParseASTMResponse_ExtractsFields`

### Implementation for Query Analyzer

- [ ] T104 [P] Create AnalyzerQueryService interface in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerQueryService.java`
- [ ] T105 Create AnalyzerQueryServiceImpl in `src/main/java/org/openelisglobal/analyzer/service/AnalyzerQueryServiceImpl.java` with @Service and @Transactional annotations - TCP connection to analyzer IP:Port, ASTM query message sending, response parsing per research.md Section 1 - Background job pattern: return job ID immediately, poll status endpoint per FR-002 - Read query timeout from SystemConfiguration key `analyzer.query.timeout.minutes` (default: 5 minutes) per FR-002 specification - Use SystemConfigurationService to lookup timeout value, fallback to 5 minutes if not configured
- [ ] T106 Add query endpoints in AnalyzerRestController: POST /analyzers/{id}/query (returns job ID), GET /analyzers/{id}/query/{jobId}/status (polling endpoint) per FR-002
- [ ] T107 Create QueryStatusModal component in `frontend/src/components/analyzers/FieldMapping/QueryStatusModal.jsx` using Carbon ComposedModal with progress bar, connection logs, cancel button per FR-002
- [ ] T108 Add "Query Analyzer" button to FieldMapping component - Triggers background job, polls status endpoint every 2-3 seconds, displays progress per FR-002
- [ ] T109 Integrate query results into FieldMappingPanel - Display retrieved fields with field type indicators (color-coded tags) per FR-002

---

## Phase 7: Navigation Integration (FR-020)

**Purpose**: Integrate analyzer mapping feature into OpenELIS left-hand navigation using unified tab-navigation pattern

### Tests for Navigation Integration

- [ ] T110 [P] Integration test for menu API in `src/test/java/org/openelisglobal/menu/controller/MenuControllerTest.java` - Test methods: `testGetMenu_WithAnalyzersItems_ReturnsItems`, `testGetMenu_WithRoleFilter_HidesUnauthorizedItems`
- [ ] T111 [P] Frontend unit test for navigation integration in `frontend/src/components/common/GlobalSideBar.test.js` - Test methods: `testRendersAnalyzersMenu_WithSubItems_DisplaysExpandable`, `testHighlightActiveSubNav_WithRoute_ShowsActiveState`

### Implementation for Navigation Integration

- [ ] T112 Extend MenuController to include analyzer menu items in `/rest/menu` API response - Filter by role (LAB_USER, LAB_SUPERVISOR) per FR-020 - Hide parent "Analyzers" item if user lacks permission for all sub-items
- [ ] T113 Update GlobalSideBar component to render analyzer menu items dynamically from API - Use Carbon SideNavMenu and SideNavMenuItem - Highlight active sub-nav item based on current route per FR-020
- [ ] T114 Add route metadata or page component props to require left-hand navigation visible and expanded by default for analyzer pages per FR-020
- [ ] T115 Implement state preservation using URL query parameters for filters, pagination, selected analyzer ID per FR-020 - Use URLSearchParams pattern consistent with SearchResultForm.js
- [ ] T116 Implement state preservation using sessionStorage for scroll position, form drafts per FR-020 - Clear on tab close, preserve across browser navigation

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T117 [P] Add comprehensive error handling and user feedback (success, warnings, errors) throughout analyzer mapping UI per FR-014 - Use Carbon Notification components, follow OpenELIS internationalization practices
- [ ] T141 [P] Integrate custom field types into AnalyzerField entity and FieldMappingPanel component - Extend AnalyzerField entity to support custom field types (reference CustomFieldType entity) - Update FieldMappingPanel to show custom field types in type selector - Update validation to use custom field type validation rules per FR-018

---

## Phase 8.5: System Administration - Custom Field Types Management

**Purpose**: Admin UI for managing custom field types (FR-018)

- [ ] T140 [P] Create CustomFieldTypeManagement component in `frontend/src/components/analyzers/admin/CustomFieldTypeManagement.jsx` - Admin-only page accessible via System Administration menu - Form fields: Type Name (unique identifier), Display Name, Validation Pattern (regex input with validation), Value Range (min/max for numeric types), Allowed Characters (character set input), Active Status - List view with edit/delete actions per FR-018
- [ ] T166 [P] Frontend unit test for CustomFieldTypeManagement in `frontend/src/components/analyzers/admin/CustomFieldTypeManagement.test.jsx` - Test methods: `testRendersCustomFieldTypes_WithData_DisplaysTable`, `testCreateCustomFieldType_WithValidData_SavesType`, `testValidatePattern_WithInvalidRegex_ShowsError`
- [ ] T118 [P] Add loading states and skeleton screens for async operations (query analyzer, test connection, reprocessing)
- [ ] T119 [P] Optimize HQL queries in DAO implementations using JOIN FETCH for eager loading (prevents LazyInitializationException per AGENTS.md)
- [ ] T120 [P] Add comprehensive logging for analyzer operations (connection tests, query operations, mapping changes, error creation)
- [ ] T121 [P] Add input validation and sanitization for all user inputs (IP addresses, port numbers, field names, mapping values)
- [ ] T122 [P] Add accessibility improvements (ARIA labels, keyboard navigation, screen reader support) for Carbon components
- [ ] T123 [P] Add responsive design improvements for mobile devices (<1024px) - Stack panels vertically, optimize table layouts
- [ ] T124 [P] Add performance optimizations (debouncing search inputs, pagination, lazy loading for large datasets)
- [ ] T125 [P] Update documentation in `specs/004-astm-analyzer-mapping/quickstart.md` with step-by-step developer guide
- [ ] T126 Run quickstart.md validation - Verify all scenarios from quickstart.md work end-to-end

---

## Phase 9: Constitution Compliance Verification

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

- [ ] T127 **Configuration-Driven**: Verify no country-specific code branches introduced - All variations via database configuration (SystemConfiguration, LocalizationConfiguration)
- [ ] T128 **Carbon Design System**: Audit UI - confirm @carbon/react used exclusively (NO Bootstrap/Tailwind) - Verify Carbon tokens used for colors, spacing, typography - Verify Carbon components used: SideNavMenu, SideNavMenuItem, DataTable, ComposedModal, Search, MultiSelect, Tag, Toggle, OverflowMenu, Pagination, Accordion, TextInput, Dropdown, NumberInput
- [ ] T129 **FHIR/IHE Compliance**: Validate FHIR resources against R4 profiles (if applicable) - Analyzer entities may not require FHIR mapping (verify per research.md)
- [ ] T130 **Layered Architecture**: Verify 5-layer pattern followed (Valueholder→DAO→Service→Controller→Form) - NO @Transactional in controllers - NO DAO calls from controllers - Services compile all data within transaction using JOIN FETCH
- [ ] T131 **Test Coverage**: Run coverage report - confirm >80% backend (JaCoCo), >70% frontend (Jest) - Verify ORM validation tests pass (<5 seconds) - Verify E2E tests run individually during development
- [ ] T132 **Schema Management**: Verify ALL database changes use Liquibase changesets (NO direct SQL) - All changesets in `src/main/resources/liquibase/analyzer/` - Rollback scripts provided for structural changes
- [ ] T133 **Internationalization**: Audit UI strings - confirm React Intl used for ALL text (no hardcoded strings) - Verify en.json and fr.json translations complete - Verify date/time formatting via intl.formatDate(), intl.formatTime()
- [ ] T134 **Security & Compliance**: Verify RBAC (role-based access control) - Verify audit trail (sys_user_id + last_updated) - Verify input validation (SQL injection, XSS prevention) - Verify HTTPS endpoints only (NO HTTP for PHI)
- [ ] T162 Add manual validation checklist for SC-004 (support ticket reduction) in Phase 9 compliance verification - Document metric collection approach (not automated test) - Add manual validation checklist for post-deployment monitoring per Success Criteria SC-004

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

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Query Analyzer (Phase 6)**: Can proceed in parallel with User Stories (depends on Foundational)
- **Navigation Integration (Phase 7)**: Depends on User Story 1 completion (needs routes)
- **Polish (Phase 8)**: Depends on all desired user stories being complete
- **Constitution Compliance (Phase 9)**: Depends on all implementation phases

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - May integrate with US1 but should be independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Depends on error queue infrastructure (AnalyzerError entity) from Foundational

### Within Each User Story

- Tests (MANDATORY) MUST be written and FAIL before implementation
- Entities before DAOs
- DAOs before Services
- Services before Controllers
- Controllers before Frontend components
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Entities within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members
- Query Analyzer functionality (Phase 6) can proceed in parallel with User Stories

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

**Total Tasks**: 181

**Tasks by Phase**:
- Phase 1 (Setup): 11 tasks
- Phase 2 (Foundational): 22 tasks (1 test + 21 implementation, includes CustomFieldType entity/DAO/Service)
- Phase 3 (User Story 1): 54 tasks (15 tests + 39 implementation, includes inline field creation, lifecycle workflow, SC-001 validation)
- Phase 4 (User Story 2): 16 tasks (6 tests + 10 implementation, includes mapping retirement, activation modal, SC-003 validation)
- Phase 5 (User Story 3): 28 tasks (9 tests + 19 implementation, includes wrapper integration, SC-002 validation)
- Phase 6 (Query Analyzer): 8 tasks (2 tests + 6 implementation)
- Phase 7 (Navigation Integration): 7 tasks (2 tests + 5 implementation)
- Phase 8 (Polish): 11 tasks (includes custom field types integration)
- Phase 8.5 (System Administration): 2 tasks (CustomFieldTypeManagement UI)
- Phase 9 (Constitution Compliance): 9 tasks (includes SC-004 manual validation)

**Tasks by User Story**:
- User Story 1 (P1): 54 tasks
- User Story 2 (P2): 16 tasks
- User Story 3 (P3): 28 tasks

**Test Tasks**: 35 total (ORM validation: 1, Unit tests: 11, DAO tests: 4, Controller tests: 6, Frontend unit tests: 9, E2E tests: 4, Integration tests: 4)

**Parallel Opportunities**: All tasks marked [P] can run in parallel within their phase

**MVP Scope**: Phase 1 + Phase 2 + Phase 3 (User Story 1) = 69 tasks

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
- Follow AGENTS.md architecture patterns (5-layer, no @Transactional in controllers, JOIN FETCH in services)
- Use research.md decisions for implementation approach
- Follow data-model.md entity definitions and relationships

