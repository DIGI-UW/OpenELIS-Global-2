<!-- d1876c4e-8378-4d9d-a3c7-2e89f7ab1381 a5f99679-9d6b-4301-8e9a-c955df9089d9 -->

# SampleItem Storage Remediation Plan

## Overview

This plan addresses the critical inconsistency where the specification requires
SampleItem-level storage tracking (`sample_item_id`), but the implementation
uses Sample-level tracking (`sample_id`). It also consolidates Liquibase
changesets into a clean final state without development artifacts.

## Stage 1: Liquibase Changeset Consolidation ✅ COMPLETE

**Objective**: Replace incremental development changesets with clean,
consolidated changesets representing the final database schema state.

**Status**: ✅ **COMPLETE** - Changesets 002 and 003 updated to use
`sample_item_id`. Development artifacts (004-009) review pending but low
priority.

### 1.1 Audit Current Changesets ✅

- Review all changesets in `src/main/resources/liquibase/3.3.x.x/`:
- `001-create-storage-hierarchy-tables.xml` (Room, Device, Shelf, Rack,
  Position)
- `002-create-assignment-tables.xml` (uses `sample_id` - needs fix)
- `003-create-indexes.xml` (references `sample_id` - needs fix)
- Other changesets (004-009) for any dependencies

### 1.2 Create Consolidated Assignment Tables Changeset

**File**:
`src/main/resources/liquibase/3.3.x.x/002-create-assignment-tables.xml`

**Action**: Replace entire changeset with final state:

- Change `sample_id` → `sample_item_id` in `sample_storage_assignment` table
- Change `sample_id` → `sample_item_id` in `sample_storage_movement` table
- Update foreign key references: `sample_item(id)` instead of `sample(id)`
- Update unique constraint: `UNIQUE (sample_item_id)` instead of
  `UNIQUE (sample_id)`
- Keep all other columns (location_id, location_type, position_coordinate, etc.)
- Keep all CHECK constraints
- Update rollback statements

### 1.3 Update Indexes Changeset

**File**: `src/main/resources/liquibase/3.3.x.x/003-create-indexes.xml`

**Action**: Update index definitions:

- Change index on `sample_storage_assignment(sample_id)` →
  `sample_storage_assignment(sample_item_id)`
- Change index on `sample_storage_movement(sample_id)` →
  `sample_storage_movement(sample_item_id)`
- Update index names to reflect `sample_item_id` (e.g.,
  `idx_assignment_sample_item`)

### 1.4 Remove/Consolidate Development Artifacts

**Action**: Review changesets 004-009:

- If any changeset modifies assignment/movement tables (e.g., adding/removing
  columns), consolidate those changes into 002
- If any changeset is purely for development/testing, mark for removal or move
  to separate test data changeset
- Ensure final state matches data-model.md exactly

## Stage 2: Entity Code Updates ✅ COMPLETE

**Status**: ✅ **COMPLETE** - Both entities updated to reference `SampleItem`.
No Hibernate XML mappings found (using JPA annotations only).

### 2.1 Update SampleStorageAssignment Entity ✅

**File**:
`src/main/java/org/openelisglobal/storage/valueholder/SampleStorageAssignment.java`

**Changes**:

- Remove `@ManyToOne Sample sample` field
- Add `@ManyToOne SampleItem sampleItem` field with
  `@JoinColumn(name = "SAMPLE_ITEM_ID")`
- Update getter/setter methods: `getSample()` → `getSampleItem()`, `setSample()`
  → `setSampleItem()`
- Update import: `org.openelisglobal.sample.valueholder.Sample` →
  `org.openelisglobal.sampleitem.valueholder.SampleItem`
- Update JavaDoc comment to reference SampleItem

### 2.2 Update SampleStorageMovement Entity

**File**:
`src/main/java/org/openelisglobal/storage/valueholder/SampleStorageMovement.java`

**Changes**:

- Remove `@ManyToOne Sample sample` field
- Add `@ManyToOne SampleItem sampleItem` field with
  `@JoinColumn(name = "SAMPLE_ITEM_ID")`
- Update getter/setter methods
- Update import statement
- Update JavaDoc comment

### 2.3 Update Hibernate Mappings (if XML-based)

