# Developer Quickstart: Refactor Inventory Audit to Use Generic AuditTrailService

**Feature**: 001-refactor-inventory-audit **Branch**:
`001-refactor-inventory-audit` **Estimated Time**: 2 days (~9 hours)

## Overview

This refactoring replaces the custom inventory audit trail implementation with
OpenELIS's standard Generic Audit Trail framework. You'll be modifying 3 service
classes, 1 controller, creating 1 Liquibase changeset, and updating tests.

## Prerequisites

- Java 21 installed (`java -version`)
- Maven 3.8+ installed (`mvn -version`)
- Docker + Docker Compose running
- OpenELIS Global 2 development environment set up (see
  [README.md](../../README.md))

## Quick Start

### 1. Checkout Branch

```bash
git checkout 001-refactor-inventory-audit
```

### 2. Review Documentation

Read these documents in order:

1. [spec.md](spec.md) - Feature requirements (10 min)
2. [research.md](research.md) - Generic Audit Trail framework analysis (15 min)
3. [plan.md](plan.md) - Implementation plan (10 min)

**Key Takeaway**: We're switching from custom `InventoryAuditService` (JSON,
manual logging) to Generic `AuditTrailService` (XML, automatic reflection-based
change detection).

### 3. Understand the Pattern

**Before** (Custom Audit):

```java
@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, Long> {
    @Autowired
    private InventoryAuditService auditService;  // Custom audit service

    @Override
    public Long insert(InventoryItem item) {
        Long result = super.insert(item);
        auditService.logItemCreate(item, item.getSysUserId());  // Manual logging
        return result;
    }
}
```

**After** (Generic Audit):

```java
@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, Long> {
    public InventoryItemServiceImpl() {
        super(InventoryItem.class);
        this.auditTrailLog = true;  // Enable automatic audit
    }

    // insert() automatically calls AuditTrailService.saveNewHistory()
    // update() automatically calls AuditTrailService.saveHistory()
}
```

## Implementation Steps

### Step 1: Register Inventory Tables (1 hour)

**Create Liquibase Changeset**:
`src/main/resources/liquibase/3.3.x.x/020-register-inventory-audit-trail.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <changeSet id="register-inventory-item-audit-trail" author="refactor-team">
        <comment>Register inventory_item table for generic audit trail</comment>
        <insert schemaName="clinlims" tableName="reference_tables">
            <column name="id" valueSequenceNext="reference_tables_seq"/>
            <column name="name" value="INVENTORY_ITEM"/>
            <column name="keep_history" value="Y"/>
            <column name="is_hl7_encoded" value="N"/>
        </insert>
    </changeSet>

    <changeSet id="register-inventory-lot-audit-trail" author="refactor-team">
        <comment>Register inventory_lot table for generic audit trail</comment>
        <insert schemaName="clinlims" tableName="reference_tables">
            <column name="id" valueSequenceNext="reference_tables_seq"/>
            <column name="name" value="INVENTORY_LOT"/>
            <column name="keep_history" value="Y"/>
            <column name="is_hl7_encoded" value="N"/>
        </insert>
    </changeSet>

    <changeSet id="register-inventory-storage-location-audit-trail" author="refactor-team">
        <comment>Register inventory_storage_location table for generic audit trail</comment>
        <insert schemaName="clinlims" tableName="reference_tables">
            <column name="id" valueSequenceNext="reference_tables_seq"/>
            <column name="name" value="INVENTORY_STORAGE_LOCATION"/>
            <column name="keep_history" value="Y"/>
            <column name="is_hl7_encoded" value="N"/>
        </insert>
    </changeSet>

</databaseChangeLog>
```

**Test Migration**:

```bash
mvn clean install -DskipTests
docker compose -f dev.docker-compose.yml up -d --force-recreate oe.openelis.org
```

**Verify**:

```bash
docker exec -it openelisglobal-database psql -U clinlims -d clinlims \
  -c "SELECT * FROM clinlims.reference_tables WHERE name LIKE 'INVENTORY%';"
```

Expected output: 3 rows (INVENTORY_ITEM, INVENTORY_LOT,
INVENTORY_STORAGE_LOCATION) with `keep_history='Y'`

---

### Step 2: Refactor Services (2 hours)

**File**:
`src/main/java/org/openelisglobal/inventory/service/InventoryItemServiceImpl.java`

**Changes**:

1. Add constructor:

   ```java
   public InventoryItemServiceImpl() {
       super(InventoryItem.class);
       this.auditTrailLog = true;
   }
   ```

