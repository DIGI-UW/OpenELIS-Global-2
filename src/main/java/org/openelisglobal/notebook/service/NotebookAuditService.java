package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.notebook.valueholder.NotebookAuditLog;

/**
 * Service interface for NotebookAuditLog operations.
 *
 * <p>
 * Provides methods to create and query audit logs for notebook entities.
 */
public interface NotebookAuditService extends BaseObjectService<NotebookAuditLog, Long> {

    /**
     * Save a new audit log entry for a notebook entity.
     *
     * <p>
     * This method: 1. Generates XML representation of changes using reflection 2.
     * Extracts denormalized fields (status, title, username) 3. Writes to
     * notebook_audit table 4. Optionally dual-writes to history table (during
     * validation period)
     *
     * @param entity    The entity being audited
     * @param tableName The table name (e.g., "notebook", "notebook_entry")
     * @param activity  Activity type: "I" (Insert), "U" (Update), "D" (Delete)
     * @param sysUserId System user ID
     */
    void saveAuditLog(BaseObject<?> entity, String tableName, String activity, String sysUserId);

    /**
     * Save audit log with old entity state (for UPDATE/DELETE operations).
     *
     * @param newEntity New entity state (null for DELETE)
     * @param oldEntity Old entity state (null for INSERT)
     * @param tableName The table name
     * @param activity  Activity type
     * @param sysUserId System user ID
     */
    void saveAuditLog(BaseObject<?> newEntity, BaseObject<?> oldEntity, String tableName, String activity,
            String sysUserId);

    /**
     * Get audit logs for a specific entity.
     *
     * @param entityId   The entity ID
     * @param entityType The entity type (e.g., "NOTEBOOK", "NOTEBOOK_ENTRY")
     * @return List of audit logs
     */
    List<NotebookAuditLog> getAuditLogsForEntity(String entityId, String entityType);

    /**
     * Get ALL audit logs related to a notebook (including all child entities:
     * entries, pages, page samples, etc.).
     *
     * @param notebookId The notebook ID
     * @return List of all audit logs related to this notebook
     */
    List<NotebookAuditLog> getAllAuditLogsForNotebook(String notebookId);

    /**
     * Search audit logs with filters and pagination.
     *
     * <p>
     * Returns a map containing:
     * <ul>
     * <li>logs - List of NotebookAuditLog objects</li>
     * <li>totalRecords - Total count of matching logs</li>
     * <li>offset - Current offset</li>
     * <li>limit - Current limit</li>
     * <li>hasMore - Boolean indicating if more records exist</li>
     * </ul>
     *
     * @param filters Filter criteria map
     * @return Map with logs and pagination metadata
     */
    Map<String, Object> searchAuditLogs(Map<String, Object> filters);

    /**
     * Get recent audit logs across all notebook entities.
     *
     * @param limit Maximum number of results
     * @return List of recent audit logs
     */
    List<NotebookAuditLog> getRecentAuditLogs(int limit);

    /**
     * Get audit log statistics.
     *
     * @return Map of entity type to count
     */
    Map<String, Long> getStatistics();
}
