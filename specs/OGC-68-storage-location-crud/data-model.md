# Data Model: Storage Location Management & Configuration

**Feature**: OGC-68-storage-location-crud  
**Date**: 2025-12-11  
**Spec**: [spec.md](./spec.md)

## Entity Updates

### StorageDevice (UPDATE)

**Table**: `storage_device`

| Column                     | Type            | Constraints                    | Notes                                         |
| -------------------------- | --------------- | ------------------------------ | --------------------------------------------- |
| id                         | INTEGER         | PK, SEQUENCE                   | Existing                                      |
| fhir_uuid                  | UUID            | NOT NULL, UNIQUE               | Existing                                      |
| name                       | VARCHAR(255)    | NOT NULL                       | Existing                                      |
| code                       | VARCHAR(10)     | NOT NULL                       | Existing                                      |
| type                       | VARCHAR(20)     | NOT NULL                       | Existing (freezer/refrigerator/cabinet/other) |
| temperature_setting        | DECIMAL(5,2)    |                                | Existing                                      |
| capacity_limit             | INTEGER         |                                | Existing                                      |
| active                     | BOOLEAN         | NOT NULL                       | Existing                                      |
| parent_room_id             | INTEGER         | FK → storage_room.id, NOT NULL | Existing                                      |
| sys_user_id                | INTEGER         | NOT NULL                       | Existing                                      |
| **ip_address**             | **VARCHAR(45)** |                                | **NEW** - IPv4/IPv6 address                   |
| **port**                   | **INTEGER**     |                                | **NEW** - 1-65535                             |
| **communication_protocol** | **VARCHAR(20)** | DEFAULT 'BACnet'               | **NEW**                                       |

### Liquibase Changeset

**File**:
`src/main/resources/liquibase/3.3.x.x/023-storage-device-connectivity.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.0.xsd">

    <changeSet id="storage-151-001-add-device-connectivity-columns" author="dev-team">
        <comment>OGC-68: Add connectivity configuration fields to storage_device table</comment>
        <addColumn tableName="storage_device">
            <column name="ip_address" type="VARCHAR(45)">
                <constraints nullable="true"/>
            </column>
            <column name="port" type="INTEGER">
                <constraints nullable="true"/>
            </column>
            <column name="communication_protocol" type="VARCHAR(20)" defaultValue="BACnet">
                <constraints nullable="true"/>
            </column>
        </addColumn>
        <rollback>
            <dropColumn tableName="storage_device" columnName="ip_address"/>
            <dropColumn tableName="storage_device" columnName="port"/>
            <dropColumn tableName="storage_device" columnName="communication_protocol"/>
        </rollback>
    </changeSet>

    <changeSet id="storage-151-002-add-port-check-constraint" author="dev-team">
        <comment>OGC-68: Add check constraint for valid port range</comment>
        <sql>
            ALTER TABLE storage_device
            ADD CONSTRAINT chk_storage_device_port_range
            CHECK (port IS NULL OR (port >= 1 AND port &lt;= 65535));
        </sql>
        <rollback>
            <sql>ALTER TABLE storage_device DROP CONSTRAINT chk_storage_device_port_range;</sql>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

## Entity Relationships

```
┌─────────────────┐
│  StorageRoom    │
│  - id           │
│  - name         │
│  - code         │
│  - description  │
│  - active       │
└────────┬────────┘
         │ 1:N
         ▼
┌─────────────────────────────┐
│  StorageDevice              │
│  - id                       │
│  - name, code, type         │
│  - temperatureSetting       │
│  - capacityLimit            │
│  - active                   │
│  - parentRoom (FK)          │
│  - **ipAddress**       NEW  │
│  - **port**            NEW  │
│  - **communicationProtocol**│
└────────┬────────────────────┘
         │ 1:N
         ▼
┌─────────────────┐
│  StorageShelf   │
│  - id           │
│  - label, code  │
│  - capacityLimit│
│  - active       │
│  - parentDevice │
└────────┬────────┘
         │ 1:N
         ▼
┌─────────────────┐
│  StorageRack    │
│  - id           │
│  - label, code  │
│  - active       │
│  - parentShelf  │
└─────────────────┘
```

## Deletion Validation Rules

### Referential Integrity Matrix

| Entity | Can Delete If...                           | Blocked By                       |
| ------ | ------------------------------------------ | -------------------------------- |
| Room   | No child Devices AND No direct assignments | Devices, SampleStorageAssignment |
| Device | No child Shelves AND No direct assignments | Shelves, SampleStorageAssignment |
| Shelf  | No child Racks AND No direct assignments   | Racks, SampleStorageAssignment   |
| Rack   | No child Boxes AND No direct assignments   | Boxes, SampleStorageAssignment   |

### Validation Flow

```java
// DeletionValidationResult.java (NEW)
public class DeletionValidationResult {
    private boolean allowed;
    private String errorCode;      // REFERENTIAL_INTEGRITY_VIOLATION, ACTIVE_ASSIGNMENTS
    private String errorMessage;
    private int dependentCount;

    public static DeletionValidationResult allowed() { ... }
    public static DeletionValidationResult blocked(String code, String message, int count) { ... }
}
```

## FHIR Mapping Updates

### StorageDevice → FHIR Location (Updated)

| OpenELIS Field        | FHIR Field                                           | Type    |
| --------------------- | ---------------------------------------------------- | ------- |
| ipAddress             | extension[device-ip-address].valueString             | String  |
| port                  | extension[device-port].valueInteger                  | Integer |
| communicationProtocol | extension[device-communication-protocol].valueString | String  |

### Extension URLs

```java
private static final String EXT_DEVICE_IP_ADDRESS =
    "http://openelis.org/fhir/extension/device-ip-address";
private static final String EXT_DEVICE_PORT =
    "http://openelis.org/fhir/extension/device-port";
private static final String EXT_DEVICE_PROTOCOL =
    "http://openelis.org/fhir/extension/device-communication-protocol";
```
