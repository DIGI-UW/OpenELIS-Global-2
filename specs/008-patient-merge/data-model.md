# Data Model: Patient Merge Backend

**Feature**: Patient Merge Backend Implementation **Branch**:
`008-patient-merge-backend` **Date**: 2025-12-08 **Status**: Design Complete
(with 1 blocker tracked)

---

## Purpose

This document defines the complete data model for the patient merge backend
feature, including database schema changes, entity models, relationships, FHIR
mappings, and DTOs. All designs follow the
[OpenELIS Global 3.0 Constitution](../../.specify/memory/constitution.md) and
existing patterns.

---

## 1. Database Schema Changes

### 1.1 New Table: `patient_merge_audit`

**Purpose**: Comprehensive audit trail for all patient merge operations

**Liquibase Changeset Location**:
`src/main/resources/liquibase/3.3.x.x/patient-merge-audit.xml`

**Schema Definition**:

```sql
CREATE TABLE patient_merge_audit (
    id BIGSERIAL PRIMARY KEY,
    primary_patient_id BIGINT NOT NULL,
    merged_patient_id BIGINT NOT NULL,
    merge_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    performed_by_user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    data_summary JSONB,

    -- Audit fields (standard pattern)
    lastupdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sys_user_id BIGINT,

    -- Foreign key constraints
    CONSTRAINT fk_pma_primary_patient FOREIGN KEY (primary_patient_id)
        REFERENCES patient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pma_merged_patient FOREIGN KEY (merged_patient_id)
        REFERENCES patient(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pma_performed_by FOREIGN KEY (performed_by_user_id)
        REFERENCES system_user(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pma_sys_user FOREIGN KEY (sys_user_id)
        REFERENCES system_user(id) ON DELETE SET NULL
);

-- Indexes for query performance
CREATE INDEX idx_pma_primary_patient ON patient_merge_audit(primary_patient_id);
CREATE INDEX idx_pma_merged_patient ON patient_merge_audit(merged_patient_id);
CREATE INDEX idx_pma_merge_date ON patient_merge_audit(merge_date);
CREATE INDEX idx_pma_performed_by ON patient_merge_audit(performed_by_user_id);

-- GIN index for JSONB queries
CREATE INDEX idx_pma_data_summary ON patient_merge_audit USING gin(data_summary);
```

**Rollback Definition**:

```sql
DROP TABLE IF EXISTS patient_merge_audit CASCADE;
```

**JSONB `data_summary` Structure**:

```json
{
  "orders": {
    "total": 23,
    "active": 2
  },
  "results": {
    "total": 47,
    "pending": 3,
    "completed": 44
  },
  "samples": {
    "total": 12
  },
  "identities": {
    "total": 3,
    "preserved": 2,
    "discarded": 1,
    "conflicts": ["National ID"]
  },
  "contacts": {
    "total": 2
  },
  "documents": {
    "total": 5
  },
  "relations": {
    "total": 1
  },
  "merge_duration_ms": 2847
}
```

---

### 1.2 Updated Table: `patient`

**Purpose**: Add merge tracking fields to existing patient table

**Liquibase Changeset Location**:
`src/main/resources/liquibase/3.3.x.x/patient-merge-tracking.xml`

**Schema Alterations**:

```sql
-- Add new columns
ALTER TABLE patient
    ADD COLUMN merged_into_patient_id BIGINT,
    ADD COLUMN is_merged BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN merge_date TIMESTAMP;

-- Add foreign key constraint (self-referencing)
ALTER TABLE patient
    ADD CONSTRAINT fk_patient_merged_into
    FOREIGN KEY (merged_into_patient_id)
    REFERENCES patient(id) ON DELETE RESTRICT;

-- Add indexes for merge queries
CREATE INDEX idx_patient_is_merged ON patient(is_merged);
CREATE INDEX idx_patient_merged_into ON patient(merged_into_patient_id);

-- Add check constraint
ALTER TABLE patient
    ADD CONSTRAINT chk_patient_merge_consistency
    CHECK ((is_merged = TRUE AND merged_into_patient_id IS NOT NULL AND merge_date IS NOT NULL)
        OR (is_merged = FALSE AND merged_into_patient_id IS NULL AND merge_date IS NULL));
```

