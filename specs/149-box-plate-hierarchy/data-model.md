# Data Model: Box Storage Hierarchy Enhancement

**Feature**: 149-box-plate-hierarchy  
**Parent Feature**: 001-sample-storage  
**Date**: December 9, 2025  
**Status**: Draft

## Executive Summary

This document details the database schema changes required to implement the Box
Storage Hierarchy Enhancement (OGC-149). The enhancement adds a fifth persistent
hierarchy level (Box) between Rack and the virtual Position coordinate, enabling
accurate representation of laboratory storage without persisting empty position
rows.

**Key Changes**:

- **StorageRack**: Remove grid fields (`rows`, `columns`,
  `position_schema_hint`), retain `label` field
- **StorageBox**: New entity with grid dimensions and barcode support
- **StoragePosition**: **Removed** (positions are virtual text coordinates
  stored on assignments)
- **SampleStorageAssignment**: Extend `location_type` enum to include `'box'`;
  add text position coordinate

---

## Entity Relationship Diagram

```
StorageRoom
   │
   └─▶ StorageDevice
           │
           └─▶ StorageShelf
                   │
                   └─▶ StorageRack (simplified: no grid)
                           │
                           └─▶ StorageBox (grid container)
                                   │
                                   └─▶ (Virtual Position as coordinate on SampleStorageAssignment)

SampleStorageAssignment
   - location_type ∈ {device, shelf, rack, box}
   - location_id → respective entity ID
   - position_coordinate (text) stores the slot within the Box (or optional note at other levels)
```

---

## 1. StorageRack (Modified)

**Purpose**: Simplified grouping container for Boxes. No longer contains grid
structure.

**Table**: `STORAGE_RACK`

### Schema Changes

| Change Type | Field                  | Action        | Description                  |
| ----------- | ---------------------- | ------------- | ---------------------------- |
| **DROP**    | `rows`                 | Remove column | Grid dimensions moved to Box |
| **DROP**    | `columns`              | Remove column | Grid dimensions moved to Box |
| **DROP**    | `position_schema_hint` | Remove column | Position schema moved to Box |
| **RETAIN**  | `LABEL`                | No change     | Field retained (not renamed) |

### Updated Schema

| Field             | Type         | Constraints             | Description                          |
| ----------------- | ------------ | ----------------------- | ------------------------------------ |
| `id`              | INTEGER      | PK, AUTO                | Primary key (sequence generator)     |
| `fhir_uuid`       | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier    |
| `label`           | VARCHAR(100) | NOT NULL                | Human-readable rack name             |
| `code`            | VARCHAR(10)  | NOT NULL                | Unique rack code within parent shelf |
| `active`          | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status               |
| `parent_shelf_id` | INTEGER      | NOT NULL, FK            | Parent shelf reference               |
| `sys_user_id`     | INTEGER      | NOT NULL                | User who created/modified            |
| `lastupdated`     | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp          |

### Constraints

