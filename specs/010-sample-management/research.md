# Research Document: Sample Management Menu

**Feature**: Sample Management Menu **Branch**: `001-sample-management`
**Date**: 2025-11-20 **Purpose**: Phase 0 research to resolve technical unknowns
and document existing patterns

---

## 1. Sample Item Entity Analysis

### Current Structure

**Entity File**:
[`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`](../../src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java)

**Key Findings**:

#### Existing Fields

- `String id` - Primary key
- `UUID fhirUuid` - FHIR R4 integration (line 37)
- **`Double quantity`** - ✅ **EXISTS** (line 35) - Single quantity field
  currently used
- `String externalId` - External identifier (line 48)
- `Timestamp collectionDate` - Collection timestamp (line 49)
- `String statusId` - Status reference (line 50)
- `ValueHolderInterface sample` - Parent Sample reference (line 38)
- `ValueHolderInterface typeOfSample` - Sample type (line 44)
- `ValueHolderInterface unitOfMeasure` - Quantity unit (line 46)

#### Current Mapping Approach

**Mapping File**:
[`src/main/resources/hibernate/hbm/SampleItem.hbm.xml`](../../src/main/resources/hibernate/hbm/SampleItem.hbm.xml)

**⚠️ LEGACY XML MAPPING** - Uses XML mapping file instead of annotations (lines
1-79)

```xml
<class name="org.openelisglobal.sampleitem.valueholder.SampleItem"
       table="SAMPLE_ITEM" optimistic-lock="version">
    <property name="quantity" type="double">
        <column name="QUANTITY" precision="22" scale="0" />
    </property>
    <property name="externalId" type="java.lang.String" column="external_id" />
    <property name="fhirUuid" column="fhir_uuid" type="java.util.UUID" />
</class>
```

### Decision: Entity Modification Strategy

**What was chosen**: Modify existing `SampleItem` entity with annotation-based
mappings for new fields

**Rationale**:

1. Constitution v1.3.0 mandates **JPA/Hibernate annotations** for new/modified
   entities
2. Existing XML mapping remains for legacy fields (exempt until refactored)
3. New fields use `@Column`, `@ManyToOne` annotations per modern Hibernate best
   practices
4. Extends `BaseObject<String>` which provides `@Version` for optimistic locking

**Alternatives considered**:

- ❌ Create new `SampleItemAliquot` entity: Would duplicate fields and
  complicate queries
- ❌ Keep XML mapping for new fields: Violates constitution requirement
- ✅ **Hybrid approach**: Legacy XML + new annotations for new fields only

**Code references**:

- [`SampleItem.java`](../../src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java) -
  Lines 29-260
- [`SampleItem.hbm.xml`](../../src/main/resources/hibernate/hbm/SampleItem.hbm.xml) -
  Lines 55-57 (quantity field)

### Required Modifications

**Add to SampleItem.java**:

```java
// New annotation-based fields (hybrid with XML mapping)
@Column(name = "original_quantity", precision = 10, scale = 3)
private Double originalQuantity;

@Column(name = "remaining_quantity", precision = 10, scale = 3)
private Double remainingQuantity;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_sample_item_id")
private SampleItem parentSampleItem;

@OneToMany(mappedBy = "parentSampleItem", cascade = CascadeType.ALL, orphanRemoval = false)
private List<SampleItem> childAliquots = new ArrayList<>();

// Getters/setters with validation
public void setRemainingQuantity(Double remainingQuantity) {
    if (originalQuantity != null && remainingQuantity > originalQuantity) {
        throw new IllegalArgumentException("Remaining quantity cannot exceed original quantity");
    }
    this.remainingQuantity = remainingQuantity;
}
```

---

## 2. Test Ordering System Analysis

### Current Test-Sample Relationship

**Entity File**:
[`src/main/java/org/openelisglobal/analysis/valueholder/Analysis.java`](../../src/main/java/org/openelisglobal/analysis/valueholder/Analysis.java)

**Key Findings**:

- ✅ **Analysis entity serves as TestOrder** - Links Test to SampleItem
- Has `@ManyToOne` relationship to `SampleItem`
- Has `@ManyToOne` relationship to `Test`
- Includes `statusId` field for test lifecycle (ORDERED, IN_PROGRESS, COMPLETED)
- Has `fhirUuid` for FHIR ServiceRequest mapping

**Search Pattern**: `AnalysisService.getAnalysesBySampleItem(SampleItem)`
retrieves tests for a sample

### Decision: Use Existing Analysis Entity

**What was chosen**: Use existing `Analysis` entity for test ordering (rename
conceptually to "TestOrder" in documentation)

**Rationale**:

1. Analysis already provides the exact functionality needed
2. Avoids creating duplicate entity with same relationships
3. Maintains consistency with existing codebase patterns
4. FHIR mapping already established (Analysis → ServiceRequest)

**Alternatives considered**:

- ❌ Create new `TestOrder` entity: Would duplicate Analysis functionality
- ❌ Create junction table manually: JPA handles this via Analysis entity
- ✅ **Use Analysis entity with service layer abstraction**

**Code references**:

- [`Analysis.java`](../../src/main/java/org/openelisglobal/analysis/valueholder/Analysis.java) -
  Entity definition
- [`AnalysisService.java`](../../src/main/java/org/openelisglobal/analysis/service/AnalysisService.java) -
  Service methods

### Duplicate Test Detection Pattern

**Existing Logic**: Query by sampleItemId + testId combination

```java
// AnalysisDAO pattern for duplicate detection
Analysis getAnalysisBySampleItemAndTest(String sampleItemId, String testId);
```

**Implementation for bulk add**:

```java
// Service layer checks before insert
List<Analysis> existing = analysisService.getAnalysesBySampleItem(sampleItem);
Set<String> existingTestIds = existing.stream()
    .map(a -> a.getTest().getId())
    .collect(Collectors.toSet());

List<String> duplicates = testIds.stream()
    .filter(existingTestIds::contains)
    .collect(Collectors.toList());
```

---

## 3. Accession Number Search Implementation

### Current Search Pattern

**DAO Interface**:
[`src/main/java/org/openelisglobal/sample/dao/SampleDAO.java`](../../src/main/java/org/openelisglobal/sample/dao/SampleDAO.java)

**Key Methods** (Lines 36-38):

```java
void getSampleByAccessionNumber(Sample sample) throws LIMSRuntimeException;
Sample getSampleByAccessionNumber(String accessionNumber) throws LIMSRuntimeException;
```

**Search Flow**:

1. User enters accession number
2. `SampleDAO.getSampleByAccessionNumber(accessionNumber)` retrieves Sample
3. `SampleItemService.getSampleItemsBySampleId(sampleId)` retrieves all
   SampleItems for that Sample
4. Display SampleItems with parent/child relationships

### Decision: Extend Existing Search with Hierarchy Support

**What was chosen**: Use existing `getSampleByAccessionNumber` + add hierarchy
queries

**Rationale**:

1. Proven search implementation already exists
2. Need to add recursive query for parent-child relationships
3. Service layer compiles full hierarchy within transaction (per Constitution
   v1.4.0)

**Alternatives considered**:

- ❌ Create new search endpoint: Duplicates existing functionality
- ❌ Search SampleItem directly: Accession number belongs to Sample, not
  SampleItem
- ✅ **Extend existing pattern with JOIN FETCH for hierarchy**

**Code references**:

- [`SampleDAO.java`](../../src/main/java/org/openelisglobal/sample/dao/SampleDAO.java) -
  Lines 36-38
- [`SampleDAOImpl.java`](../../src/main/java/org/openelisglobal/sample/daoimpl/SampleDAOImpl.java) -
  Implementation

### Hierarchy Query Pattern

**HQL with JOIN FETCH** (prevents LazyInitializationException):

```java
// DAO method for loading full hierarchy
SELECT DISTINCT si FROM SampleItem si
LEFT JOIN FETCH si.parentSampleItem parent
LEFT JOIN FETCH si.childAliquots children
WHERE si.sample.accessionNumber = :accessionNumber
ORDER BY si.externalId
```

**Service method compiles full tree**:

