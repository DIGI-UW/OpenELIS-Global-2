# Data Model: ASTM Analyzer Field Mapping

**Feature**: 004-astm-analyzer-mapping  
**Date**: 2025-11-14  
**Status**: Complete

This document defines the data model for the ASTM analyzer field mapping
feature, including entity definitions, relationships, validation rules, and
state transitions.

## Entity Overview

The feature introduces 5 new entities and extends 1 existing entity:

1. **AnalyzerConfiguration** - Extends existing `Analyzer` with connection
   settings (IP, Port, Protocol)
2. **AnalyzerField** - Represents fields/codes emitted by analyzers (test codes,
   units, qualitative values)
3. **AnalyzerFieldMapping** - Maps analyzer fields to OpenELIS fields
   (test-level, result-level, metadata)
4. **QualitativeResultMapping** - Maps analyzer qualitative values to OpenELIS
   coded results (many-to-one)
5. **UnitMapping** - Maps analyzer units to OpenELIS canonical units (with
   optional conversion factors)
6. **AnalyzerError** - Stores failed/unmapped analyzer messages for error
   dashboard

## Entity Definitions

### 1. AnalyzerConfiguration

**Purpose**: Extends existing `Analyzer` entity with connection configuration
(IP address, port, protocol version) without modifying legacy XML mappings.

**Table**: `analyzer_configuration`

**Relationships**:

- One-to-One with `Analyzer` (references legacy `analyzer` table)

**Fields**:

| Field                 | Type        | Constraints                      | Description                               |
| --------------------- | ----------- | -------------------------------- | ----------------------------------------- |
| `id`                  | VARCHAR(36) | PK, NOT NULL                     | Primary key (UUID)                        |
| `analyzer_id`         | VARCHAR(36) | FK, NOT NULL, UNIQUE             | References `analyzer.id`                  |
| `ip_address`          | VARCHAR(15) | NULL                             | IPv4 address (e.g., "192.168.1.10")       |
| `port`                | INTEGER     | NULL                             | Port number (1-65535)                     |
| `protocol_version`    | VARCHAR(20) | NOT NULL, DEFAULT 'ASTM LIS2-A2' | Protocol version                          |
| `test_unit_ids`       | TEXT[]      | NULL                             | Array of test unit IDs (PostgreSQL array) |
| `status`              | VARCHAR(20) | NOT NULL, DEFAULT 'SETUP'        | Unified analyzer status (enum)            |
| `last_activated_date` | TIMESTAMP   | NULL                             | Date when analyzer was last activated     |
| `last_updated`        | TIMESTAMP   | NOT NULL                         | Audit timestamp (from BaseObject)         |
| `sys_user_id`         | VARCHAR(36) | NULL                             | Audit user ID (from BaseObject)           |

**Validation Rules**:

- `ip_address`: Must be valid IPv4 format if provided (regex:
  `^(\d{1,3}\.){3}\d{1,3}$`)
- `port`: Must be in range 1-65535 if provided
- `ip_address` and `port` must both be provided or both be NULL (connection
  configuration is all-or-nothing)
- `status`: Must be one of: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING,
  OFFLINE
- `last_activated_date`: Auto-populated when status transitions to ACTIVE

**Unified Status Field**:

The `status` field combines lifecycle stage and operational status into a single
unified value, replacing the previous separate `active` boolean and
`lifecycle_stage` enum.

**Status Enum Values**:

- `INACTIVE`: Manually set by user (overrides all other criteria), analyzer does
  not process messages
- `SETUP`: Analyzer added but no mappings configured yet, analyzer does not
  process messages
- `VALIDATION`: Mappings being created/tested, not all required mappings
  activated, analyzer does not process messages
- `ACTIVE`: All required mappings configured and activated, analyzer
  automatically processes incoming messages into results
- `ERROR_PENDING`: Active analyzer with unacknowledged errors in error queue,
  analyzer continues processing but errors require attention
- `OFFLINE`: Connection test failed, analyzer unreachable, analyzer does not
  process messages

**Automatic Status Transitions**:

