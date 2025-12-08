# Feature Specification: Box/Plate Storage Hierarchy Enhancement

**Feature Branch**: `149-box-plate-hierarchy`  
**Created**: December 5, 2025  
**Status**: Draft  
**Jira Ticket**: [OGC-149](https://uwdigi.atlassian.net/browse/OGC-149)  
**Parent Feature**: `001-sample-storage` (Sample Storage Management)  
**Figma Design**:
[Storage Management System](https://www.figma.com/make/11G8EahqJUgoP55pJy7ivz/Storage-Management-System)

## Executive Summary

This feature **enhances** the existing Sample Storage Management (Feature 001)
by adding a sixth hierarchy level: **Box/Plate**. Currently, the storage
hierarchy ends at the Rack level with grid-based positions. This enhancement
moves the grid functionality (rows, columns, position schema) from Rack to a new
Box/Plate entity, better reflecting real-world laboratory storage where racks
hold multiple physical boxes or plates, each with their own grid of positions.

**Current Hierarchy (Feature 001):** Room → Device → Shelf → Rack (with grid
dimensions) → Position

**New Hierarchy (This Enhancement):** Room → Device → Shelf → Rack →
**Box/Plate** (with grid dimensions) → Position

**Key Changes:**

- **Rack simplification**: Remove grid-related fields (rows, columns,
  position_schema_hint) from Rack entity
- **New Box/Plate entity**: Container with barcode, grid dimensions, and
  position schema
- **Standard dimension presets**: 6 common laboratory container formats (9x9,
  10x10, 8x12, 4x6, 6x8, 16x24)
- **Barcode format update**: Extended to 6-level hierarchy
  (Room-Device-Shelf-Rack-BoxPlate-Position)
- **UI enhancements**: Box/Plate selection level, configuration modal, dashboard
  tab

**Target Users**: Reception clerks, lab technicians, quality managers, lab
managers

**Expected Impact**:

- More accurate representation of physical laboratory storage organization
- Support for multiple boxes/plates per rack (common in freezer/refrigerator
  storage)
- Improved sample tracking granularity at the container level
- Box/plate-level barcode scanning for faster sample assignment

**Dependency**: This feature depends on Feature 001 (Sample Storage Management)
infrastructure. It modifies existing entities, services, and UI components from
Feature 001.

**Migration Note**: Since Feature 001 is not yet in production, this enhancement
will use destructive migration - existing Rack grid data will be dropped and
recreated at the Box/Plate level. No data migration is required.

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Configure Box/Plate within Rack (Priority: P1)

A lab technician needs to add a new 96-well plate to an existing rack in the
storage hierarchy. They access the storage dashboard, navigate to the target
rack, and create a new Box/Plate with the appropriate dimensions and barcode.

**Why this priority**: Core functionality - without Box/Plate entity creation,
no other features work. This is the foundation of the hierarchy enhancement.

**Independent Test**: Can be fully tested by creating a Box/Plate entity via
dashboard and verifying it appears in the hierarchy. Delivers value by enabling
the new 6-level storage structure.

**Acceptance Scenarios**:

1. **Given** a rack exists in the storage hierarchy, **When** the user clicks
   "Add Box/Plate" and selects "96-well plate (8x12)" preset, **Then** a new
   Box/Plate is created with 8 rows and 12 columns with the selected position
   schema.

2. **Given** the Box/Plate creation modal is open, **When** the user enters
   custom dimensions (5 rows, 7 columns), **Then** the grid preview updates to
   show 35 positions in a 5x7 layout.

3. **Given** a Box/Plate is being created, **When** the user enters a barcode
   identifier, **Then** the system validates uniqueness and generates the full
   hierarchical barcode (Room-Device-Shelf-Rack-BoxPlate).

4. **Given** a Box/Plate creation is submitted, **When** validation passes,
   **Then** the Box/Plate appears in the rack's children list and is visible in
   the dashboard Boxes/Plates tab.

---

### User Story 2 - Assign Sample to Box/Plate Position (Priority: P1)

A lab technician receives samples for storage and needs to assign them to
specific positions within a box/plate. They scan the box/plate barcode, select
an available position, and confirm the assignment.

**Why this priority**: Core workflow - sample assignment is the primary use
case. Must work with the new Box/Plate level.

**Independent Test**: Can be fully tested by assigning a sample to a Box/Plate
position via the storage selector widget. Delivers value by enabling sample
tracking at the Box/Plate level.

**Acceptance Scenarios**:

1. **Given** a Box/Plate exists with available positions, **When** the user
   selects Room → Device → Shelf → Rack → Box/Plate → Position in the storage
   selector, **Then** the sample is assigned to that position.

2. **Given** the storage selector is open, **When** the user scans a Box/Plate
   barcode, **Then** the selector auto-populates Room, Device, Shelf, Rack, and
   Box/Plate levels, showing available positions.

3. **Given** a Box/Plate has some occupied positions, **When** the user views
   the position selector, **Then** occupied positions are visually distinguished
   (grayed out) from available positions.

---

### User Story 3 - Browse Storage Hierarchy with Box/Plate Level (Priority: P2)

A quality manager needs to view the storage hierarchy to audit sample locations.
They use the storage dashboard to drill down from Room to Device to Shelf to
Rack to Box/Plate to see position occupancy.

**Why this priority**: Important for compliance and audit workflows, but not
required for basic sample assignment.

**Independent Test**: Can be fully tested by navigating the dashboard hierarchy
from Room level down to Box/Plate level. Delivers value by enabling hierarchical
storage visibility.

**Acceptance Scenarios**:

1. **Given** the storage dashboard is displayed, **When** the user selects the
   "Boxes/Plates" tab, **Then** a table displays all Box/Plates with columns:
   Name, Code, Parent Rack, Dimensions (rows × columns), Capacity, Occupancy,
   Status.

2. **Given** the user is viewing the Boxes/Plates tab, **When** they click on a
   Box/Plate row, **Then** the view expands to show the grid layout with
   position occupancy visualization.

3. **Given** a rack exists in the hierarchy, **When** the user views the rack
   details, **Then** the rack no longer shows grid dimensions (those are now on
   Box/Plate).

---

### User Story 4 - Select Standard Dimension Preset (Priority: P2)

A lab technician is creating a new Box/Plate and wants to quickly select a
common laboratory container format rather than manually entering dimensions.

**Why this priority**: Usability enhancement - makes common workflows faster but
not essential for functionality.

**Independent Test**: Can be fully tested by creating a Box/Plate using preset
buttons. Delivers value by reducing data entry errors and speeding up
configuration.

**Acceptance Scenarios**:

1. **Given** the Box/Plate creation modal is open, **When** the user clicks the
   "96-well plate (8×12)" preset button, **Then** rows=8 and columns=12 are
   populated and the grid preview updates.

2. **Given** the six standard presets are displayed, **When** the user clicks
   each preset in sequence, **Then** each correctly populates: 9×9 (81
   positions), 10×10 (100), 8×12 (96), 4×6 (24), 6×8 (48), 16×24 (384).

3. **Given** a preset was selected, **When** the user manually edits the rows or
   columns, **Then** the "Custom" option is auto-selected and preset buttons
   become deselected.

---

### User Story 5 - Update Barcode Format for 6-Level Hierarchy (Priority: P2)

A lab technician generates barcode labels for a new box/plate. The barcode
follows the 6-level hierarchy format and can be scanned to identify the full
storage path.

**Why this priority**: Enables barcode-based workflows at the Box/Plate level,
building on existing barcode infrastructure.

**Independent Test**: Can be fully tested by generating and scanning a Box/Plate
barcode. Delivers value by enabling efficient barcode-based sample management.

**Acceptance Scenarios**:

1. **Given** a Box/Plate is created under Rack "R1" under Shelf "S1" under
   Device "Freezer-A" under Room "Lab-001", **When** the barcode is generated,
   **Then** the format is "Lab-001-Freezer-A-S1-R1-{BoxPlateCode}".

2. **Given** a Box/Plate barcode is scanned, **When** the barcode is parsed,
   **Then** the system resolves all 5 ancestor levels (Room, Device, Shelf,
   Rack, Box/Plate).

3. **Given** a position within a Box/Plate is assigned, **When** the full
   barcode is generated, **Then** it includes 6 levels:
   "Room-Device-Shelf-Rack-BoxPlate-Position".

---

### Edge Cases

- What happens when a user tries to create a Box/Plate with 0 rows or 0 columns?
  → Validation error: "Rows and columns must be at least 1"
- What happens when a user tries to assign a sample to a Box/Plate position
  that's already occupied? → Error message displayed, position selection
  prevented
- How does the system handle racks created before this enhancement (with grid
  data)? → Destructive migration: old grid data removed, manual re-creation at
  Box/Plate level required
- What happens when scanning a barcode with invalid middle segments (e.g., valid
  room but invalid device)? → System autofills valid room only, shows contextual
  warning: "Device '{code}' not found in Room '{room}'"
- What happens when scanning a 4-level barcode (Room-Device-Shelf-Rack)? →
  System validates all 4 levels, autofills up to first invalid segment, no
  special "legacy" handling required
- What happens when a Box/Plate is deactivated with samples still assigned? →
  Warning displayed, deactivation proceeds but samples remain tracked (flagged
  in dashboard)
- Can positions still be assigned directly to Rack level (without Box/Plate)? →
  Yes, position hierarchy is flexible - `parent_box_plate_id` is optional,
  positions can be assigned at device (2 levels), shelf (3 levels), or rack (4
  levels) level
- What happens when grid coordinates are populated but `parent_box_plate_id` is
  NULL? → Validation warning (grid coordinates should only be used with
  Box/Plate parent)

## Requirements _(mandatory)_

### Functional Requirements

**Rack Simplification:**

- **FR-001**: System MUST remove rows, columns, and position_schema_hint fields
  from the Rack entity
- **FR-002**: System MUST update Rack UI forms to exclude grid-related input
  fields
- **FR-003**: Rack MUST retain only: name, code, status, parent_shelf_id,
  barcode identifier

**Box/Plate Entity:**

- **FR-004**: System MUST create a new Box/Plate entity with: id, name, code,
  rows (integer), columns (integer), position_schema_hint (optional), active
  (boolean), parent_rack_id, barcode_identifier, fhir_uuid
- **FR-005**: Box/Plate MUST have a one-to-many relationship with Position
  entities
- **FR-006**: Box/Plate code MUST be unique within its parent Rack scope
- **FR-007**: Box/Plate dimensions (rows × columns) MUST be at least 1×1

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

- **FR-010**: Storage selector widget MUST include Box/Plate as the 5th
  selection level (after Rack, before Position)
- **FR-011**: Box/Plate dropdown MUST populate dynamically based on selected
  Rack
- **FR-012**: "Add New Box/Plate" option MUST be available inline in the
  selector
- **FR-013**: Scanning a Box/Plate barcode MUST auto-populate Room, Device,
  Shelf, Rack, and Box/Plate levels

**Dashboard Updates:**

- **FR-014**: Storage Dashboard MUST include a new "Boxes/Plates" tab
- **FR-015**: Boxes/Plates tab MUST display: Name, Code, Parent Rack (full
  path), Dimensions, Capacity, Occupancy %, Status
- **FR-016**: Storage Locations metric card MUST include Box/Plate count in
  breakdown
- **FR-017**: Hierarchy drill-down MUST support: Room → Device → Shelf → Rack →
  Box/Plate → Positions

**Barcode Format:**

- **FR-018**: Box/Plate barcode format MUST follow:
  `{Room}-{Device}-{Shelf}-{Rack}-{BoxPlate}`
- **FR-019**: Full position barcode format MUST follow:
  `{Room}-{Device}-{Shelf}-{Rack}-{BoxPlate}-{Position}`
- **FR-020**: Barcode parsing MUST correctly handle both 5-level (Box/Plate) and
  6-level (Position) formats

**Barcode Parsing Strategy:**

- **FR-029**: Barcode parser MUST use generic left-to-right hierarchical
  validation:
  1. Split barcode by delimiter (hyphen `-`), validate each segment against
     database
  2. Stop at first unmatched segment, autofill all preceding valid levels
  3. Display contextual warning message for unmatched segment (e.g., "Device
     'INVALID' not found in Room 'LAB'")
- **FR-030**: Barcode parser MUST autofill all validated hierarchy levels up to
  the first unmatched segment
- **FR-031**: Barcode parser MUST display contextual warning message indicating
  which segment failed validation and why
- **FR-032**: Barcode parser MUST NOT implement special "legacy format"
  detection - all barcodes follow the same generic left-to-right parsing logic
  regardless of segment count

**Sample Assignment:**

- **FR-021**: Sample assignment MUST target Box/Plate positions (not Rack
  positions)
- **FR-022**: Position entity parent reference MUST be Box/Plate (not Rack)
- **FR-022a**: StoragePosition MUST add `parent_box_plate_id` column as nullable
  FK to `STORAGE_BOX_PLATE`
- **FR-022b**: StoragePosition hierarchy constraint: If `parent_box_plate_id` is
  set, then `parent_rack_id` MUST also be set
- **FR-022c**: Grid coordinates (`row_index`, `column_index`) SHOULD only be
  populated when `parent_box_plate_id` is set
- **FR-022d**: Positions without `parent_box_plate_id` remain valid (positions
  can be at device, shelf, or rack level)
- **FR-023**: SampleStorageAssignment.location_type MUST include 'box_plate' as
  valid value

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
  - Box/Plate MUST map to FHIR Location resource with type 'container'
  - Grid dimensions MUST be represented via FHIR extensions
- **FR-024**: Box/Plate MUST map to FHIR Location with `physicalType.code` =
  "co" (container)
- **FR-025**: Box/Plate grid dimensions MUST use FHIR extensions:
  - `extension[storage-grid-rows].valueInteger` = rows
  - `extension[storage-grid-columns].valueInteger` = columns
  - `extension[storage-position-schema-hint].valueString` = position_schema_hint
    (optional)
- **FR-026**: StorageRack MUST rename `label` field to `name` for consistency
  with other storage entities
- **FR-027**: StorageBoxPlate MUST use `name` field (not `label`)
- **FR-028**: All storage entity APIs MUST use `name` consistently across
  hierarchy levels
- **CR-006**: Configuration-driven variation for country-specific requirements
  (NO code branching)
- **CR-007**: Security: RBAC, audit trail (sys_user_id + lastupdated), input
  validation
- **CR-008**: Tests MUST be included (unit + integration + E2E, >70% coverage
  goal)
- **CR-009**: Milestone-based development per Constitution Principle IX (each
  milestone = 1 PR)

### Key Entities

- **StorageRack (modified)**: Simplified container level

  - **Removed fields**: `rows`, `columns`, `position_schema_hint` (moved to
    Box/Plate)
  - **Renamed field**: `label` → `name` (for consistency with Room, Device,
    Shelf)
  - **Retained fields**: id, fhir_uuid, name, code, active, parent_shelf_id
  - Now serves as a grouping container for Box/Plates
  - Relationship: One-to-Many with StorageBoxPlate

- **StorageBoxPlate (new)**: Physical container (box, plate, tray) with
  grid-based positions

  - **Fields**: id, fhir_uuid, name, code (unique within rack), rows, columns,
    position_schema_hint, active, parent_rack_id
  - **Capacity**: Calculated as rows × columns
  - **Relationships**: Many-to-One with StorageRack, One-to-Many with
    StoragePosition
  - **FHIR**: Maps to Location resource with physicalType "co" (container)

- **StoragePosition (modified)**: Flexible storage cell at any hierarchy level
  (2-6 levels)

  - **Added field**: `parent_box_plate_id` (nullable FK to StorageBoxPlate)
  - **Hierarchy constraint**: If `parent_box_plate_id` is set, then
    `parent_rack_id` MUST also be set
  - **Grid coordinates**: `row_index`, `column_index` used when
    `parent_box_plate_id` is set
  - **Flexibility maintained**: Positions can be assigned at device (2 levels),
    shelf (3 levels), rack (4 levels), or box/plate (5 levels) level
  - **Parent references**: parent_device_id (required), parent_shelf_id
    (optional), parent_rack_id (optional), parent_box_plate_id (optional)

- **SampleStorageAssignment (modified)**: Assignment linking sample items to
  storage locations
  - **location_type enum extended**: 'device', 'shelf', 'rack', **'box_plate'**
    (new)
  - Hierarchy resolution now includes Box/Plate level
  - Supports flexible assignment at any hierarchy level

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: Lab technicians can create a new Box/Plate with preset dimensions
  in under 30 seconds
- **SC-002**: Sample assignment to Box/Plate position completes in under 15
  seconds (barcode scan workflow)
- **SC-003**: Storage dashboard displays Box/Plate tab with accurate
  capacity/occupancy metrics
- **SC-004**: 100% of existing Feature 001 E2E tests pass after hierarchy
  restructuring (with updates for new level)
- **SC-005**: Box/Plate barcode scanning correctly resolves full 5-level
  hierarchy path
- **SC-006**: FHIR Location resources correctly represent 6-level hierarchy with
  box_plate type
- **SC-007**: All 6 standard dimension presets are available and correctly
  populate grid dimensions
- **SC-008**: Zero data integrity issues during destructive migration (Rack grid
  data removal)
