package org.openelisglobal.alert.service;

import java.util.List;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.service.BaseObjectService;

public interface AlertService extends BaseObjectService<Alert, Long> {

    /**
     * Create a new alert for any entity type (polymorphic).
     *
     * <p>
     * Deduplication: If an active alert of the same type exists for the entity and
     * was created/updated within the last 30 minutes, increment duplicate_count
     * instead of creating a new alert.
     *
     * <p>
     * Publishes AlertCreatedEvent upon successful creation.
     *
     * @param alertType       Type of alert (FREEZER_TEMPERATURE, EQUIPMENT_FAILURE,
     *                        etc.)
     * @param entityType      Entity class name (e.g., "Freezer", "Equipment")
     * @param entityId        Entity ID
     * @param severity        Alert severity (WARNING, CRITICAL)
     * @param message         Human-readable alert message
     * @param contextDataJson JSON string with type-specific data
     * @return Created or updated alert
     */
    Alert createAlert(AlertType alertType, String entityType, Long entityId, AlertSeverity severity, String message,
            String contextDataJson);

    /**
     * Acknowledge an alert (transition OPEN → ACKNOWLEDGED).
     *
     * <p>
     * Publishes AlertAcknowledgedEvent upon successful acknowledgment.
     *
     * @param alertId Alert ID
     * @param userId  User ID who acknowledged the alert
     * @return Updated alert
     */
    Alert acknowledgeAlert(Long alertId, Integer userId);

    /**
     * Resolve an alert (transition ACKNOWLEDGED → RESOLVED).
     *
     * <p>
     * Publishes AlertResolvedEvent upon successful resolution.
     *
     * @param alertId         Alert ID
     * @param userId          User ID who resolved the alert
     * @param resolutionNotes Notes describing how the alert was resolved
     * @return Updated alert
     */
    Alert resolveAlert(Long alertId, Integer userId, String resolutionNotes);

    /**
     * Get all alerts for a specific entity (polymorphic query).
     *
     * @param entityType Entity class name (e.g., "Freezer")
     * @param entityId   Entity ID
     * @return List of alerts for the entity
     */
    List<Alert> getAlertsByEntity(String entityType, Long entityId);

    /**
     * Count active alerts (OPEN or ACKNOWLEDGED) for a specific entity.
     *
     * @param entityType Entity class name (e.g., "Freezer")
     * @param entityId   Entity ID
     * @return Count of active alerts
     */
    Long countActiveAlertsForEntity(String entityType, Long entityId);
}
