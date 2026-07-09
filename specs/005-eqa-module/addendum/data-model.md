# Data Model: EQA Enrollment & Navigation Addendum

**Feature**: 005-eqa-module/addendum **Date**: 2026-02-24 **Status**: Complete

## Entity Relationship Overview

```
┌────────────────────┐            ┌──────────────────────────────┐
│  EQAProgram        │            │  EQALabProgramEnrollment     │
│  (existing)        │            │  (NEW - self-enrollment)     │
│  + providerName ✓  │            │  programName, provider       │
└────────┬───────────┘            └──────┬──────────┬────────────┘
         │ FK                            │ FK       │ FK
         ▼                              ▼          ▼
┌────────────────────────┐   ┌──────────────────┐  ┌───────────────────────┐
│  EQAProgramEnrollment  │   │ EQALabEnrollment │  │ EQALabEnrollmentTest  │
│  (NEW - provider-side) │   │ LabUnit (NEW)    │  │ Map (NEW)             │
│  org → enrollment      │   │ → test_section   │  │ → test OR panel       │
└────────────┬───────────┘   └──────────────────┘  └───────────────────────┘
             │ FK
             ▼
┌──────────────────┐
│ Organization     │
│ (existing, reuse)│
└──────────────────┘
```

---

## Existing Entity Update

### EQAProgram (DM-002 — Already Satisfied)

**Table**: `clinlims.eqa_program` — `provider_name` column already exists.

The existing `EQAProgram.java` entity has:

```java
@Column(name = "provider_name", length = 255)
private String providerName;
```

No schema or entity changes needed for DM-002.

---

## New Entities

### 1. EQAProgramEnrollment (DM-007 — Provider-Side)

**Table**: `clinlims.eqa_program_enrollment` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column              | Type          | Constraints                    | Description                  |
| ------------------- | ------------- | ------------------------------ | ---------------------------- |
| id                  | numeric(10,0) | PK, NOT NULL                   | Sequence: eqa_enrollment_seq |
| eqa_program_id      | numeric(10,0) | FK → eqa_program.id, NOT NULL  | Program being distributed    |
| organization_id     | numeric(10,0) | FK → organization.id, NOT NULL | Participating organization   |
| enrollment_date     | TIMESTAMP     | NOT NULL, DEFAULT NOW()        | When enrolled                |
| status              | VARCHAR(20)   | NOT NULL, DEFAULT 'Active'     | Active/Suspended/Withdrawn   |
| status_changed_date | TIMESTAMP     | NULLABLE                       | Last status change           |
| status_changed_by   | numeric(10,0) | FK → system_user.id, NULLABLE  | Who changed status           |
| withdrawal_reason   | TEXT          | NULLABLE                       | Reason if withdrawn          |
| sys_user_id         | numeric(10,0) | NOT NULL                       | Audit: last modifying user   |
| lastupdated         | TIMESTAMP     | NOT NULL (version)             | Optimistic locking           |

**Constraints**:

- UNIQUE on (`eqa_program_id`, `organization_id`) WHERE `status` = 'Active'
  (implemented as partial unique index)

**JPA Annotations**:

```java
@Entity
@Table(name = "eqa_program_enrollment", schema = "clinlims")
public class EQAProgramEnrollment extends BaseObject<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "eqa_enrollment_seq_gen")
    @GenericGenerator(name = "eqa_enrollment_seq_gen",
                      strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
                      parameters = {
                          @Parameter(name = "sequence_name",
                                     value = "eqa_enrollment_seq")
                      })
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eqa_program_id", nullable = false)
    private EQAProgram eqaProgram;

    @Column(name = "organization_id", nullable = false)
    private String organizationId;

    @Column(name = "enrollment_date", nullable = false)
    private Date enrollmentDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "Active"; // Active, Suspended, Withdrawn

    @Column(name = "status_changed_date")
    private Date statusChangedDate;

    @Column(name = "status_changed_by")
    private String statusChangedBy;

    @Column(name = "withdrawal_reason")
    private String withdrawalReason;
}
```

**State Transitions**:

```
Active → Suspended → Active (reactivate)
Active → Withdrawn (terminal for that enrollment)
Suspended → Withdrawn (terminal)
```

---

### 2. EQALabProgramEnrollment (DM-008 — Self-Enrollment)

**Table**: `clinlims.eqa_lab_program_enrollment` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column        | Type          | Constraints             | Description                  |
| ------------- | ------------- | ----------------------- | ---------------------------- |
| id            | numeric(10,0) | PK, NOT NULL            | Sequence: eqa_lab_enroll_seq |
| program_name  | VARCHAR(255)  | NOT NULL                | External program name        |
| provider      | VARCHAR(255)  | NOT NULL                | Provider organization name   |
| description   | TEXT          | NULLABLE                | Notes about the program      |
| is_active     | BOOLEAN       | NOT NULL, DEFAULT true  | Active status                |
| created_date  | TIMESTAMP     | NOT NULL, DEFAULT NOW() | Record creation              |
| created_by    | numeric(10,0) | FK → system_user.id     | Creator                      |
| last_modified | TIMESTAMP     | NULLABLE                | Last edit timestamp          |
| sys_user_id   | numeric(10,0) | NOT NULL                | Audit: last modifying user   |
| lastupdated   | TIMESTAMP     | NOT NULL (version)      | Optimistic locking           |

**JPA Annotations**:

```java
@Entity
@Table(name = "eqa_lab_program_enrollment", schema = "clinlims")
public class EQALabProgramEnrollment extends BaseObject<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
                    generator = "eqa_lab_enroll_seq_gen")
    @GenericGenerator(name = "eqa_lab_enroll_seq_gen",
                      strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
                      parameters = {
                          @Parameter(name = "sequence_name",
                                     value = "eqa_lab_enroll_seq")
                      })
    @Column(name = "id")
    private String id;

    @Column(name = "program_name", nullable = false, length = 255)
    private String programName;

    @Column(name = "provider", nullable = false, length = 255)
    private String provider;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_date", nullable = false)
    private Date createdDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified")
    private Date lastModified;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<EQALabEnrollmentLabUnit> labUnits = new ArrayList<>();

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<EQALabEnrollmentTestMap> testMaps = new ArrayList<>();
}
```

---

### 3. EQALabEnrollmentLabUnit (DM-009 — Lab Unit Mapping)

**Table**: `clinlims.eqa_lab_enrollment_lab_unit` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column          | Type          | Constraints                                  | Description                    |
| --------------- | ------------- | -------------------------------------------- | ------------------------------ |
| id              | numeric(10,0) | PK, NOT NULL                                 | Sequence: eqa_lab_unit_map_seq |
| enrollment_id   | numeric(10,0) | FK → eqa_lab_program_enrollment.id, NOT NULL | Parent enrollment              |
| test_section_id | numeric(10,0) | FK → test_section.id, NOT NULL               | Lab unit / test section        |
| sys_user_id     | numeric(10,0) | NOT NULL                                     | Audit                          |
| lastupdated     | TIMESTAMP     | NOT NULL (version)                           | Optimistic locking             |

**Constraints**: UNIQUE on (`enrollment_id`, `test_section_id`)

```java
@Entity
@Table(name = "eqa_lab_enrollment_lab_unit", schema = "clinlims",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"enrollment_id", "test_section_id"}))
public class EQALabEnrollmentLabUnit extends BaseObject<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private EQALabProgramEnrollment enrollment;

    @Column(name = "test_section_id", nullable = false)
    private String testSectionId;
}
```

---

### 4. EQALabEnrollmentTestMap (DM-010 — Test/Panel Mapping)

**Table**: `clinlims.eqa_lab_enrollment_test_map` **Package**:
`org.openelisglobal.eqa.valueholder` **Extends**: `BaseObject<String>`

