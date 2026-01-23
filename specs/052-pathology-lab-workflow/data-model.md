# Data Model: Pathology Laboratory Workflow

**Date**: 2025-12-14 **Feature**: 052-pathology-lab-workflow **Purpose**: Define
entities, relationships, and database schema for pathology workflow

## Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           PATHOLOGY LABORATORY DATA MODEL                           │
└─────────────────────────────────────────────────────────────────────────────────────┘

                                    ┌─────────────┐
                                    │   NoteBook  │ (existing)
                                    │  (template) │
                                    └──────┬──────┘
                                           │ 1:N
                                    ┌──────▼──────┐
                                    │NotebookEntry│ (existing)
                                    │  (instance) │
                                    └──────┬──────┘
                                           │ 1:N
                              ┌────────────┼────────────┐
                              │            │            │
                       ┌──────▼──────┐     │     ┌──────▼──────┐
                       │ NoteBookPage│     │     │NoteBookFile │ (existing)
                       │  (workflow  │     │     │(attachments)│
                       │    steps)   │     │     └─────────────┘
                       └──────┬──────┘     │
                              │ 1:N        │ 1:N
                  ┌───────────┴───────┐    │
                  │                   │    │
           ┌──────▼──────┐     ┌──────▼────▼───┐
           │NotebookPage │     │   SampleItem  │ (existing)
           │   Sample    │     │   (specimen)  │
           │ (progress)  │     └───────┬───────┘
           └─────────────┘             │
                                       │ 1:1
                              ┌────────▼────────┐
                              │   Pathology     │ NEW
                              │    Sample       │
                              │  Registration   │
                              └────────┬────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
       ┌──────▼──────┐          ┌──────▼──────┐          ┌──────▼──────┐
       │  Quality    │          │ Processing  │          │   Test      │
       │  Control    │          │   Log       │          │  Result     │
       │   Record    │          │   Entry     │          │  Record     │
       └─────────────┘          └─────────────┘          └─────────────┘


                    STORAGE & MONITORING                    REFERENCE & ACCESS

       ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
       │ StorageDevice   │      │  Reference      │      │   Project       │
       │   (existing)    │◄─────│  Document       │      │   Access        │
       └────────┬────────┘      │  (SOP/Protocol) │      │ (permissions)   │
                │ 1:N           └─────────────────┘      └─────────────────┘
       ┌────────▼────────┐
       │   Storage       │
       │  Environment    │
       │     Log         │
       └─────────────────┘