**Rollback Definition**:

```sql
ALTER TABLE patient DROP CONSTRAINT IF EXISTS fk_patient_merged_into;
ALTER TABLE patient DROP CONSTRAINT IF EXISTS chk_patient_merge_consistency;
DROP INDEX IF EXISTS idx_patient_is_merged;
DROP INDEX IF EXISTS idx_patient_merged_into;
ALTER TABLE patient DROP COLUMN IF EXISTS merged_into_patient_id;
ALTER TABLE patient DROP COLUMN IF EXISTS is_merged;
ALTER TABLE patient DROP COLUMN IF EXISTS merge_date;
```

**Updated Patient Table Structure**:

```
patient
├── id (PK)
├── race
├── gender
├── birth_date
├── national_id
├── external_id
├── fhir_uuid
├── merged_into_patient_id (FK → patient.id) [NEW]
├── is_merged (BOOLEAN) [NEW]
├── merge_date (TIMESTAMP) [NEW]
├── lastupdated
└── sys_user_id
```

---

## 2. Entity Model (JPA/Hibernate)

### 2.1 New Entity: `PatientMergeAudit`

**Location**:
`src/main/java/org/openelisglobal/patient/merge/valueholder/PatientMergeAudit.java`

**Entity Definition**:

```java
package org.openelisglobal.patient.merge.valueholder;

import java.sql.Timestamp;
import javax.persistence.*;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "patient_merge_audit")
public class PatientMergeAudit extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private String id;

    @Column(name = "primary_patient_id", nullable = false)
    private String primaryPatientId;

    @Column(name = "merged_patient_id", nullable = false)
    private String mergedPatientId;

    @Column(name = "merge_date", nullable = false)
    private Timestamp mergeDate;

    @Column(name = "performed_by_user_id", nullable = false)
    private String performedByUserId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Type(type = "jsonb")
    @Column(name = "data_summary", columnDefinition = "jsonb")
    private JsonNode dataSummary;

    @Column(name = "lastupdated", insertable = false, updatable = false)
    private Timestamp lastupdated;

    @Column(name = "sys_user_id")
    private String sysUserId;

    // Constructors
    public PatientMergeAudit() {
        super();
    }

    // Getters and Setters
    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getPrimaryPatientId() {
        return primaryPatientId;
    }

    public void setPrimaryPatientId(String primaryPatientId) {
        this.primaryPatientId = primaryPatientId;
    }

    public String getMergedPatientId() {
        return mergedPatientId;
    }

    public void setMergedPatientId(String mergedPatientId) {
        this.mergedPatientId = mergedPatientId;
    }

    public Timestamp getMergeDate() {
        return mergeDate;
    }

    public void setMergeDate(Timestamp mergeDate) {
        this.mergeDate = mergeDate;
    }

    public String getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(String performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JsonNode getDataSummary() {
        return dataSummary;
    }

    public void setDataSummary(JsonNode dataSummary) {
        this.dataSummary = dataSummary;
    }

    public Timestamp getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Timestamp lastupdated) {
        this.lastupdated = lastupdated;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }
}
```

---

### 2.2 Updated Entity: `Patient`

**Location**:
`src/main/java/org/openelisglobal/patient/valueholder/Patient.java`

**New Fields to Add**:

