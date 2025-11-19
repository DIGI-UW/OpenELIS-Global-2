# Implementation Plan: Sample Storage Management

**Branch**: `001-sample-storage` | **Date**: 2025-10-30 | **Last Updated**:
2025-11-22 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-sample-storage/spec.md`

## Summary

Implement POC for Sample Storage Management to track physical location of
biological samples through a flexible storage hierarchy (Room → Device → Shelf →
Rack → Position). Positions can have 2-5 levels (minimum: room+device, maximum:
room+device+shelf+rack+position). POC scope includes core tracking workflows:
assignment (P1), search/retrieval (P2A), movement (P2B), and basic Storage
Dashboard (P4 - see spec.md POC Scope Matrix for included/deferred features).
Defers disposal workflow (P3) and advanced dashboard features to post-POC
iterations.

**Technical Approach**: Leverage existing OpenELIS infrastructure (5-layer
backend architecture, HAPI FHIR R4 server, Carbon Design System UI) to add
storage location tracking. Create reusable Storage Location Selector widget with
two-tier design (compact inline view + expanded modal) supporting cascading
dropdowns, type-ahead autocomplete, and quick-find search. Widget used in both
orders (SamplePatientEntry) and results (LogbookResults) workflows. Implement
samples table overflow menu with Manage Location (consolidates Move and View
Storage), Dispose, and View Audit (placeholder) actions. Create consolidated
Location Management Modal that handles both assignment (when no location exists)
and movement (when location exists) with dynamic wording and conditional fields.
Create Dispose modal matching Figma design. Implement full CRUD operations for
location tabs (Rooms, Devices, Shelves, Racks) with overflow menu actions (Edit,
Delete) - Edit modal allows editing all fields except Code and Parent
(read-only), Delete validates constraints (child locations, active samples)
before deletion. Map storage entities to FHIR Location resources for external
interoperability. **Note**: MoveSampleModal is largely implemented and can be
used as a starting point for the consolidated Location Management Modal.

**Amendment (2025-11-07)**: Add expandable row functionality to location tables
(Rooms, Devices, Shelves, Racks) using Carbon DataTable expandable row pattern.
Expanded rows display additional entity fields (not visible in table columns) as
key-value pairs in read-only format. Only one row can be expanded at a time.
Expansion triggered by clicking chevron icon in dedicated first column. See
[research.md](./research.md#8-carbon-datatable-expandable-rows) for
implementation details.

**Amendment (2025-11-22)**: Add comprehensive barcode workflow implementation
following TDD approach. Includes unified input field (scan/type-ahead), 5-step
validation, debouncing (500ms), visual feedback, dual barcode auto-detection,
"last-modified wins" logic, label management (short code, printing, print
history), and error recovery. All barcode requirements from FR-023 through
FR-027f must be implemented. See Phase 10 below for detailed TDD workflow.

**Amendment**: Update capacity calculation logic to implement two-tier system
(per FR-062a, FR-062b, FR-062c). Devices and Shelves support manual
`capacity_limit` (static) or calculated capacity from children. If
`capacity_limit` is NULL, calculate from child locations (sum if all children
have defined capacities). If any child lacks defined capacity, parent capacity
cannot be determined and UI displays "N/A" with tooltip. Racks always use
calculated capacity (rows × columns). UI must visually distinguish between
manual and calculated capacities (badge, tooltip, or icon). See updated capacity
calculator implementation in Phase 9.5 below.

## Implementation Phase Structure

This document is an **Implementation Plan** that describes the technical
approach and architecture for building the Sample Storage Management feature.
The phases below correspond to implementation phases that are broken down into
actionable tasks in `tasks.md`.

**Phase Organization**:

- **Phase 1**: Setup & Database Schema
- **Phase 2**: Foundational - Core Entities & FHIR Transform
- **Phase 3**: Position Hierarchy Structure Update (2-5 Level Support)
- **Phase 4**: Flexible Assignment Architecture
- **Phase 5**: User Story 1 - Basic Storage Assignment
- **Phase 6**: User Story 2A - SampleItem Search and Retrieval
- **Phase 7**: User Story 2B - SampleItem Movement
- **Phase 7.5**: Modal Consolidation
- **Phase 8**: Location CRUD Operations
- **Phase 9**: Expandable Row Functionality
- **Phase 9.5**: Capacity Calculation Logic
- **Phase 10**: Barcode Workflow Implementation
- **Phase 11**: Polish & Cross-Cutting Concerns
- **Phase 12**: Constitution Compliance Verification

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**:

- Backend: Spring Boot 3.x, Hibernate 6.x, HAPI FHIR R4 (v6.6.2), JPA
- Frontend: @carbon/react v1.15.0, React Intl 5.20.12, Formik 2.2.9,
  getFromOpenElisServer/postToOpenElisServer utilities

**Storage**: PostgreSQL 14+ (existing OpenELIS database)  
**Testing**:

- Backend: JUnit 4 (4.13.1) + Mockito 2.21.0 (unit/integration)
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
- 6 REST API endpoint groups (hierarchy CRUD with Edit/Delete, assignment,
  movement, search, barcode validation, label management)
- 1 reusable UI widget (Storage Location Selector with two-tier design)
- 6 modal components (Consolidated Location Management, Dispose, Edit Location,
  Delete Location confirmation, Label Management)
- 2 overflow menu components (samples table row actions, location table row
  actions)
- 2 integration points (SamplePatientEntry, LogbookResults)
- Barcode workflow components (unified input field, debouncing, visual feedback,
  label printing)

**Development Approach**: Test-Driven Development (TDD)

- Write tests BEFORE implementation code
- Order: API contracts → FHIR validation tests → Integration tests → Unit tests
  → Implementation → E2E tests
- All tests must pass before moving to next component
- Target >70% coverage per OpenELIS constitution

## Constitution Check

Verify compliance with
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: Position naming free-text (no validation),
      capacity thresholds configurable
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (Tabs,
      DataTable with expandable rows, Modal, TextInput, Dropdown, OverflowMenu)
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
│   ├── UnifiedBarcodeInput.jsx - Unified input field supporting both barcode scan and type-ahead search
│   ├── BarcodeValidationService.js - Client-side barcode format validation and parsing
│   ├── BarcodeDebounceHook.js - Custom hook for 500ms debouncing logic
│   ├── BarcodeVisualFeedback.jsx - Visual feedback component (green checkmark, red X, ready state)
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
│   ├── LocationManagementModal.jsx - Consolidated modal for assignment and movement (replaces MoveSampleModal and ViewStorageModal)
│   ├── DisposeSampleModal.jsx - Dispose sample modal with reason/method/confirmation
│   ├── SampleActionsOverflowMenu.jsx - Overflow menu component for samples table rows (Manage Location, Dispose, View Audit)
│   ├── BulkMoveModal.jsx
│   └── index.js
├── LocationManagement/
│   ├── LocationActionsOverflowMenu.jsx - Overflow menu component for location table rows (Edit, Delete, Label Management)
│   ├── EditLocationModal.jsx - Modal for editing location entities (Room/Device/Shelf/Rack)
│   ├── DeleteLocationModal.jsx - Confirmation modal for deleting locations with constraint validation
│   ├── LabelManagementModal.jsx - Modal for short code management and label printing (Devices, Shelves, Racks)
│   ├── ShortCodeInput.jsx - Short code input with validation (max 10 chars, alphanumeric, auto-uppercase)
│   ├── PrintLabelButton.jsx - Print label button with PDF preview in new tab
│   ├── PrintHistoryDisplay.jsx - Read-only print history list component
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
   - Validates: Capacity calculation and warning thresholds (80/90/100%) -
     includes two-tier capacity logic (manual vs calculated), hierarchical
     capacity calculation, and "N/A" handling when capacity cannot be determined
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
- Validates: Unified barcode input field accepts both scan and type-ahead
- Validates: Barcode format parsing (2-5 level hierarchical paths with hyphen
  delimiter)
- Validates: Debouncing logic (500ms cooldown, duplicate detection, different
  barcode warning)
- Validates: Visual feedback (ready state, success green checkmark, error red X)
- Validates: "Last-modified wins" behavior when both dropdowns and input field
  are used
- Validates: Pre-filling valid components when scan fails partially
- Validates: Hierarchical path display
- Validates: Validation requires Room and Device selection (minimum 2 levels),
  Shelf/Rack/Position optional
- Validates: API error handling and user feedback
- Test: `SampleActionsOverflowMenu.test.jsx`
- Validates: Menu renders with all three items (Manage Location, Dispose, View
  Audit placeholder)
- Validates: "View Audit" is disabled
- Test: `LocationManagementModal.test.jsx` (consolidated from MoveSampleModal
  and ViewStorageModal)
- Validates: Modal renders with correct title/button based on location existence
- Validates: Comprehensive sample information section (ID, Type, Status, Date
  Collected, Patient ID, Test Orders)
- Validates: Current Location section only appears when location exists
- Validates: Reason for Move field appears only when moving (location exists AND
  different location selected)
- Validates: Location selection updates preview
- Validates: Validation prevents moving to same location
- Validates: Condition Notes field always visible
- Test: `DisposeSampleModal.test.jsx`
- Validates: Modal renders with warning alert, sample info, disposal form fields
- Validates: "Confirm Disposal" button disabled until checkbox checked
- Validates: Validation requires reason and method selection

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

## Phase 1: Setup & Database Schema

**Objective**: Initialize storage module structure and database foundation.

**Technical Approach**:

- Create storage module package structure in
  `src/main/java/org/openelisglobal/storage/` with subdirectories: valueholder/,
  dao/, service/, controller/, form/, fhir/
- Create frontend storage component directory structure in
  `frontend/src/components/storage/` with subdirectories:
  StorageLocationSelector/, SampleStorage/, hooks/
- Create Liquibase changesets for database schema:
  - `001-create-storage-hierarchy-tables.xml` - Room, Device, Shelf, Rack,
    Position tables with fhir_uuid columns
  - `002-create-assignment-tables.xml` - SampleStorageAssignment and
    SampleStorageMovement tables
  - `003-create-indexes.xml` - Performance indexes (parent lookups, FHIR UUID,
    occupancy queries)
- Add storage message keys to internationalization files (en.json, fr.json,
  sw.json)

**Dependencies**: None (foundational setup)

**Implementation Status**: [COMPLETE] - All setup tasks completed. See
`tasks.md` Phase 1 for details.

---

## Phase 2: Foundational - Core Entities & FHIR Transform

**Objective**: Create storage entities and FHIR mapping infrastructure required
by ALL user stories.

**Technical Approach**:

- Create Hibernate mappings for all 7 entities (Room, Device, Shelf, Rack,
  Position, SampleStorageAssignment, SampleStorageMovement)
- Create JPA entity classes extending BaseObject with fhir_uuid columns
- Implement StorageLocationFhirTransform service for FHIR Location resource
  mapping
- Add @PostPersist and @PostUpdate hooks to all storage entities for immediate
  FHIR sync
- Create ORM validation test to verify all mappings load correctly

**Architecture Decisions**:

- Use JPA annotations directly (NOT XML mappings) per Constitution IV.1
- All entities include fhir_uuid UUID column for FHIR resource mapping
- FHIR sync happens automatically via entity lifecycle hooks

**Dependencies**: Phase 1 (Setup) must complete first

**Implementation Status**: [COMPLETE] - All foundational entities and FHIR
transform service implemented. See `tasks.md` Phase 2 for details.

---

## Phase 3: Position Hierarchy Structure Update (2-5 Level Support)

**Objective**: Update StoragePosition entity structure to support flexible
hierarchy (2-5 levels). Positions can have parent_device_id (required),
parent_shelf_id (optional), parent_rack_id (optional), coordinate (optional).
Minimum requirement is device level (room + device); cannot be just a room.

**Technical Approach**:

- Update STORAGE_POSITION table via Liquibase changeset:
  - Add parent_device_id column (VARCHAR(36), NOT NULL, FK to storage_device)
  - Add parent_shelf_id column (VARCHAR(36), NULL, FK to storage_shelf)
  - Change parent_rack_id from NOT NULL to NULL (optional)
  - Make coordinate column NULL (optional, only for 5-level positions)
  - Add CHECK constraints for hierarchy integrity
- Update StoragePosition entity with new relationships
- Update buildHierarchicalPath() method to handle optional parents
- Update validateLocationActive() to traverse flexible hierarchy
- Update FHIR transform to support all hierarchy levels

**Architecture Decisions**:

- Minimum 2 levels (room + device) required per FR-033a
- Maximum 5 levels (room + device + shelf + rack + position coordinate)
- Hierarchy integrity enforced via CHECK constraints

**Dependencies**: Phase 2 (Foundational) must complete first. This phase must
complete before Phase 7 (US2B - Movement) as movement logic requires position
hierarchy validation.

**Implementation Status**: [COMPLETE] - Position hierarchy structure updated to
support 2-5 levels. See `tasks.md` Phase 3 for details.

---

## Phase 4: Flexible Assignment Architecture (Simplified Polymorphic Location)

**Objective**: Simplify sample assignment to use a single polymorphic location
relationship (`location_id` + `location_type`) instead of requiring
StoragePosition entities for all assignments. Allows assignment directly to
device/shelf/rack levels with optional text-based coordinate.

**Technical Approach**:

- Update SAMPLE_STORAGE_ASSIGNMENT table via Liquibase changeset:
  - Drop storage_position_id column entirely (no backward compatibility)
  - Add location_id column (numeric, NOT NULL, no FK - polymorphic reference)
  - Add location_type column (VARCHAR(20), NOT NULL, enum: 'device', 'shelf',
    'rack')
  - Add position_coordinate column (VARCHAR(50), nullable, optional text)
- Update SampleStorageAssignment entity with new fields
- Update service methods: assignSampleWithLocation(), moveSampleWithLocation()
- Update controller endpoints to accept locationId + locationType
- Update frontend components to extract locationId and locationType from
  selected hierarchy

**Architecture Decisions**:

- Position is represented as optional text field (positionCoordinate), not a
  separate entity reference
- Polymorphic location reference allows assignment to any hierarchy level
  (device/shelf/rack)
- No occupancy tracking at position level (position is just text coordinate)

**Dependencies**: Phase 2 (Foundational) and Phase 3 (Position Hierarchy) must
complete first. This phase must complete before Phase 5 (US1) and Phase 7 (US2B)
as it changes the core assignment architecture.

**Implementation Status**: [COMPLETE] - Flexible assignment architecture
implemented. See `tasks.md` Phase 4 for details.

---

## Phase 5: User Story 1 - Basic Storage Assignment (Priority: P1) 🎯 MVP

**Objective**: Reception clerks can assign sample items to storage locations
during sample entry using cascading dropdowns, type-ahead search, or barcode
scanning.

**Technical Approach**:

- Implement Storage Location CRUD operations (Room, Device, Shelf, Rack,
  Position)
- Create DAOs, Services, and Controllers for storage hierarchy management
- Implement SampleItem assignment service with flexible location support
  (location_id + location_type)
- Create Storage Location Selector widget with two-tier design:
  - Compact inline view (shows location path + expand button)
  - Expanded modal view (full assignment form with cascading dropdowns,
    type-ahead, barcode input)
- Integrate widget into SamplePatientEntry workflow
- Create Storage Dashboard with:
  - Metric cards (Total SampleItems, Active, Disposed, Storage Locations)
  - Tabs (SampleItems, Rooms, Devices, Shelves, Racks)
  - Tab-specific filters and search
  - Storage Locations metric card with color-coded breakdown

**Architecture Decisions**:

- Storage tracking operates at SampleItem level (physical specimens), not Sample
  level (orders)
- Dashboard displays SampleItem ID/External ID as primary identifier, with
  parent Sample accession number as secondary context
- Search matches either SampleItem ID/External ID or Sample accession number
- Widget supports three input modes: cascading dropdowns, type-ahead
  autocomplete, barcode scanning
- Two-tier widget design: compact view for inline display, modal for full
  assignment workflow

**Dependencies**: Phase 2 (Foundational), Phase 3 (Position Hierarchy), Phase 4
(Flexible Assignment) must complete first

**Implementation Status**: [IN PROGRESS] - Core assignment functionality
complete. Dashboard features (filters, search, metric card) in progress. See
`tasks.md` Phase 5 for details.

---

## Phase 6: User Story 2A - SampleItem Search and Retrieval (Priority: P2)

**Objective**: Lab technicians can search for sample items by SampleItem
ID/External ID or Sample accession number and retrieve storage location to
physically find sample items.

**Technical Approach**:

- Implement search service supporting both SampleItem ID/External ID and Sample
  accession number
- Create search endpoints: GET /rest/storage/sample-items/search?q={term}
- Create StorageLocationDisplay component showing hierarchical path
- Integrate QuickFindSearch component into LogbookResults workflow
- Add location search endpoint: GET /rest/storage/locations/search?q={term}

**Architecture Decisions**:

- Search uses OR logic: matches either SampleItem ID/External ID or Sample
  accession number
- Case-insensitive partial substring matching
- QuickFindSearch provides type-ahead autocomplete for location names/codes at
  any hierarchy level

**Dependencies**: Phase 2 (Foundational), Phase 5 (US1 - Assignment) for initial
assignment

**Implementation Status**: [NOT STARTED] - See `tasks.md` Phase 6 for details.

---

## Phase 7: User Story 2B - SampleItem Movement (Priority: P2)

**Objective**: Lab technicians can move sample items between storage locations
(single and bulk), with audit trail tracking previous/new locations.

**Technical Approach**:

- Implement moveSampleWithLocation() service method
- Implement bulkMoveSamples() service method with auto-assignment of sequential
  positions
- Create SampleMovementForm with fields: sampleItemId, locationId, locationType,
  positionCoordinate, reason
- Create movement endpoints: POST /rest/storage/sample-items/move, POST
  /rest/storage/sample-items/bulk-move
- Update audit trail (SampleStorageMovement) for all movements

**Architecture Decisions**:

- Movement uses same flexible assignment architecture (location_id +
  location_type)
- Previous location freed automatically, new location recorded
- Individual audit records created for each movement
- Bulk movement supports manual position override

**Dependencies**: Phase 2 (Foundational), Phase 3 (Position Hierarchy), Phase 4
(Flexible Assignment), Phase 5 (US1) for initial assignment

**Implementation Status**: [NOT STARTED] - See `tasks.md` Phase 7 for details.

---

## Phase 7.5: Modal Consolidation - Immediate Priority

**Objective**: Consolidate MoveSampleModal and ViewStorageModal into a single
LocationManagementModal that handles both assignment and movement workflows.

**Technical Approach**:

- Create LocationManagementModal component consolidating previous separate
  modals
- Update SampleActionsOverflowMenu to show "Manage Location" instead of separate
  "Move" and "View Storage" items
- Modal dynamically adapts based on whether sample item has existing location:
  - Assignment mode: "Assign Storage Location" title, "Assign" button
  - Movement mode: "Move Sample Item" title, "Confirm Move" button, shows
    "Reason for Move" field
- Display comprehensive sample item information (SampleItem ID/External ID,
  Sample ID, Type, Status, Date Collected, Patient ID, Test Orders)
- Current Location section only appears if location exists

**Architecture Decisions**:

- Single modal reduces UI complexity and maintenance burden
- Dynamic wording and conditional fields based on assignment vs movement mode
- Consolidation improves UX consistency

**Dependencies**: Phase 2 (Foundational). Can start immediately after
foundational entities and services are in place.

**Implementation Status**: [COMPLETE] - Modal consolidation complete. See
`tasks.md` Phase 7.5 for details.

---

## Phase 8: Location CRUD Operations Implementation

**Objective**: Implement full CRUD operations for location tabs (Rooms, Devices,
Shelves, Racks) with overflow menu actions (Edit, Delete) per FR-037f through
FR-037v. Each location entity can be edited via modal dialog and deleted with
validation constraints.

**Technical Approach**:

- Create LocationActionsOverflowMenu component for location table rows
- Create EditLocationModal component (generic, adapts to Room/Device/Shelf/Rack)
- Create DeleteLocationModal component with constraint validation
- Update backend service layer with constraint validation methods
- Update REST controllers with PUT and DELETE endpoints
- Code and Parent fields are read-only in Edit modal

**Architecture Decisions**:

- Edit modal allows editing all fields except Code and Parent (read-only to
  prevent structural changes)
- Delete validates constraints (child locations, active samples) before deletion
- Constraint validation returns user-friendly error messages

**Dependencies**: Phase 2 (Foundational), Phase 5 (US1 - Dashboard) for location
tables

**Implementation Status**: [COMPLETE] - Location CRUD operations implemented.
See `tasks.md` Phase 8 for details.

---

## Phase 9: Expandable Row Functionality Implementation

**Objective**: Add expandable row functionality to location tables (Rooms,
Devices, Shelves, Racks) in StorageDashboard component. Expanded rows display
additional entity fields not visible in table columns, formatted as key-value
pairs in read-only format.

**Technical Approach**:

- Add expandableRows prop to DataTable components for Rooms, Devices, Shelves,
  Racks tabs
- Import TableExpandHeader, TableExpandRow, TableExpandedRow from @carbon/react
- Add state management for expanded row ID (useState for expandedRowId)
- Implement handleRowExpand function to manage single-row expansion
- Create renderExpandedContent function for each location type (room, device,
  shelf, rack)
- Update table structure to use TableExpandHeader in header row
- Replace TableRow with TableExpandRow for data rows
- Add TableExpandedRow after each TableExpandRow with expanded content
- Display fields as key-value pairs with labels from React Intl
- Format dates using intl.formatDate()
- Display "N/A" for missing optional fields

**Architecture Decisions**:

- Only one row can be expanded at a time (expanding another automatically
  collapses the previous)
- Expanded content is read-only (Edit action remains in overflow menu)
- Expansion triggered by clicking chevron icon in dedicated first column (Carbon
  DataTable standard pattern)
- Tab switching resets expanded state

**Dependencies**: Phase 5 (Dashboard) and Phase 8 (Location CRUD) must complete
first

**Implementation Status**: [COMPLETE] - Expandable row functionality implemented
for all location tables. See `tasks.md` Phase 9 for details.

**Test Order** (TDD workflow):

1. **Unit Tests** (Jest + React Testing Library):

   - Test expanded state management (`handleRowExpand` function)
   - Test expanded content rendering for each location type
   - Test single-row expansion behavior (collapsing previous row)
   - Test missing field handling ("N/A" display)
   - Test date formatting in expanded content
   - Test tab switching resets expanded state

2. **E2E Tests** (Cypress):
   - Test expand row interaction (click chevron icon)
   - Test expanded content visibility and correctness
   - Test single-row expansion (expanding new row collapses previous)
   - Test expanded content is read-only (no edit capability)
   - Test expanded content for each location type (Rooms, Devices, Shelves,
     Racks)
   - Test keyboard navigation (Enter/Space to expand)
   - Test accessibility (ARIA attributes, screen reader support)

**Test Files**:

- Unit:
  `frontend/src/components/storage/StorageDashboard/StorageDashboard.test.jsx`
- E2E: `frontend/cypress/e2e/storageLocationExpandableRows.cy.js`

### Implementation Tasks

1. **Research & Design** ✅ (Complete - see research-expandable-rows.md)
2. **Unit Tests**: Write tests for expanded state management and content
   rendering
3. **Implementation**: Modify StorageDashboard.jsx to add expandable row
   functionality
4. **E2E Tests**: Write Cypress tests for expand/collapse interaction
5. **Accessibility Testing**: Verify ARIA attributes and keyboard navigation
6. **Integration Testing**: Test with real data from API

### Files to Modify

- `frontend/src/components/storage/StorageDashboard.jsx` - Add expandable row
  functionality, update occupancy display to handle "N/A" when capacity cannot
  be determined, add visual distinction for manual vs calculated capacities (per
  FR-062c)
- `frontend/src/components/storage/StorageDashboard/StorageDashboard.test.jsx` -
  Add unit tests for capacity calculation display, "N/A" handling, and capacity
  type badges
- `frontend/cypress/e2e/storageLocationExpandableRows.cy.js` - Add E2E tests
  (new file), add tests for capacity display (manual vs calculated, "N/A"
  tooltip)
- `frontend/src/languages/en.json` - Add message keys for expanded content
  labels, capacity type labels ("Manual Limit", "Calculated"), and "N/A" tooltip
  text
- `src/main/java/org/openelisglobal/storage/service/StorageLocationServiceImpl.java` -
  Add `calculateDeviceCapacity()` and `calculateShelfCapacity()` methods, update
  `getDevicesForAPI()` and `getShelvesForAPI()` to include `totalCapacity` and
  `capacityType`

### Dependencies

- **Carbon Design System v1.15**: `@carbon/react` with `TableExpandHeader`,
  `TableExpandRow`, `TableExpandedRow`
- **React Intl**: For internationalized field labels
- **Existing StorageDashboard**: Modify current table implementations

### Constitution Compliance

- ✅ **Carbon Design System First**: Uses Carbon DataTable expandable row
  pattern exclusively
- ✅ **Internationalization**: All field labels use React Intl message keys
- ✅ **Accessibility**: Carbon components provide ARIA attributes and keyboard
  navigation
- ✅ **Test Coverage**: Unit + E2E tests planned (>70% coverage goal)

### Success Criteria

- [ ] Expandable rows work for all location types (Rooms, Devices, Shelves,
      Racks)
- [ ] Only one row can be expanded at a time
- [ ] Expanded content displays all required fields as key-value pairs
- [ ] Expanded content is read-only (no edit capability)
- [ ] All unit tests pass
- [ ] All E2E tests pass
- [ ] Accessibility verified (ARIA attributes, keyboard navigation)
- [ ] Internationalization complete (all labels use React Intl)

**Implementation Status**: [COMPLETE] - Expandable row functionality implemented
for all location tables. See `tasks.md` Phase 9 for details.

---

## Phase 9.5: Capacity Calculation Logic Implementation

**Objective**: Implement two-tier capacity calculation system (per FR-062a,
FR-062b, FR-062c) for Devices and Shelves. Supports manual `capacity_limit`
(static) or calculated capacity from children. When `capacity_limit` is NULL,
calculate from child locations (sum if all children have defined capacities). If
any child lacks defined capacity, parent capacity cannot be determined and UI
displays "N/A" with tooltip.

**Technical Approach**:

- Implement `calculateDeviceCapacity()` method in StorageLocationServiceImpl:
  - If `capacity_limit` is set, return that value (manual capacity)
  - Otherwise, calculate from child shelves (sum if all shelves have defined
    capacities)
  - Return null if any child lacks defined capacity (capacity cannot be
    determined)
- Implement `calculateShelfCapacity()` method:
  - If `capacity_limit` is set, return that value
  - Otherwise, calculate from child racks (sum of rows × columns for all racks)
- Update API responses to include `totalCapacity` and `capacityType` fields:
  - `capacityType="manual"` when `capacity_limit` is set
  - `capacityType="calculated"` when calculated from children
  - `capacityType=null` when capacity cannot be determined
- Update frontend occupancy display:
  - Show "Manual Limit" badge for manual capacities
  - Show "Calculated" badge for calculated capacities
  - Show "N/A" with tooltip when capacity cannot be determined
  - Hide progress bar when capacity is undetermined

**Architecture Decisions**:

- Racks always use calculated capacity (rows × columns), never `capacity_limit`
- Capacity warnings (80%, 90%, 100%) apply to both manual and calculated
  capacities
- Visual distinction (badge/tooltip) clearly indicates capacity type to users

**Dependencies**: Phase 5 (Dashboard) and Phase 8 (Location CRUD) must complete
first. Can be implemented in parallel with Phase 10 (Barcode Workflow).

**Implementation Status**: [COMPLETE] - Two-tier capacity calculation system
implemented. See `tasks.md` Phase 9.5 for details.

---

## Phase 10: Barcode Workflow Implementation

**Note**: Research on existing OpenELIS barcode printing infrastructure
completed (see `research.md` Section 9). Integration strategy documented. Some
areas still need clarification during implementation (printer configuration,
scanner hardware details).

### Objective

Implement comprehensive barcode workflow functionality per FR-023 through
FR-027f:

- Unified input field supporting both barcode scanning and type-ahead search
- 5-step validation process (format, existence, hierarchy, activity, conflicts)
- Debouncing with 500ms cooldown period
- Visual feedback (ready state, success green checkmark, error red X)
- Dual barcode auto-detection (sample vs location barcodes)
- "Last-modified wins" logic when both dropdowns and input field are used
- Label management (short code, printing, print history)
- Error recovery with pre-filling valid components

### TDD Approach

Following strict test-first development with small, manageable iterations:

1. **Write failing tests** (Red phase)
2. **Implement minimal code to pass** (Green phase)
3. **Refactor while keeping tests green** (Refactor phase)

### Test-Driven Development Plan

#### Iteration 8.1: Backend Barcode Parsing and Validation

**Objective**: Implement server-side barcode parsing and 5-step validation
process.

**Test Order** (TDD workflow):

1. **Backend Unit Tests** (Write First):

   - Test: `BarcodeParsingServiceTest.java`
   - Validates: Parse 2-level barcode (Room-Device format)
   - Validates: Parse 3-level barcode (Room-Device-Shelf format)
   - Validates: Parse 4-level barcode (Room-Device-Shelf-Rack format)
   - Validates: Parse 5-level barcode (Room-Device-Shelf-Rack-Position format)
   - Validates: Fixed hyphen delimiter parsing
   - Validates: Invalid delimiter rejection
   - Validates: Empty/null barcode handling

2. **Backend Service Tests** (Write Second):

   - Test: `BarcodeValidationServiceTest.java`
   - Validates: Step 1 - Format validation (parseable structure)
   - Validates: Step 2 - Location existence check (all codes exist in database)
   - Validates: Step 3 - Hierarchy validation (Shelf is child of Device, etc.)
   - Validates: Step 4 - Activity check (location is active, not decommissioned)
   - Validates: Step 5 - Conflict check (position not occupied if applicable)
   - Validates: Error messages for each failure type
   - Validates: Partial validation (some components valid, some invalid)
   - Validates: Pre-fill valid components in response

3. **Backend Integration Tests** (Write Third):
   - Test: `BarcodeValidationRestControllerTest.java`
   - Validates: `POST /rest/storage/barcode/validate` endpoint
   - Validates: Request/response format matches API contract
   - Validates: Database persistence after validation
   - Validates: Error responses (400, 404) for validation failures

**Implementation Tasks** (After Tests Pass):

1. Create `BarcodeParsingService.java` - Parse hierarchical barcode format
2. Create `BarcodeValidationService.java` - Implement 5-step validation
3. Create `BarcodeValidationRestController.java` - REST endpoint for validation
4. Update API contract in `contracts/storage-api.json`

#### Iteration 8.2: Frontend Unified Input Field

**Objective**: Create unified input field component that accepts both barcode
scan and type-ahead search.

**Test Order** (TDD workflow):

1. **Frontend Unit Tests** (Write First):

   - Test: `UnifiedBarcodeInput.test.jsx`
   - Validates: Input field accepts keyboard input (manual typing)
   - Validates: Input field accepts rapid character input (barcode scan
     simulation)
   - Validates: Format-based detection (hyphens = barcode, no hyphens =
     type-ahead)
   - Validates: Enter key triggers validation
   - Validates: Field blur triggers validation
   - Validates: Visual feedback states (ready, success, error)
   - Validates: Auto-clear after successful population

2. **Frontend Integration Tests** (Write Second):
   - Test: `UnifiedBarcodeInput.integration.test.jsx`
   - Validates: API call to validation endpoint on Enter/blur
   - Validates: Success response populates location fields
   - Validates: Error response displays error message
   - Validates: Partial validation pre-fills valid components

**Implementation Tasks** (After Tests Pass):

1. Create `UnifiedBarcodeInput.jsx` - Unified input field component
2. Create `BarcodeVisualFeedback.jsx` - Visual feedback component
3. Integrate into `LocationSelectorModal.jsx`
4. Add React Intl message keys for barcode-related strings

#### Iteration 8.3: Debouncing Logic

**Objective**: Implement 500ms debouncing to prevent accidental double-scans.

**Test Order** (TDD workflow):

1. **Frontend Unit Tests** (Write First):
   - Test: `BarcodeDebounceHook.test.js`
   - Validates: Duplicate barcode within 500ms is ignored silently
   - Validates: Different barcode within 500ms shows warning and is ignored
   - Validates: Barcode after 500ms cooldown is processed normally
   - Validates: Cooldown timer resets after each scan
   - Validates: Multiple rapid scans handled correctly

**Implementation Tasks** (After Tests Pass):

1. Create `BarcodeDebounceHook.js` - Custom React hook for debouncing
2. Integrate into `UnifiedBarcodeInput.jsx`
3. Add warning message for different barcode within cooldown

#### Iteration 8.4: "Last-Modified Wins" Logic

**Objective**: Implement seamless switching between dropdown and input field
modes.

**Test Order** (TDD workflow):

1. **Frontend Unit Tests** (Write First):
   - Test: `LocationSelectorModal.test.jsx` (update existing)
   - Validates: Dropdown selection then input field scan overwrites dropdowns
   - Validates: Input field scan then dropdown selection overwrites input
   - Validates: Visual feedback shows which method is active (highlight
     border/icon)
   - Validates: No error when switching between methods
   - Validates: Both methods visible simultaneously

**Implementation Tasks** (After Tests Pass):

1. Update `LocationSelectorModal.jsx` to track last-modified method
2. Add visual feedback (highlight border/icon) for active method
3. Implement overwrite logic based on last modification timestamp

#### Iteration 8.5: Label Management (Short Code and Printing)

**Objective**: Implement label management modal with short code input and print
functionality.

**Test Order** (TDD workflow):

1. **Backend Unit Tests** (Write First):

   - Test: `ShortCodeValidationServiceTest.java`
   - Validates: Short code format (max 10 chars, alphanumeric, hyphen/underscore
     allowed)
   - Validates: Auto-uppercase conversion
   - Validates: Must start with letter or number (not hyphen/underscore)
   - Validates: Uniqueness within context (device/shelf/rack)
   - Validates: Warning when changing short code (affects printed labels)

2. **Backend Integration Tests** (Write Second):

   - Test: `LabelManagementRestControllerTest.java`
   - Validates: `PUT /rest/storage/{type}/{id}/short-code` endpoint
   - Validates: `POST /rest/storage/{type}/{id}/print-label` endpoint
   - Validates: Print history tracking (who, when, what)
   - Validates: PDF generation with system admin settings (label size, format,
     layout)

3. **Frontend Unit Tests** (Write Third):
   - Test: `LabelManagementModal.test.jsx`
   - Validates: Short code input with validation
   - Validates: Auto-uppercase on input
   - Validates: Warning dialog before short code change
   - Validates: Print label button opens PDF in new tab
   - Validates: Print history display (last printed, view history link)

**Implementation Tasks** (After Tests Pass):

1. Create `LabelManagementModal.jsx` - Label management modal
2. Create `ShortCodeInput.jsx` - Short code input with validation
3. Create `PrintLabelButton.jsx` - Print label button with PDF preview
4. Create `PrintHistoryDisplay.jsx` - Print history list component
5. Update `LocationActionsOverflowMenu.jsx` to include "Label Management" item
6. Create backend `ShortCodeValidationService.java`
7. Create backend `LabelManagementService.java` - Integrate with existing
   BarcodeLabelMaker (see research.md Section 9)
8. Create backend `StorageLocationLabel.java` - Extend Label class (see
   research.md Section 9)
9. Create backend `LabelManagementRestController.java` - REST endpoint for
   printing
10. Add database table for print history (Liquibase changeset)
11. Add `STORAGE_LOCATION_BARCODE_HEIGHT` and `STORAGE_LOCATION_BARCODE_WIDTH`
    to ConfigurationProperties (see research.md Section 9)

#### Iteration 8.6: E2E Tests

**Objective**: Validate complete barcode workflows end-to-end.

**Test Order** (TDD workflow):

1. **Cypress E2E Tests** (Write Last):
   - Test: `barcodeWorkflow.cy.js`
   - Validates: Scan 4-level barcode populates location fields correctly
   - Validates: Scan 2-level barcode (minimum) populates Room and Device only
   - Validates: Scan invalid barcode shows error message with parsed components
   - Validates: Debouncing prevents duplicate scans within 500ms
   - Validates: "Last-modified wins" when switching between dropdown and scan
   - Validates: Label management modal opens from overflow menu
   - Validates: Short code change shows warning dialog
   - Validates: Print label generates PDF and opens in new tab
   - Validates: Print history displays after printing

**Implementation Tasks** (After Tests Pass):

1. Create `frontend/cypress/e2e/barcodeWorkflow.cy.js`
2. Follow Constitution V.5 best practices:
   - Run tests individually during development
   - Browser console logging enabled and reviewed
   - Video recording disabled by default
   - Post-run review of console logs and screenshots

### Files to Create/Modify

**Backend**:

- `src/main/java/org/openelisglobal/storage/service/BarcodeParsingService.java`
  (new)
- `src/main/java/org/openelisglobal/storage/service/BarcodeValidationService.java`
  (new)
- `src/main/java/org/openelisglobal/storage/service/ShortCodeValidationService.java`
  (new)
- `src/main/java/org/openelisglobal/storage/service/LabelManagementService.java`
  (new)
- `src/main/java/org/openelisglobal/storage/controller/BarcodeValidationRestController.java`
  (new)
- `src/main/java/org/openelisglobal/storage/controller/LabelManagementRestController.java`
  (new)
- `src/test/java/org/openelisglobal/storage/service/BarcodeParsingServiceTest.java`
  (new)
- `src/test/java/org/openelisglobal/storage/service/BarcodeValidationServiceTest.java`
  (new)
- `src/test/java/org/openelisglobal/storage/service/ShortCodeValidationServiceTest.java`
  (new)
- `src/test/java/org/openelisglobal/storage/controller/BarcodeValidationRestControllerTest.java`
  (new)
- `src/test/java/org/openelisglobal/storage/controller/LabelManagementRestControllerTest.java`
  (new)
- `src/main/resources/liquibase/storage/004-create-print-history-table.xml`
  (new)

**Frontend**:

- `frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.jsx`
  (new)
- `frontend/src/components/storage/StorageLocationSelector/BarcodeValidationService.js`
  (new)
- `frontend/src/components/storage/StorageLocationSelector/BarcodeDebounceHook.js`
  (new)
- `frontend/src/components/storage/StorageLocationSelector/BarcodeVisualFeedback.jsx`
  (new)
- `frontend/src/components/storage/LocationManagement/LabelManagementModal.jsx`
  (new)
- `frontend/src/components/storage/LocationManagement/ShortCodeInput.jsx` (new)
- `frontend/src/components/storage/LocationManagement/PrintLabelButton.jsx`
  (new)
- `frontend/src/components/storage/LocationManagement/PrintHistoryDisplay.jsx`
  (new)
- `frontend/src/components/storage/StorageLocationSelector/UnifiedBarcodeInput.test.jsx`
  (new)
- `frontend/src/components/storage/LocationManagement/LabelManagementModal.test.jsx`
  (new)
- `frontend/src/components/storage/StorageLocationSelector/LocationSelectorModal.jsx`
  (modify)
- `frontend/src/components/storage/LocationManagement/LocationActionsOverflowMenu.jsx`
  (modify)
- `frontend/cypress/e2e/barcodeWorkflow.cy.js` (new)

**API Contracts**:

- `specs/001-sample-storage/contracts/storage-api.json` (update with barcode
  endpoints)

**Internationalization**:

- `frontend/src/languages/en.json` (add barcode-related message keys)
- `frontend/src/languages/fr.json` (add barcode-related message keys)
- `frontend/src/languages/sw.json` (add barcode-related message keys)

### Dependencies

- **Carbon Design System v1.15**: `@carbon/react` TextInput, Modal, Button
  components
- **React Intl**: For internationalized error messages and labels
- **Existing OpenELIS utilities**: `getFromOpenElisServer`,
  `postToOpenElisServer`
- **PDF Generation**: Reuse existing iTextPDF via BarcodeLabelMaker (see
  research.md Section 9)

### Constitution Compliance

- ✅ **Carbon Design System First**: Uses Carbon TextInput, Modal, Button
  exclusively
- ✅ **Internationalization**: All barcode-related strings use React Intl
  message keys
- ✅ **Layered Architecture**: Backend follows 5-layer pattern (Service →
  Controller)
- ✅ **Test Coverage**: Unit + integration + E2E tests planned (>70% coverage
  goal)
- ✅ **Schema Management**: Print history table via Liquibase changeset
- ✅ **Security & Compliance**: Input validation, audit trail for print history

### Success Criteria

- [ ] Unified input field accepts both barcode scan and type-ahead search
- [ ] 5-step validation process works correctly for all barcode formats (2-5
      levels)
- [ ] Debouncing prevents accidental double-scans (500ms cooldown)
- [ ] Visual feedback displays correctly (ready, success, error states)
- [ ] "Last-modified wins" logic works when switching between methods
- [ ] Label management modal accessible from overflow menu
- [ ] Short code validation and uniqueness checking works
- [ ] Print label generates PDF with system admin settings
- [ ] Print history tracks and displays correctly
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] All E2E tests pass
- [ ] Internationalization complete (en, fr, sw)

**Implementation Status**: [IN PROGRESS] - Backend barcode parsing and
validation complete (Iteration 9.1). Frontend unified input field (Iteration
9.2) and debouncing (Iteration 9.3) in progress. See `tasks.md` Phase 10 for
details.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Objective**: Final integration, optimization, and validation across all user
stories.

**Technical Approach**:

- Database indexes verification: Run EXPLAIN ANALYZE on common queries (sample
  search by location, hierarchical path lookups)
- Internationalization completeness audit: Verify all UI components use React
  Intl (no hardcoded English strings)
- Code formatting: Backend `mvn spotless:apply`, Frontend `npm run format`
- Test coverage report: JaCoCo for backend (>70%), Jest for frontend
- FHIR validation end-to-end: Query FHIR server, verify hierarchy complete,
  verify immediate sync working
- E2E test refactoring per Constitution V.5:
  - Update `cypress.config.js` (video: false, screenshotOnRunFailure: true)
  - Refactor tests to use intercept timing, retry-ability, element readiness
    checks
  - Remove arbitrary waits, use proper Cypress retry-ability
  - Run tests individually during development (not full suite)
  - Document post-run review process (console logs and screenshots)
- Documentation updates: Add missing details to quickstart.md based on
  implementation learnings

**Architecture Decisions**:

- E2E tests follow Constitution V.5 best practices (individual execution,
  console log review)
- Full test suite runs only in CI/CD pipeline
- Code formatting enforced via pre-commit hooks

**Dependencies**: Requires all feature phases (5, 6, 7, 8, 9, 9.5, 10) to
complete first.

**Implementation Status**: [NOT STARTED] - See `tasks.md` Phase 11 for details.

---

## Phase 12: Constitution Compliance Verification

**Objective**: Verify feature adheres to all applicable constitution principles
before deployment.

**Technical Approach**:

- Configuration-Driven: Verify no country-specific code branches, confirm
  position coordinates remain free-text
- Carbon Design System: Audit all UI components, confirm @carbon/react used
  exclusively (NO Bootstrap/Tailwind)
- FHIR/IHE Compliance: Validate FHIR Location resources against R4 profiles,
  verify IHE mCSD hierarchical queries work
- Layered Architecture: Code review storage module, verify 5-layer pattern
  followed (NO DAO calls from controllers, NO business logic in DAOs, NO
  class-level variables in controllers)
- Test Coverage: Run coverage reports, confirm >70% for new storage code
- Schema Management: Verify ALL database changes used Liquibase changesets (NO
  direct SQL)
- Internationalization: Grep for hardcoded strings, verify all use React Intl
- Security & Compliance: Verify audit trail (sys_user_id + lastupdated), verify
  input validation
- Cypress E2E Testing (Constitution V.5): Verify E2E tests follow Constitution
  V.5 requirements

**Verification Commands**:

```bash
# Backend: Code formatting + build + tests
mvn spotless:check && mvn clean install

