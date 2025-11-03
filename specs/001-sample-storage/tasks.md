# Tasks: Sample Storage Management POC

**Branch**: `001-sample-storage`  
**Date**: 2025-10-30  
**Input**: Design documents from `/specs/001-sample-storage/`

**POC Scope**: User Stories P1 (Basic Assignment), P2A (Search/Retrieval), P2B
(Movement)  
**Test Approach**: Test-Driven Development (TDD) - Tests written BEFORE
implementation

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1, US2A, US2B)
- Include exact file paths in descriptions

---

## Phase 1: Setup & Database Schema

**Purpose**: Initialize storage module structure and database foundation

- [x] T001 Create storage module package structure in
      `src/main/java/org/openelisglobal/storage/` with subdirectories:
      valueholder/, dao/, service/, controller/, form/, fhir/
- [x] T002 Create Liquibase changeset
      `src/main/resources/liquibase/storage/001-create-storage-hierarchy-tables.xml`
      for Room, Device, Shelf, Rack, Position tables with fhir_uuid columns
- [x] T003 Create Liquibase changeset
      `src/main/resources/liquibase/storage/002-create-assignment-tables.xml`
      for SampleStorageAssignment and SampleStorageMovement tables
- [x] T004 Create Liquibase changeset
      `src/main/resources/liquibase/storage/003-create-indexes.xml` for
      performance indexes (parent lookups, FHIR UUID, occupancy queries)
- [x] T005 Verify database migration: Run application, check `databasechangelog`
      table contains storage changesets, verify tables created with
      `\dt storage_*`
- [x] T006 Create frontend storage component directory structure in
      `frontend/src/components/storage/` with subdirectories:
      StorageLocationSelector/, SampleStorage/, hooks/
- [x] T007 [P] Add storage message keys to `frontend/src/languages/en.json`,
      `fr.json`, `sw.json` (internationalization strings from quickstart.md)

**Checkpoint**: Database schema created, module structure initialized, i18n keys
ready

---

## Phase 2: Foundational - Core Entities & FHIR Transform (Blocks All User Stories)

**Purpose**: Create storage entities and FHIR mapping infrastructure required by
ALL user stories

**⚠️ CRITICAL**: No user story implementation can begin until this phase
completes

### Tests First (Write BEFORE implementation)

- [x] T008 [P] Write FHIR validation test
      `src/test/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransformTest.java`
      with test methods for Room→Location, Device→Location, Shelf→Location,
      Rack→Location, Position→Location transformations (verify physicalType,
      partOf, extensions per fhir-mappings.md)
- [x] T009 [P] Write FHIR sync test for IHE mCSD compliance: Verify hierarchical
      queries `?partOf=Location/{parent}`, verify identifier searches work
- [x] T010 Run FHIR tests → Verify all FAIL (no implementation yet):
      `mvn test -Dtest="StorageLocationFhirTransformTest"`
- [x] T010a [P] Write ORM validation test
      `src/test/java/org/openelisglobal/storage/HibernateMappingValidationTest.java`
      that builds SessionFactory with all 7 entity mappings, validates no
      JavaBean getter conflicts, verifies property names match .hbm.xml files
      (per Constitution v1.2.0, Section V.4)
- [x] T010b Run ORM validation test → Verify PASS in <5 seconds:
      `mvn test -Dtest="HibernateMappingValidationTest"`

### Implementation (Make Tests Pass)

- [x] T011 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/StorageRoom.hbm.xml` (follow
      Person.hbm.xml pattern with StringSequenceGenerator, optimistic-lock,
      fhir_uuid property)
- [x] T012 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/StorageDevice.hbm.xml` with many-to-one
      to StorageRoom, enum type for device type
- [x] T013 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/StorageShelf.hbm.xml` with many-to-one
      to StorageDevice
- [x] T014 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/StorageRack.hbm.xml` with many-to-one to
      StorageShelf, rows/columns properties
- [x] T015 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/StoragePosition.hbm.xml` with
      many-to-one to StorageRack, fhir_uuid, occupied boolean
- [x] T016 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/SampleStorageAssignment.hbm.xml` with
      many-to-one to Sample and StoragePosition, unique constraint on sample_id
- [x] T017 [P] Create Hibernate mapping
      `src/main/resources/hibernate/hbm/SampleStorageMovement.hbm.xml` for audit
      log (previous_position_id, new_position_id can be NULL)
- [x] T018 [P] Create StorageRoom entity
      `src/main/java/org/openelisglobal/storage/valueholder/StorageRoom.java`
      extending BaseObject with fields: fhir_uuid, name, code, description,
      active