- **SETUP → VALIDATION**: Automatic when first mapping created
- **VALIDATION → ACTIVE**: Automatic when all required mappings activated
- **ACTIVE → ERROR_PENDING**: Automatic when unacknowledged errors detected
- **ACTIVE → OFFLINE**: Automatic when connection test fails
- **ERROR_PENDING → ACTIVE**: Automatic when all errors acknowledged
- **OFFLINE → ACTIVE**: Automatic when connection restored
- **Any → INACTIVE**: Manual override by user (available at any time)

**JPA Entity**:

```java
@Entity
@Table(name = "analyzer_configuration")
public class AnalyzerConfiguration extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @OneToOne
    @JoinColumn(name = "analyzer_id", nullable = false, unique = true)
    private Analyzer analyzer;

    @Column(name = "ip_address", length = 15)
    @Pattern(regexp = "^(\d{1,3}\.){3}\d{1,3}$", message = "Invalid IPv4 address")
    private String ipAddress;

    @Column(name = "port")
    @Min(1) @Max(65535)
    private Integer port;

    @Column(name = "protocol_version", length = 20, nullable = false)
    private String protocolVersion = "ASTM LIS2-A2";

    @Column(name = "test_unit_ids", columnDefinition = "TEXT[]")
    private String[] testUnitIds;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private AnalyzerStatus status = AnalyzerStatus.SETUP;

    @Column(name = "last_activated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivatedDate;
}

/**
 * Enum for unified analyzer status (combines lifecycle stage and operational status)
 */
public enum AnalyzerStatus {
    INACTIVE,      // Manually set by user (override)
    SETUP,         // Analyzer added, no mappings yet
    VALIDATION,    // Mappings in progress, not all activated
    ACTIVE,        // Fully operational, auto-processing results
    ERROR_PENDING, // Active but has unacknowledged errors
    OFFLINE        // Connection test failed, unreachable
}
```

### 2. AnalyzerField

**Purpose**: Represents a specific field or code emitted by an analyzer (e.g.,
test code, measurement ID, qualifier field) that can be mapped to OpenELIS
concepts.

**Table**: `analyzer_field`

**Relationships**:

- Many-to-One with `Analyzer` (via `analyzer_id`)

**Fields**:

| Field          | Type         | Constraints            | Description                                                                      |
| -------------- | ------------ | ---------------------- | -------------------------------------------------------------------------------- | --- | --------- |
| `id`           | VARCHAR(36)  | PK, NOT NULL           | Primary key (UUID)                                                               |
| `analyzer_id`  | VARCHAR(36)  | FK, NOT NULL           | References `analyzer.id`                                                         |
| `field_name`   | VARCHAR(255) | NOT NULL               | Analyzer field name (e.g., "GLUCOSE", "HIV")                                     |
| `astm_ref`     | VARCHAR(50)  | NULL                   | ASTM segment reference (e.g., "O                                                 | 1   | GLUCOSE") |
| `field_type`   | VARCHAR(20)  | NOT NULL               | Type: NUMERIC, QUALITATIVE, CONTROL_TEST, MELTING_POINT, DATE_TIME, TEXT, CUSTOM |
| `unit`         | VARCHAR(50)  | NULL                   | Unit (for numeric fields, e.g., "mg/dL", "mmol/L")                               |
| `is_active`    | BOOLEAN      | NOT NULL, DEFAULT true | Whether field is active                                                          |
| `last_updated` | TIMESTAMP    | NOT NULL               | Audit timestamp                                                                  |
| `sys_user_id`  | VARCHAR(36)  | NULL                   | Audit user ID                                                                    |

**Validation Rules**:

- `field_name`: Required, 1-255 characters
- `field_type`: Must be one of: NUMERIC, QUALITATIVE, CONTROL_TEST,
  MELTING_POINT, DATE_TIME, TEXT, CUSTOM
- `unit`: Required if `field_type` is NUMERIC
- `unit`: Must be NULL if `field_type` is not NUMERIC

**State Transitions**:

- `is_active`: `true` → `false` (field disabled/retired)
- `is_active`: `false` → `true` (field re-enabled)

**JPA Entity**:

