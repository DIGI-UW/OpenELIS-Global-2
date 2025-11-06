# Implementation Plan: Sample Storage Management

**Branch**: `001-sample-storage` | **Date**: 2025-10-30 | **Spec**:
[spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-sample-storage/spec.md`

## Summary

Implement POC for Sample Storage Management to track physical location of
biological samples through a flexible storage hierarchy (Room → Device → Shelf →
Rack → Position). Positions can have 2-5 levels (minimum: room+device, maximum:
room+device+shelf+rack+position). POC scope includes core tracking workflows:
assignment (P1), search/retrieval (P2A), movement (P2B), and basic Storage
Dashboard (P4 - metrics cards, tabs, data tables). Defers disposal workflow (P3)
and advanced dashboard features (drill-down navigation, CSV export) to post-POC
iterations.

**Technical Approach**: Leverage existing OpenELIS infrastructure (5-layer
backend architecture, HAPI FHIR R4 server, Carbon Design System UI) to add
storage location tracking. Create reusable Storage Location Selector widget with
two-tier design (compact inline view + expanded modal) supporting cascading
dropdowns, type-ahead autocomplete, and quick-find search. Widget used in both
orders (SamplePatientEntry) and results (LogbookResults) workflows. Implement
samples table overflow menu with Move, Dispose, View Audit (placeholder), and
View Storage actions. Create three modals (Move, Dispose, View Storage) matching
Figma designs with detailed UI specifications. Map storage entities to FHIR
Location resources for external interoperability.

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**:

- Backend: Spring Boot 3.x, Hibernate 6.x, HAPI FHIR R4 (v6.6.2), JPA
- Frontend: @carbon/react v1.15.0, React Intl 5.20.12, Formik 2.2.9,
  getFromOpenElisServer/postToOpenElisServer utilities

**Storage**: PostgreSQL 14+ (existing OpenELIS database)  
**Testing**:

- Backend: JUnit 5 + Mockito (unit/integration)
- Frontend: Jest + React Testing Library (unit), Cypress 12.17.3 (E2E - existing
  OpenELIS framework)
- FHIR: Resource validation against R4 profiles

**Target Platform**: Web application (Linux server deployment, browser-based
UI)  
**Project Type**: Web (backend + frontend integration)  
**Performance Goals**: Reasonable response times for POC (few seconds for
searches/saves), no optimization required  
**Constraints**:

- POC scope only (P1, P2A, P2B user stories)
- > 70% test coverage per OpenELIS constitution
- FHIR R4 integration mandatory for storage entities

**Scale/Scope**:

- 5 storage entity types (Room, Device, Shelf, Rack, Position)
- 3 REST API endpoint groups (hierarchy CRUD, assignment, movement)
- 1 reusable UI widget (Storage Location Selector with two-tier design)
- 3 modal components (Move, Dispose, View Storage)
- 1 overflow menu component (samples table row actions)
- 2 integration points (SamplePatientEntry, LogbookResults)

**Development Approach**: Test-Driven Development (TDD)

- Write tests BEFORE implementation code
- Order: API contracts → FHIR validation tests → Integration tests → Unit tests
  → Implementation → E2E tests
- All tests must pass before moving to next component
- Target >70% coverage per OpenELIS constitution

## Constitution Check

_GATE: Must pass before Phase 0 research. Re-check after Phase 1 design._

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: Position naming free-text (no validation),
      capacity thresholds configurable
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (Tabs,
      DataTable, Modal, TextInput, Dropdown, OverflowMenu)
- [x] **FHIR/IHE Compliance**: All hierarchy levels (Room, Device, Shelf, Rack,
      Position) map to FHIR Location resources, sample links via
      Specimen.container. Positions can have 2-5 levels (minimum: room+device,
      maximum: room+device+shelf+rack+position).
- [x] **Layered Architecture**: Backend follows 5-layer pattern (StorageRoom
      valueholder → DAO → Service → Controller → Form)
- [x] **Test Coverage**: Unit + integration + Cypress E2E tests planned (>70%
      coverage goal per spec)
- [x] **Schema Management**: Liquibase changesets for 5 entity tables + junction
      tables, all with fhir_uuid columns
- [x] **Internationalization**: All UI strings use React Intl message keys (en,
      fr, sw minimum)
- [x] **Security & Compliance**: RBAC (Technicians/Managers/Admins), audit trail
      (sys_user_id, lastupdated), input validation

**Complexity Justification**: None required - plan fully compliant with
constitution.

## Project Structure

### Documentation (this feature)

```text
specs/001-sample-storage/
├── spec.md              # Feature specification (completed)
├── plan.md              # This file (/speckit.plan output)
├── research.md          # Phase 0 output (technology validation)
├── data-model.md        # Phase 1 output (entity schemas)
├── quickstart.md        # Phase 1 output (dev setup)
├── contracts/           # Phase 1 output (API specifications)
│   ├── storage-api.json # OpenAPI 3.0 spec for REST endpoints
│   └── fhir-mappings.md # FHIR Location resource mappings
└── tasks.md             # Phase 2 output (/speckit.tasks - deferred)
```

### Source Code (repository root)

```text
# Backend (Java) - OpenELIS Global existing structure
src/main/java/org/openelisglobal/storage/
├── valueholder/
│   ├── StorageRoom.java
│   ├── StorageDevice.java
│   ├── StorageShelf.java
│   ├── StorageRack.java
│   ├── StoragePosition.java
│   ├── SampleStorageAssignment.java
│   └── SampleStorageMovement.java
├── dao/
│   ├── StorageRoomDAO.java (+ impl)
│   ├── StorageDeviceDAO.java (+ impl)
│   ├── StorageShelfDAO.java (+ impl)
│   ├── StorageRackDAO.java (+ impl)
│   ├── StoragePositionDAO.java (+ impl)
│   ├── SampleStorageAssignmentDAO.java (+ impl)
│   └── SampleStorageMovementDAO.java (+ impl)
├── service/
│   ├── StorageLocationService.java (+ impl) - CRUD for hierarchy
│   ├── SampleStorageService.java (+ impl) - Assignment/movement logic
│   └── StorageSearchService.java (+ impl) - Search/filter operations
├── controller/
│   ├── StorageLocationRestController.java
│   ├── SampleStorageRestController.java
│   └── StorageSearchRestController.java
├── form/
│   ├── StorageLocationForm.java
│   ├── SampleAssignmentForm.java
│   └── SampleMovementForm.java
└── fhir/
    └── StorageLocationFhirTransform.java - FHIR Location mapping

src/main/resources/
├── liquibase/storage/
│   ├── 001-create-storage-tables.xml
│   ├── 002-create-assignment-tables.xml
│   └── 003-add-fhir-uuid-columns.xml
└── hibernate/hbm/
    ├── StorageRoom.hbm.xml
    ├── StorageDevice.hbm.xml
    ├── StorageShelf.hbm.xml
    ├── StorageRack.hbm.xml
    ├── StoragePosition.hbm.xml
    ├── SampleStorageAssignment.hbm.xml
    └── SampleStorageMovement.hbm.xml

src/test/java/org/openelisglobal/storage/
├── service/ - Service layer unit tests
├── controller/ - REST endpoint integration tests
└── fhir/ - FHIR transformation tests

# Frontend (React) - OpenELIS Global existing structure
frontend/src/components/storage/
├── StorageLocationSelector/
│   ├── StorageLocationSelector.jsx - Main reusable widget (two-tier: compact + modal)
│   ├── CompactLocationView.jsx - Compact inline view showing location path + expand button
│   ├── LocationSelectorModal.jsx - Expanded modal view with full assignment form
│   ├── QuickFindSearch.jsx - Quick-find search component (type-ahead autocomplete)
│   ├── CascadingDropdownMode.jsx - Cascading dropdowns for expanded modal
│   ├── AutocompleteMode.jsx - Type-ahead autocomplete for expanded modal
│   ├── BarcodeScanMode.jsx - Barcode scan input (deferred to later stage)
│   ├── StorageLocationSelector.test.jsx
│   └── index.js
├── StorageDashboard/
│   ├── StorageDashboard.jsx - Main dashboard component
│   ├── StorageLocationsMetricCard.jsx - Color-coded metric card showing location breakdown by type
│   ├── LocationFilterDropdown.jsx - Single location dropdown with autocomplete and tree view
│   ├── LocationTreeView.jsx - Hierarchical tree view component (expand/collapse)
│   ├── LocationAutocomplete.jsx - Autocomplete search component (flat list with full paths)
│   ├── LocationFilterDropdown.test.jsx
│   ├── StorageLocationsMetricCard.test.jsx
│   └── index.js
├── SampleStorage/
│   ├── MoveSampleModal.jsx - Move sample modal with current/new location selectors
│   ├── DisposeSampleModal.jsx - Dispose sample modal with reason/method/confirmation
│   ├── ViewStorageModal.jsx - View/edit storage location assignment modal
│   ├── SampleActionsOverflowMenu.jsx - Overflow menu component for samples table rows
│   ├── BulkMoveModal.jsx
│   └── index.js
└── hooks/
    ├── useStorageLocations.js - getFromOpenElisServer data fetching
    ├── useSampleStorage.js
    └── index.js

frontend/src/languages/
├── en.json - Add storage.* message keys
├── fr.json
└── sw.json

# Integration Points (modify existing files)
frontend/src/components/sample/SamplePatientEntry.jsx
frontend/src/components/logbook/LogbookResults.jsx

# E2E Tests (Cypress)
frontend/cypress/e2e/
├── storageAssignment.cy.js
├── storageSearch.cy.js
└── storageMovement.cy.js
```

**Structure Decision**: Follows existing OpenELIS monolithic repository
structure with clear module separation. Backend uses standard 5-layer pattern in
`org.openelisglobal.storage.*` package. Frontend components in
`frontend/src/components/storage/` with reusable widget design. Integration
points modify existing sample entry/search components to embed Storage Location
Selector widget.

## Test-Driven Development Workflow

**CRITICAL**: This POC follows **strict test-first development**. Tests are
written BEFORE implementation code.

### Development Order (Enforced)

**Phase 1: Contracts & Test Specifications**

1. ✅ API contracts (OpenAPI spec) - Define expected behavior
2. ✅ FHIR mappings documentation - Define FHIR resource structure
3. ✅ Data model documentation - Define entity relationships

**Phase 2: Test Creation (BEFORE any implementation code)**

1. **FHIR Validation Tests** - Write tests for FHIR Location resource
   creation/validation

   - Test: `StorageLocationFhirTransformTest.java`
   - Validates: Room/Device/Shelf/Rack/Position → Location resource structure
   - Validates: IHE mCSD profile compliance
   - Validates: Hierarchical partOf references correct

2. **Backend Unit Tests** - Write tests for service layer business logic
   - Test: `StorageLocationServiceImplTest.java`,
     `SampleStorageServiceImplTest.java`, `StorageSearchServiceImplTest.java`
   - Validates: Assignment validation logic (require Room and Device minimum 2
     levels, prevent inactive location, double-occupancy)
   - Validates: Capacity calculation and warning thresholds (80/90/100%)
   - Validates: Hierarchical path construction
   - Validates: Bulk move auto-assignment logic
   - Validates: Audit trail creation on movements

2.5. **ORM Validation Tests** (Hibernate framework validation - ADDED based on
Phase 3 learnings)

- Test: `HibernateMappingValidationTest.java`
- Validates: All 7 storage entity Hibernate mappings load successfully
- Validates: SessionFactory builds without errors
- Validates: No JavaBean getter/setter conflicts (getActive vs isActive)
- Validates: Property names match between entities and .hbm.xml files
- Execution: <5 seconds, no database required
- **Purpose**: Catches ORM config errors before integration tests (fills gap
  between unit and integration)

3. **Backend Integration Tests** - Write tests for REST endpoints

   - Test: `StorageLocationRestControllerTest.java`,
     `SampleStorageRestControllerTest.java`,
     `StorageSearchRestControllerTest.java`
   - Validates: HTTP request/response contracts match OpenAPI spec
   - Validates: Database persistence after API calls
   - Validates: Error responses (400, 404, 409) for validation failures

4. **Frontend Unit Tests** - Write tests for React components
   - Test: `StorageLocationSelector.test.jsx`, `CompactLocationView.test.jsx`,
     `LocationSelectorModal.test.jsx`, `QuickFindSearch.test.jsx`,
     `CascadingDropdownMode.test.jsx`, etc.
   - Validates: Compact inline view displays location path correctly
   - Validates: Expand button opens modal
   - Validates: Quick-find search filters locations correctly

- Validates: Cascading dropdown state management
- Validates: Barcode input parsing (deferred functionality)
- Validates: Hierarchical path display
- Validates: Validation requires Room and Device selection (minimum 2 levels),
  Shelf/Rack/Position optional
- Validates: API error handling and user feedback
- Test: `SampleActionsOverflowMenu.test.jsx`
- Validates: Menu renders with all four items (Move, Dispose, View Audit
  placeholder, View Storage)
- Validates: "View Audit" is disabled
- Test: `MoveSampleModal.test.jsx`
- Validates: Modal renders with current location, new location selector, preview
  section
- Validates: Location selection updates preview
- Validates: Validation prevents moving to same location
- Test: `DisposeSampleModal.test.jsx`
- Validates: Modal renders with warning alert, sample info, disposal form fields
- Validates: "Confirm Disposal" button disabled until checkbox checked
- Validates: Validation requires reason and method selection
- Test: `ViewStorageModal.test.jsx`
- Validates: Modal renders with sample info, current location, full assignment
  form
- Validates: Form pre-populates with current location
- Validates: Validation requires Room and Device selection (minimum 2 levels)

**Phase 3: Implementation (Make tests pass)**

1. **Backend Implementation** - Write code to pass tests

   - Liquibase changesets (schema)
   - Valueholder entities
   - DAO implementations
   - Service implementations
   - Controller implementations
   - FHIR transform service

2. **Frontend Implementation** - Write code to pass tests
   - Storage Location Selector widget
   - Integration into SamplePatientEntry, LogbookResults
   - Data fetching hooks

### Phase 3.1: Tab-Specific Search Functionality Implementation (FR-064(a))

### Objective

Implement tab-specific search functionality per FR-064 and FR-064a:

- **Samples tab**: Debounced live search (300-500ms) by sample ID, accession
  number type/prefix, and assigned location (full hierarchical path)
- **Rooms tab**: Search by name and code
- **Devices tab**: Search by name, code, and type
- **Shelves tab**: Search by name (label)
- **Racks tab**: Search by name (label)

All searches use case-insensitive partial/substring matching with OR logic
(matches any of the specified fields).

### TDD Approach

Following strict test-first development:

1. **Write failing tests** (Red phase)
2. **Implement minimal code to pass** (Green phase)
3. **Refactor while keeping tests green** (Refactor phase)

### Test Specifications

#### Backend Integration Tests (Write First)

**File**:
`src/test/java/org/openelisglobal/storage/controller/StorageSearchRestControllerTest.java`

**Test Cases**:

1. **Samples Search Tests**:

   - `testSearchSamples_BySampleId_ReturnsMatching()` - Search by exact sample
     ID
   - `testSearchSamples_ByAccessionPrefix_ReturnsMatching()` - Search by
     accession prefix (e.g., "S-2025" matches "S-2025-001")
   - `testSearchSamples_ByLocationPath_ReturnsMatching()` - Search by location
     path substring (e.g., "Freezer" matches "Main Laboratory > Freezer Unit 1 >
     ...")
   - `testSearchSamples_CombinedFields_OR_Logic()` - Search matches ANY of the
     three fields (OR logic)
   - `testSearchSamples_CaseInsensitive()` - "freezer" matches "Freezer Unit 1"
   - `testSearchSamples_PartialMatch()` - "S-202" matches "S-2025-001"
   - `testSearchSamples_EmptyQuery_ReturnsAll()` - Empty search returns all
     samples
   - `testSearchSamples_NoMatches_ReturnsEmpty()` - No matches returns empty
     array

2. **Rooms Search Tests**:

   - `testSearchRooms_ByName_ReturnsMatching()` - Search by name
     (case-insensitive partial)
   - `testSearchRooms_ByCode_ReturnsMatching()` - Search by code
     (case-insensitive partial)
   - `testSearchRooms_CombinedFields_OR_Logic()` - Matches name OR code

3. **Devices Search Tests**:

   - `testSearchDevices_ByName_ReturnsMatching()` - Search by name
   - `testSearchDevices_ByCode_ReturnsMatching()` - Search by code
   - `testSearchDevices_ByType_ReturnsMatching()` - Search by type (freezer,
     refrigerator, etc.)
   - `testSearchDevices_CombinedFields_OR_Logic()` - Matches name OR code OR
     type

4. **Shelves Search Tests**:

   - `testSearchShelves_ByLabel_ReturnsMatching()` - Search by label
     (case-insensitive partial)

5. **Racks Search Tests**:
   - `testSearchRacks_ByLabel_ReturnsMatching()` - Search by label
     (case-insensitive partial)

**Test Data Setup**:

- Use JDBC to insert test data (rooms, devices, shelves, racks, samples,
  assignments)
- Ensure test data covers:
  - Multiple samples with different accession prefixes
  - Samples in different locations (different hierarchical paths)
  - Entities with various names/codes/types for testing partial matches

**API Contract**:

- `GET /rest/storage/samples/search?q={searchTerm}` - Search samples
- `GET /rest/storage/rooms/search?q={searchTerm}` - Search rooms
- `GET /rest/storage/devices/search?q={searchTerm}` - Search devices
- `GET /rest/storage/shelves/search?q={searchTerm}` - Search shelves
- `GET /rest/storage/racks/search?q={searchTerm}` - Search racks

**Expected Response Format**:

- All endpoints return `List<Map<String, Object>>` matching existing API format
- Samples: Include sample ID, type, status, location (full path), assigned by,
  date
- Rooms: Include id, name, code, description, active
- Devices: Include id, name, code, type, roomId, roomName, active
- Shelves: Include id, label, deviceId, deviceName, roomId, roomName, active
- Racks: Include id, label, shelfId, shelfLabel, deviceId, deviceName, roomId,
  roomName, active

#### Backend Service Unit Tests (Write First)

**File**:
`src/test/java/org/openelisglobal/storage/service/StorageSearchServiceImplTest.java`

**Test Cases**:

1. **Sample Search Service Tests**:

   - `testSearchSamples_FiltersBySampleId()` - Filter samples by ID substring
   - `testSearchSamples_FiltersByAccessionPrefix()` - Filter by accession prefix
   - `testSearchSamples_FiltersByLocationPath()` - Filter by location path
     substring
   - `testSearchSamples_OR_Logic()` - Matches if ANY field matches
   - `testSearchSamples_CaseInsensitive()` - Case-insensitive matching
   - `testSearchSamples_EmptyQuery_ReturnsAll()` - Empty query returns all
   - `testSearchSamples_NullQuery_ReturnsAll()` - Null query returns all

2. **Room Search Service Tests**:

   - `testSearchRooms_FiltersByNameOrCode()` - Matches name OR code

3. **Device Search Service Tests**:

   - `testSearchDevices_FiltersByNameCodeOrType()` - Matches name OR code OR
     type

4. **Shelf Search Service Tests**:

   - `testSearchShelves_FiltersByLabel()` - Matches label

5. **Rack Search Service Tests**:
   - `testSearchRacks_FiltersByLabel()` - Matches label

**Mock Strategy**:

- Mock DAO calls to return test data
- Verify service calls DAO methods with correct filters
- Test search logic in isolation

#### Frontend Unit Tests (Write First)

**File**:
`frontend/src/components/storage/__tests__/StorageDashboardSearch.test.jsx`

**Test Cases**:

1. **Search Input Component Tests**:

   - `testSearchInput_RendersCorrectly()` - Renders search input with
     placeholder
   - `testSearchInput_UpdatesOnChange()` - Updates state on input change
   - `testSearchInput_DebouncedForSamples()` - Debounces input for samples tab
     (300-500ms)
   - `testSearchInput_ImmediateForOtherTabs()` - Immediate or submit-button for
     other tabs

2. **Search Results Tests**:

   - `testSearchResults_FiltersSamples()` - Filters samples by search term
   - `testSearchResults_FiltersRooms()` - Filters rooms by search term
   - `testSearchResults_FiltersDevices()` - Filters devices by search term
   - `testSearchResults_FiltersShelves()` - Filters shelves by search term
   - `testSearchResults_FiltersRacks()` - Filters racks by search term
   - `testSearchResults_CaseInsensitive()` - Case-insensitive matching
   - `testSearchResults_PartialMatch()` - Partial substring matching
   - `testSearchResults_EmptySearch_ShowsAll()` - Empty search shows all items

3. **Tab-Specific Search Tests**:
   - `testSamplesTab_SearchesByIdLocationPrefix()` - Samples tab searches by ID,
     accession prefix, location
   - `testRoomsTab_SearchesByNameCode()` - Rooms tab searches by name and code
   - `testDevicesTab_SearchesByNameCodeType()` - Devices tab searches by name,
     code, type
   - `testShelvesTab_SearchesByLabel()` - Shelves tab searches by label
   - `testRacksTab_SearchesByLabel()` - Racks tab searches by label

**Mock Strategy**:

- Mock API calls (`getFromOpenElisServer`)
- Use React Testing Library for component rendering
- Test user interactions (typing, debouncing)

#### Frontend E2E Tests (Write First)

**File**: `frontend/cypress/e2e/storageSearch.cy.js` (update existing)

**Test Cases**:

1. **Samples Tab Search E2E**:

   - `testSamplesSearch_BySampleId()` - Search by sample ID, verify results
   - `testSamplesSearch_ByAccessionPrefix()` - Search by accession prefix,
     verify results
   - `testSamplesSearch_ByLocationPath()` - Search by location path, verify
     results
   - `testSamplesSearch_Debounced()` - Verify debounced search (300-500ms delay)
   - `testSamplesSearch_CaseInsensitive()` - Verify case-insensitive matching
   - `testSamplesSearch_PartialMatch()` - Verify partial substring matching

2. **Rooms Tab Search E2E**:

   - `testRoomsSearch_ByName()` - Search rooms by name
   - `testRoomsSearch_ByCode()` - Search rooms by code

3. **Devices Tab Search E2E**:

   - `testDevicesSearch_ByName()` - Search devices by name
   - `testDevicesSearch_ByCode()` - Search devices by code
   - `testDevicesSearch_ByType()` - Search devices by type

4. **Shelves Tab Search E2E**:

   - `testShelvesSearch_ByLabel()` - Search shelves by label

5. **Racks Tab Search E2E**:
   - `testRacksSearch_ByLabel()` - Search racks by label

**Test Strategy**:

- Load test fixtures with known data
- Intercept API calls to verify search parameters
- Verify UI updates with filtered results
- Verify search persists across tab switches

### Implementation Tasks (After Tests Pass)

#### Backend Implementation

1. **Create Search Service Interface** (`StorageSearchService.java`):

   ```java
   List<Map<String, Object>> searchSamples(String query);
   List<StorageRoom> searchRooms(String query);
   List<StorageDevice> searchDevices(String query);
   List<StorageShelf> searchShelves(String query);
   List<StorageRack> searchRacks(String query);
   ```

2. **Implement Search Service** (`StorageSearchServiceImpl.java`):

   - Sample search: Query by sample ID, accession prefix, location path (OR
     logic)
   - Room search: Query by name OR code (case-insensitive LIKE)
   - Device search: Query by name OR code OR type (case-insensitive LIKE)
   - Shelf search: Query by label (case-insensitive LIKE)
   - Rack search: Query by label (case-insensitive LIKE)
   - All searches use case-insensitive substring matching

3. **Update REST Controllers**:

   - Add `GET /rest/storage/samples/search?q={term}` endpoint
   - Add `GET /rest/storage/rooms/search?q={term}` endpoint
   - Add `GET /rest/storage/devices/search?q={term}` endpoint
   - Add `GET /rest/storage/shelves/search?q={term}` endpoint
   - Add `GET /rest/storage/racks/search?q={term}` endpoint
   - All endpoints return JSON arrays matching existing API format

4. **Query Optimization**:
   - Use database indexes on searchable columns (name, code, type, label)
   - For samples location search: Use full-text search on hierarchical path or
     JOIN through hierarchy
   - Consider PostgreSQL `ILIKE` for case-insensitive matching

#### Frontend Implementation

1. **Update Search Component** (`StorageDashboard.jsx`):

   - Add search input field (already exists, update logic)
   - Implement debounced search for samples tab (300-500ms delay)
   - Implement search logic for each tab:
     - Samples: Search by ID, accession prefix, location path
     - Rooms: Search by name and code
     - Devices: Search by name, code, and type
     - Shelves: Search by label
     - Racks: Search by label
   - Use case-insensitive partial matching
   - Combine search with existing filters (AND logic)

2. **API Integration**:

   - Call search endpoints when search term is entered
   - For samples: Use debounced API calls (300-500ms delay)
   - For other tabs: Use debounced or submit-button search
   - Update table data with filtered results

3. **Search State Management**:
   - Store search term in component state
   - Clear search when switching tabs
   - Persist search term within tab (optional enhancement)

### Testing Checklist

- [ ] All backend integration tests pass
- [ ] All backend service unit tests pass
- [ ] All frontend unit tests pass
- [ ] All E2E tests pass
- [ ] Search works correctly for samples (ID, accession prefix, location)
- [ ] Search works correctly for rooms (name, code)
- [ ] Search works correctly for devices (name, code, type)
- [ ] Search works correctly for shelves (label)
- [ ] Search works correctly for racks (label)
- [ ] Case-insensitive matching verified
- [ ] Partial/substring matching verified
- [ ] Debounced search (300-500ms) verified for samples tab
- [ ] Empty search shows all items
- [ ] Search combines correctly with filters (AND logic)
- [ ] Performance acceptable (<2 seconds for 100,000+ records)

### Dependencies

- Existing filter functionality (FR-065) - search must work alongside filters
- Existing API endpoints for fetching data
- Existing table rendering components

### Success Criteria

- All tests pass (integration, unit, E2E)
- Search functionality works for all 5 tabs per FR-064
- Debounced search implemented for samples tab (300-500ms)
- Case-insensitive partial matching working
- Search combines with filters (AND logic)
- Performance meets requirements (<2 seconds)

---

**Phase 4: Dashboard Storage Locations Metric Card & E2E Tests**

### Dashboard Storage Locations Metric Card Implementation

**Objective**: Implement color-coded Storage Locations metric card with
breakdown by type and matching tab accents per FR-057 and FR-057a.

**Requirements**:

- Display formatted text list: "X rooms, Y devices, Z shelves, W racks" (active
  locations only)
- Color-code text using Carbon Design System tokens:
  - Rooms: `blue-70` (blue-70)
  - Devices: `teal-70` (teal-70)
  - Shelves: `purple-70` (purple-70)
  - Racks: `orange-70` (orange-70)
- Apply matching subtle accent colors to corresponding tab labels/backgrounds:
  - Rooms tab: subtle blue accent matching "X rooms" text color
  - Devices tab: subtle teal accent matching "Y devices" text color
  - Shelves tab: subtle purple accent matching "Z shelves" text color
  - Racks tab: subtle orange accent matching "W racks" text color
- Tab coloring must be very subtle (light background tint or border accent, not
  overpowering)

**Implementation Tasks**:

1. Update `StorageDashboard.jsx` to calculate active location counts by type
   (Room, Device, Shelf, Rack)
2. Create `StorageLocationsMetricCard.jsx` component displaying formatted
   breakdown with color-coded text
3. Apply Carbon color tokens to metric card text using Carbon `Text` component
   or inline styles
4. Update tab styling to include subtle accent colors matching metric card
   colors
5. Ensure colorblind accessibility (Carbon tokens are WCAG compliant)

**Testing**:

- Verify metric card displays correct counts for active locations only
- Verify color-coding matches Carbon Design System tokens
- Verify tab accent colors are subtle and match metric card colors
- Verify colorblind accessibility (test with colorblind simulation tools)

### E2E Tests - Validate complete workflows

1. **Cypress E2E Tests** - Test user scenarios end-to-end
   - Test: `storageAssignment.cy.js` (P1 user story)
   - Test: `storageSearch.cy.js` (P2A user story)
   - Test: `storageMovement.cy.js` (P2B user story)
   - Test: `storageDashboardFilter.cy.js` (Dashboard Samples tab filtering)
     - Test single location dropdown with autocomplete search
     - Test hierarchical browsing (tree view expand/collapse)
     - Test filtering by Room, Device, Shelf, and Rack levels
     - Verify downward inclusive filtering (selecting device shows all samples
       in child shelves/racks/positions)
     - Test inactive location display (visual distinction)
     - Test combination of location filter and status filter
     - Verify Position-level locations excluded from dropdown (only
       Room/Device/Shelf/Rack)
   - Test: `storageDashboardMetrics.cy.js` (Storage Locations metric card)
     - Verify metric card displays formatted breakdown: "X rooms, Y devices, Z
       shelves, W racks"
     - Verify color-coding matches specification (blue-70, teal-70, purple-70,
       orange-70)
     - Verify only active locations included in counts
     - Verify tab accent colors match metric card colors and are subtle
     - Verify colorblind accessibility

### Test-First Principles

**MANDATORY RULES**:

- ❌ **DO NOT** write implementation code before tests exist
- ❌ **DO NOT** skip test creation "to move faster"
- ✅ **DO** write failing tests first (Red phase)
- ✅ **DO** write minimal implementation to pass tests (Green phase)
- ✅ **DO** refactor after tests pass (Refactor phase)

**Benefits for POC**:

- Clear acceptance criteria (tests define "done")
- Prevents scope creep (only implement what tests require)
- Regression protection (catch breaks immediately)
- Living documentation (tests show how code should be used)

**Verification Gates**:

- After FHIR tests: All FHIR resources validate against R4 spec
- After integration tests: All API endpoints return correct responses per
  OpenAPI spec
- After unit tests: All business logic validated (assignment rules, capacity
  warnings, audit trails)
- After frontend tests: All UI components render correctly and handle user
  interactions
- After E2E tests: All user scenarios (P1, P2A, P2B) work end-to-end

## Complexity Tracking

No complexity violations - plan fully compliant with OpenELIS Global 3.0
Constitution.

---

## Phase 0: Outline & Research

**Objective**: Validate technology choices, resolve unknowns, document best
practices for OpenELIS patterns.

### Research Tasks

No NEEDS CLARIFICATION items in Technical Context - all technologies specified
per constitution. Research focuses on validating existing OpenELIS patterns and
best practices.

**Research Questions**:

1. **Hibernate XML Mapping Pattern**: How are existing OpenELIS entities mapped
   using Hibernate XML? (Examine existing .hbm.xml files for reference patterns)

2. **FHIR Location Resource Structure**: What is the correct FHIR R4 Location
   resource structure for hierarchical storage? (Validate against IHE mCSD
   profile requirements)

3. **Carbon Dropdown Cascading**: What is the best practice for implementing
   cascading dropdowns in Carbon Design System? (Check @carbon/react Dropdown
   component API)

4. **Barcode Scanner Integration**: How do USB HID barcode scanners emit
   keyboard input in browser context? (Research browser keyboard event handling
   for scan gun input)

5. **SWR Data Fetching Pattern**: How does existing OpenELIS frontend use SWR
   for API calls? (Examine existing hooks in `frontend/src/hooks/`)

6. **Cypress E2E Setup**: What is the OpenELIS Cypress configuration and test
   structure? (Examine existing cypress.config.js and test files in
   cypress/e2e/)

### Research Output Structure

Create `research.md` with sections:

```markdown
# Research: Sample Storage Management

## 1. Hibernate XML Mapping Pattern

- **Pattern**: [Describe existing OpenELIS pattern from .hbm.xml files]
- **Example**: [Reference to existing entity mapping]
- **Application**: [How to apply to StorageRoom, StorageDevice, etc.]

## 2. FHIR Location Resource Structure

- **R4 Specification**: [Link to HL7 FHIR R4 Location resource]
- **IHE mCSD Profile**: [Hierarchical location requirements]
- **Mapping Strategy**: [Room → Location, Device → Location.partOf, etc.]

## 3. Carbon Dropdown Cascading

- **Component**: [@carbon/react Dropdown API reference]
- **Pattern**: [Controlled component with onChange handlers]
- **Data Flow**: [Parent state management for cascading selection]

## 4. Barcode Scanner Browser Integration

- **Event Type**: [Keyboard events with rapid character input]
- **Detection**: [Timing-based detection (characters within ~50ms = scan)]
- **Implementation**: [useEffect hook with keydown listener]

## 5. SWR Data Fetching Pattern

- **Existing Pattern**: [Reference to OpenELIS hooks using SWR]
- **Caching Strategy**: [SWR cache key structure]
- **Mutation Pattern**: [useSWRMutation for POST/PUT/DELETE]

## 6. Cypress E2E Configuration

- **Status**: [Existing Cypress 12.17.3 framework in OpenELIS]
- **Configuration**: [cypress.config.js structure and test patterns]
- **Test Structure**: [Page object pattern from existing tests]
```

**Deliverable**: `specs/001-sample-storage/research.md` with all 6 questions
answered

---

## Phase 1: Design & Contracts (Test Specifications)

**Prerequisites**: research.md complete

**Objective**: Create design artifacts that serve as **test specifications**.
These documents define WHAT to test BEFORE writing any code.

### Task 1.1: Generate Data Model (Test Specification)

Create `data-model.md` documenting entity schemas, relationships, and validation
rules. This document serves as the specification for:

- **FHIR validation tests**: Verify entity → FHIR Location transformation
  correctness
- **Integration tests**: Verify database persistence matches schema
- **Unit tests**: Verify validation rules enforced

**Content**:

**Entities** (extract from spec.md Key Entities section):

1. **StorageRoom**

   - Fields: id (VARCHAR(36)), fhir_uuid (UUID), name (VARCHAR(255)), code
     (VARCHAR(50)), description (TEXT), active (BOOLEAN), sys_user_id (INT),
     lastupdated (TIMESTAMP)
   - Constraints: Unique (code), NOT NULL (name, code, active)
   - Relationships: One-to-Many with StorageDevice

2. **StorageDevice**

   - Fields: id, fhir_uuid, name, code, type (ENUM:
     freezer/refrigerator/cabinet/other), temperature_setting (DECIMAL),
     capacity_limit (INT), active, parent_room_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (code within parent_room_id), NOT NULL (name, code,
     type, parent_room_id)
   - Relationships: Many-to-One with StorageRoom, One-to-Many with StorageShelf

3. **StorageShelf**

   - Fields: id, fhir_uuid, label, capacity_limit (INT), active,
     parent_device_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (label within parent_device_id), NOT NULL (label,
     parent_device_id)
   - Relationships: Many-to-One with StorageDevice, One-to-Many with StorageRack

4. **StorageRack**

   - Fields: id, fhir_uuid, label, rows (INT), columns (INT),
     position_schema_hint (VARCHAR(50)), active, parent_shelf_id (FK),
     sys_user_id, lastupdated
   - Constraints: Unique (label within parent_shelf_id), NOT NULL (label,
     parent_shelf_id), CHECK (rows >= 0 AND columns >= 0)
   - Relationships: Many-to-One with StorageShelf, One-to-Many with
     StoragePosition
   - Calculated: capacity = rows \* columns (or 0 if no grid)

5. **StoragePosition**

   - Fields: id, fhir_uuid (UUID), coordinate (VARCHAR(50), optional), row_index
     (INT, optional), column_index (INT, optional), occupied (BOOLEAN DEFAULT
     false), parent_device_id (FK, required), parent_shelf_id (FK, optional),
     parent_rack_id (FK, optional), sys_user_id, lastupdated
   - Constraints: NOT NULL (parent_device_id), UNIQUE (fhir_uuid), coordinate
     optional (only for 5-level positions), CHECK (if parent_rack_id NOT NULL
     then parent_shelf_id NOT NULL), CHECK (if coordinate NOT NULL then
     parent_rack_id NOT NULL), coordinate allows duplicates within same rack
     (flexible storage)
   - Relationships: Many-to-One with StorageDevice (required), Many-to-One with
     StorageShelf (optional), Many-to-One with StorageRack (optional),
     One-to-One with SampleStorageAssignment (current)
   - Note: Position represents the lowest level in hierarchy for a sample
     assignment. Can be at device level (2 levels), shelf level (3 levels), rack
     level (4 levels), or position level (5 levels). Minimum requirement is
     device level (room + device); cannot be just a room. Maps to FHIR Location
     resource with occupancy extension.

6. **SampleStorageAssignment**

   - Fields: id, sample_id (FK to Sample), location_id (numeric, NOT NULL),
     location_type (VARCHAR(20), NOT NULL), position_coordinate (VARCHAR(50),
     nullable), assigned_by_user_id (FK to SystemUser), assigned_date
     (TIMESTAMP), notes (TEXT)
   - Constraints: NOT NULL (sample_id, location_id, location_type,
     assigned_by_user_id), Unique (sample_id) - one current location per sample
   - CHECK constraint: `location_type` must be one of: 'device', 'shelf', 'rack'
     (position is just text coordinate, not entity)
   - Relationships: Many-to-One with Sample, Polymorphic relationship to
     StorageDevice/StorageShelf/StorageRack via location_id + location_type,
     Many-to-One with SystemUser
   - Note: Represents CURRENT location. Supports flexible assignment to any
     hierarchy level (device/shelf/rack) via simplified polymorphic
     relationship. Position is represented as optional text field
     (`position_coordinate`), not a separate entity reference. Historical moves
     tracked in SampleStorageMovement.

7. **SampleStorageMovement**
   - Fields: id, sample_id (FK), previous_position_id (FK), new_position_id
     (FK), moved_by_user_id (FK), movement_date (TIMESTAMP), reason (TEXT)
   - Constraints: NOT NULL (sample_id, moved_by_user_id, movement_date),
     previous_position_id OR new_position_id can be NULL (initial assignment or
     removal)
   - Relationships: Many-to-One with Sample, Many-to-One with StoragePosition
     (previous), Many-to-One with StoragePosition (new), Many-to-One with
     SystemUser
   - Note: Immutable audit log. Insertion only, no updates/deletes.

**Validation Rules** (from spec.md Functional Requirements):

- Require minimum 2 levels for valid location: Room and Device MUST be selected
  (FR-033a). Shelf, Rack, and Position are optional. Position can be at device
  level (2 levels), shelf level (3 levels), rack level (4 levels), or position
  level (5 levels). Minimum requirement is device level (room + device); cannot
  be just a room.
- Prevent assignment to inactive location (FR-035)
- Prevent double-occupancy unless rack allows duplicates (FR-034)
- Capacity warnings at 80%, 90%, 100% (FR-036) - no hard block
- Position coordinate free text, max 50 chars (FR-010)
- Hierarchical barcode uniqueness (FR-004)

**State Transitions**:

- Sample: No location → Assigned → Moved (multiple times) → [Disposed - deferred
  to P3]
- Position: Empty (occupied=false) → Occupied (occupied=true) → Empty (on sample
  move/disposal)
- Location hierarchy: Active → Inactive (deactivation requires no active samples
  or warning)

### Task 1.2: Generate API Contracts (Test Specification)

Create `/contracts/storage-api.json` (OpenAPI 3.0) specification. This document
serves as the contract for:

- **Backend integration tests**: Verify REST endpoints match request/response
  schemas
- **Frontend component tests**: Mock API responses match contract
- **E2E tests**: Verify complete request/response flow

**Endpoints**:

**Storage Hierarchy Management**:

- `GET /rest/storage/rooms` - List all rooms (with optional filters)
- `POST /rest/storage/rooms` - Create room
- `GET /rest/storage/rooms/{id}` - Get room details
- `PUT /rest/storage/rooms/{id}` - Update room
- `DELETE /rest/storage/rooms/{id}` - Delete room (if no children)
- `GET /rest/storage/devices` - List devices (filterable by room)
- `POST /rest/storage/devices` - Create device
- [... similar CRUD for shelves, racks, positions]

**Sample Storage Assignment**:

- `POST /rest/storage/samples/assign` - Assign sample to location
  - Request:
    `{ sample_id, location_id, location_type, position_coordinate?, notes }`
  - Response: `{ assignment_id, hierarchical_path, assigned_date }`
  - Validation:
    - `location_id` and `location_type` are required (NOT NULL)
    - `location_type` must be one of: 'device', 'shelf', 'rack' (position is
      just text coordinate, not entity)
    - If `location_type = 'device'`: Minimum 2 levels (room + device per
      FR-033a)
    - If `location_type = 'shelf'`: 3 levels (room + device + shelf)
    - If `location_type = 'rack'`: 4 levels (room + device + shelf + rack)
    - Location must be active (check entire hierarchy: room, device, shelf,
      rack)
    - `position_coordinate` is optional text (max 50 chars) for any
      location_type to provide specific position information

**Sample Search**:

- `GET /rest/storage/samples/search?sample_id={id}` - Search by sample ID
  - Response:
    `{ sample_id, type, status, location: { room, device, shelf, rack, position, hierarchical_path }, assigned_by, assigned_date }`
- `GET /rest/storage/samples?location_id={id}&location_type={room|device|shelf|rack}&status={active}` -
  Filter samples by location (single location dropdown) and status
  - `location_id`: ID of selected location (Room, Device, Shelf, or Rack)
  - `location_type`: Hierarchy level of selected location (determines downward
    inclusive filtering)
  - Filter behavior: Returns all samples within selected location's hierarchy
    (downward inclusive)
  - Example:
    `GET /rest/storage/samples?location_id=123&location_type=device&status=active`
    returns all samples in device 123 and all its child shelves/racks/positions

**Sample Movement**:

- `POST /rest/storage/samples/move` - Move sample to new location
  - Request:
    `{ sample_id, location_id, location_type, position_coordinate?, reason }`
  - Response: `{ movement_id, previous_location, new_location, moved_date }`
  - Validation:
    - `location_id` and `location_type` are required (NOT NULL)
    - `location_type` must be one of: 'device', 'shelf', 'rack' (position is
      just text coordinate, not entity)
    - If `location_type = 'device'`: Minimum 2 levels (room + device per
      FR-033a)
    - If `location_type = 'shelf'`: 3 levels (room + device + shelf)
    - If `location_type = 'rack'`: 4 levels (room + device + shelf + rack)
    - Location must be active (check entire hierarchy: room, device, shelf,
      rack)
    - `position_coordinate` is optional text (max 50 chars) for any
      location_type to provide specific position information
- `POST /rest/storage/samples/bulk-move` - Bulk move samples
  - Request:
    `{ sample_ids: [], target_rack_id, position_assignments: [{sample_id, position_coordinate}] }`
  - Response: `{ movement_ids: [], summary: { total, successful, failed } }`

**Sample Disposal**:

- `POST /rest/storage/samples/dispose` - Dispose sample
  - Request: `{ sample_id, reason, method, notes, date_time }`
  - Response: `{ disposal_id, sample_id, disposed_date, location_at_disposal }`
  - Validation: Requires authorization (role-based permission check), reason and
    method required
  - Note: Disposal workflow deferred to post-POC (P3), but endpoint structure
    defined

**Location Quick-Find Search**:

- `GET /rest/storage/locations/search?q={term}` - Search locations at any
  hierarchy level
  - Query parameter: `q` - search term (location name or code)
  - Response: `[{ id, name, code, type, hierarchical_path, level }]` - Array of
    matching locations with full hierarchical paths
  - Behavior: Case-insensitive partial matching across Room, Device, Shelf, and
    Rack levels
  - Example: `GET /rest/storage/locations/search?q=freezer` returns devices
    matching "freezer" with full paths like "Main Laboratory > Freezer Unit 1"

**Barcode Generation**: ⏸️ Deferred to post-POC (not in P1/P2A/P2B core
workflows)

### Task 1.3: Generate FHIR Mappings (Test Specification)

Create `/contracts/fhir-mappings.md` documenting FHIR resource structure. This
document serves as the specification for:

- **FHIR validation tests**: Verify transform service outputs match FHIR R4
  Location spec
- **IHE mCSD compliance tests**: Verify hierarchical queries work correctly
- **Integration tests**: Verify FHIR sync occurs after entity persistence

**Content**:

**FHIR R4 Location Resource Mapping**:

```markdown
# FHIR Location Mappings for Storage Entities

## Room → FHIR Location

- `Location.id` = StorageRoom.fhir_uuid
- `Location.name` = StorageRoom.name
- `Location.identifier.value` = StorageRoom.code
- `Location.status` = StorageRoom.active ? "active" : "inactive"
- `Location.description` = StorageRoom.description
- `Location.mode` = "instance"
- `Location.physicalType.coding.code` = "ro" (room)

## Device → FHIR Location

- `Location.id` = StorageDevice.fhir_uuid
- `Location.name` = StorageDevice.name
- `Location.identifier.value` = "ROOM_CODE-DEVICE_CODE" (hierarchical)
- `Location.status` = active/inactive
- `Location.mode` = "instance"
- `Location.physicalType.coding.code` = "ve" (vehicle/equipment)
- `Location.type.coding.code` = StorageDevice.type (freezer/fridge/cabinet)
- `Location.partOf.reference` = "Location/{parent_room_fhir_uuid}"

## Shelf → FHIR Location

- Similar to Device
- `Location.partOf.reference` = "Location/{parent_device_fhir_uuid}"
- `Location.physicalType.coding.code` = "co" (container)

## Rack → FHIR Location

- Similar to Shelf
- `Location.partOf.reference` = "Location/{parent_shelf_fhir_uuid}"
- Custom extension for rows/columns: `extension[grid-dimensions]`

## Position → FHIR Location (with Extensions)

- Maps to FHIR R4 `Location` resource (child of parent location)
- `Location.id` = StoragePosition.fhir_uuid
- `Location.identifier.value` = hierarchical code based on position level:
  - Device level: "{room_code}-{device_code}"
  - Shelf level: "{room_code}-{device_code}-{shelf_label}"
  - Rack level: "{room_code}-{device_code}-{shelf_label}-{rack_label}"
  - Position level:
    "{room_code}-{device_code}-{shelf_label}-{rack_label}-{coordinate}"
- `Location.name` = coordinate (if position level) or device/shelf/rack label
  (if lower level)
- `Location.partOf.reference` = "Location/{parent_fhir_uuid}" (parent device,
  shelf, or rack depending on position level)
- `Location.extension[position-occupancy].valueBoolean` = occupied status
- `Location.extension[position-grid-row].valueInteger` = row index (optional)
- `Location.extension[position-grid-column].valueInteger` = column index
  (optional)

## Sample-to-Location Link

- `Specimen.container.identifier.value` = full hierarchical path
- `Specimen.container.extension[storage-position-location].valueReference` =
  "Location/{position_fhir_uuid}"
- `Specimen.extension[storage-assigned-date].valueDateTime` = assignment
  timestamp
```

**IHE mCSD Compliance**:

- All Location resources queryable via `GET /fhir/Location?partOf={parent_id}`
- Hierarchical queries supported: `GET /fhir/Location?_include=Location:partOf`
- Position availability queries:
  `GET /fhir/Location?partOf={rack_fhir_uuid}&extension=position-occupancy|false`

**FHIR Sync Strategy**:

- All entities (Room, Device, Shelf, Rack, Position): Sync immediately on entity
  create/update via @PostPersist/@PostUpdate hooks
- Uses existing OpenELIS FHIR sync pattern (FhirTransformService +
  FhirPersistanceService)
- Specimen: Update container extension on sample assignment/movement

**Inline Location Creation**:

- Uses same REST endpoints (POST /storage/rooms, POST /storage/devices, etc.)
- Frontend manages state to immediately show newly created location in selector
  dropdown
- No special "inline" endpoints needed - standard CRUD operations

**SamplePatientEntry Integration** (Orders Workflow):

- **Integration Point**: Below "Collector" field in sample collection section
- **Widget Placement**: After collector dropdown, before sample collection time
- **Behavior**: Optional assignment (can be left blank and assigned later)
- **Widget Structure**: Compact inline view showing selected location path (or
  "Not assigned") with "Expand" button
- **Expanded Modal**: Opens full location assignment modal matching View Storage
  modal structure (sample info, current location, full assignment form)
- **Component**: Embeds
  `<StorageLocationSelector workflow="orders" optional={true} />`

**LogbookResults Integration** (Results Workflow):

- **Integration Point**: Below existing referral/test result fields in expanded
  sample details
- **Widget Structure**: Compact inline view with quick-find search input
  (type-ahead autocomplete) for rapidly finding existing locations + "Expand"
  button
- **Quick-Find Search**: Matches location names/codes at any hierarchy level
  (Room, Device, Shelf, or Rack) using case-insensitive partial matching,
  displays full hierarchical path in results
- **Expanded Modal**: Opens full location assignment modal matching View Storage
  modal structure (sample info, current location, full assignment form)
- **Behavior**: Shows current location (read-only or editable based on
  permissions), allows Move action via overflow menu
- **Component**: Embeds
  `<StorageLocationSelector workflow="results" showQuickFind={true} />`

### Task 1.4: Generate Quickstart (Test-First Development Guide)

Create `quickstart.md` documenting test-first development workflow and
environment setup. This guide emphasizes running tests BEFORE writing
implementation code.

````markdown
# Quickstart: Sample Storage Management POC

## Prerequisites

- OpenELIS Global 3.0 development environment running (see
  [dev_setup.md](../../../docs/dev_setup.md))
- PostgreSQL 14+ database accessible
- Java 21, Maven 3.8+, Node.js 16+

## Backend Setup

1. **Database Migration**
   ```bash
   # Liquibase changesets auto-run on application startup
   # Verify migration: psql -U clinlims -d clinlims -c "\dt storage_*"
   ```
````

2. **Build Backend**

   ```bash
   cd /Users/pmanko/code/OpenELIS-Global-2
   mvn clean install -DskipTests -Dmaven.test.skip=true
   ```

3. **Run Tests**

   ```bash
   # Unit tests
   mvn test -Dtest="org.openelisglobal.storage.**"

   # Integration tests (requires DB)
   mvn verify -Dtest="org.openelisglobal.storage.controller.**"
   ```

## Frontend Setup

1. **Install Dependencies** (if not already done)

   ```bash
   cd frontend
   npm install
   ```

2. **Add Internationalization Keys**

   - Edit `frontend/src/languages/en.json`, `fr.json`, `sw.json`
   - Add storage.\* message keys (see translations section in this doc)

3. **Run Frontend Dev Server**

   ```bash
   npm start
   # Access at https://localhost/
   ```

4. **Run Frontend Tests**

   ```bash
   # Unit tests
   npm test -- components/storage

   # E2E tests (Cypress)
   npm run cy:run -- --spec "cypress/e2e/storage*.cy.js"
   ```

## FHIR Validation

1. **Access FHIR Server**

   ```
   https://fhir.openelis.org:8443/fhir/
   ```

2. **Query Storage Locations**

   ```bash
   # Get all rooms
   curl https://fhir.openelis.org:8443/fhir/Location?physicalType=ro

   # Get devices in a specific room
   curl https://fhir.openelis.org:8443/fhir/Location?partOf=Location/{room_fhir_uuid}
   ```

3. **Validate FHIR Resource**
   ```bash
   # POST new Location resource and check response
   curl -X POST https://fhir.openelis.org:8443/fhir/Location \
     -H "Content-Type: application/fhir+json" \
     -d @test-location.json
   ```

## Testing User Scenarios

### P1: Basic Storage Assignment

1. Navigate to Sample Patient Entry
2. Complete sample accessioning
3. In Storage Location Selector widget:
   - Verify compact inline view shows "Not assigned" initially
   - Click "Expand" button to open full location assignment modal
   - In expanded modal, try cascading dropdown mode (Room → Device → Shelf →
     Rack → Position)
   - Try type-ahead autocomplete in expanded modal
   - Verify modal shows sample info box and current location section
   - Select location and verify hierarchical path updates in compact view
4. Verify assignment saved with hierarchical path

### P2A: Sample Search/Retrieval

1. Navigate to Logbook Results
2. Search for sample ID
3. Verify location displays in compact inline view (with quick-find search
   input)
4. Test quick-find search: Type location name/code, verify autocomplete results
   show full hierarchical paths
5. Select location from quick-find results, verify compact view updates
6. Click "Expand" button to open full location assignment modal
7. Navigate to Storage Dashboard, Samples tab
8. Test single location dropdown filter:
   - Open location dropdown
   - Test autocomplete search (type "Freezer" to find devices)
   - Test hierarchical browsing (expand/collapse tree view)
   - Select a Room → verify all samples in that room (downward inclusive)
   - Select a Device → verify all samples in that device and its children
   - Verify inactive locations are visually distinguished
   - Test combination: location filter + status filter

### P2B: Sample Movement

1. Navigate to Storage Dashboard, Samples tab
2. Find sample with assigned location
3. Click overflow menu (⋮) in Actions column
4. Verify menu shows: Move, Dispose, View Audit (placeholder/disabled), View
   Storage
5. Click "Move" from overflow menu
6. Verify Move modal opens with:
   - Modal title "Move Sample" with subtitle
   - Current location displayed in gray box
   - Downward arrow icon separator
   - New location selector in bordered box (Room/Device/Shelf/Rack dropdowns)
   - "Selected Location" preview box (shows "Not selected" initially)
   - Optional "Reason for Move" textarea
   - Cancel and "Confirm Move" buttons
7. Select new location, verify "Selected Location" preview updates
8. Enter reason (optional)
9. Click "Confirm Move"
10. Verify audit trail updated

### P2B Extended: View Storage Modal

1. Navigate to Storage Dashboard, Samples tab
2. Find sample with assigned location
3. Click overflow menu (⋮) in Actions column
4. Click "View Storage"
5. Verify View Storage modal opens with:
   - Modal title "Storage Location Assignment"
   - Sample information section (Sample ID, Type, Status) in highlighted box
   - Current Location section showing full hierarchical path in gray box
   - Visual separator
   - Full location assignment form (barcode scan input,
     Room/Device/Shelf/Rack/Position selectors, condition notes)
   - Cancel and "Assign Storage Location" buttons
6. Edit location assignment using form
7. Verify changes saved

### P3: Sample Disposal (Deferred to Post-POC)

1. Navigate to Storage Dashboard, Samples tab
2. Find sample to dispose
3. Click overflow menu (⋮) in Actions column
4. Click "Dispose"
5. Verify Dispose modal opens with:
   - Modal title "Dispose Sample" with subtitle
   - Red warning alert at top ("This action cannot be undone")
   - Sample information section (Sample ID, Type, Status) in gray box
   - Current Storage Location section with location pin icon
   - Disposal instructions info box
   - Required "Disposal Reason" dropdown
   - Required "Disposal Method" dropdown
   - Optional "Additional Notes" textarea
   - Confirmation checkbox ("I confirm...")
   - Cancel and "Confirm Disposal" button (red/destructive styling, disabled
     until checkbox checked)
6. Select reason and method
7. Check confirmation checkbox
8. Click "Confirm Disposal"
9. Verify sample marked as disposed

## Troubleshooting

- **Liquibase migration fails**: Check `liquibase/storage/*.xml` syntax
- **FHIR sync fails**: Verify FHIR server running at configured URI
- **Frontend widget not appearing**: Check React Intl message keys loaded
- **Barcode scanner not detected**: Verify USB HID scanner emitting keyboard
  events

````

### Task 1.5: Update Agent Context

Run agent context update script:

```bash
cd /Users/pmanko/code/OpenELIS-Global-2
.specify/scripts/bash/update-agent-context.sh cursor-agent
````

This script will:

- Detect Cursor AI agent context file
- Add new technology references from this plan:
  - Storage module package structure
  - FHIR Location resource mapping (flexible hierarchy: positions can have 2-5
    levels)
  - Carbon Design System Storage Location Selector widget
  - Cypress E2E testing patterns (existing OpenELIS framework)
- Preserve existing OpenELIS patterns and manual additions

**Deliverables**:

- `data-model.md` - Entity schemas, relationships, validation rules
- `/contracts/storage-api.json` - OpenAPI 3.0 REST endpoints
- `/contracts/fhir-mappings.md` - FHIR R4 Location resource mappings
- `quickstart.md` - Developer setup instructions
- Updated agent context file (Cursor-specific)

---

## Phase 2: Task Breakdown (Deferred)

**Not executed by `/speckit.plan` command.**

Use `/speckit.tasks` command to generate detailed task breakdown from this plan.
Tasks will be organized by user story (P1, P2A, P2B) with dependency ordering
and parallel execution markers.

---

## Post-Phase 1 Constitution Re-Check

_Re-verify compliance after design artifacts generated:_

- [x] **Configuration-Driven**: Position coordinates remain free-text, no
      hardcoded validation
- [x] **Carbon Design System**: Storage Location Selector uses @carbon/react
      Dropdown, TextInput, Button components
- [x] **FHIR/IHE Compliance**: All hierarchy levels (Room, Device, Shelf, Rack,
      Position) map to FHIR Location resources, IHE mCSD hierarchy supported.
      Positions can have 2-5 levels (minimum: room+device, maximum:
      room+device+shelf+rack+position).
- [x] **Layered Architecture**: All 5 layers present (Valueholder → DAO →
      Service → Controller → Form)
- [x] **Test Coverage**: Unit/integration/Cypress E2E test structure defined in
      quickstart
- [x] **Schema Management**: Liquibase changesets planned in
      `liquibase/storage/` with fhir_uuid columns
- [x] **Internationalization**: Message keys documented for en/fr/sw in
      quickstart
- [x] **Security & Compliance**: RBAC enforced in controllers, audit fields in
      all entities

**Final Verdict**: ✅ Plan fully compliant with OpenELIS Global 3.0 Constitution

---

---

## Phase 5: Overflow Menu and Modal Components Implementation

### Objective

Implement samples table row overflow menu with four actions (Move, Dispose, View
Audit placeholder, View Storage) and three modal components (Move, Dispose, View
Storage) matching Figma design specifications.

### Overflow Menu Component

**Component**: `SampleActionsOverflowMenu.jsx`

**Requirements**:

- Uses Carbon Design System `OverflowMenu` component
- Displays four menu items:
  1. **Move** - Opens Move modal
  2. **Dispose** - Opens Dispose modal
  3. **View Audit** - Placeholder (disabled or with visual indicator)
  4. **View Storage** - Opens View Storage modal
- Accessible via keyboard navigation and screen readers
- Integrated into samples table Actions column

**Implementation**:

```jsx
import { OverflowMenu, OverflowMenuItem } from "@carbon/react";

<OverflowMenu>
  <OverflowMenuItem itemText="Move" onClick={() => openMoveModal(sample)} />
  <OverflowMenuItem
    itemText="Dispose"
    onClick={() => openDisposeModal(sample)}
  />
  <OverflowMenuItem itemText="View Audit" disabled />
  <OverflowMenuItem
    itemText="View Storage"
    onClick={() => openViewStorageModal(sample)}
  />
</OverflowMenu>;
```

**Testing**:

- Unit test: Menu renders with all four items
- Unit test: "View Audit" is disabled
- E2E test: Clicking menu items opens corresponding modals

### Move Sample Modal Component

**Component**: `MoveSampleModal.jsx`

**Requirements** (per FR-040a through FR-040h):

- Modal title: "Move Sample" with subtitle "Move sample [Sample ID] to a new
  storage location"
- **Current Location** section: Full hierarchical path in highlighted gray
  background box
- Downward-pointing arrow icon as visual separator
- **New Location** section in bordered box:
  - Barcode scan input field (deferred to later stage - field present but not
    functional)
  - Room dropdown (required, initially shows "Select room..." placeholder)
  - Device dropdown (disabled until room selected, shows "Select device..."
    placeholder)
  - Shelf dropdown (disabled until device selected, shows "Select shelf..."
    placeholder)
  - Rack dropdown (disabled until shelf selected, shows "Select rack..."
    placeholder)
- **Selected Location** preview section: Gray background box showing selected
  hierarchical path (displays "Not selected" until location chosen)
- Optional "Reason for Move" textarea field
- Footer buttons: Cancel and "Confirm Move" (primary/dark styling)
- Uses Carbon Design System `Modal` component with proper accessibility
  attributes

**Implementation Notes**:

- Reuses `LocationSelectorModal` component for new location selection
- Updates "Selected Location" preview in real-time as user selects location
- Validates new location is different from current location
- Calls `POST /rest/storage/samples/move` endpoint on confirm

**Testing**:

- Unit test: Modal renders with all required sections
- Unit test: Location selection updates preview
- Unit test: Validation prevents moving to same location
- E2E test: Complete move workflow with audit trail verification

### Dispose Sample Modal Component

**Component**: `DisposeSampleModal.jsx`

**Requirements** (per FR-051a through FR-051k):

- Modal title: "Dispose Sample" with subtitle "Permanently dispose of sample
  [Sample ID]"
- Red warning alert box at top: "This action cannot be undone. The sample will
  be marked as disposed and removed from storage."
- **Sample Information** section: Gray background box showing Sample ID, Type,
  Status
- **Current Storage Location** section:
  - Location pin icon
  - Full hierarchical path in gray background box
  - Helper text: "Sample will be removed from this location upon disposal"
- Disposal instructions info box (blue/info styling) with sample-specific
  instructions
- Horizontal separator line
- Required fields:
  - "Disposal Reason \*" dropdown (required, marked with asterisk, initially
    shows "Select reason..." placeholder)
  - "Disposal Method \*" dropdown (required, marked with asterisk, initially
    shows "Select method..." placeholder)
- Optional "Additional Notes (optional)" textarea field
- Confirmation checkbox: "I confirm that I want to permanently dispose of this
  sample. This action cannot be undone."
- Footer buttons:
  - Cancel button
  - "Confirm Disposal" button:
    - Red/destructive action styling (e.g., rgba(231,0,11,0.6) background)
    - Disabled until confirmation checkbox checked
- Uses Carbon Design System `Modal` component with proper accessibility
  attributes

**Implementation Notes**:

- Dropdown values:
  - Disposal Reason: Expired, Contaminated, Patient Request, Testing Complete,
    Other
  - Disposal Method: Biohazard Autoclave, Chemical Neutralization, Incineration,
    Other
- Date/Time auto-set to current timestamp (editable for backdating if needed)
- Authorization check via role-based permissions
- Calls `POST /rest/storage/samples/dispose` endpoint on confirm

**Testing**:

- Unit test: Modal renders with all required sections
- Unit test: "Confirm Disposal" button disabled until checkbox checked
- Unit test: Validation requires reason and method selection
- E2E test: Complete disposal workflow with audit trail verification

### View Storage Modal Component

**Component**: `ViewStorageModal.jsx`

**Requirements** (per FR-056b through FR-056i):

- Modal title: "Storage Location Assignment"
- **Sample Information** section: Highlighted/background box showing Sample ID,
  Type, Status
- **Current Location** section: Full hierarchical path (Room > Device > Shelf >
  Rack > Position) in highlighted gray background box
- Visual separator between current location and assignment form
- **Full Location Assignment Form**:
  - Barcode scan input field (Quick Assign) - deferred to later stage (field
    present but not functional)
  - Room dropdown selector (required, marked with \*)
  - Device dropdown selector
  - Shelf dropdown selector
  - Rack/Box dropdown selector
  - Position text input (optional, with format hint)
  - Condition Notes textarea (optional)
- Footer buttons: Cancel and "Assign Storage Location"
- Uses Carbon Design System `Modal` component with proper accessibility
  attributes
- Allows editing/changing storage location assignment using same form controls
  as initial assignment

**Implementation Notes**:

- Reuses `LocationSelectorModal` component for location selection
- Pre-populates form with current location if sample has assignment
- Calls `POST /rest/storage/samples/assign` or
  `PUT /rest/storage/samples/assign` endpoint on confirm

**Testing**:

- Unit test: Modal renders with all required sections
- Unit test: Form pre-populates with current location
- Unit test: Validation requires Room and Device selection (minimum 2 levels)
- E2E test: View and edit storage location assignment

### Storage Location Selector Widget Structure Updates

**Component Updates**: `StorageLocationSelector.jsx`, `CompactLocationView.jsx`,
`LocationSelectorModal.jsx`, `QuickFindSearch.jsx`

**Two-Tier Design**:

1. **Compact Inline View** (`CompactLocationView.jsx`):

   - Displays selected location hierarchical path (or "Not assigned" if no
     location)
   - Shows "Expand" or "Edit" button
   - Results workflow: Includes quick-find search input (`QuickFindSearch.jsx`)
   - Quick-find: Type-ahead autocomplete matching Room/Device/Shelf/Rack levels,
     displays full hierarchical paths in results

2. **Expanded Modal View** (`LocationSelectorModal.jsx`):
   - Matches View Storage modal structure
   - Sample information section
   - Current location display
   - Full assignment form (barcode scan input, Room/Device/Shelf/Rack/Position
     selectors, condition notes)
   - Cancel and action buttons

**Implementation Notes**:

- Same widget component used in both SamplePatientEntry (orders) and
  LogbookResults (results)
- Quick-find search only shown in results workflow (`showQuickFind={true}` prop)
- Quick-find calls `GET /rest/storage/locations/search?q={term}` endpoint
- Barcode scan input present but functionality deferred to later stage

**Testing**:

- Unit test: Compact view displays location path correctly
- Unit test: Expand button opens modal
- Unit test: Quick-find search filters locations correctly
- Unit test: Modal structure matches View Storage modal
- E2E test: Complete assignment workflow in both orders and results contexts

## Implementation Enhancements

### Helper Methods (Service Layer)

**1. Hierarchical Path Builder**

```java
// In StorageLocationService
public String buildHierarchicalPath(StoragePosition position) {
    StringBuilder path = new StringBuilder();

    // Position always has parent_device (required), which has parent_room
    StorageDevice device = position.getParentDevice();
    StorageRoom room = device.getParentRoom();

    path.append(room.getName()).append(" > ").append(device.getName());

    // Add shelf if present (3+ level position)
    if (position.getParentShelf() != null) {
        StorageShelf shelf = position.getParentShelf();
        path.append(" > ").append(shelf.getLabel());

        // Add rack if present (4+ level position)
        if (position.getParentRack() != null) {
            StorageRack rack = position.getParentRack();
            path.append(" > ").append(rack.getLabel());

            // Add coordinate if present (5-level position)
            if (position.getCoordinate() != null && !position.getCoordinate().isEmpty()) {
                path.append(" > Position ").append(position.getCoordinate());
            }
        }
    }

    return path.toString();
}
```

**2. Capacity Calculator with Warnings**

```java
// In SampleStorageService
public CapacityWarning calculateCapacity(StorageRack rack) {
    int totalCapacity = rack.getRows() * rack.getColumns();
    if (totalCapacity == 0) return null; // No grid

    int occupied = positionDAO.countOccupied(rack.getId());
    int percentage = (occupied * 100) / totalCapacity;

    String warningMessage = null;
    if (percentage >= 100) {
        warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.",
            rack.getLabel(), percentage);
    } else if (percentage >= 90) {
        warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.",
            rack.getLabel(), percentage);
    } else if (percentage >= 80) {
        warningMessage = String.format("Rack %s is %d%% full. Consider using alternative storage.",
            rack.getLabel(), percentage);
    }

    return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
}
```

**3. Optimistic Locking Handler**

```java
// In SampleStorageService
@Transactional
public Map<String, Object> assignSampleWithLocation(String sampleId, String locationId,
        String locationType, String positionCoordinate, String notes) {
    // Validate location_id and location_type are provided
    if (locationId == null || locationType == null) {
        throw new ValidationException("location_id and location_type are required");
    }

    // Validate location_type is one of: 'device', 'shelf', 'rack'
    if (!Arrays.asList("device", "shelf", "rack").contains(locationType)) {
        throw new ValidationException("location_type must be one of: 'device', 'shelf', 'rack'");
    }

    // Load location entity based on locationType
    Object locationEntity = switch (locationType) {
        case "device" -> storageLocationService.get(Integer.parseInt(locationId), StorageDevice.class);
        case "shelf" -> storageLocationService.get(Integer.parseInt(locationId), StorageShelf.class);
        case "rack" -> storageLocationService.get(Integer.parseInt(locationId), StorageRack.class);
        default -> throw new ValidationException("Invalid location_type: " + locationType);
    };

    // Validate location has minimum 2 levels (room + device per FR-033a)
    validateLocationActiveForEntity(locationEntity, locationType);

    // Create assignment with location_id + location_type
    SampleStorageAssignment assignment = new SampleStorageAssignment();
    assignment.setSample(sampleDAO.get(sampleId).orElseThrow());
    assignment.setLocationId(Integer.parseInt(locationId));
    assignment.setLocationType(locationType);
    assignment.setPositionCoordinate(positionCoordinate);
    assignment.setAssignedByUserId(getCurrentUserId());
    assignment.setNotes(notes);

    assignmentDAO.insert(assignment);

    // Build hierarchical path
    String hierarchicalPath = buildHierarchicalPathForEntity(locationEntity, locationType, positionCoordinate);

    // Create audit log entry
    SampleStorageMovement movement = new SampleStorageMovement();
    movement.setSample(assignment.getSample());
    movement.setNewLocationId(Integer.parseInt(locationId));
    movement.setNewLocationType(locationType);
    movement.setMovedByUserId(getCurrentUserId());
    movement.setReason("Initial assignment");
    movementDAO.insert(movement);

    Map<String, Object> result = new HashMap<>();
    result.put("assignmentId", assignment.getId().toString());
    result.put("hierarchicalPath", hierarchicalPath);
    result.put("assignedDate", assignment.getAssignedDate());

    return result;
}
```

### Sequence Diagram: Sample Assignment Workflow

```
User (Browser)
    │
    │ 1. Select location via widget (cascading dropdowns)
    ├──────────> StorageLocationSelector.jsx
    │                 │
    │                 │ 2. GET /rest/storage/rooms
    │                 ├──────────> StorageLocationRestController
    │                 │                 │
    │                 │                 │ 3. getRooms()
    │                 │                 ├──────────> StorageLocationService
    │                 │                 │                 │
    │                 │                 │                 │ 4. Query DB
    │                 │                 │                 ├──────────> StorageRoomDAO
    │                 │                 │                 │
    │                 │                 │                 │ 5. Return rooms
    │                 │                 │                 <──────────┤
    │                 │                 │
    │                 │                 │ 6. Return rooms JSON
    │                 │                 <──────────┤
    │                 │
    │                 │ 7. Populate room dropdown
    │                 <──────────┤
    │
    │ ... (repeat for device, shelf, rack, position selection)
    │
    │ 8. Click "Save" with position selected
    ├──────────> SamplePatientEntry.jsx
    │                 │
    │                 │ 9. POST /rest/storage/samples/assign
    │                 │    { sampleId, locationId, locationType, positionCoordinate?, notes }
    │                 ├──────────> SampleStorageRestController
    │                 │                 │
    │                 │                 │ 10. assignSampleWithLocation()
    │                 │                 ├──────────> SampleStorageService
    │                 │                 │                 │
    │                 │                 │                 │ 11. Validate location_id and location_type provided
    │                 │                 │                 │ 12. Validate location_type is 'device', 'shelf', or 'rack'
    │                 │                 │                 │ 13. Load location entity based on location_type
    │                 │                 │                 │ 14. Validate location active (check entire hierarchy)
    │                 │                 │                 │ 15. Calculate capacity warning (if applicable)
    │                 │                 │                 │
    │                 │                 │                 │ 16. Create assignment with location_id + location_type
    │                 │                 │                 ├──────────> SampleStorageAssignmentDAO
    │                 │                 │                 │                 │
    │                 │                 │                 │                 │ 15. UPDATE storage_position
    │                 │                 │                 │                 │    (optimistic lock check)
    │                 │                 │                 │                 <──────────┤
    │                 │                 │                 │
    │                 │                 │                 │ 16. Create assignment record
    │                 │                 │                 ├──────────> SampleStorageAssignmentDAO
    │                 │                 │                 │                 │
    │                 │                 │                 │                 │ 17. INSERT assignment
    │                 │                 │                 │                 <──────────┤
    │                 │                 │                 │
    │                 │                 │                 │ 18. Create movement audit
    │                 │                 │                 ├──────────> SampleStorageMovementDAO
    │                 │                 │                 │                 │
    │                 │                 │                 │                 │ 19. INSERT movement
    │                 │                 │                 │                 <──────────┤
    │                 │                 │                 │
    │                 │                 │                 │ 20. Build hierarchical path
    │                 │                 │                 ├──────────> buildHierarchicalPath()
    │                 │                 │                 │
    │                 │                 │                 │ 21. Return assignment
    │                 │                 │                 <──────────┤
    │                 │                 │
    │                 │                 │ 22. Return assignment JSON
    │                 │                 <──────────┤
    │                 │
    │                 │ 23. Show success notification
    │                 <──────────┤
    │
    │ 24. Display location in UI
    <──────────┤

Automatic (via JPA hooks):
StoragePosition entity
    │
    │ 25. @PostUpdate hook triggered (occupied changed)
    ├──────────> StorageLocationFhirTransform.transformToFhirLocation(position)
    │                 │
    │                 │ 26. Build FHIR Location resource with position-occupancy extension
    │                 │
    │                 │ 27. POST/PUT to FHIR server
    │                 └──────────> FhirPersistanceService.save(location)
    │                                   │
    │                                   │ 28. Sync to HAPI FHIR Server
    │                                   └──────────> https://fhir.openelis.org:8443/fhir/

Specimen entity
    │
    │ 29. @PostUpdate hook triggered (assignment created)
    └──────────> Update Specimen.container.extension[storage-position-location] reference
```

### Sample Entity Integration

**Existing Sample Entity**: `org.openelisglobal.sample.valueholder.Sample`

- **No modifications required** - Sample entity remains unchanged
- **Integration via junction table**: SampleStorageAssignment links Sample to
  StoragePosition
- **Foreign key**: `SampleStorageAssignment.sample_id` → `Sample.id`
- **Query pattern**:
  `JOIN sample_storage_assignment ON sample.id = sample_storage_assignment.sample_id`

**Benefits**:

- ✅ No impact on existing Sample entity code
- ✅ Backward compatible (samples without location continue to work)
- ✅ Easy to query samples by location (JOIN on assignment table)
- ✅ Easy to query location for a sample (JOIN on assignment table)

### Task 1.4: Generate Quickstart (Test-First Development Guide)
