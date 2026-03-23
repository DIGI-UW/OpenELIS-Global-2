package org.openelisglobal.alert.dao;

import java.util.List;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.dao.BaseDAO;

public interface AlertDAO extends BaseDAO<Alert, Long> {

    /**
     * Get all alerts for a specific entity (polymorphic query).
     *
     * <p>
     * Examples: - getAlertsByEntity("Freezer", 5L) → Returns all alerts for Freezer
     * ID 5 - getAlertsByEntity("Equipment", 12L) → Returns all alerts for Equipment
     * ID 12 - getAlertsByEntity("Sample", 999L) → Returns all alerts for Sample ID
     * 999
     *
     * @param entityType Entity class name (e.g., "Freezer", "Equipment")
     * @param entityId   Entity ID
     * @return List of alerts for the entity (empty list if none found)
     */
    List<Alert> getAlertsByEntity(String entityType, Long entityId);

    /**
     * Get all alerts of a specific type.
     *
     * <p>
     * Examples: - getAlertsByAlertType(FREEZER_TEMPERATURE) → All temperature
     * alerts - getAlertsByAlertType(EQUIPMENT_FAILURE) → All equipment failure
     * alerts
     *
     * @param alertType Alert type enum
     * @return List of alerts of the specified type (empty list if none found)
     */
    List<Alert> getAlertsByAlertType(AlertType alertType);

    /**
     * Get all alerts with a specific status.
     *
     * <p>
     * Examples: - getAlertsByStatus(OPEN) → All unacknowledged alerts -
     * getAlertsByStatus(ACKNOWLEDGED) → All acknowledged but unresolved alerts -
     * getAlertsByStatus(RESOLVED) → All resolved alerts
     *
     * @param status Alert status enum
     * @return List of alerts with the specified status (empty list if none found)
     */
    List<Alert> getAlertsByStatus(AlertStatus status);

    /**
     * Count active alerts for a specific entity.
     *
     * <p>
     * Active alerts are those with status OPEN or ACKNOWLEDGED (not RESOLVED).
     *
     * <p>
     * Examples: - countActiveAlertsForEntity("Freezer", 5L) → Count of active
     * alerts for Freezer ID 5 - countActiveAlertsForEntity("Equipment", 12L) →
     * Count of active alerts for Equipment ID 12
     *
     * @param entityType Entity class name (e.g., "Freezer", "Equipment")
     * @param entityId   Entity ID
     * @return Count of active alerts (0 if none)
     */
    Long countActiveAlertsForEntity(String entityType, Long entityId);
}