2. Remove `@Autowired InventoryAuditService auditService` field

3. Remove manual logging calls:
   ```java
   // DELETE THIS:
   auditService.logItemCreate(item, item.getSysUserId());
   auditService.logItemUpdate(before, after, item.getSysUserId());
   ```

**Repeat for**:

- `InventoryLotServiceImpl.java`
- `InventoryStorageLocationServiceImpl.java`

**File**:
`src/main/java/org/openelisglobal/inventory/service/InventoryAuditService.java`

Add deprecation:

```java
@Deprecated(since = "3.4", forRemoval = true)
public interface InventoryAuditService {
    // Javadoc: This service is deprecated. Use Generic Audit Trail framework instead.
    // Historical data remains accessible via InventoryAuditLogDAO.
}
```

**File**:
`src/main/java/org/openelisglobal/inventory/service/InventoryAuditServiceImpl.java`

Add deprecation:

```java
@Deprecated(since = "3.4", forRemoval = true)
@Service
public class InventoryAuditServiceImpl implements InventoryAuditService {
    // Keep implementation for backward compatibility (historical data queries)
}
```

---

### Step 3: Update Controllers (2 hours)

**File**:
`src/main/java/org/openelisglobal/inventory/controller/InventoryRestController.java`
(or equivalent)

**Add Dependencies**:

```java
@Autowired
private HistoryService historyService;

@Autowired
private ReferenceTablesService referenceTablesService;
```

**Update Audit Query Methods**:

**Before**:

```java
@GetMapping("/audit/item/{id}")
public List<InventoryAuditLog> getItemAuditTrail(@PathVariable Long id) {
    return inventoryAuditService.getItemAuditTrail(id);
}
```

**After**:

```java
@GetMapping("/audit/item/{id}")
public List<AuditTrailDTO> getItemAuditTrail(@PathVariable Long id) {
    // 1. Get reference table ID
    ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");

    // 2. Query history
    List<History> trail = historyService.getHistoryByRefIdAndRefTableId(
        id.toString(),
        refTable.getId()
    );

    // 3. Map to DTOs (parse XML to JSON)
    return trail.stream()
        .map(this::mapHistoryToDTO)
        .collect(Collectors.toList());
}

private AuditTrailDTO mapHistoryToDTO(History history) {
    AuditTrailDTO dto = new AuditTrailDTO();
    dto.setTimestamp(history.getTimestamp());
    dto.setUserId(history.getSysUserId());
    dto.setActivity(mapActivity(history.getActivity()));

    // Parse XML changes field
    if (history.getChanges() != null) {
        String xml = new String(history.getChanges());
        dto.setChanges(parseXmlToMap(xml));
    }

    return dto;
}

private String mapActivity(String code) {
    switch (code) {
        case "I": return "INSERT";
        case "U": return "UPDATE";
        case "D": return "DELETE";
        default: return code;
    }
}

private Map<String, String> parseXmlToMap(String xml) {
    // Simple XML parsing (or use library like Jackson XML)
    Map<String, String> changes = new HashMap<>();
    // Example: <currentQuantity>100</currentQuantity> → {"currentQuantity": "100"}
    Pattern pattern = Pattern.compile("<(\\w+)>([^<]*)</\\1>");
    Matcher matcher = pattern.matcher(xml);
    while (matcher.find()) {
        changes.put(matcher.group(1), matcher.group(2));
    }
    return changes;
}
```

---

### Step 4: Update Tests (3 hours)

**Unit Test**:
`src/test/java/org/openelisglobal/inventory/service/InventoryItemServiceTest.java`

```java
@RunWith(MockitoJUnitRunner.class)
public class InventoryItemServiceTest {

    @Mock
    private InventoryItemDAO dao;

    @Mock
    private AuditTrailService auditTrailService;

    @InjectMocks
    private InventoryItemServiceImpl service;

    @Before
    public void setup() {
        // Enable audit logging
        ReflectionTestUtils.setField(service, "auditTrailLog", true);
    }

    @Test
    public void testInsert_CreatesAuditRecord() {
        // Arrange
        InventoryItem item = new InventoryItem();
        item.setId(1L);
        item.setName("Test Item");
        item.setSysUserId("1");

        when(dao.insert(any())).thenReturn(1L);

        // Act
        service.insert(item);

        // Assert
        verify(auditTrailService).saveNewHistory(eq(item), eq("1"), eq("inventory_item"));
    }

    @Test
    public void testUpdate_CreatesAuditRecord() {
        // Arrange
        InventoryItem before = new InventoryItem();
        before.setId(1L);
        before.setName("Old Name");

        InventoryItem after = new InventoryItem();
        after.setId(1L);
        after.setName("New Name");
        after.setSysUserId("1");

        when(dao.get(1L)).thenReturn(Optional.of(before));
        when(dao.update(any())).thenReturn(after);

        // Act
        service.update(after);

        // Assert
        verify(auditTrailService).saveHistory(
            eq(after),
            eq(before),
            eq("1"),
            eq(IActionConstants.AUDIT_TRAIL_UPDATE),
            eq("inventory_item")
        );
    }
}
```