```java
@Entity
@Table(name = "patient")
@Indexed
public class Patient extends BaseObject<String> {
    // ... existing fields ...

    // NEW FIELDS FOR MERGE TRACKING
    @Column(name = "merged_into_patient_id")
    private String mergedIntoPatientId;

    @Column(name = "is_merged", nullable = false)
    private Boolean isMerged = false;

    @Column(name = "merge_date")
    private Timestamp mergeDate;

    // NEW GETTERS AND SETTERS
    public String getMergedIntoPatientId() {
        return mergedIntoPatientId;
    }

    public void setMergedIntoPatientId(String mergedIntoPatientId) {
        this.mergedIntoPatientId = mergedIntoPatientId;
    }

    public Boolean getIsMerged() {
        return isMerged != null ? isMerged : false;
    }

    public void setIsMerged(Boolean isMerged) {
        this.isMerged = isMerged;
    }

    public Timestamp getMergeDate() {
        return mergeDate;
    }

    public void setMergeDate(Timestamp mergeDate) {
        this.mergeDate = mergeDate;
    }
}
```

---

## 3. Related Tables and Relationships

### 3.1 Tables Updated During Merge

These tables have foreign keys to `patient.id` that must be updated during
merge:

| Table                 | Foreign Key Column             | Update Strategy | Estimated Rows    |
| --------------------- | ------------------------------ | --------------- | ----------------- |
| `patient_identity`    | `patient_id`                   | HQL bulk UPDATE | 1-5 per patient   |
| `patient_contact`     | `patient_id`                   | HQL bulk UPDATE | 1-3 per patient   |
| `external_patient_id` | `patient_id`                   | HQL bulk UPDATE | 0-2 per patient   |
| `patient_relations`   | `patient_id` (both FK columns) | HQL bulk UPDATE | 0-5 per patient   |
| `sample_human`        | `patient_id`                   | HQL bulk UPDATE | 0-500 per patient |
| `electronic_order`    | `patient_id`                   | HQL bulk UPDATE | 0-100 per patient |

**HQL Update Pattern** (example for `sample_human`):

```java
@Modifying
@Query("UPDATE SampleHuman s SET s.patientId = :primaryPatientId WHERE s.patientId = :mergedPatientId")
int updateSampleHumanPatientId(@Param("primaryPatientId") String primaryPatientId,
                                @Param("mergedPatientId") String mergedPatientId);
```

---

### 3.2 Entity Relationship Diagram

```
┌─────────────────────────┐
│   system_user           │
│  (existing)             │
└───────────┬─────────────┘
            │
            │ performed_by
            │
┌───────────▼─────────────┐         ┌─────────────────────────┐
│  patient_merge_audit    │         │   patient               │
│  (NEW)                  │◄────────┤   (UPDATED)             │
│  - id (PK)              │ primary │  - id (PK)              │
│  - primary_patient_id   │─────────┤  - merged_into_patient_id│◄─┐
│  - merged_patient_id    │◄────────┤  - is_merged            │  │
│  - merge_date           │ merged  │  - merge_date           │  │
│  - performed_by_user_id │         │  - national_id          │  │
│  - reason               │         │  - fhir_uuid            │  │
│  - data_summary (JSONB) │         │  - ... (existing)       │  │
└─────────────────────────┘         └──────┬──────────────────┘  │
                                           │                     │
                                           │ patient_id          │ self-reference
                                           │                     │
            ┌──────────────────────────────┼─────────────────────┘
            │                              │
            │                              │
┌───────────▼──────────────┐  ┌───────────▼──────────────┐
│  patient_identity        │  │  sample_human            │
│  - id (PK)               │  │  - id (PK)               │
│  - patient_id (FK)       │  │  - patient_id (FK)       │
│  - identity_type_id      │  │  - sample_id             │
│  - identity_data         │  │  - provider_id           │
└──────────────────────────┘  └──────────────────────────┘
```

---

## 4. FHIR Resource Mapping

### 4.1 FHIR Patient Resource Structure

**Primary Patient** (active, replaces merged patient):

