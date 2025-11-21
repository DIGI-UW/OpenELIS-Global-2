# Data Model: Sample Storage Management

**Date**: 2025-10-30  
**Feature**: Sample Storage Management POC  
**Branch**: 001-sample-storage

## Entity Relationship Diagram

```
StorageRoom (1) ──┬──> (N) StorageDevice
                  │
StorageDevice (1) ─┼──> (N) StorageShelf
                  │
StorageShelf (1) ──┼──> (N) StorageRack
                  │
                  │
Sample (1) ──> (N) SampleItem (one Sample can have multiple SampleItems)
                  │
                  │
SampleStorageAssignment (1) ──> (1) SampleItem (one current location per SampleItem)
  └──> Uses polymorphic location: location_id + location_type
       (references StorageDevice, StorageShelf, or StorageRack)
  └──> Optional position_coordinate (text field)

SampleStorageAssignment ──> (N) SampleStorageMovement (audit log)
  └──> Records all changes to SampleStorageAssignment
  └──> Captures previous_location_id + previous_location_type + previous_position_coordinate
  └──> Captures new_location_id + new_location_type + new_position_coordinate

Note: Flexible assignment model allows SampleItems to be assigned directly to:
- StorageDevice (minimum 2 levels: room + device)
- StorageShelf (3 levels: room + device + shelf)
- StorageRack (4 levels: room + device + shelf + rack)
- Optional position_coordinate (text field) can be used with any location_type

Storage Granularity: Storage tracking operates at the SampleItem level (physical specimens),
not at the Sample level (orders). Each SampleItem can be stored independently, even when
multiple SampleItems belong to the same parent Sample.
```

---

## 1. StorageRoom

**Purpose**: Top-level physical location entity representing laboratory rooms or
facility areas.

**Table**: `STORAGE_ROOM`

**Fields**:

| Field         | Type         | Constraints             | Description                                   |
| ------------- | ------------ | ----------------------- | --------------------------------------------- |
| `id`          | VARCHAR(36)  | PK, AUTO                | Primary key (StringSequenceGenerator)         |
| `fhir_uuid`   | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier             |
| `name`        | VARCHAR(255) | NOT NULL                | Human-readable room name                      |
| `code`        | VARCHAR(50)  | NOT NULL, UNIQUE        | Unique room code (e.g., "MAIN", "LAB-2")      |
| `description` | TEXT         | NULL                    | Optional room description                     |
| `active`      | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status                        |
| `sys_user_id` | INT          | NOT NULL                | User who created/modified (audit)             |
| `lastupdated` | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp (optimistic lock) |

**Constraints**:

- PRIMARY KEY (`id`)
- UNIQUE (`code`)
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

**Relationships**:

- One-to-Many with `StorageDevice` (parent)

**FHIR Mapping**:

- Maps to FHIR R4 `Location` resource
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` = `code`
- `Location.name` = `name`
- `Location.status` = `active` ? "active" : "inactive"
- `Location.physicalType.code` = "ro" (room)
- `Location.mode` = "instance"

**Validation Rules**:

- Code must be unique across all rooms
- Name cannot be empty
- Cannot delete room with active child devices (FK constraint)
- Cannot deactivate room with active samples assigned to child locations
  (business logic check)

---

## 2. StorageDevice

**Purpose**: Storage equipment (freezers, refrigerators, cabinets) within a
room.

**Table**: `STORAGE_DEVICE`

**Fields**:

| Field                 | Type         | Constraints             | Description                                   |
| --------------------- | ------------ | ----------------------- | --------------------------------------------- |
| `id`                  | VARCHAR(36)  | PK, AUTO                | Primary key                                   |
| `fhir_uuid`           | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier             |
| `name`                | VARCHAR(255) | NOT NULL                | Device name (e.g., "Freezer Unit 1")          |
| `code`                | VARCHAR(50)  | NOT NULL                | Device code (unique within parent room)       |
| `type`                | VARCHAR(20)  | NOT NULL                | Enum: freezer, refrigerator, cabinet, other   |
| `temperature_setting` | DECIMAL(5,2) | NULL                    | Optional temperature in Celsius               |
| `capacity_limit`      | INT          | NULL                    | Optional capacity limit (number of positions) |
| `active`              | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status                        |
| `parent_room_id`      | VARCHAR(36)  | NOT NULL, FK            | Parent room reference                         |
| `sys_user_id`         | INT          | NOT NULL                | User who created/modified                     |
| `lastupdated`         | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp                   |

**Constraints**:

- PRIMARY KEY (`id`)
- UNIQUE (`parent_room_id`, `code`) - Code unique within parent room
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_room_id`) REFERENCES `storage_room(id)` ON DELETE
  RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`
- CHECK (`type` IN ('freezer', 'refrigerator', 'cabinet', 'other'))

**Relationships**:

- Many-to-One with `StorageRoom` (parent)
- One-to-Many with `StorageShelf` (children)

**FHIR Mapping**:

- Maps to FHIR R4 `Location` resource
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` = "{room_code}-{device_code}" (hierarchical)
- `Location.name` = `name`
- `Location.type.coding.code` = `type`
- `Location.physicalType.code` = "ve" (vehicle/equipment)
- `Location.partOf.reference` = "Location/{parent_room_fhir_uuid}"

