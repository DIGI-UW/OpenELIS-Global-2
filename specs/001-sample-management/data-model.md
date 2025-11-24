# Data Model Design: Sample Management Menu

**Feature**: Sample Management Menu **Branch**: `001-sample-management`
**Date**: 2025-11-20 **Phase**: 1 - Design & Data Model **Prerequisites**:
[research.md](research.md) complete

---

## Overview

This document specifies the data model changes required to support the Sample
Management Menu feature, including aliquoting with parent-child relationships,
quantity tracking, and test ordering.

**Design Principles**:

- Extend existing entities rather than create duplicates
- Use JPA annotations for new fields (Constitution v1.3.0)
- Maintain backward compatibility with legacy XML mappings
- Support unlimited nested aliquoting hierarchy
- Ensure FHIR R4 compliance for external integration

---

## Entity Modifications

### 1. SampleItem Entity (MODIFIED)

**File**:
`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`

**Status**: ✏️ EXISTING ENTITY - Add new fields with annotations

**Changes Required**:

#### New Fields

```java
// Quantity tracking for aliquoting
@Column(name = "original_quantity", precision = 10, scale = 3)
private Double originalQuantity;

@Column(name = "remaining_quantity", precision = 10, scale = 3)
private Double remainingQuantity;

// Parent-child relationship for aliquots
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_sample_item_id")
private SampleItem parentSampleItem;

@OneToMany(mappedBy = "parentSampleItem", cascade = CascadeType.ALL, orphanRemoval = false)
private List<SampleItem> childAliquots = new ArrayList<>();
```

#### Field Specifications

| Field               | Type               | Nullable | Default    | Constraints                                         | Purpose                                                |
| ------------------- | ------------------ | -------- | ---------- | --------------------------------------------------- | ------------------------------------------------------ |
| `originalQuantity`  | `Double`           | YES      | `NULL`     | `precision=10, scale=3`                             | Initial volume/mass when sample created                |
| `remainingQuantity` | `Double`           | YES      | `NULL`     | `precision=10, scale=3`, must be ≤ originalQuantity | Current available volume after aliquoting              |
| `parentSampleItem`  | `SampleItem`       | YES      | `NULL`     | FK to `sample_item.id`                              | Reference to parent sample (NULL for original samples) |
| `childAliquots`     | `List<SampleItem>` | N/A      | Empty list | Mapped by `parentSampleItem`                        | Collection of aliquots created from this sample        |

#### Validation Methods

```java
public void setRemainingQuantity(Double remainingQuantity) {
    if (remainingQuantity != null && originalQuantity != null &&
        remainingQuantity > originalQuantity) {
        throw new IllegalArgumentException(
            "Remaining quantity (" + remainingQuantity +
            ") cannot exceed original quantity (" + originalQuantity + ")");
    }
    this.remainingQuantity = remainingQuantity;
}

public void setOriginalQuantity(Double originalQuantity) {
    if (originalQuantity != null && originalQuantity < 0) {
        throw new IllegalArgumentException(
            "Original quantity cannot be negative: " + originalQuantity);
    }
    this.originalQuantity = originalQuantity;
}

public boolean hasRemainingQuantity() {
    return remainingQuantity != null && remainingQuantity > 0;
}

public boolean isAliquot() {
    return parentSampleItem != null;
}
```

#### Existing Fields (Preserved)

- `String id` - Primary key (UUID string)
- `UUID fhirUuid` - FHIR Specimen resource ID
- `String externalId` - Human-readable identifier (will include .1, .2, .3
  suffix for aliquots)
- `Double quantity` - Legacy single quantity field (deprecated in favor of
  originalQuantity/remainingQuantity)
- `ValueHolderInterface sample` - Parent Sample entity reference
- `ValueHolderInterface typeOfSample` - Sample type (blood, urine, etc.)
- `Timestamp collectionDate` - Collection timestamp
- `Timestamp lastupdated` - Optimistic locking version field (`@Version` from
  BaseObject)

#### Migration Strategy