```java
@Entity
@Table(name = "analyzer_field")
public class AnalyzerField extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @Column(name = "field_name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String fieldName;

    @Column(name = "astm_ref", length = 50)
    private String astmRef;

    @Column(name = "field_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum FieldType {
        NUMERIC, QUALITATIVE, CONTROL_TEST, MELTING_POINT, DATE_TIME, TEXT, CUSTOM
    }
}
```

### 3. AnalyzerFieldMapping

**Purpose**: Represents the mapping configuration between an `AnalyzerField` and
one or more OpenELIS field entries, including mapping type and activation state.

**Table**: `analyzer_field_mapping`

**Relationships**:

- Many-to-One with `AnalyzerField` (via `analyzer_field_id`)
- Many-to-One with OpenELIS entities (via `openelis_field_id` and
  `openelis_field_type`)

**Fields**:

| Field                      | Type        | Constraints             | Description                                                  |
| -------------------------- | ----------- | ----------------------- | ------------------------------------------------------------ |
| `id`                       | VARCHAR(36) | PK, NOT NULL            | Primary key (UUID)                                           |
| `analyzer_field_id`        | VARCHAR(36) | FK, NOT NULL            | References `analyzer_field.id`                               |
| `openelis_field_id`        | VARCHAR(36) | NOT NULL                | OpenELIS field ID (test, analyte, result field, etc.)        |
| `openelis_field_type`      | VARCHAR(20) | NOT NULL                | Type: TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT |
| `mapping_type`             | VARCHAR(20) | NOT NULL                | Type: TEST_LEVEL, RESULT_LEVEL, METADATA                     |
| `is_required`              | BOOLEAN     | NOT NULL, DEFAULT false | Whether mapping is required for analyzer activation          |
| `is_active`                | BOOLEAN     | NOT NULL, DEFAULT false | Whether mapping is active (draft → active workflow)          |
| `specimen_type_constraint` | VARCHAR(50) | NULL                    | Optional specimen type constraint                            |
| `panel_constraint`         | VARCHAR(50) | NULL                    | Optional panel constraint                                    |
| `last_updated`             | TIMESTAMP   | NOT NULL                | Audit timestamp                                              |
| `sys_user_id`              | VARCHAR(36) | NULL                    | Audit user ID                                                |

**Validation Rules**:

- `openelis_field_type`: Must be one of: TEST, PANEL, RESULT, ORDER, SAMPLE, QC,
  METADATA, UNIT
- `mapping_type`: Must be one of: TEST_LEVEL, RESULT_LEVEL, METADATA
- Type compatibility: `analyzer_field.field_type` must be compatible with
  `openelis_field_type` (validated in service layer)
- Required mappings: At least one mapping with `is_required=true` must exist for
  each analyzer (Sample ID, Test Code, Result Value)

**State Transitions**:

- `is_active`: `false` → `true` (mapping activated, requires confirmation for
  active analyzers)
- `is_active`: `true` → `false` (mapping disabled/retired)

**JPA Entity**:

```java
@Entity
@Table(name = "analyzer_field_mapping")
public class AnalyzerFieldMapping extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_field_id", nullable = false)
    private AnalyzerField analyzerField;

    @Column(name = "openelis_field_id", nullable = false, length = 36)
    @NotNull
    private String openelisFieldId;

    @Column(name = "openelis_field_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private OpenELISFieldType openelisFieldType;

    @Column(name = "mapping_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private MappingType mappingType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "specimen_type_constraint", length = 50)
    private String specimenTypeConstraint;

    @Column(name = "panel_constraint", length = 50)
    private String panelConstraint;

    public enum OpenELISFieldType {
        TEST, PANEL, RESULT, ORDER, SAMPLE, QC, METADATA, UNIT
    }

    public enum MappingType {
        TEST_LEVEL, RESULT_LEVEL, METADATA
    }
}
```

### 4. QualitativeResultMapping

**Purpose**: Represents mapping of instrument-specific qualitative values
(strings or codes) to canonical OpenELIS-coded results, supporting many-to-one
mapping (multiple analyzer values → single OpenELIS code).

**Table**: `qualitative_result_mapping`

**Relationships**:

- Many-to-One with `AnalyzerField` (via `analyzer_field_id`)

**Fields**:

| Field               | Type         | Constraints             | Description                                               |
| ------------------- | ------------ | ----------------------- | --------------------------------------------------------- |
| `id`                | VARCHAR(36)  | PK, NOT NULL            | Primary key (UUID)                                        |
| `analyzer_field_id` | VARCHAR(36)  | FK, NOT NULL            | References `analyzer_field.id`                            |
| `analyzer_value`    | VARCHAR(100) | NOT NULL                | Analyzer qualitative value (e.g., "POS", "+", "Reactive") |
| `openelis_code`     | VARCHAR(100) | NOT NULL                | OpenELIS coded result (e.g., "POSITIVE", "NEGATIVE")      |
| `is_default`        | BOOLEAN      | NOT NULL, DEFAULT false | Default code for unmapped values                          |
| `last_updated`      | TIMESTAMP    | NOT NULL                | Audit timestamp                                           |
| `sys_user_id`       | VARCHAR(36)  | NULL                    | Audit user ID                                             |

**Validation Rules**:

- `analyzer_value`: Required, 1-100 characters
- `openelis_code`: Required, 1-100 characters
- `analyzer_field.field_type` must be QUALITATIVE
- Unique constraint: `(analyzer_field_id, analyzer_value)` - one mapping per
  analyzer value per field

**State Transitions**: N/A (static mappings)

**JPA Entity**:

```java
@Entity
@Table(name = "qualitative_result_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"analyzer_field_id", "analyzer_value"}))
public class QualitativeResultMapping extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_field_id", nullable = false)
    private AnalyzerField analyzerField;

    @Column(name = "analyzer_value", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String analyzerValue;

    @Column(name = "openelis_code", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String openelisCode;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
```

### 5. UnitMapping

**Purpose**: Represents mapping of analyzer-reported units to OpenELIS canonical
units, including optional conversion factors for unit mismatches.

**Table**: `unit_mapping`

**Relationships**:

- Many-to-One with `AnalyzerField` (via `analyzer_field_id`)

**Fields**:

| Field                | Type          | Constraints             | Description                                          |
| -------------------- | ------------- | ----------------------- | ---------------------------------------------------- |
| `id`                 | VARCHAR(36)   | PK, NOT NULL            | Primary key (UUID)                                   |
| `analyzer_field_id`  | VARCHAR(36)   | FK, NOT NULL            | References `analyzer_field.id`                       |
| `analyzer_unit`      | VARCHAR(50)   | NOT NULL                | Analyzer unit (e.g., "mg/dL")                        |
| `openelis_unit`      | VARCHAR(50)   | NOT NULL                | OpenELIS canonical unit (e.g., "mmol/L")             |
| `conversion_factor`  | DECIMAL(10,6) | NULL                    | Conversion factor (e.g., 0.0555 for mg/dL → mmol/L)  |
| `reject_if_mismatch` | BOOLEAN       | NOT NULL, DEFAULT false | Reject if units don't match and no conversion factor |
| `last_updated`       | TIMESTAMP     | NOT NULL                | Audit timestamp                                      |
| `sys_user_id`        | VARCHAR(36)   | NULL                    | Audit user ID                                        |

**Validation Rules**:

- `analyzer_unit`: Required, 1-50 characters
- `openelis_unit`: Required, 1-50 characters
- `analyzer_field.field_type` must be NUMERIC
- `conversion_factor`: Required if `analyzer_unit` != `openelis_unit` and
  `reject_if_mismatch=false`
- Unique constraint: `(analyzer_field_id, analyzer_unit)` - one mapping per
  analyzer unit per field

**State Transitions**: N/A (static mappings)

**JPA Entity**:

```java
@Entity
@Table(name = "unit_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"analyzer_field_id", "analyzer_unit"}))
public class UnitMapping extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_field_id", nullable = false)
    private AnalyzerField analyzerField;

    @Column(name = "analyzer_unit", nullable = false, length = 50)
    @NotNull
    @Size(min = 1, max = 50)
    private String analyzerUnit;

    @Column(name = "openelis_unit", nullable = false, length = 50)
    @NotNull
    @Size(min = 1, max = 50)
    private String openelisUnit;

    @Column(name = "conversion_factor", precision = 10, scale = 6)
    private BigDecimal conversionFactor;

    @Column(name = "reject_if_mismatch", nullable = false)
    private Boolean rejectIfMismatch = false;
}
```

