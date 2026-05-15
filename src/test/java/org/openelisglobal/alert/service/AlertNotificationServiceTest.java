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
        siteInformationService.save(siteInfo);
    }

    @After
    public void tearDown() {
        // Reset the global configuration to avoid affecting other tests in CI
        ConfigurationProperties.getInstance().setPropertyValue(Property.PATIENT_RESULTS_SMTP_ENABLED, "false");
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
    public void handleAlertCreated_shouldSendEmailNotification_whenValidAlertProvided() {
        Alert alert = alertService.get(100L);
        AlertCreatedEvent event = new AlertCreatedEvent(this, alert);

        alertNotificationService.handleAlertCreated(event);

        // Verify that JavaMailSender.send() is called within 5 seconds (to handle
        // @Async)
        Mockito.verify(javaMailSender, Mockito.timeout(5000)).send(ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    public void getUnacknowledgedAlertsOlderThan_shouldReturnCorrectAlerts_whenCutoffProvided() {
        OffsetDateTime cutoff = OffsetDateTime.parse("2025-05-10T12:00:00Z");
        List<Alert> alerts = alertService.getUnacknowledgedAlertsOlderThan("Freezer", AlertStatus.OPEN,
                AlertSeverity.CRITICAL, cutoff);

        Assert.assertEquals("Should find 1 old unacknowledged alert (100)", 1, alerts.size());
        Assert.assertEquals("Should be alert 100", Long.valueOf(100), alerts.get(0).getId());
    }
}
