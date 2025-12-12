# Research: Refactoring Inventory Audit to Use Generic AuditTrailService

**Feature**: 001-refactor-inventory-audit **Date**: 2025-12-12 **Researcher**:
AI Agent (Claude)

## Executive Summary

OpenELIS Global has an established Generic Audit Trail framework
(`AuditTrailService`) that uses reflection-based field comparison and XML
storage in a unified `history` table. The current inventory management feature
implemented a custom parallel audit system (`InventoryAuditService`) with JSON
storage in `inventory_audit_log`. This research analyzes both approaches and
recommends a refactoring strategy.

---

## 1. Generic Audit Trail Framework Architecture

### Core Components

**AuditTrailService Interface**:
`src/main/java/org/openelisglobal/audittrail/dao/AuditTrailService.java`

- `saveNewHistory(BaseObject, String sysUserId, String tableName)` - Logs INSERT
  operations
- `saveHistory(BaseObject new, BaseObject old, String sysUserId, String event, String tableName)` -
  Logs UPDATE/DELETE

**Implementation**:
`src/main/java/org/openelisglobal/audittrail/daoimpl/AuditTrailServiceImpl.java`
(1433 lines)

- Reflection-based field comparison
- Automatic change detection for all non-transient fields
- XML serialization of before/after values
- Support for custom audit methods (e.g., `getFieldName_Audit()`)
- Handles lazy-loaded relationships gracefully

### Storage Schema

**History Table** (`clinlims.history`):

```sql
CREATE TABLE history (
    id VARCHAR(36) PRIMARY KEY,
    reference_id VARCHAR(255),       -- Entity ID being audited
    reference_table VARCHAR(36),     -- FK to reference_tables.id
    timestamp TIMESTAMP,
    activity CHAR(1),                -- 'I', 'U', 'D'
    changes BYTEA,                   -- XML blob of field changes
    sys_user_id VARCHAR(36)
);
```

**Reference Tables Registry** (`clinlims.reference_tables`):

```sql
CREATE TABLE reference_tables (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) UNIQUE,        -- Uppercase table name
    keep_history CHAR(1),            -- 'Y' or 'N'
    is_hl7_encoded CHAR(1)
);
```

### Integration Pattern

Services extend `AuditableBaseObjectServiceImpl` and enable auditing via
constructor:

```java
public class AnalysisServiceImpl extends AuditableBaseObjectServiceImpl<Analysis, Long> {
    public AnalysisServiceImpl() {
        super(Analysis.class);
        this.auditTrailLog = true;  // Automatic audit on insert/update/delete
    }
}
```

Automatic audit logging occurs in base class methods (lines 28-110 of
AuditableBaseObjectServiceImpl.java):

- `insert()` → `saveNewHistory()`
- `update()` → `saveHistory()` with event='U'
- `delete()` → `saveHistory()` with event='D'

### Change Detection Mechanism

**Reflection-Based Comparison** (AuditTrailServiceImpl.java lines 264-497):

1. Get all fields via `getAllFields()` (includes superclass fields)
2. Skip transient, static, final, and system fields (id, sysUserId, lastupdated)
3. Compare `toString()` values between old and new objects
4. Handle lazy-loaded collections (catch LazyInitializationException)
5. Support custom audit methods for complex fields
6. Generate XML only if changes detected

**XML Format** (example):

```xml
<currentQuantity>100</currentQuantity>
<qcStatus>PENDING</qcStatus>
<notes>Initial receipt</notes>
```

### Querying Audit Trail

**HistoryService**
(`src/main/java/org/openelisglobal/history/service/HistoryService.java`):

```java
List<History> getHistoryByRefIdAndRefTableId(String entityId, String referenceTableId);
```

**Usage Pattern**:

```java
// 1. Get reference table ID
ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");

// 2. Query history
List<History> auditTrail = historyService.getHistoryByRefIdAndRefTableId(
    itemId.toString(),
    refTable.getId()
);
```

---

## 2. Current Inventory Audit Implementation

### Custom Architecture

**InventoryAuditService**:
`src/main/java/org/openelisglobal/inventory/service/InventoryAuditServiceImpl.java`
(336 lines)

- JSON serialization (Jackson ObjectMapper)
- Explicit before/after snapshots
- Custom operation types (ITEM_CREATE, LOT_RECEIVE, LOT_ADJUST, LOT_DISPOSE,
  etc.)
- Denormalized query fields for performance