**Files**: Check if XML mappings exist:

- `src/main/resources/hibernate/hbm/SampleStorageAssignment.hbm.xml`
- `src/main/resources/hibernate/hbm/SampleStorageMovement.hbm.xml`

**Action**: If they exist:

- Change `many-to-one` relationship from `Sample` to `SampleItem`
- Update column name: `SAMPLE_ID` → `SAMPLE_ITEM_ID`
- Update foreign key reference: `sample(id)` → `sample_item(id)`

## Stage 3: DAO Layer Updates ✅ COMPLETE

**Status**: ✅ **COMPLETE** - Both DAOs updated with `findBySampleItemId()`
methods. Type conversion issue resolved (String→Integer parsing for HQL
parameters).

### 3.1 Update SampleStorageAssignmentDAO ✅

**File**:
`src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAO.java`
(interface) **File**:
`src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAOImpl.java`
(implementation)

**Changes**:

- Rename method: `findBySampleId(String sampleId)` →
  `findBySampleItemId(String sampleItemId)`
- Update HQL queries: Change `sample.id` → `sampleItem.id` in WHERE clauses
- Update JOIN clauses: Change `JOIN assignment.sample` →
  `JOIN assignment.sampleItem`
- Update any other methods that reference `sample` to use `sampleItem`

### 3.2 Update SampleStorageMovementDAO

**File**:
`src/main/java/org/openelisglobal/storage/dao/SampleStorageMovementDAO.java`
(interface) **File**:
`src/main/java/org/openelisglobal/storage/dao/SampleStorageMovementDAOImpl.java`
(implementation)

**Changes**:

- Rename method: `findBySampleId(String sampleId)` →
  `findBySampleItemId(String sampleItemId)`
- Update HQL queries similarly to assignment DAO

## Stage 4: Service Layer Updates ⚠️ MOSTLY COMPLETE

**Status**: ⚠️ **MOSTLY COMPLETE** - `SampleStorageService` and
`SampleStorageServiceImpl` updated. `StorageSearchService` still needs update
(pending).

### 4.1 Update SampleStorageService Interface ✅

**File**:
`src/main/java/org/openelisglobal/storage/service/SampleStorageService.java`

**Changes**:

- Update method signatures:
- `assignSampleWithLocation(String sampleId, ...)` →
  `assignSampleItemWithLocation(String sampleItemId, ...)`
- `moveSampleWithLocation(String sampleId, ...)` →
  `moveSampleItemWithLocation(String sampleItemId, ...)`
- `getSampleAssignment(String sampleId)` →
  `getSampleItemAssignment(String sampleItemId)`
- Update JavaDoc to reference SampleItem

### 4.2 Update SampleStorageServiceImpl

**File**:
`src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java`

**Changes**:

- Update all method implementations to use `sampleItemId` parameter
- Change entity loading: `sampleDAO.get(sampleId)` →
  `sampleItemDAO.get(sampleItemId)`
- Update assignment creation: `assignment.setSample(sample)` →
  `assignment.setSampleItem(sampleItem)`
- Update movement creation: `movement.setSample(sample)` →
  `movement.setSampleItem(sampleItem)`
- Update all queries that join on Sample to join on SampleItem instead
- Update error messages to reference "SampleItem" instead of "Sample"

### 4.3 Update StorageSearchService (if exists) ⚠️ PENDING

**File**:
`src/main/java/org/openelisglobal/storage/service/StorageSearchService.java` ✅
EXISTS

**Status**: ⚠️ **PENDING** - Service exists but still references `sampleId` in
JavaDoc. Needs update to search by SampleItem ID/External ID OR parent Sample
accession number.

**Changes**:

- Update search methods to search by SampleItem ID/External ID OR parent Sample
  accession number
- Update query logic to join SampleItem and parent Sample for search
- Update JavaDoc comments to reference SampleItem

## Stage 5: Controller Layer Updates ✅ COMPLETE

**Status**: ✅ **COMPLETE** - Controller endpoints updated to
`/rest/storage/sample-items/`. Form objects updated to use `sampleItemId`.

### 5.1 Update SampleStorageRestController ✅

