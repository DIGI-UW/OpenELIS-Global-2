# Tasks: Sample Management Menu

**Input**: Design documents from `/specs/001-sample-management/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Tests are MANDATORY per Constitution V and Testing Roadmap. Test
tasks MUST appear BEFORE implementation tasks to enforce TDD workflow.

**Organization**: Tasks are grouped by user story to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

This is a web application with monorepo structure:

- Backend: `src/main/java/`, `src/main/resources/`, `src/test/java/`
- Frontend: `frontend/src/`, `frontend/cypress/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and database schema setup

- [x] T001 Create Liquibase changeset for sample_item table modifications in
      src/main/resources/liquibase/2.8.x.x/sample-management-001.xml (add
      original_quantity, remaining_quantity, parent_sample_item_id columns)
- [x] T002 [P] Create Liquibase changeset for sample_item_aliquot_relationship
      table in src/main/resources/liquibase/2.8.x.x/sample-management-001.xml
      (new table with foreign keys and unique constraint)
- [x] T003 [P] Add internationalization keys for sample management UI strings in
      frontend/src/languages/en.json
- [x] T004 [P] Add French translation keys in frontend/src/languages/fr.json
- [x] T005 Register changeset in base.xml for automatic migration on application
      startup (Liquibase runs via Spring Boot integration)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core entities and DAOs that MUST be complete before ANY user story
can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### ORM Validation Tests (Constitution V.4)

- [x] T006 [P] ORM validation test for SampleItem entity modifications in
      src/test/java/org/openelisglobal/sampleitem/SampleItemOrmValidationTest.java -
      Reference:
      [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4) -
      Build SessionFactory using `config.addAnnotatedClass(SampleItem.class)` -
      Validate new annotations: originalQuantity, remainingQuantity,
      parentSampleItem, childAliquots - MUST execute in <5 seconds - MUST NOT
      require database connection - **SDD Checkpoint**: Must pass after Phase 2
      (Entities)
