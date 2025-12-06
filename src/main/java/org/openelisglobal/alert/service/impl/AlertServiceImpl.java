package org.openelisglobal.alert.service.impl;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.alert.dao.AlertDAO;
import org.openelisglobal.alert.event.AlertAcknowledgedEvent;
import org.openelisglobal.alert.event.AlertCreatedEvent;
import org.openelisglobal.alert.event.AlertResolvedEvent;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic: - Alert creation with deduplication (30-minute window) -
 * Alert lifecycle management (OPEN → ACKNOWLEDGED → RESOLVED) - Event
 * publishing for downstream processing
 */
@Service
@Transactional
public class AlertServiceImpl extends BaseObjectServiceImpl<Alert, Long> implements AlertService {

    @Autowired
    private AlertDAO alertDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static final int DEDUPLICATION_WINDOW_MINUTES = 30;

    public AlertServiceImpl() {
        super(Alert.class);
    }

    @Override
    protected AlertDAO getBaseObjectDAO() {
        return alertDAO;
    }

    @Override
    @Transactional
    public Alert createAlert(AlertType alertType, String entityType, Long entityId, AlertSeverity severity,
            String message, String contextDataJson) {

        Alert existingAlert = findDuplicateAlert(alertType, entityType, entityId);
        if (existingAlert != null) {
            existingAlert.setDuplicateCount(existingAlert.getDuplicateCount() + 1);
            existingAlert.setLastDuplicateTime(OffsetDateTime.now());
            alertDAO.update(existingAlert);
            return existingAlert;
        }

        Alert alert = new Alert();
        alert.setAlertType(alertType);
        alert.setAlertEntityType(entityType);
        alert.setAlertEntityId(entityId);
        alert.setSeverity(severity);
        alert.setStatus(AlertStatus.OPEN);
        alert.setStartTime(OffsetDateTime.now());
        alert.setMessage(message);
        alert.setContextData(contextDataJson);
        alert.setDuplicateCount(0);

        Long id = alertDAO.insert(alert);
        Alert createdAlert = alertDAO.get(id).orElse(alert);
        eventPublisher.publishEvent(new AlertCreatedEvent(this, createdAlert));
        return createdAlert;
    }

    @Override
    @Transactional
    public Alert acknowledgeAlert(Long alertId, Integer userId) {
        Alert alert = alertDAO.get(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        SystemUser user = systemUserService.get(userId.toString());
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setAcknowledgedBy(user);

        Alert updatedAlert = alertDAO.update(alert);
        eventPublisher.publishEvent(new AlertAcknowledgedEvent(this, updatedAlert, userId.longValue()));
        return updatedAlert;
    }

    @Override
    @Transactional
    public Alert resolveAlert(Long alertId, Integer userId, String resolutionNotes) {
        Alert alert = alertDAO.get(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        SystemUser user = systemUserService.get(userId.toString());
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(OffsetDateTime.now());
        alert.setResolvedBy(user);
        alert.setResolutionNotes(resolutionNotes);
        alert.setEndTime(OffsetDateTime.now());

        Alert updatedAlert = alertDAO.update(alert);
        eventPublisher.publishEvent(new AlertResolvedEvent(this, updatedAlert, userId.longValue(), resolutionNotes));
        return updatedAlert;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> getAlertsByEntity(String entityType, Long entityId) {
        return alertDAO.getAlertsByEntity(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveAlertsForEntity(String entityType, Long entityId) {
        return alertDAO.countActiveAlertsForEntity(entityType, entityId);
    }

    /**
     * Find duplicate alert within deduplication window.
     *
     * <p>
     * Checks for active alert of same type for same entity created/updated within
     * last 30 minutes.
     *
     * @return Existing alert if found, null otherwise
     */
    private Alert findDuplicateAlert(AlertType alertType, String entityType, Long entityId) {
        List<Alert> existingAlerts = alertDAO.getAlertsByEntity(entityType, entityId);

        OffsetDateTime deduplicationCutoff = OffsetDateTime.now().minusMinutes(DEDUPLICATION_WINDOW_MINUTES);

        for (Alert existingAlert : existingAlerts) {
            if (existingAlert.getAlertType() == alertType && (existingAlert.getStatus() == AlertStatus.OPEN
                    || existingAlert.getStatus() == AlertStatus.ACKNOWLEDGED)) {

                OffsetDateTime lastUpdate = existingAlert.getLastDuplicateTime() != null
                        ? existingAlert.getLastDuplicateTime()
                        : existingAlert.getStartTime();

                if (lastUpdate.isAfter(deduplicationCutoff)) {
                    return existingAlert;
                }
            }
        }

        return null;
    }
}
