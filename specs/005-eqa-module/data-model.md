# Data Model: External Quality Assurance (EQA) Module

**Feature**: 005-eqa-module **Date**: 2025-11-18 **Status**: Complete

## Entity Relationship Overview

```
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│  EQAProgram  │────<│EQAProgramTest│>────│   Test (existing)│
└──────┬──────┘     └──────────────┘     └────────────────┘
       │
       │ FK
       ▼
┌──────────────────┐     ┌────────────────────┐
│  EQADistribution │────<│  Sample (existing)  │
└──────┬───────────┘     │  + SampleEQA (1:1)  │
       │                 └─────────┬──────────┘
       │ FK                        │ FK
       ▼                           ▼
┌──────────────┐          ┌──────────────────┐
│   EQAResult  │          │ Organization     │
│              │─────────>│ (existing, reuse)│
└──────────────┘          └──────────────────┘

┌──────────────┐
│Alert (existing│  ← Extended with new AlertType values
│ polymorphic) │
└──────────────┘
```

---

## New Entities

### 1. EQAProgram

**Table**: `clinlims.eqa_program` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column      | Type          | Constraints            | Description                             |
| ----------- | ------------- | ---------------------- | --------------------------------------- |
| id          | numeric(10,0) | PK, NOT NULL           | Primary key (sequence: eqa_program_seq) |
| fhir_uuid   | UUID          | NOT NULL, UNIQUE       | FHIR R4 external identifier             |
| name        | VARCHAR(255)  | NOT NULL               | Program name (e.g., "WHO Malaria EQA")  |
| description | TEXT          | NULLABLE               | Program details and objectives          |
| is_active   | BOOLEAN       | NOT NULL, DEFAULT true | Active/inactive status                  |
| sys_user_id | numeric(10,0) | NOT NULL               | Audit: last modifying user              |
| lastupdated | TIMESTAMP     | NOT NULL (version)     | Optimistic locking                      |

**JPA Annotations**:

```java
@Entity
@Table(name = "eqa_program", schema = "clinlims")
public class EQAProgram extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "eqa_program_generator")
    @GenericGenerator(name = "eqa_program_generator",
        strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator",
        parameters = @Parameter(name = "sequence_name", value = "eqa_program_seq"))
    @Column(name = "id")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    public void prePersist() {
        if (fhirUuid == null) fhirUuid = UUID.randomUUID();
    }
}
```

**Relationships**:

- One-to-many → `EQAProgramTest` (test assignments)
- One-to-many → `EQADistribution` (distributions using this program)
- One-to-many → `SampleEQA` (samples assigned to this program)

---

### 2. EQAProgramTest

**Table**: `clinlims.eqa_program_test` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column         | Type          | Constraints                    | Description        |
| -------------- | ------------- | ------------------------------ | ------------------ |
| id             | numeric(10,0) | PK, NOT NULL                   | Primary key        |
| eqa_program_id | numeric(10,0) | FK → eqa_program(id), NOT NULL | Parent program     |
| test_id        | numeric(10,0) | FK → test(id), NOT NULL        | Assigned test      |
| is_active      | BOOLEAN       | NOT NULL, DEFAULT true         | Assignment status  |
| sys_user_id    | numeric(10,0) | NOT NULL                       | Audit user         |
| lastupdated    | TIMESTAMP     | NOT NULL (version)             | Optimistic locking |

**Unique Constraint**: `(eqa_program_id, test_id)` - prevent duplicate
assignments

**JPA Annotations**:

```java
@Entity
@Table(name = "eqa_program_test", schema = "clinlims",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"eqa_program_id", "test_id"}))
public class EQAProgramTest extends BaseObject<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id", nullable = false)
    private EQAProgram eqaProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
```

---

### 3. EQADistribution

**Table**: `clinlims.eqa_distribution` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column            | Type          | Constraints                    | Description                           |
| ----------------- | ------------- | ------------------------------ | ------------------------------------- |
| id                | numeric(10,0) | PK, NOT NULL                   | Primary key                           |
| fhir_uuid         | UUID          | NOT NULL, UNIQUE               | FHIR R4 identifier                    |
| eqa_program_id    | numeric(10,0) | FK → eqa_program(id), NOT NULL | Associated program                    |
| distribution_name | VARCHAR(255)  | NOT NULL                       | Unique name (e.g., "Malaria-2025-Q1") |
| distribution_date | TIMESTAMP     | NOT NULL                       | When distribution was created         |
| deadline          | TIMESTAMP     | NOT NULL                       | Testing deadline for all participants |
| status            | VARCHAR(20)   | NOT NULL, DEFAULT 'DRAFT'      | DRAFT/PREPARED/SHIPPED/COMPLETED      |
| created_by        | numeric(10,0) | FK → system_user(id), NOT NULL | EQA coordinator                       |
| target_value      | DECIMAL(15,5) | NULLABLE                       | Expected correct value                |
| sys_user_id       | numeric(10,0) | NOT NULL                       | Audit user                            |
| lastupdated       | TIMESTAMP     | NOT NULL (version)             | Optimistic locking                    |

