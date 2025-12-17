# Research: Immunology Laboratory Workflow

**Date**: 2025-12-07 **Feature**: OGC-51 Immunology Laboratory Workflow

## Executive Summary

Research completed to understand existing OpenELIS Global architecture and
identify integration points for the new immunology workflow feature.

---

## 1. Existing NoteBook Entity Structure

### Decision: Extend existing NoteBook/NoteBookPage architecture

### Rationale: NoteBook already supports templates, pages, samples, and files - we add per-sample-per-page tracking

### NoteBook Entity

**File**: `src/main/java/org/openelisglobal/notebook/valueholder/NoteBook.java`

**Key Fields**:

- `id` (Integer, PK) - Sequence: notebook_seq
- `title` (String, VARCHAR 255)
- `type` (Dictionary FK)
- `status` (Enum: DRAFT, SUBMITTED, FINALIZED, LOCKED, ARCHIVED)
- `isTemplate` (Boolean) - Distinguishes templates from entries
- `questionnaireFhirUuid` (UUID) - Links to FHIR questionnaire
- `technician`, `creator` (FK to SystemUser)

**Relationships**:

- `samples` (OneToMany via notebook_samples_list) -> SampleItem entities
- `pages` (OneToMany, CascadeType.ALL) -> NoteBookPage entities
- `files` (OneToMany, CascadeType.ALL) -> NoteBookFile entities
- `comments` (OneToMany, CascadeType.ALL) -> NoteBookComment entities
- `analysers` (ManyToMany via notebook_analysers) -> Analyzer entities

### NoteBookPage Entity

**File**:
`src/main/java/org/openelisglobal/notebook/valueholder/NoteBookPage.java`

**Key Fields**:

- `id` (Integer, PK)
- `order` (Integer) - Page ordering
- `title`, `instructions`, `content` (String, @SafeHtml protected)
- `completed` (Boolean, default: false)
- `sampleTypeId` (Integer FK to type_of_sample)
- `notebook` (ManyToOne -> NoteBook)

**Relationships**:

- `tests` (ElementCollection) -> notebook_page_tests
- `panels` (ElementCollection) -> notebook_page_panels

### NoteBookSample Entity

**File**:
`src/main/java/org/openelisglobal/notebook/valueholder/NoteBookSample.java`

**Key Fields**:

- `notebook` (ManyToOne -> NoteBook)
- `sampleItem` (ManyToOne -> SampleItem)
- `questionnaireResponseUuid` (UUID) - Per-sample questionnaire response

**Gap Identified**: NoteBookSample tracks notebook-level sample assignment but
NOT per-page status. **Solution**: New NotebookPageSample entity for page-level
tracking.

---

## 2. SampleStorageAssignment System

### Decision: Reuse existing storage system with well coordinate support

### Rationale: SampleStorageService already handles location assignment and movement tracking

### SampleStorageAssignment Entity

**File**:
`src/main/java/org/openelisglobal/storage/valueholder/SampleStorageAssignment.java`

**Key Fields**:

- `sampleItemId` (Integer) - Foreign key to SampleItem
- `locationId` (Integer) - ID of location (device, shelf, rack, or box)
- `locationType` (String) - Enum: 'device', 'shelf', 'rack', 'box'
- `positionCoordinate` (String, max 50) - Text coordinate (e.g., "A1", "H12")
- `assignedByUserId`, `assignedDate`, `notes`

**Key Pattern**: One SampleItem -> One current storage assignment (unique
constraint)

### SampleStorageMovement Entity

**File**:
`src/main/java/org/openelisglobal/storage/valueholder/SampleStorageMovement.java`

**Key Fields**:

- `previousLocationId`, `previousLocationType`, `previousPositionCoordinate`
- `newLocationId`, `newLocationType`, `newPositionCoordinate`
- `movedByUserId`, `movementDate`, `reason`

**Key Pattern**: Immutable audit log (@Immutable) - insert only