**File**:
`src/main/java/org/openelisglobal/storage/controller/SampleStorageRestController.java`

**Changes**:

- Update endpoint parameters: `@PathVariable String sampleId` →
  `@PathVariable String sampleItemId`
- Update endpoint paths: `/rest/storage/samples/{sampleId}/assign` →
  `/rest/storage/sample-items/{sampleItemId}/assign`
- Update method calls to service layer
- Update request/response DTOs to use `sampleItemId`
- Update JavaDoc

### 5.2 Update Form Objects

**File**:
`src/main/java/org/openelisglobal/storage/form/SampleAssignmentForm.java`
**File**:
`src/main/java/org/openelisglobal/storage/form/SampleMovementForm.java`

**Changes**:

- Change field: `private String sampleId;` → `private String sampleItemId;`
- Update getter/setter methods
- Update validation annotations if any reference `sampleId`

## Stage 6: Frontend Updates ⚠️ PARTIAL

**Status**: ⚠️ **PARTIAL** - Core hooks and dashboard updated.
LocationManagementModal and integration points still pending.

### 6.1 Update API Service Hooks ✅

**File**: `frontend/src/components/storage/hooks/useSampleStorage.js`

**Changes**:

- Update API endpoint URLs: `/rest/storage/samples/` →
  `/rest/storage/sample-items/`
- Update parameter names: `sampleId` → `sampleItemId`
- Update response handling to expect SampleItem data structure

### 6.2 Update Dashboard Component

**File**: `frontend/src/components/storage/StorageDashboard.jsx`

**Changes**:

- Update table columns to display SampleItem ID/External ID as primary
  identifier
- Add parent Sample accession number as secondary context column
- Update sorting to support sorting by SampleItem ID or parent Sample accession
- Update search logic to search by SampleItem ID/External ID OR Sample accession
  number

### 6.3 Update Location Management Modal ⚠️ PENDING

**File**:
`frontend/src/components/storage/SampleStorage/LocationManagementModal.jsx`

**Status**: ⚠️ **PENDING** - Still uses `sampleId` in props and comments. Needs
update to accept `sampleItemId` and display SampleItem information.

**Changes**:

- Update to accept `sampleItemId` prop instead of `sampleId`
- Update sample information section to display:
- SampleItem ID/External ID (primary)
- Parent Sample accession number (secondary)
- SampleItem Type, Status, Date Collected, Patient ID, Test Orders
- Update API calls to use `sampleItemId`

### 6.4 Update Integration Points ⚠️ PENDING

**Files**:

- `frontend/src/components/sample/SamplePatientEntry.jsx`
- `frontend/src/components/logbook/LogbookResults.jsx`

**Status**: ⚠️ **PENDING** - Integration points not yet updated for SampleItem
context.

**Changes**:

- Update Storage Location Selector widget integration to work with SampleItem
  context
- When Sample has multiple SampleItems, allow user to select which SampleItem to
  assign
- Update save logic to pass `sampleItemId` to assignment API

## Stage 7: Tasks.md Updates

### 7.1 Update Phase 4 Tasks

**File**: `specs/001-sample-storage/tasks.md`

**Changes**:

- T026t: Update changeset description to use `sample_item_id` instead of
  `sample_id`
- T026v: Update entity update description to reference `SampleItem` instead of
  `Sample`
- T026w: Update form object description to use `sampleItemId`
- T026x-T026z: Update service method descriptions to use `sampleItemId`

### 7.2 Update Phase 5 Tasks

**Changes**:

- T042-T044: Update test descriptions to use "SampleItem" terminology
- T045: Change `findBySampleId()` → `findBySampleItemId()` in description
- T047-T049: Update method signatures and descriptions to use `sampleItemId`
- T050-T052: Update controller endpoint descriptions to use `sampleItemId`
- Add new task: "Update dashboard to display SampleItem ID/External ID as
  primary identifier with parent Sample accession as secondary context"
- Add new task: "Update search to support both SampleItem ID/External ID and
  parent Sample accession number"

### 7.3 Update Phase 6 Tasks

**Changes**:

- Ensure all tasks explicitly reference SampleItem ID/External ID and parent
  Sample accession number
- Update search endpoint descriptions to use `sampleItemId`

