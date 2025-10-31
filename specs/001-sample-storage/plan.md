# Implementation Plan: Sample Storage Management

**Branch**: `001-sample-storage` | **Date**: 2025-10-30 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/001-sample-storage/spec.md`

## Summary

Implement POC for Sample Storage Management to track physical location of biological samples through a 5-level hierarchy (Room → Device → Shelf → Rack → Position). POC scope limited to core tracking workflows: assignment (P1), search/retrieval (P2A), and movement (P2B). Defers disposal workflow (P3) and dashboard monitoring (P4) to post-POC iterations.

**Technical Approach**: Leverage existing OpenELIS infrastructure (5-layer backend architecture, HAPI FHIR R4 server, Carbon Design System UI) to add storage location tracking. Create reusable Storage Location Selector widget with three input modes (cascading dropdowns, type-ahead autocomplete, barcode scanning). Integrate widget into existing sample entry and search workflows. Map storage entities to FHIR Location resources for external interoperability.

## Technical Context

**Language/Version**: Java 21 LTS (backend), React 17 (frontend)  
**Primary Dependencies**: 
- Backend: Spring Boot 3.x, Hibernate 6.x, HAPI FHIR R4 (v6.6.2), JPA
- Frontend: @carbon/react v1.15.0, React Intl 5.20.12, Formik 2.2.9, SWR 2.0.3

**Storage**: PostgreSQL 14+ (existing OpenELIS database)  
**Testing**: 
- Backend: JUnit 5 + Mockito (unit/integration)
- Frontend: Jest + React Testing Library (unit), Playwright (E2E - replacing Cypress per user specification)
- FHIR: Resource validation against R4 profiles

**Target Platform**: Web application (Linux server deployment, browser-based UI)  
**Project Type**: Web (backend + frontend integration)  
**Performance Goals**: Reasonable response times for POC (few seconds for searches/saves), no optimization required  
**Constraints**: 
- POC scope only (P1, P2A, P2B user stories)
- >70% test coverage per OpenELIS constitution
- FHIR R4 integration mandatory for storage entities

**Scale/Scope**: 
- 5 storage entity types (Room, Device, Shelf, Rack, Position)
- 3 REST API endpoint groups (hierarchy CRUD, assignment, movement)
- 1 reusable UI widget (Storage Location Selector)
- 2 integration points (SamplePatientEntry, LogbookResults)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Verify compliance with [OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md):

- [x] **Configuration-Driven**: Position naming free-text (no validation), capacity thresholds configurable
- [x] **Carbon Design System**: UI uses @carbon/react exclusively (Tabs, DataTable, Modal, TextInput, Dropdown, OverflowMenu)
- [x] **FHIR/IHE Compliance**: Storage entities map to FHIR Location resources, sample links via Specimen.container
- [x] **Layered Architecture**: Backend follows 5-layer pattern (StorageRoom valueholder → DAO → Service → Controller → Form)
- [x] **Test Coverage**: Unit + integration + Playwright E2E tests planned (>70% coverage goal per spec)
- [x] **Schema Management**: Liquibase changesets for 5 entity tables + junction tables
- [x] **Internationalization**: All UI strings use React Intl message keys (en, fr, sw minimum)
- [x] **Security & Compliance**: RBAC (Technicians/Managers/Admins), audit trail (sys_user_id, lastupdated), input validation

**Complexity Justification**: None required - plan fully compliant with constitution.

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
├── SampleStorage/
│   ├── AssignLocationModal.jsx
│   ├── MoveLocationModal.jsx
│   ├── BulkMoveModal.jsx
│   └── index.js
└── hooks/
    ├── useStorageLocations.js - SWR data fetching
    ├── useSampleStorage.js
    └── index.js

frontend/src/languages/
├── en.json - Add storage.* message keys
├── fr.json
└── sw.json

# Integration Points (modify existing files)
frontend/src/components/sample/SamplePatientEntry.jsx
frontend/src/components/logbook/LogbookResults.jsx

# E2E Tests (Playwright)
frontend/tests/e2e/storage/
├── assignment.spec.js
├── search.spec.js
├── movement.spec.js
└── bulk-movement.spec.js
```

**Structure Decision**: Follows existing OpenELIS monolithic repository structure with clear module separation. Backend uses standard 5-layer pattern in `org.openelisglobal.storage.*` package. Frontend components in `frontend/src/components/storage/` with reusable widget design. Integration points modify existing sample entry/search components to embed Storage Location Selector widget.

## Complexity Tracking

No complexity violations - plan fully compliant with OpenELIS Global 3.0 Constitution.

---

## Phase 0: Outline & Research

**Objective**: Validate technology choices, resolve unknowns, document best practices for OpenELIS patterns.

### Research Tasks

No NEEDS CLARIFICATION items in Technical Context - all technologies specified per constitution. Research focuses on validating existing OpenELIS patterns and best practices.

**Research Questions**:

1. **Hibernate XML Mapping Pattern**: How are existing OpenELIS entities mapped using Hibernate XML? (Examine existing .hbm.xml files for reference patterns)