```

---

## New Entities

### 1. PathologySampleRegistration

**Purpose**: Extended metadata for pathology specimens, distinguishing clinical
from research samples.

**Table**: `pathology_sample_registration`

| Column               | Type         | Constraints              | Description                       |
| -------------------- | ------------ | ------------------------ | --------------------------------- |
| id                   | INTEGER      | PK, SEQUENCE             | Primary key                       |
| sample_item_id       | VARCHAR(255) | FK, UNIQUE, NOT NULL     | Reference to SampleItem           |
| fhir_uuid            | UUID         |                          | FHIR Specimen reference           |
| category             | VARCHAR(20)  | NOT NULL                 | CLINICAL, RESEARCH                |
| patient_id           | VARCHAR(100) |                          | Patient identifier (clinical)     |
| requesting_clinician | VARCHAR(255) |                          | Ordering clinician name           |
| clinical_details     | TEXT         |                          | Free-text clinical history        |
| specimen_site        | VARCHAR(255) |                          | Anatomical collection site        |
| study_id             | VARCHAR(100) |                          | Research study identifier         |
| pi_name              | VARCHAR(255) |                          | Principal investigator (research) |
| participant_id       | VARCHAR(100) |                          | Participant/animal ID (research)  |
| ethical_approval_ref | VARCHAR(255) |                          | IRB/ethics approval reference     |
| sample_source        | VARCHAR(255) | DEFAULT 'Alert Hospital' | Source institution                |
| receiving_date       | TIMESTAMP    | NOT NULL                 | Date/time received                |
| receiving_staff_id   | INTEGER      | FK → system_user         | Staff who received sample         |
| lastupdated          | TIMESTAMP    | VERSION                  | Optimistic locking                |

**Relationships**:

- `sample_item_id` → `sample_item.id` (1:1)
- `receiving_staff_id` → `system_user.id` (N:1)

**Indexes**:

- `idx_path_reg_sample` ON (sample_item_id)
- `idx_path_reg_category` ON (category)
- `idx_path_reg_study` ON (study_id)
- `idx_path_reg_patient` ON (patient_id)

---

### 2. QualityControlRecord

**Purpose**: QC inspection records for samples and tissue blocks.

**Table**: `quality_control_record`

| Column                | Type         | Constraints                | Description                                                                                           |
| --------------------- | ------------ | -------------------------- | ----------------------------------------------------------------------------------------------------- |
| id                    | INTEGER      | PK, SEQUENCE               | Primary key                                                                                           |
| sample_item_id        | VARCHAR(255) | FK, NOT NULL               | Sample being QC'd                                                                                     |
| fhir_uuid             | UUID         |                            | FHIR Observation reference                                                                            |
| qc_type               | VARCHAR(30)  | NOT NULL                   | INITIAL_INSPECTION, BLOCK_QC, SLIDE_QC, STAIN_QC                                                      |
| status                | VARCHAR(10)  | NOT NULL                   | PASS, FAIL                                                                                            |
| criteria_results      | JSONB        |                            | Specimen-type-specific criteria                                                                       |
| failure_reason        | TEXT         |                            | Required if status=FAIL                                                                               |
| action_taken          | VARCHAR(50)  |                            | RECOLLECTION_REQUESTED, PROCESSED_WITH_LIMITATIONS, AWAITING_PI_DECISION, RE_EMBED, MELT_AND_RE_EMBED |
| technician_id         | INTEGER      | FK → system_user, NOT NULL | Technician performing QC                                                                              |
| recorded_at           | TIMESTAMP    | NOT NULL                   | QC timestamp                                                                                          |
| pathologist_escalated | BOOLEAN      | DEFAULT FALSE              | Escalated for pathologist review                                                                      |
| notes                 | TEXT         |                            | Additional remarks                                                                                    |
| lastupdated           | TIMESTAMP    | VERSION                    | Optimistic locking                                                                                    |

**Relationships**:

- `sample_item_id` → `sample_item.id` (N:1)
- `technician_id` → `system_user.id` (N:1)

**Indexes**:

- `idx_qc_sample` ON (sample_item_id)
- `idx_qc_status` ON (status)
- `idx_qc_type_status` ON (qc_type, status)
- `idx_qc_recorded` ON (recorded_at)

**JSONB Schema Examples**:

```json
// INITIAL_INSPECTION - Histology
{
  "requisitionMatch": true,
  "fixationType": "10% NBF",
  "fixationRatio": "10:1",
  "fixationDuration": "24h",
  "tissueIntegrity": "adequate"
}

// BLOCK_QC
{
  "surfaceSmoothness": true,
  "correctDepth": true,
  "correctOrientation": true,
  "paraffinOverflow": false
}
```

---

### 3. ProcessingLogEntry

**Purpose**: Step-by-step processing records for audit trail.

**Table**: `processing_log_entry`

| Column               | Type         | Constraints                | Description                                |
| -------------------- | ------------ | -------------------------- | ------------------------------------------ |
| id                   | INTEGER      | PK, SEQUENCE               | Primary key                                |
| sample_item_id       | VARCHAR(255) | FK, NOT NULL               | Sample being processed                     |
| step_name            | VARCHAR(100) | NOT NULL                   | e.g., "Grossing", "Embedding", "Microtomy" |
| step_details         | JSONB        |                            | Step-specific data                         |
| parent_processing_id | INTEGER      | FK → self                  | Parent step (for hierarchy)                |
| completed_by         | INTEGER      | FK → system_user, NOT NULL | Staff performing step                      |
| completed_at         | TIMESTAMP    | NOT NULL                   | Completion timestamp                       |
| notes                | TEXT         |                            | Free-text notes                            |
| lastupdated          | TIMESTAMP    | VERSION                    | Optimistic locking                         |

**Relationships**:

- `sample_item_id` → `sample_item.id` (N:1)
- `completed_by` → `system_user.id` (N:1)
- `parent_processing_id` → `processing_log_entry.id` (N:1, self-reference)

**Indexes**:

- `idx_proc_sample` ON (sample_item_id)
- `idx_proc_step` ON (step_name)
- `idx_proc_completed` ON (completed_at)

**JSONB Schema Examples**:

```json
// Grossing
{
  "grossExamFindings": "Tan-brown tissue, 2.5x1.8cm",
  "description": "Lymph node with focal hemorrhage",
  "sectionCount": 4
}

