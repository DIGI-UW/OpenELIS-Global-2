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
StorageRack (1) ───┼──> (N) StoragePosition
                  │
StoragePosition (1) ─┴──> (0..1) SampleStorageAssignment ──> (1) Sample
                  
SampleStorageAssignment ──> (N) SampleStorageMovement (audit log)
```

---

## 1. StorageRoom

**Purpose**: Top-level physical location entity representing laboratory rooms or facility areas.

**Table**: `STORAGE_ROOM`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key (StringSequenceGenerator) |
| `fhir_uuid` | UUID | NOT NULL, UNIQUE | FHIR Location resource identifier |
| `name` | VARCHAR(255) | NOT NULL | Human-readable room name |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Unique room code (e.g., "MAIN", "LAB-2") |
| `description` | TEXT | NULL | Optional room description |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Active/inactive status |
| `sys_user_id` | INT | NOT NULL | User who created/modified (audit) |
| `lastupdated` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp (optimistic lock) |

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
- Cannot deactivate room with active samples assigned to child locations (business logic check)

---

## 2. StorageDevice

**Purpose**: Storage equipment (freezers, refrigerators, cabinets) within a room.

**Table**: `STORAGE_DEVICE`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key |
| `fhir_uuid` | UUID | NOT NULL, UNIQUE | FHIR Location resource identifier |
| `name` | VARCHAR(255) | NOT NULL | Device name (e.g., "Freezer Unit 1") |
| `code` | VARCHAR(50) | NOT NULL | Device code (unique within parent room) |
| `type` | VARCHAR(20) | NOT NULL | Enum: freezer, refrigerator, cabinet, other |
| `temperature_setting` | DECIMAL(5,2) | NULL | Optional temperature in Celsius |
| `capacity_limit` | INT | NULL | Optional capacity limit (number of positions) |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Active/inactive status |
| `parent_room_id` | VARCHAR(36) | NOT NULL, FK | Parent room reference |
| `sys_user_id` | INT | NOT NULL | User who created/modified |
| `lastupdated` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Constraints**:
- PRIMARY KEY (`id`)
- UNIQUE (`parent_room_id`, `code`) - Code unique within parent room
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_room_id`) REFERENCES `storage_room(id)` ON DELETE RESTRICT
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

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key |
| `fhir_uuid` | UUID | NOT NULL, UNIQUE | FHIR Location resource identifier |
| `label` | VARCHAR(100) | NOT NULL | Shelf label (e.g., "Shelf-A", "Top") |
| `capacity_limit` | INT | NULL | Optional capacity limit |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Active/inactive status |
| `parent_device_id` | VARCHAR(36) | NOT NULL, FK | Parent device reference |
| `sys_user_id` | INT | NOT NULL | User who created/modified |
| `lastupdated` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Constraints**:
- PRIMARY KEY (`id`)
- UNIQUE (`parent_device_id`, `label`) - Label unique within parent device
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_device_id`) REFERENCES `storage_device(id)` ON DELETE RESTRICT
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

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key |
| `fhir_uuid` | UUID | NOT NULL, UNIQUE | FHIR Location resource identifier |
| `label` | VARCHAR(100) | NOT NULL | Rack label (e.g., "Rack R1", "Tray-1") |
| `rows` | INT | NOT NULL, DEFAULT 0 | Grid rows (0 = no grid) |
| `columns` | INT | NOT NULL, DEFAULT 0 | Grid columns (0 = no grid) |
| `position_schema_hint` | VARCHAR(50) | NULL | Optional hint for position naming (e.g., "A1", "1-1") |
| `active` | BOOLEAN | NOT NULL, DEFAULT true | Active/inactive status |
| `parent_shelf_id` | VARCHAR(36) | NOT NULL, FK | Parent shelf reference |
| `sys_user_id` | INT | NOT NULL | User who created/modified |
| `lastupdated` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Constraints**:
- PRIMARY KEY (`id`)
- UNIQUE (`parent_shelf_id`, `label`) - Label unique within parent shelf
- UNIQUE (`fhir_uuid`)
- CHECK (`rows` >= 0 AND `columns` >= 0)
- FOREIGN KEY (`parent_shelf_id`) REFERENCES `storage_shelf(id)` ON DELETE RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

**Relationships**:
- Many-to-One with `StorageShelf` (parent)
- One-to-Many with `StoragePosition` (children)

**Calculated Fields**:
- `capacity` = `rows` * `columns` (computed, not stored)
- If rows=0 OR columns=0, capacity=0 (no grid, rack-level assignment only)

**FHIR Mapping**:
- Maps to FHIR R4 `Location` resource
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` = "{room_code}-{device_code}-{shelf_label}-{rack_label}"
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

