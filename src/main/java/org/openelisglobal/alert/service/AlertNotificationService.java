package org.openelisglobal.alert.service;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.openelisglobal.alert.event.AlertCreatedEvent;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertNotificationPayload;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.notification.dao.NotificationConfigOptionDAO;
import org.openelisglobal.notification.service.sender.ClientNotificationSender;
import org.openelisglobal.notification.valueholder.EmailNotification;
import org.openelisglobal.notification.valueholder.NotificationConfigOption;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationMethod;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.notification.valueholder.RemoteNotification;
import org.openelisglobal.notification.valueholder.SMSNotification;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertNotificationService.class);

    @Autowired
    private NotificationConfigOptionDAO notificationConfigOptionDAO;

    @Autowired
    private SiteInformationService siteInformationService;

    @Autowired
    private FreezerService freezerService;

    @SuppressWarnings("rawtypes")
    @Autowired
    private List<ClientNotificationSender> notificationSenders;

    @Value("${org.openelisglobal.ozeki.active:false}")
    private Boolean ozekiActive;

    private static final String SITE_INFO_ALERT_EMAIL = "alert.notification.email";
    private static final String SITE_INFO_ALERT_PHONE = "alert.notification.phone";
    private static final String FREEZER_ENTITY_TYPE = "Freezer";
    private static final DateTimeFormatter MESSAGE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx");

    /**
     * Handle AlertCreatedEvent and send notification.
     *
     * <p>
     *
     * @Async ensures this doesn't block alert creation.
     *
     * @param event AlertCreatedEvent containing the created alert
     */
    @EventListener
    @Async
    @Transactional(readOnly = true)
    public void handleAlertCreated(AlertCreatedEvent event) {
        Alert alert = event.getAlert();

        // Map AlertType to NotificationNature
        NotificationNature nature = mapAlertTypeToNotificationNature(alert.getAlertType());

        // Skip notification if alert type has no notification nature
        if (nature == null) {
            logger.debug("Alert type {} has no notification nature configured, skipping notification",
                    alert.getAlertType());
            return;
        }

        // Build notification message
        String subject = buildNotificationSubject(alert);
        String message = buildNotificationMessage(alert);

        // Check if notifications are enabled for this alert nature
        List<NotificationConfigOption> configs = notificationConfigOptionDAO.getByNature(nature);

        if (configs.isEmpty()) {
            logger.info("No notification configuration found for alert nature: {}", nature);
            return;
        }

        // Send notifications via configured methods
        for (NotificationConfigOption config : configs) {
            if (config.getActive()) {
                if (config.getNotificationMethod() == NotificationMethod.EMAIL && emailEnabledForSystem()) {
                    sendEmailNotification(subject, message, config);
                } else if (config.getNotificationMethod() == NotificationMethod.SMS && smsEnabledForSystem()) {
                    sendSMSNotification(subject, message, config);
                }
            }
        }
    }

    /**
     * Send email notification for alert.
     */
    private void sendEmailNotification(String subject, String message, NotificationConfigOption config) {
        try {
            // Get alert notification email from site information
            SiteInformation emailInfo = siteInformationService.getSiteInformationByName(SITE_INFO_ALERT_EMAIL);
            if (emailInfo == null || emailInfo.getValue() == null || emailInfo.getValue().isEmpty()) {
                logger.warn("Alert notification email not configured in site_information");
                return;
            }

            EmailNotification emailNotification = new EmailNotification();
            emailNotification.setRecipientEmailAddress(emailInfo.getValue());
            emailNotification.setPayload(new AlertNotificationPayload(subject, message));

            // Add BCC recipients if configured
            if (config.getAdditionalContacts() != null && !config.getAdditionalContacts().isEmpty()) {
                emailNotification.setBccs(config.getAdditionalContacts());
            }

            sendNotification(emailNotification);
            logger.info("Alert email notification sent to: {}", emailInfo.getValue());
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendEmailNotification",
                    "Failed to send alert email notification");
            LogEvent.logError(e);
        }
    }

    /**
     * Send SMS notification for alert.
     */
    private void sendSMSNotification(String subject, String message, NotificationConfigOption config) {
        try {
            // Get alert notification phone from site information
            SiteInformation phoneInfo = siteInformationService.getSiteInformationByName(SITE_INFO_ALERT_PHONE);
            if (phoneInfo == null || phoneInfo.getValue() == null || phoneInfo.getValue().isEmpty()) {
                logger.warn("Alert notification phone not configured in site_information");
                return;
            }

            // Extract digits only from phone number
            String phoneNumber = "";
            for (char ch : phoneInfo.getValue().toCharArray()) {
                if (Character.isDigit(ch)) {
                    phoneNumber = phoneNumber + ch;
                }
            }

            if (phoneNumber.isEmpty()) {
                logger.warn("Invalid alert notification phone number in site_information");
                return;
            }

            SMSNotification smsNotification = new SMSNotification();
            smsNotification.setReceiverPhoneNumber(phoneNumber);
            smsNotification.setPayload(new AlertNotificationPayload(subject, subject + "\n\n" + message));

            sendNotification(smsNotification);
            logger.info("Alert SMS notification sent to configured recipient ending {}",
                    phoneNumber.length() > 4 ? phoneNumber.substring(phoneNumber.length() - 4) : "****");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "sendSMSNotification",
                    "Failed to send alert SMS notification");
            LogEvent.logError(e);
        }
    }

    /**
     * Send notification using appropriate sender.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void sendNotification(RemoteNotification clientNotification) {
        for (ClientNotificationSender notificationSender : notificationSenders) {
            if (clientNotification.getClass().isAssignableFrom(notificationSender.forClass())) {
                notificationSender.send(clientNotification);
            }
        }
    }

    /**
     * Check if email is enabled for the system.
     */
    private boolean emailEnabledForSystem() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.PATIENT_RESULTS_SMTP_ENABLED)
                .equals(Boolean.TRUE.toString());
    }

    /**
     * Check if SMS is enabled for the system.
     */
    private boolean smsEnabledForSystem() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.PATIENT_RESULTS_BMP_SMS_ENABLED)
                .equals(Boolean.TRUE.toString())
                || ConfigurationProperties.getInstance().getPropertyValue(Property.PATIENT_RESULTS_SMPP_SMS_ENABLED)
                        .equals(Boolean.TRUE.toString())
                || ozekiActive;
    }

    /**
     * Map AlertType to NotificationNature for notification system. Returns null for
     * alert types without specific notification nature.
     */
    private NotificationNature mapAlertTypeToNotificationNature(AlertType alertType) {
        switch (alertType) {
        // Humidity shares the cold-storage environmental toggle with temperature:
        // both describe the same cabinet drifting out of its band, and a lab that
        // wants one almost always wants the other. Splitting them would need a new
        // nature, its own constraint value and a row in Alert Settings.
        case FREEZER_TEMPERATURE:
        case FREEZER_HUMIDITY:
            return NotificationNature.FREEZER_TEMPERATURE_ALERT;
        // A dead sensor is an equipment failure: no new nature or toggle.
        case FREEZER_OFFLINE:
        case EQUIPMENT_FAILURE:
            return NotificationNature.EQUIPMENT_ALERT;
        case INVENTORY_LOW:
            return NotificationNature.INVENTORY_ALERT;
        case SAMPLE_TRACKING:
        case OTHER:
        default:
            return null; // No notification nature for these alert types
        }
    }

    /**
     * Build notification subject from alert.
     */
    private String buildNotificationSubject(Alert alert) {
        return String.format("Alert: %s - %s", alert.getSeverity(), alert.getAlertType());
    }

    /**
     * Build notification message from alert.
     */
    private String buildNotificationMessage(Alert alert) {
        StringBuilder message = new StringBuilder();
        appendLine(message, "alert.notification.label.type", alert.getAlertType());
        appendLine(message, "alert.notification.label.severity", alert.getSeverity());
        appendLine(message, "alert.notification.label.entity", describeEntity(alert));
        appendLine(message, "alert.notification.label.message", alert.getMessage());
        appendLine(message, "alert.notification.label.time", alert.getStartTime() == null ? null
                : alert.getStartTime().truncatedTo(ChronoUnit.SECONDS).format(MESSAGE_TIME_FORMAT));
        return message.toString();
    }

    /**
     * Runs off-request, so MessageUtil resolves the site-default locale; do not
     * look up a request locale.
     */
    private void appendLine(StringBuilder message, String labelKey, Object value) {
        message.append(MessageUtil.getMessage(labelKey)).append(": ").append(value).append("\n");
    }

    private String describeEntity(Alert alert) {
        if (FREEZER_ENTITY_TYPE.equals(alert.getAlertEntityType()) && alert.getAlertEntityId() != null) {
            String name = freezerService.findById(alert.getAlertEntityId()).map(Freezer::getName).orElse(null);
            if (name != null) {
                return name + " (" + alert.getAlertEntityType() + ")";
            }
        }
        return alert.getAlertEntityType() + " (ID: " + alert.getAlertEntityId() + ")";
    }
}