**Validation Rules**:

- Code must be unique within parent room (not globally unique)
- Type must be one of enumerated values
- Temperature setting (if provided) must be reasonable (-273.15 to 100 Celsius)
- Cannot delete device with active child shelves
- Cannot deactivate device with active samples in child locations

---

## 3. StorageShelf

**Purpose**: Storage shelf within a device.

**Table**: `STORAGE_SHELF`

**Fields**:

| Field              | Type         | Constraints             | Description                          |
| ------------------ | ------------ | ----------------------- | ------------------------------------ |
| `id`               | VARCHAR(36)  | PK, AUTO                | Primary key                          |
| `fhir_uuid`        | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier    |
| `label`            | VARCHAR(100) | NOT NULL                | Shelf label (e.g., "Shelf-A", "Top") |
| `capacity_limit`   | INT          | NULL                    | Optional capacity limit              |
| `active`           | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status               |
| `parent_device_id` | VARCHAR(36)  | NOT NULL, FK            | Parent device reference              |
| `sys_user_id`      | INT          | NOT NULL                | User who created/modified            |
| `lastupdated`      | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp          |

**Constraints**:

- PRIMARY KEY (`id`)
- UNIQUE (`parent_device_id`, `label`) - Label unique within parent device
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_device_id`) REFERENCES `storage_device(id)` ON DELETE
  RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

**Relationships**:

- Many-to-One with `StorageDevice` (parent)
- One-to-Many with `StorageRack` (children)

**FHIR Mapping**:

- Maps to FHIR R4 `Location` resource
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` = "{room_code}-{device_code}-{shelf_label}"
- `Location.name` = `label`
- `Location.physicalType.code` = "co" (container)
- `Location.partOf.reference` = "Location/{parent_device_fhir_uuid}"

**Validation Rules**:

- Label must be unique within parent device
- Cannot delete shelf with active child racks
- Cannot deactivate shelf with active samples in child locations

---

## 4. StorageRack

**Purpose**: Storage rack/tray on a shelf with optional grid structure.

**Table**: `STORAGE_RACK`

**Fields**:

| Field                  | Type         | Constraints             | Description                                           |
| ---------------------- | ------------ | ----------------------- | ----------------------------------------------------- |
| `id`                   | VARCHAR(36)  | PK, AUTO                | Primary key                                           |
| `fhir_uuid`            | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier                     |
| `label`                | VARCHAR(100) | NOT NULL                | Rack label (e.g., "Rack R1", "Tray-1")                |
| `rows`                 | INT          | NOT NULL, DEFAULT 0     | Grid rows (0 = no grid)                               |
| `columns`              | INT          | NOT NULL, DEFAULT 0     | Grid columns (0 = no grid)                            |
| `position_schema_hint` | VARCHAR(50)  | NULL                    | Optional hint for position naming (e.g., "A1", "1-1") |
| `active`               | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status                                |
| `parent_shelf_id`      | VARCHAR(36)  | NOT NULL, FK            | Parent shelf reference                                |
| `sys_user_id`          | INT          | NOT NULL                | User who created/modified                             |
| `lastupdated`          | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp                           |