**Purpose**: Specific storage location within a rack. Flexible free-text coordinate system.

**Table**: `STORAGE_POSITION`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key |
| `coordinate` | VARCHAR(50) | NOT NULL | Free-text position coordinate (e.g., "A1", "RED-01") |
| `row_index` | INT | NULL | Optional row number for grid visualization |
| `column_index` | INT | NULL | Optional column number for grid visualization |
| `occupied` | BOOLEAN | NOT NULL, DEFAULT false | Occupancy status |
| `parent_rack_id` | VARCHAR(36) | NOT NULL, FK | Parent rack reference |
| `fhir_uuid` | UUID | NOT NULL, UNIQUE | FHIR Location resource identifier |
| `sys_user_id` | INT | NOT NULL | User who created/modified |
| `lastupdated` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Last modification timestamp |

**Constraints**:
- PRIMARY KEY (`id`)
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_rack_id`) REFERENCES `storage_rack(id)` ON DELETE CASCADE
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`
- NOTE: Duplicate coordinates within same rack allowed (flexible storage, per FR-014)

**Relationships**:
- Many-to-One with `StorageRack` (parent)
- One-to-One with `SampleStorageAssignment` (current assignment, if occupied)

**FHIR Mapping**:
- Maps to FHIR R4 `Location` resource (child of Rack Location)
- `Location.id` = `fhir_uuid`
- `Location.identifier.value` = "{room_code}-{device_code}-{shelf_label}-{rack_label}-{coordinate}"
- `Location.name` = `coordinate`
- `Location.physicalType.code` = "co" (container)
- `Location.partOf.reference` = "Location/{parent_rack_fhir_uuid}"
- `Location.extension[position-occupancy].valueBoolean` = `occupied`
- `Location.extension[position-grid-row].valueInteger` = `row_index` (if provided)
- `Location.extension[position-grid-column].valueInteger` = `column_index` (if provided)

**Validation Rules**:
- Coordinate is free text, max 50 characters (per FR-010)
- Disallow control characters (tabs, newlines) in coordinate
- Duplicate coordinates within same rack allowed (per FR-014 - flexible storage scenarios)
- Row_index and column_index are optional, used only for grid visualization
- Cannot delete position if occupied (occupied=true)

---

## 6. SampleStorageAssignment

**Purpose**: Current storage location assignment for a sample. One-to-one relationship (one sample, one current location).