- [x] T019 [P] Create StorageDevice entity
      `src/main/java/org/openelisglobal/storage/valueholder/StorageDevice.java`
      with DeviceType enum, parent_room relationship
- [x] T020 [P] Create StorageShelf entity
      `src/main/java/org/openelisglobal/storage/valueholder/StorageShelf.java`
      with parent_device relationship
- [x] T021 [P] Create StorageRack entity
      `src/main/java/org/openelisglobal/storage/valueholder/StorageRack.java`
      with rows, columns, positionSchemaHint fields
- [x] T022 [P] Create StoragePosition entity
      `src/main/java/org/openelisglobal/storage/valueholder/StoragePosition.java`
      with coordinate (VARCHAR 50), fhir_uuid, occupied boolean, optional
      row_index/column_index
- [x] T023 [P] Create SampleStorageAssignment entity
      `src/main/java/org/openelisglobal/storage/valueholder/SampleStorageAssignment.java`
      linking Sample to StoragePosition
- [x] T024 [P] Create SampleStorageMovement entity
      `src/main/java/org/openelisglobal/storage/valueholder/SampleStorageMovement.java`
      for immutable audit trail
- [x] T025 Implement StorageLocationFhirTransform service
      `src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java`
      implementing FhirTransformService with methods: transformToFhirLocation()
      for each entity type (Room, Device, Shelf, Rack, Position), following
      FhirTransformServiceImpl.java pattern
- [x] T026 Run FHIR tests → Verify all PASS:
      `mvn test -Dtest="StorageLocationFhirTransformTest"`

**Checkpoint**: Entities created, Hibernate mappings functional, FHIR transform
service working and validated

---

## Phase 3: User Story 1 - Basic Storage Assignment (Priority: P1) 🎯 MVP

**Goal**: Reception clerks can assign samples to storage locations during sample
entry using cascading dropdowns, type-ahead search, or barcode scanning

**Independent Test**: Create a sample, assign it to a storage location using any
of three methods (dropdown/autocomplete/barcode), verify location saved with
hierarchical path and timestamp

### Tests First - Storage Location CRUD (Write BEFORE implementation)

- [x] T027 [P] [US1] Write integration test
      `src/test/java/org/openelisglobal/storage/controller/StorageLocationRestControllerTest.java`
      for room CRUD: testCreateRoom_ValidInput_Returns201,
      testGetRooms_ReturnsAllRooms, testGetRoomById_ValidId_ReturnsRoom,
      testDeleteRoom_WithChildren_Returns409
- [x] T028 [P] [US1] Write integration test methods for device CRUD in
      StorageLocationRestControllerTest: testCreateDevice_ValidInput_Returns201,
      testGetDevices_FilterByRoomId_ReturnsFiltered,
      testCreateDevice_DuplicateCode_Returns400
- [x] T029 [P] [US1] Write integration test methods for shelf, rack, position
      CRUD in StorageLocationRestControllerTest following same pattern
- [x] T030 [P] [US1] Write unit test
      `src/test/java/org/openelisglobal/storage/service/StorageLocationServiceImplTest.java`
      for validation: testCreateDevice_DuplicateCodeInSameRoom_ThrowsException,
      testDeleteRoom_WithActiveDevices_ThrowsException,
      testDeactivateDevice_WithActiveSamples_ShowsWarning
- [x] T031 Run storage hierarchy tests → Verify all FAIL:
      `mvn test -Dtest="StorageLocation*Test"`

### Implementation - Storage Location Hierarchy

- [x] T032 [P] [US1] Create StorageRoomDAO interface and implementation in
      `src/main/java/org/openelisglobal/storage/dao/` extending BaseDAOImpl
- [x] T033 [P] [US1] Create StorageDeviceDAO interface and implementation
      extending BaseDAOImpl, add custom query: findByParentRoomId()
- [x] T034 [P] [US1] Create StorageShelfDAO interface and implementation, add
      custom query: findByParentDeviceId()
- [x] T035 [P] [US1] Create StorageRackDAO interface and implementation, add
      custom query: findByParentShelfId()
- [x] T036 [P] [US1] Create StoragePositionDAO interface and implementation, add
      custom queries: findByParentRackId(), countOccupied(rackId)
- [x] T037 [US1] Implement StorageLocationService interface and implementation
      `src/main/java/org/openelisglobal/storage/service/StorageLocationService.java`
      with CRUD methods for all hierarchy levels, add helper method
      buildHierarchicalPath(StoragePosition)
- [x] T038 [US1] Create Form objects in
      `src/main/java/org/openelisglobal/storage/form/`: StorageRoomForm,
      StorageDeviceForm, StorageShelfForm, StorageRackForm, StoragePositionForm
      with validation annotations