### 6. AnalyzerError

**Purpose**: Stores failed/unmapped analyzer messages for error dashboard and
reprocessing workflow.

**Table**: `analyzer_error`

**Relationships**:

- Many-to-One with `Analyzer` (via `analyzer_id`)

**Fields**:

| Field             | Type        | Constraints                        | Description                                              |
| ----------------- | ----------- | ---------------------------------- | -------------------------------------------------------- |
| `id`              | VARCHAR(36) | PK, NOT NULL                       | Primary key (UUID)                                       |
| `analyzer_id`     | VARCHAR(36) | FK, NOT NULL                       | References `analyzer.id`                                 |
| `error_type`      | VARCHAR(20) | NOT NULL                           | Type: MAPPING, VALIDATION, TIMEOUT, PROTOCOL, CONNECTION |
| `severity`        | VARCHAR(20) | NOT NULL                           | Severity: CRITICAL, ERROR, WARNING                       |
| `error_message`   | TEXT        | NOT NULL                           | Human-readable error message                             |
| `raw_message`     | TEXT        | NULL                               | Raw ASTM message (for reprocessing)                      |
| `status`          | VARCHAR(20) | NOT NULL, DEFAULT 'UNACKNOWLEDGED' | Status: UNACKNOWLEDGED, ACKNOWLEDGED, RESOLVED           |
| `acknowledged_by` | VARCHAR(36) | NULL                               | User ID who acknowledged error                           |
| `acknowledged_at` | TIMESTAMP   | NULL                               | Timestamp when error was acknowledged                    |
| `resolved_at`     | TIMESTAMP   | NULL                               | Timestamp when error was resolved (after reprocessing)   |
| `last_updated`    | TIMESTAMP   | NOT NULL                           | Audit timestamp                                          |
| `sys_user_id`     | VARCHAR(36) | NULL                               | Audit user ID                                            |

**Validation Rules**:

- `error_type`: Must be one of: MAPPING, VALIDATION, TIMEOUT, PROTOCOL,
  CONNECTION
- `severity`: Must be one of: CRITICAL, ERROR, WARNING
- `status`: Must be one of: UNACKNOWLEDGED, ACKNOWLEDGED, RESOLVED
- `acknowledged_by`: Required if `status` is ACKNOWLEDGED or RESOLVED
- `acknowledged_at`: Required if `status` is ACKNOWLEDGED or RESOLVED

**State Transitions**:

- `status`: `UNACKNOWLEDGED` → `ACKNOWLEDGED` (user acknowledges error)
- `status`: `ACKNOWLEDGED` → `RESOLVED` (reprocessing successful)
- `status`: `RESOLVED` → `UNACKNOWLEDGED` (reprocessing failed, error re-opened)

**JPA Entity**:

```java
@Entity
@Table(name = "analyzer_error")
public class AnalyzerError extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @Column(name = "error_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    @Column(name = "severity", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String errorMessage;

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ErrorStatus status = ErrorStatus.UNACKNOWLEDGED;

    @Column(name = "acknowledged_by", length = 36)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Timestamp acknowledgedAt;

    @Column(name = "resolved_at")
    private Timestamp resolvedAt;

    public enum ErrorType {
        MAPPING, VALIDATION, TIMEOUT, PROTOCOL, CONNECTION
    }

    public enum Severity {
        CRITICAL, ERROR, WARNING
    }

    public enum ErrorStatus {
        UNACKNOWLEDGED, ACKNOWLEDGED, RESOLVED
    }
}
```

### 7. CustomFieldType

**Purpose**: Represents administrator-defined custom field types that extend
beyond standard field types (NUMERIC, QUALITATIVE, etc.) with custom validation
rules.

**Table**: `custom_field_type`

**Relationships**:

- One-to-Many with `ValidationRuleConfiguration`
- Referenced by `AnalyzerField` (when `field_type='CUSTOM'`,
  `custom_field_type_id` references this table)

