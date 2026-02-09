# Data Model: Madagascar Analyzer Integration

**Feature**: 011-madagascar-analyzer-integration **Date**: 2026-01-22 **Spec
Reference**: [spec.md](spec.md)

---

## Overview

This data model extends the existing Feature 004 analyzer entities to support:

- HL7, RS232, and file-based protocol configurations
- Order export tracking with status management
- Enhanced instrument metadata with location history

**Design Principle**: All new entities use JPA/Hibernate annotations
(Constitution IV). Integration with legacy XML-mapped `Analyzer` entity follows
the manual relationship management pattern established in Feature 004.

---

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          EXISTING ENTITIES (Feature 004)                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────┐     ┌────────────────────┐     ┌─────────────────────┐   │
│   │   Analyzer   │────▶│AnalyzerConfiguration│────▶│  AnalyzerField     │   │
│   │  (XML-mapped)│     │   (JPA-annotated)   │     │  (JPA-annotated)   │   │
│   └──────┬───────┘     └────────────────────┘     └─────────────────────┘   │
│          │                                                                   │
│          │ 1:1 (manual relationship)                                        │
│          ▼                                                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                          NEW ENTITIES (Feature 011)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────────────┐                                                  │
│   │  InstrumentMetadata  │──────────────────────────────────────────┐       │
│   │   (JPA-annotated)    │                                          │       │
│   └──────────┬───────────┘                                          │       │
│              │ 1:N                                                   │       │
│              ▼                                                       │       │
│   ┌────────────────────────────┐     ┌───────────────────┐          │       │
│   │InstrumentLocationHistory   │     │   Organization    │◀─────────┘       │
│   │     (JPA-annotated)        │────▶│   (existing)      │                  │
│   └────────────────────────────┘     └───────────────────┘                  │
│                                                                             │
│   ┌──────────────────────┐     ┌─────────────────────────────┐              │
│   │SerialPortConfiguration│     │  FileImportConfiguration   │              │
│   │   (JPA-annotated)    │     │     (JPA-annotated)         │              │
│   └──────────────────────┘     └─────────────────────────────┘              │
│              │                              │                               │
│              └──────────────┬───────────────┘                               │
│                             │                                               │
│                    Links to Analyzer via analyzer_id                        │
│                                                                             │
│   ┌──────────────────────┐                                                  │
│   │     OrderExport      │                                                  │
│   │   (JPA-annotated)    │                                                  │
│   └──────────┬───────────┘                                                  │
│              │                                                              │
│              ├── Links to Sample                                            │
│              └── Links to Analyzer                                          │
│                                                                             │
│   ┌──────────────────────┐                                                  │
│   │   GeneXpertModule    │  (Post-deadline, for US-7)                       │
│   │   (JPA-annotated)    │                                                  │
│   └──────────────────────┘                                                  │
│                                                                             │
│   ┌──────────────────────┐                                                  │
│   │   MaintenanceEvent   │  (Post-deadline, for US-8)                       │
│   │   (JPA-annotated)    │                                                  │
│   └──────────────────────┘                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## New Entities

### 1. InstrumentMetadata

**Purpose**: Extended instrument information beyond basic Analyzer entity.
Captures installation, warranty, calibration, and location data.

**Relationship**: One-to-one with existing Analyzer entity (manual FK management
due to legacy XML mapping).

```java
@Entity
@Table(name = "instrument_metadata")
public class InstrumentMetadata extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "analyzer_id", nullable = false)
    private Integer analyzerId;  // Manual FK to Analyzer (XML-mapped)

    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "warranty_expiration")
    private LocalDate warrantyExpiration;

    @Column(name = "software_version", length = 50)
    private String softwareVersion;

    @Column(name = "calibration_due_date")
    private LocalDate calibrationDueDate;

    @Column(name = "service_status", length = 20)
    @Enumerated(EnumType.STRING)
    private ServiceStatus serviceStatus;  // OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @OneToMany(mappedBy = "instrumentMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("effectiveFrom DESC")
    private List<InstrumentLocationHistory> locationHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters/setters omitted for brevity
}

public enum ServiceStatus {
    OPERATIONAL,
    MAINTENANCE,
    OUT_OF_SERVICE
}
```

**Validation Rules**:

- `analyzerId` is required and must reference valid Analyzer
- `calibrationDueDate` generates UI warning if within 30 days of current date
- `serviceStatus` defaults to OPERATIONAL

---

### 2. InstrumentLocationHistory

**Purpose**: Historical record of instrument locations with effective dates.
Supports audit trail for instrument relocations.

**Relationship**: Many-to-one with InstrumentMetadata. Links to existing
Organization entity.

```java
@Entity
@Table(name = "instrument_location_history")
public class InstrumentLocationHistory extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_metadata_id", nullable = false)
    private InstrumentMetadata instrumentMetadata;

    @Column(name = "organization_id")
    private Integer organizationId;  // Manual FK to Organization (reuse existing hierarchy)

    @Column(name = "room_detail", length = 100)
    private String roomDetail;  // Optional room-level extension

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;  // NULL = current location

    @Column(name = "moved_by_user_id")
    private Integer movedByUserId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters/setters omitted for brevity
}
```

**Validation Rules**:

- `effectiveFrom` is required
- `effectiveTo` must be >= `effectiveFrom` if set
- When creating new location, previous location's `effectiveTo` is set
  automatically

**State Transitions** (on relocation):

1. Set previous location's `effectiveTo` = relocation date - 1 day
2. Create new location with `effectiveFrom` = relocation date
3. Log audit trail with `movedByUserId`

---

### 3. SerialPortConfiguration

**Purpose**: RS232 serial communication parameters for analyzers using serial
connections.

**Relationship**: One-to-one with Analyzer (via `analyzer_id`).

```java
@Entity
@Table(name = "serial_port_configuration")
public class SerialPortConfiguration extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "analyzer_id", nullable = false, unique = true)
    private Integer analyzerId;  // Manual FK to Analyzer

    @Column(name = "port_name", nullable = false, length = 50)
    private String portName;  // e.g., "/dev/ttyUSB0", "COM3"

    @Column(name = "baud_rate")
    private Integer baudRate = 9600;  // 9600, 19200, 38400, 57600, 115200

    @Column(name = "data_bits")
    private Integer dataBits = 8;  // 7 or 8

    @Column(name = "stop_bits", length = 10)
    @Enumerated(EnumType.STRING)
    private StopBits stopBits = StopBits.ONE;  // ONE, ONE_POINT_FIVE, TWO

    @Column(name = "parity", length = 10)
    @Enumerated(EnumType.STRING)
    private Parity parity = Parity.NONE;  // NONE, EVEN, ODD, MARK, SPACE

    @Column(name = "flow_control", length = 20)
    @Enumerated(EnumType.STRING)
    private FlowControl flowControl = FlowControl.NONE;  // NONE, RTS_CTS, XON_XOFF

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters/setters omitted for brevity
}

public enum StopBits {
    ONE, ONE_POINT_FIVE, TWO
}

public enum Parity {
    NONE, EVEN, ODD, MARK, SPACE
}

public enum FlowControl {
    NONE, RTS_CTS, XON_XOFF
}
```

**Validation Rules**:

- `portName` is required and must be valid system port name
- `baudRate` must be one of: 9600, 19200, 38400, 57600, 115200
- `dataBits` must be 7 or 8
- Unique constraint on `analyzer_id` (one serial config per analyzer)

---

### 4. FileImportConfiguration

**Purpose**: Configuration for file-based result import (directory watching, CSV
parsing).

**Relationship**: One-to-one with Analyzer (via `analyzer_id`).

```java
@Entity
@Table(name = "file_import_configuration")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class FileImportConfiguration extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "analyzer_id", nullable = false, unique = true)
    private Integer analyzerId;  // Manual FK to Analyzer

    @Column(name = "import_directory", nullable = false, length = 255)
    private String importDirectory;  // e.g., "/data/analyzer-imports/quantstudio"

    @Column(name = "file_pattern", length = 100)
    private String filePattern = "*.csv";  // Glob pattern

    @Column(name = "archive_directory", length = 255)
    private String archiveDirectory;  // Move processed files here

    @Column(name = "error_directory", length = 255)
    private String errorDirectory;  // Move failed files here

    @Type(type = "jsonb")
    @Column(name = "column_mappings", columnDefinition = "jsonb")
    private Map<String, String> columnMappings;  // {"Sample_ID": "sampleId", "Result": "result"}

    @Column(name = "delimiter", length = 10)
    private String delimiter = ",";  // CSV delimiter

    @Column(name = "has_header")
    private Boolean hasHeader = true;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters/setters omitted for brevity
}
```