```json
{
  "resourceType": "Patient",
  "id": "primary-patient-fhir-uuid",
  "identifier": [
    {
      "system": "http://openelis-global.org/patient-id",
      "value": "PAT-12345",
      "use": "official"
    },
    {
      "system": "http://openelis-global.org/patient-id",
      "value": "PAT-67890",
      "use": "old",
      "period": {
        "end": "2024-11-19T10:30:00Z"
      }
    },
    {
      "system": "http://country.gov/national-id",
      "value": "1234567890",
      "use": "official"
    }
  ],
  "active": true,
  "name": [
    {
      "use": "official",
      "family": "Doe",
      "given": ["John", "Michael"]
    }
  ],
  "gender": "male",
  "birthDate": "1985-03-15",
  "link": [
    {
      "other": {
        "reference": "Patient/merged-patient-fhir-uuid"
      },
      "type": "replaces"
    }
  ]
}
```

**Merged Patient** (inactive, replaced by primary):

```json
{
  "resourceType": "Patient",
  "id": "merged-patient-fhir-uuid",
  "identifier": [
    {
      "system": "http://openelis-global.org/patient-id",
      "value": "PAT-67890",
      "use": "old",
      "period": {
        "end": "2024-11-19T10:30:00Z"
      }
    }
  ],
  "active": false,
  "link": [
    {
      "other": {
        "reference": "Patient/primary-patient-fhir-uuid"
      },
      "type": "replaced-by"
    }
  ]
}
```

---

### 4.2 FHIR Related Resources Updates

**Resources to Update** (subject references):

- `ServiceRequest` (lab orders)
- `Specimen` (samples)
- `Observation` (lab results)
- `DiagnosticReport` (result reports)

**Update Pattern**:

```java
// Example: Update ServiceRequest subject references
Bundle orders = fhirClient.search()
    .forResource(ServiceRequest.class)
    .where(ServiceRequest.SUBJECT.hasId("Patient/" + mergedPatientFhirUuid))
    .returnBundle(Bundle.class)
    .execute();

for (Bundle.BundleEntryComponent entry : orders.getEntry()) {
    ServiceRequest order = (ServiceRequest) entry.getResource();
    order.setSubject(new Reference("Patient/" + primaryPatientFhirUuid));
    fhirPersistanceService.updateFhirResource(order);
}
```

---

## 5. Data Transfer Objects (DTOs)

### 5.1 `PatientMergeRequestDTO` (Request DTO)

**Location**:
`src/main/java/org/openelisglobal/patient/merge/dto/PatientMergeRequestDTO.java`

**Purpose**: REST API request DTO for merge execution

```java
package org.openelisglobal.patient.merge.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientMergeRequestDTO {

    @NotBlank(message = "Patient 1 ID is required")
    private String patient1Id;

    @NotBlank(message = "Patient 2 ID is required")
    private String patient2Id;

    @NotBlank(message = "Primary patient ID is required")
    private String primaryPatientId;

    @NotBlank(message = "Reason for merge is required")
    private String reason;

    @NotNull(message = "Confirmation is required")
    private Boolean confirmed;
}
```

---

### 5.2 `PatientMergeValidationResultDTO` (Validation Response DTO)

**Location**:
`src/main/java/org/openelisglobal/patient/merge/dto/PatientMergeValidationResultDTO.java`

**Purpose**: REST API response for validation endpoint

```java
package org.openelisglobal.patient.merge.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PatientMergeValidationResultDTO {

    private boolean valid;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private PatientMergeDataSummaryDTO dataSummary;

    public static PatientMergeValidationResultDTO success(PatientMergeDataSummaryDTO dataSummary) {
        PatientMergeValidationResultDTO result = new PatientMergeValidationResultDTO();
        result.setValid(true);
        result.setDataSummary(dataSummary);
        return result;
    }

    public static PatientMergeValidationResultDTO failed(List<String> errors) {
        PatientMergeValidationResultDTO result = new PatientMergeValidationResultDTO();
        result.setValid(false);
        result.setErrors(errors);
        return result;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
```

---

### 5.3 `PatientMergeDataSummaryDTO` (Data Summary DTO)