```java
@Transactional(readOnly = true)
public List<SampleItemDTO> searchByAccessionNumber(String accessionNumber) {
    Sample sample = sampleDAO.getSampleByAccessionNumber(accessionNumber);
    List<SampleItem> items = sampleItemDAO.getSampleItemsWithHierarchy(sample.getId());

    // Compile full hierarchy within transaction
    return items.stream()
        .map(item -> {
            SampleItemDTO dto = new SampleItemDTO();
            dto.setId(item.getId());
            dto.setExternalId(item.getExternalId());
            dto.setOriginalQuantity(item.getOriginalQuantity());
            dto.setRemainingQuantity(item.getRemainingQuantity());

            // Load parent/children within transaction
            if (item.getParentSampleItem() != null) {
                dto.setParentId(item.getParentSampleItem().getId());
                dto.setParentExternalId(item.getParentSampleItem().getExternalId());
            }

            dto.setChildAliquots(item.getChildAliquots().stream()
                .map(child -> new AliquotSummaryDTO(child.getId(), child.getExternalId()))
                .collect(Collectors.toList()));

            return dto;
        })
        .collect(Collectors.toList());
}
```

---

## 4. Nested Aliquoting Technical Approach

### Recursive Parent-Child Relationships in JPA/Hibernate

**Pattern**: Self-referential `@ManyToOne` / `@OneToMany` relationship

**Best Practice** (from Hibernate documentation):

```java
@Entity
@Table(name = "sample_item")
public class SampleItem {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sample_item_id")
    private SampleItem parentSampleItem;

    @OneToMany(mappedBy = "parentSampleItem", cascade = CascadeType.ALL)
    private List<SampleItem> childAliquots = new ArrayList<>();
}
```

### Decision: Self-Referential Relationship + Separate Tracking Table

**What was chosen**:

1. `SampleItem.parentSampleItem` field for direct parent reference
2. `SampleItemAliquotRelationship` table for metadata (sequence number, quantity
   transferred)

**Rationale**:

1. Self-referential FK enables efficient parent/child queries
2. Separate tracking table stores aliquot-specific metadata (sequence, quantity)
3. Enables recursive CTE queries for full lineage
4. Follows storage location hierarchy pattern (see feature 001-sample-storage)

**Alternatives considered**:

- ❌ Closure table pattern: Overkill for tree structure
- ❌ Materialized path (store "1.2.3" as string): Difficult to query efficiently
- ✅ **Adjacency list (parent FK) + metadata table**

**Code references from similar implementation**:

- [`StoragePosition.java`](../../src/main/java/org/openelisglobal/storage/valueholder/StoragePosition.java) -
  Similar parent-child pattern
- Storage feature uses: Position → Rack → Shelf → Device → Room hierarchy

### External ID Generation Pattern

**Decision**: Append ".{sequence}" to parent's external ID

**Algorithm**:

```java
public String generateAliquotExternalId(SampleItem parent) {
    // Get max sequence number for this parent
    Integer maxSeq = aliquotRelationshipDAO.getMaxSequenceNumber(parent.getId());
    int nextSeq = (maxSeq != null) ? maxSeq + 1 : 1;

    // Append to parent's external ID
    String parentExternalId = parent.getExternalId();
    return parentExternalId + "." + nextSeq;
}
```

**Examples**:

- Parent: `SAMPLE001` → First aliquot: `SAMPLE001.1`
- `SAMPLE001.1` → Nested aliquot: `SAMPLE001.1.1`
- `SAMPLE001.1.1` → Deeper nesting: `SAMPLE001.1.1.1`

### Efficient Hierarchical Queries

**Recursive CTE for Full Lineage** (PostgreSQL):

```sql
WITH RECURSIVE lineage AS (
    -- Anchor: start with specific sample
    SELECT id, external_id, parent_sample_item_id, 0 AS level
    FROM sample_item
    WHERE id = ?

    UNION ALL

    -- Recursive: get all ancestors
    SELECT si.id, si.external_id, si.parent_sample_item_id, l.level + 1
    FROM sample_item si
    INNER JOIN lineage l ON si.id = l.parent_sample_item_id
)
SELECT * FROM lineage ORDER BY level DESC;
```

**HQL Alternative** (Hibernate):

