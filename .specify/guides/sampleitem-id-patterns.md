# SampleItem ID Patterns: External ID vs Numeric ID

**Quick Reference Guide** for understanding and working with SampleItem
identifier types in OpenELIS Global 2.

**Related Documentation**:

- [Storage Feature Research - SampleItem Entity Structure](specs/001-sample-storage/research.md#11-sampleitem-entity-structure-and-storage-integration)
- [Storage Feature Data Model - SampleStorageAssignment](specs/001-sample-storage/data-model.md#6-samplestorageassignment)

---

## Overview

OpenELIS uses **two distinct identifier types** for `SampleItem` entities:

1. **Numeric ID** (`id` field) - Primary key, stored as integer in database but
   exposed as String in Java
2. **External ID** (`externalId` field) - User-friendly identifier (e.g.,
   "EXT-1765401458866")

This dual-ID pattern enables user-friendly identifiers while maintaining
database efficiency through integer-based foreign keys.

---

## Entity Structure

### SampleItem Entity

```java
public class SampleItem extends BaseObject<String> {
    private String id;           // Numeric ID (String representation)
    private String externalId;   // External ID (e.g., "EXT-1765401458866")
    // ... other fields
}
```

**Key Characteristics**:

- `id` (String): Primary key, generated via `StringSequenceGenerator` from
  `sample_item_seq`
  - Stored in database as **numeric** but typed as **String** in Java
  - Uses `LIMSStringNumberUserType` for Hibernate mapping
- `externalId` (String): User-facing identifier, optional but typically unique
  in practice

### SampleStorageAssignment Entity

```java
@Entity
public class SampleStorageAssignment extends BaseObject<Integer> {
    @Column(name = "SAMPLE_ITEM_ID", nullable = false, unique = true)
    private Integer sampleItemId;  // Stores numeric ID only (not external ID)
    // ... other fields
}
```

**Critical Constraint**: `SampleStorageAssignment.sampleItemId` stores **numeric
ID only** (Integer), not external ID. This avoids cross-mapping issues between
JPA annotations and HBM XML mapping.

---

## ID Resolution Pattern: `resolveSampleItem()`

**Location**: `SampleStorageServiceImpl.resolveSampleItem(String identifier)`

**Purpose**: Accepts flexible input (accession number, external ID, or numeric
ID) and returns a `SampleItem` entity with full data.

**Implementation Flow**:

```java
private SampleItem resolveSampleItem(String identifier) {
    // Step 1: Try accession number lookup (Sample → SampleItems)
    Sample sample = sampleService.getSampleByAccessionNumber(trimmedId);
    if (sample != null) {
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (sampleItems.size() == 1) {
            return sampleItems.get(0);
        }
        // Multiple SampleItems require external ID specification
    }

    // Step 2: Try external ID lookup (direct SampleItem lookup)
    List<SampleItem> sampleItemsByExtId = sampleItemService.getSampleItemsByExternalID(trimmedId);
    if (sampleItemsByExtId != null && !sampleItemsByExtId.isEmpty()) {
        return sampleItemsByExtId.get(0);
    }

    // Not found
    throw new LIMSRuntimeException("Sample not found with identifier: " + trimmedId);
}
```

**Usage Pattern in Service Methods**:

```java
public Map<String, Object> disposeSampleItem(String sampleItemId, String reason, ...) {
    // ✅ CORRECT: Accept flexible identifier (external ID or accession)
    SampleItem sampleItem = resolveSampleItem(sampleItemId);

    // ✅ CORRECT: Use numeric ID (as String) for DAO lookups
    SampleStorageAssignment assignment = sampleStorageAssignmentDAO
            .findBySampleItemId(sampleItem.getId());

    // ... rest of method
}
```

**Conversion Flow**:

1. **Service method** accepts `sampleItemId` (external ID or accession number)
2. **`resolveSampleItem(identifier)`** → returns `SampleItem` entity
3. **`sampleItem.getId()`** → numeric ID (String representation)
4. **DAO layer** parses String to Integer for database queries

---

## DAO Layer: ID Conversion Requirements

### `SampleStorageAssignmentDAO.findBySampleItemId()`

**Signature**: `findBySampleItemId(String sampleItemId)`

**Requirement**: Must receive **numeric ID** (String that can be parsed to
Integer). External IDs will cause `NumberFormatException`.

```java
@Override
public SampleStorageAssignment findBySampleItemId(String sampleItemId) {
    // ❌ Will fail if sampleItemId is external ID like "EXT-123"
    Integer sampleItemIdInt = Integer.parseInt(sampleItemId.trim());

    String hql = "FROM SampleStorageAssignment ssa WHERE ssa.sampleItemId = :sampleItemId";
    query.setParameter("sampleItemId", sampleItemIdInt);
    // ... query execution
}
```

**Critical Rule**: **Never pass external ID directly to DAO methods**. Always
resolve via `resolveSampleItem()` first, then use `sampleItem.getId()`.

---

## API Response Structure

**Location**: `SampleStorageServiceImpl.getAllSamplesWithAssignments()`

**Response Fields**:

```java
Map<String, Object> map = new HashMap<>();
map.put("id", sampleItem.getId());                    // Numeric ID (String)
map.put("sampleItemId", sampleItem.getId());          // Duplicate (legacy compatibility)
map.put("sampleItemExternalId", sampleItem.getExternalId() != null
    ? sampleItem.getExternalId() : "");               // External ID
```

**Field Mapping**:

- `id`: Numeric ID (String representation, e.g., "12345")
- `sampleItemId`: Duplicate of `id` (legacy compatibility, may be deprecated)
- `sampleItemExternalId`: External ID (e.g., "EXT-1765401458866")

---

## Common Patterns & Anti-Patterns

### ✅ Correct Patterns

**1. Service Methods Accept Flexible Identifiers**

```java
// ✅ ACCEPT: External ID or accession number
public Map<String, Object> disposeSampleItem(String sampleItemId, ...) {
    SampleItem sampleItem = resolveSampleItem(sampleItemId);  // Handles conversion
    // Use sampleItem.getId() for database operations
}
```

**2. Resolve Before DAO Calls**

```java
// ✅ CORRECT: Resolve first, then use numeric ID
SampleItem sampleItem = resolveSampleItem(identifier);
SampleStorageAssignment assignment = assignmentDAO.findBySampleItemId(sampleItem.getId());
```

**3. Test Helpers for ID Conversion**

```java
// ✅ CORRECT: Helper method converts external ID → numeric ID for direct SQL
private int getSampleItemNumericId(String externalId) {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM sample_item WHERE external_id = ?",
        Integer.class, externalId);
}
```

**4. Test Assertions Compare Correct Fields**

```java
// ✅ CORRECT: Compare external ID with external ID field
String sampleItemExternalId = sample.get("sampleItemExternalId").asText();
if (sampleItemId.equals(sampleItemExternalId)) {  // sampleItemId is external ID
    // Match found
}
```

### ❌ Anti-Patterns

**1. Passing External ID Directly to DAO**

```java
// ❌ WRONG: External ID cannot be parsed to Integer
String externalId = "EXT-1765401458866";
SampleStorageAssignment assignment = assignmentDAO.findBySampleItemId(externalId);
// → NumberFormatException!
```

**Fix**: Resolve via `resolveSampleItem()` first:

```java
SampleItem sampleItem = resolveSampleItem(externalId);
SampleStorageAssignment assignment = assignmentDAO.findBySampleItemId(sampleItem.getId());
```

**2. Comparing External ID with Numeric ID Field**

```java
// ❌ WRONG: Comparing external ID with numeric ID field
if (sampleItemId.equals(sample.get("id").asText())) {
    // sampleItemId is "EXT-123", but "id" is "12345" → never matches
}
```

**Fix**: Use `sampleItemExternalId` field:

```java
String sampleItemExternalId = sample.get("sampleItemExternalId").asText();
if (sampleItemId.equals(sampleItemExternalId)) {  // Both are external IDs
    // Match found
}
```

**3. Missing ID Conversion in Test SQL**

```java
// ❌ WRONG: Using external ID in SQL insert
jdbcTemplate.update(
    "INSERT INTO sample_storage_assignment (sample_item_id, ...) VALUES (?, ...)",
    "EXT-123",  // External ID - but column expects integer!
    ...
);
// → NumberFormatException or constraint violation
```

**Fix**: Convert external ID to numeric ID first:

```java
int numericId = getSampleItemNumericId(externalId);
jdbcTemplate.update(
    "INSERT INTO sample_storage_assignment (sample_item_id, ...) VALUES (?, ...)",
    numericId,  // Numeric ID
    ...
);
```

---

## Conversion Points Reference

| Location         | Input                   | Output              | Method                                                        |
| ---------------- | ----------------------- | ------------------- | ------------------------------------------------------------- |
| REST API Input   | External ID / Accession | `SampleItem` entity | `resolveSampleItem()`                                         |
| Service → DAO    | Numeric ID (String)     | Integer             | `Integer.parseInt()`                                          |
| DAO → Database   | Integer                 | Numeric column      | Direct mapping                                                |
| API Response     | Both IDs                | JSON fields         | `map.put("id", ...)` + `map.put("sampleItemExternalId", ...)` |
| Test SQL Inserts | External ID             | Numeric ID          | `getSampleItemNumericId()` helper                             |

---

## Design Rationale

### Why Two ID Types?

1. **User Experience**

   - External IDs: Human-readable (e.g., "EXT-2024-001")
   - Numeric IDs: Opaque sequence numbers (e.g., "12345")

2. **Database Efficiency**

   - Foreign keys use numeric IDs (integer indexes, faster joins)
   - External IDs are not indexed and may change

3. **Legacy Compatibility**
   - `SampleItem.id` is String in Java but numeric in DB
   - Uses `LIMSStringNumberUserType` for Hibernate mapping

### Why Not Use External ID as Primary Key?

- External IDs may not be present for all records
- External IDs can change (not immutable)
- Numeric IDs enable efficient integer-based foreign keys
- Legacy system constraints

---

## Testing Best Practices

### Test Helper Methods

**Standard Pattern for Tests**:

```java
/**
 * Create a test sample item. Returns the external_id which is used to identify
 * the sample item (resolveSampleItem only accepts accession numbers or external IDs).
 */
private String createTestSampleItem() throws Exception {
    String externalId = "EXT-" + System.currentTimeMillis();
    jdbcTemplate.update(
        "INSERT INTO sample_item (id, samp_id, sort_order, status_id, external_id, ...) VALUES (?, ?, ?, ?, ?, ...)",
        10000, sampleId, 1, 1, externalId, ...
    );
    return externalId;  // Return external ID for API calls
}

/**
 * Helper to get the numeric sample_item.id from the external_id. Used for
 * database verification queries.
 */
private int getSampleItemNumericId(String externalId) {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM sample_item WHERE external_id = ?",
        Integer.class, externalId);
}
```

**Usage in Tests**:

```java
@Test
public void testDisposal() throws Exception {
    // Create sample with external ID
    String sampleItemExternalId = createTestSampleItem();

    // Get numeric ID for database verification
    int numericId = getSampleItemNumericId(sampleItemExternalId);

    // API call uses external ID (flexible identifier)
    String requestBody = String.format(
        "{\"sampleItemId\":\"%s\",\"reason\":\"expired\"}",
        sampleItemExternalId);
    mockMvc.perform(post("/rest/storage/sample-items/dispose")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
        .andExpect(status().isOk());

    // Database verification uses numeric ID
    String statusId = jdbcTemplate.queryForObject(
        "SELECT status_id FROM sample_item WHERE id = ?",
        String.class, numericId);
    assertEquals("24", statusId);  // Disposed status
}
```

### API Response Assertions

**Correct Pattern**:

```java
// ✅ CORRECT: Compare external ID with external ID field
JsonNode samples = objectMapper.readTree(result.getResponse().getContentAsString());
for (JsonNode sample : samples) {
    String sampleItemExternalId = sample.has("sampleItemExternalId")
        ? sample.get("sampleItemExternalId").asText() : "";
    if (sampleItemId.equals(sampleItemExternalId)) {  // Both external IDs
        String status = sample.get("status").asText();
        assertEquals("disposed", status);
        break;
    }
}
```

---

## Remediation Checklist for Current Code

Based on recent fixes and identified complexity, verify:

- [ ] All service methods use `resolveSampleItem()` before DAO calls
- [ ] No direct DAO calls with external IDs (must use numeric ID)
- [ ] Test assertions compare external IDs with `sampleItemExternalId` field
      (not `id` field)
- [ ] Test SQL inserts convert external ID to numeric ID via helper method
- [ ] API responses include both `id` (numeric) and `sampleItemExternalId`
      fields
- [ ] Documentation updated to reference this guide

---

## Related Issues & Fixes

**Issue #1**: Test assertion compared external ID with numeric ID field

- **File**: `SampleStorageRestControllerDisposalTest.java`
- **Fix**: Updated to compare with `sampleItemExternalId` field
- **PR**: #2413

**Issue #2**: Direct SQL inserts using external ID

- **File**: `SampleStorageRestControllerDisposalTest.java`
- **Fix**: Added `getSampleItemNumericId()` helper method
- **PR**: #2413

---

## References

- **SampleItem Entity**:
  `src/main/java/org/openelisglobal/sampleitem/valueholder/SampleItem.java`
- **SampleStorageAssignment Entity**:
  `src/main/java/org/openelisglobal/storage/valueholder/SampleStorageAssignment.java`
- **SampleStorageServiceImpl**:
  `src/main/java/org/openelisglobal/storage/service/SampleStorageServiceImpl.java`
- **SampleStorageAssignmentDAO**:
  `src/main/java/org/openelisglobal/storage/dao/SampleStorageAssignmentDAOImpl.java`
- **Hibernate Mapping**: `src/main/resources/hibernate/hbm/SampleItem.hbm.xml`