- PRIMARY KEY (`id`)
- UNIQUE (`parent_shelf_id`, `code`) - Code unique within parent shelf
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_shelf_id`) REFERENCES `storage_shelf(id)` ON DELETE
  RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

### Relationships

- **Many-to-One** with `StorageShelf` (parent)
- **One-to-Many** with `StorageBox` (children) - NEW

### FHIR Mapping Changes

**Before (Feature 001)**:

```
Location.extension[grid-dimensions].valueString = "{rows} × {columns}"
```

**After (OGC-149)**:

```
(No grid extensions - Rack is now a simple container)
Location.physicalType.code = "co" (container)
```

---

## 2. StorageBox (New Entity)

**Purpose**: Physical container (box, plate, tray) with grid-based position
structure. Holds the grid dimensions previously on Rack.

**Table**: `STORAGE_BOX`

### Schema

| Field                  | Type         | Constraints             | Description                                           |
| ---------------------- | ------------ | ----------------------- | ----------------------------------------------------- |
| `id`                   | INTEGER      | PK, AUTO                | Primary key (sequence generator)                      |
| `fhir_uuid`            | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier                     |
| `label`                | VARCHAR(100) | NOT NULL                | Human-readable box name                               |
| `code`                 | VARCHAR(10)  | NOT NULL                | Unique code within parent rack                        |
| `rows`                 | INTEGER      | NOT NULL                | Grid rows (minimum 1)                                 |
| `columns`              | INTEGER      | NOT NULL                | Grid columns (minimum 1)                              |
| `position_schema_hint` | VARCHAR(50)  | NULL                    | Optional hint for position naming (e.g., "A1", "1-1") |
| `active`               | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status                                |
| `parent_rack_id`       | INTEGER      | NOT NULL, FK            | Parent rack reference                                 |
| `sys_user_id`          | INTEGER      | NOT NULL                | User who created/modified                             |
| `lastupdated`          | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp                           |

### Constraints

- PRIMARY KEY (`id`)
- UNIQUE (`parent_rack_id`, `code`) - Code unique within parent rack
- UNIQUE (`fhir_uuid`)
- CHECK (`rows` >= 1 AND `columns` >= 1) - Grid dimensions must be at least 1×1
- FOREIGN KEY (`parent_rack_id`) REFERENCES `storage_rack(id)` ON DELETE
  RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

### Relationships

- **Many-to-One** with `StorageRack` (parent)
- **One-to-Many** with `SampleStorageAssignment` (via
  location_id/location_type="box" and position_coordinate for occupancy)

### Calculated Fields

- **capacity** = `rows` × `columns` (computed, not stored in database)

### Indexes

```sql
CREATE INDEX idx_box_parent ON storage_box(parent_rack_id);
CREATE INDEX idx_box_fhir_uuid ON storage_box(fhir_uuid);
CREATE INDEX idx_box_active ON storage_box(active);
CREATE INDEX idx_box_code ON storage_box(parent_rack_id, code);
```

### FHIR Mapping

```
Location.id = fhir_uuid
Location.identifier[0].system = "http://openelis-global.org/storage/box"
Location.identifier[0].value = "{room_code}-{device_code}-{shelf_code}-{rack_code}-{box_code}"
Location.name = label
Location.status = active ? "active" : "inactive"
Location.type[0].coding[0].system = "http://terminology.hl7.org/CodeSystem/location-physical-type"
Location.type[0].coding[0].code = "co" (container)
Location.physicalType.coding[0].code = "co"
Location.partOf.reference = "Location/{parent_rack_fhir_uuid}"

