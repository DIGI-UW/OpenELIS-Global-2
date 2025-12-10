package org.openelisglobal.inventory.service;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.EntityType;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.OperationType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ItemType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

@Rollback
public class InventoryAuditLogServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private InventoryAuditService auditService;

    @Autowired
    private InventoryItemService itemService;

    @Autowired
    private InventoryLotService lotService;

    @Autowired
    private InventoryStorageLocationService locationService;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-audit-test-data.xml");
    }

    @Test
    public void logItemCreate_shouldCreateAuditLogEntry() {
        // Given
        InventoryItem item = itemService.get(1000L);

        // When
        auditService.logItemCreate(item, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getItemAuditTrail(1000L);
        assertNotNull("Audit logs should not be null", logs);
        assertFalse("Should have at least one audit log", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be ITEM_CREATE", OperationType.ITEM_CREATE, log.getOperationType());
        assertEquals("Entity type should be ITEM", EntityType.ITEM, log.getEntityType());
        assertEquals("Entity ID should be 1000", Long.valueOf(1000L), log.getEntityId());
        assertEquals("Item ID should be denormalized", Long.valueOf(1000L), log.getItemId());
        assertEquals("Item name should be denormalized", "Test Reagent A", log.getItemName());
        assertNotNull("After state should be captured", log.getAfterState());
        assertNull("Before state should be null for create", log.getBeforeState());
    }

    @Test
    public void logItemUpdate_shouldCaptureBeforeAndAfterState() {
        // Given
        InventoryItem before = itemService.get(1000L);
        InventoryItem after = itemService.get(1000L);
        after.setDescription("Updated description");

        // When
        auditService.logItemUpdate(before, after, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getItemAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be ITEM_UPDATE", OperationType.ITEM_UPDATE, log.getOperationType());
        assertNotNull("Before state should be captured", log.getBeforeState());
        assertNotNull("After state should be captured", log.getAfterState());
        assertTrue("Before state should contain original description",
                log.getBeforeState().contains("Test reagent for unit testing"));
        assertTrue("After state should contain updated description",
                log.getAfterState().contains("Updated description"));
    }

    @Test
    public void logItemDeactivate_shouldLogDeactivation() {
        // Given
        InventoryItem item = itemService.get(1000L);

        // When
        auditService.logItemDeactivate(item, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getItemAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be ITEM_DEACTIVATE", OperationType.ITEM_DEACTIVATE, log.getOperationType());
        assertEquals("Item ID should be set", Long.valueOf(1000L), log.getItemId());
    }

    @Test
    public void logLotReceive_shouldCreateAuditLogEntry() {
        // Given
        InventoryLot lot = lotService.get(1000L);

        // When
        auditService.logLotReceive(lot, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);
        assertNotNull("Audit logs should not be null", logs);
        assertFalse("Should have at least one audit log", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOT_RECEIVE", OperationType.LOT_RECEIVE, log.getOperationType());
        assertEquals("Entity type should be LOT", EntityType.LOT, log.getEntityType());
        assertEquals("Entity ID should be 1000", Long.valueOf(1000L), log.getEntityId());
        assertEquals("Lot ID should be denormalized", Long.valueOf(1000L), log.getLotId());
        assertEquals("Lot number should be denormalized", "LOT-2025-001", log.getLotNumber());
        assertEquals("Item ID should be denormalized", Long.valueOf(1000L), log.getItemId());
        assertNotNull("After state should be captured", log.getAfterState());
    }

    @Test
    public void logLotOpen_shouldCaptureStatusChange() {
        // Given
        InventoryLot before = lotService.get(1000L);
        InventoryLot after = lotService.get(1000L);
        after.setStatus(LotStatus.IN_USE);
        after.setDateOpened(new Timestamp(System.currentTimeMillis()));

        // When
        auditService.logLotOpen(before, after, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOT_OPEN", OperationType.LOT_OPEN, log.getOperationType());
        assertNotNull("Before state should be captured", log.getBeforeState());
        assertNotNull("After state should be captured", log.getAfterState());
        assertTrue("Before state should show ACTIVE status", log.getBeforeState().contains("ACTIVE"));
        assertTrue("After state should show IN_USE status", log.getAfterState().contains("IN_USE"));
    }

    @Test
    public void logLotQCUpdate_shouldRecordQCStatusChange() {
        // Given
        InventoryLot before = lotService.get(1000L);
        InventoryLot after = lotService.get(1000L);
        after.setQcStatus(org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus.FAILED);
        String notes = "Failed QC due to contamination";

        // When
        auditService.logLotQCUpdate(before, after, notes, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOT_QC_UPDATE", OperationType.LOT_QC_UPDATE, log.getOperationType());
        assertNotNull("Operation details should contain notes", log.getOperationDetails());
        assertTrue("Operation details should contain QC notes",
                log.getOperationDetails().contains("Failed QC due to contamination"));
    }

    @Test
    public void logLotStatusUpdate_shouldCaptureStatusChange() {
        // Given
        InventoryLot before = lotService.get(1000L);
        InventoryLot after = lotService.get(1000L);
        after.setStatus(LotStatus.QUARANTINED);

        // When
        auditService.logLotStatusUpdate(before, after, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOT_STATUS_UPDATE", OperationType.LOT_STATUS_UPDATE,
                log.getOperationType());
        assertTrue("Before state should show ACTIVE", log.getBeforeState().contains("ACTIVE"));
        assertTrue("After state should show QUARANTINED", log.getAfterState().contains("QUARANTINED"));
    }

    @Test
    public void logLotAdjust_shouldIncludeReasonInOperationDetails() {
        // Given
        InventoryLot before = lotService.get(1000L);
        InventoryLot after = lotService.get(1000L);
        after.setCurrentQuantity(95.0);
        String reason = "Damaged vials";

        // When
        auditService.logLotAdjust(before, after, reason, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOT_ADJUST", OperationType.LOT_ADJUST, log.getOperationType());
        assertNotNull("Operation details should be present", log.getOperationDetails());
        assertTrue("Operation details should contain reason", log.getOperationDetails().contains("Damaged vials"));
        assertTrue("Operation details should contain quantity change", log.getOperationDetails().contains("-5.0"));
    }

    @Test
    public void logLotDispose_shouldRecordDisposal() {
        // Given
        InventoryLot before = lotService.get(1000L);
        InventoryLot after = lotService.get(1000L);
        after.setStatus(LotStatus.DISPOSED);
        after.setCurrentQuantity(0.0);
        String reason = "Expired";
        String notes = "Past expiration date";

        // When
        auditService.logLotDispose(before, after, reason, notes, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOT_DISPOSE", OperationType.LOT_DISPOSE, log.getOperationType());
        assertNotNull("Operation details should be present", log.getOperationDetails());
        assertTrue("Operation details should contain reason", log.getOperationDetails().contains("Expired"));
        assertTrue("Operation details should contain notes",
                log.getOperationDetails().contains("Past expiration date"));
        assertTrue("After state should show DISPOSED status", log.getAfterState().contains("DISPOSED"));
    }

    @Test
    public void logLotUsage_shouldRecordUsageWithQuantity() {
        // Given
        Long lotId = 1000L;
        Double quantityUsed = 10.0;
        Long testResultId = 5000L;

        // When
        auditService.logLotUsage(lotId, quantityUsed, testResultId, null, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(lotId);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be USAGE_RECORD", OperationType.USAGE_RECORD, log.getOperationType());
        assertEquals("Entity type should be LOT", EntityType.LOT, log.getEntityType());
        assertEquals("Lot ID should be denormalized", lotId, log.getLotId());
        assertNotNull("Operation details should be present", log.getOperationDetails());
        assertTrue("Operation details should contain quantity", log.getOperationDetails().contains("10.0"));
        assertTrue("Operation details should contain test result ID", log.getOperationDetails().contains("5000"));
    }

    @Test
    public void logLocationCreate_shouldCreateAuditLogEntry() {
        // Given
        InventoryStorageLocation location = locationService.get(1000L);

        // When
        auditService.logLocationCreate(location, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getAuditTrail(EntityType.LOCATION, 1000L);
        assertNotNull("Audit logs should not be null", logs);
        assertFalse("Should have at least one audit log", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOCATION_CREATE", OperationType.LOCATION_CREATE, log.getOperationType());
        assertEquals("Entity type should be LOCATION", EntityType.LOCATION, log.getEntityType());
        assertEquals("Location ID should be denormalized", Long.valueOf(1000L), log.getLocationId());
        assertNotNull("After state should be captured", log.getAfterState());
    }

    @Test
    public void logLocationUpdate_shouldCaptureChanges() {
        // Given
        InventoryStorageLocation before = locationService.get(1000L);
        InventoryStorageLocation after = locationService.get(1000L);
        after.setName("Updated Refrigerator Name");

        // When
        auditService.logLocationUpdate(before, after, "1");

        // Then
        List<InventoryAuditLog> logs = auditService.getAuditTrail(EntityType.LOCATION, 1000L);
        assertFalse("Should have audit logs", logs.isEmpty());

        InventoryAuditLog log = logs.get(0);
        assertEquals("Operation type should be LOCATION_UPDATE", OperationType.LOCATION_UPDATE, log.getOperationType());
        assertNotNull("Before state should be captured", log.getBeforeState());
        assertNotNull("After state should be captured", log.getAfterState());
        assertTrue("Before state should contain original name", log.getBeforeState().contains("Test Refrigerator 1"));
        assertTrue("After state should contain updated name",
                log.getAfterState().contains("Updated Refrigerator Name"));
    }

    @Test
    public void fullAuditTrail_shouldCaptureCompleteLifecycle() {
        // Given - Create a new item
        InventoryItem newItem = new InventoryItem();
        newItem.setName("Audit Test Item");
        newItem.setItemType(ItemType.REAGENT);
        newItem.setUnits("mL");
        newItem.setIsActive("Y");
        newItem.setStabilityAfterOpening(1);
        newItem.setFhirUuid(java.util.UUID.randomUUID());
        newItem.setSysUserId("1");

        // When - Complete lifecycle
        Long itemId = itemService.insert(newItem); // Triggers logItemCreate

        InventoryItem retrievedItem = itemService.get(itemId);
        retrievedItem.setDescription("Updated in test");
        retrievedItem.setSysUserId("1");
        itemService.update(retrievedItem); // Triggers logItemUpdate

        itemService.deactivateItem(itemId, "1"); // Triggers logItemDeactivate

        // Then - Verify complete audit trail
        List<InventoryAuditLog> auditTrail = auditService.getItemAuditTrail(itemId);

        assertNotNull("Audit trail should not be null", auditTrail);
        assertTrue("Should have at least 3 audit entries", auditTrail.size() >= 3);

        // Verify operations are present (may have duplicates from service
        // implementations)
        boolean hasCreate = auditTrail.stream().anyMatch(log -> log.getOperationType() == OperationType.ITEM_CREATE);
        boolean hasUpdate = auditTrail.stream().anyMatch(log -> log.getOperationType() == OperationType.ITEM_UPDATE);
        boolean hasDeactivate = auditTrail.stream()
                .anyMatch(log -> log.getOperationType() == OperationType.ITEM_DEACTIVATE);

        assertTrue("Should have ITEM_CREATE operation", hasCreate);
        assertTrue("Should have ITEM_UPDATE operation", hasUpdate);
        assertTrue("Should have ITEM_DEACTIVATE operation", hasDeactivate);

        // Verify most recent is deactivate
        assertEquals("Most recent should be deactivate", OperationType.ITEM_DEACTIVATE,
                auditTrail.get(0).getOperationType());

        // Verify all entries have correct item information
        for (InventoryAuditLog log : auditTrail) {
            assertEquals("Item ID should be consistent", itemId, log.getItemId());
            assertEquals("Item name should be denormalized", "Audit Test Item", log.getItemName());
        }
    }

    @Test
    public void getAuditTrail_shouldReturnLogsOrderedByTimestamp() throws Exception {
        // Given
        InventoryItem item = itemService.get(1000L);
        auditService.logItemCreate(item, "1");

        Thread.sleep(10); // Small delay

        InventoryItem updated = itemService.get(1000L);
        updated.setDescription("First update");
        auditService.logItemUpdate(item, updated, "1");

        Thread.sleep(10); // Small delay

        auditService.logItemDeactivate(updated, "1");

        // When
        List<InventoryAuditLog> logs = auditService.getAuditTrail(EntityType.ITEM, 1000L);

        // Then
        assertNotNull("Logs should not be null", logs);
        assertTrue("Should have at least 3 logs", logs.size() >= 3);

        // Verify logs are ordered by timestamp DESC (most recent first)
        for (int i = 0; i < logs.size() - 1; i++) {
            assertTrue("Logs should be ordered by timestamp DESC",
                    logs.get(i).getTimestamp().compareTo(logs.get(i + 1).getTimestamp()) >= 0);
        }
    }

    @Test
    public void getLotAuditTrail_shouldReturnAllLogsForLot() {
        InventoryLot lot = lotService.get(1000L);
        auditService.logLotReceive(lot, "1");
        auditService.logLotUsage(1000L, 5.0, null, null, "1");

        List<InventoryAuditLog> logs = auditService.getLotAuditTrail(1000L);

        assertNotNull("Logs should not be null", logs);
        assertTrue("Should have at least 2 logs", logs.size() >= 2);

        for (InventoryAuditLog log : logs) {
            assertEquals("All logs should have correct lot ID", Long.valueOf(1000L), log.getLotId());
        }
    }

    @Test
    public void auditLogShouldBeImmutable_cannotUpdate() {
        InventoryItem item = itemService.get(1000L);
        auditService.logItemCreate(item, "1");

        List<InventoryAuditLog> logs = auditService.getItemAuditTrail(1000L);
        InventoryAuditLog log = logs.getFirst();

        // Note: @Immutable annotation on entity prevents updates
        String originalAfterState = log.getAfterState();
        log.setAfterState("Modified state");

        List<InventoryAuditLog> refetchedLogs = auditService.getItemAuditTrail(1000L);
        InventoryAuditLog refetchedLog = refetchedLogs.getFirst();

        assertEquals("After state should not have changed", originalAfterState, refetchedLog.getAfterState());
    }
}