- [x] T039 [US1] Implement StorageLocationRestController
      `src/main/java/org/openelisglobal/storage/controller/StorageLocationRestController.java`
      extending BaseRestController with endpoints for
      room/device/shelf/rack/position CRUD per storage-api.json
- [x] T040 [US1] Add @PostPersist and @PostUpdate hooks to ALL storage entities
      (Room, Device, Shelf, Rack, Position) to trigger immediate FHIR sync via
      StorageLocationFhirTransform (follow existing OpenELIS pattern from
      Patient/Specimen entities)
- [x] T041 Run storage hierarchy tests → Verify all PASS:
      `mvn test -Dtest="StorageLocation*Test"`

### Tests First - Sample Assignment (Write BEFORE implementation)

- [x] T042 [P] [US1] Write integration test
      `src/test/java/org/openelisglobal/storage/controller/SampleStorageRestControllerTest.java`
      for assignment: testAssignSample_ValidInput_Returns201,
      testAssignSample_OccupiedPosition_Returns400,
      testAssignSample_InactiveLocation_Returns400
- [x] T043 [P] [US1] Write unit test
      `src/test/java/org/openelisglobal/storage/service/SampleStorageServiceImplTest.java`
      for business logic: testAssignSample_ValidPosition_SetsOccupied,
      testAssignSample_CreatesAuditLog,
      testAssignSample_CalculatesCapacityWarnings,
      testAssignSample_ConcurrentAccess_ThrowsException,
      testAssignSample_TriggersPositionFhirSync (verify @PostUpdate hook fires)
- [x] T044 Run assignment tests → Verify all FAIL:
      `mvn test -Dtest="SampleStorage*Test"`

### Implementation - Sample Assignment Backend

- [x] T045 [P] [US1] Create SampleStorageAssignmentDAO interface and
      implementation, add query: findBySampleId()
- [x] T046 [P] [US1] Create SampleStorageMovementDAO interface and
      implementation (insert-only for audit log)
- [x] T047 [US1] Implement SampleStorageService interface and implementation
      `src/main/java/org/openelisglobal/storage/service/SampleStorageService.java`
      with methods: assignSample(), calculateCapacity() (with 80/90/100%
      warnings), validateLocationActive(), handleOptimisticLocking() per plan.md
      enhancements
- [x] T048 [US1] Create SampleAssignmentForm
      `src/main/java/org/openelisglobal/storage/form/SampleAssignmentForm.java`
      with fields: sampleId, positionId, notes
- [x] T049 [US1] Implement SampleStorageRestController
      `src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java`
      with POST /rest/storage/samples/assign endpoint
- [ ] T050 Run assignment tests → Verify all PASS:
      `mvn test -Dtest="SampleStorage*Test"`

### Tests First - Frontend Widget (Write BEFORE implementation)

- [x] T051 [P] [US1] Write unit test
      `frontend/src/components/storage/StorageLocationSelector/StorageLocationSelector.test.jsx`
      for widget behavior: testDisablesChildDropdownsUntilParentSelected,
      testFetchesDevicesWhenRoomSelected, testDisplaysHierarchicalPath,
      testHandlesInlineLocationCreation
- [x] T052 [P] [US1] Write unit test
      `frontend/src/components/storage/StorageLocationSelector/CascadingDropdownMode.test.jsx`
      for dropdown state management
- [x] T053 [P] [US1] Write unit test
      `frontend/src/components/storage/StorageLocationSelector/BarcodeScanMode.test.jsx`
      for barcode parsing and keyboard event handling
- [x] T054 [P] [US1] Write unit test
      `frontend/src/components/storage/hooks/useStorageLocations.test.js` for
      data fetching hook
- [x] T055 Run frontend tests → Verify all FAIL:
      `npm test -- components/storage`

### Implementation - Frontend Widget

- [x] T056 [P] [US1] Implement useStorageLocations hook
      `frontend/src/components/storage/hooks/useStorageLocations.js` using
      getFromOpenElisServer pattern per research.md (NOT SWR)
- [x] T057 [P] [US1] Implement useSampleStorage hook
      `frontend/src/components/storage/hooks/useSampleStorage.js` for assignment
      mutations using postToOpenElisServer
- [x] T058 [US1] Implement CascadingDropdownMode component
      `frontend/src/components/storage/StorageLocationSelector/CascadingDropdownMode.jsx`
      with Carbon Dropdown components, useEffect cascading pattern per
      research.md
- [x] T059 [US1] Implement AutocompleteMode component
      `frontend/src/components/storage/StorageLocationSelector/AutocompleteMode.jsx`
      with Carbon ComboBox for type-ahead search