Extensions (FR-025):
Location.extension[0].url = "http://openelis-global.org/fhir/StructureDefinition/storage-grid-rows"
Location.extension[0].valueInteger = rows
Location.extension[1].url = "http://openelis-global.org/fhir/StructureDefinition/storage-grid-columns"
Location.extension[1].valueInteger = columns
Location.extension[2].url = "http://openelis-global.org/fhir/StructureDefinition/storage-position-schema-hint"
Location.extension[2].valueString = position_schema_hint (optional)
```

---

## 3. SampleStorageAssignment (Modified)

**Purpose**: Current storage location assignment for a SampleItem. Supports
flexible assignment to any hierarchy level with virtual coordinates.

**Table**: `SAMPLE_STORAGE_ASSIGNMENT`

### Schema Changes

| Change Type | Field           | Action      | Description               |
| ----------- | --------------- | ----------- | ------------------------- |
| **UPDATE**  | `location_type` | Extend enum | Add 'box' to valid values |

### Updated Schema

| Field                 | Type            | Constraints             | Description                                |
| --------------------- | --------------- | ----------------------- | ------------------------------------------ |
| `id`                  | INTEGER         | PK, AUTO                | Primary key                                |
| `sample_item_id`      | INTEGER         | NOT NULL, UNIQUE        | SampleItem reference                       |
| `location_id`         | INTEGER         | NOT NULL                | Polymorphic location ID                    |
| **`location_type`**   | **VARCHAR(20)** | **NOT NULL**            | **Type: 'device', 'shelf', 'rack', 'box'** |
| `position_coordinate` | VARCHAR(50)     | NULL                    | Optional text-based position coordinate    |
| `assigned_by_user_id` | INTEGER         | NOT NULL, FK            | User who assigned                          |
| `assigned_date`       | TIMESTAMP       | NOT NULL, DEFAULT NOW() | Assignment timestamp                       |
| `notes`               | TEXT            | NULL                    | Optional assignment notes                  |

### Constraints

- PRIMARY KEY (`id`)
- UNIQUE (`sample_item_id`)
- FOREIGN KEY (`sample_item_id`) REFERENCES `sample_item(id)` ON DELETE CASCADE
- FOREIGN KEY (`assigned_by_user_id`) REFERENCES `system_user(id)`
- **CHECK (`location_type IN ('device', 'shelf', 'rack', 'box')`)**

### Location Type Mapping

| location_type | location_id references | Hierarchy Levels                           |
| ------------- | ---------------------- | ------------------------------------------ |
| 'device'      | storage_device.id      | 2 (Room → Device)                          |
| 'shelf'       | storage_shelf.id       | 3 (Room → Device → Shelf)                  |
| 'rack'        | storage_rack.id        | 4 (Room → Device → Shelf → Rack)           |
| **'box'**     | **storage_box.id**     | **5 (Room → Device → Shelf → Rack → Box)** |

---

## Migration Strategy

### Approach: Destructive Migration

Since Feature 001 is **not yet in production**, this enhancement uses a
destructive migration approach - existing Rack grid data will be dropped without
preservation.

### Migration Steps

#### Step 1: Backup Current State (Safety)

```sql
-- Create backup of current rack data
CREATE TABLE storage_rack_backup_ogc149 AS
SELECT * FROM storage_rack;
```

#### Step 2: Drop Rack Grid Columns

```sql
-- Remove grid-related columns from STORAGE_RACK
ALTER TABLE storage_rack DROP COLUMN IF EXISTS rows;
ALTER TABLE storage_rack DROP COLUMN IF EXISTS columns;
ALTER TABLE storage_rack DROP COLUMN IF EXISTS position_schema_hint;
```

#### Step 3: Retain Rack Label Field

```sql
-- No change needed - LABEL field is retained (not renamed)
```

#### Step 4: Create StorageBox Table

```sql
-- Create new STORAGE_BOX table
CREATE TABLE storage_box (
    id INTEGER NOT NULL,
    fhir_uuid UUID NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    code VARCHAR(10) NOT NULL,
    rows INTEGER NOT NULL CHECK (rows >= 1),
    columns INTEGER NOT NULL CHECK (columns >= 1),
    position_schema_hint VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT true,
    parent_rack_id INTEGER NOT NULL,
    sys_user_id INTEGER NOT NULL,
    lastupdated TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    UNIQUE (parent_rack_id, code),
    FOREIGN KEY (parent_rack_id) REFERENCES storage_rack(id) ON DELETE RESTRICT,
    FOREIGN KEY (sys_user_id) REFERENCES system_user(id)
);

-- Create sequence for ID generation
CREATE SEQUENCE storage_box_seq START WITH 1 INCREMENT BY 1;

-- Create indexes
CREATE INDEX idx_box_parent ON storage_box(parent_rack_id);
CREATE INDEX idx_box_fhir_uuid ON storage_box(fhir_uuid);
CREATE INDEX idx_box_active ON storage_box(active);
CREATE INDEX idx_box_code ON storage_box(parent_rack_id, code);
```

#### Step 5: Update location_type enums

```sql
-- Update CHECK constraint to include 'box'
ALTER TABLE sample_storage_assignment
DROP CONSTRAINT IF EXISTS chk_location_type;

