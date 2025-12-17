package org.openelisglobal.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.inventory.dao.InventoryAuditLogDAO;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.EntityType;
import org.openelisglobal.inventory.valueholder.InventoryAuditLog.OperationType;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.inventory.valueholder.InventoryStorageLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryAuditServiceImpl implements InventoryAuditService {

    @Autowired
    private InventoryAuditLogDAO auditLogDAO;

    private final ObjectMapper objectMapper;

    public InventoryAuditServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    @Override
    @Transactional
    public void logOperation(OperationType operationType, EntityType entityType, Long entityId, Object beforeState,
            Object afterState, String operationDetails, String sysUserId) {
        try {
            InventoryAuditLog auditLog = new InventoryAuditLog();
            auditLog.setTimestamp(new Timestamp(System.currentTimeMillis()));
            // Only set user ID if it's provided and not null
            if (sysUserId != null && !sysUserId.trim().isEmpty()) {
                try {
                    auditLog.setPerformedByUser(Integer.valueOf(sysUserId));
                } catch (NumberFormatException e) {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "logOperation",
                            "Invalid user ID format: " + sysUserId + ". Audit log will have null user.");
                    auditLog.setPerformedByUser(null);
                }
            } else {
                auditLog.setPerformedByUser(null);
            }
            auditLog.setOperationType(operationType);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);

            // Serialize states to JSON
            if (beforeState != null) {
                auditLog.setBeforeState(toJson(beforeState));
            }
            if (afterState != null) {
                auditLog.setAfterState(toJson(afterState));
            }
            auditLog.setOperationDetails(operationDetails);

            // Populate denormalized fields for quick queries
            populateDenormalizedFields(auditLog, beforeState, afterState);

            // For cases where we don't have state objects, manually set denormalized fields
            if (auditLog.getLotId() == null && entityType == EntityType.LOT) {
                auditLog.setLotId(entityId);
            }
            if (auditLog.getItemId() == null && entityType == EntityType.ITEM) {
                auditLog.setItemId(entityId);
            }
            if (auditLog.getLocationId() == null && entityType == EntityType.LOCATION) {
                auditLog.setLocationId(entityId);
            }

            auditLogDAO.insert(auditLog);

            LogEvent.logInfo(this.getClass().getSimpleName(), "logOperation",
                    "Logged " + operationType + " for " + entityType + " " + entityId);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "logOperation",
                    "Failed to log audit entry: " + e.getMessage());
            LogEvent.logError(e);
            // Don't throw - audit logging failure shouldn't break operations
        }
    }

    @Override
    @Transactional
    public void logItemCreate(InventoryItem item, String sysUserId) {
        logOperation(OperationType.ITEM_CREATE, EntityType.ITEM, item.getId(), null, item, null, sysUserId);
    }

    @Override
    @Transactional
    public void logItemUpdate(InventoryItem before, InventoryItem after, String sysUserId) {
        logOperation(OperationType.ITEM_UPDATE, EntityType.ITEM, after.getId(), before, after, null, sysUserId);
    }

    @Override
    @Transactional
    public void logItemDeactivate(InventoryItem item, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "DEACTIVATE");
        details.put("itemName", item.getName());

        try {
            logOperation(OperationType.ITEM_DEACTIVATE, EntityType.ITEM, item.getId(), null, item, toJson(details),
                    sysUserId);
        } catch (JsonProcessingException e) {
            logOperation(OperationType.ITEM_DEACTIVATE, EntityType.ITEM, item.getId(), null, item,
                    "Deactivated item: " + item.getName(), sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotReceive(InventoryLot lot, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "RECEIVE");
        details.put("lotNumber", lot.getLotNumber());
        details.put("initialQuantity", lot.getInitialQuantity());
        details.put("expirationDate", lot.getExpirationDate());

        try {
            logOperation(OperationType.LOT_RECEIVE, EntityType.LOT, lot.getId(), null, lot, toJson(details), sysUserId);
        } catch (JsonProcessingException e) {
            logOperation(OperationType.LOT_RECEIVE, EntityType.LOT, lot.getId(), null, lot,
                    "Received lot: " + lot.getLotNumber(), sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotUpdate(InventoryLot before, InventoryLot after, String sysUserId) {
        logOperation(OperationType.LOT_UPDATE, EntityType.LOT, after.getId(), before, after, null, sysUserId);
    }

    @Override
    @Transactional
    public void logLotOpen(InventoryLot before, InventoryLot after, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "OPEN");
        details.put("dateOpened", after.getDateOpened());
        details.put("calculatedExpiryAfterOpening", after.getCalculatedExpiryAfterOpening());

        try {
            logOperation(OperationType.LOT_OPEN, EntityType.LOT, after.getId(), before, after, toJson(details),
                    sysUserId);
        } catch (JsonProcessingException e) {
            logOperation(OperationType.LOT_OPEN, EntityType.LOT, after.getId(), before, after,
                    "Opened lot: " + after.getLotNumber(), sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotQCUpdate(InventoryLot before, InventoryLot after, String notes, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "QC_STATUS_UPDATE");
        details.put("oldStatus", before.getQcStatus());
        details.put("newStatus", after.getQcStatus());
        if (notes != null) {
            details.put("notes", notes);
        }

        try {
            logOperation(OperationType.LOT_QC_UPDATE, EntityType.LOT, after.getId(), before, after, toJson(details),
                    sysUserId);
        } catch (JsonProcessingException e) {
            String detailStr = "QC status changed from " + before.getQcStatus() + " to " + after.getQcStatus();
            if (notes != null) {
                detailStr += ". Notes: " + notes;
            }
            logOperation(OperationType.LOT_QC_UPDATE, EntityType.LOT, after.getId(), before, after, detailStr,
                    sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotStatusUpdate(InventoryLot before, InventoryLot after, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "STATUS_UPDATE");
        details.put("oldStatus", before.getStatus());
        details.put("newStatus", after.getStatus());

        try {
            logOperation(OperationType.LOT_STATUS_UPDATE, EntityType.LOT, after.getId(), before, after, toJson(details),
                    sysUserId);
        } catch (JsonProcessingException e) {
            logOperation(OperationType.LOT_STATUS_UPDATE, EntityType.LOT, after.getId(), before, after,
                    "Status changed from " + before.getStatus() + " to " + after.getStatus(), sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotAdjust(InventoryLot before, InventoryLot after, String reason, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "ADJUST_QUANTITY");
        details.put("oldQuantity", before.getCurrentQuantity());
        details.put("newQuantity", after.getCurrentQuantity());
        details.put("quantityChange", after.getCurrentQuantity() - before.getCurrentQuantity());
        if (reason != null) {
            details.put("reason", reason);
        }

        try {
            logOperation(OperationType.LOT_ADJUST, EntityType.LOT, after.getId(), before, after, toJson(details),
                    sysUserId);
        } catch (JsonProcessingException e) {
            String detailStr = "Quantity adjusted from " + before.getCurrentQuantity() + " to "
                    + after.getCurrentQuantity();
            if (reason != null) {
                detailStr += ". Reason: " + reason;
            }
            logOperation(OperationType.LOT_ADJUST, EntityType.LOT, after.getId(), before, after, detailStr, sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotDispose(InventoryLot before, InventoryLot after, String reason, String notes, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "DISPOSE");
        details.put("quantityDisposed", before.getCurrentQuantity());
        if (reason != null) {
            details.put("reason", reason);
        }
        if (notes != null) {
            details.put("notes", notes);
        }

        try {
            logOperation(OperationType.LOT_DISPOSE, EntityType.LOT, after.getId(), before, after, toJson(details),
                    sysUserId);
        } catch (JsonProcessingException e) {
            String detailStr = "Disposed " + before.getCurrentQuantity() + " units";
            if (reason != null) {
                detailStr += ". Reason: " + reason;
            }
            if (notes != null) {
                detailStr += ". Notes: " + notes;
            }
            logOperation(OperationType.LOT_DISPOSE, EntityType.LOT, after.getId(), before, after, detailStr, sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLotUsage(Long lotId, Double quantityUsed, Long testResultId, Long analysisId, String sysUserId) {
        Map<String, Object> details = new HashMap<>();
        details.put("action", "USAGE");
        details.put("quantityUsed", quantityUsed);
        if (testResultId != null) {
            details.put("testResultId", testResultId);
        }
        if (analysisId != null) {
            details.put("analysisId", analysisId);
        }

        try {
            logOperation(OperationType.USAGE_RECORD, EntityType.LOT, lotId, null, null, toJson(details), sysUserId);
        } catch (JsonProcessingException e) {
            logOperation(OperationType.USAGE_RECORD, EntityType.LOT, lotId, null, null,
                    "Used " + quantityUsed + " units", sysUserId);
        }
    }

    @Override
    @Transactional
    public void logLocationCreate(InventoryStorageLocation location, String sysUserId) {
        logOperation(OperationType.LOCATION_CREATE, EntityType.LOCATION, location.getId(), null, location, null,
                sysUserId);
    }

    @Override
    @Transactional
    public void logLocationUpdate(InventoryStorageLocation before, InventoryStorageLocation after, String sysUserId) {
        logOperation(OperationType.LOCATION_UPDATE, EntityType.LOCATION, after.getId(), before, after, null, sysUserId);
    }

    @Override
    public List<InventoryAuditLog> getAuditTrail(EntityType entityType, Long entityId) {
        return auditLogDAO.getByEntity(entityType, entityId);
    }

    @Override
    public List<InventoryAuditLog> getItemAuditTrail(Long itemId) {
        return auditLogDAO.getByItemId(itemId);
    }

    @Override
    public List<InventoryAuditLog> getLotAuditTrail(Long lotId) {
        return auditLogDAO.getByLotId(lotId);
    }

    @Override
    public String toJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Populate denormalized fields for efficient querying
     */
    private void populateDenormalizedFields(InventoryAuditLog auditLog, Object beforeState, Object afterState) {
        Object state = afterState != null ? afterState : beforeState;

        if (state instanceof InventoryLot) {
            InventoryLot lot = (InventoryLot) state;
            auditLog.setLotId(lot.getId());
            auditLog.setLotNumber(lot.getLotNumber());
            if (lot.getInventoryItem() != null) {
                auditLog.setItemId(lot.getInventoryItem().getId());
                auditLog.setItemName(lot.getInventoryItem().getName());
            }
            if (lot.getStorageLocation() != null) {
                auditLog.setLocationId(lot.getStorageLocation().getId());
            }
        } else if (state instanceof InventoryItem) {
            InventoryItem item = (InventoryItem) state;
            auditLog.setItemId(item.getId());
            auditLog.setItemName(item.getName());
        } else if (state instanceof InventoryStorageLocation) {
            InventoryStorageLocation location = (InventoryStorageLocation) state;
            auditLog.setLocationId(location.getId());
        }
    }
}