**Status Enum**:

```java
public enum EQADistributionStatus {
    DRAFT, PREPARED, SHIPPED, COMPLETED
}
```

**JPA Annotations**:

```java
@Entity
@Table(name = "eqa_distribution", schema = "clinlims")
public class EQADistribution extends BaseObject<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id", nullable = false)
    private EQAProgram eqaProgram;

    @Column(name = "distribution_name", nullable = false)
    private String distributionName;

    @Column(name = "distribution_date", nullable = false)
    private Timestamp distributionDate;

    @Column(name = "deadline", nullable = false)
    private Timestamp deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EQADistributionStatus status = EQADistributionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private SystemUser createdBy;

    @Column(name = "target_value", precision = 15, scale = 5)
    private BigDecimal targetValue;
}
```

**Relationships**:

- Many-to-one → `EQAProgram`
- One-to-many → `SampleEQA` (distributed samples)
- One-to-many → `EQAResult` (collected results)

---

### 4. EQAResult

**Table**: `clinlims.eqa_result` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column                        | Type          | Constraints                         | Description                          |
| ----------------------------- | ------------- | ----------------------------------- | ------------------------------------ |
| id                            | numeric(10,0) | PK, NOT NULL                        | Primary key                          |
| fhir_uuid                     | UUID          | NOT NULL, UNIQUE                    | FHIR R4 identifier                   |
| eqa_distribution_id           | numeric(10,0) | FK → eqa_distribution(id), NOT NULL | Parent distribution                  |
| participant_organization_id   | numeric(10,0) | FK → organization(id), NOT NULL     | Submitting laboratory                |
| test_id                       | numeric(10,0) | FK → test(id), NOT NULL             | Test performed                       |
| result_value                  | DECIMAL(15,5) | NULLABLE                            | Participant's submitted value        |
| target_value                  | DECIMAL(15,5) | NULLABLE                            | Expected correct value               |
| z_score                       | DECIMAL(10,5) | NULLABLE                            | Calculated Z-score                   |
| submission_method             | VARCHAR(20)   | NOT NULL                            | FHIR/MANUAL/FILE_UPLOAD              |
| submission_date               | TIMESTAMP     | NOT NULL                            | When result was submitted            |
| performance_status            | VARCHAR(20)   | NULLABLE                            | ACCEPTABLE/QUESTIONABLE/UNACCEPTABLE |
| is_late_submission            | BOOLEAN       | NOT NULL, DEFAULT false             | Submitted after deadline             |
| late_submission_justification | TEXT          | NULLABLE                            | Required if late                     |
| approved_by                   | numeric(10,0) | FK → system_user(id), NULLABLE      | Supervisor who approved late sub     |
| sys_user_id                   | numeric(10,0) | NOT NULL                            | Audit user                           |
| lastupdated                   | TIMESTAMP     | NOT NULL (version)                  | Optimistic locking                   |

**Enums**:

```java
public enum EQASubmissionMethod {
    FHIR, MANUAL, FILE_UPLOAD
}

public enum EQAPerformanceStatus {
    ACCEPTABLE, QUESTIONABLE, UNACCEPTABLE
}
```

**Unique Constraint**:
`(eqa_distribution_id, participant_organization_id, test_id)` - one result per
participant per test per distribution

---

### 5. SampleEQA

**Table**: `clinlims.sample_eqa` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column                       | Type          | Constraints                         | Description                     |
| ---------------------------- | ------------- | ----------------------------------- | ------------------------------- |
| id                           | numeric(10,0) | PK, NOT NULL                        | Primary key                     |
| sample_id                    | numeric(10,0) | FK → sample(id), NOT NULL, UNIQUE   | Linked sample (1:1)             |
| is_eqa_sample                | BOOLEAN       | NOT NULL, DEFAULT false             | EQA designation flag            |
| eqa_program_id               | numeric(10,0) | FK → eqa_program(id), NULLABLE      | Associated EQA program          |
| eqa_provider_organization_id | numeric(10,0) | FK → organization(id), NULLABLE     | External provider org           |
| eqa_provider_sample_id       | VARCHAR(100)  | NULLABLE                            | Provider's sample identifier    |
| eqa_participant_id           | VARCHAR(100)  | NULLABLE                            | Lab's participant ID in program |
| eqa_deadline                 | TIMESTAMP     | NULLABLE                            | Testing/submission deadline     |
| eqa_priority                 | VARCHAR(20)   | NULLABLE, DEFAULT 'STANDARD'        | STANDARD/URGENT/CRITICAL        |
| eqa_distribution_id          | numeric(10,0) | FK → eqa_distribution(id), NULLABLE | For distributed samples         |
| sys_user_id                  | numeric(10,0) | NOT NULL                            | Audit user                      |
| lastupdated                  | TIMESTAMP     | NOT NULL (version)                  | Optimistic locking              |

