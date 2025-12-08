     1|# Feature Specification: Box/Plate Storage Hierarchy Enhancement
     2|
     3|**Feature Branch**: `fix/spec-alignment-ogc-149`  
     4|**Created**: December 5, 2025  
     5|**Updated**: December 9, 2025 (Aligned with `develop` implementation)
     6|**Status**: Draft  
     7|**Jira Ticket**: [OGC-149](https://uwdigi.atlassian.net/browse/OGC-149)  
     8|**Parent Feature**: `001-sample-storage` (Sample Storage Management)  
     9|**Figma Design**:
    10|[Storage Management System](https://www.figma.com/make/11G8EahqJUgoP55pJy7ivz/Storage-Management-System)
    11|
    12|## Executive Summary
    13|
    14|This feature **enhances** the existing Sample Storage Management (Feature 001)
    15|by adding a fifth hierarchy level: **Box**. Currently, the storage
    16|hierarchy ends at the Rack level. This enhancement introduces a new `StorageBox`
    17|entity to represent physical containers (boxes, plates, trays) and moves the
    18|grid structure definition (rows/columns) to this level.
    19|
    20|**Architecture Change**: To optimize performance for high-volume labs, the
    21|system shifts from a "Persistent Position Entity" model (where every empty slot
    22|is a database row) to a "Virtual Position" model (where positions are
    23|coordinates on the assignment).
    24|
    25|**Current Hierarchy (Feature 001):** Room → Device → Shelf → Rack (with grid) → Position (Entity)
    26|
    27|**New Hierarchy (This Enhancement):** Room → Device → Shelf → Rack → **Box** (with grid) → [Virtual Position]
    28|
    29|**Key Changes:**
    30|
    31|- **Rack simplification**: Remove grid-related fields (rows, columns,
    32|  position_schema_hint) from Rack entity
    33|- **New StorageBox entity**: Container with barcode, grid dimensions, and
    34|  position schema
    35|- **Virtual Positions**: Removal of `StoragePosition` entity table. Position is
    36|  now a coordinate property of the assignment.
    37|- **Standard dimension presets**: 6 common laboratory container formats (9x9,
    38|  10x10, 8x12, 4x6, 6x8, 16x24)
    39|- **Barcode format update**: Extended to 6-level hierarchy support
    40|  (Room-Device-Shelf-Rack-Box-Position)
    41|
    42|**Target Users**: Reception clerks, lab technicians, quality managers, lab
    43|managers
    44|
    45|**Expected Impact**:
    46|
    47|- **Performance**: Removes overhead of maintaining millions of empty position rows
    48|- **Accuracy**: Better representation of physical laboratory storage (multiple boxes per rack)
    49|- **Flexibility**: Support for arbitrary text positions at any level
    50|
    51|**Dependency**: This feature depends on Feature 001 (Sample Storage Management)
    52|infrastructure.
    53|
    54|**Migration Note**: Destructive migration - existing Rack grid data will be dropped and
    55|recreated at the Box level. `StoragePosition` table will be dropped.
    56|
    57|## User Scenarios & Testing _(mandatory)_
    58|
    59|### User Story 1 - Configure Box within Rack (Priority: P1)
    60|
    61|A lab technician needs to add a new 96-well plate to an existing rack in the
    62|storage hierarchy. They access the storage dashboard, navigate to the target
    63|rack, and create a new Box with the appropriate dimensions.
    64|
    65|**Acceptance Scenarios**:
    66|
    67|1. **Given** a rack exists, **When** user adds a "96-well plate", **Then** a new
    68|   Box is created with 8 rows and 12 columns.
    69|2. **Given** the Box creation modal, **When** user enters custom dimensions (5x7),
    70|   **Then** the grid preview updates.
    71|3. **Given** a Box is created, **When** submitted, **Then** it appears in the
    72|   rack's children list.
    73|
    74|---
    75|
    76|### User Story 2 - Assign Sample to Box Position (Priority: P1)
    77|
    78|A lab technician assigns samples to specific positions within a box.
    79|
    80|**Acceptance Scenarios**:
    81|
    82|1. **Given** a Box exists, **When** user assigns sample to "A1", **Then** the
    83|   assignment records the Box ID and "A1" coordinate.
    84|2. **Given** position A1 is occupied, **When** user attempts to assign another
    85|   sample to A1, **Then** system prevents assignment (unless duplicate allowed).
    86|
    87|---
    88|
    89|## Requirements _(mandatory)_
    90|
    91|### Functional Requirements
    92|
    93|**Rack Simplification:**
    94|
    95|- **FR-001**: System MUST remove rows, columns, and position_schema_hint fields
    96|  from the Rack entity
    97|- **FR-002**: Rack MUST retain only: name, code, status, parent_shelf_id,
    98|  barcode identifier
    99|
   100|**StorageBox Entity:**
   101|
   102|- **FR-004**: System MUST create a new `StorageBox` entity with: id, name, code,
   103|  rows (integer), columns (integer), position_schema_hint (optional), active
   104|  (boolean), parent_rack_id, barcode_identifier
   105|- **FR-005**: Box code MUST be unique within its parent Rack scope
   106|- **FR-006**: Box dimensions MUST be at least 1x1
   107|
   108|**Virtual Positioning (Architecture Change):**
   109|
   110|- **FR-021**: The `StoragePosition` entity table MUST be removed to optimize performance.
   111|- **FR-022**: Sample assignments MUST store position information as a `position_coordinate`
   112|  text field on the `SampleStorageAssignment` table.
   113|- **FR-023**: The system MUST calculate occupancy by counting assignments linked to
   114|  a specific Box ID, rather than querying child position entities.
   115|
   116|**Sample Assignment:**
   117|
   118|- **FR-024**: `SampleStorageAssignment.location_type` MUST include `'box'` as a valid value.
   119|- **FR-025**: Assignments to a Box MUST populate `location_id` with the Box ID and
   120|  `location_type` with "box".
   121|
   122|### Key Entities
   123|
   124|- **StorageRack (modified)**: Simplified container.
   125|  - **Removed**: `rows`, `columns`.
   126|  - **Relationship**: One-to-Many with `StorageBox`.
   127|
   128|- **StorageBox (new)**: The grid container.
   129|  - **Fields**: `name`, `code`, `rows`, `columns`, `parent_rack_id`.
   130|  - **Leaf Node**: This is the lowest persistent entity in the hierarchy.
   131|
   132|- **StoragePosition (DELETED)**:
   133|  - **Status**: Removed from data model.
   134|  - **Replacement**: Virtual coordinates on Assignments.
   135|
   136|- **SampleStorageAssignment (modified)**:
   137|  - **location_type**: Added `'box'`.
   138|  - **position_coordinate**: Stores the specific slot (e.g., "A1", "1-1") within the location.
   139|
   140|## Success Criteria
   141|
   142|- **SC-001**: Performance: Page load times for storage dashboard < 2s with 100k+ samples.
   143|- **SC-002**: Capability: Users can define 96-well plates and assign samples to specific wells.
   144|- **SC-003**: Migration: Database schema successfully transitions from Positions table to Box table.
   145|