**Integration Test**:
`src/test/java/org/openelisglobal/inventory/integration/InventoryAuditIntegrationTest.java`

```java
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class InventoryAuditIntegrationTest {

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Test
    public void testInventoryItemInsert_CreatesHistoryRecord() {
        // Arrange
        InventoryItem item = new InventoryItem();
        item.setName("Integration Test Item");
        item.setSysUserId("1");

        // Act
        Long itemId = inventoryItemService.insert(item);

        // Assert
        ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");
        List<History> trail = historyService.getHistoryByRefIdAndRefTableId(
            itemId.toString(),
            refTable.getId()
        );

        assertEquals(1, trail.size());
        assertEquals(IActionConstants.AUDIT_TRAIL_INSERT, trail.get(0).getActivity());
        assertNotNull(trail.get(0).getTimestamp());
        assertEquals("1", trail.get(0).getSysUserId());
    }

    @Test
    public void testInventoryLotUpdate_CapturesChanges() {
        // Arrange
        InventoryLot lot = new InventoryLot();
        lot.setLotNumber("LOT-001");
        lot.setCurrentQuantity(100.0);
        lot.setSysUserId("1");
        Long lotId = inventoryLotService.insert(lot);

        // Act - Update quantity
        InventoryLot updated = inventoryLotService.get(lotId);
        updated.setCurrentQuantity(85.0);
        inventoryLotService.update(updated);

        // Assert - Verify change recorded
        ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_LOT");
        List<History> trail = historyService.getHistoryByRefIdAndRefTableId(
            lotId.toString(),
            refTable.getId()
        );

        assertEquals(2, trail.size());  // 1 INSERT + 1 UPDATE

        History updateRecord = trail.stream()
            .filter(h -> h.getActivity().equals(IActionConstants.AUDIT_TRAIL_UPDATE))
            .findFirst()
            .orElseThrow();

        assertNotNull(updateRecord.getChanges());
        String xml = new String(updateRecord.getChanges());
        assertTrue(xml.contains("currentQuantity"));
        assertTrue(xml.contains("100"));  // Old value captured
    }

    @Test
    public void testNoRecordsInInventoryAuditLog() {
        // Arrange
        InventoryItem item = new InventoryItem();
        item.setName("Test Item");
        item.setSysUserId("1");

        // Act
        inventoryItemService.insert(item);

        // Assert - Verify NO records in old audit table
        List<InventoryAuditLog> oldAuditLogs = inventoryAuditLogDAO.getByItemId(item.getId());
        assertEquals(0, oldAuditLogs.size());  // Custom audit should not be used
    }
}
```

---

### Step 5: Run and Verify (1 hour)

**Run Tests**:

```bash
# Unit tests
mvn test -Dtest=InventoryItemServiceTest
mvn test -Dtest=InventoryLotServiceTest

# Integration tests
mvn test -Dtest=InventoryAuditIntegrationTest

# All tests
mvn clean install
```

**Manual Verification**:

1. Start development environment:

   ```bash
   docker compose -f dev.docker-compose.yml up -d
   ```

2. Create an inventory item via API or UI

3. Query history table:

   ```bash
   docker exec -it openelisglobal-database psql -U clinlims -d clinlims -c "
   SELECT h.id, h.reference_id, rt.name AS table_name, h.activity, h.timestamp
   FROM clinlims.history h
   JOIN clinlims.reference_tables rt ON h.reference_table = rt.id
   WHERE rt.name = 'INVENTORY_ITEM'
   ORDER BY h.timestamp DESC
   LIMIT 5;"
   ```

4. Verify zero new records in inventory_audit_log:
   ```bash
   docker exec -it openelisglobal-database psql -U clinlims -d clinlims -c "
   SELECT COUNT(*) FROM clinlims.inventory_audit_log
   WHERE timestamp > NOW() - INTERVAL '1 hour';"
   ```
   Expected: 0