**Priority Enum**:

```java
public enum EQAPriority {
    STANDARD, URGENT, CRITICAL
}
```

**JPA Annotations**:

```java
@Entity
@Table(name = "sample_eqa", schema = "clinlims")
public class SampleEQA extends BaseObject<String> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false, unique = true)
    private Sample sample;

    @Column(name = "is_eqa_sample", nullable = false)
    private Boolean isEqaSample = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id")
    private EQAProgram eqaProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_provider_organization_id")
    private Organization eqaProviderOrganization;

    @Column(name = "eqa_provider_sample_id", length = 100)
    private String eqaProviderSampleId;

    @Column(name = "eqa_participant_id", length = 100)
    private String eqaParticipantId;

    @Column(name = "eqa_deadline")
    private Timestamp eqaDeadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "eqa_priority", length = 20)
    private EQAPriority eqaPriority = EQAPriority.STANDARD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_distribution_id")
    private EQADistribution eqaDistribution;
}
```

---

## Extended Entities

### AlertType Enum Extension

**Existing values**: FREEZER_TEMPERATURE, EQUIPMENT_FAILURE, INVENTORY_LOW,
SAMPLE_TRACKING, OTHER

**New values added**:

```java
EQA_DEADLINE,              // EQA sample approaching/past deadline
SAMPLE_EXPIRATION,         // Any sample nearing expiration date
STAT_UPCOMING,             // STAT order approaching target time
STAT_OVERDUE,              // STAT order exceeded target time
CRITICAL_UNACKNOWLEDGED    // Critical alert unacknowledged >4 hours
```

**Migration**: Liquibase changeset adds new values. Since AlertType is stored as
VARCHAR (enum name), no schema change needed - just Java enum extension.

---

## State Transitions

### EQADistribution Lifecycle

```
DRAFT → PREPARED → SHIPPED → COMPLETED
  │                   │
  └── (can go back)   └── (final, immutable)
```

- **DRAFT**: Samples being created, organizations being selected
- **PREPARED**: Barcodes generated, shipments organized
- **SHIPPED**: Samples physically sent to participants
- **COMPLETED**: All results collected and analyzed

### Alert Lifecycle (existing, reused)

```
OPEN → ACKNOWLEDGED → RESOLVED
```

- **OPEN**: Alert created, awaiting attention
- **ACKNOWLEDGED**: User has seen and accepted responsibility
- **RESOLVED**: Action taken, resolution comment recorded

### EQA Priority Processing

| Priority | Processing Target | Queue Position |
| -------- | ----------------- | -------------- |
| CRITICAL | Within 4 hours    | Top of queue   |
| URGENT   | Within 24 hours   | Elevated       |
| STANDARD | Normal workflow   | Normal         |

---

## Database Indexes

```sql
-- SampleEQA lookups
CREATE INDEX idx_sample_eqa_sample_id ON clinlims.sample_eqa(sample_id);
CREATE INDEX idx_sample_eqa_program_id ON clinlims.sample_eqa(eqa_program_id);
CREATE INDEX idx_sample_eqa_deadline ON clinlims.sample_eqa(eqa_deadline);
CREATE INDEX idx_sample_eqa_is_eqa ON clinlims.sample_eqa(is_eqa_sample);

-- EQAResult lookups
CREATE INDEX idx_eqa_result_distribution ON clinlims.eqa_result(eqa_distribution_id);
CREATE INDEX idx_eqa_result_participant ON clinlims.eqa_result(participant_organization_id);

-- EQADistribution lookups
CREATE INDEX idx_eqa_dist_program ON clinlims.eqa_distribution(eqa_program_id);
CREATE INDEX idx_eqa_dist_status ON clinlims.eqa_distribution(status);

-- Alert dashboard queries (extend existing)
-- Already indexed: alert_type, status, severity in existing Alert entity
```

---

## Validation Rules

| Entity          | Field                 | Validation                                        |
| --------------- | --------------------- | ------------------------------------------------- |
| SampleEQA       | eqa_program_id        | Required when is_eqa_sample = true                |
| SampleEQA       | eqa_deadline          | Must be in the future at registration time        |
| SampleEQA       | eqa_provider_org_id   | Required when is_eqa_sample = true                |
| EQADistribution | participant count     | Minimum 2 organizations required for finalization |
| EQAResult       | result_value          | Must be within test-specific plausible range      |
| EQAResult       | late_submission_just. | Required when is_late_submission = true           |
| EQAProgram      | name                  | Required, max 255 chars                           |
| EQAProgramTest  | (program_id, test_id) | Unique combination                                |
