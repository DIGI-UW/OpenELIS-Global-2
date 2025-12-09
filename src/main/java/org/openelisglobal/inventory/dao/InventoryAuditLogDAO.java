package org.openelisglobal.inventory.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.EntityType;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.OperationType;

public interface InventoryAuditLogDAO extends BaseDAO<InventoryAuditLog, Long> {

    /**
     * Get all audit logs for a specific entity
     */
    List<InventoryAuditLog> getByEntity(EntityType entityType, Long entityId);

    /**
     * Get all audit logs for a specific item
     */
    List<InventoryAuditLog> getByItemId(Long itemId);

    /**
     * Get all audit logs for a specific lot
     */
    List<InventoryAuditLog> getByLotId(Long lotId);

    /**
     * Get all audit logs for a specific location
     */
    List<InventoryAuditLog> getByLocationId(Long locationId);

    /**
     * Get all audit logs by operation type
     */
    List<InventoryAuditLog> getByOperationType(OperationType operationType);

    /**
     * Get all audit logs by user
     */
    List<InventoryAuditLog> getByUser(Integer userId);

    /**
     * Get audit logs for entity within date range
     */
    List<InventoryAuditLog> getByEntityAndDateRange(EntityType entityType, Long entityId, java.sql.Timestamp startDate,
            java.sql.Timestamp endDate);
}