---

## Troubleshooting

### Issue: "Reference table is null" error

**Cause**: `reference_tables` entries not created (Liquibase changeset didn't
run)

**Fix**:

```bash
# Check if entries exist
docker exec -it openelisglobal-database psql -U clinlims -d clinlims -c "
SELECT * FROM clinlims.reference_tables WHERE name LIKE 'INVENTORY%';"

# If missing, run Liquibase manually
mvn liquibase:update
```

### Issue: Unit tests fail with NullPointerException

**Cause**: `auditTrailLog` not enabled in test

**Fix**: Add to test setup:

```java
@Before
public void setup() {
    ReflectionTestUtils.setField(service, "auditTrailLog", true);
}
```

### Issue: Integration test fails - history table empty

**Cause**: Transaction rolled back before audit record persisted

**Fix**: Ensure service method is `@Transactional` and audit logging happens
within same transaction

### Issue: LazyInitializationException during change detection

**Cause**: Service accessing lazy-loaded relationships after transaction closed

**Fix**: Follow Constitution IV - Services must compile all data within
transaction (use JOIN FETCH in queries)

---

## Performance Benchmarking

**Before Refactoring**:

```bash
# Measure audit query time
docker exec -it openelisglobal-database psql -U clinlims -d clinlims -c "
EXPLAIN ANALYZE
SELECT * FROM clinlims.inventory_audit_log
WHERE item_id = 1;"
```

**After Refactoring**:

```bash
# Measure audit query time
docker exec -it openelisglobal-database psql -U clinlims -d clinlims -c "
EXPLAIN ANALYZE
SELECT h.* FROM clinlims.history h
JOIN clinlims.reference_tables rt ON h.reference_table = rt.id
WHERE rt.name = 'INVENTORY_ITEM' AND h.reference_id = '1';"
```

**Target**: Query time within 10% variance

---

## Code Formatting (MANDATORY)

**Before committing**:

```bash
# Backend formatting
mvn spotless:apply

# Verify no changes
git diff
```

---

## Commit and PR

```bash
# Stage changes
git add .

# Commit
git commit -m "refactor: Use generic AuditTrailService for inventory audit

- Register inventory tables in reference_tables via Liquibase
- Enable auditTrailLog in InventoryItemServiceImpl, InventoryLotServiceImpl, InventoryStorageLocationServiceImpl
- Remove manual InventoryAuditService logging calls
- Update controllers to query history table via HistoryService
- Deprecate InventoryAuditService (keep for backward compatibility)
- Add unit and integration tests

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"

# Push
git push origin 001-refactor-inventory-audit

# Create PR (targeting develop)
gh pr create --title "refactor(001): Use generic AuditTrailService for inventory audit" \
  --body "$(cat <<'EOF'
## Summary
Refactor inventory audit trail to use Generic AuditTrailService framework instead of custom InventoryAuditServiceImpl. Achieves consistency with 20+ other OpenELIS features and reduces code duplication.

## Changes
- ✅ Registered inventory tables in reference_tables
- ✅ Enabled auditTrailLog in all inventory services
- ✅ Removed manual audit logging calls
- ✅ Updated controllers to query history table
- ✅ Deprecated InventoryAuditService
- ✅ All unit and integration tests pass

## Test Results
- Unit tests: ✅ PASS
- Integration tests: ✅ PASS
- Performance: Within 10% variance ✅

## Backward Compatibility
- Historical data in inventory_audit_log remains accessible
- InventoryAuditService kept as @Deprecated

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)" \
  --base develop
```

---

## Success Checklist

- [ ] Liquibase changeset created and tested
- [ ] All 3 services refactored (constructor + removed manual logging)
- [ ] InventoryAuditService deprecated
- [ ] Controllers updated to query history table
- [ ] Unit tests pass (verify audit calls)
- [ ] Integration tests pass (end-to-end verification)
- [ ] Performance within 10% variance
- [ ] Code formatted (`mvn spotless:apply`)
- [ ] PR created targeting `develop`

---

## Resources

- [Feature Specification](spec.md)
- [Research Document](research.md)
- [Implementation Plan](plan.md)
- [Constitution](../../.specify/memory/constitution.md)
- [Testing Roadmap](../../.specify/guides/testing-roadmap.md)
- Example:
  `src/main/java/org/openelisglobal/analysis/service/AnalysisServiceImpl.java`
