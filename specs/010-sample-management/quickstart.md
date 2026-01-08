# Developer Quickstart: Sample Management Menu

**Feature**: Sample Management Menu **Branch**: `001-sample-management`
**Date**: 2025-11-20 **Audience**: Developers implementing this feature

---

## Overview

This quickstart guide provides step-by-step instructions for implementing the
Sample Management Menu feature following OpenELIS Global's Spec-Driven
Development (SDD) methodology and constitution requirements.

**What This Feature Does**:

- Search for sample items by accession number
- Add multiple tests to sample items (bulk operations)
- Create aliquots from samples with quantity tracking
- Support nested aliquoting (unlimited depth)
- Display parent-child relationships in hierarchy view

**Prerequisites**:

- Read [AGENTS.md](../../AGENTS.md) for comprehensive project context
- Review [constitution.md](../../.specify/memory/constitution.md) (v1.7.0)
- Understand [research.md](research.md) - technical decisions
- Review [data-model.md](data-model.md) - entity specifications
- Review API contracts in [contracts/](contracts/) directory

---

## Table of Contents

1. [Development Environment Setup](#1-development-environment-setup)
2. [Phase 1: Database & Entities](#2-phase-1-database--entities)
3. [Phase 2: Service Layer](#3-phase-2-service-layer)
4. [Phase 3: Controller Layer](#4-phase-3-controller-layer)
5. [Phase 4: Frontend Components](#5-phase-4-frontend-components)
6. [Phase 5: FHIR Integration](#6-phase-5-fhir-integration)
7. [Phase 6: Testing](#7-phase-6-testing)
8. [Common Patterns & Best Practices](#8-common-patterns--best-practices)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Development Environment Setup

### 1.1 Verify Prerequisites

```bash
# Check Java version (must be Java 21 LTS)
java -version  # Should show "openjdk version \"21.x.x\""

# Check Node.js version (14.x or higher)
node --version  # Should show "v14.x.x" or higher

# Check PostgreSQL (14+)
psql --version  # Should show "psql (PostgreSQL) 14.x" or higher

# Verify on correct feature branch
git branch  # Should show * 001-sample-management
```

### 1.2 Database Setup

```bash
# Start PostgreSQL (if using Docker)
docker-compose up -d database

# Or start local PostgreSQL service
sudo systemctl start postgresql

# Verify database connection
psql -h localhost -U clinlims -d clinlims -c "SELECT version();"
```

### 1.3 Build Project

```bash
# Clean and build (skip tests initially)
mvn clean install -DskipTests -Dmaven.test.skip=true

# Install frontend dependencies
cd frontend && npm install && cd ..
```

### 1.4 Run Application Locally

```bash
# Start backend (from project root)
mvn spring-boot:run

# In separate terminal, start frontend
cd frontend && npm start
```

**Verify**: Access `http://localhost:3000` - login page should load

---

## 2. Phase 1: Database & Entities

### 2.1 Apply Liquibase Changeset

**File to create**:
`src/main/resources/liquibase/2.8.x/sample-management-001.xml`

**Reference**: See
[data-model.md - Database Schema Changes](data-model.md#database-schema-changes)

```bash
# Verify Liquibase changelog is registered in master changelog
grep "sample-management-001.xml" src/main/resources/liquibase/2.8.x/db.changelog-master.xml
```

**Test Database Migration**:

```bash
# Run migrations
mvn liquibase:update

# Verify new columns exist
psql -h localhost -U clinlims -d clinlims -c "\d sample_item"
# Should show: original_quantity, remaining_quantity, parent_sample_item_id

# Verify new table exists
psql -h localhost -U clinlims -d clinlims -c "\d sample_item_aliquot_relationship"
```

**Rollback if needed**:

```bash
# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### 2.2 Modify SampleItem Entity

**File**:
`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`

**Changes**: Add new fields with JPA annotations (hybrid with XML mapping)

**Reference**: See
[data-model.md - SampleItem Entity](data-model.md#1-sampleitem-entity-modified)

**Code snippet**:

```java
// Add after existing fields (around line 50)

@Column(name = "original_quantity", precision = 10, scale = 3)
private Double originalQuantity;

@Column(name = "remaining_quantity", precision = 10, scale = 3)
private Double remainingQuantity;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_sample_item_id")
private SampleItem parentSampleItem;

@OneToMany(mappedBy = "parentSampleItem", cascade = CascadeType.ALL, orphanRemoval = false)
private List<SampleItem> childAliquots = new ArrayList<>();

// Add getters/setters with validation (see data-model.md)
```

**Write Test First** (TDD - Red):

**File**:
`src/test/java/org/openelisglobal/sampleitem/valueholder/SampleItemTest.java`

```java
@Test
public void testRemainingQuantityCannotExceedOriginalQuantity() {
    SampleItem item = new SampleItem();
    item.setOriginalQuantity(10.0);

    assertThrows(IllegalArgumentException.class, () -> {
        item.setRemainingQuantity(15.0); // Should fail
    });
}

@Test
public void testIsAliquotReturnsTrueWhenParentExists() {
    SampleItem parent = new SampleItem();
    SampleItem aliquot = new SampleItem();
    aliquot.setParentSampleItem(parent);

    assertTrue(aliquot.isAliquot());
    assertFalse(parent.isAliquot());
}
```

**Run Test** (should fail - Red phase):

```bash
mvn test -Dtest=SampleItemTest
```

**Implement Code** (Green phase) - Add validation methods to SampleItem.java

**Run Test Again** (should pass - Green phase)

### 2.3 Create SampleItemAliquotRelationship Entity

**File**:
`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItemAliquotRelationship.java`

**Reference**: See
[data-model.md - SampleItemAliquotRelationship Entity](data-model.md#2-sampleitemaliquotrelationship-entity-new)

**Test First**:

```java
@Test
public void testUniqueConstraintOnParentAndSequence() {
    SampleItem parent = createAndSaveSampleItem("PARENT001");

    SampleItemAliquotRelationship rel1 = new SampleItemAliquotRelationship();
    rel1.setParentSampleItem(parent);
    rel1.setSequenceNumber(1);
    rel1.setQuantityTransferred(5.0);
    aliquotRelationshipDAO.save(rel1);

    // Try to create duplicate sequence number - should fail
    SampleItemAliquotRelationship rel2 = new SampleItemAliquotRelationship();
    rel2.setParentSampleItem(parent);
    rel2.setSequenceNumber(1); // Duplicate!
    rel2.setQuantityTransferred(3.0);

    assertThrows(DataIntegrityViolationException.class, () -> {
        aliquotRelationshipDAO.save(rel2);
    });
}
```

### 2.4 Create DTOs

**Files to create**:

- `src/main/java/org/openelisglobal/sampleitem/dto/SampleItemDTO.java`
- `src/main/java/org/openelisglobal/sampleitem/dto/AliquotSummaryDTO.java`
- `src/main/java/org/openelisglobal/sampleitem/dto/TestSummaryDTO.java`

**Reference**: See
[data-model.md - Data Transfer Objects](data-model.md#data-transfer-objects-dtos)

**Use Lombok** for boilerplate reduction:

```java
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SampleItemDTO {
    private String id;
    private String externalId;
    // ... (see data-model.md for full specification)
}
```

**Checkpoint**: Build project

```bash
mvn clean compile
# Should succeed with no errors
```

---

## 3. Phase 2: Service Layer

### 3.1 Create DAO Interfaces and Implementations

**Files to create**:

- `src/main/java/org/openelisglobal/sampleitem/dao/SampleItemAliquotRelationshipDAO.java`
- `src/main/java/org/openelisglobal/sampleitem/daoimpl/SampleItemAliquotRelationshipDAOImpl.java`

**Pattern**: Extend `BaseObjectDAO` and `BaseObjectDAOImpl`

**Key Methods**:

```java
public interface SampleItemAliquotRelationshipDAO extends BaseObjectDAO<SampleItemAliquotRelationship, String> {
    Optional<Integer> getMaxSequenceNumber(String parentSampleItemId);
    List<SampleItemAliquotRelationship> getByParentSampleItem(String parentSampleItemId);
}
```

**HQL Query Example**:

```java
@Override
public Optional<Integer> getMaxSequenceNumber(String parentSampleItemId) {
    String hql = "SELECT MAX(r.sequenceNumber) FROM SampleItemAliquotRelationship r " +
                 "WHERE r.parentSampleItem.id = :parentId";
    Query<Integer> query = entityManager.unwrap(Session.class).createQuery(hql, Integer.class);
    query.setParameter("parentId", parentSampleItemId);
    return Optional.ofNullable(query.uniqueResult());
}
```

**Test DAO Method**:

```java
@Test
@Transactional
public void testGetMaxSequenceNumber() {
    SampleItem parent = createTestParent();

    // No aliquots yet
    Optional<Integer> max = aliquotRelationshipDAO.getMaxSequenceNumber(parent.getId());
    assertFalse(max.isPresent());

    // Create 3 aliquots
    createAliquotRelationship(parent, 1, 5.0);
    createAliquotRelationship(parent, 2, 3.0);
    createAliquotRelationship(parent, 3, 2.0);

    max = aliquotRelationshipDAO.getMaxSequenceNumber(parent.getId());
    assertTrue(max.isPresent());
    assertEquals(3, max.get().intValue());
}
```

### 3.2 Extend SampleItemDAO for Hierarchy Queries

**File**: `src/main/java/org/openelisglobal/sampleitem/dao/SampleItemDAO.java`

**Add method**:

```java
List<SampleItem> getSampleItemsWithHierarchy(String sampleId);
```

**Implementation** (in `SampleItemDAOImpl.java`):

```java
@Override
public List<SampleItem> getSampleItemsWithHierarchy(String sampleId) {
    String hql = "SELECT DISTINCT si FROM SampleItem si " +
                 "LEFT JOIN FETCH si.parentSampleItem parent " +
                 "LEFT JOIN FETCH si.childAliquots children " +
                 "WHERE si.sample.id = :sampleId " +
                 "ORDER BY si.externalId";

    TypedQuery<SampleItem> query = entityManager.createQuery(hql, SampleItem.class);
    query.setParameter("sampleId", sampleId);
    return query.getResultList();
}
```

**Why JOIN FETCH?** Prevents `LazyInitializationException` by loading
relationships within transaction (Constitution v1.4.0).

### 3.3 Create SampleManagementService

**File**:
`src/main/java/org/openelisglobal/sampleitem/service/SampleManagementService.java`

**Key Methods**:

```java
public interface SampleManagementService {
    SearchSamplesResponse searchByAccessionNumber(String accessionNumber, boolean includeTests);
    SampleItemDTO createAliquot(CreateAliquotForm form);
    AddTestsResponse addTestsToSamples(AddTestsForm form);
}
```

### 3.4 Implement createAliquot Method

**File**:
`src/main/java/org/openelisglobal/sampleitem/service/SampleManagementServiceImpl.java`

**Reference**: See
[data-model.md - Concurrency & Transaction Management](data-model.md#concurrency--transaction-management)

**Key Points**:

- `@Transactional` annotation (Constitution - services only)
- `@Retryable` for optimistic lock failures
- Compile full DTO within transaction

**Write Test First** (Integration Test):

```java
@Test
@Transactional
public void testCreateAliquotReducesParentRemainingQuantity() {
    // Setup
    SampleItem parent = createTestSample("SAMPLE001", 10.0, 10.0);
    sampleItemDAO.save(parent);

    // Create aliquot form
    CreateAliquotForm form = new CreateAliquotForm();
    form.setParentSampleItemId(parent.getId());
    form.setQuantityToTransfer(5.0);

    // Execute
    SampleItemDTO aliquot = sampleManagementService.createAliquot(form);

    // Verify
    assertNotNull(aliquot);
    assertEquals("SAMPLE001.1", aliquot.getExternalId());
    assertEquals(5.0, aliquot.getOriginalQuantity(), 0.001);
    assertEquals(5.0, aliquot.getRemainingQuantity(), 0.001);

    // Reload parent and verify remaining quantity
    SampleItem updatedParent = sampleItemDAO.get(parent.getId());
    assertEquals(5.0, updatedParent.getRemainingQuantity(), 0.001);
}

@Test
@Transactional
public void testCreateAliquotThrowsExceptionWhenInsufficientQuantity() {
    SampleItem parent = createTestSample("SAMPLE001", 10.0, 5.0);
    sampleItemDAO.save(parent);

    CreateAliquotForm form = new CreateAliquotForm();
    form.setParentSampleItemId(parent.getId());
    form.setQuantityToTransfer(10.0); // Exceeds remaining (5.0)

    assertThrows(InsufficientQuantityException.class, () -> {
        sampleManagementService.createAliquot(form);
    });
}
```

**Run Tests** (Red phase):

```bash
mvn test -Dtest=SampleManagementServiceTest
```

**Implement Service Method** (Green phase):

```java
@Override
@Transactional
@Retryable(
    value = OptimisticLockException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public SampleItemDTO createAliquot(CreateAliquotForm form) {
    // Load parent
    SampleItem parent = sampleItemDAO.get(form.getParentSampleItemId());
    if (parent == null) {
        throw new NotFoundException("Sample item not found: " + form.getParentSampleItemId());
    }

    // Validate remaining quantity
    if (parent.getRemainingQuantity() == null || parent.getRemainingQuantity() <= 0) {
        throw new NoRemainingQuantityException("All volume dispensed");
    }
    if (parent.getRemainingQuantity() < form.getQuantityToTransfer()) {
        throw new InsufficientQuantityException(
            String.format("Cannot aliquot: requested %.3f exceeds remaining %.3f",
                form.getQuantityToTransfer(), parent.getRemainingQuantity()));
    }

    // Generate sequence number and external ID
    Integer nextSequence = aliquotRelationshipDAO.getMaxSequenceNumber(parent.getId())
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

    // Update parent (triggers @Version check)
    parent.setRemainingQuantity(parent.getRemainingQuantity() - form.getQuantityToTransfer());
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

    // Return DTO (compiled within transaction)
    return mapToDTO(aliquot);
}
```

**Run Tests Again** (should pass - Green phase)

### 3.5 Implement addTestsToSamples Method

**Test First**:

```java
@Test
@Transactional
public void testAddTestsToMultipleSamples() {
    // Setup
    SampleItem sample1 = createTestSample("SAMPLE001");
    SampleItem sample2 = createTestSample("SAMPLE002");
    Test cbcTest = createTestDefinition("CBC");
    Test chemTest = createTestDefinition("Chemistry");

    // Create form
    AddTestsForm form = new AddTestsForm();
    form.setSampleItemIds(Arrays.asList(sample1.getId(), sample2.getId()));
    form.setTestIds(Arrays.asList(cbcTest.getId(), chemTest.getId()));

    // Execute
    AddTestsResponse response = sampleManagementService.addTestsToSamples(form);

    // Verify
    assertEquals(4, response.getSuccessCount()); // 2 samples × 2 tests
    assertEquals(4, response.getResults().size());
    assertTrue(response.getResults().stream().allMatch(TestAdditionResult::isSuccess));
}

@Test
@Transactional
public void testAddTestsSkipsDuplicates() {
    // Setup
    SampleItem sample = createTestSample("SAMPLE001");
    Test cbcTest = createTestDefinition("CBC");

    // Add CBC test first time
    Analysis existing = new Analysis();
    existing.setSampleItem(sample);
    existing.setTest(cbcTest);
    existing.setStatusId("ORDERED");
    analysisDAO.save(existing);

    // Try to add same test again
    AddTestsForm form = new AddTestsForm();
    form.setSampleItemIds(Collections.singletonList(sample.getId()));
    form.setTestIds(Collections.singletonList(cbcTest.getId()));

    // Execute
    AddTestsResponse response = sampleManagementService.addTestsToSamples(form);

    // Verify
    assertEquals(0, response.getSuccessCount());
    assertEquals(1, response.getResults().size());
    assertFalse(response.getResults().get(0).isSuccess());
    assertEquals("Test already ordered for this sample", response.getResults().get(0).getMessage());
}
```

**Checkpoint**: Run all service tests

```bash
mvn test -Dtest=*ServiceTest
```

---

## 4. Phase 3: Controller Layer

### 4.1 Create Form Validation Classes

**Files to create**:

- `src/main/java/org/openelisglobal/sampleitem/form/CreateAliquotForm.java`
- `src/main/java/org/openelisglobal/sampleitem/form/AddTestsForm.java`

**Reference**: See [data-model.md - DTOs](data-model.md#4-createaliquotrequest)

**Use Jakarta Validation**:

```java
import jakarta.validation.constraints.*;

@Data
public class CreateAliquotForm {
    @NotBlank(message = "{validation.parentSampleItemId.required}")
    private String parentSampleItemId;

    @NotNull(message = "{validation.quantityToTransfer.required}")
    @DecimalMin(value = "0.001", message = "{validation.quantityToTransfer.min}")
    private Double quantityToTransfer;

    @Size(max = 1000, message = "{validation.notes.size}")
    private String notes;
}
```

**Add validation messages** to `messages_en.properties`:

```properties
validation.parentSampleItemId.required=Parent sample item ID is required
validation.quantityToTransfer.required=Quantity to transfer is required
validation.quantityToTransfer.min=Quantity must be at least 0.001
validation.notes.size=Notes must not exceed 1000 characters
```

### 4.2 Create REST Controller

**File**:
`src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementRestController.java`

**Reference**: See [contracts/](contracts/) for OpenAPI specifications

**Code Structure**:

```java
package org.openelisglobal.sampleitem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/sample-management")
@Validated
public class SampleManagementRestController {

    @Autowired
    private SampleManagementService sampleManagementService;

    @GetMapping("/search")
    public ResponseEntity<SearchSamplesResponse> searchSamples(
        @RequestParam @NotBlank String accessionNumber,
        @RequestParam(defaultValue = "false") boolean includeTests) {

        SearchSamplesResponse response = sampleManagementService
            .searchByAccessionNumber(accessionNumber, includeTests);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/aliquot")
    public ResponseEntity<CreateAliquotResponse> createAliquot(
        @Valid @RequestBody CreateAliquotForm form) {

        SampleItemDTO aliquot = sampleManagementService.createAliquot(form);

        CreateAliquotResponse response = new CreateAliquotResponse();
        response.setAliquot(aliquot);
        response.setParentUpdatedRemainingQuantity(/* fetch parent remaining qty */);
        response.setMessage("Aliquot created successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/add-tests")
    public ResponseEntity<AddTestsResponse> addTests(
        @Valid @RequestBody AddTestsForm form) {

        AddTestsResponse response = sampleManagementService.addTestsToSamples(form);
        return ResponseEntity.ok(response);
    }
}
```

### 4.3 Add Exception Handlers

**File**:
`src/main/java/org/openelisglobal/sampleitem/controller/SampleManagementExceptionHandler.java`

```java
@RestControllerAdvice(basePackages = "org.openelisglobal.sampleitem.controller")
public class SampleManagementExceptionHandler {

    @ExceptionHandler(InsufficientQuantityException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientQuantity(
        InsufficientQuantityException ex) {

        ErrorResponse error = new ErrorResponse();
        error.setError("INSUFFICIENT_QUANTITY");
        error.setMessage(ex.getMessage());
        error.setTimestamp(Instant.now().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
        OptimisticLockException ex) {

        ErrorResponse error = new ErrorResponse();
        error.setError("CONCURRENT_MODIFICATION");
        error.setMessage("Another user modified this sample. Please refresh and try again.");
        error.setTimestamp(Instant.now().toString());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Add other exception handlers...
}
```

### 4.4 Test Controller Endpoints

**File**:
`src/test/java/org/openelisglobal/sampleitem/controller/SampleManagementRestControllerTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class SampleManagementRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSearchSamplesEndpoint() throws Exception {
        mockMvc.perform(get("/api/sample-management/search")
                .param("accessionNumber", "2025-001234")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessionNumber").value("2025-001234"))
            .andExpect(jsonPath("$.sampleItems").isArray());
    }

    @Test
    public void testCreateAliquotEndpoint() throws Exception {
        String requestBody = """
            {
                "parentSampleItemId": "parent-uuid",
                "quantityToTransfer": 5.0,
                "notes": "For PCR testing"
            }
            """;

        mockMvc.perform(post("/api/sample-management/aliquot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.aliquot.externalId").exists())
            .andExpect(jsonPath("$.message").value("Aliquot created successfully"));
    }

    @Test
    public void testCreateAliquotWithInsufficientQuantityReturns409() throws Exception {
        // Setup parent with low remaining quantity...

        String requestBody = """
            {
                "parentSampleItemId": "parent-uuid",
                "quantityToTransfer": 100.0
            }
            """;

        mockMvc.perform(post("/api/sample-management/aliquot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("INSUFFICIENT_QUANTITY"));
    }
}
```

**Checkpoint**: Run controller tests

```bash
mvn test -Dtest=*ControllerTest
```

---

## 5. Phase 4: Frontend Components

### 5.1 Create React Components

**Directory Structure**:

```
frontend/src/components/sampleManagement/
├── SampleManagement.js (main container)
├── SampleSearch.js (search interface)
├── SampleResultsTable.js (results display)
├── AliquotModal.js (aliquot creation form)
├── TestSelectorModal.js (bulk test selection)
└── HierarchyTreeView.js (parent-child hierarchy)
```

### 5.2 Implement SampleSearch Component

**File**: `frontend/src/components/sampleManagement/SampleSearch.js`

**Use Carbon ComboBox** (Constitution - Carbon Design System required):

```javascript
import React, { useState } from "react";
import { ComboBox, Search } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const SampleSearch = ({ onSearchResults }) => {
  const intl = useIntl();
  const [accessionNumber, setAccessionNumber] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!accessionNumber.trim()) return;

    setLoading(true);
    try {
      const response = await fetch(
        `/api/sample-management/search?accessionNumber=${encodeURIComponent(
          accessionNumber
        )}`
      );
      const data = await response.json();
      onSearchResults(data);
    } catch (error) {
      console.error("Search failed:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="sample-search">
      <Search
        id="accession-number-search"
        labelText={intl.formatMessage({ id: "sample.search.accessionNumber" })}
        placeholder={intl.formatMessage({
          id: "sample.search.placeholder",
        })}
        value={accessionNumber}
        onChange={(e) => setAccessionNumber(e.target.value)}
        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
        disabled={loading}
      />
    </div>
  );
};

export default SampleSearch;
```

**Add i18n strings** to `messages_en.js`:

```javascript
{
  "sample.search.accessionNumber": "Accession Number",
  "sample.search.placeholder": "Enter accession number to search...",
  "sample.results.externalId": "External ID",
  "sample.results.type": "Sample Type",
  "sample.results.originalQty": "Original Qty",
  "sample.results.remainingQty": "Remaining Qty",
  // ... add all UI strings
}
```

### 5.3 Implement SampleResultsTable Component

**File**: `frontend/src/components/sampleManagement/SampleResultsTable.js`

**Use Carbon DataTable** with selection:

```javascript
import React from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";

const SampleResultsTable = ({ sampleItems, onSelectionChange }) => {
  const headers = [
    {
      key: "externalId",
      header: <FormattedMessage id="sample.results.externalId" />,
    },
    {
      key: "sampleType",
      header: <FormattedMessage id="sample.results.type" />,
    },
    {
      key: "originalQuantity",
      header: <FormattedMessage id="sample.results.originalQty" />,
    },
    {
      key: "remainingQuantity",
      header: <FormattedMessage id="sample.results.remainingQty" />,
    },
    { key: "status", header: <FormattedMessage id="sample.results.status" /> },
  ];

  const rows = sampleItems.map((item) => ({
    id: item.id,
    externalId: item.externalId,
    sampleType: item.sampleType,
    originalQuantity: item.originalQuantity
      ? `${item.originalQuantity} ${item.unitOfMeasure}`
      : "-",
    remainingQuantity: item.remainingQuantity
      ? `${item.remainingQuantity} ${item.unitOfMeasure}`
      : "-",
    status: item.status,
  }));

  return (
    <DataTable rows={rows} headers={headers}>
      {({
        rows,
        headers,
        getHeaderProps,
        getRowProps,
        getSelectionProps,
        selectedRows,
      }) => {
        // Call parent callback when selection changes
        React.useEffect(() => {
          onSelectionChange(selectedRows.map((row) => row.id));
        }, [selectedRows]);

        return (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableSelectAll {...getSelectionProps()} />
                  {headers.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    <TableSelectRow {...getSelectionProps({ row })} />
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        );
      }}
    </DataTable>
  );
};

export default SampleResultsTable;
```

### 5.4 Implement AliquotModal Component

**File**: `frontend/src/components/sampleManagement/AliquotModal.js`

**Use Carbon ComposedModal**:

```javascript
import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const AliquotModal = ({
  open,
  onClose,
  parentSampleItem,
  onAliquotCreated,
}) => {
  const intl = useIntl();
  const [quantity, setQuantity] = useState("");
  const [notes, setNotes] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!quantity || parseFloat(quantity) <= 0) {
      setError("Quantity must be greater than 0");
      return;
    }

    if (parseFloat(quantity) > parentSampleItem.remainingQuantity) {
      setError(
        `Quantity exceeds remaining (${parentSampleItem.remainingQuantity})`
      );
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch("/api/sample-management/aliquot", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          parentSampleItemId: parentSampleItem.id,
          quantityToTransfer: parseFloat(quantity),
          notes: notes,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || "Failed to create aliquot");
      }

      const data = await response.json();
      onAliquotCreated(data.aliquot);
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <ComposedModal open={open} onClose={onClose}>
      <ModalHeader
        title={<FormattedMessage id="sample.aliquot.createTitle" />}
        label={parentSampleItem?.externalId}
      />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            title="Error"
            subtitle={error}
            onClose={() => setError(null)}
          />
        )}

        <TextInput
          id="parent-external-id"
          labelText={<FormattedMessage id="sample.aliquot.parentExternalId" />}
          value={parentSampleItem?.externalId || ""}
          disabled
        />

        <TextInput
          id="remaining-quantity"
          labelText={<FormattedMessage id="sample.aliquot.remainingQuantity" />}
          value={`${parentSampleItem?.remainingQuantity || 0} ${
            parentSampleItem?.unitOfMeasure || ""
          }`}
          disabled
        />

        <TextInput
          id="quantity-to-transfer"
          labelText={
            <FormattedMessage id="sample.aliquot.quantityToTransfer" />
          }
          type="number"
          step="0.001"
          min="0.001"
          max={parentSampleItem?.remainingQuantity}
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
          invalid={parseFloat(quantity) > parentSampleItem?.remainingQuantity}
          invalidText="Exceeds remaining quantity"
        />

        <TextInput
          id="notes"
          labelText={<FormattedMessage id="sample.aliquot.notes" />}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          maxLength={1000}
        />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          <FormattedMessage id="button.cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSubmit}
          disabled={loading || !quantity}
        >
          <FormattedMessage id="sample.aliquot.create" />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default AliquotModal;
```

### 5.5 Add to Main Menu

**File**: `frontend/src/components/menu/MenuLinks.js`

```javascript
const menuLinks = [
  // ... existing menu items
  {
    id: "sample-management",
    labelId: "menu.sampleManagement",
    path: "/sample-management",
    icon: <Flask />,
    requiredRoles: ["Lab Technician", "Lab Manager"],
  },
];
```

**Checkpoint**: Run frontend

```bash
cd frontend
npm start
# Verify Sample Management menu appears and components render
```

---

## 6. Phase 5: FHIR Integration

### 6.1 Extend FhirTransformServiceImpl

**File**:
`src/main/java/org/openelisglobal/dataexchange/fhir/service/FhirTransformServiceImpl.java`

**Modify `transformToSpecimen` method** (around line 1003):

**Reference**: See
[data-model.md - FHIR R4 Mapping](data-model.md#fhir-r4-mapping)

```java
private Specimen transformToSpecimen(SampleItem sampleItem) {
    Specimen specimen = new Specimen();

    // ... existing code ...

    // NEW: Add quantity using originalQuantity
    if (sampleItem.getOriginalQuantity() != null) {
        SpecimenContainerComponent container = new SpecimenContainerComponent();
        Quantity specimenQuantity = new Quantity();
        specimenQuantity.setValue(sampleItem.getOriginalQuantity());
        specimenQuantity.setUnit(sampleItem.getUnitOfMeasure().getName());
        specimenQuantity.setSystem("http://unitsofmeasure.org");
        container.setSpecimenQuantity(specimenQuantity);
        specimen.addContainer(container);
    }

    // NEW: Add parent reference for aliquots
    if (sampleItem.getParentSampleItem() != null) {
        Reference parentRef = createReferenceFor(
            ResourceType.Specimen,
            sampleItem.getParentSampleItem().getFhirUuidAsString());
        specimen.addParent(parentRef);
    }

    // NEW: Add custom extension for remaining quantity
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

    return specimen;
}
```

### 6.2 Test FHIR Mapping

```java
@Test
public void testSpecimenIncludesParentReferenceForAliquot() {
    // Setup
    SampleItem parent = createTestSample("PARENT001");
    parent.setFhirUuid(UUID.randomUUID());

    SampleItem aliquot = createTestSample("PARENT001.1");
    aliquot.setParentSampleItem(parent);
    aliquot.setOriginalQuantity(5.0);
    aliquot.setRemainingQuantity(5.0);

    // Transform
    Specimen specimen = fhirTransformService.transformToSpecimen(aliquot);

    // Verify
    assertNotNull(specimen.getParent());
    assertEquals(1, specimen.getParent().size());
    assertEquals(parent.getFhirUuidAsString(), specimen.getParent().get(0).getReferenceElement().getIdPart());
}
```

---

## 7. Phase 6: Testing

### 7.1 Unit Tests

**Coverage Target**: >70% (Constitution V.1)

```bash
# Run unit tests with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### 7.2 Integration Tests

**File**:
`src/test/java/org/openelisglobal/sampleitem/integration/SampleManagementIntegrationTest.java`

```java
@SpringBootTest
@Transactional
public class SampleManagementIntegrationTest {

    @Test
    public void testCompleteAliquotingWorkflow() {
        // Create parent sample
        // Create first aliquot
        // Create nested aliquot from first aliquot
        // Verify quantities cascade correctly
        // Verify external IDs follow pattern (PARENT.1.1)
    }
}
```

### 7.3 E2E Tests (Cypress)

**File**: `frontend/cypress/e2e/sampleManagement.cy.js`

**Constitution Requirement**: Individual E2E tests must be runnable
independently (v1.5.0)

```javascript
describe("Sample Management - Search", () => {
  beforeEach(() => {
    cy.login("admin", "password");
    cy.visit("/sample-management");
  });

  it("should search for sample by accession number", () => {
    cy.get("#accession-number-search").type("2025-001234");
    cy.get("#accession-number-search").type("{enter}");

    cy.get("[data-testid=sample-results-table]").should("be.visible");
    cy.contains("2025-001234").should("exist");
  });
});

describe("Sample Management - Create Aliquot", () => {
  it("should create aliquot and update parent quantity", () => {
    // ... test implementation
  });
});

describe("Sample Management - Add Tests", () => {
  it("should add multiple tests to a sample", () => {
    // ... test implementation
  });
});
```

**Run individual E2E test**:

```bash
# Constitution v1.5.0: Run tests individually during development
npm run cy:run -- --spec "cypress/e2e/sampleManagement.cy.js"

# Run specific test
npm run cy:run -- --spec "cypress/e2e/sampleManagement.cy.js" --grep "should search for sample"
```

---

## 8. Common Patterns & Best Practices

### 8.1 Constitution Compliance Checklist

Before committing code, verify:

- [ ] **CR-001**: UI uses Carbon Design System components (NO custom CSS)
- [ ] **CR-002**: All UI strings use React Intl (NO hardcoded text)
- [ ] **CR-003**: Backend follows 5-layer architecture
- [ ] **CR-004**: Database changes use Liquibase (NO direct DDL)
- [ ] **CR-005**: FHIR R4 compliance for external integration
- [ ] **CR-006**: @Transactional only in service layer
- [ ] **CR-007**: Services compile data within transaction
- [ ] **CR-008**: Tests included (unit + integration + E2E)
- [ ] **CR-009**: Code formatted with Spotless/Prettier

### 8.2 Pre-Commit Workflow

**MANDATORY before every commit**:

```bash
# Backend formatting
mvn spotless:apply

# Frontend formatting
cd frontend && npm run format && cd ..

# Run tests
mvn test

# Verify build
mvn clean install -DskipTests -Dmaven.test.skip=true
```

### 8.3 TDD Workflow (Red-Green-Refactor)

1. **Red**: Write failing test first
2. **Green**: Write minimal code to pass test
3. **Refactor**: Improve code quality while keeping tests green

**Example**:

```bash
# Write test (Red)
# Run: mvn test -Dtest=SampleManagementServiceTest#testCreateAliquot
# Should FAIL

# Implement feature (Green)
# Run: mvn test -Dtest=SampleManagementServiceTest#testCreateAliquot
# Should PASS

# Refactor (extract methods, improve naming, etc.)
# Run: mvn test
# All tests should still PASS
```

---

## 9. Troubleshooting

### 9.1 LazyInitializationException

**Problem**: `could not initialize proxy - no Session`

**Solution**: Use `JOIN FETCH` in HQL queries and compile DTOs within
transaction

```java
// BAD
@Transactional(readOnly = true)
public List<SampleItemDTO> search(String accessionNumber) {
    List<SampleItem> items = dao.findByAccessionNumber(accessionNumber);
    // Transaction ends here
    return items.stream().map(this::mapToDTO).collect(Collectors.toList());
    // FAIL - accessing lazy collections outside transaction
}

// GOOD
@Transactional(readOnly = true)
public List<SampleItemDTO> search(String accessionNumber) {
    List<SampleItem> items = dao.findWithHierarchy(accessionNumber); // JOIN FETCH
    // Compile DTOs WITHIN transaction
    return items.stream()
        .map(item -> {
            SampleItemDTO dto = new SampleItemDTO();
            dto.setId(item.getId());
            // Access all lazy collections HERE while transaction is open
            dto.setChildAliquots(item.getChildAliquots().stream()...);
            return dto;
        })
        .collect(Collectors.toList());
    // Transaction ends AFTER all data is compiled
}
```

### 9.2 OptimisticLockException

**Problem**:
`org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction`

**Solution**: Add `@Retryable` annotation to service method

```java
@Transactional
@Retryable(
    value = OptimisticLockException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public SampleItemDTO createAliquot(CreateAliquotForm form) {
    // Implementation that updates parent's remaining quantity
}
```

### 9.3 Liquibase Checksum Mismatch

**Problem**: `Validation Failed: 1 change sets check sum`

**Solution**: Clear Liquibase checksums and reapply

```bash
# Clear checksums
mvn liquibase:clearCheckSums

# Reapply changesets
mvn liquibase:update
```

### 9.4 Frontend Component Not Rendering

**Problem**: Component doesn't appear or throws error

**Checklist**:

- [ ] Is component imported correctly?
- [ ] Are all i18n message keys defined in `messages_en.js`?
- [ ] Are Carbon components imported from `@carbon/react`?
- [ ] Is React Intl `<FormattedMessage>` used (not hardcoded strings)?

---

## Next Steps

After completing this quickstart:

1. Review completed implementation against [spec.md](spec.md) acceptance
   criteria
2. Run `/speckit.analyze` to validate consistency across artifacts
3. Create pull request following
   [PULL_REQUEST_TIPS.md](../../PULL_REQUEST_TIPS.md)
4. Update agent context: `.specify/scripts/bash/update-agent-context.sh claude`

---

**Questions?** See [AGENTS.md](../../AGENTS.md) or ask in project Slack channel.
