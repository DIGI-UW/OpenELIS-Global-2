package org.openelisglobal.coldstorage.dao;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.common.dao.BaseDAO;

public interface CorrectiveActionDAO extends BaseDAO<CorrectiveAction, Long> {

    /**
     * Get all corrective actions for a specific device (freezer).
     *
     * @param freezerId Freezer ID
     * @return List of corrective actions for the device (empty list if none found)
     */
    List<CorrectiveAction> getActionsByFreezerId(Long freezerId);

    /**
     * Get all corrective actions with a specific status.
     *
     * @param status CorrectiveActionStatus enum
     * @return List of corrective actions with the specified status (empty list if
     *         none found)
     */
    List<CorrectiveAction> getActionsByStatus(CorrectiveActionStatus status);

    /**
     * Get all corrective actions within a date range.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of corrective actions (empty list if none found)
     */
    List<CorrectiveAction> getActionsByDateRange(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Get all corrective actions (for dashboard).
     *
     * @return List of all corrective actions ordered by created_at DESC
     */
    List<CorrectiveAction> getAllActions();

    /**
     * Count all pending corrective actions across all devices.
     *
     * @return Count of pending actions (0 if none)
     */
    Long countPendingActions();
}