**Backward Compatibility**:

- Existing `quantity` field remains in XML mapping (legacy samples)
- New samples use `originalQuantity` and `remainingQuantity`
- Data migration script (optional): Copy `quantity` → `originalQuantity` and
  `remainingQuantity` for existing samples

**Hybrid Mapping Approach**:

- Legacy fields: XML mapping in `SampleItem.hbm.xml` (unchanged)
- New fields: JPA annotations in `SampleItem.java` (Constitution-compliant)
- Hibernate 6.x supports both mapping approaches simultaneously

---

### 2. SampleItemAliquotRelationship Entity (NEW)

**File**:
`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItemAliquotRelationship.java`

**Status**: ✨ NEW ENTITY - Full annotation-based mapping

**Purpose**: Tracks metadata for each aliquoting operation (sequence number,
quantity transferred)

#### Entity Definition

```java
package org.openelisglobal.sampleitem.valueholder;

import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import java.util.UUID;

@Entity
@Table(name = "sample_item_aliquot_relationship",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"parent_sample_item_id", "sequence_number"}
       ))
public class SampleItemAliquotRelationship extends BaseObject<String> {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_sample_item_id", nullable = false)
    private SampleItem parentSampleItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_sample_item_id", nullable = false)
    private SampleItem childSampleItem;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "quantity_transferred", precision = 10, scale = 3, nullable = false)
    private Double quantityTransferred;

    @Column(name = "fhir_uuid", columnDefinition = "uuid")
    private UUID fhirUuid;

    @Column(name = "notes", length = 1000)
    private String notes;

    // Constructor
    public SampleItemAliquotRelationship() {
        this.id = UUID.randomUUID().toString();
        this.fhirUuid = UUID.randomUUID();
    }

    // Getters and setters...
}
```

#### Field Specifications

| Field                 | Type         | Nullable | Constraints             | Purpose                                      |
| --------------------- | ------------ | -------- | ----------------------- | -------------------------------------------- |
| `id`                  | `String`     | NO       | PK, UUID                | Primary key                                  |
| `parentSampleItem`    | `SampleItem` | NO       | FK to `sample_item.id`  | Parent sample from which aliquot was created |
| `childSampleItem`     | `SampleItem` | NO       | FK to `sample_item.id`  | Aliquot sample item created                  |
| `sequenceNumber`      | `Integer`    | NO       | Unique with parent      | Sequential number (.1, .2, .3, etc.)         |
| `quantityTransferred` | `Double`     | NO       | `precision=10, scale=3` | Volume/mass transferred to aliquot           |
| `fhirUuid`            | `UUID`       | YES      | UUID                    | FHIR resource identifier                     |
| `notes`               | `String`     | YES      | Max 1000 chars          | Optional notes about aliquoting operation    |

#### Unique Constraints

```sql
UNIQUE (parent_sample_item_id, sequence_number)
```

**Rationale**: Ensures each parent has unique sequence numbers (1, 2, 3...)
without gaps when aliquots are deleted.

#### Indexes

```sql
CREATE INDEX idx_aliquot_parent ON sample_item_aliquot_relationship(parent_sample_item_id);
CREATE INDEX idx_aliquot_child ON sample_item_aliquot_relationship(child_sample_item_id);
```

**Rationale**: Optimize queries for parent → children (loading aliquot list) and
child → parent (lineage lookup).

---

### 3. Analysis Entity (NO CHANGE)

**File**: `src/main/java/org/openelisglobal/analysis/valueholder/Analysis.java`

**Status**: ✅ USE AS-IS - Serves as "TestOrder" relationship

**Relevant Fields**:

- `@ManyToOne SampleItem sampleItem` - Links test to sample
- `@ManyToOne Test test` - Test definition
- `String statusId` - Test lifecycle status (ORDERED, IN_PROGRESS, COMPLETED)
- `UUID fhirUuid` - Maps to FHIR ServiceRequest

**Rationale**: Existing entity already provides test-to-sample relationship. No
modifications needed.