```java
// Load full tree with single query
@Query("SELECT si FROM SampleItem si " +
       "LEFT JOIN FETCH si.parentSampleItem " +
       "LEFT JOIN FETCH si.childAliquots " +
       "WHERE si.sample.id = :sampleId")
List<SampleItem> getSampleItemsWithHierarchy(@Param("sampleId") String sampleId);
```

---

## 5. FHIR R4 Specimen Mapping

### Existing FHIR Transform Pattern

**Transform Service**:
[`FhirTransformServiceImpl.java`](../../src/main/java/org/openelisglobal/dataexchange/fhir/service/FhirTransformServiceImpl.java)

**Specimen Transform Method** (Lines 1003-1024):

```java
private Specimen transformToSpecimen(SampleItem sampleItem) {
    Specimen specimen = new Specimen();
    specimen.setId(sampleItem.getFhirUuidAsString());
    specimen.addIdentifier(this.createIdentifier(
        fhirConfig.getOeFhirSystem() + "/sampleItem_uuid",
        sampleItem.getFhirUuidAsString()));
    specimen.setAccessionIdentifier(this.createIdentifier(
        fhirConfig.getOeFhirSystem() + "/sampleItem_labNo",
        sampleItem.getSample().getAccessionNumber() + "-" + sampleItem.getSortOrder()));
    specimen.setStatus(SpecimenStatus.AVAILABLE);
    specimen.setType(transformTypeOfSampleToCodeableConcept(sampleItem.getTypeOfSample()));
    specimen.setReceivedTime(new Date());
    specimen.setCollection(transformToCollection(sampleItem.getCollectionDate(), sampleItem.getCollector()));

    // Link to ServiceRequests (Analysis)
    for (Analysis analysis : analysisService.getAnalysesBySampleItem(sampleItem)) {
        specimen.addRequest(this.createReferenceFor(ResourceType.ServiceRequest, analysis.getFhirUuidAsString()));
    }

    // Link to Patient
    specimen.setSubject(this.createReferenceFor(ResourceType.Patient, patient.getFhirUuidAsString()));

    return specimen;
}
```

### Parent-Child Specimen Relationships

**FHIR R4 Specimen.parent Field**:

```java
// Add parent reference for aliquots
if (sampleItem.getParentSampleItem() != null) {
    Reference parentRef = this.createReferenceFor(
        ResourceType.Specimen,
        sampleItem.getParentSampleItem().getFhirUuidAsString());
    specimen.addParent(parentRef);
}
```

**Specimen Container Quantity**:

```java
SpecimenContainerComponent container = new SpecimenContainerComponent();
Quantity specimenQuantity = new Quantity();
specimenQuantity.setValue(sampleItem.getOriginalQuantity());
specimenQuantity.setUnit(sampleItem.getUnitOfMeasure().getName());
specimenQuantity.setSystem("http://unitsofmeasure.org");
container.setSpecimenQuantity(specimenQuantity);
specimen.setContainer(container);
```

### fhirUuid Generation Pattern

**From BaseObject** (Constitution-compliant pattern):

```java
@PrePersist
protected void onCreate() {
    if (fhirUuid == null) {
        fhirUuid = UUID.randomUUID();
    }
}

public String getFhirUuidAsString() {
    return fhirUuid != null ? fhirUuid.toString() : null;
}
```

**Lifecycle Hooks for FHIR Sync**:

```java
@PostPersist
protected void onPostPersist() {
    syncToFhir(true); // Create operation
}

@PostUpdate
protected void onPostUpdate() {
    syncToFhir(false); // Update operation
}
```

### Decision: Extend Existing Specimen Transform

**What was chosen**: Extend `transformToSpecimen` to include parent reference
and remaining quantity extension

**Rationale**:

1. Existing transform infrastructure handles SampleItem → Specimen
2. Add `specimen.addParent()` for aliquots
3. Add custom extension for `remainingQuantity` (not in base Specimen spec)
4. Maintain existing FHIR persistence pattern

**Alternatives considered**:

- ❌ Create separate Specimen resource for aliquots: Violates FHIR hierarchy
  model
