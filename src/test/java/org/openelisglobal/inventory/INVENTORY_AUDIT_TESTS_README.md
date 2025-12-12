# Inventory Audit Trail Test Suite

## Overview

This test suite validates that inventory entities (InventoryItem, InventoryLot,
InventoryUsage) properly create audit trail entries in the `history` table when
created, updated, or deleted. It also verifies that the
InventoryAuditLogRestController properly transforms audit trail data for UI
consumption.

## Test Classes

### 1. InventoryAuditTrailIntegrationTest

**Location:**
`src/test/java/org/openelisglobal/inventory/service/InventoryAuditTrailIntegrationTest.java`

**Purpose:** Integration tests that verify the audit trail mechanism works
correctly for inventory entities at the service layer.

**Test Coverage:**

#### InventoryItem Audit Trail Tests

- ✅ `testInventoryItem_Insert_CreatesHistoryEntry()` - Verifies INSERT
  operations create history entries
- ✅ `testInventoryItem_Update_CreatesHistoryEntry()` - Verifies UPDATE
  operations create history entries with XML changes

#### InventoryLot Audit Trail Tests

- ✅ `testInventoryLot_Insert_CreatesHistoryEntry()` - Verifies lot creation is
  audited
- ✅ `testInventoryLot_Update_CreatesHistoryEntry()` - Verifies lot updates are
  audited
- ✅ `testInventoryLot_StatusChange_CreatesAuditTrail()` - Verifies status
  changes (ACTIVE→DISPOSED) are captured

#### InventoryUsage Audit Trail Tests

- ✅ `testInventoryUsage_Insert_CreatesHistoryEntry()` - Verifies usage tracking
  creates audit entries

#### Audit Trail Data Transformation Tests

- ✅ `testHistoryTransformation_ParsesXMLChanges()` - Verifies XML change data
  can be parsed
- ✅ `testHistoryTransformation_IncludesTimestampAndUser()` - Verifies metadata
  is captured
- ✅ `testHistoryTransformation_SortsDescendingByTimestamp()` - Verifies
  chronological ordering
- ✅ `testHistoryTracking_CapturesMultipleFieldChanges()` - Verifies multi-field
  updates are tracked

**Key Assertions:**

- History entries are created with correct `activity` type (I/U/D)
- `sysUserId` is tracked for all operations
- Timestamps are properly set
- Changes are captured in XML format
- History is sorted by timestamp descending

---

### 2. InventoryAuditLogRestControllerTest

**Location:**
`src/test/java/org/openelisglobal/inventory/controller/rest/InventoryAuditLogRestControllerTest.java`

**Purpose:** Tests the REST API endpoints that expose audit trail data to the
frontend, verifying proper transformation of History entities into UI-friendly
JSON format.

**Test Coverage:**

#### Inventory Item Audit Log Endpoint Tests

- ✅ `testGetItemAuditTrail_ReturnsAuditLogs()` - GET
  `/rest/inventory/audit-logs/item/{itemId}` returns audit logs
- ✅ `testGetItemAuditTrail_ContainsRequiredFields()` - Verifies response has
  all required fields
- ✅ `testGetItemAuditTrail_ParsesChangesCorrectly()` - Verifies XML changes are
  parsed into structured JSON
- ✅ `testGetItemAuditTrail_IncludesUserName()` - Verifies user information is
  included
- ✅ `testGetItemAuditTrail_NonExistentItem_ReturnsNotFound()` - Verifies 404
  for invalid IDs

#### Inventory Lot Audit Log Endpoint Tests

- ✅ `testGetLotAuditTrail_ReturnsAuditLogs()` - GET
  `/rest/inventory/audit-logs/lot/{lotId}` works
- ✅ `testGetLotAuditTrail_TracksQuantityChanges()` - Verifies quantity
  adjustments are captured
- ✅ `testGetLotAuditTrail_GeneratesSummary()` - Verifies human-readable summary
  is generated

#### Storage Location Audit Log Endpoint Tests

- ✅ `testGetLocationAuditTrail_ReturnsAuditLogs()` - GET
  `/rest/inventory/audit-logs/location/{locationId}` works

#### Audit Log Transformation Tests

- ✅ `testAuditLogTransformation_FormatsFieldNames()` - Verifies camelCase→Title
  Case formatting
- ✅ `testAuditLogTransformation_HandlesMultipleFieldChanges()` - Verifies
  multi-field updates
- ✅ `testAuditLogTransformation_ShowsOldAndNewValues()` - Verifies before/after
  values
- ✅ `testAuditLogTransformation_OrdersByTimestampDescending()` - Verifies
  newest-first ordering
- ✅ `testAuditLogTransformation_IncludesActivityType()` - Verifies activity
  (I/U/D) is present

**Expected JSON Response Format:**

```json
[
  {
    "id": 123,
    "timestamp": "2025-12-12T10:30:00",
    "activity": "U",
    "performedByUser": "testuser",
    "sysUserId": "1",
    "changes": {
      "description": {
        "old": "Original description",
        "new": "Updated description"
      },
      "lowStockThreshold": {
        "old": "10",
        "new": "20"
      }
    },
    "changesXml": "<field name=\"description\">...</field>",
    "summary": "Description: Original description → Updated description, Low Stock Threshold: 10 → 20"
  }
]
```

---

## Running the Tests

### Run All Inventory Audit Tests

```bash
mvn test -Dtest=InventoryAuditTrailIntegrationTest,InventoryAuditLogRestControllerTest
```

### Run Individual Test Class

```bash
# Service layer tests
mvn test -Dtest=InventoryAuditTrailIntegrationTest

# REST controller tests
mvn test -Dtest=InventoryAuditLogRestControllerTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=InventoryAuditTrailIntegrationTest#testInventoryItem_Insert_CreatesHistoryEntry
```