// Embedding
{
  "processingStages": ["70% alcohol", "95% alcohol", "xylene", "paraffin"],
  "processingDuration": "18h"
}

// Microtomy
{
  "sectionThickness": "4µm",
  "slideCount": 6
}
```

---

### 4. TestResultRecord

**Purpose**: Test/assay results with control validation and pathologist
sign-off.

**Table**: `test_result_record`

| Column                  | Type         | Constraints                | Description                                           |
| ----------------------- | ------------ | -------------------------- | ----------------------------------------------------- |
| id                      | INTEGER      | PK, SEQUENCE               | Primary key                                           |
| sample_item_id          | VARCHAR(255) | FK, NOT NULL               | Sample/slide being tested                             |
| fhir_uuid               | UUID         |                            | FHIR DiagnosticReport reference                       |
| block_slide_id          | VARCHAR(100) |                            | Block or slide identifier                             |
| test_type               | VARCHAR(50)  | NOT NULL                   | ROUTINE_STAIN, SPECIAL_STAIN, IHC, ICC, ISH, RESEARCH |
| stain_name              | VARCHAR(100) | NOT NULL                   | e.g., "H&E", "CD20", "AFB"                            |
| protocol_reference_id   | INTEGER      | FK → reference_document    | SOP used                                              |
| positive_control_status | VARCHAR(10)  |                            | PASS, FAIL (for IHC/ICC/ISH)                          |
| negative_control_status | VARCHAR(10)  |                            | PASS, FAIL (for IHC/ICC/ISH)                          |
| stain_quality           | VARCHAR(20)  |                            | ACCEPTABLE, SUBOPTIMAL, FAILED                        |
| result_data             | JSONB        | NOT NULL                   | Test-specific results                                 |
| pathologist_signoff_id  | INTEGER      | FK → system_user           | Pathologist reviewing                                 |
| signoff_timestamp       | TIMESTAMP    |                            | Sign-off date/time                                    |
| performed_by            | INTEGER      | FK → system_user, NOT NULL | Technician performing test                            |
| performed_at            | TIMESTAMP    | NOT NULL                   | Test timestamp                                        |
| lastupdated             | TIMESTAMP    | VERSION                    | Optimistic locking                                    |

**Relationships**:

- `sample_item_id` → `sample_item.id` (N:1)
- `protocol_reference_id` → `reference_document.id` (N:1)
- `pathologist_signoff_id` → `system_user.id` (N:1)
- `performed_by` → `system_user.id` (N:1)

**Indexes**:

- `idx_test_sample` ON (sample_item_id)
- `idx_test_type` ON (test_type)
- `idx_test_stain` ON (stain_name)
- `idx_test_signoff` ON (pathologist_signoff_id, signoff_timestamp)

**JSONB Schema Examples**:

```json
// H&E Result
{
  "morphologyDescription": "Malignant cells with high N:C ratio",
  "diagnosis": "Invasive ductal carcinoma, Grade 2"
}

// IHC Result
{
  "stainingPattern": "membranous",
  "intensity": "3+",
  "percentPositive": 85,
  "interpretation": "Positive for HER2"
}