2. **FHIR Location Resource Structure**: What is the correct FHIR R4 Location resource structure for hierarchical storage? (Validate against IHE mCSD profile requirements)

3. **Carbon Dropdown Cascading**: What is the best practice for implementing cascading dropdowns in Carbon Design System? (Check @carbon/react Dropdown component API)

4. **Barcode Scanner Integration**: How do USB HID barcode scanners emit keyboard input in browser context? (Research browser keyboard event handling for scan gun input)

5. **SWR Data Fetching Pattern**: How does existing OpenELIS frontend use SWR for API calls? (Examine existing hooks in `frontend/src/hooks/`)

6. **Playwright E2E Setup**: What is the OpenELIS Playwright configuration for E2E tests? (Note: User specified Playwright instead of Cypress - verify if OpenELIS already uses Playwright or needs migration)

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

## 6. Playwright E2E Configuration
- **Status**: [Check if OpenELIS uses Playwright or needs setup]
- **Configuration**: [playwright.config.js structure if needed]
- **Migration**: [If migrating from Cypress, document steps]
```

**Deliverable**: `specs/001-sample-storage/research.md` with all 6 questions answered

---

## Phase 1: Design & Contracts

**Prerequisites**: research.md complete

### Task 1.1: Generate Data Model

Create `data-model.md` documenting:

**Entities** (extract from spec.md Key Entities section):

1. **StorageRoom**
   - Fields: id (VARCHAR(36)), fhir_uuid (UUID), name (VARCHAR(255)), code (VARCHAR(50)), description (TEXT), active (BOOLEAN), sys_user_id (INT), lastupdated (TIMESTAMP)
   - Constraints: Unique (code), NOT NULL (name, code, active)
   - Relationships: One-to-Many with StorageDevice

2. **StorageDevice**
   - Fields: id, fhir_uuid, name, code, type (ENUM: freezer/refrigerator/cabinet/other), temperature_setting (DECIMAL), capacity_limit (INT), active, parent_room_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (code within parent_room_id), NOT NULL (name, code, type, parent_room_id)
   - Relationships: Many-to-One with StorageRoom, One-to-Many with StorageShelf

3. **StorageShelf**
   - Fields: id, fhir_uuid, label, capacity_limit (INT), active, parent_device_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (label within parent_device_id), NOT NULL (label, parent_device_id)
   - Relationships: Many-to-One with StorageDevice, One-to-Many with StorageRack

4. **StorageRack**
   - Fields: id, fhir_uuid, label, rows (INT), columns (INT), position_schema_hint (VARCHAR(50)), active, parent_shelf_id (FK), sys_user_id, lastupdated
   - Constraints: Unique (label within parent_shelf_id), NOT NULL (label, parent_shelf_id), CHECK (rows >= 0 AND columns >= 0)
   - Relationships: Many-to-One with StorageShelf, One-to-Many with StoragePosition
   - Calculated: capacity = rows * columns (or 0 if no grid)

5. **StoragePosition**
   - Fields: id, coordinate (VARCHAR(50)), row_index (INT), column_index (INT), occupied (BOOLEAN DEFAULT false), parent_rack_id (FK), sys_user_id, lastupdated
   - Constraints: NOT NULL (parent_rack_id), coordinate allows duplicates within same rack (flexible storage)
   - Relationships: Many-to-One with StorageRack, One-to-One with SampleStorageAssignment (current)
   - Note: fhir_uuid NOT needed (positions don't map to separate FHIR resources - encoded in parent Location)

6. **SampleStorageAssignment**
   - Fields: id, sample_id (FK to Sample), storage_position_id (FK), assigned_by_user_id (FK to SystemUser), assigned_date (TIMESTAMP), notes (TEXT)
   - Constraints: NOT NULL (sample_id, storage_position_id, assigned_by_user_id), Unique (sample_id) - one current location per sample
   - Relationships: Many-to-One with Sample, Many-to-One with StoragePosition, Many-to-One with SystemUser
   - Note: Represents CURRENT location. Historical moves tracked in SampleStorageMovement.

7. **SampleStorageMovement**
   - Fields: id, sample_id (FK), previous_position_id (FK), new_position_id (FK), moved_by_user_id (FK), movement_date (TIMESTAMP), reason (TEXT)
   - Constraints: NOT NULL (sample_id, moved_by_user_id, movement_date), previous_position_id OR new_position_id can be NULL (initial assignment or removal)
   - Relationships: Many-to-One with Sample, Many-to-One with StoragePosition (previous), Many-to-One with StoragePosition (new), Many-to-One with SystemUser
   - Note: Immutable audit log. Insertion only, no updates/deletes.

**Validation Rules** (from spec.md Functional Requirements):
- Prevent assignment to inactive location (FR-035)
- Prevent double-occupancy unless rack allows duplicates (FR-034)
- Capacity warnings at 80%, 90%, 100% (FR-036) - no hard block
- Position coordinate free text, max 50 chars (FR-010)
- Hierarchical barcode uniqueness (FR-004)

**State Transitions**:
- Sample: No location → Assigned → Moved (multiple times) → [Disposed - deferred to P3]
- Position: Empty (occupied=false) → Occupied (occupied=true) → Empty (on sample move/disposal)
- Location hierarchy: Active → Inactive (deactivation requires no active samples or warning)

### Task 1.2: Generate API Contracts

Create `/contracts/storage-api.json` (OpenAPI 3.0) with endpoints:

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
  - Response: `{ sample_id, type, status, location: { room, device, shelf, rack, position, hierarchical_path }, assigned_by, assigned_date }`
- `GET /rest/storage/samples?room={id}&device={id}&status={active}` - Filter samples by location/status

**Sample Movement**:
- `POST /rest/storage/samples/move` - Move sample to new position
  - Request: `{ sample_id, target_position_id, reason }`
  - Response: `{ movement_id, previous_location, new_location, moved_date }`
  - Validation: Target position not occupied, location active
- `POST /rest/storage/samples/bulk-move` - Bulk move samples
  - Request: `{ sample_ids: [], target_rack_id, position_assignments: [{sample_id, position_coordinate}] }`
  - Response: `{ movement_ids: [], summary: { total, successful, failed } }`

**Barcode Generation**:
- `POST /rest/storage/barcodes/generate` - Generate printable barcode labels
  - Request: `{ location_type: "device"|"shelf"|"rack", location_id }`
  - Response: `{ barcode_value, label_url (PDF/PNG) }`

### Task 1.3: Generate FHIR Mappings

Create `/contracts/fhir-mappings.md` documenting:

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

## Position → Not Separate FHIR Resource
- Positions encoded in Rack's Location.extension[available-positions]
- Individual position occupancy tracked in OpenELIS database only

## Sample-to-Location Link
- `Specimen.container.identifier.value` = full hierarchical path
- `Specimen.container.extension[storage-location].valueReference` = "Location/{rack_fhir_uuid}"
- `Specimen.extension[storage-position].valueString` = position coordinate
```