# Frontend: Formatting + linting + E2E tests (run individually per Constitution V.5)
cd frontend && npm run format:check && npm run lint
npm run cy:run -- --spec "cypress/e2e/storageAssignment.cy.js"
npm run cy:run -- --spec "cypress/e2e/storageSearch.cy.js"
npm run cy:run -- --spec "cypress/e2e/storageMovement.cy.js"

# Coverage reports
mvn verify  # JaCoCo report in target/site/jacoco/
cd frontend && npm test -- --coverage  # Jest coverage
```

**Dependencies**: Phase 11 (Polish) must complete first. This is the FINAL phase
before deployment.

**Implementation Status**: [NOT STARTED] - See `tasks.md` Phase 12 for details.

---

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

**Capacity Determination Logic (per FR-062a, FR-062b)**:

- **Racks**: Capacity is ALWAYS calculated as `rows × columns` (per FR-017). If
  rows=0 OR columns=0, capacity=0 (no grid, rack-level assignment only)
- **Devices and Shelves**: Two-tier system:
  - If `capacity_limit` is set (static/manual limit), use that value as total
    capacity
  - If `capacity_limit` is NULL, calculate capacity from child locations:
    - If ALL child locations have defined capacities (either static
      `capacity_limit` set OR calculated capacity from their own children), sum
      those capacities
    - If ANY child location lacks a defined capacity, parent capacity cannot be
      determined (return null, UI shows "N/A")

```java
// In StorageLocationService
/**
 * Calculate total capacity for a device using two-tier logic (per FR-062a, FR-062b).
 * Returns null if capacity cannot be determined.
 */
