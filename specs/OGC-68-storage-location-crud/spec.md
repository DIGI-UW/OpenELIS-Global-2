# Feature Specification: Storage Location Management & Configuration

**Feature Branch**: `OGC-68-storage-location-crud`  
**Created**: December 11, 2025  
**Status**: Draft  
**Input**: User description: "Implement full CRUD capabilities for storage
locations (Room, Device, Shelf, Rack) in the Storage Dashboard. Include specific
updates to StorageDevice to support automated monitoring: add IP Address, Port,
and Communication Protocol (default: BACnet) fields. Ensure deletion logic
handles referential integrity (cannot delete locations with children or assigned
samples). UI must use Carbon Design System components (Modals/Forms) and be
fully internationalized."

## Executive Summary

This feature empowers lab managers to fully manage the storage hierarchy (Room →
Device → Shelf → Rack) directly from the Storage Dashboard. Currently, storage
locations can be viewed but not easily managed or configured. This feature adds
comprehensive CRUD (Create, Read, Update, Delete) capabilities for all hierarchy
levels.

Crucially, it prepares OpenELIS Global for automated device monitoring by adding
connectivity configuration (IP Address, Port, Communication Protocol) to storage
devices. This allows future integration with IoT monitoring systems.

Strict referential integrity rules ensure data safety: locations cannot be
deleted if they contain child locations or active samples. The UI will use
Carbon Design System patterns (Modals, Forms, Overflow Menus) and be fully
localized.

**Target Users**: Lab Managers, Quality Managers, System Administrators

**Expected Impact**:

- Enables self-service management of laboratory storage layout
- Prepares system for automated temperature monitoring integration
- Maintains data integrity through strict deletion validation rules

## Clarifications

### Session 2025-12-11

- Q: Should `StorageDevice` connectivity fields be mapped to FHIR `Location` or
  `Device` resource? → A: Map to FHIR `Location` resource (physicalType="ve")
  with custom extensions, consistent with existing architecture. New extensions:
  `device-ip-address`, `device-port`, `device-communication-protocol`.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Manage Storage Locations (Priority: P1)

A Lab Manager needs to reflect changes in the physical lab layout (e.g., adding
a new freezer, renaming a room, removing an old shelf) in the LIMS. They access
the Storage Dashboard to create, update, or delete location entities.

**Why this priority**: Core functionality required to maintain an accurate
digital twin of the laboratory storage.

**Independent Test**: Can be fully tested by performing CRUD operations on each
entity type (Room, Device, Shelf, Rack) and verifying persistence and validation
rules.

**Acceptance Scenarios**:

1. **Given** the Storage Dashboard "Rooms" tab, **When** the user clicks "Add
   Room" and submits valid details, **Then** a new Room is created and appears
   in the list.
2. **Given** a storage location (any type), **When** the user selects "Edit"
   from the row menu, **Then** a modal opens with current values pre-filled,
   allowing updates to editable fields (Name, Description, Status).
3. **Given** a storage location with NO children or assigned samples, **When**
   the user selects "Delete", **Then** the system requests confirmation and
   permanently removes the entity.
4. **Given** a storage location with children OR assigned samples, **When** the
   user selects "Delete", **Then** the system prevents deletion and displays a
   clear error message explaining the dependency.

---

### User Story 2 - Configure Device Connectivity (Priority: P2)

A System Administrator needs to configure network settings for a smart freezer
to enable future monitoring. They edit the device details to add IP address and
protocol information.

**Why this priority**: Enabler for future IoT integration (O150-68 specific
requirement).

**Independent Test**: Can be tested by creating/editing a Device entity and
verifying the new configuration fields are persisted and validated.

**Acceptance Scenarios**:

1. **Given** the "Add Device" or "Edit Device" modal, **When** the form is
   displayed, **Then** it includes fields for "IP Address", "Port", and
   "Communication Protocol".
2. **Given** the "Communication Protocol" field, **When** viewed, **Then** it
   defaults to "BACnet" but allows selection of other supported protocols (if
   any).