**IHE mCSD Compliance**:
- All Location resources queryable via `GET /fhir/Location?partOf={parent_id}`
- Hierarchical queries supported: `GET /fhir/Location?_include=Location:partOf`

### Task 1.4: Generate Quickstart

Create `quickstart.md` with developer setup:

```markdown
# Quickstart: Sample Storage Management POC

## Prerequisites
- OpenELIS Global 3.0 development environment running (see [dev_setup.md](../../../docs/dev_setup.md))
- PostgreSQL 14+ database accessible
- Java 21, Maven 3.8+, Node.js 16+

## Backend Setup

1. **Database Migration**
   ```bash
   # Liquibase changesets auto-run on application startup
   # Verify migration: psql -U clinlims -d clinlims -c "\dt storage_*"
   ```

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
   - Add storage.* message keys (see translations section in this doc)

3. **Run Frontend Dev Server**
   ```bash
   npm start
   # Access at https://localhost/
   ```

4. **Run Frontend Tests**
   ```bash
   # Unit tests
   npm test -- components/storage
   
   # E2E tests (Playwright)
   npx playwright test tests/e2e/storage
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
4. Test filters: by room, device, status

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
- **Barcode scanner not detected**: Verify USB HID scanner emitting keyboard events
```

### Task 1.5: Update Agent Context

Run agent context update script:

```bash
cd /Users/pmanko/code/OpenELIS-Global-2
.specify/scripts/bash/update-agent-context.sh cursor-agent
```

This script will:
- Detect Cursor AI agent context file
- Add new technology references from this plan:
  - Storage module package structure
  - FHIR Location resource mapping
  - Carbon Design System Storage Location Selector widget
  - Playwright E2E testing configuration (if new to OpenELIS)
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

Use `/speckit.tasks` command to generate detailed task breakdown from this plan. Tasks will be organized by user story (P1, P2A, P2B) with dependency ordering and parallel execution markers.

---

## Post-Phase 1 Constitution Re-Check

*Re-verify compliance after design artifacts generated:*

- [x] **Configuration-Driven**: Position coordinates remain free-text, no hardcoded validation
- [x] **Carbon Design System**: Storage Location Selector uses @carbon/react Dropdown, TextInput, Button components
- [x] **FHIR/IHE Compliance**: FHIR Location resources validated against R4 spec, IHE mCSD hierarchy supported
- [x] **Layered Architecture**: All 5 layers present (Valueholder → DAO → Service → Controller → Form)
- [x] **Test Coverage**: Unit/integration/E2E test structure defined in quickstart
- [x] **Schema Management**: Liquibase changesets planned in `liquibase/storage/`
- [x] **Internationalization**: Message keys documented for en/fr/sw in quickstart
- [x] **Security & Compliance**: RBAC enforced in controllers, audit fields in all entities

**Final Verdict**: ✅ Plan fully compliant with OpenELIS Global 3.0 Constitution

---

**Status**: Phase 0-1 Ready for Execution  
**Next Command**: Continue below to execute research and design phases