**Storage Schema** (`inventory_audit_log`):

```sql
CREATE TABLE inventory_audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP,
    performed_by_user INTEGER,
    operation_type VARCHAR(50),      -- Granular: ITEM_CREATE, LOT_RECEIVE, etc.
    entity_type VARCHAR(50),         -- ITEM, LOT, LOCATION
    entity_id BIGINT,
    before_state TEXT,               -- JSON
    after_state TEXT,                -- JSON
    operation_details TEXT,          -- JSON metadata (reason, notes)
    -- Denormalized for efficient queries
    item_id BIGINT,
    lot_id BIGINT,
    location_id BIGINT,
    lot_number VARCHAR(100),
    item_name VARCHAR(255)
);
```

### Operation Types

**Inventory-Specific Operations** (beyond simple CRUD):

- `ITEM_CREATE`, `ITEM_UPDATE`, `ITEM_DEACTIVATE`
- `LOT_RECEIVE` (initial receipt with quantity)
- `LOT_OPEN` (opening sealed lot, sets shelf life)
- `LOT_ADJUST` (manual quantity adjustment with reason)
- `LOT_QC_UPDATE` (QC status change with notes)
- `LOT_STATUS_UPDATE` (workflow status change)
- `LOT_DISPOSE` (disposal with reason and notes)
- `USAGE_RECORD` (consumption tied to test results)
- `LOCATION_CREATE`, `LOCATION_UPDATE`

### Integration Pattern

Manual logging in service methods:

```java
@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, Long> {
    @Autowired
    private InventoryAuditService auditService;

    // NOTE: auditTrailLog NOT enabled (bypasses generic audit)

    @Override
    public Long insert(InventoryItem item) {
        Long result = super.insert(item);
        auditService.logItemCreate(item, item.getSysUserId());  // Manual call
        return result;
    }

    @Override
    public InventoryItem update(InventoryItem item) {
        InventoryItem before = get(item.getId());
        InventoryItem result = super.update(item);
        auditService.logItemUpdate(before, result, item.getSysUserId());
        return result;
    }
}
```

### Querying Pattern

**Optimized Queries** (InventoryAuditLogDAO):

```java
List<InventoryAuditLog> getByItemId(Long itemId);           // Uses denormalized item_id
List<InventoryAuditLog> getByLotId(Long lotId);             // Uses denormalized lot_id
List<InventoryAuditLog> getByEntity(EntityType, Long id);    // Generic lookup
```

---

## 3. Comparative Analysis

| Dimension              | Generic AuditTrailService                         | Current InventoryAuditService              |
| ---------------------- | ------------------------------------------------- | ------------------------------------------ |
| **Storage**            | `history` table (XML)                             | `inventory_audit_log` (JSON)               |
| **Registration**       | Requires `reference_tables` entry                 | Direct table creation                      |
| **Activity Codes**     | 3 codes: 'I', 'U', 'D'                            | 10+ codes: ITEM_CREATE, LOT_RECEIVE, etc.  |
| **Change Detection**   | Automatic reflection-based diff                   | Manual before/after snapshots              |
| **Format**             | XML (byte[])                                      | JSON (TEXT)                                |
| **Performance**        | Indexed by (ref_table, ref_id)                    | Denormalized (item_id, lot_id, lot_number) |
| **Integration**        | Automatic via `auditTrailLog = true`              | Manual `auditService.log*()` calls         |
| **Operation Context**  | Field-level changes only                          | Rich metadata (reason, notes, quantities)  |
| **Query Method**       | `historyService.getHistoryByRefIdAndRefTableId()` | `auditLogDAO.getByItemId()`                |
| **Null User Handling** | Throws exception                                  | Gracefully handles null (sets to null)     |

---

## 4. Decision: Refactoring Strategy

### Recommendation: Enable Generic Audit, Deprecate Custom Service

**Rationale**:

1. **Consistency**: All features should use the same audit mechanism for unified
   reporting
2. **Maintainability**: Reduces code duplication (336 lines of custom audit
   logic)
3. **Standards**: Follows established OpenELIS patterns (20+ services already
   use generic audit)
4. **Simplicity**: Automatic change tracking requires less manual intervention

### Approach

**Phase 1: Register Inventory Tables**

Create Liquibase changeset:

```xml
<changeSet id="001-register-inventory-audit-trail" author="refactor-team">
    <insert tableName="reference_tables">
        <column name="id" valueSequenceNext="reference_tables_seq"/>
        <column name="name" value="INVENTORY_ITEM"/>
        <column name="keep_history" value="Y"/>
        <column name="is_hl7_encoded" value="N"/>
    </insert>
    <insert tableName="reference_tables">
        <column name="id" valueSequenceNext="reference_tables_seq"/>
        <column name="name" value="INVENTORY_LOT"/>
        <column name="keep_history" value="Y"/>
        <column name="is_hl7_encoded" value="N"/>
    </insert>
    <insert tableName="reference_tables">
        <column name="id" valueSequenceNext="reference_tables_seq"/>
        <column name="name" value="INVENTORY_STORAGE_LOCATION"/>
        <column name="keep_history" value="Y"/>
        <column name="is_hl7_encoded" value="N"/>
    </insert>
</changeSet>
```

**Phase 2: Enable Generic Audit in Services**

Modify service constructors:

```java
@Service
public class InventoryItemServiceImpl extends AuditableBaseObjectServiceImpl<InventoryItem, Long> {
    public InventoryItemServiceImpl() {
        super(InventoryItem.class);
        this.auditTrailLog = true;  // Enable generic audit
    }

    // Remove manual auditService.logItemCreate() calls
    // Base class handles insert/update/delete automatically
}
```

**Phase 3: Remove Custom Audit Calls**

Delete manual logging:

```java
// REMOVE THIS:
auditService.logItemCreate(item, item.getSysUserId());
auditService.logItemUpdate(before, after, sysUserId);
```

**Phase 4: Deprecate InventoryAuditService**

Mark as deprecated but keep for backward compatibility:

```java
@Deprecated(since = "3.4", forRemoval = true)
@Service
public class InventoryAuditServiceImpl implements InventoryAuditService {
    // Keep for historical data queries
    // No new audit records created
}
```

**Phase 5: Migrate Query Methods**

Update controllers to use HistoryService:

```java
// OLD:
List<InventoryAuditLog> trail = inventoryAuditService.getItemAuditTrail(itemId);

// NEW:
ReferenceTables refTable = referenceTablesService.getReferenceTableByName("INVENTORY_ITEM");
List<History> trail = historyService.getHistoryByRefIdAndRefTableId(
    itemId.toString(),
    refTable.getId()
);
```

### Handling Operation-Specific Context

**Challenge**: Generic audit tracks field changes, but doesn't capture operation
context (e.g., disposal reason, adjustment notes).

**Solution 1: Use operation_details Field**

The custom `InventoryAuditService` stores operation metadata in
`operation_details` as JSON. This can be preserved by:

1. Adding a `notes` or `context` field to inventory entities
2. Storing metadata temporarily during transaction
3. Generic audit captures it as part of field changes

**Solution 2: Dual Logging (Hybrid)**

Keep `InventoryAuditService` for operation-specific metadata, but also enable
generic audit:

```java
@Transactional
public void adjustLotQuantity(Long lotId, Double newQty, String reason, String sysUserId) {
    InventoryLot before = get(lotId);
    before.setCurrentQuantity(newQty);
    update(before);  // Generic audit logs quantity change

    // Optional: Custom audit adds operation context
    Map<String, Object> details = Map.of("reason", reason, "adjustedBy", sysUserId);
    auditService.logOperation(OperationType.LOT_ADJUST, EntityType.LOT, lotId,
        before, get(lotId), toJson(details), sysUserId);
}
```

**Recommendation**: **Solution 1 (Single Audit Source)** for simplicity.
Operation context can be inferred from field changes + comments field.

---

## 5. Migration Path for Historical Data

### Options

**Option A: Leave Historical Data in Place**

- Keep `inventory_audit_log` table as read-only archive
- New audits go to `history` table
- Queries check both sources until migration complete

**Option B: Migrate to History Table**

- Write migration script to convert JSON → XML
- Map operation types to 'I'/'U'/'D' codes
- Preserve timestamps and user IDs
- More complex but achieves full unification

**Recommendation**: **Option A** (Dual Read) for simplicity and safety.
Historical data remains queryable without risk of data loss.

---

## 6. Impact Analysis

### Code Changes Required

**Files to Modify**:

- `InventoryItemServiceImpl.java` - Enable auditTrailLog, remove manual logging
- `InventoryLotServiceImpl.java` - Enable auditTrailLog, remove manual logging
- `InventoryStorageLocationServiceImpl.java` - Enable auditTrailLog, remove
  manual logging