**Constraints**:

- PRIMARY KEY (`id`)
- UNIQUE (`parent_shelf_id`, `label`) - Label unique within parent shelf
- UNIQUE (`fhir_uuid`)
- CHECK (`rows` >= 0 AND `columns` >= 0)
- FOREIGN KEY (`parent_shelf_id`) REFERENCES `storage_shelf(id)` ON DELETE
  RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

**Relationships**:

- Many-to-One with `StorageShelf` (parent)
- One-to-Many with `StoragePosition` (children)

**Calculated Fields**:

- `capacity` = `rows` \* `columns` (computed, not stored)
- If rows=0 OR columns=0, capacity=0 (no grid, rack-level assignment only)

**FHIR Mapping**:

- Maps to FHIR R4 `Location` resource
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` =
  "{room_code}-{device_code}-{shelf_label}-{rack_label}"
- `Location.name` = `label`
- `Location.physicalType.code` = "co" (container)
- `Location.partOf.reference` = "Location/{parent_shelf_fhir_uuid}"
- `Location.extension[grid-dimensions].valueString` = "{rows} × {columns}"

**Validation Rules**:

- Label must be unique within parent shelf
- Rows and columns must be non-negative integers
- Cannot delete rack with active child positions (occupied positions)
- Cannot deactivate rack with active samples in child positions
- Position schema hint is advisory only (not enforced)

---

## 5. StoragePosition

**Purpose**: Storage location representing the lowest level in the hierarchy for
a sample assignment. A position can have at most 5 levels (Room → Device → Shelf
→ Rack → Position) but at least 2 levels (Room → Device). The position
represents where in the hierarchy the sample is assigned. Minimum requirement is
device level (room + device); cannot be just a room. Position can be at: device
level (2 levels), shelf level (3 levels), rack level (4 levels), or position
level (5 levels). When assigning a sample, we select the lowest position in the
hierarchy, which provides all necessary location information.

**Table**: `STORAGE_POSITION`

**Fields**:

| Field              | Type        | Constraints             | Description                                                          |
| ------------------ | ----------- | ----------------------- | -------------------------------------------------------------------- |
| `id`               | VARCHAR(36) | PK, AUTO                | Primary key                                                          |
| `coordinate`       | VARCHAR(50) | NULL                    | Free-text position coordinate (optional, only for 5-level positions) |
| `row_index`        | INT         | NULL                    | Optional row number for grid visualization                           |
| `column_index`     | INT         | NULL                    | Optional column number for grid visualization                        |
| `occupied`         | BOOLEAN     | NOT NULL, DEFAULT false | Occupancy status                                                     |
| `parent_device_id` | VARCHAR(36) | NOT NULL, FK            | Parent device reference (required - minimum 2 levels: Room + Device) |
| `parent_shelf_id`  | VARCHAR(36) | NULL, FK                | Parent shelf reference (optional - for 3+ level positions)           |
| `parent_rack_id`   | VARCHAR(36) | NULL, FK                | Parent rack reference (optional - for 4+ level positions)            |
| `fhir_uuid`        | UUID        | NOT NULL, UNIQUE        | FHIR Location resource identifier                                    |
| `sys_user_id`      | INT         | NOT NULL                | User who created/modified                                            |
| `lastupdated`      | TIMESTAMP   | NOT NULL, DEFAULT NOW() | Last modification timestamp                                          |

**Note on `parent_device_id` requirement**: Every `StoragePosition` MUST have
`parent_device_id` because the minimum hierarchy is Room + Device (2 levels per
FR-033a). This ensures every position can be traced to at least a device,
providing a complete hierarchy path. The `parent_device_id` field is NOT NULL
and enforced by database constraint.

**Constraints**:

- PRIMARY KEY (`id`)
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_device_id`) REFERENCES `storage_device(id)` ON DELETE
  CASCADE