### StorageBox Entity

**File**: `src/main/java/org/openelisglobal/storage/valueholder/StorageBox.java`

**Key Fields**:

- `rows`, `columns` (Integer) - Grid dimensions
- `type` (String) - e.g., "96-well", "384-well"
- `positionSchemaHint` (String) - Format hint (e.g., "letter-number")
- `getCapacity()` returns rows \* columns

### Well Coordinate System

- Coordinates stored as text strings: "A1", "B12", "H6"
- Format: row letter + column number
- 96-well plate: A-H rows, 1-12 columns
- Row-major order for auto-assignment: A1, A2, ..., A12, B1, B2, ...

### SampleStorageService Methods (to integrate)

- `assignSampleItemWithLocation(sampleItemId, locationId, locationType, positionCoordinate, notes)`
- `moveSampleItemWithLocation(...)` - Creates movement audit record
- `getSampleItemLocation(sampleItemId)` - Returns current location

---

## 3. SampleItem Entity Structure

### Decision: Use existing SampleItem with parent-child support

### Rationale: SampleItem already supports aliquoting relationships and external_id

### SampleItem Entity

**File**:
`src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`

**Key Fields**:

- `sampleItemId` (String) - Unique identifier
- `externalId` (String) - **EXISTS** - For manifest ID storage
- `quantity`, `remainingQuantity` (Double/BigDecimal)
- `collectionDate` (Timestamp)
- `statusId` (String) - Current status
- `fhirUuid` (UUID) - FHIR R4 identifier

### Parent-Child Relationships

- `parentSampleItem` (ManyToOne -> SampleItem) - **EXISTS**
- `childAliquots` (OneToMany) - **EXISTS**
- `isAliquot()` - Check if has parent
- `getNestingLevel()` - Calculate depth

**Confirmation**: SampleItem supports parent-child relationships natively.

---

## 4. Frontend Patterns

### Decision: Use Carbon Design System patterns from existing notebook components

### Rationale: Consistency with NoteBookDashBoard, NoteBookEntryForm implementations

### Key Components to Reuse

**NoteBookDashBoard.js** (723 lines):

- Tile-based metrics display
- FilterableMultiSelect for filtering
- CustomDatePicker for date range
- Pagination with [10, 20, 30, 50, 100] page sizes
- Status Tag component with colors

**NoteBookEntryForm.js** (1,854 lines):

- ContentSwitcher for tab navigation
- Modal for complex dialogs
- FileUploaderDropContainer for file uploads
- Accordion for page management
- SafeHtml protected text areas

**GenericSampleOrderImport.js** (400+ lines):

- FileUploader for CSV/Excel
- Two-phase process: Validate -> Import
- Preview table with configurable headers
- Error table with row/field/message

### Carbon Components for Grid/Bulk Operations

```javascript
// Table with selection
import {
  DataTable,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
} from "@carbon/react";

// Bulk apply form
import { TextInput, Select, SelectItem, Button } from "@carbon/react";

// Status display
import { Tag } from "@carbon/react"; // Types: red, green, blue, gray, cyan, purple

// Pagination
import { Pagination } from "@carbon/react"; // pageSizes={[10, 20, 30, 50, 100]}
```

### Grid Virtualization

- Consider `react-virtualized` or `react-window` for 200+ sample grids
- Carbon DataTable handles sorting/selection
- Virtualization handles scroll performance

---

## 5. API Patterns

### Decision: Follow existing REST controller patterns

### Rationale: Consistency with NoteBookRestController structure

### Base Path Structure

```
/rest/notebook/{notebookId}/samples/...     # Sample entry
/rest/notebook/bulk/page/{pageId}/...       # Bulk operations
/rest/notebook/{notebookId}/samples/route   # Routing
```

### Controller Pattern

