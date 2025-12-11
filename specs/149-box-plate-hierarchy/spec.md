# Feature Specification: Box Storage Hierarchy Enhancement

**Feature Branch**: `spec/clarify/OGC-149-box-plate-hierarchy`  
**Created**: December 5, 2025  
**Updated**: December 9, 2025  
**Status**: Draft  
**Jira Ticket**: [OGC-149](https://uwdigi.atlassian.net/browse/OGC-149)  
**Parent Feature**: `001-sample-storage` (Sample Storage Management)  
**Figma Design**:
[Storage Management System](https://www.figma.com/make/11G8EahqJUgoP55pJy7ivz/Storage-Management-System)

## Executive Summary

This feature **enhances** the existing Sample Storage Management (Feature 001)
by adding a fifth persistent hierarchy level: **Box**. Currently, the storage
hierarchy ends at the Rack level. This enhancement introduces a new `StorageBox`
entity to represent physical containers (boxes, plates, trays) and moves the
grid structure definition (rows/columns) to this level.

**Architecture Change**: To optimize performance for high-volume labs, the
system shifts from a "Persistent Position Entity" model (where every empty slot
is a database row) to a "Virtual Position" model (where positions are
coordinates on the assignment).

**Current Hierarchy (Feature 001):** Room → Device → Shelf → Rack (with grid) →
Position (Entity)

**New Hierarchy (This Enhancement):** Room → Device → Shelf → Rack → **Box**
(with grid) → [Virtual Position]

**Key Changes:**

- **Rack simplification**: Remove grid-related fields (rows, columns,
  position_schema_hint) from Rack entity
- **New StorageBox entity**: Container with barcode, grid dimensions, and
  position schema
- **Virtual Positions**: Removal of `StoragePosition` entity table. Position is
  now a coordinate property of the assignment.
- **Standard dimension presets**: 6 common laboratory container formats (9x9,
  10x10, 8x12, 4x6, 6x8, 16x24)
- **Barcode format update**: Extended to 5-level hierarchy support
  (Room-Device-Shelf-Rack-Box) and 6-level with position coordinate
  (Room-Device-Shelf-Rack-Box-Position)
- **Flexible Assignment**: Samples can be assigned to any hierarchy level
  (device, shelf, rack, or box) with optional position coordinates

**Target Users**: Reception clerks, lab technicians, quality managers, lab
managers

**Expected Impact**:

- **Performance**: Removes overhead of maintaining millions of empty position
  rows
- **Accuracy**: Better representation of physical laboratory storage (multiple
  boxes per rack)
- **Flexibility**: Support for arbitrary text positions at any level (device,
  shelf, rack, box)
- **Granularity**: Improved sample tracking at the container level with
  grid-based boxes

**Dependency**: This feature depends on Feature 001 (Sample Storage Management)
infrastructure. It modifies existing entities, services, and UI components from
Feature 001.

**Migration Note**: Destructive migration - existing Rack grid data will be
dropped and recreated at the Box level. `StoragePosition` table will be dropped.

**Virtual Position Architecture**:

Positions are **not persistent database entities**. Instead:

- Position information is stored as `position_coordinate` (text) on
  `SampleStorageAssignment`
- Occupancy is calculated dynamically by counting assignments to a Box with
  specific coordinates
- This eliminates the need to pre-create thousands of empty position rows
- Supports high-volume labs without database bloat
- Flexible assignment: Samples can be assigned to device, shelf, rack, or box
  levels with optional position coordinates

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Configure Box within Rack (Priority: P1)

A lab technician needs to add a new 96-well plate to an existing rack in the
storage hierarchy. They access the storage dashboard, navigate to the target
rack, and create a new Box with the appropriate dimensions and barcode.

**Why this priority**: Core functionality - without Box entity creation, no
other features work. This is the foundation of the hierarchy enhancement.

**Independent Test**: Can be fully tested by creating a Box entity via dashboard
and verifying it appears in the hierarchy. Delivers value by enabling the new
5-level storage structure.

**Acceptance Scenarios**:

1. **Given** a rack exists in the storage hierarchy, **When** the user clicks
   "Add Box" and selects "96-well plate (8x12)" preset, **Then** a new Box is
   created with 8 rows and 12 columns with the selected position schema.

2. **Given** the Box creation modal is open, **When** the user enters custom
   dimensions (5 rows, 7 columns), **Then** the grid preview updates to show 35
   positions in a 5x7 layout.

3. **Given** a Box is being created, **When** the user enters a barcode
   identifier, **Then** the system validates uniqueness and generates the full
   hierarchical barcode (Room-Device-Shelf-Rack-Box).

4. **Given** a Box creation is submitted, **When** validation passes, **Then**
   the Box appears in the rack's children list and is visible in the dashboard
   Boxes tab.

---

### User Story 2 - Assign Sample to Storage Location (Flexible Levels) (Priority: P1)

A lab technician receives samples for storage and needs to assign them to
storage locations. The system supports flexible assignment to any hierarchy
level (device, shelf, rack, or box) with optional position coordinates.

**Why this priority**: Core workflow - sample assignment is the primary use
case. Must support all hierarchy levels for maximum flexibility.

**Independent Test**: Can be fully tested by assigning samples to different
hierarchy levels via the storage selector widget. Delivers value by enabling
flexible storage workflows.

**Acceptance Scenarios**:

1. **Given** a Box exists with available positions, **When** the user selects
   Room → Device → Shelf → Rack → Box → Position "A1" in the storage selector,
   **Then** the sample is assigned with `location_type='box'`,
   `location_id=<box_id>`, and `position_coordinate='A1'`.

2. **Given** a Rack exists (without boxes), **When** the user selects Room →
   Device → Shelf → Rack and optionally enters position "Top shelf", **Then**
   the sample is assigned with `location_type='rack'`, `location_id=<rack_id>`,
   and optional `position_coordinate='Top shelf'`.

3. **Given** a Shelf exists, **When** the user selects Room → Device → Shelf,
   **Then** the sample is assigned with `location_type='shelf'`,
   `location_id=<shelf_id>`, and no position coordinate required.

4. **Given** a Device exists, **When** the user selects Room → Device, **Then**
   the sample is assigned with `location_type='device'`,
   `location_id=<device_id>`, and no position coordinate required.

5. **Given** the storage selector is open, **When** the user scans a Box
   barcode, **Then** the selector auto-populates Room, Device, Shelf, Rack, and
   Box levels, showing available positions.

6. **Given** a Box has some occupied positions, **When** the user views the
   position selector, **Then** occupied positions are visually distinguished
   (grayed out) from available positions.

7. **Given** a user attempts to assign a sample to a Box position that's already
   occupied, **When** the assignment is submitted, **Then** the system prevents
   assignment and displays an error message.

---

### User Story 3 - Browse Storage Hierarchy with Box Level (Priority: P2)

A quality manager needs to view the storage hierarchy to audit sample locations.
They use the storage dashboard to drill down from Room to Device to Shelf to
Rack to Box to see position occupancy.

**Why this priority**: Important for compliance and audit workflows, but not
required for basic sample assignment.

**Independent Test**: Can be fully tested by navigating the dashboard hierarchy
from Room level down to Box level. Delivers value by enabling hierarchical
storage visibility.

**Acceptance Scenarios**:

1. **Given** the storage dashboard is displayed, **When** the user selects the
   "Boxes" tab, **Then** a table displays all Boxes with columns: Name, Code,
   Parent Rack, Dimensions (rows × columns), Capacity, Occupancy, Status.

2. **Given** the user is viewing the Boxes tab, **When** they click on a Box
   row, **Then** the view expands to show the grid layout with position
   occupancy visualization.

3. **Given** a rack exists in the hierarchy, **When** the user views the rack
   details, **Then** the rack no longer shows grid dimensions (those are now on
   Box).

---

## Requirements _(mandatory)_

### Functional Requirements

**Rack Simplification:**

- **FR-001**: System MUST remove rows, columns, and position_schema_hint fields
  from the Rack entity
- **FR-002**: System MUST update Rack UI forms to exclude grid-related input
  fields
- **FR-003**: Rack MUST retain only: label, code, status, parent_shelf_id,
  barcode identifier

**StorageBox Entity:**

- **FR-004**: System MUST create a new `StorageBox` entity with: id, label,
  code, rows (integer), columns (integer), position_schema_hint (optional),
  active (boolean), parent_rack_id, barcode_identifier, fhir_uuid
- **FR-005**: Box code MUST be unique within its parent Rack scope
- **FR-006**: Box dimensions (rows × columns) MUST be at least 1×1
- **FR-007**: Box capacity is calculated as `rows × columns` (computed, not
  stored)

**Standard Dimension Presets:**

- **FR-008**: System MUST provide 6 standard dimension presets:
  - 9×9 (81-position box)
  - 10×10 (100-position box)
  - 8×12 (96-well plate)
  - 4×6 (24-well plate)
  - 6×8 (48-well plate)
  - 16×24 (384-well plate)
- **FR-009**: Users MUST be able to enter custom dimensions in addition to
  presets

**Storage Selector Updates:**

- **FR-010**: Storage selector widget MUST support selection at any hierarchy
  level: Device, Shelf, Rack, or Box
- **FR-011**: When Box level is selected, Box dropdown MUST populate dynamically
  based on selected Rack
- **FR-012**: "Add New Box" option MUST be available inline in the selector
- **FR-013**: Scanning a Box barcode MUST auto-populate Room, Device, Shelf,
  Rack, and Box levels
- **FR-014**: Storage selector MUST allow optional `position_coordinate` input
  at any level
- **FR-015**: When Box is selected, position coordinate input MUST be shown with
  grid visualization

**Dashboard Updates:**

- **FR-016**: Storage Dashboard MUST include a new "Boxes" tab
- **FR-017**: Boxes tab MUST display: Name, Code, Parent Rack (full path),
  Dimensions, Capacity, Occupancy %, Status
- **FR-018**: Storage Locations metric card MUST include Box count in breakdown
- **FR-019**: Hierarchy drill-down MUST support: Room → Device → Shelf → Rack →
  Box → Virtual Positions (shown via assignments)

**Barcode Format:**

- **FR-020**: Box barcode format MUST follow:
  `{Room}-{Device}-{Shelf}-{Rack}-{Box}`
- **FR-021**: Full position barcode format MUST follow:
  `{Room}-{Device}-{Shelf}-{Rack}-{Box}-{PositionCoordinate}`
- **FR-022**: Barcode parsing MUST correctly handle both 5-level (Box) and
  6-level (Box + Position Coordinate) formats

**Barcode Parsing Strategy:**

- **FR-033**: Barcode parser MUST use generic left-to-right hierarchical
  validation:
  1. Split barcode by delimiter (hyphen `-`), validate each segment against
     database
  2. Stop at first unmatched segment, autofill all preceding valid levels
  3. Display contextual warning message for unmatched segment (e.g., "Device
     'INVALID' not found in Room 'LAB'")
- **FR-034**: Barcode parser MUST autofill all validated hierarchy levels up to
  the first unmatched segment
- **FR-035**: Barcode parser MUST display contextual warning message indicating
  which segment failed validation and why
- **FR-036**: Barcode parser MUST NOT implement special "legacy format"
  detection - all barcodes follow the same generic left-to-right parsing logic
  regardless of segment count

**Virtual Positioning (Architecture Change):**

- **FR-023**: The `StoragePosition` entity table MUST be removed to optimize
  performance. Positions are virtual coordinates stored on assignments.
- **FR-024**: Sample assignments MUST store position information as a
  `position_coordinate` text field on the `SampleStorageAssignment` table.
- **FR-025**: The system MUST calculate occupancy by counting assignments linked
  to a specific Box ID and coordinate, rather than querying child position
  entities.

**Sample Assignment Flexibility:**

- **FR-026**: System MUST support flexible assignment to any hierarchy level:
  - Device level: `location_type='device'`, `location_id=<device_id>`, optional
    `position_coordinate`
  - Shelf level: `location_type='shelf'`, `location_id=<shelf_id>`, optional
    `position_coordinate`
  - Rack level: `location_type='rack'`, `location_id=<rack_id>`, optional
    `position_coordinate`
  - Box level: `location_type='box'`, `location_id=<box_id>`, optional
    `position_coordinate=<slot>` (e.g., "A1", "1-1")
- **FR-027**: `position_coordinate` is an optional free-text field that can be
  used at any level:
  - At Box level: Represents grid slot (e.g., "A1", "B12", "1-5") and MUST be
    unique within the Box when provided. If omitted, system may use Box label as
    fallback.
  - At other levels: Optional note/label (e.g., "Top shelf", "Rack 3, Position
    5") with no uniqueness validation
- **FR-028**: System MUST validate that `position_coordinate` is unique within a
  Box when `location_type='box'` and `position_coordinate` is provided. When
  `position_coordinate` is missing at Box level, the system MAY use the Box
  label as a fallback coordinate.
- **FR-029**: `SampleStorageAssignment.location_type` MUST include `'box'` as a
  valid value (in addition to 'device', 'shelf', 'rack')
- **FR-030**: System MUST require minimum 2 hierarchy levels (Room + Device) for
  any assignment (per Feature 001 FR-033a)

### Constitution Compliance Requirements (OpenELIS Global 3.0)

- **CR-001**: UI components MUST use Carbon Design System (@carbon/react) - NO
  custom CSS frameworks
- **CR-002**: All UI strings MUST be internationalized via React Intl (no
  hardcoded text)
- **CR-003**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)
  - **Valueholders MUST use JPA/Hibernate annotations** (NO XML mapping files)
- **CR-004**: Database changes MUST use Liquibase changesets (NO direct DDL/DML)
- **CR-005**: External data integration MUST use FHIR R4 + IHE profiles
  - Box MUST map to FHIR Location resource with type 'container'
  - Grid dimensions MUST be represented via FHIR extensions
- **CR-006**: Box MUST map to FHIR Location with `physicalType.code` = "co"
  (container)
- **CR-007**: Box grid dimensions MUST use FHIR extensions:
  - `extension[storage-grid-rows].valueInteger` = rows
  - `extension[storage-grid-columns].valueInteger` = columns
  - `extension[storage-position-schema-hint].valueString` = position_schema_hint
    (optional)
- **CR-008**: Configuration-driven variation for country-specific requirements
  (NO code branching)
- **CR-009**: Security: RBAC, audit trail (sys_user_id + lastupdated), input
  validation
- **CR-010**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal)
- **CR-011**: Milestone-based development per Constitution Principle IX (each
  milestone = 1 PR)

### Key Entities

- **StorageRack (modified)**: Simplified container level

  - **Removed fields**: `rows`, `columns`, `position_schema_hint` (moved to Box)
  - **Retained fields**: id, fhir_uuid, label, code, active, parent_shelf_id
  - Now serves as a grouping container for Boxes
  - Relationship: One-to-Many with StorageBox

- **StorageBox (new)**: Physical container (box, plate, tray) with grid-based
  positions

  - **Fields**: id, fhir_uuid, label, code (unique within rack), rows, columns,
    position_schema_hint, active, parent_rack_id
  - **Capacity**: Calculated as rows × columns (computed, not stored)
  - **Relationships**: Many-to-One with StorageRack
  - **Leaf Node**: This is the lowest persistent entity in the hierarchy
  - **FHIR**: Maps to Location resource with physicalType "co" (container)

- **StoragePosition (REMOVED)**:

  - **Status**: Entity table removed from data model
  - **Replacement**: Virtual coordinates stored on
    `SampleStorageAssignment.position_coordinate`
  - **Rationale**: Eliminates need for millions of empty position rows in
    high-volume labs

- **SampleStorageAssignment (modified)**: Assignment linking sample items to
  storage locations with flexible hierarchy support
  - **location_type enum**: 'device', 'shelf', 'rack', **'box'** (all valid
    values)
  - **location_id**: References the ID of the selected entity
    (device/shelf/rack/box)
  - **position_coordinate**: Optional text field for position information
    - At Box level: Grid slot coordinate (e.g., "A1", "B12") - MUST be unique
      within Box
    - At other levels: Optional note/label (e.g., "Top shelf") - no uniqueness
      validation
  - **Flexible Assignment**: Supports assignment to any hierarchy level (device,
    shelf, rack, or box)
  - **Minimum Levels**: Requires at least Room + Device (2 levels) per Feature
    001 FR-033a

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Lab technicians can create a new Box with preset dimensions in
  under 30 seconds
- **SC-002**: Sample assignment to Box position completes in under 15 seconds
  (barcode scan workflow)
- **SC-003**: Storage dashboard displays Box tab with accurate
  capacity/occupancy metrics
- **SC-004**: 100% of existing Feature 001 E2E tests pass after hierarchy
  restructuring (with updates for new level)
- **SC-005**: Box barcode scanning correctly resolves full 5-level hierarchy
  path
- **SC-006**: FHIR Location resources correctly represent 5-level hierarchy with
  box type
- **SC-007**: All 6 standard dimension presets are available and correctly
  populate grid dimensions
- **SC-008**: Zero data integrity issues during destructive migration (Rack grid
  data removal)
- **SC-009**: Flexible assignment supports all hierarchy levels (device, shelf,
  rack, box) with optional position coordinates
- **SC-010**: Occupancy calculation for Boxes accurately reflects assigned
  positions without requiring position entities

## Edge Cases

- What happens when a user tries to create a Box with 0 rows or 0 columns? →
  Validation error: "Rows and columns must be at least 1"
- What happens when a user tries to assign a sample to a Box position that's
  already occupied? → Error message displayed, position selection prevented
- How does the system handle racks created before this enhancement (with grid
  data)? → Destructive migration: old grid data removed, manual re-creation at
  Box level required
- What happens when scanning a barcode with invalid middle segments (e.g., valid
  room but invalid device)? → System autofills valid room only, shows contextual
  warning: "Device '{code}' not found in Room '{room}'"
- What happens when scanning a 4-level barcode (Room-Device-Shelf-Rack)? →
  System validates all 4 levels, autofills up to first invalid segment, no
  special "legacy" handling required
- What happens when a Box is deactivated with samples still assigned? → Warning
  displayed, deactivation proceeds but samples remain tracked (flagged in
  dashboard)
- Can samples be assigned directly to Device level without Shelf/Rack/Box? →
  Yes, `location_type='device'` with optional `position_coordinate` (e.g., "Top
  drawer")
- Can samples be assigned directly to Rack level without Box? → Yes,
  `location_type='rack'` with optional `position_coordinate` (e.g., "Rack 3,
  Position 5")
- Can samples be assigned to Shelf level? → Yes, `location_type='shelf'` with
  optional `position_coordinate` (e.g., "Front section")
- What happens if `position_coordinate` is provided at Device/Shelf/Rack level?
  → Treated as optional note/label, no uniqueness validation required
- What happens if `position_coordinate` is missing at Box level? → System uses
  Box label as fallback coordinate (allows flexible assignment workflows)
- How is occupancy calculated for a Box? → Count of SampleStorageAssignment
  records where `location_type='box'` AND `location_id=<box_id>` AND
  `position_coordinate=<slot>` (e.g., "A1")
- What happens when a user attempts to assign a sample to a Box position that's
  already occupied? → System prevents assignment, displays error: "Position
  {coordinate} is already occupied by another sample"