| Column        | Type          | Constraints                                  | Description                    |
| ------------- | ------------- | -------------------------------------------- | ------------------------------ |
| id            | numeric(10,0) | PK, NOT NULL                                 | Sequence: eqa_lab_test_map_seq |
| enrollment_id | numeric(10,0) | FK → eqa_lab_program_enrollment.id, NOT NULL | Parent enrollment              |
| test_id       | numeric(10,0) | FK → test.id, NULLABLE                       | Mapped test (null if panel)    |
| panel_id      | numeric(10,0) | FK → panel.id, NULLABLE                      | Mapped panel (null if test)    |
| sys_user_id   | numeric(10,0) | NOT NULL                                     | Audit                          |
| lastupdated   | TIMESTAMP     | NOT NULL (version)                           | Optimistic locking             |

**Constraints**: CHECK (`test_id IS NOT NULL OR panel_id IS NOT NULL`)

```java
@Entity
@Table(name = "eqa_lab_enrollment_test_map", schema = "clinlims")
public class EQALabEnrollmentTestMap extends BaseObject<String> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private EQALabProgramEnrollment enrollment;

    @Column(name = "test_id")
    private String testId;

    @Column(name = "panel_id")
    private String panelId;
}
```

---

## Liquibase Changesets

### eqa-009-create-enrollment-tables.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd">

  <!-- Sequences -->
  <changeSet id="eqa-009-01-sequences" author="eqa-team">
    <createSequence sequenceName="eqa_enrollment_seq"
                    schemaName="clinlims" startValue="1" incrementBy="1"/>
    <createSequence sequenceName="eqa_lab_enroll_seq"
                    schemaName="clinlims" startValue="1" incrementBy="1"/>
    <createSequence sequenceName="eqa_lab_unit_map_seq"
                    schemaName="clinlims" startValue="1" incrementBy="1"/>
    <createSequence sequenceName="eqa_lab_test_map_seq"
                    schemaName="clinlims" startValue="1" incrementBy="1"/>
  </changeSet>

  <!-- Provider-side enrollment (DM-007) -->
  <changeSet id="eqa-009-02-program-enrollment" author="eqa-team">
    <createTable tableName="eqa_program_enrollment" schemaName="clinlims">
      <column name="id" type="numeric(10,0)">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="eqa_program_id" type="numeric(10,0)">
        <constraints nullable="false"
                     foreignKeyName="fk_enrollment_program"
                     references="clinlims.eqa_program(id)"/>
      </column>
      <column name="organization_id" type="numeric(10,0)">
        <constraints nullable="false"
                     foreignKeyName="fk_enrollment_org"
                     references="clinlims.organization(id)"/>
      </column>
      <column name="enrollment_date" type="TIMESTAMP"
              defaultValueComputed="NOW()">
        <constraints nullable="false"/>
      </column>
      <column name="status" type="VARCHAR(20)" defaultValue="Active">
        <constraints nullable="false"/>
      </column>
      <column name="status_changed_date" type="TIMESTAMP"/>
      <column name="status_changed_by" type="numeric(10,0)"/>
      <column name="withdrawal_reason" type="TEXT"/>
      <column name="sys_user_id" type="numeric(10,0)">
        <constraints nullable="false"/>
      </column>
      <column name="lastupdated" type="TIMESTAMP"
              defaultValueComputed="NOW()"/>
    </createTable>
    <rollback>
      <dropTable tableName="eqa_program_enrollment"
                 schemaName="clinlims"/>
    </rollback>
  </changeSet>

  <!-- Partial unique index: one active enrollment per org per program -->
  <changeSet id="eqa-009-03-enrollment-unique-idx" author="eqa-team">
    <sql>
      CREATE UNIQUE INDEX idx_enrollment_active_unique
      ON clinlims.eqa_program_enrollment (eqa_program_id, organization_id)
      WHERE status = 'Active';
    </sql>
    <rollback>
      DROP INDEX IF EXISTS clinlims.idx_enrollment_active_unique;
    </rollback>
  </changeSet>

  <!-- Self-enrollment (DM-008) -->
  <changeSet id="eqa-009-04-lab-program-enrollment" author="eqa-team">
    <createTable tableName="eqa_lab_program_enrollment"
                 schemaName="clinlims">
      <column name="id" type="numeric(10,0)">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="program_name" type="VARCHAR(255)">
        <constraints nullable="false"/>
      </column>
      <column name="provider" type="VARCHAR(255)">
        <constraints nullable="false"/>
      </column>
      <column name="description" type="TEXT"/>
      <column name="is_active" type="BOOLEAN" defaultValueBoolean="true">
        <constraints nullable="false"/>
      </column>
      <column name="created_date" type="TIMESTAMP"
              defaultValueComputed="NOW()">
        <constraints nullable="false"/>
      </column>
      <column name="created_by" type="numeric(10,0)"/>
      <column name="last_modified" type="TIMESTAMP"/>
      <column name="sys_user_id" type="numeric(10,0)">
        <constraints nullable="false"/>
      </column>
      <column name="lastupdated" type="TIMESTAMP"
              defaultValueComputed="NOW()"/>
    </createTable>
    <rollback>
      <dropTable tableName="eqa_lab_program_enrollment"
                 schemaName="clinlims"/>
    </rollback>
  </changeSet>

  <!-- Lab unit mapping (DM-009) -->
  <changeSet id="eqa-009-05-lab-enrollment-lab-unit" author="eqa-team">
    <createTable tableName="eqa_lab_enrollment_lab_unit"
                 schemaName="clinlims">
      <column name="id" type="numeric(10,0)">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="enrollment_id" type="numeric(10,0)">
        <constraints nullable="false"
                     foreignKeyName="fk_lab_unit_enrollment"
                     references="clinlims.eqa_lab_program_enrollment(id)"/>
      </column>
      <column name="test_section_id" type="numeric(10,0)">
        <constraints nullable="false"
                     foreignKeyName="fk_lab_unit_section"
                     references="clinlims.test_section(id)"/>
      </column>
      <column name="sys_user_id" type="numeric(10,0)">
        <constraints nullable="false"/>
      </column>
      <column name="lastupdated" type="TIMESTAMP"
              defaultValueComputed="NOW()"/>
    </createTable>
    <addUniqueConstraint tableName="eqa_lab_enrollment_lab_unit"
                         schemaName="clinlims"
                         columnNames="enrollment_id, test_section_id"
                         constraintName="uq_lab_enrollment_unit"/>
    <rollback>
      <dropTable tableName="eqa_lab_enrollment_lab_unit"
                 schemaName="clinlims"/>
    </rollback>
  </changeSet>

  <!-- Test/panel mapping (DM-010) -->
  <changeSet id="eqa-009-06-lab-enrollment-test-map" author="eqa-team">
    <createTable tableName="eqa_lab_enrollment_test_map"
                 schemaName="clinlims">
      <column name="id" type="numeric(10,0)">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="enrollment_id" type="numeric(10,0)">
        <constraints nullable="false"
                     foreignKeyName="fk_test_map_enrollment"
                     references="clinlims.eqa_lab_program_enrollment(id)"/>
      </column>
      <column name="test_id" type="numeric(10,0)">
        <constraints foreignKeyName="fk_test_map_test"
                     references="clinlims.test(id)"/>
      </column>
      <column name="panel_id" type="numeric(10,0)">
        <constraints foreignKeyName="fk_test_map_panel"
                     references="clinlims.panel(id)"/>
      </column>
      <column name="sys_user_id" type="numeric(10,0)">
        <constraints nullable="false"/>
      </column>
      <column name="lastupdated" type="TIMESTAMP"
              defaultValueComputed="NOW()"/>
    </createTable>
    <sql>
      ALTER TABLE clinlims.eqa_lab_enrollment_test_map
      ADD CONSTRAINT chk_test_or_panel
      CHECK (test_id IS NOT NULL OR panel_id IS NOT NULL);
    </sql>
    <rollback>
      <dropTable tableName="eqa_lab_enrollment_test_map"
                 schemaName="clinlims"/>
    </rollback>
  </changeSet>

</databaseChangeLog>
```

### eqa-010-restructure-menu.xml

Navigation restructure changeset — moves from single "EQA" parent to "EQA
Tests" + "EQA Management" parents + standalone "Alerts". See research.md R-A3
for details. Changeset to be written during implementation.