- [x] T007 [P] ORM validation test for SampleItemAliquotRelationship entity in
      src/test/java/org/openelisglobal/sampleitem/SampleItemAliquotRelationshipOrmValidationTest.java -
      Reference:
      [Testing Roadmap - ORM Validation Tests](.specify/guides/testing-roadmap.md#orm-validation-tests-constitution-v4) -
      Build SessionFactory using
      `config.addAnnotatedClass(SampleItemAliquotRelationship.class)` - Validate
      all JPA annotations and relationships - MUST execute in <5 seconds - MUST
      NOT require database connection - **SDD Checkpoint**: Must pass after
      Phase 2 (Entities)

### Entity Modifications

- [x] T008 Modify SampleItem entity to add new fields in
      src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java
      (add @Column originalQuantity, @Column remainingQuantity, @ManyToOne
      parentSampleItem, @OneToMany childAliquots with JPA annotations)
- [x] T009 [P] Add validation methods to SampleItem entity in
      src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java
      (setRemainingQuantity validation, setOriginalQuantity validation,
      hasRemainingQuantity(), isAliquot() helper methods)
- [x] T010 [P] Create SampleItemAliquotRelationship entity in
      src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItemAliquotRelationship.java
      (full annotation-based entity with @Entity, @Table, unique constraint on
      parent+sequence)

### DTOs

- [x] T011 [P] Create SampleItemDTO in
      src/main/java/org/openelisglobal/sampleitem/dto/SampleItemDTO.java (with
      fields: id, externalId, sampleAccessionNumber, sampleType,
      originalQuantity, remainingQuantity, unitOfMeasure, status,
      collectionDate, parentId, parentExternalId, childAliquots, orderedTests,
      hasRemainingQuantity, isAliquot, nestingLevel)
- [x] T012 [P] Create AliquotSummaryDTO in
      src/main/java/org/openelisglobal/sampleitem/dto/AliquotSummaryDTO.java
      (with fields: id, externalId, originalQuantity, remainingQuantity,
      createdDate)
- [x] T013 [P] Create TestSummaryDTO in
      src/main/java/org/openelisglobal/sampleitem/dto/TestSummaryDTO.java (with
      fields: analysisId, testId, testName, status, orderedDate)
- [x] T014 [P] Create CreateAliquotForm in
      src/main/java/org/openelisglobal/sampleitem/form/CreateAliquotForm.java
      (with Jakarta validation: @NotBlank parentSampleItemId, @NotNull
      @DecimalMin quantityToTransfer, @Size notes)
- [x] T015 [P] Create AddTestsForm in
      src/main/java/org/openelisglobal/sampleitem/form/AddTestsForm.java (with
      Jakarta validation: @NotEmpty sampleItemIds, @NotEmpty testIds)
- [x] T016 [P] Create SearchSamplesResponse in
      src/main/java/org/openelisglobal/sampleitem/dto/SearchSamplesResponse.java
      (with fields: accessionNumber, sampleItems list, totalCount)
- [x] T017 [P] Create CreateAliquotResponse in
      src/main/java/org/openelisglobal/sampleitem/dto/CreateAliquotResponse.java
      (with fields: aliquot SampleItemDTO, parentUpdatedRemainingQuantity,
      message)
- [x] T018 [P] Create AddTestsResponse in
      src/main/java/org/openelisglobal/sampleitem/dto/AddTestsResponse.java
      (with fields: successCount, results list of TestAdditionResult)

### DAOs

- [ ] T019 Create SampleItemAliquotRelationshipDAO interface in
      src/main/java/org/openelisglobal/sampleitem/dao/SampleItemAliquotRelationshipDAO.java
      (extend BaseObjectDAO with methods: getMaxSequenceNumber,
      getByParentSampleItem)
- [ ] T020 Create SampleItemAliquotRelationshipDAOImpl in
      src/main/java/org/openelisglobal/sampleitem/daoimpl/SampleItemAliquotRelationshipDAOImpl.java
      (implement getMaxSequenceNumber with HQL MAX query, implement
      getByParentSampleItem with HQL)
- [ ] T021 [P] Extend SampleItemDAO interface in
      src/main/java/org/openelisglobal/sampleitem/dao/SampleItemDAO.java (add
      method: getSampleItemsWithHierarchy for JOIN FETCH queries)
- [ ] T022 [P] Implement getSampleItemsWithHierarchy in
      src/main/java/org/openelisglobal/sampleitem/daoimpl/SampleItemDAOImpl.java
      (HQL query with LEFT JOIN FETCH si.parentSampleItem and si.childAliquots)

**Checkpoint**: Foundation ready - user story implementation can now begin in
parallel

---

## Phase 3: User Story 1 - Search Sample Items by Accession Number (Priority: P1) 🎯 MVP

**Goal**: Enable laboratory technicians to quickly locate sample items using
accession numbers to verify sample status, view associated tests, and see
parent-child relationships

**Independent Test**: Enter an accession number in the search interface and
verify that the correct sample item(s) are displayed with their current status,
metadata, and hierarchy

### Tests for User Story 1 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T023 [P] [US1] Unit test for
      SampleManagementService.searchByAccessionNumber in
      src/test/java/org/openelisglobal/sampleitem/service/SampleManagementServiceTest.java
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests (JUnit 4 + Mockito)](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) -
      Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md) -
      **TDD Workflow**: Write test FIRST (RED), then implement (GREEN), then
      refactor - **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` (NOT
      `@SpringBootTest`) - **Mocking**: Use `@Mock` for SampleDAO, SampleItemDAO
      (NOT `@MockBean`) - **Test Cases**: (1) single sample no aliquots, (2)
      parent with multiple aliquots showing hierarchy, (3) no samples found, (4)
      partial accession number match - **Coverage Goal**: >80% - **SDD
      Checkpoint**: Must pass after Phase 3 implementation
- [ ] T024 [P] [US1] DAO test for getSampleItemsWithHierarchy in
      src/test/java/org/openelisglobal/sampleitem/dao/SampleItemDAOTest.java
      (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) -
      Reference:
      [Testing Roadmap - @DataJpaTest](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer) -
      Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md) -
      **Test Slicing**: Use `@DataJpaTest` (NOT `@SpringBootTest`) - **Test
      Data**: Use `TestEntityManager` to create test samples with parent-child
      relationships - **Test Cases**: (1) load single sample, (2) load parent
      with children, (3) load nested hierarchy (3 levels deep), (4) verify JOIN
      FETCH prevents LazyInitializationException - **Transaction Management**:
      Automatic rollback
- [ ] T025 [P] [US1] Controller test for GET /api/sample-management/search in
      src/test/java/org/openelisglobal/sampleitem/controller/SampleManagementRestControllerTest.java
      (Template:
      `.specify/templates/testing/WebMvcTestController.java.template`) -
      Reference:
      [Testing Roadmap - @WebMvcTest](.specify/guides/testing-roadmap.md#webmvctest-controller-layer) -
      Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md) -
      **Test Slicing**: Use `@WebMvcTest(SampleManagementRestController.class)`
      (NOT `@SpringBootTest`) - **Mocking**: Use `@MockBean` for
      SampleManagementService - **HTTP Testing**: Use MockMvc to perform GET
      request with accessionNumber query param - **Test Cases**: (1) 200 OK with
      results, (2) 200 OK with empty results, (3) 400 BAD REQUEST for missing
      accessionNumber - **Coverage Goal**: >80% - **SDD Checkpoint**: Must pass
      after Phase 3 implementation
- [ ] T026 [P] [US1] Frontend unit test for SampleSearch component in
      frontend/src/components/sampleManagement/SampleSearch.test.js (Template:
      `.specify/templates/testing/JestComponent.test.jsx.template`) - Reference:
      [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) -
      Reference: [Jest Best Practices](.specify/guides/jest-best-practices.md) -
      **TDD Workflow**: Write test FIRST (RED), then implement (GREEN) -
      **Import Order**: React → Testing Library → userEvent → jest-dom → Intl →
      Component - **Mocks BEFORE imports**: Jest hoisting requires mocks before
      imports - **userEvent PREFERRED**: Use `userEvent.type()` for search input
      (NOT `fireEvent`) - **Test Cases**: (1) search input renders, (2) search
      triggers on Enter key, (3) search button click triggers search, (4)
      loading state displays during fetch - **Coverage Goal**: >70% - **SDD
      Checkpoint**: Must pass after Phase 3 frontend implementation
- [ ] T027 [P] [US1] Frontend unit test for SampleResultsTable component in
      frontend/src/components/sampleManagement/SampleResultsTable.test.js
      (Template: `.specify/templates/testing/JestComponent.test.jsx.template`) -
      Reference:
      [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) -
      Reference: [Jest Best Practices](.specify/guides/jest-best-practices.md) -
      **Test Cases**: (1) renders results with sample data, (2) displays
      hierarchy (parent-child indicators), (3) shows "no results" message when
      empty, (4) row selection works correctly - **Carbon Components**: Use
      `within()` for scoped queries in DataTable - **Coverage Goal**: >70%
- [ ] T028 [P] [US1] Cypress E2E test for sample search workflow in
      frontend/cypress/e2e/sampleManagement.cy.js (Template:
      `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference:
      [Constitution Section V.5](.specify/memory/constitution.md#section-v5-cypress-e2e-testing-best-practices) -
      Reference:
      [Testing Roadmap - Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing) -
      Reference:
      [Cypress Best Practices](.specify/guides/cypress-best-practices.md) -
      **Test Setup**: Use cy.session() for login (10-20x faster), use API-based
      test data setup with cy.request() to create test samples - **Test Cases**:
      (1) search by complete accession number displays results, (2) search
      results show sample details, (3) parent-child hierarchy visible in
      results, (4) no results message for non-existent accession -
      **Selectors**: Use data-testid (PREFERRED) - **Run Individually**:
      `npm run cy:run -- --spec "cypress/e2e/sampleManagement.cy.js"` during
      development - **Post-run Review**: Review console logs, screenshots

### Implementation for User Story 1

> **CRITICAL: Implementation tasks depend on test tasks. Tests must pass before
> proceeding to next phase checkpoint.**

- [ ] T029 [US1] Create SampleManagementService interface in
      src/main/java/org/openelisglobal/sampleitem/service/SampleManagementService.java
      (with method: SearchSamplesResponse searchByAccessionNumber(String
      accessionNumber, boolean includeTests))
- [ ] T030 [US1] Implement SampleManagementServiceImpl.searchByAccessionNumber
      in
      src/main/java/org/openelisglobal/sampleitem/service/SampleManagementServiceImpl.java
      (use @Transactional(readOnly=true), call
      SampleDAO.getSampleByAccessionNumber, call
      SampleItemDAO.getSampleItemsWithHierarchy, compile full DTOs with
      parent-child data WITHIN transaction to prevent
      LazyInitializationException, optionally load tests if includeTests=true)
- [ ] T031 [US1] Create SampleManagementRestController with GET
      /api/sample-management/search endpoint in
      src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java
      (accept @RequestParam accessionNumber and includeTests, call
      SampleManagementService.searchByAccessionNumber, return
      SearchSamplesResponse with 200 OK)
- [ ] T032 [P] [US1] Add validation and error handling to search endpoint in
      src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java
      (validate accessionNumber @NotBlank, handle exceptions with
      @ExceptionHandler returning 400 BAD REQUEST or 404 NOT FOUND)
- [ ] T033 [P] [US1] Create SampleSearch React component in
      frontend/src/components/sampleManagement/SampleSearch.js (use Carbon
      Search component, debounce search input, call GET
      /api/sample-management/search, handle loading and error states, use React
      Intl for all strings)
- [ ] T034 [P] [US1] Create SampleResultsTable React component in
      frontend/src/components/sampleManagement/SampleResultsTable.js (use Carbon
      DataTable with headers: externalId, sampleType, originalQuantity,
      remainingQuantity, status, use TableSelectRow for multi-select, display
      parent-child indicators, use React Intl for all strings)
- [ ] T035 [US1] Create SampleManagement container component in
      frontend/src/components/sampleManagement/SampleManagement.js (integrate
      SampleSearch and SampleResultsTable, manage state for search results,
      handle API errors with Carbon InlineNotification)
- [ ] T036 [US1] Add Sample Management menu item to main navigation in
      frontend/src/components/menu/MenuLinks.js (with route to
      /sample-management, icon, requiredRoles: ["Lab Technician", "Lab
      Manager"], labelId for React Intl)

**Checkpoint Validation**: At this point, User Story 1 should be fully
functional and testable independently. Users can search for samples by accession
number and view results with hierarchy. ALL tests from T023-T028 MUST pass
before proceeding.

---

## Phase 4: User Story 2 - Add Multiple Tests to Sample Items (Priority: P1) 🎯 MVP

**Goal**: Enable laboratory staff to add multiple test orders to single or
multiple sample items efficiently without repetitive data entry

**Independent Test**: Load a sample item, select multiple tests from catalog,
verify all selected tests are associated with the sample item and visible in the
tests list

### Tests for User Story 2 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T037 [P] [US2] Unit test for SampleManagementService.addTestsToSamples in
      src/test/java/org/openelisglobal/sampleitem/service/SampleManagementServiceTest.java
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) -
      Reference:
      [Backend Testing Best Practices](.specify/guides/backend-testing-best-practices.md) -
      **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` - **Mocking**:
      Use `@Mock` for AnalysisDAO, SampleItemDAO, TestDAO - **Test Cases**: (1)
      add multiple tests to single sample, (2) add same test to multiple
      samples, (3) skip duplicate tests with warning, (4) skip incompatible test
      types with warning, (5) bulk operation with mix of success and skipped -
      **Coverage Goal**: >80%
- [ ] T038 [P] [US2] DAO test for Analysis duplicate detection in
      src/test/java/org/openelisglobal/analysis/dao/AnalysisDAOTest.java
      (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) -
      Reference:
      [Testing Roadmap - @DataJpaTest](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer) -
      **Test Slicing**: Use `@DataJpaTest` - **Test Data**: Use
      `TestEntityManager` to create test samples and analyses - **Test Cases**:
      (1) getAnalysisBySampleItemAndTest finds existing, (2) returns null for
      non-existent combination, (3) getAnalysesBySampleItem returns all tests
      for sample
- [ ] T039 [P] [US2] Controller test for POST /api/sample-management/add-tests
      in
      src/test/java/org/openelisglobal/sampleitem/controller/SampleManagementRestControllerTest.java
      (Template:
      `.specify/templates/testing/WebMvcTestController.java.template`) -
      Reference:
      [Testing Roadmap - @WebMvcTest](.specify/guides/testing-roadmap.md#webmvctest-controller-layer) -
      **Test Slicing**: Use
      `@WebMvcTest(SampleManagementRestController.class)` - **Mocking**: Use
      `@MockBean` for SampleManagementService - **HTTP Testing**: Use MockMvc to
      POST with AddTestsForm JSON - **Test Cases**: (1) 200 OK with success
      count, (2) 400 BAD REQUEST for empty sampleItemIds, (3) 400 BAD REQUEST
      for empty testIds, (4) 404 NOT FOUND for non-existent sample
- [ ] T040 [P] [US2] Frontend unit test for TestSelector component in
      frontend/src/components/sampleManagement/TestSelector.test.js (Template:
      `.specify/templates/testing/JestComponent.test.jsx.template`) - Reference:
      [Testing Roadmap - Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) -
      Reference: [Jest Best Practices](.specify/guides/jest-best-practices.md) -
      **Test Cases**: (1) renders test catalog with MultiSelect, (2) filters
      tests by sample type, (3) selection works correctly, (4) displays
      duplicate warning before submission, (5) handles API errors
- [ ] T041 [P] [US2] Integration test for complete add tests workflow in
      src/test/java/org/openelisglobal/sampleitem/integration/SampleManagementIntegrationTest.java -
      Reference:
      [Testing Roadmap - @SpringBootTest](.specify/guides/testing-roadmap.md#springboottest-full-integration) -
      **Test Slicing**: Use `@SpringBootTest` only for full workflow -
      **Transaction Management**: Use `@Transactional` for automatic rollback -
      **Test Cases**: (1) search sample → add tests → verify tests associated,
      (2) bulk add to multiple samples → verify all associations, (3) duplicate
      detection workflow
- [ ] T042 [P] [US2] Cypress E2E test for add tests workflow in
      frontend/cypress/e2e/sampleManagement.cy.js (Template:
      `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference:
      [Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing) -
      **Test Setup**: Use cy.session() for login, use cy.request() to create
      test samples and test definitions - **Test Cases**: (1) add multiple tests
      to single sample, (2) add same test to multiple samples, (3) duplicate
      test warning displayed, (4) verify tests appear in sample details

### Implementation for User Story 2

- [ ] T043 [P] [US2] Add getAnalysisBySampleItemAndTest method to AnalysisDAO in
      src/main/java/org/openelisglobal/analysis/dao/AnalysisDAO.java (for
      duplicate detection)
- [ ] T044 [P] [US2] Implement getAnalysisBySampleItemAndTest in AnalysisDAOImpl
      in src/main/java/org/openelisglobal/analysis/daoimpl/AnalysisDAOImpl.java
      (HQL query by sampleItem.id and test.id)
- [ ] T045 [US2] Implement SampleManagementService.addTestsToSamples in
      src/main/java/org/openelisglobal/sampleitem/service/SampleManagementServiceImpl.java
      (use @Transactional, loop through sampleItemIds x testIds combinations,
      check for duplicates with getAnalysisBySampleItemAndTest, create Analysis
      entities for non-duplicates, collect results with success/skipped status,
      return AddTestsResponse)
- [ ] T046 [US2] Add POST /api/sample-management/add-tests endpoint to
      SampleManagementRestController in
      src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java
      (accept @Valid @RequestBody AddTestsForm, call
      SampleManagementService.addTestsToSamples, return AddTestsResponse with
      200 OK)
- [ ] T047 [P] [US2] Add validation and error handling to add-tests endpoint in
      src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java
      (validate form with Jakarta Bean Validation, handle NotFoundException with
      404, handle ValidationException with 400)
- [ ] T048 [P] [US2] Create TestSelector React component in
      frontend/src/components/sampleManagement/TestSelector.js (use Carbon
      MultiSelect or ComboBox for test catalog, filter by sample type, handle
      selection state, display duplicate warnings with InlineNotification, use
      React Intl)
- [ ] T049 [US2] Integrate TestSelector into SampleManagement container in
      frontend/src/components/sampleManagement/SampleManagement.js (show
      TestSelector when samples selected, call POST
      /api/sample-management/add-tests on submit, display success message with
      count and skipped list, refresh search results to show new tests)

**Checkpoint Validation**: At this point, User Stories 1 AND 2 should both work
independently. Users can search samples AND add tests to them. ALL tests from
T037-T042 MUST pass.

---

## Phase 5: User Story 3 - Aliquot Sample Items with Volume Tracking (Priority: P2)

**Goal**: Enable laboratory technicians to create child sample items (aliquots)
from parent samples by dividing volume, while maintaining accurate tracking of
quantities to prevent over-dispensing

**Independent Test**: Select a parent sample with 10mL, create an aliquot of
3mL, verify: (a) parent remaining quantity updates to 7mL, (b) new aliquot
created with external ID "originalID.1", (c) parent-child relationship recorded

### Tests for User Story 3 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T050 [P] [US3] Unit test for SampleManagementService.createAliquot in
      src/test/java/org/openelisglobal/sampleitem/service/SampleManagementServiceTest.java
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) -
      **Test Slicing**: Use `@RunWith(MockitoJUnitRunner.class)` - **Mocking**:
      Use `@Mock` for SampleItemDAO, SampleItemAliquotRelationshipDAO - **Test
      Cases**: (1) create aliquot reduces parent quantity, (2) external ID
      generation with sequence (.1, .2, .3), (3) nested aliquoting (create
      aliquot from aliquot), (4) throw InsufficientQuantityException when
      exceeds remaining, (5) throw NoRemainingQuantityException when
      remaining=0, (6) optimistic locking retry on concurrent modification -
      **Coverage Goal**: >80%
- [ ] T051 [P] [US3] DAO test for SampleItemAliquotRelationshipDAO in
      src/test/java/org/openelisglobal/sampleitem/dao/SampleItemAliquotRelationshipDAOTest.java
      (Template: `.specify/templates/testing/DataJpaTestDao.java.template`) -
      Reference:
      [Testing Roadmap - @DataJpaTest](.specify/guides/testing-roadmap.md#datajpatest-daorepository-layer) -
      **Test Slicing**: Use `@DataJpaTest` - **Test Data**: Use
      `TestEntityManager` - **Test Cases**: (1) getMaxSequenceNumber returns
      null for no aliquots, (2) getMaxSequenceNumber returns max for multiple
      aliquots, (3) getByParentSampleItem returns all children, (4) unique
      constraint enforced on parent+sequence
- [ ] T052 [P] [US3] Controller test for POST /api/sample-management/aliquot in
      src/test/java/org/openelisglobal/sampleitem/controller/SampleManagementRestControllerTest.java
      (Template:
      `.specify/templates/testing/WebMvcTestController.java.template`) -
      Reference:
      [Testing Roadmap - @WebMvcTest](.specify/guides/testing-roadmap.md#webmvctest-controller-layer) -
      **Test Slicing**: Use `@WebMvcTest` - **HTTP Testing**: MockMvc POST with
      CreateAliquotForm JSON - **Test Cases**: (1) 201 CREATED with aliquot DTO,
      (2) 400 BAD REQUEST for missing parentSampleItemId, (3) 400 BAD REQUEST
      for invalid quantity, (4) 409 CONFLICT for InsufficientQuantityException,
      (5) 409 CONFLICT for concurrent modification
- [ ] T053 [P] [US3] Frontend unit test for AliquotModal component in
      frontend/src/components/sampleManagement/AliquotModal.test.js (Template:
      `.specify/templates/testing/JestComponent.test.jsx.template`) - Reference:
      [Jest + React Testing Library](.specify/guides/testing-roadmap.md#jest--react-testing-library-unit-tests) -
      **Test Cases**: (1) renders modal with parent info, (2) quantity input
      validation (exceeds remaining), (3) submit calls API, (4) displays error
      for insufficient quantity, (5) closes on success
- [ ] T054 [P] [US3] Integration test for complete aliquot workflow in
      src/test/java/org/openelisglobal/sampleitem/integration/SampleManagementIntegrationTest.java -
      Reference:
      [Testing Roadmap - @SpringBootTest](.specify/guides/testing-roadmap.md#springboottest-full-integration) -
      **Test Cases**: (1) create aliquot → verify parent/child state, (2) nested
      aliquoting (3 levels deep), (3) concurrent aliquoting by 2 threads with
      optimistic locking, (4) full workflow: search → create aliquot → verify
      hierarchy
- [ ] T055 [P] [US3] Cypress E2E test for aliquot creation workflow in
      frontend/cypress/e2e/sampleManagement.cy.js (Template:
      `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference:
      [Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing) -
      **Test Setup**: Use cy.request() to create test sample with quantity -
      **Test Cases**: (1) click "Create Aliquot" button opens modal, (2) enter
      quantity and submit creates aliquot, (3) parent quantity updates in UI,
      (4) new aliquot appears in results with .1 suffix, (5) error displayed for
      exceeding quantity

### Implementation for User Story 3

- [ ] T056 [US3] Implement SampleManagementService.createAliquot in
      src/main/java/org/openelisglobal/sampleitem/service/SampleManagementServiceImpl.java
      (use @Transactional, add @Retryable for OptimisticLockException with
      maxAttempts=3 and exponential backoff, load parent SampleItem, validate
      remaining quantity, get max sequence number from
      SampleItemAliquotRelationshipDAO, generate external ID with
      parent.externalId + "." + nextSequence, create new SampleItem with
      originalQuantity=quantityToTransfer and
      remainingQuantity=quantityToTransfer and parentSampleItem=parent, update
      parent.remainingQuantity -= quantityToTransfer, save parent (triggers
      @Version check), save aliquot, create and save
      SampleItemAliquotRelationship record, compile and return SampleItemDTO
      within transaction)
- [ ] T057 [P] [US3] Add @Recover method for OptimisticLockException in
      SampleManagementServiceImpl in
      src/main/java/org/openelisglobal/sampleitem/service/SampleManagementServiceImpl.java
      (throw ConcurrentModificationException with user-friendly message after
      retry exhaustion)
- [ ] T058 [P] [US3] Create InsufficientQuantityException in
      src/main/java/org/openelisglobal/sampleitem/exception/InsufficientQuantityException.java
      (custom exception for validation errors)
- [ ] T059 [P] [US3] Create NoRemainingQuantityException in
      src/main/java/org/openelisglobal/sampleitem/exception/NoRemainingQuantityException.java
      (custom exception for zero quantity)
- [ ] T060 [US3] Add POST /api/sample-management/aliquot endpoint to
      SampleManagementRestController in
      src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java
      (accept @Valid @RequestBody CreateAliquotForm, call
      SampleManagementService.createAliquot, return CreateAliquotResponse with
      201 CREATED status)
- [ ] T061 [P] [US3] Add exception handlers to SampleManagementRestController in
      src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java
      (add @ExceptionHandler for InsufficientQuantityException returning 409
      CONFLICT, add @ExceptionHandler for NoRemainingQuantityException returning
      409 CONFLICT, add @ExceptionHandler for OptimisticLockException returning
      409 CONFLICT)
- [ ] T062 [P] [US3] Create AliquotModal React component in
      frontend/src/components/sampleManagement/AliquotModal.js (use Carbon
      ComposedModal with ModalHeader/ModalBody/ModalFooter, display parent
      external ID and remaining quantity as read-only TextInputs, quantity to
      transfer as editable number TextInput with min/max/step validation, notes
      as optional TextInput, validate quantity <= parent remaining on client
      side, call POST /api/sample-management/aliquot on submit, display Carbon
      InlineNotification for errors, use React Intl for all strings)
- [ ] T063 [US3] Integrate AliquotModal into SampleManagement container in
      frontend/src/components/sampleManagement/SampleManagement.js (add "Create
      Aliquot" button when single sample selected, open AliquotModal with
      selected sample data, refresh search results on success to show updated
      parent and new aliquot, display success notification with aliquot external
      ID)
- [ ] T064 [P] [US3] Add HierarchyTreeView component for nested aliquot display
      in frontend/src/components/sampleManagement/HierarchyTreeView.js (render
      parent-child relationships with indentation, use Carbon
      ChevronRight/ChevronDown icons for expand/collapse, show external ID and
      quantity for each level, use React Intl)
- [ ] T065 [US3] Integrate HierarchyTreeView into SampleResultsTable in
      frontend/src/components/sampleManagement/SampleResultsTable.js (display
      hierarchy when childAliquots array is not empty, expandable rows showing
      nested structure)

**Checkpoint Validation**: At this point, User Stories 1, 2, AND 3 should all
work independently. Users can search, add tests, AND create aliquots. ALL tests
from T050-T055 MUST pass.

---

## Phase 6: User Story 4 - Add Tests to Aliquots in Bulk (Priority: P3)

**Goal**: Enable laboratory staff to add tests to multiple aliquot sample items
simultaneously to efficiently manage workflows where aliquots are designated for
different test types

**Independent Test**: Create 3 aliquots, select all of them, choose a test,
verify that the test is added to all 3 aliquots in a single operation

### Tests for User Story 4 (MANDATORY - TDD Enforcement)

> **CRITICAL: Write these tests FIRST, ensure they FAIL before implementation**
>
> Reference: [OpenELIS Testing Roadmap](.specify/guides/testing-roadmap.md)
> Templates: `.specify/templates/testing/`

- [ ] T066 [P] [US4] Unit test for bulk test addition to aliquots in
      src/test/java/org/openelisglobal/sampleitem/service/SampleManagementServiceTest.java
      (Template: `.specify/templates/testing/JUnit4ServiceTest.java.template`) -
      Reference:
      [Testing Roadmap - Unit Tests](.specify/guides/testing-roadmap.md#unit-tests-junit-4--mockito) -
      **Test Cases**: (1) add same test to multiple aliquots (3 aliquots, 1 test
      = 3 associations), (2) add multiple tests to multiple aliquots (2
      aliquots, 2 tests = 4 associations), (3) skip duplicates per aliquot with
      individual warnings, (4) mixed sample types with incompatibility
      warnings - **Coverage Goal**: >80%
- [ ] T067 [P] [US4] Integration test for bulk aliquot test ordering in
      src/test/java/org/openelisglobal/sampleitem/integration/SampleManagementIntegrationTest.java -
      Reference:
      [Testing Roadmap - @SpringBootTest](.specify/guides/testing-roadmap.md#springboottest-full-integration) -
      **Test Cases**: (1) create parent → create 5 aliquots → bulk add test to
      all 5 → verify all associations, (2) nested aliquots (3 levels) → bulk add
      test → verify all receive test, (3) duplicate test on some aliquots →
      verify skipped appropriately
- [ ] T068 [P] [US4] Cypress E2E test for bulk test addition to aliquots in
      frontend/cypress/e2e/sampleManagement.cy.js (Template:
      `.specify/templates/testing/CypressE2E.cy.js.template`) - Reference:
      [Cypress E2E Testing](.specify/guides/testing-roadmap.md#cypress-e2e-testing) -
      **Test Setup**: Use cy.request() to create parent sample and 3 aliquots -
      **Test Cases**: (1) select all 3 aliquots using checkboxes, (2) open test
      selector, (3) select test and submit, (4) verify success notification
      shows "3 tests added", (5) verify all 3 aliquots now show test in results

### Implementation for User Story 4

- [ ] T069 [US4] Update SampleManagementService.addTestsToSamples to handle
      aliquot filtering in
      src/main/java/org/openelisglobal/sampleitem/service/SampleManagementServiceImpl.java
      (add logic to accept mixed original samples and aliquots, detect if all
      selected samples are aliquots for UI messaging, maintain existing
      duplicate detection and bulk add logic - no code changes needed if
      AddTestsForm already accepts any sampleItemIds)
- [ ] T070 [US4] Update SampleManagement container to enable bulk selection of
      aliquots in frontend/src/components/sampleManagement/SampleManagement.js
      (update UI to show "Add Tests to Aliquots" button text when all selected
      samples are aliquots vs. "Add Tests to Samples" for mixed or original
      samples, display count of selected aliquots in button label, maintain
      existing TestSelector integration)
- [ ] T071 [P] [US4] Update AddTestsResponse handling to display per-aliquot
      results in frontend/src/components/sampleManagement/SampleManagement.js
      (show detailed success/skipped breakdown per aliquot external ID in Carbon
      notification or modal, highlight duplicates per aliquot in results
      display)

**Checkpoint Validation**: All user stories (1-4) should now be independently
functional. Users can search, add tests, create aliquots, and bulk-add tests to
aliquots. ALL tests from T066-T068 MUST pass.

---

## Phase 7: FHIR R4 Integration

**Purpose**: Extend FHIR transforms to support aliquot relationships and
quantity tracking

- [ ] T072 [P] Extend FhirTransformService.transformToSpecimen to add
      originalQuantity in
      src/main/java/org/openelisglobal/dataexchange/fhir/service/FhirTransformServiceImpl.java
      (add SpecimenContainerComponent with specimenQuantity from
      sampleItem.originalQuantity, set unit from unitOfMeasure, set
      system="http://unitsofmeasure.org")
- [ ] T073 [P] Add parent reference to Specimen transform in
      FhirTransformServiceImpl (check if sampleItem.parentSampleItem != null,
      create Reference to parent Specimen using parentSampleItem.fhirUuid, add
      to specimen.addParent())
- [ ] T074 [P] Add remainingQuantity custom extension to Specimen transform in
      FhirTransformServiceImpl (create Extension with
      url=fhirConfig.getOeFhirSystem()+"/remainingQuantity", create Quantity
      with sampleItem.remainingQuantity, add to specimen.addExtension())
- [ ] T075 Unit test for FHIR Specimen mapping in
      src/test/java/org/openelisglobal/dataexchange/fhir/service/FhirTransformServiceTest.java
      (test cases: (1) original sample has originalQuantity in container, (2)
      aliquot includes parent reference, (3) remainingQuantity in custom
      extension, (4) nested aliquot hierarchy reflected in parent chain)

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T076 [P] Add validation messages to messages_en.properties in
      src/main/resources/i18n/messages_en.properties
      (validation.parentSampleItemId.required,
      validation.quantityToTransfer.required, validation.quantityToTransfer.min,
      validation.notes.size, etc.)
- [ ] T077 [P] Add French translations to messages_fr.properties in
      src/main/resources/i18n/messages_fr.properties
- [ ] T078 [P] Code cleanup and refactoring across all service methods (extract
      common DTO mapping logic to utility class, extract hierarchy compilation
      to helper methods)
- [ ] T079 [P] Performance optimization for hierarchy queries (add database
      index on parent_sample_item_id if not in Liquibase, verify JOIN FETCH
      prevents N+1 queries with SQL logging)
- [ ] T080 [P] Security: Add RBAC checks to SampleManagementRestController (use
      @PreAuthorize annotations with roles: "ROLE_LAB_TECHNICIAN",
      "ROLE_LAB_MANAGER")
- [ ] T081 Run quickstart.md validation (follow quickstart steps to verify
      developer onboarding docs are accurate)

---

## Phase 9: Constitution Compliance Verification (OpenELIS Global 3.0)

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

- [ ] T082 **Configuration-Driven**: Verify no country-specific code branches
      introduced (code review for hardcoded country checks)
- [ ] T083 **Carbon Design System**: Audit UI - confirm @carbon/react used
      exclusively (grep frontend for "bootstrap", "tailwind", confirm none
      found)
- [ ] T084 **FHIR/IHE Compliance**: Validate FHIR Specimen resources against R4
      profiles (run FHIR validator on sample output, verify Specimen.parent
      structure)
- [ ] T085 **Layered Architecture**: Verify 5-layer pattern followed (check
      @Transactional only in service layer, verify no business logic in
      controllers, verify DAO only has data access)
- [ ] T086 **Test Coverage**: Run coverage report - confirm >80% backend, >70%
      frontend for new code (mvn verify for JaCoCo, npm test -- --coverage for
      Jest, review target/site/jacoco/index.html and
      frontend/coverage/index.html)
- [ ] T087 **Schema Management**: Verify ALL database changes use Liquibase
      changesets (check git diff for any .sql files outside liquibase directory,
      confirm no direct DDL)
- [ ] T088 **Internationalization**: Audit UI strings - confirm React Intl used
      for ALL text (grep frontend for hardcoded English strings like "Search",
      "Add Test", confirm wrapped in FormattedMessage)
- [ ] T089 **Security & Compliance**: Verify RBAC on endpoints (test endpoints
      without auth return 401, test with wrong role returns 403), verify audit
      trail fields populated (sys_user_id, lastupdated), verify input validation
      on all forms

**Verification Commands**:

```bash
# Backend: Code formatting (MUST run before each commit) + build + tests
mvn spotless:apply && mvn spotless:check && mvn clean install

# Frontend: Formatting (MUST run before each commit) + E2E tests
cd frontend && npm run format
# Run E2E tests individually (per Constitution V.5):
npm run cy:run -- --spec "cypress/e2e/sampleManagement.cy.js"
# Full suite only in CI/CD: npm run cy:run

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user
  stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P1 → P2 → P3)
- **FHIR Integration (Phase 7)**: Depends on Phase 3 (US1 - search) and Phase 5
  (US3 - aliquot) completion
- **Polish (Phase 8)**: Depends on all desired user stories being complete
- **Constitution Verification (Phase 9)**: Depends on all implementation phases
  complete

### User Story Dependencies

- **User Story 1 (P1) - Search**: Can start after Foundational (Phase 2) - No
  dependencies on other stories
- **User Story 2 (P1) - Add Tests**: Can start after Foundational (Phase 2) - No
  dependencies on other stories
- **User Story 3 (P2) - Aliquot**: Can start after Foundational (Phase 2) - No
  dependencies on other stories (independently testable)
- **User Story 4 (P3) - Bulk Add Tests to Aliquots**: Depends on US2 (add tests)
  and US3 (aliquot) completion - Integrates features from both

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- ORM validation tests (T006, T007) before entity modifications (T008-T010)
- Entities (T008-T010) before DAOs (T019-T022)
- DAOs before services
- Services before controllers
- Backend before frontend
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel (T002, T003, T004)
- All ORM validation tests marked [P] can run in parallel (T006, T007)
- All DTO creation tasks marked [P] can run in parallel (T011-T018)
- All DAO tasks marked [P] can run in parallel (T021, T022)
- Once Foundational phase completes, User Stories 1 and 2 can start in parallel
  (both P1 priority, no interdependencies)
- All tests for a user story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members
- Within each story: tests, entities, DAOs, form/DTOs marked [P] can be
  parallelized

---

## Parallel Example: User Story 1 (Search)

```bash
# Launch all tests for User Story 1 together:
Task T023: "Unit test for SampleManagementService.searchByAccessionNumber" [P]
Task T024: "DAO test for getSampleItemsWithHierarchy" [P]
Task T025: "Controller test for GET /api/sample-management/search" [P]
Task T026: "Frontend unit test for SampleSearch component" [P]
Task T027: "Frontend unit test for SampleResultsTable component" [P]
Task T028: "Cypress E2E test for sample search workflow" [P]

# After tests pass, launch all parallelizable implementation tasks:
Task T032: "Add validation and error handling to search endpoint" [P]
Task T033: "Create SampleSearch React component" [P]
Task T034: "Create SampleResultsTable React component" [P]
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2 Only)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational (T006-T022) - CRITICAL - blocks all stories
3. Complete Phase 3: User Story 1 - Search (T023-T036)
4. Complete Phase 4: User Story 2 - Add Tests (T037-T049)
5. **STOP and VALIDATE**: Test User Stories 1 + 2 together
6. Deploy/demo if ready

**MVP Delivers**: Core sample management with search and test ordering -
immediately useful to laboratory staff

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 (Search) → Test independently → Deploy/Demo (Basic search
   capability!)
3. Add User Story 2 (Add Tests) → Test independently → Deploy/Demo (Search +
   test ordering!)
4. Add User Story 3 (Aliquot) → Test independently → Deploy/Demo (Full
   aliquoting with volume tracking!)
5. Add User Story 4 (Bulk Tests to Aliquots) → Test independently → Deploy/Demo
   (Complete feature set!)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T022)
2. Once Foundational is done:
   - Developer A: User Story 1 (Search) - T023-T036
   - Developer B: User Story 2 (Add Tests) - T037-T049
   - After both complete, Developer C: User Story 3 (Aliquot) - T050-T065
   - After User Story 3, Developer D: User Story 4 (Bulk) - T066-T071
3. Stories complete and integrate independently

---

## Summary

- **Total Tasks**: 89 tasks
- **User Story 1 (Search)**: 14 tasks (6 tests + 8 implementation)
- **User Story 2 (Add Tests)**: 13 tasks (6 tests + 7 implementation)
- **User Story 3 (Aliquot)**: 16 tasks (6 tests + 10 implementation)
- **User Story 4 (Bulk Tests to Aliquots)**: 3 tasks (3 tests + 3
  implementation)
- **Setup**: 5 tasks
- **Foundational**: 17 tasks (2 ORM validation tests + 7 entities/DTOs + 4 DAOs)
- **FHIR Integration**: 4 tasks
- **Polish**: 6 tasks
- **Constitution Compliance**: 8 tasks
- **Parallel Opportunities**: 52 tasks marked [P]
- **MVP Scope**: Phases 1-4 (User Stories 1 + 2) = 41 tasks

**Independent Test Criteria**:

- **US1**: Search by accession number → displays results with hierarchy
- **US2**: Select sample → add tests → tests appear in sample details
- **US3**: Select sample → create aliquot → parent quantity reduced, new aliquot
  with .1 suffix
- **US4**: Select multiple aliquots → add test → all aliquots receive test

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD workflow)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Format validation: ALL tasks follow checklist format with checkbox, ID,
  optional [P]/[Story], and file paths
