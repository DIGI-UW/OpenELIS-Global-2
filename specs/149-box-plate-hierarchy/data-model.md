# Data Model: Box/Plate Hierarchy Enhancement

**Feature**: 149-box-plate-hierarchy  
**Parent Feature**: 001-sample-storage  
**Date**: December 5, 2025  
**Status**: Draft

## Executive Summary

This document details the database schema changes required to implement the
Box/Plate hierarchy enhancement (OGC-149). The enhancement adds a sixth storage
hierarchy level (Box/Plate) between Rack and Position, enabling more accurate
representation of laboratory storage organization.

**Key Changes**:

- **StorageRack**: Remove grid fields (`rows`, `columns`,
  `position_schema_hint`), rename `label` → `name`
- **StorageBoxPlate**: New entity with grid dimensions and barcode support
- **StoragePosition**: Add `parent_box_plate_id` (optional), maintain flexible
  2-6 level hierarchy
- **SampleStorageAssignment**: Extend `location_type` enum to include
  `'box_plate'`

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Existing Feature 001 Entities                 │
│                   (unchanged in this feature)                    │
└─────────────────────────────────────────────────────────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                             ▼
         StorageRoom                  StorageDevice
              │                             │
              └──────────┬──────────────────┘
                         ▼
                   StorageShelf
                         │
                         ▼
                   StorageRack (MODIFIED)
                   - Remove: rows, columns, position_schema_hint
                   - Rename: label → name
                         │
                         │ One-to-Many (NEW)
                         ▼
                   StorageBoxPlate (NEW)
                   - id, fhir_uuid, name, code
                   - rows, columns, position_schema_hint
                   - active, parent_rack_id
                         │
                         │ One-to-Many
                         ▼
                   StoragePosition (MODIFIED)
                   - Add: parent_box_plate_id (nullable)
                   - Maintains flexible hierarchy:
                     * 2 levels: device
                     * 3 levels: device + shelf
                     * 4 levels: device + shelf + rack
                     * 5 levels: device + shelf + rack + box_plate
                     * 6 levels: device + shelf + rack + box_plate + position
                         │
                         │ Referenced by
                         ▼
                   SampleStorageAssignment (MODIFIED)
                   - location_type: 'device', 'shelf', 'rack', 'box_plate' (NEW)
```

---

## 1. StorageRack (Modified)

**Purpose**: Simplified grouping container for Box/Plates. No longer contains
grid structure.

**Table**: `STORAGE_RACK`

### Schema Changes

| Change Type | Field                  | Action        | Description                           |
| ----------- | ---------------------- | ------------- | ------------------------------------- |
| **DROP**    | `rows`                 | Remove column | Grid dimensions moved to Box/Plate    |
| **DROP**    | `columns`              | Remove column | Grid dimensions moved to Box/Plate    |
| **DROP**    | `position_schema_hint` | Remove column | Position schema moved to Box/Plate    |
| **RENAME**  | `LABEL` → `NAME`       | Rename column | Align with Room, Device, Shelf naming |

### Updated Schema

| Field             | Type         | Constraints             | Description                                 |
| ----------------- | ------------ | ----------------------- | ------------------------------------------- |
| `id`              | INTEGER      | PK, AUTO                | Primary key (sequence generator)            |
| `fhir_uuid`       | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier           |
| `name`            | VARCHAR(100) | NOT NULL                | Human-readable rack name (formerly `label`) |
| `code`            | VARCHAR(10)  | NOT NULL                | Unique rack code within parent shelf        |
| `active`          | BOOLEAN      | NOT NULL, DEFAULT true  | Active/inactive status                      |
| `parent_shelf_id` | INTEGER      | NOT NULL, FK            | Parent shelf reference                      |
| `sys_user_id`     | INTEGER      | NOT NULL                | User who created/modified                   |
| `lastupdated`     | TIMESTAMP    | NOT NULL, DEFAULT NOW() | Last modification timestamp                 |

### Constraints

- PRIMARY KEY (`id`)
- UNIQUE (`parent_shelf_id`, `code`) - Code unique within parent shelf
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_shelf_id`) REFERENCES `storage_shelf(id)` ON DELETE
  RESTRICT
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`

### Relationships

- **Many-to-One** with `StorageShelf` (parent)
- **One-to-Many** with `StorageBoxPlate` (children) - NEW

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

## 2. StorageBoxPlate (New Entity)

**Purpose**: Physical container (box, plate, tray) with grid-based position
structure. Holds the grid dimensions previously on Rack.

**Table**: `STORAGE_BOX_PLATE`

### Schema

| Field                  | Type         | Constraints             | Description                                           |
| ---------------------- | ------------ | ----------------------- | ----------------------------------------------------- |
| `id`                   | INTEGER      | PK, AUTO                | Primary key (sequence generator)                      |
| `fhir_uuid`            | UUID         | NOT NULL, UNIQUE        | FHIR Location resource identifier                     |
| `name`                 | VARCHAR(100) | NOT NULL                | Human-readable box/plate name                         |
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
- **One-to-Many** with `StoragePosition` (children)

### Calculated Fields

- **capacity** = `rows` × `columns` (computed, not stored in database)

### Indexes

```sql
CREATE INDEX idx_box_plate_parent ON storage_box_plate(parent_rack_id);
CREATE INDEX idx_box_plate_fhir_uuid ON storage_box_plate(fhir_uuid);
CREATE INDEX idx_box_plate_active ON storage_box_plate(active);
CREATE INDEX idx_box_plate_code ON storage_box_plate(parent_rack_id, code);
```

### FHIR Mapping

```
Location.id = fhir_uuid
Location.identifier[0].system = "http://openelis-global.org/storage/box-plate"
Location.identifier[0].value = "{room_code}-{device_code}-{shelf_code}-{rack_code}-{box_plate_code}"
Location.name = name
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