public Integer calculateDeviceCapacity(StorageDevice device) {
    // Tier 1: Check if static capacity_limit is set
    if (device.getCapacityLimit() != null && device.getCapacityLimit() > 0) {
        return device.getCapacityLimit();
    }

    // Tier 2: Calculate from child shelves
    List<StorageShelf> shelves = storageShelfDAO.findByParentDeviceId(device.getId());
    if (shelves == null || shelves.isEmpty()) {
        return null; // No children, cannot determine capacity
    }

    int totalCapacity = 0;
    for (StorageShelf shelf : shelves) {
        Integer shelfCapacity = calculateShelfCapacity(shelf);
        if (shelfCapacity == null) {
            // Any child lacks defined capacity - cannot determine parent capacity
            return null;
        }
        totalCapacity += shelfCapacity;
    }

    return totalCapacity;
}

/**
 * Calculate total capacity for a shelf using two-tier logic (per FR-062a, FR-062b).
 * Returns null if capacity cannot be determined.
 */
public Integer calculateShelfCapacity(StorageShelf shelf) {
    // Tier 1: Check if static capacity_limit is set
    if (shelf.getCapacityLimit() != null && shelf.getCapacityLimit() > 0) {
        return shelf.getCapacityLimit();
    }

    // Tier 2: Calculate from child racks (racks always have defined capacity)
    List<StorageRack> racks = storageRackDAO.findByParentShelfId(shelf.getId());
    if (racks == null || racks.isEmpty()) {
        return null; // No children, cannot determine capacity
    }

    int totalCapacity = 0;
    for (StorageRack rack : racks) {
        // Racks always have defined capacity (rows × columns)
        int rackCapacity = rack.getRows() * rack.getColumns();
        totalCapacity += rackCapacity;
    }

    return totalCapacity;
}