**Validation Rules**:

- `importDirectory` is required and must be valid directory path
- `importDirectory` must have read permissions
- `archiveDirectory` and `errorDirectory` must have write permissions if
  specified
- `columnMappings` must include at least sample ID mapping
- Unique constraint on `analyzer_id`

---

### 5. OrderExport

**Purpose**: Tracks test orders exported to analyzers with status management and
retry logic.

**Relationship**: Links to Sample and Analyzer entities.

```java
@Entity
@Table(name = "order_export")
public class OrderExport extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "sample_id", nullable = false)
    private Integer sampleId;  // Manual FK to Sample

    @Column(name = "analyzer_id", nullable = false)
    private Integer analyzerId;  // Manual FK to Analyzer

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private OrderExportStatus status = OrderExportStatus.PENDING;

    @Column(name = "message_type", length = 10)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;  // ASTM, HL7

    @Column(name = "message_content", columnDefinition = "TEXT")
    private String messageContent;  // Actual message sent

    @Column(name = "sent_timestamp")
    private Timestamp sentTimestamp;

    @Column(name = "acknowledged_timestamp")
    private Timestamp acknowledgedTimestamp;

    @Column(name = "results_received_timestamp")
    private Timestamp resultsReceivedTimestamp;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Getters/setters omitted for brevity
}

public enum OrderExportStatus {
    PENDING,      // Created, not yet sent
    SENT,         // Message sent to analyzer
    ACKNOWLEDGED, // Analyzer acknowledged receipt
    RESULTS_RECEIVED,  // Results matched to this order
    FAILED,       // Export failed after retries
    EXPIRED,      // No results received within timeout
    CANCELLED     // Order cancelled by user
}

public enum MessageType {
    ASTM, HL7
}
```

**Validation Rules**:

- `sampleId` and `analyzerId` are required
- `status` transitions follow state machine (see below)
- `retryCount` max is 3 (configurable)

**State Machine**:

```
PENDING ──send──▶ SENT ──ack──▶ ACKNOWLEDGED ──result──▶ RESULTS_RECEIVED
    │               │                │
    │             timeout          timeout
    │               ▼                ▼
    │            FAILED           EXPIRED
    │               │
    │          retry < 3?
    │               │
    └──retry──────◀─┘

Any state ──cancel──▶ CANCELLED
```

---

### 6. GeneXpertModule (Post-Deadline)

**Purpose**: Module-level tracking for GeneXpert instruments. Deferred to
post-deadline per spec.

```java
@Entity
@Table(name = "genexpert_module")
public class GeneXpertModule extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "instrument_metadata_id", nullable = false)
    private String instrumentMetadataId;  // FK to InstrumentMetadata

    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;  // 1, 2, 3, 4

    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private ModuleStatus status;  // ENABLED, DISABLED, REPLACED

    @Column(name = "test_count")
    private Integer testCount = 0;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "last_test_date")
    private Timestamp lastTestDate;

    @Column(name = "replacement_date")
    private LocalDate replacementDate;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    // Getters/setters omitted for brevity
}

public enum ModuleStatus {
    ENABLED, DISABLED, REPLACED
}
```

---

### 7. MaintenanceEvent (Post-Deadline)

**Purpose**: Calibration, preventative, and curative maintenance records.
Deferred to post-deadline per spec.

```java
@Entity
@Table(name = "maintenance_event")
public class MaintenanceEvent extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "instrument_metadata_id", nullable = false)
    private String instrumentMetadataId;  // FK to InstrumentMetadata

    @Column(name = "event_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MaintenanceType eventType;  // CALIBRATION, PREVENTATIVE, CURATIVE

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "performed_by_user_id")
    private Integer performedByUserId;

    @Column(name = "results", columnDefinition = "TEXT")
    private String results;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "next_scheduled_date")
    private LocalDate nextScheduledDate;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    // Getters/setters omitted for brevity
}

public enum MaintenanceType {
    CALIBRATION, PREVENTATIVE, CURATIVE
}
```

---

## Liquibase Changesets

All schema changes will be placed in `src/main/resources/liquibase/3.8.x.x/`
(version TBD).

### Changeset 001: Instrument Metadata Table

