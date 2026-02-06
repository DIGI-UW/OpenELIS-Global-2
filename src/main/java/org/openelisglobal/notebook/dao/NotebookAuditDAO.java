package org.openelisglobal.notebook.dao;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notebook.valueholder.NotebookAuditLog;

/**
 * DAO interface for NotebookAuditLog operations.
 *
 * <p>
 * Provides methods to query audit logs with filtering and pagination support.
 */
public interface NotebookAuditDAO extends BaseDAO<NotebookAuditLog, Long> {

    /**
     * Get audit logs for a specific entity by reference ID and entity type.
     *
     * @param referenceId The ID of the entity (e.g., notebook ID, entry ID)
     * @param entityType  The type of entity (e.g., "NOTEBOOK", "NOTEBOOK_ENTRY")
     * @return List of audit logs ordered by timestamp DESC
     */
    List<NotebookAuditLog> getAuditLogsByEntityId(Long referenceId, String entityType);

    /**
     * Get audit logs for a specific entity by reference ID and reference table.
     *
     * @param referenceId    The ID of the entity
     * @param referenceTable The table reference ID
     * @return List of audit logs ordered by timestamp DESC
     */
    List<NotebookAuditLog> getAuditLogsByReference(Long referenceId, Long referenceTable);

    /**
     * Search audit logs with filtering and pagination.
     *
     * <p>
     * Supported filters (all optional):
     * <ul>
     * <li>entityType (String) - Filter by entity type</li>
     * <li>activity (String) - Filter by activity (I/U/D)</li>
     * <li>sysUserId (String) - Filter by user ID</li>
     * <li>startDate (java.sql.Timestamp) - Filter by start timestamp</li>
     * <li>endDate (java.sql.Timestamp) - Filter by end timestamp</li>
     * <li>statusNew (String) - Filter by new status value</li>
     * </ul>
     *
     * @param filters Map of filter criteria
     * @param offset  Pagination offset (0-based)
     * @param limit   Maximum number of results
     * @return List of audit logs matching filters
     */
    List<NotebookAuditLog> searchAuditLogs(Map<String, Object> filters, int offset, int limit);

    /**
     * Count audit logs matching filter criteria.
     *
     * @param filters Map of filter criteria (same as searchAuditLogs)
     * @return Total count of matching audit logs
     */
    long countAuditLogs(Map<String, Object> filters);

    /**
     * Get all audit logs for a specific user.
     *
     * @param sysUserId The system user ID
     * @param offset    Pagination offset
     * @param limit     Maximum number of results
     * @return List of audit logs for the user
     */
    List<NotebookAuditLog> getAuditLogsByUser(String sysUserId, int offset, int limit);

    /**
     * Get recent audit logs across all notebook entities.
     *
     * @param limit Maximum number of results
     * @return List of most recent audit logs
     */
    List<NotebookAuditLog> getRecentAuditLogs(int limit);

    /**
     * Get audit log statistics grouped by entity type.
     *
     * @return Map of entity type to count
     */
    Map<String, Long> getAuditLogStatistics();
}