- ❌ Store relationship externally: FHIR Specimen.parent is designed for this
- ✅ **Use Specimen.parent + custom extension for remainingQuantity**

**Code references**:

- [`FhirTransformServiceImpl.java`](../../src/main/java/org/openelisglobal/dataexchange/fhir/service/FhirTransformServiceImpl.java) -
  Lines 1003-1024
- Storage feature FHIR mappings:
  [`StorageLocationFhirTransform.java`](../../src/main/java/org/openelisglobal/storage/fhir/StorageLocationFhirTransform.java) -
  Lines 114-118 (partOf pattern)

---

## 6. Carbon Design System Components

### Search Interface Components

**Component**: `ComboBox` for type-ahead search

**Example from codebase**:

```javascript
// File: frontend/src/components/storage/StorageLocationSelector/QuickFindSearch.jsx
import { ComboBox } from "@carbon/react";

const QuickFindSearch = ({ onLocationSelect, debounceMs = 300 }) => {
  const [searchResults, setSearchResults] = useState([]);

  return (
    <ComboBox
      id="quick-find-location-search"
      placeholder="Search for location..."
      items={searchResults}
      itemToString={(item) => (item ? item.hierarchicalPath : "")}
      onChange={({ selectedItem }) => onLocationSelect(selectedItem)}
      onInputChange={handleInputChange}
    />
  );
};
```

### DataTable for Results

**Component**: `DataTable` with selectable rows

**Example from codebase**:

```javascript
// File: frontend/src/components/admin/testManagementConfigMenu/PanelTestAssign.js
import { DataTable, TableSelectRow, TableSelectAll } from "@carbon/react";

<DataTable rows={panelTestList} headers={tableHeaders}>
  {({ rows, headers, getHeaderProps, getRowProps, selectedRows }) => (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableSelectAll {...getHeaderProps()} />
            {headers.map((header) => (
              <TableHeader key={header.key}>{header.header}</TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.id}>
              <TableSelectRow {...getRowProps({ row })} />
              {row.cells.map((cell) => (
                <TableCell key={cell.id}>{cell.value}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )}
</DataTable>;
```

### Modal for Aliquot Creation

**Component**: `ComposedModal` for forms

**Example from codebase**:

```javascript
// File: frontend/src/components/storage/SampleStorage/LocationManagementModal.jsx
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
} from "@carbon/react";

const AliquotModal = ({ open, onClose, onConfirm }) => {
  return (
    <ComposedModal open={open} onClose={onClose}>
      <ModalHeader title="Create Aliquot" />
      <ModalBody>
        <TextInput
          id="quantity-input"
          labelText="Quantity to Transfer (mL)"
          type="number"
          step="0.001"
          value={quantity}
          onChange={(e) => setQuantity(e.target.value)}
        />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button kind="primary" onClick={onConfirm}>
          Create Aliquot
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};
```

### Hierarchy Display (Tree View)

**Custom Component**: Tree view for hierarchical data

**Example from codebase**:

```javascript
// File: frontend/src/components/storage/StorageDashboard/LocationTreeView.jsx
const renderNode = (node, nodeType, level = 0) => {
  const isExpanded = expandedNodes.has(key);

  return (
    <li style={{ paddingLeft: `${level * 0.75}rem` }}>
      <div className="tree-node-content">
        <button onClick={() => toggleNode(node.id)}>
          {isExpanded ? <ChevronDown /> : <ChevronRight />}
        </button>
        <button onClick={() => handleNodeClick(node)}>{node.name}</button>
      </div>
      {isExpanded && (
        <ul className="tree-children">
          {children.map((child) => renderNode(child, childType, level + 1))}
        </ul>
      )}
    </li>
  );
};
```

### Decision: Carbon Components Selection

**What was chosen**:

1. **Search**: `ComboBox` with type-ahead for accession number search
2. **Results Table**: `DataTable` with `TableSelectRow` for multi-select
3. **Aliquot Form**: `ComposedModal` with `TextInput` for quantity entry
4. **Hierarchy Display**: Custom tree view with Carbon icons (`ChevronRight`,
   `ChevronDown`)
5. **Test Selection**: `MultiSelect` or `ComboBox` for test catalog