---

## Database Schema Changes

### Liquibase Changeset

**File**: `src/main/resources/liquibase/2.8.x/sample-management-001.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <!-- Add quantity tracking and parent reference columns to sample_item -->
    <changeSet id="sample-mgmt-001-add-aliquot-columns" author="dev-team">
        <addColumn tableName="sample_item">
            <column name="original_quantity" type="DECIMAL(10,3)">
                <constraints nullable="true"/>
            </column>
            <column name="remaining_quantity" type="DECIMAL(10,3)">
                <constraints nullable="true"/>
            </column>
            <column name="parent_sample_item_id" type="VARCHAR(36)">
                <constraints nullable="true"/>
            </column>
        </addColumn>
    </changeSet>

    <!-- Add foreign key constraint -->
    <changeSet id="sample-mgmt-002-add-parent-fk" author="dev-team">
        <addForeignKeyConstraint
            constraintName="fk_sample_item_parent"
            baseTableName="sample_item"
            baseColumnNames="parent_sample_item_id"
            referencedTableName="sample_item"
            referencedColumnNames="id"
            onDelete="RESTRICT"
            onUpdate="CASCADE"/>
    </changeSet>

    <!-- Add index for parent-child queries -->
    <changeSet id="sample-mgmt-003-add-parent-index" author="dev-team">
        <createIndex tableName="sample_item" indexName="idx_sample_item_parent">
            <column name="parent_sample_item_id"/>
        </createIndex>
    </changeSet>

    <!-- Create aliquot relationship tracking table -->
    <changeSet id="sample-mgmt-004-create-aliquot-relationship" author="dev-team">
        <createTable tableName="sample_item_aliquot_relationship">
            <column name="id" type="VARCHAR(36)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="parent_sample_item_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="child_sample_item_id" type="VARCHAR(36)">
                <constraints nullable="false"/>
            </column>
            <column name="sequence_number" type="INTEGER">
                <constraints nullable="false"/>
            </column>
            <column name="quantity_transferred" type="DECIMAL(10,3)">
                <constraints nullable="false"/>
            </column>
            <column name="fhir_uuid" type="uuid">
                <constraints nullable="true" unique="true"/>
            </column>
            <column name="notes" type="VARCHAR(1000)">
                <constraints nullable="true"/>
            </column>
            <column name="lastupdated" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>

    <!-- Add foreign keys for relationship table -->
    <changeSet id="sample-mgmt-005-add-relationship-fks" author="dev-team">
        <addForeignKeyConstraint
            constraintName="fk_aliquot_rel_parent"
            baseTableName="sample_item_aliquot_relationship"
            baseColumnNames="parent_sample_item_id"
            referencedTableName="sample_item"
            referencedColumnNames="id"
            onDelete="CASCADE"
            onUpdate="CASCADE"/>

        <addForeignKeyConstraint
            constraintName="fk_aliquot_rel_child"
            baseTableName="sample_item_aliquot_relationship"
            baseColumnNames="child_sample_item_id"
            referencedTableName="sample_item"
            referencedColumnNames="id"
            onDelete="CASCADE"
            onUpdate="CASCADE"/>
    </changeSet>

    <!-- Add unique constraint on parent + sequence number -->
    <changeSet id="sample-mgmt-006-add-sequence-unique" author="dev-team">
        <addUniqueConstraint
            tableName="sample_item_aliquot_relationship"
            columnNames="parent_sample_item_id, sequence_number"
            constraintName="uk_aliquot_parent_sequence"/>
    </changeSet>

    <!-- Add indexes for relationship table -->
    <changeSet id="sample-mgmt-007-add-relationship-indexes" author="dev-team">
        <createIndex tableName="sample_item_aliquot_relationship" indexName="idx_aliquot_rel_parent">
            <column name="parent_sample_item_id"/>
        </createIndex>
        <createIndex tableName="sample_item_aliquot_relationship" indexName="idx_aliquot_rel_child">
            <column name="child_sample_item_id"/>
        </createIndex>
    </changeSet>

    <!-- Optional: Migrate existing quantity data -->
    <changeSet id="sample-mgmt-008-migrate-legacy-quantity" author="dev-team">
        <sql>
            UPDATE sample_item
            SET original_quantity = quantity,
                remaining_quantity = quantity
            WHERE quantity IS NOT NULL
              AND original_quantity IS NULL;
        </sql>
    </changeSet>

</databaseChangeLog>
```