**Table**: `SAMPLE_STORAGE_ASSIGNMENT`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key |
| `sample_id` | VARCHAR(36) | NOT NULL, UNIQUE | Sample reference (one current location per sample) |
| `storage_position_id` | VARCHAR(36) | NOT NULL, FK | Position reference |
| `assigned_by_user_id` | INT | NOT NULL, FK | User who assigned |
| `assigned_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Assignment timestamp |
| `notes` | TEXT | NULL | Optional assignment notes |

**Constraints**:
- PRIMARY KEY (`id`)
- UNIQUE (`sample_id`) - Enforces one current location per sample
- FOREIGN KEY (`sample_id`) REFERENCES `sample(id)` ON DELETE CASCADE
- FOREIGN KEY (`storage_position_id`) REFERENCES `storage_position(id)` ON DELETE RESTRICT
- FOREIGN KEY (`assigned_by_user_id`) REFERENCES `system_user(id)`

**Relationships**:
- Many-to-One with `Sample` (one sample, one current assignment)
- Many-to-One with `StoragePosition` (many samples can be assigned to different positions in same rack)
- Many-to-One with `SystemUser` (assigned by user)

**Business Logic**:
- On INSERT: Set `storage_position.occupied = true`, create entry in `SampleStorageMovement` audit log
- On UPDATE (sample moved): Update `storage_position_id`, set old position `occupied = false`, new position `occupied = true`, create audit log entry
- On DELETE (sample disposed): Set `storage_position.occupied = false`, create audit log entry with `new_position_id = NULL`

**Validation Rules**:
- Cannot assign sample to inactive storage location (check entire hierarchy: room, device, shelf, rack)
- Cannot assign sample to already-occupied position (unless rack allows duplicates - see FR-014)
- Sample can have only one current assignment (enforced by UNIQUE constraint on sample_id)

---

## 7. SampleStorageMovement

**Purpose**: Immutable audit log of sample storage movements. Records all location changes for compliance.

**Table**: `SAMPLE_STORAGE_MOVEMENT`

**Fields**:

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | VARCHAR(36) | PK, AUTO | Primary key |
| `sample_id` | VARCHAR(36) | NOT NULL, FK | Sample reference |
| `previous_position_id` | VARCHAR(36) | NULL, FK | Previous position (NULL for initial assignment) |
| `new_position_id` | VARCHAR(36) | NULL, FK | New position (NULL for disposal/removal) |
| `moved_by_user_id` | INT | NOT NULL, FK | User who performed move |
| `movement_date` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Movement timestamp |
| `reason` | TEXT | NULL | Optional reason for move |

**Constraints**:
- PRIMARY KEY (`id`)
- FOREIGN KEY (`sample_id`) REFERENCES `sample(id)` ON DELETE CASCADE
- FOREIGN KEY (`previous_position_id`) REFERENCES `storage_position(id)` ON DELETE SET NULL
- FOREIGN KEY (`new_position_id`) REFERENCES `storage_position(id)` ON DELETE SET NULL
- FOREIGN KEY (`moved_by_user_id`) REFERENCES `system_user(id)`
- CHECK (`previous_position_id` IS NOT NULL OR `new_position_id` IS NOT NULL) - At least one position must be specified

**Relationships**:
- Many-to-One with `Sample`
- Many-to-One with `StoragePosition` (previous location)
- Many-to-One with `StoragePosition` (new location)
- Many-to-One with `SystemUser` (moved by)

**Immutability**:
- INSERT only (no UPDATE or DELETE allowed)
- Audit trail for compliance (SLIPTA/ISO requirements)
- Retained for 7+ years per constitution

**Event Types**:
- **Initial Assignment**: `previous_position_id = NULL`, `new_position_id` = assigned position
- **Movement**: Both previous and new position IDs populated
- **Disposal/Removal**: `new_position_id = NULL`, `previous_position_id` = last position

**Validation Rules**:
- At least one of previous_position_id or new_position_id must be non-NULL
- movement_date must not be in the future
- Cannot update or delete existing records (immutability enforced via database permissions)

---

## State Transitions

### Sample Location Lifecycle

```
[No Location] 
    │
    ├─(initial assignment)─> [Assigned to Position A]
    │                            │
    │                            ├─(moved)─> [Assigned to Position B]
    │                            │              │
    │                            │              └─(moved)─> [Assigned to Position C]
    │                            │
    │                            └─(disposed - P3, out of POC scope)─> [Disposed, No Location]
    │
    └─(direct disposal without assignment - P3, out of POC scope)─> [Disposed, No Location]
```

### Position Occupancy State

```
[Empty]
  │
  ├─(sample assigned)─> [Occupied]
  │                        │
  │                        ├─(sample moved away)─> [Empty]
  │                        │
  │                        └─(sample disposed)─> [Empty]
  │
  └─(position created but not used)─> [Empty]
```

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

-- Sample lookups
CREATE INDEX idx_assignment_sample ON sample_storage_assignment(sample_id);
CREATE INDEX idx_assignment_position ON sample_storage_assignment(storage_position_id);

-- Movement audit queries
CREATE INDEX idx_movement_sample ON sample_storage_movement(sample_id);
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

| Entity | Estimated Rows | Storage (MB) |
|--------|----------------|--------------|
| StorageRoom | 5 | <1 |
| StorageDevice | 20 | <1 |
| StorageShelf | 50 | <1 |
| StorageRack | 200 | <1 |
| StoragePosition | 10,000 | ~2 |
| SampleStorageAssignment | 12,000 (6 months × 2k/month) | ~3 |
| SampleStorageMovement | 15,000 (audit log, 1.25 moves/sample avg) | ~4 |
| **Total** | **37,275** | **~11 MB** |

**Growth Rate**: +2,000 assignments/month, +2,500 movements/month during POC

**Scalability**: Design supports 100,000+ samples with <100MB storage footprint and <100ms query times (with proper indexing).

---

## Summary

**Entities**: 7 (5 hierarchy + 2 assignment/audit)  
**Relationships**: 6 parent-child + 3 cross-entity  
**FHIR Resources**: 5 Location resources (Room, Device, Shelf, Rack, Position)  
**Audit Trail**: Complete (SampleStorageMovement immutable log)  
**Flexibility**: Position coordinates free-text, duplicate positions allowed within racks  
**Performance**: Indexed for common queries (parent traversal, sample lookup, audit queries)