- FOREIGN KEY (`parent_shelf_id`) REFERENCES `storage_shelf(id)` ON DELETE
  CASCADE (if not NULL)
- FOREIGN KEY (`parent_rack_id`) REFERENCES `storage_rack(id)` ON DELETE CASCADE
  (if not NULL)
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`
- CHECK: If `parent_rack_id` is NOT NULL, then `parent_shelf_id` must also be
  NOT NULL
- CHECK: If `coordinate` is NOT NULL, then `parent_rack_id` must also be NOT
  NULL
- NOTE: Duplicate coordinates within same rack allowed (flexible storage, per
  FR-014)

**Relationships**:

- Many-to-One with `StorageDevice` (parent, required - minimum 2 levels)
- Many-to-One with `StorageShelf` (parent, optional - for 3+ level positions)
- Many-to-One with `StorageRack` (parent, optional - for 4+ level positions)
- One-to-One with `SampleStorageAssignment` (current assignment, if occupied)

**FHIR Mapping**:

- Maps to FHIR R4 `Location` resource (child of parent location)
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` = hierarchical code based on position level:
  - Device level: "{room_code}-{device_code}"
  - Shelf level: "{room_code}-{device_code}-{shelf_label}"
  - Rack level: "{room_code}-{device_code}-{shelf_label}-{rack_label}"
  - Position level:
    "{room_code}-{device_code}-{shelf_label}-{rack_label}-{coordinate}"
- `Location.name` = coordinate (if position level) or device/shelf/rack label
  (if lower level)
- `Location.physicalType.code` = "co" (container)
- `Location.partOf.reference` = "Location/{parent_fhir_uuid}" (parent device,
  shelf, or rack depending on level)
- `Location.extension[position-occupancy].valueBoolean` = `occupied`
- `Location.extension[position-grid-row].valueInteger` = `row_index` (if
  provided)
- `Location.extension[position-grid-column].valueInteger` = `column_index` (if
  provided)

**Validation Rules**:

- **Parent device is required (minimum 2 levels: room + device)**: The
  `parent_device_id` field is NOT NULL, ensuring every StoragePosition entity
  has at least a device parent. This enforces the minimum 2-level hierarchy
  requirement (Room + Device per FR-033a).
- If parent shelf is provided, parent device must exist
- If parent rack is provided, parent shelf must exist
- Coordinate is optional, only required for 5-level positions (when
  parent_rack_id is provided)
- Coordinate is free text, max 50 characters (per FR-010)
- Disallow control characters (tabs, newlines) in coordinate
- Duplicate coordinates within same rack allowed (per FR-014 - flexible storage
  scenarios)
- Row_index and column_index are optional, used only for grid visualization
- Cannot delete position if occupied (occupied=true)

**Terminology Clarification**:

- **StoragePosition entity**: The database entity with `parent_device_id`,
  `parent_shelf_id`, `parent_rack_id`, and `coordinate` fields. Represents a
  physical storage position in the hierarchy.
- **Position coordinate**: The text field (`position_coordinate` in
  `SampleStorageAssignment`). A free-text identifier for a specific position
  within a location (e.g., "A1", "Top shelf", "Rack 3, Position 5").

---

## 6. SampleStorageAssignment

**Purpose**: Current storage location assignment for a SampleItem (physical
specimen). One-to-one relationship (one SampleItem, one current location).
Supports flexible assignment to any hierarchy level (device, shelf, or rack)
with optional text-based position coordinate. Position is represented as a text
field (`position_coordinate`), not a separate entity reference.

**Storage Granularity**: Storage tracking operates at the **SampleItem level**
(physical specimens), not at the Sample level (orders). Each SampleItem can be
stored independently, even when multiple SampleItems belong to the same parent
Sample.

**Table**: `SAMPLE_STORAGE_ASSIGNMENT`

**Fields**:

| Field                 | Type        | Constraints             | Description                                                                  |
| --------------------- | ----------- | ----------------------- | ---------------------------------------------------------------------------- |
| `id`                  | VARCHAR(36) | PK, AUTO                | Primary key                                                                  |
| `sample_item_id`      | VARCHAR(36) | NOT NULL, UNIQUE        | SampleItem reference (one current location per SampleItem)                   |
| `location_id`         | NUMERIC(10) | NOT NULL                | Polymorphic location ID (references device, shelf, or rack)                  |
| `location_type`       | VARCHAR(20) | NOT NULL                | Type discriminator: 'device', 'shelf', or 'rack'                             |
| `position_coordinate` | VARCHAR(50) | NULL                    | Optional text-based position coordinate (can be used with any location_type) |
| `assigned_by_user_id` | INT         | NOT NULL, FK            | User who assigned                                                            |
| `assigned_date`       | TIMESTAMP   | NOT NULL, DEFAULT NOW() | Assignment timestamp                                                         |
| `notes`               | TEXT        | NULL                    | Optional assignment notes                                                    |

**Constraints**:

- PRIMARY KEY (`id`)
- UNIQUE (`sample_item_id`) - Enforces one current location per SampleItem
- FOREIGN KEY (`sample_item_id`) REFERENCES `sample_item(id)` ON DELETE CASCADE
- FOREIGN KEY (`assigned_by_user_id`) REFERENCES `system_user(id)`
- CHECK (`location_type IN ('device', 'shelf', 'rack')`) - Valid location type
  enum (position is just text coordinate, not entity)
- NOT NULL (`location_id`, `location_type`) - Both required for polymorphic
  location reference

**Relationships**:

- Many-to-One with `SampleItem` (one SampleItem, one current assignment)
- Polymorphic relationship to `StorageDevice`, `StorageShelf`, or `StorageRack`
  via `location_id` + `location_type`
- Many-to-One with `SystemUser` (assigned by user)

**Business Logic**:

- **Assignment using `location_id` + `location_type`**:
  - No occupancy tracking (assignment at hierarchy level, not specific
    StoragePosition entity)
  - Optional `position_coordinate` provides text-based position information
    (e.g., "A1", "Top shelf", "Rack 3, Position 5")
  - Create entry in `SampleStorageMovement` audit log
- **On UPDATE (SampleItem moved)**: Update location reference, create audit log
  entry
- **On DELETE (SampleItem disposed)**: Create audit log entry with
  `new_location_id = NULL`, `new_location_type = NULL`,
  `new_position_coordinate = NULL`

**Validation Rules**:

- `location_id` and `location_type` are required (NOT NULL)
- `location_type` must be one of: 'device', 'shelf', or 'rack' (enforced by
  CHECK constraint)
- If `location_type = 'device'`: `location_id` must reference a valid
  `StorageDevice` (minimum 2 levels: room + device per FR-033a)
- If `location_type = 'shelf'`: `location_id` must reference a valid
  `StorageShelf` (3 levels: room + device + shelf)
- If `location_type = 'rack'`: `location_id` must reference a valid
  `StorageRack` (4 levels: room + device + shelf + rack)
- Cannot assign SampleItem to inactive storage location (check entire hierarchy:
  room, device, shelf, rack)
- SampleItem can have only one current assignment (enforced by UNIQUE constraint
  on sample_item_id)
- `position_coordinate` is optional text (max 50 chars per FR-010), can be used
  with any `location_type` to provide specific position information

---

## 7. SampleStorageMovement

**Purpose**: Immutable audit log of SampleItem storage movements. Records all
location changes for compliance. This table serves as a complete audit trail of
all changes to `SampleStorageAssignment`, capturing both the previous and new
location states for each movement event.

**Table**: `SAMPLE_STORAGE_MOVEMENT`

**Fields**:

| Field                          | Type        | Constraints             | Description                                                |
| ------------------------------ | ----------- | ----------------------- | ---------------------------------------------------------- |
| `id`                           | VARCHAR(36) | PK, AUTO                | Primary key                                                |
| `sample_item_id`               | VARCHAR(36) | NOT NULL, FK            | SampleItem reference                                       |
| `previous_location_id`         | NUMERIC(10) | NULL                    | Previous location ID (polymorphic: device, shelf, or rack) |
| `previous_location_type`       | VARCHAR(20) | NULL                    | Previous location type: 'device', 'shelf', or 'rack'       |
| `previous_position_coordinate` | VARCHAR(50) | NULL                    | Previous position coordinate (optional text field)         |
| `new_location_id`              | NUMERIC(10) | NULL                    | New location ID (polymorphic: device, shelf, or rack)      |
| `new_location_type`            | VARCHAR(20) | NULL                    | New location type: 'device', 'shelf', or 'rack'            |
| `new_position_coordinate`      | VARCHAR(50) | NULL                    | New position coordinate (optional text field)              |
| `moved_by_user_id`             | INT         | NOT NULL, FK            | User who performed move                                    |
| `movement_date`                | TIMESTAMP   | NOT NULL, DEFAULT NOW() | Movement timestamp                                         |
| `reason`                       | TEXT        | NULL                    | Optional reason for move                                   |

**Constraints**:

- PRIMARY KEY (`id`)
- FOREIGN KEY (`sample_item_id`) REFERENCES `sample_item(id)` ON DELETE CASCADE
- FOREIGN KEY (`moved_by_user_id`) REFERENCES `system_user(id)`
- CHECK (`previous_location_id` IS NOT NULL AND `previous_location_type` IS NOT
  NULL) OR (`new_location_id` IS NOT NULL AND `new_location_type` IS NOT NULL) -
  At least one location (previous or new) must be specified
- CHECK (`previous_location_type` IS NULL OR `previous_location_type` IN
  ('device', 'shelf', 'rack'))
- CHECK (`new_location_type` IS NULL OR `new_location_type` IN ('device',
  'shelf', 'rack'))

**Relationships**:

- Many-to-One with `SampleItem`
- Polymorphic relationship to `StorageDevice`, `StorageShelf`, or `StorageRack`
  via `previous_location_id` + `previous_location_type` (previous location)
- Polymorphic relationship to `StorageDevice`, `StorageShelf`, or `StorageRack`
  via `new_location_id` + `new_location_type` (new location)
- Many-to-One with `SystemUser` (moved by)

**Immutability**:

- INSERT only (no UPDATE or DELETE allowed)
- Audit trail for compliance (SLIPTA/ISO requirements)
- Retained for 7+ years per constitution

**Event Types**:

- **Initial Assignment**: `previous_location_id = NULL`,
  `previous_location_type = NULL`, `previous_position_coordinate = NULL`,
  `new_location_id` and `new_location_type` populated with assigned location
- **Movement**: Both previous and new location fields populated (captures the
  change from one location to another)
- **Disposal/Removal**: `new_location_id = NULL`, `new_location_type = NULL`,
  `new_position_coordinate = NULL`, previous location fields contain the last
  known location

**Validation Rules**:

- At least one of (previous_location_id + previous_location_type) or
  (new_location_id + new_location_type) must be non-NULL
- `previous_location_type` and `new_location_type` must be one of: 'device',
  'shelf', or 'rack' (enforced by CHECK constraint)
- `movement_date` must not be in the future
- Cannot update or delete existing records (immutability enforced via database
  permissions)

**Relationship to SampleStorageAssignment**:

- `SampleStorageMovement` is an **audit log** of all changes to
  `SampleStorageAssignment`
- When a SampleItem is assigned or moved:
  1. `SampleStorageAssignment` is created or updated with the new current
     location
  2. A `SampleStorageMovement` record is created capturing both the previous
     state (from the old assignment) and the new state (the updated assignment)
- This provides a complete audit trail: the assignment table shows "where is it
  now", the movement table shows "how did it get there"

---

## State Transitions

### SampleItem Location Lifecycle

```
[No Location]
    │
    ├─(initial assignment)─> [Assigned to Location A (device/shelf/rack)]
    │                            │
    │                            ├─(moved)─> [Assigned to Location B (device/shelf/rack)]
    │                            │              │
    │                            │              └─(moved)─> [Assigned to Location C (device/shelf/rack)]
    │                            │
    │                            └─(disposed - P3, out of POC scope)─> [Disposed, No Location]
    │
    └─(direct disposal without assignment - P3, out of POC scope)─> [Disposed, No Location]
```