3. **Given** invalid input (e.g., invalid IP format), **When** submitting the
   form, **Then** the system displays a validation error and prevents saving.
4. **Given** a configured device, **When** viewed in the dashboard, **Then** the
   connectivity details are visible (or available in an expanded view).

---

## Requirements _(mandatory)_

### Functional Requirements

**CRUD Operations:**

- **FR-001**: System MUST provide "Add [Entity]" buttons on Rooms, Devices,
  Shelves, and Racks tabs.
- **FR-002**: System MUST provide "Edit" and "Delete" actions in the overflow
  menu for each table row.
- **FR-003**: "Add" and "Edit" actions MUST open a Carbon Modal with a Formik
  form for data entry.
- **FR-004**: System MUST validate uniqueness of Location Names/Codes within
  their parent scope (e.g., Device names unique within a Room).

**Device Configuration:**

- **FR-005**: `StorageDevice` entity MUST include fields: `ip_address` (String),
  `port` (Integer), `communication_protocol` (String).
- **FR-006**: `communication_protocol` MUST default to 'BACnet'.
- **FR-007**: System MUST validate `ip_address` format (IPv4/IPv6) if provided.
- **FR-008**: System MUST validate `port` is a valid integer (1-65535) if
  provided.

**Referential Integrity (Deletion Logic):**

- **FR-009**: System MUST prevent deletion of a Room if it contains Devices.
- **FR-010**: System MUST prevent deletion of a Device if it contains Shelves or
  Racks.
- **FR-011**: System MUST prevent deletion of a Shelf if it contains Racks.
- **FR-012**: System MUST prevent deletion of a Rack if it contains Boxes or
  direct sample assignments.
- **FR-013**: System MUST prevent deletion of any location that has active
  `SampleStorageAssignment` records linked to it.

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) -
  specific usage of `Modal`, `TextInput`, `Select`, `OverflowMenu`.
- **CR-002**: All UI strings MUST be internationalized via React Intl.
- **CR-003**: Backend MUST follow 5-layer architecture (`StorageDevice` updates
  in Valueholder -> DAO -> Service -> Controller -> Form).
- **CR-004**: Database changes MUST use Liquibase changesets (adding columns to
  `storage_device` table).
- **CR-005**: External data integration MUST use FHIR R4.
  - **CR-005a**: New device config fields MUST be mapped to FHIR `Location`
    resource extensions (consistent with existing architecture):
    - `http://openelis.org/fhir/extension/device-ip-address` (valueString)
    - `http://openelis.org/fhir/extension/device-port` (valueInteger)
    - `http://openelis.org/fhir/extension/device-communication-protocol`
      (valueString)
- **CR-007**: Security: RBAC (Manage Storage permission required), audit trail
  enabled for all CRUD actions.

### Key Entities

- **StorageDevice**:
  - Updates: Add `ip_address`, `port`, `communication_protocol`.
- **StorageRoom**: No schema changes, but enabled for CRUD.
- **StorageShelf**: No schema changes, but enabled for CRUD.
- **StorageRack**: No schema changes, but enabled for CRUD.

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Lab Manager can add a new complete hierarchy branch (Room ->
  Device -> Shelf -> Rack) in under 3 minutes.
- **SC-002**: System successfully blocks 100% of deletion attempts on locations
  with child entities or samples.
- **SC-003**: Device configuration data (IP/Port) is accurately persisted and
  retrievable via REST API.
- **SC-004**: All new UI elements pass Carbon Design System visual compliance
  check.

## Edge Cases

- **Duplicate Names**: What happens if a user names a device the same as an
  existing one in the same room? -> Validation error displayed.
- **Network Format**: What happens if a user enters a non-standard IP address?
  -> Field validation regex triggers error.
- **Partial Deletion**: What happens if a user tries to delete a Shelf with
  empty Racks? -> Blocked (Racks must be deleted first).
- **Concurrent Edits**: What happens if two users edit the same location? ->
  Last write wins (standard optimistic locking if versioning enabled, otherwise
  LWW).
