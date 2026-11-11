package org.openelisglobal.alert.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.event.AlertCreatedEvent;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Integration tests for {@link AlertNotificationService} and
 * {@link AlertService}.
 *
 * <p>
 * This test covers the full alert lifecycle in {@link AlertService} (creation,
 * deduplication, acknowledgement, and resolution) and verifies that
 * notifications are correctly triggered and sent via {@link JavaMailSender}.
 */
public class AlertNotificationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertNotificationService alertNotificationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private SiteInformationService siteInformationService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/alert.xml");

        // Set up required site information for alert notifications without truncating
        // the shared table.
        // This ensures the test doesn't return early due to missing configuration.
        saveSiteInfo("alert.notification.email", "test@example.com");
        saveSiteInfo("alert.notification.phone", "1234567890");

        // Enable SMTP manually for this test to avoid full DB reload NPE
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATIENT_RESULTS_SMTP_ENABLED, "true");
        // Reset the mock to ensure each test starts with zero email counts
        Mockito.reset(javaMailSender);
    }

    private void saveSiteInfo(String name, String value) {
        org.openelisglobal.siteinformation.valueholder.SiteInformation siteInfo = siteInformationService
                .getSiteInformationByName(name);
        if (siteInfo == null) {
            siteInfo = new org.openelisglobal.siteinformation.valueholder.SiteInformation();
            siteInfo.setName(name);
            siteInfo.setValueType("text");
        }
        siteInfo.setValue(value);
        siteInfo.setSysUserId("1");
        siteInformationService.save(siteInfo);
    }

    @After
    public void tearDown() {
        // Reset the global configuration to avoid affecting other tests in CI
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATIENT_RESULTS_SMTP_ENABLED, "false");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_attempt WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE product = 'ALERT')");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log_channel WHERE notification_log_id IN"
                + " (SELECT id FROM clinlims.notification_log WHERE product = 'ALERT')");
        jdbcTemplate.update("DELETE FROM clinlims.notification_log WHERE product = 'ALERT'");
    }

    @Test
    public void createAlert_shouldProcessSuccessfully_whenFreezerTemperatureAlertIsCreated() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5, \"threshold\": -20.0}");

        Assert.assertNotNull("Alert should not be null", alert);
        Assert.assertNotNull("Alert ID should not be null", alert.getId());
        Assert.assertEquals("Alert type should be FREEZER_TEMPERATURE", AlertType.FREEZER_TEMPERATURE,
                alert.getAlertType());
        Assert.assertEquals("Alert status should be OPEN", AlertStatus.OPEN, alert.getStatus());
    }

    @Test
    public void createAlert_shouldIncrementDuplicateCount_whenDuplicateOpenAlertExists() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 1L, AlertSeverity.CRITICAL,
                "Repeat excursion", null);

        Assert.assertEquals("Should reuse existing alert ID 100", Long.valueOf(100), alert.getId());
        Assert.assertEquals("Duplicate count should be incremented to 1", Integer.valueOf(1),
                alert.getDuplicateCount());
    }

    @Test
    public void acknowledgeAlert_shouldUpdateStatusToAcknowledged_whenValidAlertIdAndUserIdProvided() {
        Alert alert = alertService.acknowledgeAlert(100L, 1);

        Assert.assertEquals("Status should be ACKNOWLEDGED", AlertStatus.ACKNOWLEDGED, alert.getStatus());
        Assert.assertNotNull("Acknowledged timestamp should be set", alert.getAcknowledgedAt());
        Assert.assertEquals("Acknowledged by user ID should be 1", "1", alert.getAcknowledgedBy().getId());
    }

    @Test
    public void resolveAlert_shouldUpdateStatusToResolved_whenValidAlertIdAndUserIdProvided() {
        Alert alert = alertService.resolveAlert(101L, 1, "Fixed the issue");

        Assert.assertEquals("Status should be RESOLVED", AlertStatus.RESOLVED, alert.getStatus());
        Assert.assertNotNull("Resolved timestamp should be set", alert.getResolvedAt());
        Assert.assertNotNull("End time should be set", alert.getEndTime());
        Assert.assertEquals("Resolution notes should match", "Fixed the issue", alert.getResolutionNotes());
    }

    @Test
    public void getAlertsByEntity_shouldReturnCorrectAlerts_whenEntityInfoProvided() {
        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", 1L);
        Assert.assertEquals("Should find 1 alert for Freezer 1", 1, alerts.size());

        List<Alert> alerts3 = alertService.getAlertsByEntity("Freezer", 3L);
        Assert.assertEquals("Should find 1 alert for Freezer 3", 1, alerts3.size());
    }

    @Test
    public void countActiveAlertsForEntity_shouldReturnCorrectCount_whenEntityInfoProvided() {
        Long count = alertService.countActiveAlertsForEntity("Freezer", 1L);
        Assert.assertEquals("Should count 1 active alert for Freezer 1", Long.valueOf(1), count);

        Long count3 = alertService.countActiveAlertsForEntity("Freezer", 3L);
        Assert.assertEquals("Should count 1 active alert for Freezer 3", Long.valueOf(1), count3);
    }

    @Test
    public void createAlert_shouldCreateNewAlert_whenPreviousAlertIsResolved() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 2L, AlertSeverity.CRITICAL,
                "New failure after resolution", null);

        Assert.assertNotNull("Created alert should not be null", alert);
        Assert.assertNotNull("Created alert should be persisted with a generated id", alert.getId());
        Assert.assertFalse("Should create a new alert, not reuse an existing seeded alert (100, 101, 102)",
                List.of(100L, 101L, 102L).contains(alert.getId()));
        Assert.assertEquals("Status should be OPEN", AlertStatus.OPEN, alert.getStatus());
    }

    @Test
    public void handleAlertCreated_dispatchesEmailThroughTheSmtpServerConnection_notTheMailSenderBean() {
        // The demo-silnas sync moved email transport onto the SMTP_SERVER
        // external_connection row (read live, editable in the External
        // Connections admin UI), so the container-injected JavaMailSender bean
        // this test used to verify is no longer part of the path. With no
        // SMTP_SERVER configured the sender refuses loudly and the alert flow
        // logs the failure without letting it escape.
        Alert alert = alertService.get(100L);
        AlertCreatedEvent event = new AlertCreatedEvent(this, alert);

        alertNotificationService.handleAlertCreated(event);

        Mockito.verify(javaMailSender, Mockito.after(2000).never()).send(ArgumentMatchers.any(SimpleMailMessage.class));
    }

    /**
     * An alert's email is queued in the delivery outbox under product Alert, where
     * Sent Messages shows it and the delivery worker sends it; nothing is sent from
     * the alert thread itself.
     */
    @Test
    public void handleAlertCreated_queuesAMessageInTheOutbox_forAFreezerOfflineAlert() {
        // The fixture seeds an active EMAIL config for both alert natures, so a
        // queued row alone would not say which one governs offline alerts. Silencing
        // the temperature nature leaves EQUIPMENT_ALERT as the only route that can
        // produce a message, which is the contract the Alert Settings screen shows.
        jdbcTemplate.update("UPDATE clinlims.notification_config_option SET active = false"
                + " WHERE notification_nature = 'FREEZER_TEMPERATURE_ALERT'");
        long before = countAlertRows();

        Alert offlineAlert = new Alert();
        offlineAlert.setAlertType(AlertType.FREEZER_OFFLINE);
        offlineAlert.setAlertEntityType("Freezer");
        offlineAlert.setAlertEntityId(7L);
        offlineAlert.setSeverity(AlertSeverity.CRITICAL);
        offlineAlert.setStatus(AlertStatus.OPEN);
        offlineAlert.setMessage("Freezer is not responding to monitoring polls: timeout");
        offlineAlert.setStartTime(OffsetDateTime.now());

        alertNotificationService.handleAlertCreated(new AlertCreatedEvent(this, offlineAlert));

        Assert.assertEquals("An offline alert should notify through the equipment nature, not the temperature one",
                before + 1, countAlertRows());
        java.util.Map<String, Object> row = jdbcTemplate.queryForMap("SELECT event_code, recipient_email,"
                + " rendered_subject, rendered_message, overall_status FROM clinlims.notification_log"
                + " WHERE product = 'ALERT' ORDER BY id DESC LIMIT 1");
        Assert.assertEquals("EQUIPMENT_ALERT", row.get("event_code"));
        Assert.assertEquals("test@example.com", row.get("recipient_email"));
        Assert.assertTrue("Subject should name the alert type",
                String.valueOf(row.get("rendered_subject")).contains("FREEZER_OFFLINE"));
        Assert.assertTrue("Message should carry the offline reason",
                String.valueOf(row.get("rendered_message")).contains("not responding to monitoring polls"));
        Assert.assertTrue("queued for the worker, or failed at once when Email is not set up",
                List.of("QUEUED", "FAILED").contains(String.valueOf(row.get("overall_status"))));
    }

    private long countAlertRows() {
        Long count = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM clinlims.notification_log WHERE product = 'ALERT'", Long.class);
        return count == null ? 0 : count;
    }

    @Test
    public void getUnacknowledgedAlertsOlderThan_shouldReturnCorrectAlerts_whenCutoffProvided() {
        OffsetDateTime cutoff = alertService.get(100L).getStartTime().plusHours(2);
        List<Alert> alerts = alertService.getUnacknowledgedAlertsOlderThan("Freezer", AlertStatus.OPEN,
                AlertSeverity.CRITICAL, cutoff);

        Assert.assertEquals("Should find 1 old unacknowledged alert (100)", 1, alerts.size());
        Assert.assertEquals("Should be alert 100", Long.valueOf(100), alerts.get(0).getId());
    }
}
