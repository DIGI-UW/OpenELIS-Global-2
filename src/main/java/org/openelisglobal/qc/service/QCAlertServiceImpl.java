package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notifications.dao.NotificationDAO;
import org.openelisglobal.notifications.entity.Notification;
import org.openelisglobal.qc.dao.QCAlertDAO;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QC Alert management (T100-T102).
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers)
 */
@Service
public class QCAlertServiceImpl implements QCAlertService {

    // Batching window in minutes for WARNING severity alerts
    private static final int BATCHING_WINDOW_MINUTES = 15;

    // Severity that requires immediate alerting (no batching)
    private static final String IMMEDIATE_SEVERITY = "REJECTION";

    // Alert type for rule violations
    private static final String ALERT_TYPE_VIOLATION = "QC_RULE_VIOLATION";

    @Autowired
    private QCAlertDAO alertDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private SystemUserService systemUserService;

    @Override
    @Transactional
    public List<QCAlert> createAlertsForViolation(QCRuleViolation violation) {
        if (violation == null) {
            LogEvent.logWarn(this.getClass().getName(), "createAlertsForViolation",
                    "Cannot create alert for null violation");
            return List.of();
        }

        // Check if this is a WARNING that should be batched
        if (!IMMEDIATE_SEVERITY.equals(violation.getSeverity()) && shouldBatchAlert(violation)) {
            LogEvent.logInfo(this.getClass().getName(), "createAlertsForViolation",
                    "Alert batched for violation: " + violation.getId());
            return List.of();
        }

        String subject = formatAlertSubject(violation);
        String body = formatAlertBody(violation);
        Timestamp now = Timestamp.from(Instant.now());

        // TODO: Make recipient selection configurable (e.g., by role or instrument
        // assignment) instead of broadcasting to all active users.
        List<SystemUser> activeUsers = getActiveUsers();
        List<QCAlert> createdAlerts = new ArrayList<>();

        for (SystemUser user : activeUsers) {
            QCAlert alert = new QCAlert();
            alert.setId(UUID.randomUUID().toString());
            alert.setViolationId(violation.getId());
            alert.setAlertType(ALERT_TYPE_VIOLATION);
            alert.setSentDateTime(now);
            alert.setReadStatus(false);
            alert.setMessageSubject(subject);
            alert.setMessageBody(body);
            alert.setRecipientUserId(Integer.valueOf(user.getId()));
            alert.setSysUserId(user.getId());
            alert.setSystemUserId(Integer.valueOf(user.getId()));

            alertDAO.insert(alert);
            createdAlerts.add(alert);

            // Also create an in-app notification so the Header bell badge lights up
            try {
                Notification notification = new Notification();
                notification.setMessage(subject);
                notification.setCreatedDate(OffsetDateTime.now());
                notification.setReadAt(null);
                notification.setUser(user);
                notificationDAO.save(notification);
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "createAlertsForViolation",
                        "Failed to create notification for user " + user.getId() + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getName(), "createAlertsForViolation",
                "Created " + createdAlerts.size() + " alerts for violation " + violation.getId());

        return createdAlerts;
    }

    private List<SystemUser> getActiveUsers() {
        List<SystemUser> allUsers = systemUserService.getAllSystemUsers();
        List<SystemUser> active = new ArrayList<>();
        for (SystemUser user : allUsers) {
            if ("Y".equals(user.getIsActive())) {
                active.add(user);
            }
        }
        return active;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCAlert> getAlertsForUser(Integer userId) {
        return alertDAO.findByRecipient(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCAlert> getUnreadAlertsForUser(Integer userId) {
        return alertDAO.findUnreadByRecipient(userId);
    }

    @Override
    @Transactional
    public QCAlert markAsRead(String alertId) {
        QCAlert alert = alertDAO.get(alertId).orElse(null);
        if (alert == null) {
            LogEvent.logWarn(this.getClass().getName(), "markAsRead", "Alert not found: " + alertId);
            return null;
        }

        if (!alert.getReadStatus()) {
            alert.setReadStatus(true);
            alert.setReadDateTime(Timestamp.from(Instant.now()));
            alertDAO.update(alert);
        }

        return alert;
    }

    @Override
    @Transactional
    public int markMultipleAsRead(List<String> alertIds) {
        if (alertIds == null || alertIds.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String alertId : alertIds) {
            QCAlert alert = markAsRead(alertId);
            if (alert != null) {
                count++;
            }
        }

        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadAlertCount(Integer userId) {
        List<QCAlert> unread = alertDAO.findUnreadByRecipient(userId);
        return unread != null ? unread.size() : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldBatchAlert(QCRuleViolation violation) {
        if (violation == null) {
            return false;
        }

        // REJECTION severity is never batched
        if (IMMEDIATE_SEVERITY.equals(violation.getSeverity())) {
            return false;
        }

        // Check for recent alerts for the same violation type
        Timestamp windowStart = Timestamp.from(Instant.now().minus(BATCHING_WINDOW_MINUTES, ChronoUnit.MINUTES));

        List<QCAlert> recentAlerts = alertDAO.findByViolation(violation.getId());

        // If there are any alerts for this violation in the window, batch it
        for (QCAlert alert : recentAlerts) {
            if (alert.getSentDateTime().after(windowStart)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Format the alert subject line.
     */
    private String formatAlertSubject(QCRuleViolation violation) {
        String severity = violation.getSeverity();
        String ruleCode = violation.getRuleCode();

        if (IMMEDIATE_SEVERITY.equals(severity)) {
            return String.format("URGENT: QC Rule %s Violation - Immediate Action Required", ruleCode);
        } else {
            return String.format("QC Warning: Rule %s Violation Detected", ruleCode);
        }
    }

    /**
     * Format the alert body.
     */
    private String formatAlertBody(QCRuleViolation violation) {
        StringBuilder body = new StringBuilder();

        body.append("A Westgard rule violation has been detected.\n\n");
        body.append("Rule: ").append(violation.getRuleCode()).append("\n");
        body.append("Severity: ").append(violation.getSeverity()).append("\n");
        body.append("Detection Time: ").append(violation.getViolationDateTime()).append("\n");
        body.append("Instrument ID: ").append(violation.getInstrumentId()).append("\n");
        body.append("Test ID: ").append(violation.getTestId()).append("\n\n");

        if (IMMEDIATE_SEVERITY.equals(violation.getSeverity())) {
            body.append("IMMEDIATE ACTION REQUIRED: Please review the QC results ");
            body.append("and take corrective action before continuing patient testing.\n");
        } else {
            body.append("Please review the QC results at your earliest convenience.\n");
        }

        if (violation.getResolutionNotes() != null && !violation.getResolutionNotes().isEmpty()) {
            body.append("\nDetails: ").append(violation.getResolutionNotes()).append("\n");
        }

        return body.toString();
    }
}
