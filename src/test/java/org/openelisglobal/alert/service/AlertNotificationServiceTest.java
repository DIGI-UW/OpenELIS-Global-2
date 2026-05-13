package org.openelisglobal.alert.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link AlertNotificationService} and
 * {@link AlertService}.
 *
 * <p>
 * This test covers the full alert lifecycle: creation, deduplication,
 * acknowledgement, and resolution. It also verifies query methods used by the
 * dashboard.
 */
public class AlertNotificationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertNotificationService alertNotificationService;

    @Autowired
    private AlertService alertService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/alert.xml");
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

        Assert.assertNotEquals("Should create a new alert, not reuse resolved 102", Long.valueOf(102), alert.getId());
        Assert.assertEquals("Status should be OPEN", AlertStatus.OPEN, alert.getStatus());
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