## 3. StoragePosition (Modified)

**Purpose**: Flexible storage location at any hierarchy level (2-6 levels). Can
now optionally reference a Box/Plate parent.

**Table**: `STORAGE_POSITION`

### Schema Changes

| Change Type | Field                 | Action          | Description                         |
| ----------- | --------------------- | --------------- | ----------------------------------- |
| **ADD**     | `parent_box_plate_id` | Add nullable FK | Optional Box/Plate parent reference |

### Updated Schema

| Field                     | Type        | Constraints             | Description                                       |
| ------------------------- | ----------- | ----------------------- | ------------------------------------------------- |
| `id`                      | INTEGER     | PK, AUTO                | Primary key                                       |
| `coordinate`              | VARCHAR(50) | NULL                    | Free-text position coordinate                     |
| `row_index`               | INTEGER     | NULL                    | Optional row number for grid visualization        |
| `column_index`            | INTEGER     | NULL                    | Optional column number for grid visualization     |
| `occupied`                | BOOLEAN     | NOT NULL, DEFAULT false | Occupancy status                                  |
| `parent_device_id`        | INTEGER     | NOT NULL, FK            | Parent device (required - minimum 2 levels)       |
| `parent_shelf_id`         | INTEGER     | NULL, FK                | Parent shelf (optional - 3+ levels)               |
| `parent_rack_id`          | INTEGER     | NULL, FK                | Parent rack (optional - 4+ levels)                |
| **`parent_box_plate_id`** | **INTEGER** | **NULL, FK**            | **Parent box/plate (optional - 5+ levels) - NEW** |
| `fhir_uuid`               | UUID        | NOT NULL, UNIQUE        | FHIR Location resource identifier                 |
| `sys_user_id`             | INTEGER     | NOT NULL                | User who created/modified                         |
| `lastupdated`             | TIMESTAMP   | NOT NULL, DEFAULT NOW() | Last modification timestamp                       |

### Constraints

- PRIMARY KEY (`id`)
- UNIQUE (`fhir_uuid`)
- FOREIGN KEY (`parent_device_id`) REFERENCES `storage_device(id)` ON DELETE
  CASCADE
- FOREIGN KEY (`parent_shelf_id`) REFERENCES `storage_shelf(id)` ON DELETE
  CASCADE (if not NULL)
- FOREIGN KEY (`parent_rack_id`) REFERENCES `storage_rack(id)` ON DELETE CASCADE
  (if not NULL)
- **FOREIGN KEY (`parent_box_plate_id`) REFERENCES `storage_box_plate(id)` ON
  DELETE CASCADE (if not NULL)** - NEW
- FOREIGN KEY (`sys_user_id`) REFERENCES `system_user(id)`
- **CHECK**: If `parent_box_plate_id` is NOT NULL, then `parent_rack_id` must
  also be NOT NULL (FR-022b)
- **CHECK**: If `parent_rack_id` is NOT NULL, then `parent_shelf_id` must also
  be NOT NULL
- **CHECK**: If `parent_shelf_id` is NOT NULL, then `parent_device_id` must be
  NOT NULL (always true since device is required)

