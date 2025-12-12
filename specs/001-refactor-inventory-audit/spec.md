# Feature Specification: Refactor Inventory Audit to Use Generic AuditTrailService

**Feature Branch**: `001-refactor-inventory-audit` **Created**: 2025-12-12
**Status**: Draft **Input**: User description: "We have an existing Generic
Audit Trail framework where you just need your service to extend the
AuditTrailService. this was a comment from the code reviewer when they saw the
audit work I had done for the inventory management feature on this branch"

## User Scenarios & Testing _(mandatory)_

### User Story 1 - Consistent Audit Trail Across All Features (Priority: P1)

System administrators and auditors need a consistent audit trail mechanism
across all OpenELIS features to simplify compliance reporting and system
analysis. The inventory audit functionality should use the same generic audit
trail framework as other features in the system.

**Why this priority**: This is the core requirement - establishing consistency
with existing system architecture and eliminating custom duplicate audit logging
code.

**Independent Test**: Can be fully tested by performing inventory operations
(create item, receive lot, adjust quantity) and verifying that audit records
appear in the standard history table with the same structure as other feature
audits, and delivers consistent audit trail reporting across the system.

**Acceptance Scenarios**:

1. **Given** the inventory audit service has been refactored to extend
   AuditTrailService, **When** an inventory item is created, **Then** an audit
   record is created in the standard history table (not a custom
   inventory_audit_log table)
2. **Given** the refactored audit service is in use, **When** a lot is updated,
   **Then** the audit trail captures before/after states using the same XML
   format as other features
3. **Given** multiple inventory operations are performed, **When** an
   administrator views audit reports, **Then** inventory changes appear
   alongside other system changes in the unified audit trail
4. **Given** the system uses the generic AuditTrailService, **When** audit
   queries are run, **Then** the same audit querying mechanisms work for
   inventory as for other features

---

### User Story 2 - Maintain Existing Audit Detail Level (Priority: P2)

Users reviewing inventory audit trails need to see the same level of detail they
currently have (operation type, before/after states, operation details) to
maintain compliance and operational visibility.

**Why this priority**: While switching to the generic framework is critical,
preserving audit detail ensures no regression in functionality for existing
users.

**Independent Test**: Can be tested by performing specific inventory operations
(lot adjustments, QC updates, disposal) and verifying that all relevant details
(quantity changes, reasons, notes) are captured in the generic audit framework.

**Acceptance Scenarios**:

1. **Given** a lot quantity is adjusted from 100 to 85 with reason "Damaged
   units", **When** the audit trail is viewed, **Then** the change shows old
   value (100), new value (85), and reason in the changes column
2. **Given** a lot QC status changes from PENDING to APPROVED with notes,
   **When** the audit record is created, **Then** the notes and status change
   are captured in the XML changes format
3. **Given** a lot is disposed with disposal reason and notes, **When** viewing
   the audit trail, **Then** all disposal details (quantity, reason, notes) are
   preserved
4. **Given** inventory usage is recorded for a test, **When** the audit trail is
   reviewed, **Then** the test result ID and quantity used are visible

---

### User Story 3 - Backward Compatibility for Historical Data (Priority: P3)

System administrators and auditors need to access historical audit data created
by the old InventoryAuditServiceImpl to maintain complete audit history and
compliance with regulatory requirements.

**Why this priority**: Historical data preservation is important but can be
handled separately from the core refactoring through data migration or dual-read
strategies.

**Independent Test**: Can be tested by querying audit trails for inventory
items/lots that existed before the refactoring and verifying that historical
records remain accessible.

**Acceptance Scenarios**:

1. **Given** inventory audit logs exist in the old inventory_audit_log table,
   **When** a user views an item's complete audit history, **Then** both old and
   new audit records are displayed
2. **Given** historical audit data needs to be reported, **When** generating
   compliance reports, **Then** the report includes both legacy and new audit
   format data
3. **Given** the refactoring is complete, **When** new audit records are
   created, **Then** they use only the generic history table

---

### Edge Cases

- What happens when the generic AuditTrailService encounters an InventoryLot
  object with complex nested relationships (item, storage location)?
- How does the system handle denormalized fields (lotId, itemId, locationId)
  that were used for efficient querying in the custom implementation?
- What happens if audit logging fails during a transactional inventory
  operation?
- How are inventory-specific operation types (LOT_RECEIVE, LOT_ADJUST,
  LOT_DISPOSE) mapped to the generic audit framework's activity codes?
