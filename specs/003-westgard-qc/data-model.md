# Data Model: Westgard Rules Quality Control Compliance

**Feature**: 003-westgard-qc  
**Date**: 2025-11-20  
**Status**: Complete  
**Cross-Reference**: Feature 004 (ASTM Analyzer Field Mapping) - Analyzer entity

This document defines the data model for the Westgard Rules Quality Control compliance feature, including entity definitions, relationships, validation rules, and state transitions.

## Entity Overview

The feature introduces 7 new entities and references 1 entity from Feature 004:

1. **QCControlLot** - Represents a batch of quality control material with statistical parameters
2. **QCResult** - Represents a single quality control measurement
3. **QCStatistics** - Represents calculated statistical parameters (mean, SD) for a control lot
4. **WestgardRuleConfig** - Represents enabled Westgard rules for a test-analyzer combination
5. **QCRuleViolation** - Represents detected violation of one or more Westgard rules
6. **QCCorrectiveAction** - Represents action taken to resolve a violation
7. **QCAlert** - Represents notification sent about a violation
8. **Analyzer** (from Feature 004) - Referenced by QC entities via foreign key

## Entity Definitions

### 1. QCControlLot

**Purpose**: Represents a specific batch of quality control material (e.g., BioRad Lyphochek Level 1, Lot# 12345) with defined characteristics and statistical calculation method.

**Table**: `qc_control_lot`

**Relationships**:
- Many-to-One with `Analyzer` (via `analyzer_id`)
- Many-to-One with `TestUnit` (via `test_unit_id`)
- One-to-Many with `QCResult`
- One-to-Many with `QCStatistics`

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `analyzer_id` | VARCHAR(36) | FK, NOT NULL | References `analyzer.id` (from Feature 004) |
| `test_unit_id` | VARCHAR(36) | FK, NOT NULL | References `test_unit.id` |
| `product_name` | VARCHAR(255) | NOT NULL | Product name (e.g., "BioRad Lyphochek") |
| `lot_number` | VARCHAR(100) | NOT NULL | Manufacturer lot number |
| `control_level` | VARCHAR(20) | NOT NULL | Control level enum (LOW, NORMAL, HIGH) |
| `manufacturer_name` | VARCHAR(255) | NULL | Manufacturer name |
| `manufacturer_mean` | DECIMAL(10,4) | NULL | Manufacturer-provided mean value |
| `manufacturer_sd` | DECIMAL(10,4) | NULL | Manufacturer-provided standard deviation |
| `activation_date` | DATE | NOT NULL | Date lot activated for use |
| `expiration_date` | DATE | NOT NULL | Date lot expires |
| `calculation_method` | VARCHAR(20) | NOT NULL | Statistics calculation method enum |
| `initial_runs_count` | INTEGER | NULL | Number of runs for initial establishment |
| `rolling_window_size` | INTEGER | NULL | Window size for rolling calculation |
| `status` | VARCHAR(20) | NOT NULL | Lot status enum (ESTABLISHMENT, ACTIVE, EXPIRED, DEACTIVATED) |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID |

**Validation Rules**:
- `control_level`: Must be one of LOW, NORMAL, HIGH
- `calculation_method`: Must be one of INITIAL_RUNS, ROLLING, MANUFACTURER_FIXED
- `manufacturer_mean` and `manufacturer_sd`: Required if `calculation_method=MANUFACTURER_FIXED`
- `initial_runs_count`: Required if `calculation_method=INITIAL_RUNS`, default 20, range 10-50
- `rolling_window_size`: Required if `calculation_method=ROLLING`, default 20, range 20-100
- `expiration_date`: Must be after `activation_date`
- `status`: Must be one of ESTABLISHMENT, ACTIVE, EXPIRED, DEACTIVATED
- Unique constraint: `(analyzer_id, test_unit_id, lot_number, control_level)` - one lot per analyzer/test/lot#/level combination

**State Transitions**:
- `ESTABLISHMENT` → `ACTIVE`: Automatic when sufficient results collected (for INITIAL_RUNS method)
- `ACTIVE` → `EXPIRED`: Automatic at expiration date
- `ACTIVE` → `DEACTIVATED`: Manual deactivation by administrator
- `EXPIRED` → `DEACTIVATED`: Manual archival after replacement lot activated

**JPA Entity**:

```java
@Entity
@Table(name = "qc_control_lot",
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "analyzer_id", "test_unit_id", "lot_number", "control_level"
       }))
public class QCControlLot extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_unit_id", nullable = false)
    private TestUnit testUnit;

    @Column(name = "product_name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String productName;

    @Column(name = "lot_number", nullable = false, length = 100)
    @NotNull
    @Size(min = 1, max = 100)
    private String lotNumber;

    @Column(name = "control_level", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ControlLevel controlLevel;

    @Column(name = "manufacturer_name", length = 255)
    private String manufacturerName;

    @Column(name = "manufacturer_mean", precision = 10, scale = 4)
    private BigDecimal manufacturerMean;

    @Column(name = "manufacturer_sd", precision = 10, scale = 4)
    private BigDecimal manufacturerSd;

    @Column(name = "activation_date", nullable = false)
    @Temporal(TemporalType.DATE)
    @NotNull
    private Date activationDate;

    @Column(name = "expiration_date", nullable = false)
    @Temporal(TemporalType.DATE)
    @NotNull
    private Date expirationDate;

    @Column(name = "calculation_method", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private CalculationMethod calculationMethod;

    @Column(name = "initial_runs_count")
    private Integer initialRunsCount;

    @Column(name = "rolling_window_size")
    private Integer rollingWindowSize;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private LotStatus status = LotStatus.ESTABLISHMENT;

    public enum ControlLevel {
        LOW, NORMAL, HIGH
    }

    public enum CalculationMethod {
        INITIAL_RUNS,       // First N runs (default 20)
        ROLLING,            // Moving window
        MANUFACTURER_FIXED  // Fixed manufacturer values
    }

    public enum LotStatus {
        ESTABLISHMENT,  // Collecting initial data
        ACTIVE,         // Ready for QC evaluation
        EXPIRED,        // Past expiration date
        DEACTIVATED     // Manually deactivated
    }
}
```

### 2. QCResult

**Purpose**: Represents a single quality control measurement run on an analyzer for a specific test and control lot.

**Table**: `qc_result`

**Relationships**:
- Many-to-One with `QCControlLot` (via `control_lot_id`)
- Many-to-One with `Analyzer` (via `analyzer_id`)
- Many-to-One with `TestUnit` (via `test_unit_id`)

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `control_lot_id` | VARCHAR(36) | FK, NOT NULL | References `qc_control_lot.id` |
| `analyzer_id` | VARCHAR(36) | FK, NOT NULL | References `analyzer.id` |
| `test_unit_id` | VARCHAR(36) | FK, NOT NULL | References `test_unit.id` |
| `result_value` | DECIMAL(10,4) | NOT NULL | Measured value |
| `unit_of_measure` | VARCHAR(50) | NOT NULL | Unit (e.g., "mg/dL", "mmol/L") |
| `z_score` | DECIMAL(8,4) | NULL | Calculated z-score (value - mean) / SD |
| `run_date_time` | TIMESTAMP | NOT NULL | Date and time QC was performed |
| `status` | VARCHAR(20) | NOT NULL | Result status enum (PENDING, ACCEPTED, REJECTED) |
| `non_conformity_flag` | BOOLEAN | NOT NULL, DEFAULT false | True if rejection-level violation |
| `external_note` | TEXT | NULL | Note added by corrective action |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID (technician who ran QC) |

**Validation Rules**:
- `result_value`: Must be positive and within reasonable range for test (validated in service layer)
- `z_score`: Calculated automatically, range typically -5.0 to +5.0
- `status`: Must be one of PENDING, ACCEPTED, REJECTED
- `non_conformity_flag`: Set to true automatically when rejection-level violation detected
- Results from same control lot must be chronologically ordered by `run_date_time`

**State Transitions**:
- `PENDING` → `ACCEPTED`: No violations detected or warning-level only
- `PENDING` → `REJECTED`: Rejection-level violation detected
- `REJECTED` → `ACCEPTED`: Corrective action completed and violation resolved

**JPA Entity**:

```java
@Entity
@Table(name = "qc_result")
public class QCResult extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_lot_id", nullable = false)
    private QCControlLot controlLot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_unit_id", nullable = false)
    private TestUnit testUnit;

    @Column(name = "result_value", nullable = false, precision = 10, scale = 4)
    @NotNull
    private BigDecimal resultValue;

    @Column(name = "unit_of_measure", nullable = false, length = 50)
    @NotNull
    private String unitOfMeasure;

    @Column(name = "z_score", precision = 8, scale = 4)
    private BigDecimal zScore;

    @Column(name = "run_date_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Timestamp runDateTime;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ResultStatus status = ResultStatus.PENDING;

    @Column(name = "non_conformity_flag", nullable = false)
    private Boolean nonConformityFlag = false;

    @Column(name = "external_note", columnDefinition = "TEXT")
    private String externalNote;

    public enum ResultStatus {
        PENDING,   // Awaiting rule evaluation
        ACCEPTED,  // Passed QC (no rejections)
        REJECTED   // Failed QC (rejection-level violation)
    }
}
```

### 3. QCStatistics

**Purpose**: Represents calculated statistical parameters (mean, standard deviation) for a control lot at a specific point in time. Cached to avoid recalculation.

**Table**: `qc_statistics`

**Relationships**:
- Many-to-One with `QCControlLot` (via `control_lot_id`)

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `control_lot_id` | VARCHAR(36) | FK, NOT NULL | References `qc_control_lot.id` |
| `calculated_date` | TIMESTAMP | NOT NULL | When statistics were calculated |
| `mean` | DECIMAL(10,4) | NOT NULL | Calculated or manufacturer mean |
| `standard_deviation` | DECIMAL(10,4) | NOT NULL | Calculated or manufacturer SD |
| `num_values` | INTEGER | NOT NULL | Number of results used in calculation |
| `calculation_method` | VARCHAR(20) | NOT NULL | Method used (INITIAL_RUNS, ROLLING, MANUFACTURER_FIXED) |
| `valid_from` | TIMESTAMP | NOT NULL | Start of validity period |
| `valid_to` | TIMESTAMP | NULL | End of validity period (NULL if current) |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID |

**Validation Rules**:
- `mean`: Must be positive
- `standard_deviation`: Must be positive and <50% of mean (reasonable range check)
- `num_values`: Must be ≥20 for INITIAL_RUNS/ROLLING, can be 0 for MANUFACTURER_FIXED
- `valid_to`: NULL indicates current statistics, non-NULL indicates superseded by newer calculation

**State Transitions**:
- Current statistics (`valid_to=NULL`) → Superseded (`valid_to=timestamp`) when new statistics calculated

**JPA Entity**:

```java
@Entity
@Table(name = "qc_statistics")
public class QCStatistics extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_lot_id", nullable = false)
    private QCControlLot controlLot;

    @Column(name = "calculated_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Timestamp calculatedDate;

    @Column(name = "mean", nullable = false, precision = 10, scale = 4)
    @NotNull
    private BigDecimal mean;

    @Column(name = "standard_deviation", nullable = false, precision = 10, scale = 4)
    @NotNull
    private BigDecimal standardDeviation;

    @Column(name = "num_values", nullable = false)
    @NotNull
    private Integer numValues;

    @Column(name = "calculation_method", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private CalculationMethod calculationMethod;

    @Column(name = "valid_from", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Timestamp validFrom;

    @Column(name = "valid_to")
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp validTo;

    public enum CalculationMethod {
        INITIAL_RUNS, ROLLING, MANUFACTURER_FIXED
    }
}
```

### 4. WestgardRuleConfig

**Purpose**: Represents the configuration of Westgard rules for a specific test-analyzer combination, including which rules are enabled and their parameters.

**Table**: `westgard_rule_config`

**Relationships**:
- Many-to-One with `Analyzer` (via `analyzer_id`)
- Many-to-One with `TestUnit` (via `test_unit_id`)

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `analyzer_id` | VARCHAR(36) | FK, NOT NULL | References `analyzer.id` |
| `test_unit_id` | VARCHAR(36) | FK, NOT NULL | References `test_unit.id` |
| `rule_code` | VARCHAR(10) | NOT NULL | Rule code (1_2s, 1_3s, 2_2s, R_4s, 4_1s, 10_x, 3_1s, 7_t) |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT true | Whether rule is enabled |
| `severity` | VARCHAR(20) | NOT NULL | Severity enum (WARNING, REJECTION) |
| `requires_corrective_action` | BOOLEAN | NOT NULL | Whether violation requires corrective action |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID |

**Validation Rules**:
- `rule_code`: Must be one of 1_2s, 1_3s, 2_2s, R_4s, 4_1s, 10_x, 3_1s, 7_t
- `severity`: Must be one of WARNING, REJECTION
- At least one rejection-level rule must be enabled per test-analyzer combination (validated in service layer)
- Unique constraint: `(analyzer_id, test_unit_id, rule_code)` - one config per rule per analyzer/test

**State Transitions**:
- `enabled=true` → `enabled=false`: Rule disabled (no longer evaluated)
- `enabled=false` → `enabled=true`: Rule re-enabled

**JPA Entity**:

```java
@Entity
@Table(name = "westgard_rule_config",
       uniqueConstraints = @UniqueConstraint(columnNames = {
           "analyzer_id", "test_unit_id", "rule_code"
       }))
public class WestgardRuleConfig extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_unit_id", nullable = false)
    private TestUnit testUnit;

    @Column(name = "rule_code", nullable = false, length = 10)
    @NotNull
    private String ruleCode;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "severity", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private RuleSeverity severity;

    @Column(name = "requires_corrective_action", nullable = false)
    private Boolean requiresCorrectiveAction;

    public enum RuleSeverity {
        WARNING,   // Alert only, does not block results
        REJECTION  // Blocks patient result release
    }
}
```

### 5. QCRuleViolation

**Purpose**: Represents a detected violation of one or more Westgard rules, including all QC results involved in the violation and resolution status.

**Table**: `qc_rule_violation`

**Relationships**:
- Many-to-One with `QCResult` (via `triggering_result_id`) - the result that caused violation
- Many-to-One with `Analyzer` (via `analyzer_id`)
- Many-to-One with `TestUnit` (via `test_unit_id`)
- Many-to-Many with `QCResult` (via `violation_result` join table) - all results involved

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `triggering_result_id` | VARCHAR(36) | FK, NOT NULL | QC result that triggered violation |
| `analyzer_id` | VARCHAR(36) | FK, NOT NULL | References `analyzer.id` |
| `test_unit_id` | VARCHAR(36) | FK, NOT NULL | References `test_unit.id` |
| `rule_code` | VARCHAR(10) | NOT NULL | Rule code violated (can be comma-separated for multiple) |
| `violation_date_time` | TIMESTAMP | NOT NULL | When violation was detected |
| `severity` | VARCHAR(20) | NOT NULL | Severity enum (WARNING, REJECTION) |
| `resolution_status` | VARCHAR(20) | NOT NULL | Resolution status enum |
| `resolved_date_time` | TIMESTAMP | NULL | When violation was resolved |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID |

**Validation Rules**:
- `rule_code`: Must be one or more valid rule codes (comma-separated if multiple rules violated simultaneously)
- `severity`: Must be one of WARNING, REJECTION
- `resolution_status`: Must be one of UNRESOLVED, ACKNOWLEDGED, CORRECTIVE_ACTION_PENDING, RESOLVED
- `resolved_date_time`: Required if `resolution_status=RESOLVED`

**State Transitions**:
- `UNRESOLVED` → `ACKNOWLEDGED`: User acknowledges violation
- `ACKNOWLEDGED` → `CORRECTIVE_ACTION_PENDING`: Corrective action created
- `CORRECTIVE_ACTION_PENDING` → `RESOLVED`: Corrective action completed
- `UNRESOLVED` → `RESOLVED`: Warning-level violation acknowledged without corrective action

**JPA Entity**:

```java
@Entity
@Table(name = "qc_rule_violation")
public class QCRuleViolation extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggering_result_id", nullable = false)
    private QCResult triggeringResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_unit_id", nullable = false)
    private TestUnit testUnit;

    @Column(name = "rule_code", nullable = false, length = 10)
    @NotNull
    private String ruleCode;

    @Column(name = "violation_date_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Timestamp violationDateTime;

    @Column(name = "severity", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private RuleSeverity severity;

    @Column(name = "resolution_status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ResolutionStatus resolutionStatus = ResolutionStatus.UNRESOLVED;

    @Column(name = "resolved_date_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp resolvedDateTime;

    @ManyToMany
    @JoinTable(
        name = "violation_result",
        joinColumns = @JoinColumn(name = "violation_id"),
        inverseJoinColumns = @JoinColumn(name = "result_id")
    )
    private List<QCResult> relatedResults;

    public enum RuleSeverity {
        WARNING, REJECTION
    }

    public enum ResolutionStatus {
        UNRESOLVED,                  // Violation detected, not yet addressed
        ACKNOWLEDGED,                // User acknowledged, reviewing
        CORRECTIVE_ACTION_PENDING,   // Corrective action in progress
        RESOLVED                     // Corrective action completed, violation closed
    }
}
```

### 6. QCCorrectiveAction

**Purpose**: Represents an action taken to resolve a QC rule violation, including action type, assignment, status, and resolution notes.

**Table**: `qc_corrective_action`

**Relationships**:
- Many-to-One with `QCRuleViolation` (via `violation_id`)
- Many-to-One with `User` (via `assigned_to_user_id`)
- Many-to-One with `User` (via `created_by_user_id`)

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `violation_id` | VARCHAR(36) | FK, NOT NULL | References `qc_rule_violation.id` |
| `action_type` | VARCHAR(20) | NOT NULL | Action type enum |
| `description` | TEXT | NOT NULL | Description of action to be taken |
| `assigned_to_user_id` | VARCHAR(36) | FK, NULL | References `system_user.id` |
| `created_by_user_id` | VARCHAR(36) | FK, NOT NULL | References `system_user.id` |
| `created_date_time` | TIMESTAMP | NOT NULL | When action was created |
| `completed_date_time` | TIMESTAMP | NULL | When action was marked completed |
| `status` | VARCHAR(20) | NOT NULL | Action status enum |
| `resolution_notes` | TEXT | NULL | Notes entered when action completed |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID |

**Validation Rules**:
- `action_type`: Must be one of RECALIBRATION, MAINTENANCE, REPEAT_CONTROL, REAGENT_CHANGE, OTHER
- `status`: Must be one of PENDING, ASSIGNED, IN_PROGRESS, COMPLETED
- `assigned_to_user_id`: Required if `status` is ASSIGNED, IN_PROGRESS, or COMPLETED
- `resolution_notes`: Required if `status=COMPLETED`
- `completed_date_time`: Required if `status=COMPLETED`

**State Transitions**:
- `PENDING` → `ASSIGNED`: Action assigned to user
- `ASSIGNED` → `IN_PROGRESS`: User starts working on action
- `IN_PROGRESS` → `COMPLETED`: User completes action with resolution notes

**JPA Entity**:

```java
@Entity
@Table(name = "qc_corrective_action")
public class QCCorrectiveAction extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id", nullable = false)
    private QCRuleViolation violation;

    @Column(name = "action_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private SystemUser assignedToUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private SystemUser createdByUser;

    @Column(name = "created_date_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Timestamp createdDateTime;

    @Column(name = "completed_date_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp completedDateTime;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ActionStatus status = ActionStatus.PENDING;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public enum ActionType {
        RECALIBRATION,   // Recalibrate analyzer
        MAINTENANCE,     // Perform maintenance
        REPEAT_CONTROL,  // Repeat QC control run
        REAGENT_CHANGE,  // Replace reagents
        OTHER            // Custom action (description required)
    }

    public enum ActionStatus {
        PENDING,       // Created, not yet assigned
        ASSIGNED,      // Assigned to user, not started
        IN_PROGRESS,   // User actively working
        COMPLETED      // Finished, ready for verification
    }
}
```

### 7. QCAlert

**Purpose**: Represents a notification sent about a QC rule violation, tracking alert type, recipient, and read status.

**Table**: `qc_alert`

**Relationships**:
- Many-to-One with `QCRuleViolation` (via `violation_id`)
- Many-to-One with `User` (via `recipient_user_id`)

**Fields**:

| Field | Type | Constraints | Description |
|---|---|---|---|
| `id` | VARCHAR(36) | PK, NOT NULL | Primary key (UUID) |
| `violation_id` | VARCHAR(36) | FK, NOT NULL | References `qc_rule_violation.id` |
| `alert_type` | VARCHAR(20) | NOT NULL | Alert type enum (EMAIL, SYSTEM) |
| `recipient_user_id` | VARCHAR(36) | FK, NOT NULL | References `system_user.id` |
| `sent_date_time` | TIMESTAMP | NOT NULL | When alert was sent |
| `read_status` | BOOLEAN | NOT NULL, DEFAULT false | Whether alert has been read |
| `read_date_time` | TIMESTAMP | NULL | When alert was read |
| `last_updated` | TIMESTAMP | NOT NULL | Audit timestamp |
| `sys_user_id` | VARCHAR(36) | NULL | Audit user ID |

**Validation Rules**:
- `alert_type`: Must be one of EMAIL, SYSTEM
- `read_date_time`: Required if `read_status=true`

**State Transitions**:
- `read_status=false` → `read_status=true`: User views violation detail page

**JPA Entity**:

```java
@Entity
@Table(name = "qc_alert")
public class QCAlert extends BaseObject<String> {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id", nullable = false)
    private QCRuleViolation violation;

    @Column(name = "alert_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private SystemUser recipientUser;

    @Column(name = "sent_date_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Timestamp sentDateTime;

    @Column(name = "read_status", nullable = false)
    private Boolean readStatus = false;

    @Column(name = "read_date_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Timestamp readDateTime;

    public enum AlertType {
        EMAIL,   // Email notification
        SYSTEM   // In-system notification (dashboard)
    }
}
```

## Entity Relationships

```
Analyzer (from Feature 004)
  ├── QCControlLot (1:N)
  │   ├── QCResult (1:N)
  │   └── QCStatistics (1:N)
  ├── QCResult (1:N)
  ├── WestgardRuleConfig (1:N)
  └── QCRuleViolation (1:N)

TestUnit (existing)
  ├── QCControlLot (1:N)
  ├── QCResult (1:N)
  ├── WestgardRuleConfig (1:N)
  └── QCRuleViolation (1:N)

QCRuleViolation (1:N)
  ├── QCCorrectiveAction (1:N)
  ├── QCAlert (1:N)
  └── QCResult (N:M via violation_result join table)

SystemUser (existing)
  ├── QCCorrectiveAction.assignedToUser (1:N)
  ├── QCCorrectiveAction.createdByUser (1:N)
  └── QCAlert.recipientUser (1:N)
```

## Database Constraints

### Foreign Keys
- `qc_control_lot.analyzer_id` → `analyzer.id`
- `qc_control_lot.test_unit_id` → `test_unit.id`
- `qc_result.control_lot_id` → `qc_control_lot.id`
- `qc_result.analyzer_id` → `analyzer.id`
- `qc_result.test_unit_id` → `test_unit.id`
- `qc_statistics.control_lot_id` → `qc_control_lot.id`
- `westgard_rule_config.analyzer_id` → `analyzer.id`
- `westgard_rule_config.test_unit_id` → `test_unit.id`
- `qc_rule_violation.triggering_result_id` → `qc_result.id`
- `qc_rule_violation.analyzer_id` → `analyzer.id`
- `qc_rule_violation.test_unit_id` → `test_unit.id`
- `qc_corrective_action.violation_id` → `qc_rule_violation.id`
- `qc_corrective_action.assigned_to_user_id` → `system_user.id`
- `qc_corrective_action.created_by_user_id` → `system_user.id`
- `qc_alert.violation_id` → `qc_rule_violation.id`
- `qc_alert.recipient_user_id` → `system_user.id`

### Unique Constraints
- `(qc_control_lot.analyzer_id, qc_control_lot.test_unit_id, qc_control_lot.lot_number, qc_control_lot.control_level)` - one lot per analyzer/test/lot#/level
- `(westgard_rule_config.analyzer_id, westgard_rule_config.test_unit_id, westgard_rule_config.rule_code)` - one config per rule per analyzer/test

### Indexes
- `qc_control_lot.analyzer_id` (for analyzer dashboard queries)
- `qc_control_lot.status` (for active lot queries)
- `qc_result.control_lot_id, qc_result.run_date_time` (for historical result retrieval, sequential rules)
- `qc_result.analyzer_id` (for analyzer-specific queries)
- `qc_result.run_date_time` (for chronological ordering)
- `qc_statistics.control_lot_id, qc_statistics.valid_to` (for current statistics lookup)
- `qc_rule_violation.analyzer_id, qc_rule_violation.resolution_status` (for violation dashboard)
- `qc_rule_violation.violation_date_time` (for chronological sorting)
- `qc_corrective_action.assigned_to_user_id, qc_corrective_action.status` (for user task lists)
- `qc_alert.recipient_user_id, qc_alert.read_status` (for unread alerts)

## Validation Rules Summary

### Statistical Validation
- Mean and SD must be positive
- SD should be <50% of mean (reasonable range check)
- Z-score typically in range -5.0 to +5.0
- Initial runs count: 10-50 (default 20)
- Rolling window size: 20-100 (default 20)

### Business Rules
- At least one rejection-level rule must be enabled per test-analyzer
- Control lot cannot be used until statistics established (for INITIAL_RUNS method)
- Rejection-level violations require corrective action before resolution
- Warning-level violations can be acknowledged without corrective action
- Patient results blocked if unresolved rejection-level violation exists for analyzer/test

### Data Integrity
- Expiration date must be after activation date
- Z-score automatically calculated on result entry
- Statistics cached in qc_statistics table (recalculation triggered only when needed)
- Violations cannot be deleted (audit requirement), only marked as resolved

## State Transitions

### Control Lot Lifecycle
1. **ESTABLISHMENT**: Collecting initial data (for INITIAL_RUNS method)
2. **ACTIVE**: Statistics established, lot ready for QC evaluation
3. **EXPIRED**: Past expiration date, new results not accepted
4. **DEACTIVATED**: Manually deactivated by administrator

### QC Result Lifecycle
1. **PENDING**: Awaiting rule evaluation
2. **ACCEPTED**: Passed QC (no rejection-level violations)
3. **REJECTED**: Failed QC (rejection-level violation detected)

### Violation Resolution Workflow
1. **UNRESOLVED**: Violation detected, not yet addressed
2. **ACKNOWLEDGED**: User acknowledged, reviewing
3. **CORRECTIVE_ACTION_PENDING**: Corrective action in progress
4. **RESOLVED**: Corrective action completed, violation closed

### Corrective Action Workflow
1. **PENDING**: Created, not yet assigned
2. **ASSIGNED**: Assigned to user, not started
3. **IN_PROGRESS**: User actively working
4. **COMPLETED**: Finished with resolution notes

## Performance Optimization

### Query Optimization
- Use JOIN FETCH in service layer to prevent N+1 queries:
  ```java
  String hql = "SELECT r FROM QCResult r " +
               "JOIN FETCH r.controlLot lot " +
               "JOIN FETCH r.analyzer " +
               "WHERE r.controlLot.id = :lotId " +
               "AND r.runDateTime < :currentDateTime " +
               "ORDER BY r.runDateTime DESC";
  ```

- Index coverage for common queries:
  - Dashboard: `(analyzer_id, status)` on qc_control_lot
  - Historical results: `(control_lot_id, run_date_time)` on qc_result
  - Violation filtering: `(analyzer_id, resolution_status)` on qc_rule_violation

### Caching Strategy
- Statistics cached in `qc_statistics` table (avoid recalculation per result)
- Cache invalidation: Check `valid_to IS NULL` for current statistics
- Result-level caching: Z-score stored in `qc_result.z_score` column

## Notes
- All entities extend `BaseObject<String>` for audit trail support (`last_updated`, `sys_user_id`)
- All entities use UUID primary keys (`VARCHAR(36)`) for distributed system compatibility
- Foreign key relationships use `LAZY` fetching to prevent N+1 queries (services must use JOIN FETCH)
- Indexes optimize common query patterns (dashboard, violation filtering, historical result retrieval)
- State machine pattern enforced in service layer for validation workflow transitions
- Z-score calculation: `(value - mean) / SD` performed at result entry time and cached