// Special Stain (AFB)
{
  "result": "Positive",
  "organismsObserved": "acid-fast bacilli",
  "quantitation": "rare"
}
```

---

### 5. StorageEnvironmentLog

**Purpose**: Temperature monitoring for storage units.

**Table**: `storage_environment_log`

| Column               | Type         | Constraints                   | Description                              |
| -------------------- | ------------ | ----------------------------- | ---------------------------------------- |
| id                   | INTEGER      | PK, SEQUENCE                  | Primary key                              |
| storage_unit_id      | INTEGER      | FK → storage_device, NOT NULL | Storage unit (freezer, fridge)           |
| recorded_temperature | DECIMAL(5,2) | NOT NULL                      | Temperature in Celsius                   |
| recorded_at          | TIMESTAMP    | NOT NULL                      | Recording timestamp                      |
| recorded_by          | INTEGER      | FK → system_user, NOT NULL    | Staff recording                          |
| is_excursion         | BOOLEAN      | NOT NULL                      | Out of acceptable range                  |
| notes                | TEXT         |                               | Excursion explanation, corrective action |

**Note**: This entity is marked `@Immutable` - records cannot be updated after
creation.

**Relationships**:

- `storage_unit_id` → `storage_device.id` (N:1)
- `recorded_by` → `system_user.id` (N:1)

**Indexes**:

- `idx_env_unit` ON (storage_unit_id)
- `idx_env_excursion` ON (is_excursion, recorded_at)
- `idx_env_date` ON (recorded_at)

---

### 6. ReferenceDocument

**Purpose**: SOP and protocol document management with version control.

**Table**: `reference_document`

| Column         | Type         | Constraints                | Description                       |
| -------------- | ------------ | -------------------------- | --------------------------------- |
| id             | INTEGER      | PK, SEQUENCE               | Primary key                       |
| title          | VARCHAR(255) | NOT NULL                   | Document title                    |
| document_type  | VARCHAR(20)  | NOT NULL                   | SOP, PROTOCOL, REFERENCE          |
| file_path      | VARCHAR(500) |                            | Path to file storage              |
| file_content   | BYTEA        |                            | Inline file content (alternative) |
| file_name      | VARCHAR(255) | NOT NULL                   | Original filename                 |
| mime_type      | VARCHAR(100) |                            | application/pdf, etc.             |
| version        | VARCHAR(20)  | NOT NULL                   | e.g., "2.1"                       |
| effective_date | DATE         | NOT NULL                   | When document became effective    |
| change_summary | TEXT         |                            | What changed in this version      |
| uploaded_by    | INTEGER      | FK → system_user, NOT NULL | Uploader                          |
| uploaded_at    | TIMESTAMP    | NOT NULL                   | Upload timestamp                  |
| replaced_by_id | INTEGER      | FK → self                  | Newer version (NULL = current)    |
| is_active      | BOOLEAN      | DEFAULT TRUE               | Soft delete                       |
| lastupdated    | TIMESTAMP    | VERSION                    | Optimistic locking                |

**Relationships**:

- `uploaded_by` → `system_user.id` (N:1)
- `replaced_by_id` → `reference_document.id` (N:1, version chain)

**Indexes**:

- `idx_refdoc_title` ON (title)
- `idx_refdoc_type` ON (document_type)
- `idx_refdoc_current` ON (title, replaced_by_id) WHERE replaced_by_id IS NULL
- `idx_refdoc_active` ON (is_active)

---

### 7. ProjectAccess

**Purpose**: Project-based access control for research samples.

**Table**: `project_access`

| Column      | Type        | Constraints                | Description                          |
| ----------- | ----------- | -------------------------- | ------------------------------------ |
| id          | INTEGER     | PK, SEQUENCE               | Primary key                          |
| project_id  | INTEGER     | FK → project, NOT NULL     | Project reference                    |
| user_id     | INTEGER     | FK → system_user, NOT NULL | Authorized user                      |
| access_role | VARCHAR(20) | NOT NULL                   | TECHNICIAN, COORDINATOR, PATHOLOGIST |
| granted_by  | INTEGER     | FK → system_user, NOT NULL | Who granted access                   |
| granted_at  | TIMESTAMP   | NOT NULL                   | Grant timestamp                      |
| revoked_at  | TIMESTAMP   |                            | Revocation timestamp (NULL = active) |
| lastupdated | TIMESTAMP   | VERSION                    | Optimistic locking                   |

**Constraints**:

- UNIQUE (project_id, user_id) WHERE revoked_at IS NULL

**Relationships**:

- `project_id` → `project.id` (N:1)
- `user_id` → `system_user.id` (N:1)
- `granted_by` → `system_user.id` (N:1)

**Indexes**:

- `idx_proj_access_project` ON (project_id)
- `idx_proj_access_user` ON (user_id)
- `idx_proj_access_active` ON (project_id, user_id, revoked_at) WHERE revoked_at
  IS NULL

---

## Existing Entities to Extend

### SampleItem (existing)

**Additional Fields Needed** (if not present):

- Verify `parent_sample_item_id` exists for parent-child hierarchy
- Verify `external_id` exists for manifest ID storage

### StorageDevice (existing)

**Additional Fields Needed**:

- `min_temp DECIMAL(5,2)` - Minimum acceptable temperature
- `max_temp DECIMAL(5,2)` - Maximum acceptable temperature

---

## Database Schema (Liquibase)

### Changeset Order

| File                                    | Description                                              |
| --------------------------------------- | -------------------------------------------------------- |
| `pathology-001-sample-registration.xml` | PathologySampleRegistration table                        |
| `pathology-002-quality-control.xml`     | QualityControlRecord table                               |
| `pathology-003-processing-log.xml`      | ProcessingLogEntry table                                 |
| `pathology-004-test-result.xml`         | TestResultRecord table                                   |
| `pathology-005-storage-environment.xml` | StorageEnvironmentLog table + StorageDevice temp columns |
| `pathology-006-reference-document.xml`  | ReferenceDocument table                                  |
| `pathology-007-project-access.xml`      | ProjectAccess table                                      |
| `pathology-008-seed-data.xml`           | Sample types, QC criteria templates, predefined sources  |

### Sample Liquibase Changeset

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd">

    <changeSet id="pathology-001-sample-registration" author="speckit">
        <createSequence sequenceName="pathology_sample_registration_seq"
                        startValue="1" incrementBy="1"/>

        <createTable tableName="pathology_sample_registration">
            <column name="id" type="INTEGER">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="sample_item_id" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"
                             foreignKeyName="fk_path_reg_sample"
                             references="sample_item(id)"/>
            </column>
            <column name="fhir_uuid" type="UUID"/>
            <column name="category" type="VARCHAR(20)">
                <constraints nullable="false"/>
            </column>
            <column name="patient_id" type="VARCHAR(100)"/>
            <column name="requesting_clinician" type="VARCHAR(255)"/>
            <column name="clinical_details" type="TEXT"/>
            <column name="specimen_site" type="VARCHAR(255)"/>
            <column name="study_id" type="VARCHAR(100)"/>
            <column name="pi_name" type="VARCHAR(255)"/>
            <column name="participant_id" type="VARCHAR(100)"/>
            <column name="ethical_approval_ref" type="VARCHAR(255)"/>
            <column name="sample_source" type="VARCHAR(255)" defaultValue="Alert Hospital"/>
            <column name="receiving_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="receiving_staff_id" type="INTEGER">
                <constraints foreignKeyName="fk_path_reg_staff"
                             references="system_user(id)"/>
            </column>
            <column name="lastupdated" type="TIMESTAMP"/>
        </createTable>

        <createIndex tableName="pathology_sample_registration"
                     indexName="idx_path_reg_sample">
            <column name="sample_item_id"/>
        </createIndex>

        <createIndex tableName="pathology_sample_registration"
                     indexName="idx_path_reg_category">
            <column name="category"/>
        </createIndex>

        <rollback>
            <dropTable tableName="pathology_sample_registration"/>
            <dropSequence sequenceName="pathology_sample_registration_seq"/>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

---

## Enumerations

### SampleCategory

```java
public enum SampleCategory {
    CLINICAL,   // Clinical diagnostic specimens
    RESEARCH    // Research specimens
}
```

### QCType

```java
public enum QCType {
    INITIAL_INSPECTION,  // Reception QC
    BLOCK_QC,            // Post-embedding tissue block
    SLIDE_QC,            // Slide quality
    STAIN_QC             // Staining quality
}
```

### QCStatus

```java
public enum QCStatus {
    PASS,
    FAIL
}
```

### QCAction

```java
public enum QCAction {
    RECOLLECTION_REQUESTED,
    PROCESSED_WITH_LIMITATIONS,
    AWAITING_PI_DECISION,
    RE_EMBED,
    MELT_AND_RE_EMBED
}
```

### TestType

```java
public enum TestType {
    ROUTINE_STAIN,    // H&E, Papanicolaou, Romanowsky
    SPECIAL_STAIN,    // AFB, GMS, PAS, Gram
    IHC,              // Immunohistochemistry
    ICC,              // Immunocytochemistry
    ISH,              // In-situ hybridization
    RESEARCH          // Research assays
}
```

### StainQuality

```java
public enum StainQuality {
    ACCEPTABLE,
    SUBOPTIMAL,
    FAILED
}
```

### ControlStatus

```java
public enum ControlStatus {
    PASS,
    FAIL
}
```

### DocumentType

```java
public enum DocumentType {
    SOP,        // Standard Operating Procedure
    PROTOCOL,   // Test protocol
    REFERENCE   // Reference material
}
```

### AccessRole

```java
public enum AccessRole {
    TECHNICIAN,     // Lab technician
    COORDINATOR,    // Project coordinator
    PATHOLOGIST     // Pathologist
}
```

---

## Validation Rules

### PathologySampleRegistration

- `category` must be CLINICAL or RESEARCH
- If CLINICAL: `patient_id` recommended (not required for anonymous samples)
- If RESEARCH: `study_id` and `pi_name` recommended
- `receiving_date` cannot be in the future
- `sample_source` defaults to "Alert Hospital"

### QualityControlRecord

- `failure_reason` required when `status` = FAIL
- `action_taken` recommended when `status` = FAIL
- `criteria_results` JSONB must contain specimen-type-appropriate fields

### TestResultRecord

- `positive_control_status` and `negative_control_status` required for IHC, ICC,
  ISH
- `pathologist_signoff_id` required for clinical samples before results released
- `result_data` JSONB must contain test-type-appropriate fields

### StorageEnvironmentLog

- `is_excursion` auto-calculated based on storage_device temp range
- Cannot be updated after creation (immutable)

### ReferenceDocument

- New version must have higher version number than previous
- `replaced_by_id` creates version chain; current version has NULL

### ProjectAccess

- Unique (project_id, user_id) for active grants (where revoked_at IS NULL)
- `granted_by` cannot equal `user_id` (self-grant prevention)

---

## State Transitions

### QualityControlRecord State Machine

```
                  ┌──────────────┐
                  │   Pending    │ (implicit - no record exists)
                  └──────┬───────┘
                         │ Inspection performed
                  ┌──────▼───────┐
        ┌─────────┤ QC Recorded  ├─────────┐
        │         └──────────────┘         │
        │ Pass                             │ Fail
  ┌─────▼─────┐                      ┌─────▼─────┐
  │   PASS    │                      │   FAIL    │
  │ (cleared) │                      │ (blocked) │
  └───────────┘                      └─────┬─────┘
                                           │ Action taken
                                     ┌─────▼─────┐
                                     │  Action   │
                                     │ Recorded  │
                                     └───────────┘