**Location**:
`src/main/java/org/openelisglobal/patient/merge/dto/PatientMergeDataSummaryDTO.java`

**Purpose**: Consolidated data counts and conflict detection

```java
package org.openelisglobal.patient.merge.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class PatientMergeDataSummaryDTO {

    // Order counts
    private int totalOrders;
    private int activeOrders;

    // Result counts
    private int totalResults;
    private int pendingResults;
    private int completedResults;

    // Sample counts
    private int totalSamples;

    // Identity counts
    private int totalIdentities;
    private int preservedIdentities;
    private int discardedIdentities;

    // Contact counts
    private int totalContacts;

    // Document counts
    private int totalDocuments;

    // Relation counts
    private int totalRelations;

    // Conflict detection
    private List<String> conflictingFields = new ArrayList<>();
    private List<String> conflictingIdentityTypes = new ArrayList<>();

    public boolean hasConflicts() {
        return !conflictingFields.isEmpty() || !conflictingIdentityTypes.isEmpty();
    }
}
```

---

### 5.4 `PatientMergeDetailsDTO` (Merge Details Response DTO)

**Location**:
`src/main/java/org/openelisglobal/patient/merge/dto/PatientMergeDetailsDTO.java`

**Purpose**: REST API response for merge details endpoint

```java
package org.openelisglobal.patient.merge.dto;

import java.util.List;
import lombok.Data;

@Data
public class PatientMergeDetailsDTO {

    private String patientId;
    private String nationalId;
    private String externalId;
    private String name;
    private String gender;
    private String birthDate;

    // Data summary
    private int totalOrders;
    private int activeOrders;
    private int totalResults;
    private int totalSamples;
    private int totalDocuments;

    // Identifiers
    private List<IdentifierDTO> identifiers;

    // Potential conflicts (when comparing two patients)
    private List<String> conflictingFields;

    @Data
    public static class IdentifierDTO {
        private String type;
        private String value;
    }
}
```

---

### 5.5 `PatientMergeExecutionResultDTO` (Execution Response DTO)

**Location**:
`src/main/java/org/openelisglobal/patient/merge/dto/PatientMergeExecutionResultDTO.java`

**Purpose**: REST API response for merge execution endpoint

```java
package org.openelisglobal.patient.merge.dto;

import lombok.Data;

@Data
public class PatientMergeExecutionResultDTO {

    private boolean success;
    private String mergeAuditId;
    private String message;
    private String primaryPatientId;
    private String mergedPatientId;
    private long mergeDurationMs;
}
```

---

## 6. Data Flow Diagrams

### 6.1 Merge Execution Data Flow