### 7.4 Update Phase 7 Tasks

**Changes**:

- Update movement tasks to use `sampleItemId` terminology
- Ensure bulk move tasks reference SampleItem-level operations

### 7.5 Add Missing Tasks

**Add to Phase 5**:

- Task: "Update FHIR Specimen.container extension to map SampleItem storage
  location (per research.md Section 11, Q5)"
- Task: "Add SampleItem selection UI when Sample has multiple SampleItems (per
  FR-033b)"

## Stage 8: Test Updates ⚠️ PARTIAL

**Status**: ⚠️ **PARTIAL** - All backend unit and integration tests updated and
passing (33/33 tests). E2E tests still pending.

### 8.1 Update Unit Tests ✅

**Files**: All test files in `src/test/java/org/openelisglobal/storage/`

**Changes**:

- Update test methods to use `sampleItemId` instead of `sampleId`
- Update mock data to use `SampleItem` entities instead of `Sample`
- Update assertions to verify `SampleItem` relationships
- Update test method names: `testAssignSample_*` → `testAssignSampleItem_*`

### 8.2 Update Integration Tests

**Changes**:

- Update API endpoint URLs in tests
- Update request/response DTOs to use `sampleItemId`
- Update database setup/teardown to create `SampleItem` test data

### 8.3 Update E2E Tests ⚠️ PENDING

**Files**: `frontend/cypress/e2e/storage*.cy.js` (multiple files exist:
storageDashboard.cy.js, storageLocationCRUD.cy.js, etc.)

**Status**: ⚠️ **PENDING** - E2E tests need update to work with SampleItem
context.

**Changes**:

- Update test scenarios to work with SampleItem context
- Update element selectors if they reference Sample vs SampleItem
- Update assertions to verify SampleItem ID display

## Stage 9: Documentation Updates ⚠️ PENDING

**Status**: ⚠️ **PENDING** - API contracts and FHIR mappings documentation need
updates.

### 9.1 Update API Contracts ⚠️ PENDING

**File**: `specs/001-sample-storage/contracts/storage-api.json`

**Status**: ⚠️ **PENDING** - API contract still uses `/samples/` endpoints.
Needs update to `/sample-items/`.

**Changes**:

- Update endpoint paths: `/samples/` → `/sample-items/`
- Update parameter names: `sampleId` → `sampleItemId`
- Update request/response schemas to reference SampleItem
- Update examples to use SampleItem IDs

### 9.2 Update FHIR Mappings Documentation

**File**: `specs/001-sample-storage/contracts/fhir-mappings.md`

**Changes**:

- Ensure documentation clearly states SampleItem storage maps to
  Specimen.container extension
- Update examples to show SampleItem → Specimen mapping

## Stage 10: Verification Checklist ⚠️ PARTIAL

**Status**: ⚠️ **PARTIAL** - Backend code and tests verified. Frontend and
documentation verification pending.

### 10.1 Database Schema Verification ✅

- [x] Run Liquibase changesets and verify tables created with `sample_item_id`
      ✅
- [x] Verify foreign key constraint: `sample_item_id` → `sample_item(id)` ✅
- [x] Verify unique constraint: `UNIQUE (sample_item_id)` ✅
- [x] Verify indexes created on `sample_item_id` columns ✅

### 10.2 Code Verification ⚠️ PARTIAL

- [x] All entity classes reference `SampleItem` not `Sample` ✅
- [x] All DAO methods use `sampleItemId` parameter ✅
- [x] All service methods use `sampleItemId` parameter ✅ (except
      StorageSearchService)
- [x] All controller endpoints use `sampleItemId` path variable ✅
- [x] All form objects use `sampleItemId` field ✅
- [ ] LocationManagementModal uses `sampleItemId` ⚠️ PENDING
- [ ] Integration points (SamplePatientEntry, LogbookResults) updated ⚠️ PENDING

### 10.3 Test Verification ⚠️ PARTIAL

- [x] All unit tests pass with SampleItem changes ✅ (33/33 passing)
- [x] All integration tests pass with updated endpoints ✅
- [ ] All E2E tests pass with SampleItem context ⚠️ PENDING

### 10.4 Documentation Verification ⚠️ PENDING