**Rationale**:

1. All components from `@carbon/react` v1.15 (matches constitution)
2. Proven patterns from storage feature implementation
3. Accessibility (WCAG 2.1 AA) built into Carbon components
4. React Intl integration for all labels

**Code references**:

- [`QuickFindSearch.jsx`](../../frontend/src/components/storage/StorageLocationSelector/QuickFindSearch.jsx) -
  ComboBox pattern
- [`PanelTestAssign.js`](../../frontend/src/components/admin/testManagementConfigMenu/PanelTestAssign.js) -
  DataTable with selection
- [`LocationManagementModal.jsx`](../../frontend/src/components/storage/SampleStorage/LocationManagementModal.jsx) -
  Modal form pattern
- [`LocationTreeView.jsx`](../../frontend/src/components/storage/StorageDashboard/LocationTreeView.jsx) -
  Tree hierarchy

---

## 7. Concurrent Access Patterns

### Optimistic Locking Implementation

**BaseObject Pattern**:
[`src/main/java/org/openelisglobal/common/valueholder/BaseObject.java`](../../src/main/java/org/openelisglobal/common/valueholder/BaseObject.java)

**Key Finding** (Lines 34-36):

```java
@Column(name = "last_updated")
@Version
private Timestamp lastupdated;
```

**How Optimistic Locking Works**:

1. `@Version` annotation enables Hibernate optimistic locking
2. On update, Hibernate checks if `lastupdated` matches database value
3. If mismatch detected → `OptimisticLockException` thrown
4. Application retries or notifies user of concurrent modification

### Decision: Use @Version for Aliquot Concurrency

**What was chosen**: Leverage existing `BaseObject.lastupdated` field with
`@Version` annotation

**Rationale**:

1. SampleItem extends BaseObject → automatic optimistic locking
2. Prevents lost updates when multiple users aliquot from same parent
3. Transaction isolation + version checking prevents race conditions
4. No additional configuration needed

**Scenario**:

```
Time  User A                          User B                          Database
----  ------------------------------  ------------------------------  -----------
T1    Load SAMPLE001 (remaining=10)   -                               remaining=10
T2    -                               Load SAMPLE001 (remaining=10)   remaining=10
T3    Create aliquot 5mL              -                               -
      Update remaining=5, version++   -                               -
T4    COMMIT (SUCCESS)                -                               remaining=5, version=2
T5    -                               Create aliquot 6mL              -
      -                               Update remaining=4, version++   -
T6    -                               COMMIT (FAIL)                   remaining=5, version=2
      -                               OptimisticLockException thrown  -
```

**Retry Logic Pattern**:

```java
@Service
public class SampleManagementServiceImpl {

    @Transactional
    @Retryable(
        value = OptimisticLockException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public SampleItem createAliquot(String parentId, Double quantity) {
        SampleItem parent = sampleItemDAO.get(parentId);

        // Validate remaining quantity
        if (parent.getRemainingQuantity() < quantity) {
            throw new InsufficientQuantityException(
                "Cannot aliquot: requested " + quantity +
                " exceeds remaining " + parent.getRemainingQuantity());
        }

        // Create aliquot
        SampleItem aliquot = new SampleItem();
        aliquot.setOriginalQuantity(quantity);
        aliquot.setRemainingQuantity(quantity);
        aliquot.setParentSampleItem(parent);

        // Update parent (triggers @Version check)
        parent.setRemainingQuantity(parent.getRemainingQuantity() - quantity);
        sampleItemDAO.save(parent);

        // Save aliquot
        sampleItemDAO.save(aliquot);

        return aliquot;
    }
}
```

**Alternatives considered**:

- ❌ Pessimistic locking (`SELECT FOR UPDATE`): Blocks concurrent reads, reduces
  throughput
- ❌ Application-level locking: Requires distributed lock manager
- ✅ **Optimistic locking with retry**: Balance between concurrency and
  consistency

**Code references**:

- [`BaseObject.java`](../../src/main/java/org/openelisglobal/common/valueholder/BaseObject.java) -
  Lines 34-36
- Storage feature uses same pattern for concurrent sample assignments

---

## Summary of Technical Decisions

