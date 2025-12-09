package org.openelisglobal.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.EntityType;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.OperationType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;

public interface InventoryAuditService {

    /**
     * Log an inventory operation with full before/after state
     */
    void logOperation(OperationType operationType, EntityType entityType, Long entityId, Object beforeState,
            Object afterState, String operationDetails, String sysUserId);

    /**
     * Log item creation
     */
    void logItemCreate(InventoryItem item, String sysUserId);

    /**
     * Log item update
     */
    void logItemUpdate(InventoryItem before, InventoryItem after, String sysUserId);

    /**
     * Log item deactivation
     */
    void logItemDeactivate(InventoryItem item, String sysUserId);

    /**
     * Log lot receive
     */
    void logLotReceive(InventoryLot lot, String sysUserId);

    /**
     * Log lot update (generic update)
     */
    void logLotUpdate(InventoryLot before, InventoryLot after, String sysUserId);

    /**
     * Log lot open
     */
    void logLotOpen(InventoryLot before, InventoryLot after, String sysUserId);

    /**
     * Log lot QC status update
     */
    void logLotQCUpdate(InventoryLot before, InventoryLot after, String notes, String sysUserId);

    /**
     * Log lot status update
     */
    void logLotStatusUpdate(InventoryLot before, InventoryLot after, String sysUserId);

    /**
     * Log lot quantity adjustment
     */
    void logLotAdjust(InventoryLot before, InventoryLot after, String reason, String sysUserId);

    /**
     * Log lot disposal
     */
    void logLotDispose(InventoryLot before, InventoryLot after, String reason, String notes, String sysUserId);

    /**
     * Log lot usage/consumption
     */
    void logLotUsage(Long lotId, Double quantityUsed, Long testResultId, Long analysisId, String sysUserId);

    /**
     * Log location operations
     */
    void logLocationCreate(InventoryStorageLocation location, String sysUserId);

    void logLocationUpdate(InventoryStorageLocation before, InventoryStorageLocation after, String sysUserId);

    /**
     * Get audit trail for entity
     */
    List<InventoryAuditLog> getAuditTrail(EntityType entityType, Long entityId);

    /**
     * Get audit trail for item
     */
    List<InventoryAuditLog> getItemAuditTrail(Long itemId);

    /**
     * Get audit trail for lot
     */
    List<InventoryAuditLog> getLotAuditTrail(Long lotId);

    /**
     * Helper to serialize objects to JSON
     */
    String toJson(Object object) throws JsonProcessingException;
}
