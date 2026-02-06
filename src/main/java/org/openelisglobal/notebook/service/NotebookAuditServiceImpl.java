package org.openelisglobal.notebook.service;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.notebook.dao.NotebookAuditDAO;
import org.openelisglobal.notebook.valueholder.NotebookAuditLog;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for NotebookAuditLog operations.
 *
 * <p>
 * Integrates with existing AuditTrailService for dual-write during validation
 * period.
 */
@Service
public class NotebookAuditServiceImpl extends AuditableBaseObjectServiceImpl<NotebookAuditLog, Long>
        implements NotebookAuditService {

    @Autowired
    protected NotebookAuditDAO baseObjectDAO;

    @Autowired
    @Lazy
    private AuditTrailService auditTrailService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    /**
     * Table name to entity type mapping.
     */
    private static final Map<String, String> TABLE_TO_ENTITY_TYPE = Map.of("notebook", "NOTEBOOK", "notebook_entry",
            "NOTEBOOK_ENTRY", "notebook_page_sample", "NOTEBOOK_PAGE_SAMPLE", "notebook_page", "NOTEBOOK_PAGE",
            "notebook_comment", "NOTEBOOK_COMMENT", "notebook_entry_comment", "NOTEBOOK_ENTRY_COMMENT", "notebook_file",
            "NOTEBOOK_FILE");

    public NotebookAuditServiceImpl() {
        super(NotebookAuditLog.class);
        this.auditTrailLog = false; // Don't audit the audit logs
    }

    @Override
    protected NotebookAuditDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(BaseObject<?> entity, String tableName, String activity, String sysUserId) {
        saveAuditLog(entity, null, tableName, activity, sysUserId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(BaseObject<?> newEntity, BaseObject<?> oldEntity, String tableName, String activity,
            String sysUserId) {
        try {
            NotebookAuditLog auditLog = new NotebookAuditLog();
            String referenceId = newEntity != null ? newEntity.getStringId() : oldEntity.getStringId();
            auditLog.setReferenceId(Long.parseLong(referenceId));

            ReferenceTables refTable = referenceTablesService.getReferenceTableByName(tableName);
            if (refTable != null) {
                auditLog.setReferenceTable(Long.parseLong(refTable.getId()));
            } else {
                LogEvent.logWarn("NotebookAuditService", "saveAuditLog", "Reference table not found for: " + tableName);
                return; // Skip audit logging if reference table not found
            }

            auditLog.setActivity(activity);
            auditLog.setSysUserId(sysUserId);

            Timestamp timestamp = newEntity != null ? newEntity.getLastupdated() : oldEntity.getLastupdated();
            if (timestamp == null) {
                timestamp = new Timestamp(System.currentTimeMillis());
            }
            auditLog.setTimestamp(timestamp);

            String entityType = TABLE_TO_ENTITY_TYPE.getOrDefault(tableName, tableName.toUpperCase());
            auditLog.setEntityType(entityType);

            if (newEntity != null) {
                extractDenormalizedFields(auditLog, newEntity, oldEntity);
            }

            try {
                SystemUser user = systemUserService.get(sysUserId);
                if (user != null) {
                    auditLog.setPerformedByUser(user.getLoginName());
                }
            } catch (Exception e) {
                LogEvent.logWarn("NotebookAuditService", "saveAuditLog",
                        "Could not load username for sysUserId: " + sysUserId);
            }

            String xmlChanges = generateChangesXml(newEntity, oldEntity, activity);
            if (xmlChanges != null && !xmlChanges.isEmpty()) {
                auditLog.setChangesXml(xmlChanges);
            }

            insert(auditLog);
            LogEvent.logInfo(this.getClass().getSimpleName(), "saveAuditLog",
                    "Created " + activity + " audit log for " + tableName + " (ID: " + referenceId + ")");

            // DUAL-WRITE: Also save to history table (during validation period)
            // This can be disabled after validation period by commenting out this block
            // Only attempt dual-write if sysUserId is not null (AuditTrailService requires
            // it)
            if (sysUserId != null && !sysUserId.isEmpty()) {
                try {
                    if ("I".equals(activity)) {
                        auditTrailService.saveNewHistory(newEntity, sysUserId, tableName);
                    } else {
                        auditTrailService.saveHistory(newEntity, oldEntity, sysUserId, activity, tableName);
                    }
                } catch (Exception e) {
                    // Log but don't fail - audit log is primary
                    LogEvent.logWarn("NotebookAuditService", "saveAuditLog",
                            "Dual-write to history table failed: " + e.getMessage());
                }
            } else {
                LogEvent.logDebug("NotebookAuditService", "saveAuditLog",
                        "Skipping dual-write to history table (sysUserId is null)");
            }

        } catch (Exception e) {
            LogEvent.logError(e);
            throw new RuntimeException("Error saving notebook audit log", e);
        }
    }

    /**
     * Extract denormalized fields from entity for fast queries.
     */
    private void extractDenormalizedFields(NotebookAuditLog auditLog, BaseObject<?> newEntity,
            BaseObject<?> oldEntity) {
        try {
            // Extract title (try different field names)
            String title = getFieldValue(newEntity, "title");
            if (title != null) {
                auditLog.setEntityTitle(title);
            }

            // Extract status fields (old and new)
            if (oldEntity != null) {
                String oldStatus = getFieldValue(oldEntity, "status");
                if (oldStatus != null) {
                    auditLog.setStatusOld(oldStatus);
                }
            }

            String newStatus = getFieldValue(newEntity, "status");
            if (newStatus != null) {
                auditLog.setStatusNew(newStatus);
            }
        } catch (Exception e) {
            LogEvent.logWarn("NotebookAuditService", "extractDenormalizedFields",
                    "Could not extract denormalized fields: " + e.getMessage());
        }
    }

    /**
     * Get field value from entity using reflection.
     */
    private String getFieldValue(BaseObject<?> entity, String fieldName) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(entity);
                return value != null ? value.toString() : null;
            }
        } catch (Exception e) {
            // Ignore - field may not exist
        }
        return null;
    }

    /**
     * Find field in class hierarchy.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Generate XML representation of changes.
     */
    private String generateChangesXml(BaseObject<?> newEntity, BaseObject<?> oldEntity, String activity) {
        StringBuilder xml = new StringBuilder();

        try {
            BaseObject<?> sourceEntity = newEntity != null ? newEntity : oldEntity;
            Class<?> entityClass = sourceEntity.getClass();
            Field[] fields = getAllFields(entityClass);

            for (Field field : fields) {
                field.setAccessible(true);

                // Skip transient, final, static fields
                int modifiers = field.getModifiers();
                if (java.lang.reflect.Modifier.isTransient(modifiers) || java.lang.reflect.Modifier.isFinal(modifiers)
                        || java.lang.reflect.Modifier.isStatic(modifiers)) {
                    continue;
                }

                String fieldName = field.getName();

                // Skip certain fields
                if (fieldName.equals("id") || fieldName.equals("sysUserId") || fieldName.equals("systemUser")
                        || fieldName.equals("lastUpdated")) {
                    continue;
                }

                try {
                    Object newValue = newEntity != null ? field.get(newEntity) : null;
                    Object oldValue = oldEntity != null ? field.get(oldEntity) : null;

                    // Only include changed fields for UPDATE
                    if ("U".equals(activity)) {
                        if (newValue == null && oldValue == null) {
                            continue;
                        }
                        if (newValue != null && newValue.equals(oldValue)) {
                            continue;
                        }
                    }

                    // Generate XML field
                    xml.append("<field name=\"").append(escapeXmlAttribute(fieldName)).append("\">");

                    if (oldValue != null) {
                        xml.append("<old>").append(escapeXmlContent(oldValue.toString())).append("</old>");
                    }

                    if (newValue != null) {
                        xml.append("<new>").append(escapeXmlContent(newValue.toString())).append("</new>");
                    }

                    xml.append("</field>");
                } catch (IllegalAccessException e) {
                    // Skip field if can't access
                }
            }
        } catch (Exception e) {
            LogEvent.logError(e);
        }

        return xml.length() > 0 ? xml.toString() : null;
    }

    /**
     * Get all fields including inherited fields.
     */
    private Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(java.util.Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }

        return fields.toArray(new Field[0]);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> getAuditLogsForEntity(String entityId, String entityType) {
        return baseObjectDAO.getAuditLogsByEntityId(Long.parseLong(entityId), entityType);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> searchAuditLogs(Map<String, Object> filters) {
        int offset = filters.containsKey("offset") ? (int) filters.get("offset") : 0;
        int limit = filters.containsKey("limit") ? (int) filters.get("limit") : 100;

        if (limit > 1000) {
            limit = 1000;
        }

        List<NotebookAuditLog> logs = baseObjectDAO.searchAuditLogs(filters, offset, limit);
        long totalRecords = baseObjectDAO.countAuditLogs(filters);

        Map<String, Object> response = new HashMap<>();
        response.put("logs", logs);
        response.put("totalRecords", totalRecords);
        response.put("offset", offset);
        response.put("limit", limit);
        response.put("hasMore", (offset + limit) < totalRecords);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookAuditLog> getRecentAuditLogs(int limit) {
        return baseObjectDAO.getRecentAuditLogs(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        return baseObjectDAO.getAuditLogStatistics();
    }

    /**
     * Escape XML attribute values for safe storage.
     */
    private String escapeXmlAttribute(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Escape XML content for safe storage.
     */
    private String escapeXmlContent(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
