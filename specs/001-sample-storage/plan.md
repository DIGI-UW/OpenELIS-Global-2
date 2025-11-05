# Implementation Plan: Sample Storage Management

**Branch**: `001-sample-storage` | **Date**: 2025-10-30 | **Spec**:
[spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-sample-storage/spec.md`

## Summary

Implement POC for Sample Storage Management to track physical location of
biological samples through a 5-level hierarchy (Room → Device → Shelf → Rack →
Position). POC scope includes core tracking workflows: assignment (P1),
search/retrieval (P2A), movement (P2B), and basic Storage Dashboard (P4 -
metrics cards, tabs, data tables). Defers disposal workflow (P3) and advanced
dashboard features (drill-down navigation, CSV export) to post-POC iterations.

**Technical Approach**: Leverage existing OpenELIS infrastructure (5-layer
backend architecture, HAPI FHIR R4 server, Carbon Design System UI) to add
storage location tracking. Create reusable Storage Location Selector widget with
three input modes (cascading dropdowns, type-ahead autocomplete, barcode
scanning). Integrate widget into existing sample entry and search workflows. Map
storage entities to FHIR Location resources for external interoperability.

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
- 1 reusable UI widget (Storage Location Selector)
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
- [x] **FHIR/IHE Compliance**: All 5 hierarchy levels (Room, Device, Shelf,
      Rack, Position) map to FHIR Location resources, sample links via
      Specimen.container
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
│   ├── StorageLocationSelector.jsx - Main reusable widget
│   ├── CascadingDropdownMode.jsx
│   ├── AutocompleteMode.jsx
│   ├── BarcodeScanMode.jsx
│   ├── StorageLocationSelector.test.jsx
│   └── index.js
├── StorageDashboard/
│   ├── StorageDashboard.jsx - Main dashboard component
│   ├── LocationFilterDropdown.jsx - Single location dropdown with autocomplete and tree view
│   ├── LocationTreeView.jsx - Hierarchical tree view component (expand/collapse)
│   ├── LocationAutocomplete.jsx - Autocomplete search component (flat list with full paths)
│   ├── LocationFilterDropdown.test.jsx
│   └── index.js
├── SampleStorage/
│   ├── AssignLocationModal.jsx
│   ├── MoveLocationModal.jsx
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
   - Validates: Assignment validation logic (prevent inactive location,
     double-occupancy)
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
   - Test: `StorageLocationSelector.test.jsx`, `CascadingDropdownMode.test.jsx`,
     etc.
   - Validates: Cascading dropdown state management
   - Validates: Barcode input parsing
   - Validates: Hierarchical path display
   - Validates: API error handling and user feedback

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

**Phase 4: E2E Tests** - Validate complete workflows

1. **Cypress E2E Tests** - Test user scenarios end-to-end
   - Test: `storageAssignment.cy.js` (P1 user story)
   - Test: `storageSearch.cy.js` (P2A user story)
   - Test: `storageMovement.cy.js` (P2B user story)
   - Test: `storageDashboardFilter.cy.js` (Dashboard Samples tab filtering)
     - Test single location dropdown with autocomplete search
     - Test hierarchical browsing (tree view expand/collapse)
     - Test filtering by Room, Device, Shelf, and Rack levels
     - Verify downward inclusive filtering (selecting device shows all samples in child shelves/racks/positions)
     - Test inactive location display (visual distinction)
     - Test combination of location filter and status filter
     - Verify Position-level locations excluded from dropdown (only Room/Device/Shelf/Rack)

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

   - Fields: id, fhir_uuid (UUID), coordinate (VARCHAR(50)), row_index (INT),
     column_index (INT), occupied (BOOLEAN DEFAULT false), parent_rack_id (FK),
     sys_user_id, lastupdated
   - Constraints: NOT NULL (parent_rack_id), UNIQUE (fhir_uuid), coordinate
     allows duplicates within same rack (flexible storage)
   - Relationships: Many-to-One with StorageRack, One-to-One with
     SampleStorageAssignment (current)
   - Note: Maps to FHIR Location resource (child of Rack Location) with
     occupancy extension

6. **SampleStorageAssignment**

   - Fields: id, sample_id (FK to Sample), storage_position_id (FK),
     assigned_by_user_id (FK to SystemUser), assigned_date (TIMESTAMP), notes
     (TEXT)
   - Constraints: NOT NULL (sample_id, storage_position_id,
     assigned_by_user_id), Unique (sample_id) - one current location per sample
   - Relationships: Many-to-One with Sample, Many-to-One with StoragePosition,
     Many-to-One with SystemUser
   - Note: Represents CURRENT location. Historical moves tracked in
     SampleStorageMovement.

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

- `POST /rest/storage/samples/assign` - Assign sample to position
  - Request: `{ sample_id, position_id, notes }`
  - Response: `{ assignment_id, hierarchical_path, assigned_date }`
  - Validation: Position not occupied, location active

**Sample Search**:

- `GET /rest/storage/samples/search?sample_id={id}` - Search by sample ID
  - Response:
    `{ sample_id, type, status, location: { room, device, shelf, rack, position, hierarchical_path }, assigned_by, assigned_date }`
- `GET /rest/storage/samples?location_id={id}&location_type={room|device|shelf|rack}&status={active}` - Filter
  samples by location (single location dropdown) and status
  - `location_id`: ID of selected location (Room, Device, Shelf, or Rack)
  - `location_type`: Hierarchy level of selected location (determines downward inclusive filtering)
  - Filter behavior: Returns all samples within selected location's hierarchy (downward inclusive)
  - Example: `GET /rest/storage/samples?location_id=123&location_type=device&status=active` returns all samples in device 123 and all its child shelves/racks/positions

**Sample Movement**:

- `POST /rest/storage/samples/move` - Move sample to new position
  - Request: `{ sample_id, target_position_id, reason }`
  - Response: `{ movement_id, previous_location, new_location, moved_date }`
  - Validation: Target position not occupied, location active
- `POST /rest/storage/samples/bulk-move` - Bulk move samples
  - Request:
    `{ sample_ids: [], target_rack_id, position_assignments: [{sample_id, position_coordinate}] }`
  - Response: `{ movement_ids: [], summary: { total, successful, failed } }`

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

- Maps to FHIR R4 `Location` resource (child of Rack Location)
- `Location.id` = StoragePosition.fhir_uuid
- `Location.name` = position coordinate
- `Location.identifier.value` = full hierarchical code (e.g.,
  "MAIN-FRZ01-SHA-RKR1-A5")
- `Location.partOf.reference` = "Location/{parent_rack_fhir_uuid}"
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

**SamplePatientEntry Integration**:

- **Integration Point**: Below "Collector" field in sample collection section
- **Widget Placement**: After collector dropdown, before sample collection time
- **Behavior**: Optional assignment (can be left blank and assigned later)
- **Component**: Embeds
  `<StorageLocationSelector mode="assign" optional={true} />`

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
   mvn clean install -DskipTests
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
   - Try cascading dropdown mode
   - Try type-ahead autocomplete
   - Try barcode scan (if scanner available)
4. Verify assignment saved with hierarchical path

### P2A: Sample Search/Retrieval

1. Navigate to Logbook Results
2. Search for sample ID
3. Verify location displays in expanded view
4. Navigate to Storage Dashboard, Samples tab
5. Test single location dropdown filter:
   - Open location dropdown
   - Test autocomplete search (type "Freezer" to find devices)
   - Test hierarchical browsing (expand/collapse tree view)
   - Select a Room → verify all samples in that room (downward inclusive)
   - Select a Device → verify all samples in that device and its children
   - Verify inactive locations are visually distinguished
   - Test combination: location filter + status filter

### P2B: Sample Movement

1. Find sample with assigned location
2. Click Actions → Move
3. Select new location
4. Enter reason
5. Confirm move
6. Verify audit trail updated

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
  - FHIR Location resource mapping (all 5 hierarchy levels including positions)
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
- [x] **FHIR/IHE Compliance**: All 5 hierarchy levels (Room, Device, Shelf,
      Rack, Position) map to FHIR Location resources, IHE mCSD hierarchy
      supported
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

## Implementation Enhancements

### Helper Methods (Service Layer)

**1. Hierarchical Path Builder**

```java
// In StorageLocationService
public String buildHierarchicalPath(StoragePosition position) {
    StringBuilder path = new StringBuilder();

    StorageRack rack = position.getParentRack();
    StorageShelf shelf = rack.getParentShelf();
    StorageDevice device = shelf.getParentDevice();
    StorageRoom room = device.getParentRoom();

    path.append(room.getName())
        .append(" > ")
        .append(device.getName())
        .append(" > ")
        .append(shelf.getLabel())
        .append(" > ")
        .append(rack.getLabel())
        .append(" > Position ")
        .append(position.getCoordinate());

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
public SampleStorageAssignment assignSample(String sampleId, String positionId, String notes)
        throws ConcurrentModificationException {
    try {
        StoragePosition position = positionDAO.get(positionId);

        // Optimistic locking check via Hibernate version field (lastupdated)
        if (position.isOccupied()) {
            throw new ValidationException("Position " + position.getCoordinate() +
                " is already occupied");
        }

        position.setOccupied(true);
        positionDAO.update(position); // Will throw StaleObjectStateException if concurrent update

        // Create assignment
        SampleStorageAssignment assignment = new SampleStorageAssignment();
        assignment.setSampleId(sampleId);
        assignment.setStoragePositionId(positionId);
        assignment.setAssignedByUserId(getCurrentUserId());
        assignment.setNotes(notes);

        assignmentDAO.insert(assignment);

        // FHIR sync happens automatically via @PostPersist hook on Position entity
        // (Position.occupied changed triggers FHIR Location update)

        return assignment;

    } catch (StaleObjectStateException e) {
        throw new ConcurrentModificationException(
            "Position was just modified by another user. Please refresh and try again.");
    }
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
    │                 │    { sampleId, positionId, notes }
    │                 ├──────────> SampleStorageRestController
    │                 │                 │
    │                 │                 │ 10. assignSample()
    │                 │                 ├──────────> SampleStorageService
    │                 │                 │                 │
    │                 │                 │                 │ 11. Validate location active
    │                 │                 │                 │ 12. Check position not occupied
    │                 │                 │                 │ 13. Calculate capacity warning
    │                 │                 │                 │
    │                 │                 │                 │ 14. Set position.occupied = true
    │                 │                 │                 ├──────────> StoragePositionDAO
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