### Schema Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│ sample_item (MODIFIED)                                          │
├─────────────────────────────────────────────────────────────────┤
│ id (PK, VARCHAR(36))                                            │
│ external_id (VARCHAR, e.g., "SAMPLE001.1.2")                   │
│ fhir_uuid (UUID)                                                │
│ quantity (DOUBLE, legacy - deprecated)                          │
│ original_quantity (DECIMAL(10,3), NEW)                          │
│ remaining_quantity (DECIMAL(10,3), NEW)                         │
│ parent_sample_item_id (FK to sample_item.id, NEW)              │
│ sample_id (FK to sample.id)                                     │
│ type_of_sample_id (FK)                                          │
│ collection_date (TIMESTAMP)                                     │
│ lastupdated (TIMESTAMP, @Version)                               │
└─────────────────────────────────────────────────────────────────┘
                │                    ▲
                │                    │
                │ 1                  │ *
                │                    │
                │         ┌──────────┴──────────────────────────────┐
                │         │ sample_item_aliquot_relationship (NEW)  │
                │         ├─────────────────────────────────────────┤
                │         │ id (PK, VARCHAR(36))                    │
                └────────>│ parent_sample_item_id (FK, NOT NULL)    │
                          │ child_sample_item_id (FK, NOT NULL)     │
                          │ sequence_number (INTEGER, NOT NULL)     │
                          │ quantity_transferred (DECIMAL(10,3))    │
                          │ fhir_uuid (UUID)                        │
                          │ notes (VARCHAR(1000))                   │
                          │ lastupdated (TIMESTAMP)                 │
                          │ UNIQUE (parent_id, sequence_number)     │
                          └─────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ analysis (NO CHANGE - existing TestOrder relationship)          │
├─────────────────────────────────────────────────────────────────┤
│ id (PK)                                                          │
│ sample_item_id (FK to sample_item.id)                           │
│ test_id (FK to test.id)                                         │
│ status_id (VARCHAR)                                              │
│ fhir_uuid (UUID - maps to FHIR ServiceRequest)                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Transfer Objects (DTOs)

### 1. SampleItemDTO

**File**: `src/main/java/org/openelisglobal/sampleitem/dto/SampleItemDTO.java`

```java
package org.openelisglobal.sampleitem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleItemDTO {
    private String id;
    private String externalId;
    private String sampleAccessionNumber;
    private String sampleType;
    private Double originalQuantity;
    private Double remainingQuantity;
    private String unitOfMeasure;
    private String status;
    private String collectionDate;

    // Parent-child relationship
    private String parentId;
    private String parentExternalId;
    private List<AliquotSummaryDTO> childAliquots;

    // Test associations
    private List<TestSummaryDTO> orderedTests;

    // Computed fields
    private Boolean hasRemainingQuantity;
    private Boolean isAliquot;
    private Integer nestingLevel; // 0 = original, 1 = first-level aliquot, etc.
}
```

### 2. AliquotSummaryDTO

```java
@Data
public class AliquotSummaryDTO {
    private String id;
    private String externalId;
    private Double originalQuantity;
    private Double remainingQuantity;
    private String createdDate;
}
```

### 3. TestSummaryDTO

```java
@Data
public class TestSummaryDTO {
    private String analysisId;
    private String testId;
    private String testName;
    private String status; // ORDERED, IN_PROGRESS, COMPLETED
    private String orderedDate;
}
```

### 4. CreateAliquotRequest

**File**:
`src/main/java/org/openelisglobal/sampleitem/form/CreateAliquotForm.java`