### Hierarchy Flexibility

StoragePosition supports 2-6 level hierarchy:

| Level Count | Hierarchy                                           | parent_device_id | parent_shelf_id | parent_rack_id | parent_box_plate_id |
| ----------- | --------------------------------------------------- | ---------------- | --------------- | -------------- | ------------------- |
| 2           | Room → Device                                       | SET              | NULL            | NULL           | NULL                |
| 3           | Room → Device → Shelf                               | SET              | SET             | NULL           | NULL                |
| 4           | Room → Device → Shelf → Rack                        | SET              | SET             | SET            | NULL                |
| 5           | Room → Device → Shelf → Rack → Box/Plate            | SET              | SET             | SET            | SET                 |
| 6           | Room → Device → Shelf → Rack → Box/Plate → Position | SET              | SET             | SET            | SET                 |

### Indexes

```sql
CREATE INDEX idx_position_parent_device ON storage_position(parent_device_id);
CREATE INDEX idx_position_parent_shelf ON storage_position(parent_shelf_id);
CREATE INDEX idx_position_parent_rack ON storage_position(parent_rack_id);
CREATE INDEX idx_position_parent_box_plate ON storage_position(parent_box_plate_id); -- NEW
CREATE INDEX idx_position_occupied ON storage_position(parent_box_plate_id, occupied); -- Updated for Box/Plate queries
```

---

## 4. SampleStorageAssignment (Modified)

**Purpose**: Current storage location assignment for a SampleItem. Supports
flexible assignment to any hierarchy level.

**Table**: `SAMPLE_STORAGE_ASSIGNMENT`

### Schema Changes

| Change Type | Field           | Action      | Description                     |
| ----------- | --------------- | ----------- | ------------------------------- |
| **UPDATE**  | `location_type` | Extend enum | Add 'box_plate' to valid values |

### Updated Schema

| Field                 | Type            | Constraints             | Description                                                 |
| --------------------- | --------------- | ----------------------- | ----------------------------------------------------------- |
| `id`                  | INTEGER         | PK, AUTO                | Primary key                                                 |
| `sample_item_id`      | INTEGER         | NOT NULL, UNIQUE        | SampleItem reference                                        |
| `location_id`         | INTEGER         | NOT NULL                | Polymorphic location ID                                     |
| **`location_type`**   | **VARCHAR(20)** | **NOT NULL**            | **Type: 'device', 'shelf', 'rack', 'box_plate' - EXTENDED** |
| `position_coordinate` | VARCHAR(50)     | NULL                    | Optional text-based position coordinate                     |
| `assigned_by_user_id` | INTEGER         | NOT NULL, FK            | User who assigned                                           |
| `assigned_date`       | TIMESTAMP       | NOT NULL, DEFAULT NOW() | Assignment timestamp                                        |
| `notes`               | TEXT            | NULL                    | Optional assignment notes                                   |

### Constraints

- PRIMARY KEY (`id`)
- UNIQUE (`sample_item_id`)
- FOREIGN KEY (`sample_item_id`) REFERENCES `sample_item(id)` ON DELETE CASCADE
- FOREIGN KEY (`assigned_by_user_id`) REFERENCES `system_user(id)`
- **CHECK (`location_type IN ('device', 'shelf', 'rack', 'box_plate')`)** -
  Updated enum

### Location Type Mapping

| location_type   | location_id references   | Hierarchy Levels                                       |
| --------------- | ------------------------ | ------------------------------------------------------ |
| 'device'        | storage_device.id        | 2 (Room → Device)                                      |
| 'shelf'         | storage_shelf.id         | 3 (Room → Device → Shelf)                              |
| 'rack'          | storage_rack.id          | 4 (Room → Device → Shelf → Rack)                       |
| **'box_plate'** | **storage_box_plate.id** | **5 (Room → Device → Shelf → Rack → Box/Plate)** - NEW |

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

#### Step 3: Rename Rack Label Field

```sql
-- Rename LABEL to NAME for consistency
ALTER TABLE storage_rack RENAME COLUMN label TO name;
```

#### Step 4: Create StorageBoxPlate Table

```sql
-- Create new STORAGE_BOX_PLATE table
CREATE TABLE storage_box_plate (
    id INTEGER NOT NULL,
    fhir_uuid UUID NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
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
CREATE SEQUENCE storage_box_plate_seq START WITH 1 INCREMENT BY 1;

-- Create indexes
CREATE INDEX idx_box_plate_parent ON storage_box_plate(parent_rack_id);
CREATE INDEX idx_box_plate_fhir_uuid ON storage_box_plate(fhir_uuid);
CREATE INDEX idx_box_plate_active ON storage_box_plate(active);
CREATE INDEX idx_box_plate_code ON storage_box_plate(parent_rack_id, code);
```