```xml
<changeSet id="011-001-create-instrument-metadata-table" author="madagascar-integration">
    <createTable tableName="instrument_metadata">
        <column name="id" type="VARCHAR(36)">
            <constraints primaryKey="true"/>
        </column>
        <column name="analyzer_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="installation_date" type="DATE"/>
        <column name="warranty_expiration" type="DATE"/>
        <column name="software_version" type="VARCHAR(50)"/>
        <column name="calibration_due_date" type="DATE"/>
        <column name="service_status" type="VARCHAR(20)" defaultValue="OPERATIONAL"/>
        <column name="notes" type="TEXT"/>
        <column name="fhir_uuid" type="UUID">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="sys_user_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
    </createTable>

    <addForeignKeyConstraint
        baseTableName="instrument_metadata"
        baseColumnNames="analyzer_id"
        referencedTableName="analyzer"
        referencedColumnNames="id"
        constraintName="fk_instrument_metadata_analyzer"/>

    <createIndex tableName="instrument_metadata" indexName="idx_instrument_metadata_analyzer">
        <column name="analyzer_id"/>
    </createIndex>

    <rollback>
        <dropTable tableName="instrument_metadata"/>
    </rollback>
</changeSet>
```

### Changeset 002: Order Export Table

```xml
<changeSet id="011-002-create-order-export-table" author="madagascar-integration">
    <createTable tableName="order_export">
        <column name="id" type="VARCHAR(36)">
            <constraints primaryKey="true"/>
        </column>
        <column name="sample_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="analyzer_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="status" type="VARCHAR(20)">
            <constraints nullable="false"/>
        </column>
        <column name="message_type" type="VARCHAR(10)"/>
        <column name="message_content" type="TEXT"/>
        <column name="sent_timestamp" type="TIMESTAMP"/>
        <column name="acknowledged_timestamp" type="TIMESTAMP"/>
        <column name="results_received_timestamp" type="TIMESTAMP"/>
        <column name="retry_count" type="INT" defaultValueNumeric="0"/>
        <column name="error_message" type="TEXT"/>
        <column name="fhir_uuid" type="UUID">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="sys_user_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
    </createTable>

    <addForeignKeyConstraint
        baseTableName="order_export"
        baseColumnNames="sample_id"
        referencedTableName="sample"
        referencedColumnNames="id"
        constraintName="fk_order_export_sample"/>

    <addForeignKeyConstraint
        baseTableName="order_export"
        baseColumnNames="analyzer_id"
        referencedTableName="analyzer"
        referencedColumnNames="id"
        constraintName="fk_order_export_analyzer"/>

    <createIndex tableName="order_export" indexName="idx_order_export_sample">
        <column name="sample_id"/>
    </createIndex>

    <createIndex tableName="order_export" indexName="idx_order_export_analyzer_status">
        <column name="analyzer_id"/>
        <column name="status"/>
    </createIndex>

    <rollback>
        <dropTable tableName="order_export"/>
    </rollback>
</changeSet>
```

### Changeset 003: Serial Port Configuration Table

```xml
<changeSet id="011-003-create-serial-port-configuration-table" author="madagascar-integration">
    <createTable tableName="serial_port_configuration">
        <column name="id" type="VARCHAR(36)">
            <constraints primaryKey="true"/>
        </column>
        <column name="analyzer_id" type="INT">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="port_name" type="VARCHAR(50)">
            <constraints nullable="false"/>
        </column>
        <column name="baud_rate" type="INT" defaultValueNumeric="9600"/>
        <column name="data_bits" type="INT" defaultValueNumeric="8"/>
        <column name="stop_bits" type="VARCHAR(10)" defaultValue="ONE"/>
        <column name="parity" type="VARCHAR(10)" defaultValue="NONE"/>
        <column name="flow_control" type="VARCHAR(20)" defaultValue="NONE"/>
        <column name="active" type="BOOLEAN" defaultValueBoolean="true"/>
        <column name="fhir_uuid" type="UUID">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="sys_user_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
    </createTable>

    <addForeignKeyConstraint
        baseTableName="serial_port_configuration"
        baseColumnNames="analyzer_id"
        referencedTableName="analyzer"
        referencedColumnNames="id"
        constraintName="fk_serial_port_config_analyzer"/>

    <rollback>
        <dropTable tableName="serial_port_configuration"/>
    </rollback>
</changeSet>
```