---

## Test Data

Tests use the test dataset: `testdata/inventory-audit-test-data.xml`

**Key Test Data:**

- **Reference Tables:** Pre-registered with `keep_history='Y'`

  - `inventory_item` (ID: 100000)
  - `inventory_lot` (ID: 100001)
  - `inventory_storage_location` (ID: 100002)

- **System User:** `testuser` (ID: 1)
- **Storage Location:** "Audit Test Refrigerator" (ID: 2000)
- **Inventory Items:**
  - "Audit Test Reagent" (ID: 2000) - REAGENT type
  - "Audit Test RDT" (ID: 2001) - RDT type
- **Inventory Lots:**
  - Lot 2000 - Active, 100mL
  - Lot 2001 - In Use, 30mL

---

## How the Audit Trail Works

### 1. **Registration in reference_tables**

For audit tracking to work, the table must be registered in `reference_tables`
with `keep_history='Y'`:

```sql
INSERT INTO reference_tables (name, keep_history, is_hl7_encoded)
VALUES ('INVENTORY_ITEM', 'Y', 'N');
```

This is done via Liquibase migration: `020-register-inventory-audit-trail.xml`

### 2. **Service Layer Audit Hooks**

When you call service methods (`insert()`, `update()`, `delete()`), the
`AuditTrailService` automatically:

- Creates a `History` entry in the `clinlims.history` table
- Records:
  - `reference_id`: The entity ID
  - `reference_table`: The table ID from `reference_tables`
  - `activity`: 'I' (INSERT), 'U' (UPDATE), or 'D' (DELETE)
  - `sys_user_id`: The user performing the action
  - `timestamp`: When the change occurred
  - `changes`: XML representation of field changes (for UPDATE)

### 3. **Change Tracking XML Format**

For UPDATE operations, field changes are stored in XML format:

```xml
<field name="description">
  <old>Original value</old>
  <new>New value</new>
</field>
<field name="lowStockThreshold">
  <old>10</old>
  <new>20</new>
</field>
```

### 4. **REST API Transformation**

The `InventoryAuditLogRestController` transforms raw `History` entries into
UI-friendly JSON:

- Parses XML changes into structured `Map<String, Map<String, String>>`
- Resolves user IDs to usernames
- Generates human-readable summaries
- Formats field names (camelCase → Title Case)
- Sorts by timestamp descending (newest first)

---

## Expected Test Results

When you run the tests, you should see:

- ✅ **Service layer tests:** Verify audit trail entries are created in the
  database
- ✅ **REST controller tests:** Verify API returns properly formatted audit logs

### Success Criteria

1. All tests pass (green)
2. No compilation errors
3. Audit trail entries are created for CREATE, UPDATE operations
4. User tracking works correctly
5. XML change data is properly parsed
6. REST API returns well-structured JSON

### Common Issues to Watch For

- **No history entries created:** Check that `reference_tables` has
  `keep_history='Y'`
- **User ID is null:** Ensure `setSysUserId()` is called before service
  operations
- **XML parsing fails:** Verify changes are in correct XML format
- **404 responses:** Ensure test data IDs match the dataset

---

## Integration with Frontend

The audit log endpoints are consumed by the React frontend component:
`AuditLogViewer.jsx`

**Frontend Usage:**

```javascript
// Fetch audit logs for an inventory item
fetch(`/rest/inventory/audit-logs/item/${itemId}`)
  .then((res) => res.json())
  .then((logs) => {
    // logs is an array of audit trail entries
    logs.forEach((log) => {
      console.log(`${log.timestamp}: ${log.summary}`);
      console.log(`Performed by: ${log.performedByUser}`);
      console.log("Changes:", log.changes);
    });
  });
```

---

## Maintenance

### Adding Audit Trail to New Inventory Entities

If you add a new inventory entity (e.g., `InventoryTransaction`):

1. **Register in reference_tables** (Liquibase migration):

```xml
<insert schemaName="clinlims" tableName="reference_tables">
    <column name="id" valueSequenceNext="reference_tables_seq"/>
    <column name="name" value="INVENTORY_TRANSACTION"/>
    <column name="keep_history" value="Y"/>
    <column name="is_hl7_encoded" value="N"/>
</insert>
```

2. **Add REST endpoint** in `InventoryAuditLogRestController`:

```java
@GetMapping(value = "/transaction/{transactionId}", produces = MediaType.APPLICATION_JSON_VALUE)
public ResponseEntity<List<Map<String, Object>>> getTransactionAuditTrail(@PathVariable Long transactionId) {
    // ... similar pattern to getItemAuditTrail()
}
```

3. **Add tests** following the patterns in these test classes

---

## Related Files

- **Controller:**
  `src/main/java/org/openelisglobal/inventory/controller/rest/InventoryAuditLogRestController.java`
- **Audit Service:**
  `src/main/java/org/openelisglobal/audittrail/dao/AuditTrailService.java`
- **History Service:**
  `src/main/java/org/openelisglobal/history/service/HistoryService.java`
- **Liquibase Migration:**
  `src/main/resources/liquibase/3.3.x.x/020-register-inventory-audit-trail.xml`
- **Frontend Component:** `frontend/src/components/inventory/AuditLogViewer.jsx`

---

## Questions or Issues?

If tests fail or audit trail doesn't work as expected:

1. Check the `clinlims.history` table has entries:
   `SELECT * FROM clinlims.history ORDER BY timestamp DESC LIMIT 10;`
2. Verify `reference_tables` registration:
   `SELECT * FROM clinlims.reference_tables WHERE name ILIKE '%inventory%';`
3. Enable debug logging in `AuditTrailServiceImpl` to see audit trail operations
4. Review test output for specific assertion failures

**Last Updated:** 2025-12-12