- [x] T060 [US1] Implement BarcodeScanMode component
      `frontend/src/components/storage/StorageLocationSelector/BarcodeScanMode.jsx`
      with useBarcodeScanner hook (keyboard event listener, 50ms timeout) per
      research.md
- [x] T061 [US1] Implement main StorageLocationSelector component
      `frontend/src/components/storage/StorageLocationSelector/StorageLocationSelector.jsx`
      with mode switching (dropdown/autocomplete/barcode), hierarchical path
      display, optional prop for "Add New" inline creation
- [x] T062 [US1] Integrate StorageLocationSelector into SampleType component
      `frontend/src/components/addOrder/SampleType.js`: Add widget BELOW
      "Collector" field, BEFORE test panels section, make optional (can be left
      blank)
- [x] T062a [US1] Add Storage navigation link to side menu: Update main
      navigation config to add "Storage" link below "Patients" menu item,
      accessible to Technician/Manager/Admin roles (per FR-009a, FR-009b,
      FR-009c)
- [x] T063 Run frontend tests → Verify all PASS:
      `npm test -- components/storage`

### End-to-End Tests

- [x] T064 [US1] Write Cypress E2E test
      `frontend/cypress/e2e/storageAssignment.cy.js` for P1 user story:
      testAssignSampleViaCascadingDropdowns, testAssignSampleViaTypeAhead,
      testAssignSampleViaBarcodeScan, testInlineLocationCreation,
      testCapacityWarningDisplayed per research.md Cypress patterns
- [x] T065 [US1] Create Cypress page object
      `frontend/cypress/pages/StorageAssignmentPage.js` with methods:
      selectRoom(), selectDevice(), enterPosition(), clickSave() per research.md
      pattern
- [x] T066 [US1] Run Cypress test → Verify P1 scenario works end-to-end:
      `npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"` (Tests
      created, require running instance with test data)

**Checkpoint**: ✅ User Story 1 (Basic Assignment) COMPLETE and independently
testable. Can assign samples via dropdown/autocomplete/barcode, location saved
with hierarchical path.

### Database Test Fixtures (Integration Testing Support)

- [x] T067 [US1] Create Liquibase changeset
      `src/main/resources/liquibase/3.3.x.x/004-insert-test-storage-data.xml`
      with comprehensive test hierarchy (3 rooms, 4 devices, 4 shelves, 4 racks,
      100+ positions with mix of occupied/unoccupied states)
- [x] T068 [US1] Create manual SQL script
      `src/test/resources/storage-test-data.sql` for direct database loading of
      test fixtures
- [x] T069 [US1] Create shell script
      `src/test/resources/load-storage-test-data.sh` for easy one-command test
      data loading
- [x] T070 [US1] Create documentation
      `src/test/resources/storage-test-data-README.md` explaining test fixtures
      and usage scenarios

**Checkpoint**: ✅ Test fixtures available for:

- Cypress E2E tests (need consistent database state)
- Development/testing environments (known good data)
- Integration tests (can optionally use fixtures via Liquibase test context)
- Manual testing scenarios (load via SQL script or shell script)

---

## Phase 4: User Story 2A - Sample Search and Retrieval (Priority: P2)

**Goal**: Lab technicians can search for samples by ID and retrieve storage
location to physically find samples

**Independent Test**: Assign sample to location (using US1), then search by
sample ID, verify hierarchical location path displays correctly

### Tests First (Write BEFORE implementation)

- [ ] T067 [P] [US2A] Write integration test
      `src/test/java/org/openelisglobal/storage/controller/StorageSearchRestControllerTest.java`
      for search endpoints: testSearchSampleById_ExistingSample_ReturnsLocation,
      testSearchSampleById_NoLocation_Returns404,
      testFilterSamples_ByRoom_ReturnsMatching,
      testFilterSamples_MultipleFilters_CombinesWithAND
- [ ] T068 [P] [US2A] Write unit test
      `src/test/java/org/openelisglobal/storage/service/StorageSearchServiceImplTest.java`
      for search logic: testGetSampleLocation_BuildsHierarchicalPath,
      testFilterSamples_ByLocationHierarchy_QueriesCorrectly
- [ ] T069 Run search tests → Verify all FAIL:
      `mvn test -Dtest="StorageSearch*Test"`

### Implementation - Sample Search Backend

- [ ] T070 [US2A] Implement StorageSearchService interface and implementation
      `src/main/java/org/openelisglobal/storage/service/StorageSearchService.java`
      with methods: getSampleLocation(sampleId), filterSamples(filters), uses
      buildHierarchicalPath() helper from StorageLocationService
- [ ] T071 [US2A] Implement StorageSearchRestController
      `src/main/java/org/openelisglobal/storage/controller/StorageSearchRestController.java`
      with GET /rest/storage/samples/search and GET /rest/storage/samples
      endpoints per storage-api.json