```java
package org.openelisglobal.sampleitem.form;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateAliquotForm {
    @NotBlank(message = "Parent sample item ID is required")
    private String parentSampleItemId;

    @NotNull(message = "Quantity to transfer is required")
    @DecimalMin(value = "0.001", message = "Quantity must be at least 0.001")
    private Double quantityToTransfer;

    private String unitOfMeasure; // Optional, defaults to parent's unit

    private String notes; // Optional notes
}
```

### 5. AddTestsRequest

**File**: `src/main/java/org/openelisglobal/sampleitem/form/AddTestsForm.java`

```java
package org.openelisglobal.sampleitem.form;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class AddTestsForm {
    @NotEmpty(message = "At least one sample item ID is required")
    private List<String> sampleItemIds;

    @NotEmpty(message = "At least one test ID is required")
    private List<String> testIds;
}
```

### 6. AddTestsResponse

```java
@Data
public class AddTestsResponse {
    private Integer successCount;
    private List<TestAdditionResult> results;

    @Data
    public static class TestAdditionResult {
        private String sampleItemId;
        private String sampleItemExternalId;
        private String testId;
        private String testName;
        private Boolean success;
        private String message; // e.g., "Test already ordered" or "Success"
    }
}
```

### 7. SearchSamplesResponse

```java
@Data
public class SearchSamplesResponse {
    private String accessionNumber;
    private List<SampleItemDTO> sampleItems;
    private Integer totalCount;
}
```

---

## Business Rules & Validation

### 1. Aliquot Creation Rules

| Rule ID    | Description                                                         | Validation Location                                     |
| ---------- | ------------------------------------------------------------------- | ------------------------------------------------------- |
| **BR-001** | Quantity to transfer must be > 0                                    | Form validation (`@DecimalMin`)                         |
| **BR-002** | Quantity to transfer must be ≤ parent's remaining quantity          | Service layer (`SampleManagementService.createAliquot`) |
| **BR-003** | Parent must have remaining quantity > 0                             | Service layer (check before aliquot creation)           |
| **BR-004** | External ID generated as `parent.externalId + "." + sequenceNumber` | Service layer (automatic)                               |
| **BR-005** | Aliquot inherits sample type, collection date from parent           | Service layer (copy from parent)                        |
| **BR-006** | Aliquot gets new UUID and FHIR UUID                                 | Entity constructor (`@PrePersist`)                      |
| **BR-007** | Parent's remaining quantity reduced by transfer amount              | Service layer (atomic update)                           |

### 2. Test Ordering Rules