ALTER TABLE sample_storage_assignment
ADD CONSTRAINT chk_location_type
CHECK (location_type IN ('device', 'shelf', 'rack', 'box'));

ALTER TABLE sample_storage_movement
DROP CONSTRAINT IF EXISTS chk_previous_location_type;

ALTER TABLE sample_storage_movement
ADD CONSTRAINT chk_previous_location_type
CHECK (previous_location_type IS NULL OR previous_location_type IN ('device', 'shelf', 'rack', 'box'));

ALTER TABLE sample_storage_movement
DROP CONSTRAINT IF EXISTS chk_new_location_type;

ALTER TABLE sample_storage_movement
ADD CONSTRAINT chk_new_location_type
CHECK (new_location_type IS NULL OR new_location_type IN ('device', 'shelf', 'rack', 'box'));
```

#### Step 6: Update SampleStorageAssignment Enum

```sql
-- Update CHECK constraint to include 'box'
ALTER TABLE sample_storage_assignment
DROP CONSTRAINT IF EXISTS chk_location_type;

ALTER TABLE sample_storage_assignment
ADD CONSTRAINT chk_location_type
CHECK (location_type IN ('device', 'shelf', 'rack', 'box'));
```

#### Step 7: Update SampleStorageMovement Enum

```sql
-- Update CHECK constraints for previous and new location types
ALTER TABLE sample_storage_movement
DROP CONSTRAINT IF EXISTS chk_previous_location_type;

ALTER TABLE sample_storage_movement
ADD CONSTRAINT chk_previous_location_type
CHECK (previous_location_type IS NULL OR previous_location_type IN ('device', 'shelf', 'rack', 'box'));

ALTER TABLE sample_storage_movement
DROP CONSTRAINT IF EXISTS chk_new_location_type;

ALTER TABLE sample_storage_movement
ADD CONSTRAINT chk_new_location_type
CHECK (new_location_type IS NULL OR new_location_type IN ('device', 'shelf', 'rack', 'box'));
```

### Liquibase Changesets

All migration steps will be implemented as Liquibase changesets:

**File**:
`src/main/resources/liquibase/3.3.x.x/XXX-restructure-rack-add-box-plate.xml`
(use next available sequence number)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <!-- Changeset 010-1: Drop Rack grid columns -->
    <changeSet id="ogc-149-drop-rack-grid-columns" author="openelis">
        <dropColumn tableName="storage_rack" columnName="rows"/>
        <dropColumn tableName="storage_rack" columnName="columns"/>
        <dropColumn tableName="storage_rack" columnName="position_schema_hint"/>
    </changeSet>

    <!-- Changeset 010-2: No rename needed - LABEL field is retained -->
    <!-- Note: StorageRack.label field is retained (no rename to name) -->

    <!-- Changeset 010-3: Create STORAGE_BOX table -->
    <changeSet id="ogc-149-create-storage-box" author="openelis">
        <createTable tableName="storage_box">
            <column name="id" type="INTEGER">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="fhir_uuid" type="UUID">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="label" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="code" type="VARCHAR(10)">
                <constraints nullable="false"/>
            </column>
            <column name="rows" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="columns" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="position_schema_hint" type="VARCHAR(50)"/>
            <column name="active" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="parent_rack_id" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="sys_user_id" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint baseTableName="storage_box"
                                 baseColumnNames="parent_rack_id"
                                 constraintName="fk_box_rack"
                                 referencedTableName="storage_rack"
                                 referencedColumnNames="id"
                                 onDelete="RESTRICT"/>

        <addForeignKeyConstraint baseTableName="storage_box"
                                 baseColumnNames="sys_user_id"
                                 constraintName="fk_box_user"
                                 referencedTableName="system_user"
                                 referencedColumnNames="id"/>

        <addUniqueConstraint tableName="storage_box"
                             columnNames="parent_rack_id, code"
                             constraintName="uq_box_code_per_rack"/>

        <createSequence sequenceName="storage_box_seq" startValue="1" incrementBy="1"/>

        <createIndex tableName="storage_box" indexName="idx_box_parent">
            <column name="parent_rack_id"/>
        </createIndex>

        <createIndex tableName="storage_box" indexName="idx_box_fhir_uuid">
            <column name="fhir_uuid"/>
        </createIndex>

        <createIndex tableName="storage_box" indexName="idx_box_active">
            <column name="active"/>
        </createIndex>
    </changeSet>

    <!-- Changeset 010-4: Update location_type enums to add 'box' -->
    <changeSet id="ogc-149-update-location-type-enums" author="openelis">
        <sql>
            ALTER TABLE sample_storage_assignment
            DROP CONSTRAINT IF EXISTS chk_location_type;

            ALTER TABLE sample_storage_assignment
            ADD CONSTRAINT chk_location_type
            CHECK (location_type IN ('device', 'shelf', 'rack', 'box'));

            ALTER TABLE sample_storage_movement
            DROP CONSTRAINT IF EXISTS chk_previous_location_type;

            ALTER TABLE sample_storage_movement
            ADD CONSTRAINT chk_previous_location_type
            CHECK (previous_location_type IS NULL OR previous_location_type IN ('device', 'shelf', 'rack', 'box'));

            ALTER TABLE sample_storage_movement
            DROP CONSTRAINT IF EXISTS chk_new_location_type;

            ALTER TABLE sample_storage_movement
            ADD CONSTRAINT chk_new_location_type
            CHECK (new_location_type IS NULL OR new_location_type IN ('device', 'shelf', 'rack', 'box'));
        </sql>
    </changeSet>

</databaseChangeLog>
```