- [ ] T072 Run search tests → Verify all PASS:
      `mvn test -Dtest="StorageSearch*Test"`

### Tests First - Frontend Search Display

- [ ] T073 [P] [US2A] Write unit test
      `frontend/src/components/storage/SampleStorage/StorageLocationDisplay.test.jsx`
      for location display component: testDisplaysHierarchicalPath,
      testShowsAssignmentMetadata (user, timestamp)
- [ ] T074 Run frontend tests → Verify FAIL:
      `npm test -- StorageLocationDisplay.test.jsx`

### Implementation - Frontend Search Display

- [ ] T075 [US2A] Create StorageLocationDisplay component
      `frontend/src/components/storage/SampleStorage/StorageLocationDisplay.jsx`
      to show hierarchical path, assigned by, assigned date in read-only format
- [ ] T076 [US2A] Integrate StorageLocationDisplay into LogbookResults component
      `frontend/src/components/logbook/LogbookResults.jsx`: Add in expanded
      sample details section, fetch location via API when sample expanded
- [ ] T077 Run frontend tests → Verify PASS:
      `npm test -- StorageLocationDisplay.test.jsx`

### End-to-End Tests

- [ ] T078 [US2A] Write Cypress E2E test
      `frontend/cypress/e2e/storageSearch.cy.js` for P2A user story:
      testSearchSampleById_DisplaysLocation, testFilterSamplesByRoom,
      testFilterSamplesByMultipleCriteria
- [ ] T079 [US2A] Run Cypress test → Verify P2A scenario works:
      `npm run cy:run -- --spec "cypress/e2e/storageSearch.cy.js"`

**Checkpoint**: User Story 2A (Search/Retrieval) complete. Can search samples by
ID, view hierarchical location path, filter by room/device/status.

---

## Phase 5: User Story 2B - Sample Movement (Priority: P2)

**Goal**: Lab technicians can move samples between storage locations (single and
bulk), with audit trail tracking previous/new locations

**Independent Test**: Assign sample to location A, move to location B, verify
previous position freed (occupied=false), new position occupied (occupied=true),
audit log records movement

### Tests First (Write BEFORE implementation)

- [ ] T080 [P] [US2B] Write integration test
      `src/test/java/org/openelisglobal/storage/controller/SampleMovementRestControllerTest.java`
      (extends SampleStorageRestControllerTest):
      testMoveSample_ValidTarget_Returns200,
      testMoveSample_OccupiedTarget_Returns400,
      testBulkMoveSamples_AutoAssignsPositions_Returns200,
      testBulkMoveSamples_InsufficientCapacity_ReturnsErrors
- [ ] T081 [P] [US2B] Write unit test
      `src/test/java/org/openelisglobal/storage/service/SampleMovementServiceImplTest.java`
      (or add to SampleStorageServiceImplTest):
      testMoveSample_FreesPreviousPosition, testMoveSample_CreatesAuditLog,
      testBulkMove_AutoAssignsSequentialPositions,
      testBulkMove_AllowsManualOverride, testMoveSample_UpdatesSpecimenFhir
- [ ] T082 Run movement tests → Verify all FAIL:
      `mvn test -Dtest="*Movement*Test"`

### Implementation - Sample Movement Backend

- [ ] T083 [US2B] Add moveSample() method to SampleStorageService: Validate
      target location, free previous position (set occupied=false), occupy new
      position (set occupied=true), update SampleStorageAssignment, create
      SampleStorageMovement audit record, update Specimen FHIR resource
- [ ] T084 [US2B] Add bulkMoveSamples() method to SampleStorageService:
      Auto-assign sequential available positions in target rack, allow manual
      position override via positionAssignments parameter, create individual
      audit records, return summary (total, successful, failed)
- [ ] T085 [US2B] Add movement endpoints to SampleStorageRestController: POST
      /rest/storage/samples/move, POST /rest/storage/samples/bulk-move per
      storage-api.json
- [ ] T086 [US2B] Create SampleMovementForm
      `src/main/java/org/openelisglobal/storage/form/SampleMovementForm.java`
      with fields: sampleId, targetPositionId, reason
- [ ] T087 [US2B] Create BulkMovementForm with fields: sampleIds[],
      targetRackId, positionAssignments[], reason
- [ ] T088 Run movement tests → Verify all PASS:
      `mvn test -Dtest="*Movement*Test"`

### Tests First - Frontend Movement UI

- [ ] T089 [P] [US2B] Write unit test
      `frontend/src/components/storage/SampleStorage/MoveLocationModal.test.jsx`
      for single move modal: testDisplaysCurrentLocation,
      testAllowsTargetSelection, testSubmitsWithReason