| Rule ID    | Description                                                | Validation Location                                          |
| ---------- | ---------------------------------------------------------- | ------------------------------------------------------------ |
| **BR-008** | Cannot add same test twice to same sample item             | Service layer (duplicate detection)                          |
| **BR-009** | Test must be compatible with sample type                   | Service layer (type compatibility check)                     |
| **BR-010** | Bulk test addition skips duplicates, continues with others | Service layer (collect results, don't fail entire operation) |

### 3. Search Rules

| Rule ID    | Description                                                                        | Validation Location                              |
| ---------- | ---------------------------------------------------------------------------------- | ------------------------------------------------ |
| **BR-011** | Search by base accession number returns all related samples (parent + aliquots)    | DAO layer (HQL query)                            |
| **BR-012** | Search by full external ID (e.g., "SAMPLE001.2") returns exact match + descendants | DAO layer (pattern matching)                     |
| **BR-013** | Search results include parent/child relationships fully loaded                     | Service layer (`@Transactional` with JOIN FETCH) |

---

## FHIR R4 Mapping

### SampleItem → FHIR Specimen

**Extension to**: `FhirTransformServiceImpl.transformToSpecimen(SampleItem)`

```java
private Specimen transformToSpecimen(SampleItem sampleItem) {
    Specimen specimen = new Specimen();

    // Identifiers
    specimen.setId(sampleItem.getFhirUuidAsString());
    specimen.addIdentifier(createIdentifier(
        fhirConfig.getOeFhirSystem() + "/sampleItem_uuid",
        sampleItem.getFhirUuidAsString()));
    specimen.setAccessionIdentifier(createIdentifier(
        fhirConfig.getOeFhirSystem() + "/sampleItem_externalId",
        sampleItem.getExternalId()));

    // Type and status
    specimen.setType(transformTypeOfSampleToCodeableConcept(sampleItem.getTypeOfSample()));
    specimen.setStatus(SpecimenStatus.AVAILABLE);

    // Collection info
    specimen.setCollection(transformToCollection(
        sampleItem.getCollectionDate(),
        sampleItem.getCollector()));

    // Quantity (NEW - use originalQuantity)
    if (sampleItem.getOriginalQuantity() != null) {
        SpecimenContainerComponent container = new SpecimenContainerComponent();
        Quantity specimenQuantity = new Quantity();
        specimenQuantity.setValue(sampleItem.getOriginalQuantity());
        specimenQuantity.setUnit(sampleItem.getUnitOfMeasure().getName());
        specimenQuantity.setSystem("http://unitsofmeasure.org");
        container.setSpecimenQuantity(specimenQuantity);
        specimen.addContainer(container);
    }

    // Parent reference (NEW - for aliquots)
    if (sampleItem.getParentSampleItem() != null) {
        Reference parentRef = createReferenceFor(
            ResourceType.Specimen,
            sampleItem.getParentSampleItem().getFhirUuidAsString());
        specimen.addParent(parentRef);
    }

    // Custom extension for remaining quantity (NEW)
    if (sampleItem.getRemainingQuantity() != null) {
        Extension remainingQtyExtension = new Extension();
        remainingQtyExtension.setUrl(fhirConfig.getOeFhirSystem() + "/remainingQuantity");
        Quantity remainingQty = new Quantity();
        remainingQty.setValue(sampleItem.getRemainingQuantity());
        remainingQty.setUnit(sampleItem.getUnitOfMeasure().getName());
        remainingQty.setSystem("http://unitsofmeasure.org");
        remainingQtyExtension.setValue(remainingQty);
        specimen.addExtension(remainingQtyExtension);
    }

    // Link to ServiceRequests (tests)
    for (Analysis analysis : analysisService.getAnalysesBySampleItem(sampleItem)) {
        specimen.addRequest(createReferenceFor(
            ResourceType.ServiceRequest,
            analysis.getFhirUuidAsString()));
    }

    // Link to Patient
    specimen.setSubject(createReferenceFor(
        ResourceType.Patient,
        sampleItem.getSample().getPatient().getFhirUuidAsString()));

    return specimen;
}
```

### Key FHIR Elements

| OpenELIS Field                 | FHIR Element                            | Notes                                  |
| ------------------------------ | --------------------------------------- | -------------------------------------- |
| `sampleItem.id`                | `Specimen.id`                           | Internal database ID                   |
| `sampleItem.fhirUuid`          | `Specimen.identifier[0]`                | FHIR resource identifier               |
| `sampleItem.externalId`        | `Specimen.accessionIdentifier`          | Human-readable lab number              |
| `sampleItem.originalQuantity`  | `Specimen.container.specimenQuantity`   | Initial volume/mass                    |
| `sampleItem.remainingQuantity` | `Specimen.extension[remainingQuantity]` | Custom extension (not in base spec)    |
| `sampleItem.parentSampleItem`  | `Specimen.parent[0]`                    | Reference to parent specimen           |
| `sampleItem.typeOfSample`      | `Specimen.type`                         | CodeableConcept (SNOMED CT preferred)  |
| `analysis` (test order)        | `Specimen.request[]`                    | References to ServiceRequest resources |

---

## Concurrency & Transaction Management

### Optimistic Locking Strategy

**Mechanism**: `@Version` annotation on `BaseObject.lastupdated` field

**Scenario**: Multiple users attempt to aliquot from same parent simultaneously

```java
@Service
public class SampleManagementServiceImpl {

    @Transactional
    @Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public SampleItemDTO createAliquot(CreateAliquotForm form) {
        // Load parent with pessimistic read lock (optional for high contention)
        SampleItem parent = sampleItemDAO.get(form.getParentSampleItemId());

        // Validate remaining quantity
        if (parent.getRemainingQuantity() == null ||
            parent.getRemainingQuantity() < form.getQuantityToTransfer()) {
            throw new InsufficientQuantityException(
                "Cannot aliquot: requested " + form.getQuantityToTransfer() +
                " exceeds remaining " + parent.getRemainingQuantity());
        }

        // Generate sequence number and external ID
        Integer nextSequence = aliquotRelationshipDAO
            .getMaxSequenceNumber(parent.getId())
            .map(max -> max + 1)
            .orElse(1);
        String aliquotExternalId = parent.getExternalId() + "." + nextSequence;

        // Create aliquot
        SampleItem aliquot = new SampleItem();
        aliquot.setExternalId(aliquotExternalId);
        aliquot.setOriginalQuantity(form.getQuantityToTransfer());
        aliquot.setRemainingQuantity(form.getQuantityToTransfer());
        aliquot.setParentSampleItem(parent);
        aliquot.setSample(parent.getSample());
        aliquot.setTypeOfSample(parent.getTypeOfSample());
        aliquot.setUnitOfMeasure(parent.getUnitOfMeasure());
        aliquot.setCollectionDate(parent.getCollectionDate());

        // Update parent (triggers @Version check on commit)
        parent.setRemainingQuantity(
            parent.getRemainingQuantity() - form.getQuantityToTransfer());
        sampleItemDAO.save(parent);

        // Save aliquot
        sampleItemDAO.save(aliquot);

        // Create relationship record
        SampleItemAliquotRelationship relationship = new SampleItemAliquotRelationship();
        relationship.setParentSampleItem(parent);
        relationship.setChildSampleItem(aliquot);
        relationship.setSequenceNumber(nextSequence);
        relationship.setQuantityTransferred(form.getQuantityToTransfer());
        relationship.setNotes(form.getNotes());
        aliquotRelationshipDAO.save(relationship);

        // Return DTO (compile within transaction)
        return mapToDTO(aliquot);
    }

    @Recover
    public SampleItemDTO recoverFromOptimisticLock(
        OptimisticLockException ex,
        CreateAliquotForm form) {
        // All retries exhausted - inform user
        throw new ConcurrentModificationException(
            "Another user modified this sample. Please refresh and try again.");
    }
}
```

### Transaction Boundaries

| Operation          | Transaction Scope                                | Rationale                                    |
| ------------------ | ------------------------------------------------ | -------------------------------------------- |
| **Create Aliquot** | Service method (`@Transactional`)                | Atomic update of parent + aliquot creation   |
| **Add Tests**      | Service method (`@Transactional`)                | Bulk insert with duplicate detection         |
| **Search**         | Service method (`@Transactional(readOnly=true)`) | Load hierarchy with JOIN FETCH, compile DTOs |

---

## Summary

### Entities Modified

- ✏️ **SampleItem**: Add 4 new fields (originalQuantity, remainingQuantity,
  parentSampleItem, childAliquots)

### Entities Created

- ✨ **SampleItemAliquotRelationship**: Track aliquot metadata (sequence,
  quantity transferred)

### Database Changes

- 3 new columns on `sample_item` table
- 1 new table `sample_item_aliquot_relationship`
- 2 foreign keys, 3 indexes, 1 unique constraint

### DTOs Created

- 7 new DTOs for request/response (SampleItemDTO, CreateAliquotForm,
  AddTestsForm, etc.)

### FHIR Mapping Extended

- Add `Specimen.parent` reference for aliquots
- Add custom extension for `remainingQuantity`

### Concurrency Control

- Optimistic locking via `@Version` (existing BaseObject field)
- Retry logic with exponential backoff

---

**Next Step**: Create API contracts in `contracts/` directory (OpenAPI 3.0
specifications)