**Fields**:

| Field          | Type         | Constraints      | Description                       |
| -------------- | ------------ | ---------------- | --------------------------------- |
| `id`           | VARCHAR(36)  | PK, NOT NULL     | Primary key (UUID)                |
| `type_name`    | VARCHAR(100) | NOT NULL, UNIQUE | Display name (e.g., "pH Level")   |
| `description`  | TEXT         | NULL             | Description of field type usage   |
| `is_active`    | BOOLEAN      | NOT NULL         | Whether type is available for use |
| `last_updated` | TIMESTAMP    | NOT NULL         | Audit timestamp (from BaseObject) |
| `sys_user_id`  | VARCHAR(36)  | NULL             | Audit user ID (from BaseObject)   |

**Validation Rules**:

- `type_name`: Must be unique, 1-100 characters
- Cannot delete custom field type if referenced by active analyzer fields

**JPA Entity**:

```java
@Entity
@Table(name = "custom_field_type")
public class CustomFieldType extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @Column(name = "type_name", length = 100, nullable = false, unique = true)
    @NotNull
    private String typeName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customFieldType", cascade = CascadeType.ALL)
    private List<ValidationRuleConfiguration> validationRules;
}
```

### 8. ValidationRuleConfiguration

**Purpose**: Defines validation rules for custom field types, supporting regex
patterns, value ranges, enumerated values, and length constraints.

**Table**: `validation_rule_configuration`

**Relationships**:

- Many-to-One with `CustomFieldType`

**Fields**:

| Field                  | Type         | Constraints  | Description                                 |
| ---------------------- | ------------ | ------------ | ------------------------------------------- |
| `id`                   | VARCHAR(36)  | PK, NOT NULL | Primary key (UUID)                          |
| `custom_field_type_id` | VARCHAR(36)  | FK, NOT NULL | References `custom_field_type.id`           |
| `rule_name`            | VARCHAR(100) | NOT NULL     | Display name (e.g., "pH Range Validator")   |
| `rule_type`            | VARCHAR(20)  | NOT NULL     | Rule type enum (REGEX, RANGE, ENUM, LENGTH) |
| `rule_expression`      | TEXT         | NOT NULL     | JSON or pattern string                      |
| `error_message`        | VARCHAR(500) | NOT NULL     | Custom error message template               |
| `is_active`            | BOOLEAN      | NOT NULL     | Whether rule is enforced                    |
| `last_updated`         | TIMESTAMP    | NOT NULL     | Audit timestamp (from BaseObject)           |
| `sys_user_id`          | VARCHAR(36)  | NULL         | Audit user ID (from BaseObject)             |

**Rule Expression Formats**:

- **REGEX**: Pattern string (e.g., `"^[0-9]{3}-[A-Z]{2}$"`)
- **RANGE**: JSON `{"min": 0.0, "max": 14.0}` for numeric ranges
- **ENUM**: JSON array `["value1", "value2", "value3"]` for allowed values
- **LENGTH**: JSON `{"minLength": 5, "maxLength": 50}` for string length

**Validation Rules**:

- `rule_type`: Must be one of REGEX, RANGE, ENUM, LENGTH
- `rule_expression`: Must be valid JSON or regex pattern depending on rule_type
- REGEX patterns must compile without errors
- RANGE min must be < max
- ENUM must have at least one value
- LENGTH minLength must be <= maxLength

**JPA Entity**:

```java
@Entity
@Table(name = "validation_rule_configuration")
public class ValidationRuleConfiguration extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_field_type_id", nullable = false)
    @NotNull
    private CustomFieldType customFieldType;

    @Column(name = "rule_name", length = 100, nullable = false)
    @NotNull
    private String ruleName;

    @Column(name = "rule_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private RuleType ruleType;

    @Column(name = "rule_expression", columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String ruleExpression;

    @Column(name = "error_message", length = 500, nullable = false)
    @NotNull
    private String errorMessage;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public enum RuleType {
        REGEX,    // Regular expression pattern
        RANGE,    // Numeric min/max range
        ENUM,     // Enumerated allowed values
        LENGTH    // String length constraints
    }
}
```

## Entity Relationships