/**
 * Calculate rack capacity (always rows × columns, per FR-017).
 */
public int calculateRackCapacity(StorageRack rack) {
    return rack.getRows() * rack.getColumns();
}

// In SampleStorageService
/**
 * Calculate capacity warning for a rack (per FR-036).
 * Racks always have defined capacity (rows × columns).
 */
public CapacityWarning calculateCapacity(StorageRack rack) {
    int totalCapacity = rack.getRows() * rack.getColumns();
    if (totalCapacity == 0) return null; // No grid

    int occupied = storageLocationService.countOccupied(rack.getId());
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

/**
 * Calculate capacity warning for a device or shelf (per FR-036).
 * Returns null if capacity cannot be determined (per FR-062b).
 */
public CapacityWarning calculateCapacity(StorageDevice device) {
    Integer totalCapacity = storageLocationService.calculateDeviceCapacity(device);
    if (totalCapacity == null) {
        return null; // Capacity cannot be determined - UI will show "N/A"
    }

    int occupied = storageLocationService.countOccupiedInDevice(device.getId());
    int percentage = (occupied * 100) / totalCapacity;

    // Only show warnings if capacity is defined (per FR-036)
    String warningMessage = null;
    if (percentage >= 100) {
        warningMessage = String.format("Device %s is %d%% full. Consider using alternative storage.",
            device.getName(), percentage);
    } else if (percentage >= 90) {
        warningMessage = String.format("Device %s is %d%% full. Consider using alternative storage.",
            device.getName(), percentage);
    } else if (percentage >= 80) {
        warningMessage = String.format("Device %s is %d%% full. Consider using alternative storage.",
            device.getName(), percentage);
    }

    return new CapacityWarning(occupied, totalCapacity, percentage, warningMessage);
}

public CapacityWarning calculateCapacity(StorageShelf shelf) {
    Integer totalCapacity = storageLocationService.calculateShelfCapacity(shelf);
    if (totalCapacity == null) {
        return null; // Capacity cannot be determined - UI will show "N/A"
    }

    int occupied = storageLocationService.countOccupiedInShelf(shelf.getId());
    int percentage = (occupied * 100) / totalCapacity;

    String warningMessage = null;
    if (percentage >= 100) {
        warningMessage = String.format("Shelf %s is %d%% full. Consider using alternative storage.",
            shelf.getLabel(), percentage);
    } else if (percentage >= 90) {
        warningMessage = String.format("Shelf %s is %d%% full. Consider using alternative storage.",
            shelf.getLabel(), percentage);
    } else if (percentage >= 80) {
        warningMessage = String.format("Shelf %s is %d%% full. Consider using alternative storage.",
            shelf.getLabel(), percentage);
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

### SampleItem Entity Integration

**Storage Granularity**: Storage tracking operates at the **SampleItem level**
(physical specimens), not at the Sample level (orders). Each SampleItem can be
stored independently, even when multiple SampleItems belong to the same parent
Sample.

**Existing SampleItem Entity**:
`org.openelisglobal.sampleitem.valueholder.SampleItem`

- **No modifications required** - SampleItem entity remains unchanged
- **Integration via junction table**: SampleStorageAssignment links SampleItem
  to storage location via polymorphic `location_id` + `location_type`
  (device/shelf/rack)
- **Foreign key**: `SampleStorageAssignment.sample_item_id` → `SampleItem.id`
- **Query pattern**:
  `JOIN sample_storage_assignment ON sample_item.id = sample_storage_assignment.sample_item_id`
- **Parent Sample context**: Dashboard and assignment workflows display
  SampleItem ID/External ID as primary identifier, with parent Sample accession
  number as secondary context for sorting/grouping

**FHIR Integration**:

- Each SampleItem's storage location maps to its corresponding FHIR Specimen
  resource `container` reference
- Specimen.container.extension[storage-location] contains Location reference for
  the SampleItem's current storage location

**Benefits**:

- ✅ No impact on existing SampleItem entity code
- ✅ Backward compatible (sample items without location continue to work)
- ✅ Supports independent storage of multiple SampleItems from same Sample
- ✅ Easy to query sample items by location (JOIN on assignment table)
- ✅ Easy to query location for a sample item (JOIN on assignment table)
- ✅ Dashboard and search support both SampleItem ID/External ID and Sample
  accession number

### Task 1.4: Generate Quickstart (Test-First Development Guide)