```java
@RestController
@RequestMapping("/rest/notebook")
public class NotebookBulkOperationController extends BaseRestController {

    @Autowired
    private NotebookBulkOperationService bulkService;

    @PostMapping("/bulk/page/{pageId}/samples/apply")
    public ResponseEntity<?> applyToSamples(
            @PathVariable Integer pageId,
            @RequestBody BulkApplyForm form) {
        // NO @Transactional here - belongs in service
        return ResponseEntity.ok(bulkService.applyToSamples(pageId, form));
    }
}
```

### Service Pattern

```java
@Service
@Transactional
public class NotebookBulkOperationServiceImpl implements NotebookBulkOperationService {

    // Batch processing pattern
    public BulkOperationResult applyToSamples(Integer pageId, BulkApplyForm form) {
        List<Integer> sampleIds = form.getSampleIds();
        int batchSize = 50;
        int processed = 0;

        for (int i = 0; i < sampleIds.size(); i += batchSize) {
            List<Integer> batch = sampleIds.subList(i, Math.min(i + batchSize, sampleIds.size()));
            processBatch(pageId, batch, form.getData());
            processed += batch.size();
        }

        return new BulkOperationResult(processed, Collections.emptyList());
    }
}
```

---

## 6. Database Migration Strategy

### Decision: Use Liquibase XML format with proper sequencing

### Rationale: Constitution requirement (Principle VI)

### Changeset Naming

- Pattern: `{module}-{sequence}-{description}`
- Example: `notebook-001-page-sample-table`

### Migration Files

```
src/main/resources/liquibase/3.4.x.x/
├── 001-notebook-page-sample.xml
├── 002-analyzer-result-import.xml
└── 003-sample-routing.xml
```

### Example Changeset

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd">

    <changeSet id="notebook-001-page-sample-table" author="openelis">
        <createTable tableName="notebook_page_sample">
            <column name="id" type="SERIAL">
                <constraints primaryKey="true"/>
            </column>
            <column name="notebook_page_id" type="INTEGER">
                <constraints nullable="false"
                    foreignKeyName="fk_nps_page"
                    references="notebook_page(id)"
                    deleteCascade="true"/>
            </column>
            <!-- ... -->
        </createTable>
        <rollback>
            <dropTable tableName="notebook_page_sample"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

---

## 7. Testing Strategy

### Unit Tests (JUnit 4)

```java
@RunWith(MockitoJUnitRunner.class)
public class NotebookBulkOperationServiceTest {
    @Mock
    private NotebookPageSampleDAO pageSampleDAO;

    @InjectMocks
    private NotebookBulkOperationServiceImpl service;

    @Test
    public void testApplyToSamples_BatchesCorrectly() {
        // Arrange
        List<Integer> sampleIds = IntStream.range(1, 151).boxed().collect(Collectors.toList());
        // ... test batch processing in groups of 50
    }
}
```

### Integration Tests (BaseWebContextSensitiveTest)

- Use `BaseWebContextSensitiveTest` (NOT Spring Boot test annotations)
- MockMvc for controller tests
- Real database for DAO tests

### E2E Tests (Cypress)

```javascript
describe("Immunology Workflow - Sample Reception", () => {
  before(() => {
    cy.login("admin", "password");
  });

  it("should import manifest CSV and create samples", () => {
    cy.visit("/notebook/123/workflow");
    cy.get('[data-testid="import-manifest-btn"]').click();
    cy.get('[data-testid="file-upload"]').selectFile("fixtures/manifest.csv");
    // ...
  });
});
```

---

## Alternatives Considered

### 1. Embedded Per-Page Data in NoteBookSample

**Rejected**: Would require complex JSONB queries and lose referential integrity

### 2. Custom Storage System for Well Plates

**Rejected**: Existing StorageBox with positionCoordinate already supports well
coordinates

### 3. Real-Time WebSocket Updates

**Deferred**: Out of scope per spec; use polling for progress updates

### 4. GraphQL API

**Rejected**: REST pattern consistency; existing infrastructure supports REST