#### Step 5: Add Parent Box/Plate to StoragePosition

```sql
-- Add new parent_box_plate_id column to STORAGE_POSITION
ALTER TABLE storage_position
ADD COLUMN parent_box_plate_id INTEGER;

-- Add foreign key constraint
ALTER TABLE storage_position
ADD CONSTRAINT fk_position_box_plate
FOREIGN KEY (parent_box_plate_id)
REFERENCES storage_box_plate(id)
ON DELETE CASCADE;

-- Add hierarchy constraint (FR-022b)
ALTER TABLE storage_position
ADD CONSTRAINT chk_box_plate_requires_rack
CHECK (
    (parent_box_plate_id IS NULL) OR
    (parent_box_plate_id IS NOT NULL AND parent_rack_id IS NOT NULL)
);

-- Create index
CREATE INDEX idx_position_parent_box_plate ON storage_position(parent_box_plate_id);
```

#### Step 6: Update SampleStorageAssignment Enum

```sql
-- Update CHECK constraint to include 'box_plate'
ALTER TABLE sample_storage_assignment
DROP CONSTRAINT IF EXISTS chk_location_type;

ALTER TABLE sample_storage_assignment
ADD CONSTRAINT chk_location_type
CHECK (location_type IN ('device', 'shelf', 'rack', 'box_plate'));
```

#### Step 7: Update SampleStorageMovement Enum

```sql
-- Update CHECK constraints for previous and new location types
ALTER TABLE sample_storage_movement
DROP CONSTRAINT IF EXISTS chk_previous_location_type;

ALTER TABLE sample_storage_movement
ADD CONSTRAINT chk_previous_location_type
CHECK (previous_location_type IS NULL OR previous_location_type IN ('device', 'shelf', 'rack', 'box_plate'));

ALTER TABLE sample_storage_movement
DROP CONSTRAINT IF EXISTS chk_new_location_type;

ALTER TABLE sample_storage_movement
ADD CONSTRAINT chk_new_location_type
CHECK (new_location_type IS NULL OR new_location_type IN ('device', 'shelf', 'rack', 'box_plate'));
```

### Liquibase Changesets

All migration steps will be implemented as Liquibase changesets:

**File**:
`src/main/resources/liquibase/storage/010-restructure-rack-add-box-plate.xml`

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

    <!-- Changeset 010-2: Rename LABEL to NAME -->
    <changeSet id="ogc-149-rename-rack-label-to-name" author="openelis">
        <renameColumn tableName="storage_rack"
                      oldColumnName="label"
                      newColumnName="name"
                      columnDataType="VARCHAR(100)"/>
    </changeSet>

    <!-- Changeset 010-3: Create STORAGE_BOX_PLATE table -->
    <changeSet id="ogc-149-create-storage-box-plate" author="openelis">
        <createTable tableName="storage_box_plate">
            <column name="id" type="INTEGER">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="fhir_uuid" type="UUID">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="name" type="VARCHAR(100)">
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

        <addForeignKeyConstraint baseTableName="storage_box_plate"
                                 baseColumnNames="parent_rack_id"
                                 constraintName="fk_box_plate_rack"
                                 referencedTableName="storage_rack"
                                 referencedColumnNames="id"
                                 onDelete="RESTRICT"/>

        <addForeignKeyConstraint baseTableName="storage_box_plate"
                                 baseColumnNames="sys_user_id"
                                 constraintName="fk_box_plate_user"
                                 referencedTableName="system_user"
                                 referencedColumnNames="id"/>

        <addUniqueConstraint tableName="storage_box_plate"
                             columnNames="parent_rack_id, code"
                             constraintName="uq_box_plate_code_per_rack"/>

        <createSequence sequenceName="storage_box_plate_seq" startValue="1" incrementBy="1"/>

        <createIndex tableName="storage_box_plate" indexName="idx_box_plate_parent">
            <column name="parent_rack_id"/>
        </createIndex>

        <createIndex tableName="storage_box_plate" indexName="idx_box_plate_fhir_uuid">
            <column name="fhir_uuid"/>
        </createIndex>

        <createIndex tableName="storage_box_plate" indexName="idx_box_plate_active">
            <column name="active"/>
        </createIndex>
    </changeSet>

    <!-- Changeset 010-4: Add parent_box_plate_id to STORAGE_POSITION -->
    <changeSet id="ogc-149-add-box-plate-parent-to-position" author="openelis">
        <addColumn tableName="storage_position">
            <column name="parent_box_plate_id" type="INTEGER"/>
        </addColumn>

        <addForeignKeyConstraint baseTableName="storage_position"
                                 baseColumnNames="parent_box_plate_id"
                                 constraintName="fk_position_box_plate"
                                 referencedTableName="storage_box_plate"
                                 referencedColumnNames="id"
                                 onDelete="CASCADE"/>

        <createIndex tableName="storage_position" indexName="idx_position_parent_box_plate">
            <column name="parent_box_plate_id"/>
        </createIndex>

        <sql>
            ALTER TABLE storage_position
            ADD CONSTRAINT chk_box_plate_requires_rack
            CHECK (
                (parent_box_plate_id IS NULL) OR
                (parent_box_plate_id IS NOT NULL AND parent_rack_id IS NOT NULL)
            );
        </sql>
    </changeSet>

    <!-- Changeset 010-5: Update location_type enums -->
    <changeSet id="ogc-149-update-location-type-enums" author="openelis">
        <sql>
            ALTER TABLE sample_storage_assignment
            DROP CONSTRAINT IF EXISTS chk_location_type;

            ALTER TABLE sample_storage_assignment
            ADD CONSTRAINT chk_location_type
            CHECK (location_type IN ('device', 'shelf', 'rack', 'box_plate'));

            ALTER TABLE sample_storage_movement
            DROP CONSTRAINT IF EXISTS chk_previous_location_type;

            ALTER TABLE sample_storage_movement
            ADD CONSTRAINT chk_previous_location_type
            CHECK (previous_location_type IS NULL OR previous_location_type IN ('device', 'shelf', 'rack', 'box_plate'));

            ALTER TABLE sample_storage_movement
            DROP CONSTRAINT IF EXISTS chk_new_location_type;

            ALTER TABLE sample_storage_movement
            ADD CONSTRAINT chk_new_location_type
            CHECK (new_location_type IS NULL OR new_location_type IN ('device', 'shelf', 'rack', 'box_plate'));
        </sql>
    </changeSet>

</databaseChangeLog>
```

---

## Data Volume Impact

**Assumptions** (POC scope):

- Existing: 200 racks from Feature 001
- New: Average 5 boxes/plates per rack = 1,000 box/plates
- Positions: Remain same count (10,000), but some now reference box/plates

| Entity                  | Before (Feature 001) | After (OGC-149) | Delta                 |
| ----------------------- | -------------------- | --------------- | --------------------- |
| StorageRack             | 200 rows             | 200 rows        | 0 (simplified schema) |
| **StorageBoxPlate**     | 0 rows               | **1,000 rows**  | **+1,000** (new)      |
| StoragePosition         | 10,000 rows          | 10,000 rows     | 0 (added 1 column)    |
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
DROP TABLE IF EXISTS storage_box_plate CASCADE;
DROP SEQUENCE IF EXISTS storage_box_plate_seq;

-- 2. Remove parent_box_plate_id from positions
ALTER TABLE storage_position DROP COLUMN IF EXISTS parent_box_plate_id;

-- 3. Restore rack grid columns
ALTER TABLE storage_rack
ADD COLUMN rows INTEGER NOT NULL DEFAULT 0,
ADD COLUMN columns INTEGER NOT NULL DEFAULT 0,
ADD COLUMN position_schema_hint VARCHAR(50);

-- 4. Rename NAME back to LABEL
ALTER TABLE storage_rack RENAME COLUMN name TO label;

-- 5. Revert location_type enums
-- (Revert to 'device', 'shelf', 'rack' only)
```

**Note**: Feature 001 not in production, so rollback is unlikely to be needed.

---

## Summary

This data model enhancement adds the Box/Plate hierarchy level while maintaining
backward compatibility and flexibility. Key highlights:

- **StorageRack simplified**: Removes grid complexity, now a pure container
- **StorageBoxPlate introduced**: Captures grid dimensions, supports 6 standard
  presets + custom
- **StoragePosition extended**: Flexible 2-6 level hierarchy maintained
- **Migration**: Destructive (safe since Feature 001 not in production)
- **Storage impact**: Minimal (~50 KB for 1,000 box/plates)
- **FHIR compliance**: Full Location resource mapping with extensions