- What happens during the transition period when both old and new audit records
  exist?

## Requirements _(mandatory)_

### Functional Requirements

- **FR-001**: Inventory services MUST extend or use the generic
  AuditTrailService instead of the custom InventoryAuditServiceImpl
- **FR-002**: System MUST capture the same audit information (before state,
  after state, operation details) as the current implementation
- **FR-003**: System MUST store inventory audit records in the standard history
  table used by other features
- **FR-004**: System MUST support all current inventory operation types
  (ITEM_CREATE, ITEM_UPDATE, LOT_RECEIVE, LOT_ADJUST, LOT_DISPOSE,
  LOT_QC_UPDATE, USAGE_RECORD, LOCATION_CREATE, LOCATION_UPDATE)
- **FR-005**: System MUST maintain the ability to query audit trails by item ID,
  lot ID, or location ID
- **FR-006**: System MUST preserve audit details like adjustment reasons,
  disposal notes, and QC status changes
- **FR-007**: System MUST handle null or empty user IDs gracefully (as current
  implementation does)
- **FR-008**: Audit logging failures MUST NOT break inventory operations
  (defensive logging)
- **FR-009**: System MUST maintain transaction boundaries - audit logs created
  within the same transaction as inventory changes
- **FR-010**: System MUST support generating JSON representations of
  before/after states for complex objects

### Constitution Compliance Requirements (OpenELIS Global 3.0)

_Derived from `.specify/memory/constitution.md` - include only relevant
principles for this feature:_

- **CR-001**: Backend MUST follow 5-layer architecture
  (Valueholder→DAO→Service→Controller→Form)
- **CR-002**: Services MUST use @Transactional annotation for transaction
  management
- **CR-003**: Database changes MUST use Liquibase changesets (if schema changes
  needed for migration)
- **CR-004**: Security: Audit trail MUST capture sys_user_id for all operations
- **CR-005**: Tests MUST be included (unit + integration tests for refactored
  audit service)
- **CR-006**: Code MUST follow existing patterns established by
  AuditTrailService framework

### Key Entities _(include if feature involves data)_

- **History**: The generic audit trail entity that stores all system audit
  records, including reference table ID, reference ID, activity type, changes
  (XML), timestamp, and sys_user_id
- **InventoryItem**: Inventory item entity whose create/update/deactivate
  operations need audit logging
- **InventoryLot**: Lot entity whose receive/update/adjust/dispose/QC/usage
  operations need audit logging
- **InventoryStorageLocation**: Storage location entity whose create/update
  operations need audit logging
- **ReferenceTables**: Maps table names to reference table IDs for the generic
  audit framework
- **AuditTrailService**: The generic audit service interface that provides
  saveNewHistory() and saveHistory() methods

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: All inventory operations create audit records in the standard
  history table (0% in custom inventory_audit_log table for new operations)
- **SC-002**: Audit trail queries execute in comparable time to current
  implementation (within 10% performance variance)
- **SC-003**: All existing inventory audit unit tests pass with the refactored
  implementation (100% test pass rate)
- **SC-004**: Code review confirms elimination of duplicate audit logging code
  (InventoryAuditServiceImpl removed or deprecated)
- **SC-005**: Audit reports show inventory changes integrated with other system
  changes in unified view
- **SC-006**: Historical audit data remains accessible through migration
  strategy or backward-compatible queries
- **SC-007**: No regression in audit detail - all operation-specific information
  (reasons, notes, quantities) is preserved in the generic framework

## Assumptions

- The generic AuditTrailService
  (src/main/java/org/openelisglobal/audittrail/daoimpl/AuditTrailServiceImpl.java)
  supports capturing complex object state changes via reflection
- The XML format used by the generic framework can accommodate all
  inventory-specific audit details
- The reference_tables entry for inventory entities exists or can be created
- Backward compatibility with existing inventory_audit_log data can be handled
  through a migration script or dual-read approach
- The generic audit framework's performance is acceptable for the volume of
  inventory operations

## Out of Scope

- Modifications to the core AuditTrailService framework itself (using it as-is)
- Real-time audit trail notifications or alerting
- Audit trail data archival or retention policies
- Audit trail UI enhancements beyond standard history reports
- Migration of historical inventory_audit_log data to history table (can be
  separate task)
- Performance optimization of the generic AuditTrailService framework