---

## Data Volume Impact

**Assumptions** (POC scope):

- Existing: 200 racks from Feature 001
- New: Average 5 boxes per rack = 1,000 boxes
- Positions: Virtual (no persistent rows for empty slots)

| Entity                  | Before (Feature 001) | After (OGC-149) | Delta                 |
| ----------------------- | -------------------- | --------------- | --------------------- |
| StorageRack             | 200 rows             | 200 rows        | 0 (simplified schema) |
| **StorageBox**          | 0 rows               | **1,000 rows**  | **+1,000** (new)      |
| StoragePosition         | 10,000 rows          | 0 rows          | **-10,000** (removed) |
| SampleStorageAssignment | 12,000 rows          | 12,000 rows     | 0 (enum extended)     |
| **Total New Data**      | -                    | **~1,000 rows** | **~50 KB**            |

**Storage Impact**: Minimal (<100 KB additional storage)

---

## Rollback Strategy

### Manual Rollback (if needed)

Since this is destructive migration, rollback requires manual data
reconstruction:

```sql
-- 1. Drop new structures
DROP TABLE IF EXISTS storage_box CASCADE;
DROP SEQUENCE IF EXISTS storage_box_seq;

-- 2. Restore rack grid columns
ALTER TABLE storage_rack
ADD COLUMN rows INTEGER NOT NULL DEFAULT 0,
ADD COLUMN columns INTEGER NOT NULL DEFAULT 0,
ADD COLUMN position_schema_hint VARCHAR(50);

-- 3. No label field rename needed (label field was retained, not renamed)

-- 4. Revert location_type enums
-- (Revert to 'device', 'shelf', 'rack' only)
```

**Note**: Feature 001 not in production, so rollback is unlikely to be needed.

---

## Summary

This data model enhancement adds the Box hierarchy level while maintaining
backward compatibility and flexibility. Key highlights:

- **StorageRack simplified**: Removes grid complexity, now a pure container
- **StorageBox introduced**: Captures grid dimensions, supports 6 standard
  presets + custom
- **Positions virtualized**: Coordinates stored on assignments (no position
  table)
- **Migration**: Destructive (safe since Feature 001 not in production)
- **Storage impact**: Minimal (~50 KB for 1,000 boxes)
- **FHIR compliance**: Location resource for Box with grid extensions