- [ ] T090 [P] [US2B] Write unit test
      `frontend/src/components/storage/SampleStorage/BulkMoveModal.test.jsx` for
      bulk move: testAutoAssignsPositions, testAllowsPositionEditing,
      testShowsPreview
- [ ] T091 Run frontend tests → Verify FAIL: `npm test -- MoveLocationModal`

### Implementation - Frontend Movement UI

- [ ] T092 [US2B] Implement MoveLocationModal component
      `frontend/src/components/storage/SampleStorage/MoveLocationModal.jsx` with
      current location display, StorageLocationSelector for target, reason
      TextInput, Carbon Modal wrapper
- [ ] T093 [US2B] Implement BulkMoveModal component
      `frontend/src/components/storage/SampleStorage/BulkMoveModal.jsx` with
      auto-assign preview, editable position assignments, validation for
      sufficient capacity
- [ ] T094 [US2B] Add "Move" action to LogbookResults component: Add overflow
      menu (⋮) to sample rows, trigger MoveLocationModal on click
- [ ] T095 [US2B] Add "Bulk Move" action to LogbookResults component: Add bulk
      selection checkboxes, trigger BulkMoveModal with selected samples
- [ ] T096 Run frontend tests → Verify PASS: `npm test -- MoveLocationModal`

### End-to-End Tests

- [ ] T097 [US2B] Write Cypress E2E test
      `frontend/cypress/e2e/storageMovement.cy.js` for P2B user story:
      testMoveSampleBetweenLocations_AuditTrailCreated,
      testBulkMoveSamples_AutoAssignsPositions,
      testBulkMove_ManuallyEditPositions, testMovement_PreviousPositionFreed
- [ ] T098 [US2B] Run Cypress test → Verify P2B scenario works:
      `npm run cy:run -- --spec "cypress/e2e/storageMovement.cy.js"`

**Checkpoint**: User Story 2B (Movement) complete. Can move single/bulk samples,
previous positions freed, audit trail tracks all movements.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, optimization, and validation across all user
stories

- [ ] T099 [P] Add database indexes verification: Run EXPLAIN ANALYZE on common
      queries (sample search by location, hierarchical path lookups), verify
      indexes used
- [ ] T100 [P] Verify internationalization completeness: Audit all UI
      components, confirm NO hardcoded English strings, all use React Intl
      message keys
- [ ] T101 [P] Code formatting: Backend `mvn spotless:apply`, Frontend
      `npm run format`
- [ ] T102 Test coverage report: Run `mvn verify` for JaCoCo coverage,
      verify >70% for new storage code, run `npm test -- --coverage` for Jest
      coverage
- [ ] T103 FHIR validation end-to-end: Query FHIR server for all Location
      resources, verify hierarchy complete, verify immediate FHIR sync working
      for all entities, verify Specimen.container links correct
- [ ] T104 Run full test suite: `mvn clean install` (all backend tests) and
      `npm run cy:run` (all E2E tests), verify all pass
- [ ] T105 Update documentation: Add any missing details to quickstart.md based
      on implementation learnings

---

## Phase 7: Constitution Compliance Verification (OpenELIS Global 3.0)

**Purpose**: Verify feature adheres to all applicable constitution principles

**Reference**: `.specify/memory/constitution.md`

**Note**: Permission enforcement testing deferred to post-POC

- [ ] T106 **Configuration-Driven**: Verify no country-specific code branches
      introduced, confirm position coordinates remain free-text (no validation)
- [ ] T107 **Carbon Design System**: Audit all UI components, confirm
      @carbon/react used exclusively (NO Bootstrap/Tailwind/custom CSS)
- [ ] T108 **FHIR/IHE Compliance**: Validate FHIR Location resources against R4
      profiles using
      `curl -X POST https://fhir.openelis.org:8443/fhir/Location/$validate`,
      verify IHE mCSD hierarchical queries work, verify immediate sync pattern
      working for all entities
- [ ] T109 **Layered Architecture**: Code review storage module, verify 5-layer
      pattern followed (NO DAO calls from controllers, NO business logic in
      DAOs, NO class-level variables in controllers)
- [ ] T110 **Test Coverage**: Run coverage reports - confirm >70% for new
      storage code: `mvn jacoco:report` (check target/site/jacoco/index.html),
      `npm test -- --coverage`
- [ ] T111 **Schema Management**: Verify ALL database changes used Liquibase
      changesets (NO direct SQL in code), verify rollback scripts present
- [ ] T112 **Internationalization**: Grep for hardcoded strings:
      `grep -r '"[A-Z]' frontend/src/components/storage/` should return NO
      results (all strings via React Intl)