- Controllers using `InventoryAuditService` - Switch to `HistoryService`

**Files to Create**:

- Liquibase changeset for `reference_tables` registration

**Files to Deprecate**:

- `InventoryAuditService.java` (interface)
- `InventoryAuditServiceImpl.java` (implementation)
- `InventoryAuditLog.java` (entity)
- `InventoryAuditLogDAO.java` (DAO)

### Testing Impact

**Unit Tests**: Minimal impact (mock auditing in tests)

**Integration Tests**: Update to verify `history` table instead of
`inventory_audit_log`

**E2E Tests**: No impact (audit trail is backend-only concern)

### Performance Considerations

**Concern**: Denormalized fields (item_id, lot_id, lot_number) improve query
performance.

**Mitigation**: Generic audit uses composite index on (reference_table,
reference_id). Queries like "all changes to lot X" remain efficient:

```sql
-- Efficient with (reference_table, reference_id) index
SELECT * FROM history
WHERE reference_table = :lotTableId
  AND reference_id = :lotId
ORDER BY timestamp DESC;
```

**Benchmark**: Existing services with high audit volume (e.g., `Sample`,
`Analysis`) successfully use generic audit without performance issues.

---

## 7. References

### Key Source Files

| Component                      | File Path                                                                             |
| ------------------------------ | ------------------------------------------------------------------------------------- |
| Generic Audit Service          | `src/main/java/org/openelisglobal/audittrail/daoimpl/AuditTrailServiceImpl.java`      |
| Base Service (Auto-Audit)      | `src/main/java/org/openelisglobal/common/service/AuditableBaseObjectServiceImpl.java` |
| History Entity                 | `src/main/java/org/openelisglobal/audittrail/valueholder/History.java`                |
| History Service                | `src/main/java/org/openelisglobal/history/service/HistoryService.java`                |
| Reference Tables               | `src/main/java/org/openelisglobal/referencetables/valueholder/ReferenceTables.java`   |
| Current Inventory Audit        | `src/main/java/org/openelisglobal/inventory/service/InventoryAuditServiceImpl.java`   |
| Inventory Item Service         | `src/main/java/org/openelisglobal/inventory/service/InventoryItemServiceImpl.java`    |
| Liquibase Registration Example | `src/main/resources/liquibase/3.3.x.x/012-register-freezer-audit-trail.xml`           |

### Example Services Using Generic Audit

1. `AnalysisServiceImpl` - Sets `auditTrailLog = true` in constructor
2. `SampleServiceImpl` - Uses automatic audit via base class
3. `ResultServiceImpl` - Tracks all result changes via generic audit
4. `PatientServiceImpl` - PHI audit trail for compliance

---

## 8. Risks and Mitigation

| Risk                              | Mitigation                                                          |
| --------------------------------- | ------------------------------------------------------------------- |
| **Loss of operation context**     | Add `notes` field to entities for reasons/context                   |
| **XML vs JSON mismatch**          | Accept XML format (industry standard for OpenELIS)                  |
| **Query performance degradation** | Benchmark before/after, index optimization if needed                |
| **Historical data inaccessible**  | Keep `inventory_audit_log` as read-only archive                     |
| **Null user ID handling**         | Test edge cases, update code to prevent null sysUserId              |
| **LazyInitializationException**   | Ensure services compile all data before audit (per Constitution IV) |

---

## 9. Success Metrics

**Post-Refactoring Validation**:

1. All inventory operations create `history` records (verify via integration
   tests)
2. Zero new records in `inventory_audit_log` table
3. Audit queries execute within 10% of current performance
4. All unit tests pass (100% success rate)
5. Code review confirms `InventoryAuditService` calls removed

---

## 10. Recommendation Summary

**Decision**: **Refactor inventory audit to use Generic AuditTrailService**

**Key Actions**:

1. Register inventory tables in `reference_tables` via Liquibase
2. Enable `auditTrailLog = true` in all inventory service constructors
3. Remove manual `InventoryAuditService` logging calls
4. Update controllers to query `history` table via `HistoryService`
5. Deprecate `InventoryAuditService` (keep for backward compatibility)
6. Keep `inventory_audit_log` as read-only historical archive

**Rationale**: Achieves consistency with existing OpenELIS architecture, reduces
maintenance burden, and follows established patterns used by 20+ other services.