```
┌────────────────────────────────────────────────────────────────┐
│ FRONTEND                                                       │
│  - User selects two patients                                   │
│  - User selects primary patient                                │
│  - User provides reason                                        │
│  - User confirms merge                                         │
└──────────────────┬─────────────────────────────────────────────┘
                   │
                   │ POST /api/patient/merge/execute
                   │ {patient1Id, patient2Id, primaryPatientId, reason, confirmed}
                   │
┌──────────────────▼─────────────────────────────────────────────┐
│ CONTROLLER LAYER: PatientMergeRestController                   │
│  - @PreAuthorize("hasRole('ROLE_GLOBAL_ADMIN')")              │
│  - Validate PatientMergeRequestDTO (@Valid)                   │
│  - Pass DTO to service layer                                  │
└──────────────────┬─────────────────────────────────────────────┘
                   │
                   │ executeMerge(PatientMergeRequestDTO)
                   │
┌──────────────────▼─────────────────────────────────────────────┐
│ SERVICE LAYER: PatientMergeServiceImpl                         │
│  @Transactional (READ_COMMITTED)                              │
│                                                                │
│  1. Permission Check                                           │
│     - Verify Global Administrator permission                   │
│                                                                │
│  2. Lock Patients (Concurrency Control)                        │
│     - patientDAO.findByIdForUpdate(primaryId)                 │
│     - patientDAO.findByIdForUpdate(mergedId)                  │
│                                                                │
│  3. Validation                                                 │
│     - Same patient check                                       │
│     - Already merged check                                     │
│     - Circular reference check                                 │
│                                                                │
│  4. Consolidate Related Data                                   │
│     a) patient_identity → consolidateIdentifiers()            │
│        - Check for duplicate types (BLOCKER issue)            │
│        - patientIdentityDAO.updatePatientId(...)              │
│     b) patient_contact                                         │
│        - patientContactDAO.updatePatientId(...)               │
│     c) external_patient_id                                     │
│        - externalPatientIdDAO.updatePatientId(...)            │
│     d) patient_relations                                       │
│        - patientRelationsDAO.updatePatientId(...)             │
│     e) sample_human (500+ rows possible)                      │
│        - sampleHumanDAO.updatePatientId(...)                  │
│     f) electronic_order                                        │
│        - electronicOrderDAO.updatePatientId(...)              │
│                                                                │
│  5. Mark Merged Patient                                        │
│     - merged.setIsMerged(true)                                │
│     - merged.setMergedIntoPatientId(primary.getId())          │
│     - merged.setMergeDate(new Timestamp())                    │
│     - patientDAO.save(merged)                                 │
│                                                                │
│  6. Update FHIR Resources                                      │
│     - fhirPatientLinkService.createMergeLinks(...)            │
│       → Primary: link.type = "replaces"                       │
│       → Merged: active=false, link.type = "replaced-by"       │
│     - fhirResourceUpdateService.updateReferences(...)          │
│       → ServiceRequest, Specimen, Observation, DiagnosticReport│
│                                                                │
│  7. Create Audit Trail                                         │
│     - Create PatientMergeAudit entity                         │
│     - Set dataSummary (JSONB)                                 │
│     - patientMergeAuditDAO.save(audit)                        │
│                                                                │
│  8. Return Result                                              │
│     - PatientMergeAudit with audit ID                         │
└──────────────────┬─────────────────────────────────────────────┘
                   │
                   │ If exception: Transaction rollback (automatic)
                   │ If success: Transaction commit
                   │
┌──────────────────▼─────────────────────────────────────────────┐
│ DATABASE: PostgreSQL                                           │
│  - patient_merge_audit (NEW ROW)                              │
│  - patient (merged patient: is_merged=true)                   │
│  - patient_identity (patient_id updated)                      │
│  - patient_contact (patient_id updated)                       │
│  - sample_human (patient_id updated)                          │
│  - electronic_order (patient_id updated)                      │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│ FHIR STORE (HAPI FHIR R4)                                      │
│  - Patient (primary): link = [{"type":"replaces", ...}]       │
│  - Patient (merged): active=false, link = [{"type":...}]      │
│  - ServiceRequest: subject = Patient/primary                   │
│  - Specimen: subject = Patient/primary                         │
│  - Observation: subject = Patient/primary                      │
│  - DiagnosticReport: subject = Patient/primary                 │
└────────────────────────────────────────────────────────────────┘
```

---

## 7. Identifier Consolidation Logic

### 7.1 Blocker: Duplicate Identifier Types

**Critical Issue**: OpenELIS does NOT support multiple identifiers of the same
type for a single patient.

**See**: [`BLOCKERS.md`](./BLOCKERS.md) for full details and PM clarification
questions.

**Temporary Design** (assuming Option A: Primary Patient Wins):