- [ ] tasks.md updated with correct terminology ⚠️ PENDING
- [ ] API contracts updated ⚠️ PENDING
- [x] All code comments reference SampleItem ✅ (backend complete)

## Implementation Order

1. **Stage 1** (Liquibase Consolidation) - Must be first to establish correct
   schema
2. **Stage 2** (Entity Code) - Foundation for all other changes
3. **Stage 3** (DAO Layer) - Required by service layer
4. **Stage 4** (Service Layer) - Required by controller layer
5. **Stage 5** (Controller Layer) - Required by frontend
6. **Stage 6** (Frontend) - Can be done in parallel with Stage 7
7. **Stage 7** (Tasks.md) - Documentation update
8. **Stage 8** (Tests) - Update after code changes
9. **Stage 9** (Documentation) - Final documentation polish
10. **Stage 10** (Verification) - Final validation

## Risk Mitigation

- **Database Migration Risk**: The change from `sample_id` to `sample_item_id`
  is a breaking change. If production data exists, create a data migration
  script to map existing `sample_id` values to appropriate `sample_item_id`
  values (likely one-to-one if each Sample has one SampleItem initially).

- **Backward Compatibility**: This is a breaking API change. All existing API
  clients must be updated. Consider versioning API endpoints if backward
  compatibility is required.

- **Test Data**: Ensure test data setup creates both Sample and SampleItem
  entities with proper relationships.

### To-dos

- [x] Audit all Liquibase changesets in 3.3.x.x/ directory to identify what
      needs consolidation ✅
- [x] Replace 002-create-assignment-tables.xml with final state using
      sample_item_id instead of sample_id ✅
- [x] Update 003-create-indexes.xml to reference sample_item_id columns ✅
- [x] Update SampleStorageAssignment.java to reference SampleItem instead of
      Sample ✅
- [x] Update SampleStorageMovement.java to reference SampleItem instead of
      Sample ✅
- [x] Update Hibernate XML mappings (if they exist) to reference SampleItem ✅
      (No XML mappings found)
- [x] Update SampleStorageAssignmentDAO to use findBySampleItemId() and update
      all queries ✅
- [x] Update SampleStorageMovementDAO to use findBySampleItemId() and update all
      queries ✅
- [x] Update SampleStorageService interface method signatures to use
      sampleItemId ✅
- [x] Update SampleStorageServiceImpl to use sampleItemId and SampleItem
      entities ✅
- [x] Update SampleStorageRestController endpoints to use sampleItemId path
      variables ✅
- [x] Update SampleAssignmentForm and SampleMovementForm to use sampleItemId
      field ✅
- [x] Update useSampleStorage.js hook to use sample-item endpoints and
      sampleItemId parameters ✅
- [x] Update StorageDashboard to display SampleItem ID/External ID as primary
      with parent Sample as secondary ✅
- [x] Update all unit tests to use sampleItemId and SampleItem entities ✅
- [x] Update all integration tests to use updated endpoints and sampleItemId ✅
- [x] Fix type conversion issues in DAO queries ✅
- [x] Fix response format issues in controller ✅
- [x] Update test fixtures to create SampleItem test data ✅
- [x] Fix empty test files ✅
- [x] All backend tests passing (33/33) ✅
- [ ] Review and consolidate/remove any development artifact changesets
      (004-009) that modify assignment tables (Low priority)
- [ ] Update StorageSearchService to search by SampleItem ID/External ID OR
      parent Sample accession
- [ ] Update LocationManagementModal to accept sampleItemId and display
      SampleItem information
- [ ] Update SamplePatientEntry and LogbookResults to work with SampleItem
      context
- [ ] Update Phase 4 tasks in tasks.md to use sampleItemId terminology
- [ ] Update Phase 5 tasks in tasks.md to use SampleItem terminology and add
      missing tasks
- [ ] Update Phase 6 and 7 tasks in tasks.md to explicitly reference SampleItem
      operations
- [ ] Update E2E tests to work with SampleItem context
- [ ] Update storage-api.json to use sample-item endpoints and sampleItemId
      parameters
- [ ] Update fhir-mappings.md to clearly document SampleItem →
      Specimen.container mapping
