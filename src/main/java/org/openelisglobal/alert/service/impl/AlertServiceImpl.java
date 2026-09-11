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
 * Business logic: - Alert creation with one-alert-per-lifecycle deduplication
 * (an existing OPEN/ACKNOWLEDGED alert for the same (type, entity) is reused;
 * its duplicate count and lastDuplicateTime are bumped instead of inserting a
 * new row) - Alert lifecycle management (OPEN → ACKNOWLEDGED → RESOLVED) -
 * Event publishing for downstream processing
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
            boolean escalated = escalateSeverity(existingAlert, severity, message, contextDataJson);
            alertDAO.update(existingAlert);
            // Only on escalation, and escalation can happen at most once per
            // alert, so this adds one notification for a condition that got
            // worse - not one per repeat poll.
            if (escalated) {
                eventPublisher.publishEvent(new AlertCreatedEvent(this, existingAlert));
            }
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
    public Alert createAlert(AlertType alertType, String entityType, String entityRef, AlertSeverity severity,
            String message, String contextDataJson) {

        Alert existingAlert = findDuplicateAlertByRef(alertType, entityType, entityRef);
        if (existingAlert != null) {
            existingAlert.setDuplicateCount(existingAlert.getDuplicateCount() + 1);
            existingAlert.setLastDuplicateTime(OffsetDateTime.now());
            alertDAO.update(existingAlert);
            return existingAlert;
        }

        Alert alert = new Alert();
        alert.setAlertType(alertType);
        alert.setAlertEntityType(entityType);
        alert.setAlertEntityRef(entityRef);
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
        return acknowledgeAlert(alertId, userId, null);
    }

    @Override
    @Transactional
    public Alert acknowledgeAlert(Long alertId, Integer userId, String acknowledgmentNotes) {
        Alert alert = alertDAO.get(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        SystemUser user = systemUserService.get(userId.toString());
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        AlertStatus previousStatus = alert.getStatus();

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(OffsetDateTime.now());
        alert.setAcknowledgedBy(user);
        // Assigned unconditionally, as resolveAlert does with resolutionNotes: a
        // re-acknowledgment moves acknowledgedAt forward, so leaving an earlier note
        // in place would attribute it to an acknowledgment that did not produce it,
        // and a note entered by mistake could never be cleared.
        alert.setAcknowledgmentNotes(
                acknowledgmentNotes == null || acknowledgmentNotes.isBlank() ? null : acknowledgmentNotes);

        Alert updatedAlert = alertDAO.update(alert);

        eventPublisher.publishEvent(new AlertAcknowledgedEvent(this, updatedAlert, userId.longValue(), alertId, null,
                previousStatus, AlertStatus.ACKNOWLEDGED, updatedAlert.getAcknowledgedAt()));

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
    public List<Alert> getAlertsByEntityRef(String entityType, String entityRef) {
        return alertDAO.getAlertsByEntityRef(entityType, entityRef);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveAlertsForEntity(String entityType, Long entityId) {
        return alertDAO.countActiveAlertsForEntity(entityType, entityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> getUnacknowledgedAlertsOlderThan(String entityType, AlertStatus status, AlertSeverity severity,
            OffsetDateTime cutoff) {
        return alertDAO.getUnacknowledgedAlertsOlderThan(entityType, status, severity, cutoff);
    }

    /**
     * Raises an open alert's severity when the condition behind it has got worse,
     * and refreshes the message and context that travelled with it.
     *
     * <p>
     * Never lowers it: one reading back inside the warning band is not a recovery,
     * and downgrading an open CRITICAL would hide an excursion that is still
     * running. Recovery is a resolve, not a lesser duplicate.
     *
     * @return whether the alert was escalated
     */
    private boolean escalateSeverity(Alert alert, AlertSeverity severity, String message, String contextDataJson) {
        if (severity == null || alert.getSeverity() != null && severity.compareTo(alert.getSeverity()) <= 0) {
            return false;
        }
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setContextData(contextDataJson);
        return true;
    }

    /**
     * Reuses the open alert for a (type, entity) so a repeat bumps its duplicate
     * count; do not add a time window, which spawned a fresh row per expiry.
     */
    private Alert findDuplicateAlert(AlertType alertType, String entityType, Long entityId) {
        List<Alert> existingAlerts = alertDAO.getAlertsByEntity(entityType, entityId);
        return findDuplicateAmong(existingAlerts, alertType);
    }

    /**
     * Same one-alert-per-lifecycle rule as {@link #findDuplicateAlert}, for
     * string-keyed entities.
     */
    private Alert findDuplicateAlertByRef(AlertType alertType, String entityType, String entityRef) {
        List<Alert> existingAlerts = alertDAO.getAlertsByEntityRef(entityType, entityRef);
        return findDuplicateAmong(existingAlerts, alertType);
    }

    private Alert findDuplicateAmong(List<Alert> existingAlerts, AlertType alertType) {
        for (Alert existingAlert : existingAlerts) {
            if (existingAlert.getAlertType() == alertType && (existingAlert.getStatus() == AlertStatus.OPEN
                    || existingAlert.getStatus() == AlertStatus.ACKNOWLEDGED)) {
                return existingAlert;
            }
        }
        return null;
    }
}