### Changeset 004: File Import Configuration Table

```xml
<changeSet id="011-004-create-file-import-configuration-table" author="madagascar-integration">
    <createTable tableName="file_import_configuration">
        <column name="id" type="VARCHAR(36)">
            <constraints primaryKey="true"/>
        </column>
        <column name="analyzer_id" type="INT">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="import_directory" type="VARCHAR(255)">
            <constraints nullable="false"/>
        </column>
        <column name="file_pattern" type="VARCHAR(100)" defaultValue="*.csv"/>
        <column name="archive_directory" type="VARCHAR(255)"/>
        <column name="error_directory" type="VARCHAR(255)"/>
        <column name="column_mappings" type="JSONB"/>
        <column name="delimiter" type="VARCHAR(10)" defaultValue=","/>
        <column name="has_header" type="BOOLEAN" defaultValueBoolean="true"/>
        <column name="active" type="BOOLEAN" defaultValueBoolean="true"/>
        <column name="fhir_uuid" type="UUID">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="sys_user_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
    </createTable>

    <addForeignKeyConstraint
        baseTableName="file_import_configuration"
        baseColumnNames="analyzer_id"
        referencedTableName="analyzer"
        referencedColumnNames="id"
        constraintName="fk_file_import_config_analyzer"/>

    <rollback>
        <dropTable tableName="file_import_configuration"/>
    </rollback>
</changeSet>
```

### Changeset 005: Instrument Location History Table

```xml
<changeSet id="011-005-create-instrument-location-history-table" author="madagascar-integration">
    <createTable tableName="instrument_location_history">
        <column name="id" type="VARCHAR(36)">
            <constraints primaryKey="true"/>
        </column>
        <column name="instrument_metadata_id" type="VARCHAR(36)">
            <constraints nullable="false"/>
        </column>
        <column name="organization_id" type="INT"/>
        <column name="room_detail" type="VARCHAR(100)"/>
        <column name="effective_from" type="DATE">
            <constraints nullable="false"/>
        </column>
        <column name="effective_to" type="DATE"/>
        <column name="moved_by_user_id" type="INT"/>
        <column name="notes" type="TEXT"/>
        <column name="fhir_uuid" type="UUID">
            <constraints nullable="false" unique="true"/>
        </column>
        <column name="sys_user_id" type="INT">
            <constraints nullable="false"/>
        </column>
        <column name="lastupdated" type="TIMESTAMP" defaultValueComputed="NOW()"/>
    </createTable>

    <addForeignKeyConstraint
        baseTableName="instrument_location_history"
        baseColumnNames="instrument_metadata_id"
        referencedTableName="instrument_metadata"
        referencedColumnNames="id"
        constraintName="fk_location_history_instrument"/>

    <addForeignKeyConstraint
        baseTableName="instrument_location_history"
        baseColumnNames="organization_id"
        referencedTableName="organization"
        referencedColumnNames="id"
        constraintName="fk_location_history_organization"/>

    <createIndex tableName="instrument_location_history" indexName="idx_location_history_instrument">
        <column name="instrument_metadata_id"/>
    </createIndex>

    <createIndex tableName="instrument_location_history" indexName="idx_location_history_effective">
        <column name="effective_from"/>
        <column name="effective_to"/>
    </createIndex>

    <rollback>
        <dropTable tableName="instrument_location_history"/>
    </rollback>
</changeSet>
```

---

## Data Model Summary

| Entity                    | Table                       | Purpose                | Milestone     |
| ------------------------- | --------------------------- | ---------------------- | ------------- |
| InstrumentMetadata        | instrument_metadata         | Extended analyzer info | M16           |
| InstrumentLocationHistory | instrument_location_history | Location audit trail   | M16           |
| SerialPortConfiguration   | serial_port_configuration   | RS232 settings         | M2            |
| FileImportConfiguration   | file_import_configuration   | File import settings   | M3            |
| OrderExport               | order_export                | Order tracking         | M15           |
| GeneXpertModule           | genexpert_module            | Module tracking        | Post-deadline |
| MaintenanceEvent          | maintenance_event           | Maintenance records    | Post-deadline |

---

**Data Model Created**: 2026-01-22 **Review Required**: Constitution compliance
(JPA annotations, layered architecture)