```java
@Transactional
public void consolidateIdentifiers(Patient primary, Patient merged,
                                   PatientMergeDataSummary summary) {

    // Fetch all identities for both patients
    List<PatientIdentity> primaryIdentities =
        patientIdentityDAO.getPatientIdentitiesByPatientId(primary.getId());
    List<PatientIdentity> mergedIdentities =
        patientIdentityDAO.getPatientIdentitiesByPatientId(merged.getId());

    // Build map of primary patient's identity types
    Set<String> primaryIdentityTypes = primaryIdentities.stream()
        .map(PatientIdentity::getIdentityTypeId)
        .collect(Collectors.toSet());

    int preserved = 0;
    int discarded = 0;
    List<String> conflicts = new ArrayList<>();

    for (PatientIdentity mergedIdentity : mergedIdentities) {
        String identityType = mergedIdentity.getIdentityTypeId();

        if (primaryIdentityTypes.contains(identityType)) {
            // CONFLICT: Primary patient already has this identity type
            log.warn("Identifier conflict during merge: " +
                "Primary patient {} keeps {} = {}, " +
                "Discarding merged patient {} {} = {}",
                primary.getId(),
                getIdentityTypeName(identityType),
                getPrimaryIdentityValue(primary, identityType),
                merged.getId(),
                getIdentityTypeName(identityType),
                mergedIdentity.getIdentityData());

            conflicts.add(getIdentityTypeName(identityType));
            discarded++;

            // Record discarded identifier in audit
            recordDiscardedIdentifier(primary, merged, mergedIdentity);

        } else {
            // No conflict: Update patient_id to primary
            int updated = patientIdentityDAO.updatePatientId(
                primary.getId(),
                merged.getId(),
                identityType);
            preserved += updated;
        }
    }

    // Update summary
    summary.setTotalIdentities(primaryIdentities.size() + mergedIdentities.size());
    summary.setPreservedIdentities(primaryIdentities.size() + preserved);
    summary.setDiscardedIdentities(discarded);
    summary.setConflictingIdentityTypes(conflicts);
}
```

**Status**: ⏸️ **Implementation blocked until PM clarifies approach** (see
BLOCKERS.md).

---

## 8. Transaction Isolation and Rollback

### 8.1 Transaction Configuration

**Isolation Level**: `READ_COMMITTED` (PostgreSQL default)

**Propagation**: `REQUIRED` (Spring default)

**Rollback Policy**: Automatic rollback on any `RuntimeException`

**Implementation**:

```java
@Service
public class PatientMergeServiceImpl implements PatientMergeService {

    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    @Override
    public PatientMergeAudit executeMerge(PatientMergeRequest request) {
        // All database operations within single transaction
        // If any operation throws exception, entire transaction rolls back
    }
}
```

---

### 8.2 Rollback Scenarios

| Failure Point                 | Rollback Behavior                                    | Data State After Rollback    |
| ----------------------------- | ---------------------------------------------------- | ---------------------------- |
| Permission check fails        | Immediate exception, no DB changes                   | Unchanged                    |
| Lock acquisition timeout      | Exception after 30s, no changes committed            | Unchanged                    |
| Validation fails              | Exception before updates, no changes                 | Unchanged                    |
| patient_identity update fails | All changes rolled back                              | Unchanged                    |
| patient_contact update fails  | All changes rolled back                              | Unchanged                    |
| sample_human update fails     | All changes rolled back                              | Unchanged                    |
| FHIR update fails             | **Critical**: DB changes committed, FHIR out of sync | Manual intervention required |
| Audit entry creation fails    | All changes rolled back                              | Unchanged                    |

**FHIR Synchronization Failure Handling**:

```java
try {
    // Database operations (within transaction)
    consolidateData();
    markMergedPatient();

    // FHIR operations (may fail independently)
    fhirPatientLinkService.createMergeLinks(primary, merged);
    fhirResourceUpdateService.updateReferences(primary, merged);

    createAuditEntry();

} catch (FhirUpdateException e) {
    // Critical: Database succeeded, FHIR failed
    log.error("CRITICAL: Patient merge DB succeeded but FHIR failed. " +
              "Manual FHIR reconciliation required. " +
              "Primary: {}, Merged: {}",
              primary.getId(), merged.getId(), e);

    // Log to critical alerts table
    criticalAlertService.logFhirSyncFailure(primary, merged, e);

    // Notify administrator
    notificationService.notifyAdmin("FHIR sync failure after patient merge",
                                    primary, merged, e);

    // Do NOT rollback database transaction - data is already consistent
    // FHIR will be manually reconciled by admin
}
```