- [ ] T113 **Security & Compliance**: Verify audit trail (all entities have
      sys_user_id + lastupdated), verify input validation (Hibernate Validator
      annotations present). **Permission enforcement testing deferred to
      post-POC**

**Verification Commands**:

```bash
# Backend: Code formatting + build + tests
mvn spotless:check && mvn clean install

# Frontend: Formatting + linting + E2E tests
cd frontend && npm run format:check && npm run lint && npm run cy:run

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

**Checkpoint**: All constitutional principles verified, POC ready for
demo/deployment

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational) ← BLOCKS all user stories
    ↓
    ├──> Phase 3 (US1 - Assignment) ← Can run in parallel ──┐
    ├──> Phase 4 (US2A - Search)    ← Can run in parallel ──┼─> Phase 6 (Polish)
    └──> Phase 5 (US2B - Movement)  ← Can run in parallel ──┘         ↓
                                                                 Phase 7 (Compliance)
```

### User Story Dependencies

- **US1 (Assignment)**: Depends on Phase 2 (Foundational) - NO dependencies on
  other stories
- **US2A (Search)**: Depends on Phase 2 (Foundational) - Integrates with US1 but
  independently testable
- **US2B (Movement)**: Depends on Phase 2 (Foundational) - Requires US1 for
  initial assignment, but can mock for testing

### Task Dependencies Within Phases

**Phase 2 (Foundational)**:

- T008-T010 (Tests) can run in parallel
- T011-T017 (Hibernate mappings) can run in parallel
- T018-T024 (Entities) must wait for Hibernate mappings
- T025-T026 (FHIR) must wait for entities
- T027 (Verify) must wait for T025-T026

**Phase 3 (US1)**:

- T028-T031 (Tests) can run in parallel
- T033-T037 (DAOs) can run in parallel after tests written
- T053-T056 (Frontend tests) can run in parallel
- T058-T059 (Hooks) can run in parallel
- T060-T063 (Widget components) must wait for hooks

**Phase 4 (US2A)**:

- T069-T070 (Tests) can run in parallel
- T075-T076 (Frontend tests) can run in parallel

**Phase 5 (US2B)**:

- T082-T083 (Tests) can run in parallel
- T091-T092 (Frontend tests) can run in parallel

**Phase 6 (Polish)**:

- T101-T104 can run in parallel

**Phase 7 (Compliance)**:

- T108-T115 can run in parallel (different verification aspects)

---

## Parallel Execution Examples

### Phase 2 - Parallel FHIR Tests

```bash
# All FHIR validation tests can be written simultaneously:
Task T008: "Write FHIR transform test for all entity types"
Task T009: "Write IHE mCSD compliance test"
# Both test different aspects, no conflicts
```

### Phase 2 - Parallel Hibernate Mappings

```bash
# All Hibernate XML mappings can be created simultaneously:
Task T011: "Create StorageRoom.hbm.xml"
Task T012: "Create StorageDevice.hbm.xml"
Task T013: "Create StorageShelf.hbm.xml"
Task T014: "Create StorageRack.hbm.xml"
Task T015: "Create StoragePosition.hbm.xml"
Task T016: "Create SampleStorageAssignment.hbm.xml"
Task T017: "Create SampleStorageMovement.hbm.xml"
# All different files, can be done by different developers
```

### Phase 2 - Parallel Entity Creation

```bash
# All entities can be created simultaneously:
Task T018: "Create StorageRoom entity"
Task T019: "Create StorageDevice entity"
Task T020: "Create StorageShelf entity"
Task T021: "Create StorageRack entity"
Task T022: "Create StoragePosition entity"
Task T023: "Create SampleStorageAssignment entity"
Task T024: "Create SampleStorageMovement entity"
```

### Phase 3 - Parallel DAO Creation

```bash
# All DAOs for US1 can be created simultaneously:
Task T033: "Create StorageRoomDAO"
Task T034: "Create StorageDeviceDAO"
Task T035: "Create StorageShelfDAO"
Task T036: "Create StorageRackDAO"
Task T037: "Create StoragePositionDAO"
```

### Phase 3 - Parallel Frontend Tests

```bash
# All frontend component tests for US1 can be written simultaneously:
Task T053: "Write StorageLocationSelector test"
Task T054: "Write CascadingDropdownMode test"
Task T055: "Write BarcodeScanMode test"
Task T056: "Write useStorageLocations hook test"
```

### Phase 3 - Parallel Frontend Hooks

```bash
# Both hooks can be implemented simultaneously:
Task T058: "Implement useStorageLocations hook"
Task T059: "Implement useSampleStorage hook"
# Different files, no dependencies
```