```

### TestResultRecord State Machine

```
         ┌──────────────┐
         │   Pending    │ (implicit - no record exists)
         └──────┬───────┘
                │ Test performed
         ┌──────▼───────┐
         │  Recorded    │
         │(needs sign-off)│
         └──────┬───────┘
                │ Pathologist reviews
        ┌───────┼───────┐
        │       │       │
  ┌─────▼─┐ ┌───▼───┐ ┌─▼─────┐
  │Signed │ │Review │ │Rejected│
  │  Off  │ │Pending│ │(redo) │
  └───────┘ └───────┘ └───────┘
```

---

## Performance Considerations

### Indexes Summary

| Table                         | Index                  | Purpose                       |
| ----------------------------- | ---------------------- | ----------------------------- |
| pathology_sample_registration | idx_path_reg_sample    | Fast lookup by sample         |
| pathology_sample_registration | idx_path_reg_category  | Filter by clinical/research   |
| quality_control_record        | idx_qc_sample          | QC history per sample         |
| quality_control_record        | idx_qc_type_status     | Filter failed QCs by type     |
| processing_log_entry          | idx_proc_sample        | Processing history per sample |
| test_result_record            | idx_test_sample        | Results per sample            |
| test_result_record            | idx_test_signoff       | Pending sign-offs             |
| storage_environment_log       | idx_env_excursion      | Excursion monitoring          |
| reference_document            | idx_refdoc_current     | Current version lookup        |
| project_access                | idx_proj_access_active | Active access grants          |

### Query Optimization Notes

1. **Sample traceability queries**: Use `JOIN FETCH` to load registration + QC +
   processing in single query
2. **Performance reports**: Aggregate queries on timestamps, consider
   materialized views if slow
3. **Document version lookup**: Partial index on `replaced_by_id IS NULL` for
   current versions
4. **Access control filtering**: Index on (project_id, user_id, revoked_at) with
   partial index

---

## FHIR Mapping Summary

| Entity                      | FHIR Resource                    | Profile          |
| --------------------------- | -------------------------------- | ---------------- |
| PathologySampleRegistration | Specimen                         | IHE LAB Specimen |
| SampleItem (child)          | Specimen (with parent reference) | IHE LAB Specimen |
| QualityControlRecord        | Observation (quality)            | Custom extension |
| TestResultRecord            | DiagnosticReport + Observation   | IHE LAB Results  |
| ReferenceDocument           | DocumentReference                | FHIR Core        |
| StorageEnvironmentLog       | Observation (temperature)        | Custom extension |