---

## 9. Performance Considerations

### 9.1 Expected Data Volumes

| Data Type  | Typical Patient | High-Volume Patient | Max Expected |
| ---------- | --------------- | ------------------- | ------------ |
| Identities | 2-3             | 5                   | 10           |
| Contacts   | 1-2             | 3                   | 5            |
| Samples    | 5-10            | 50-100              | 500          |
| Orders     | 5-10            | 50-100              | 500          |
| Results    | 10-50           | 100-500             | 1000         |

### 9.2 Performance Targets

- **Target**: Merge operation completes within 30 seconds for patient with 500
  results
- **Actual**: Using HQL bulk UPDATEs, expected completion in 2-5 seconds
- **Timeout**: **No hard timeout** (operations continue until completion per
  clarification)

### 9.3 Optimization Strategies

1. **Batch UPDATEs**: Single UPDATE per table using HQL (10 UPDATEs total)
2. **Row-level locking**: Minimal lock scope (only 2 patient rows)
3. **Index usage**: All foreign key columns are indexed
4. **JSONB for summary**: Efficient storage and querying
5. **Lazy loading prevention**: Compile all data within transaction

---

## 10. Security Model

### 10.1 Permission Requirements

**Required Role**: `ROLE_GLOBAL_ADMIN` (Global Administrator)

**Enforcement Layers**:

1. **Controller Layer**: `@PreAuthorize("hasRole('ROLE_GLOBAL_ADMIN')")`
2. **Service Layer**: Manual permission check in service method

### 10.2 Audit Trail Fields

All audit entries include:

- `performed_by_user_id` - Who executed the merge
- `merge_date` - When merge occurred
- `reason` - Why merge was performed
- `sys_user_id` - System user for audit compliance
- `lastupdated` - Automatic timestamp

---

## 11. Constitution Compliance Checklist

| Principle                 | Requirement                                                      | Compliance     |
| ------------------------- | ---------------------------------------------------------------- | -------------- |
| I. Architecture           | 5-layer pattern (Valueholder → DAO → Service → Controller → DTO) | ✅ Implemented |
| II. Technology Stack      | Java 21, Spring Boot 3.x, Hibernate 6.x, PostgreSQL              | ✅ Compatible  |
| III. External Integration | FHIR R4 with link relationships                                  | ✅ Implemented |
| IV. Data Persistence      | JPA/Hibernate annotations (no XML)                               | ✅ Compliant   |
| V. Database Schema        | Liquibase changesets with rollback                               | ✅ Implemented |
| VI. UI Framework          | N/A (backend only)                                               | ✅ N/A         |
| VII. Testing              | TDD, >70% coverage                                               | ✅ Planned     |
| VIII. Security & Audit    | RBAC, audit trail                                                | ✅ Implemented |

---

## 12. References

- **FHIR R4 Patient**: https://hl7.org/fhir/R4/patient.html
- **FHIR R4 Patient.link**:
  https://hl7.org/fhir/R4/patient-definitions.html#Patient.link
- **PostgreSQL JSONB**:
  https://www.postgresql.org/docs/current/datatype-json.html
- **Hibernate @Type annotations**:
  https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#basic-mapping
- **OpenELIS Constitution**:
  [../../.specify/memory/constitution.md](../../.specify/memory/constitution.md)
- **Research Document**: [research.md](./research.md)
- **Implementation Plan**: [plan.md](./plan.md)
- **Blockers**: [BLOCKERS.md](./BLOCKERS.md)

---

**Data Model Status**: ✅ **Design Complete** (implementation blocked by
identifier duplication issue - see BLOCKERS.md)

**Next Phase**: API Contracts (contracts/), Quickstart Guide (quickstart.md)