Note: Each location assignment can include an optional `position_coordinate`
(text field) for additional specificity within the assigned location (device,
shelf, or rack). Storage tracking operates at the SampleItem level (physical
specimens), not at the Sample level (orders).

### Location Hierarchy Active Status

```
[Active]
  │
  ├─(deactivate with no samples)─> [Inactive]
  │                                    │
  │                                    └─(reactivate)─> [Active]
  │
  └─(deactivate with active samples)─> [Warning: Move or dispose samples first]
                                         or
                                      [Inactive, but prevents new assignments]
```

---

## Indexing Strategy

**Performance Optimization Indexes** (for queries in search/filter workflows):

```sql
-- Hierarchy traversal (parent lookups)
CREATE INDEX idx_device_parent ON storage_device(parent_room_id);
CREATE INDEX idx_shelf_parent ON storage_shelf(parent_device_id);
CREATE INDEX idx_rack_parent ON storage_rack(parent_shelf_id);
CREATE INDEX idx_position_parent ON storage_position(parent_rack_id);

-- SampleItem lookups
CREATE INDEX idx_assignment_sample_item ON sample_storage_assignment(sample_item_id);
CREATE INDEX idx_assignment_location ON sample_storage_assignment(location_id, location_type);

-- Movement audit queries
CREATE INDEX idx_movement_sample_item ON sample_storage_movement(sample_item_id);
CREATE INDEX idx_movement_date ON sample_storage_movement(movement_date DESC);

-- FHIR UUID lookups
CREATE INDEX idx_room_fhir_uuid ON storage_room(fhir_uuid);
CREATE INDEX idx_device_fhir_uuid ON storage_device(fhir_uuid);
CREATE INDEX idx_shelf_fhir_uuid ON storage_shelf(fhir_uuid);
CREATE INDEX idx_rack_fhir_uuid ON storage_rack(fhir_uuid);

-- Active status filters
CREATE INDEX idx_room_active ON storage_room(active);
CREATE INDEX idx_device_active ON storage_device(active);
CREATE INDEX idx_shelf_active ON storage_shelf(active);
CREATE INDEX idx_rack_active ON storage_rack(active);

-- Position occupancy queries
CREATE INDEX idx_position_occupied ON storage_position(parent_rack_id, occupied);
```

---

## Data Volume Estimates (POC Scope)

**Assumptions**:

- Medium-sized laboratory (2,000 samples/month)
- 6-month POC duration
- 5 storage rooms, 20 devices, 50 shelves, 200 racks, 10,000 positions

| Entity                  | Estimated Rows                                | Storage (MB) |
| ----------------------- | --------------------------------------------- | ------------ |
| StorageRoom             | 5                                             | <1           |
| StorageDevice           | 20                                            | <1           |
| StorageShelf            | 50                                            | <1           |
| StorageRack             | 200                                           | <1           |
| StoragePosition         | 10,000                                        | ~2           |
| SampleStorageAssignment | 12,000 (6 months × 2k/month)                  | ~3           |
| SampleStorageMovement   | 15,000 (audit log, 1.25 moves/SampleItem avg) | ~4           |
| **Total**               | **37,275**                                    | **~11 MB**   |

**Growth Rate**: +2,000 assignments/month, +2,500 movements/month during POC

**Scalability**: Design supports 100,000+ SampleItems with <100MB storage
footprint and <100ms query times (with proper indexing).

---

## Summary

**Entities**: 7 (5 hierarchy + 2 assignment/audit)  
**Relationships**: 6 parent-child + 4 cross-entity (including Sample →
SampleItem)  
**FHIR Resources**: 5 Location resources (Room, Device, Shelf, Rack, Position) +
Specimen.container for SampleItem storage  
**Storage Granularity**: SampleItem level (physical specimens), not Sample level
(orders)  
**Audit Trail**: Complete (SampleStorageMovement immutable log)  
**Flexibility**: Position coordinates free-text, duplicate positions allowed
within racks  
**Performance**: Indexed for common queries (parent traversal, SampleItem
lookup, audit queries)