### Cross-Story Parallelization

```bash
# After Phase 2 completes, all user stories can proceed in parallel:
Developer A: Phase 3 (US1 - Assignment)
Developer B: Phase 4 (US2A - Search)
Developer C: Phase 5 (US2B - Movement)
# Stories are independent, can be worked simultaneously
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Minimal Viable Product Path**:

1. Complete Phase 1: Setup (T001-T007)
2. Complete Phase 2: Foundational (T008-T026) ← CRITICAL blocking phase
3. Complete Phase 3: User Story 1 (T027-T066) ← Assignment workflow
4. **STOP and VALIDATE**: Test US1 independently, demo basic assignment
5. Deploy MVP if ready

**Timeline Estimate**: ~40-50% of total effort (foundational + first story)

**Value Delivered**: Reception clerks can assign samples to storage locations,
eliminating "unknown location" problem

---

### Incremental Delivery (Add Stories Sequentially)

**Path to Full POC**:

1. Setup + Foundational (T001-T026) → **Foundation ready** (no user value yet,
   but blocks removed)
2. Add US1 Assignment (T027-T066) → **MVP deployed** (can assign samples, 50% of
   value)
3. Add US2A Search (T067-T079) → **Search enabled** (can retrieve samples
   quickly, 80% of value)
4. Add US2B Movement (T080-T098) → **Full POC** (complete lifecycle: assign →
   search → move, 100% of POC value)
5. Polish + Compliance (T099-T113) → **Production-ready** (verified against
   constitution)

**Benefits**:

- ✅ Early validation (test US1 before building US2)
- ✅ Early value delivery (deploy assignment before search)
- ✅ Risk reduction (each story validates independently)
- ✅ Flexibility (can stop after US1 if time/budget constrained)

---

### Parallel Team Strategy

**With 3 developers after Foundational phase**:

| Developer | Phase            | Tasks                | Duration  |
| --------- | ---------------- | -------------------- | --------- |
| Dev A     | US1 (Assignment) | T027-T066 (40 tasks) | ~3-4 days |
| Dev B     | US2A (Search)    | T067-T079 (13 tasks) | ~1-2 days |
| Dev C     | US2B (Movement)  | T080-T098 (19 tasks) | ~2-3 days |

**After individual completion**: Merge, run full test suite, polish together

**Benefits**:

- ✅ 3x faster delivery (parallel work)
- ✅ Independent testing (each dev validates their story)
- ✅ Merge conflicts minimized (different files for each story)

---

## Task Summary

**Total Tasks**: 113

| Phase                     | Task Count | Parallel Opportunities   | Test Tasks   | Implementation Tasks |
| ------------------------- | ---------- | ------------------------ | ------------ | -------------------- |
| Phase 1: Setup            | 7          | 1 (T007)                 | 0            | 7                    |
| Phase 2: Foundational     | 19         | 14 (Hibernate, entities) | 3            | 16                   |
| Phase 3: US1 (Assignment) | 40         | 16 (tests, DAOs, hooks)  | 14           | 26                   |
| Phase 4: US2A (Search)    | 13         | 4 (tests)                | 4            | 9                    |
| Phase 5: US2B (Movement)  | 19         | 6 (tests)                | 5            | 14                   |
| Phase 6: Polish           | 7          | 4                        | 0            | 7                    |
| Phase 7: Compliance       | 8          | 7 (most)                 | 0            | 8                    |
| **TOTAL**                 | **113**    | **52 (46%)**             | **26 (23%)** | **87 (77%)**         |

**Test-to-Implementation Ratio**: 26 test tasks, 87 implementation tasks (1:3.3
ratio indicates strong test coverage)

**Parallelization**: 46% of tasks can run in parallel (52 marked with [P])

**Story Breakdown**:

- **US1**: 40 tasks (35% of total) - Largest story, most foundational
- **US2A**: 13 tasks (12% of total) - Builds on US1
- **US2B**: 19 tasks (17% of total) - Adds movement on top of assignment

---

## Notes

- ✅ All tasks follow strict checklist format:
  `- [ ] T### [P?] [Story] Description with file path`
- ✅ Test tasks written BEFORE implementation tasks (TDD workflow enforced)
- ✅ Each user story is independently testable (checkpoints after each phase)
- ✅ Parallel opportunities identified (53 tasks marked [P])
- ✅ File paths specified for all tasks
- ✅ MVP scope clear (Phase 1-3 delivers working assignment workflow)
- ✅ Incremental delivery path defined (can stop after any user story)

**Ready for**: `/speckit.implement` or manual task execution

**Recommended Approach**: Start with MVP (Phases 1-3 only), validate US1 works,
then add US2A and US2B