| Research Area              | Decision                                        | Rationale                                                |
| -------------------------- | ----------------------------------------------- | -------------------------------------------------------- |
| **SampleItem Entity**      | Hybrid XML + annotations for new fields         | Comply with constitution while preserving legacy mapping |
| **Test Ordering**          | Use existing Analysis entity                    | Avoid duplication, FHIR mapping exists                   |
| **Accession Search**       | Extend existing DAO with JOIN FETCH             | Proven pattern, add hierarchy support                    |
| **Nested Aliquoting**      | Self-referential FK + metadata table            | Efficient queries, follows storage pattern               |
| **External ID Generation** | Append ".{sequence}" to parent ID               | Simple, human-readable, unlimited depth                  |
| **FHIR Mapping**           | Extend Specimen transform with parent reference | Standard FHIR pattern for aliquots                       |
| **Carbon Components**      | ComboBox, DataTable, ComposedModal, Tree        | Proven from storage feature                              |
| **Concurrency**            | Optimistic locking (@Version) + retry           | Built-in, high throughput, Constitution-compliant        |

---

## Implementation Guidance

### Critical Paths for Phase 1 (Entities)

1. **Modify SampleItem.java**:

   - Add `@Column` annotations for `originalQuantity`, `remainingQuantity`
   - Add `@ManyToOne` for `parentSampleItem`
   - Add `@OneToMany` for `childAliquots`
   - Keep existing XML mapping for legacy fields

2. **Create SampleItemAliquotRelationship.java**:

   - Full annotation-based entity (no XML)
   - Fields: `id`, `parentSampleItem`, `childSampleItem`, `sequenceNumber`,
     `quantityTransferred`, `fhirUuid`
   - Constraints: Unique on `(parentSampleItem, sequenceNumber)`

3. **Liquibase Changeset**:
   - Add columns: `original_quantity`, `remaining_quantity`,
     `parent_sample_item_id` to `sample_item`
   - Create table: `sample_item_aliquot_relationship`

### Critical Paths for Phase 2 (Services)

1. **SampleManagementService.createAliquot()**:

   - Validate remaining quantity
   - Generate external ID with sequence
   - Update parent remaining quantity
   - Create aliquot with parent reference
   - Create SampleItemAliquotRelationship record
   - Handle OptimisticLockException with retry

2. **SampleManagementService.addTestsToSamples()**:

   - Check for existing tests (duplicate detection)
   - Create Analysis records for each sample/test combination
   - Return list of skipped duplicates

3. **SampleItemService.searchByAccessionNumber()**:
   - Query Sample by accession number
   - Load SampleItems with JOIN FETCH for hierarchy
   - Compile full DTO within transaction (prevent LazyInitializationException)

### Critical Paths for Phase 3 (Controllers)

1. **SampleManagementRestController.search()**:

   - Accept `accessionNumber` query parameter
   - Call `SampleManagementService.searchByAccessionNumber()`
   - Return JSON with hierarchy included

2. **SampleManagementRestController.createAliquot()**:

   - Accept `CreateAliquotForm` with `parentSampleItemId`, `quantityToTransfer`
   - Call `SampleManagementService.createAliquot()`
   - Return aliquot DTO + updated parent remaining quantity

3. **SampleManagementRestController.addTests()**:
   - Accept `AddTestsForm` with `sampleItemIds`, `testIds`
   - Call `SampleManagementService.addTestsToSamples()`
   - Return success count + skipped list

### Critical Paths for Phase 4 (Frontend)

1. **SampleSearch.js** (Carbon `ComboBox`):

   - Debounced search input
   - Display results in DataTable
   - Show parent-child hierarchy in expandable rows

2. **AliquotForm.js** (Carbon `ComposedModal`):

   - Quantity input with validation (0 < qty <= remaining)
   - Display parent info (external ID, remaining quantity)
   - Preview generated aliquot external ID

3. **TestSelector.js** (Carbon `MultiSelect`):
   - Load test catalog filtered by sample type
   - Multi-select for bulk test addition
   - Show duplicate warnings before submission

---

**Research Complete**: All technical unknowns resolved. Ready for Phase 1 (Data
Model Design).