```
Analyzer (legacy)
  └── AnalyzerConfiguration (1:1)
  ├── AnalyzerField (1:N)
  │   ├── AnalyzerFieldMapping (1:N)
  │   ├── QualitativeResultMapping (1:N)
  │   └── UnitMapping (1:N)
  └── AnalyzerError (1:N)
```

## Database Constraints

### Foreign Keys

- `analyzer_configuration.analyzer_id` → `analyzer.id`
- `analyzer_field.analyzer_id` → `analyzer.id`
- `analyzer_field_mapping.analyzer_field_id` → `analyzer_field.id`
- `qualitative_result_mapping.analyzer_field_id` → `analyzer_field.id`
- `unit_mapping.analyzer_field_id` → `analyzer_field.id`
- `analyzer_error.analyzer_id` → `analyzer.id`

### Unique Constraints

- `analyzer_configuration.analyzer_id` (one configuration per analyzer)
- `(qualitative_result_mapping.analyzer_field_id, qualitative_result_mapping.analyzer_value)`
  (one mapping per analyzer value per field)
- `(unit_mapping.analyzer_field_id, unit_mapping.analyzer_unit)` (one mapping
  per analyzer unit per field)

### Indexes

- `analyzer_field.analyzer_id` (for efficient field lookup by analyzer)
- `analyzer_field_mapping.analyzer_field_id` (for efficient mapping lookup)
- `analyzer_error.analyzer_id` (for error dashboard filtering)
- `analyzer_error.status` (for error dashboard filtering)
- `analyzer_error.error_type` (for error dashboard filtering)
- `analyzer_error.timestamp` (for error dashboard sorting)

## Validation Rules Summary

### Type Compatibility (Service Layer)

- **Numeric → Numeric**: Allowed (with unit conversion if needed)
- **Qualitative → Qualitative**: Allowed (many-to-one supported)
- **Text → Text**: Allowed
- **Numeric → Text**: Blocked (data loss)
- **Text → Numeric**: Blocked (unsafe conversion)
- **Qualitative → Numeric**: Blocked (semantic mismatch)

### Required Mappings

For analyzer activation, the following mappings must exist with
`is_required=true`:

- Sample ID mapping (TEST_LEVEL or METADATA)
- Test Code mapping (TEST_LEVEL)
- Result Value mapping (RESULT_LEVEL)

### Unit Conversion

- If `analyzer_unit` == `openelis_unit`: No conversion needed
- If `analyzer_unit` != `openelis_unit` and `conversion_factor` provided: Apply
  conversion
- If `analyzer_unit` != `openelis_unit` and `conversion_factor` NULL and
  `reject_if_mismatch=true`: Reject message
- If `analyzer_unit` != `openelis_unit` and `conversion_factor` NULL and
  `reject_if_mismatch=false`: Warning, use analyzer unit as-is

## State Transitions

### Mapping Activation Workflow

1. **Draft** (`is_active=false`): Mapping created but not yet active
2. **Pending Activation** (`is_active=false`, analyzer is active): Mapping ready
   but requires confirmation
3. **Active** (`is_active=true`): Mapping applied to new messages
4. **Disabled** (`is_active=false`, previously active): Mapping retired but
   retained for audit

### Error Resolution Workflow

1. **Unacknowledged** (`status=UNACKNOWLEDGED`): Error created, not yet reviewed
2. **Acknowledged** (`status=ACKNOWLEDGED`): Error reviewed, mappings being
   created
3. **Resolved** (`status=RESOLVED`): Reprocessing successful, error closed
4. **Re-opened** (`status=UNACKNOWLEDGED`, previously resolved): Reprocessing
   failed, error re-opened

## Notes

- All entities extend `BaseObject<String>` for audit trail support
  (`last_updated`, `sys_user_id`)
- All entities use UUID primary keys (`VARCHAR(36)`) for distributed system
  compatibility
- Foreign key relationships use `LAZY` fetching to prevent N+1 queries (services
  must use JOIN FETCH)
- Unique constraints prevent duplicate mappings (same analyzer value/unit mapped
  multiple times)
- Indexes optimize common query patterns (error dashboard filtering, mapping
  lookup)
