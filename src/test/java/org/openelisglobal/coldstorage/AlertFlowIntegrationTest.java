package org.openelisglobal.coldstorage;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.springframework.beans.factory.annotation.Autowired;

public class AlertFlowIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReadingIngestionService readingIngestionService;

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private AlertService alertService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/alert_flow_integration.xml");
    }

    @Test
    public void testTemperatureReadingTriggersThresholdViolationAlert() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0"); // Way above -20°C critical threshold
        OffsetDateTime recordedAt = OffsetDateTime.now();

        readingIngestionService.ingest(freezer, recordedAt, criticalTemp, null, true, null);

        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertFalse("Alert should be created", alerts.isEmpty());

        Alert alert = alerts.getFirst();
        assertEquals("Alert type should be FREEZER_TEMPERATURE", AlertType.FREEZER_TEMPERATURE, alert.getAlertType());
        assertEquals("Alert severity should be CRITICAL", AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, alert.getStatus());
        assertTrue("Alert message should mention temperature",
                alert.getMessage().contains("Temperature threshold violated"));
        assertTrue("Alert message should contain temperature value", alert.getMessage().contains("5.0"));
    }

    @Test
    public void testTemperatureReadingTriggersWarningAlert() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal warningTemp = new BigDecimal("-23.0"); // Between -25 (warning) and -20 (critical)
        OffsetDateTime recordedAt = OffsetDateTime.now();

        readingIngestionService.ingest(freezer, recordedAt, warningTemp, null, true, null);

        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertFalse("Alert should be created", alerts.isEmpty());

        Alert alert = alerts.getFirst();
        assertEquals("Alert type should be FREEZER_TEMPERATURE", AlertType.FREEZER_TEMPERATURE, alert.getAlertType());
        assertEquals("Alert severity should be WARNING", AlertSeverity.WARNING, alert.getSeverity());
        assertTrue("Alert message should contain warning threshold type", alert.getMessage().contains("WARNING_HIGH"));
    }

    @Test
    public void testAlertDeduplicationWithin30MinuteWindow() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0");
        OffsetDateTime recordedAt1 = OffsetDateTime.now();

        readingIngestionService.ingest(freezer, recordedAt1, criticalTemp, null, true, null);
        Thread.sleep(500); // Allow async processing

        List<Alert> alertsAfterFirst = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Should have 1 alert after first violation", 1, alertsAfterFirst.size());
        Alert firstAlert = alertsAfterFirst.getFirst();
        Long firstAlertId = firstAlert.getId();
        assertEquals("Duplicate count should be 0 initially", Integer.valueOf(0), firstAlert.getDuplicateCount());

        // When: Second temperature violation occurs within 30 minutes
        OffsetDateTime recordedAt2 = OffsetDateTime.now().plusMinutes(5);
        readingIngestionService.ingest(freezer, recordedAt2, criticalTemp, null, true, null);
        Thread.sleep(500); // Allow async processing

        List<Alert> alertsAfterSecond = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Should still have only 1 alert (deduplicated)", 1, alertsAfterSecond.size());

        Alert updatedAlert = alertsAfterSecond.getFirst();
        assertEquals("Alert ID should remain the same", firstAlertId, updatedAlert.getId());
        assertEquals("Duplicate count should be incremented to 1", Integer.valueOf(1),
                updatedAlert.getDuplicateCount());
        assertNotNull("Last duplicate time should be set", updatedAlert.getLastDuplicateTime());
    }

    @Test
    public void testMultipleDuplicatesIncrementCountCorrectly() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0");

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, true, null);
        Thread.sleep(500);

        readingIngestionService.ingest(freezer, OffsetDateTime.now().plusMinutes(5), criticalTemp, null, true, null);
        Thread.sleep(500);

        readingIngestionService.ingest(freezer, OffsetDateTime.now().plusMinutes(10), criticalTemp, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Should still have only 1 alert", 1, alerts.size());
        assertEquals("Duplicate count should be 2", Integer.valueOf(2), alerts.get(0).getDuplicateCount());
    }

    @Test
    public void testNoAlertCreatedWhenTemperatureIsNormal() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal normalTemp = new BigDecimal("-80.0");
        OffsetDateTime recordedAt = OffsetDateTime.now();

        readingIngestionService.ingest(freezer, recordedAt, normalTemp, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertTrue("No alerts should be created for normal temperature", alerts.isEmpty());
    }

    @Test
    public void testDifferentAlertTypesAreNotDeduplicated() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0");
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, true, null);
        Thread.sleep(500);

        alertService.createAlert(AlertType.EQUIPMENT_FAILURE, "Freezer", freezerId, AlertSeverity.CRITICAL,
                "Equipment malfunction", "{}");

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Should have 2 different alert types", 2, alerts.size());

        long temperatureAlerts = alerts.stream().filter(a -> a.getAlertType() == AlertType.FREEZER_TEMPERATURE).count();
        long equipmentAlerts = alerts.stream().filter(a -> a.getAlertType() == AlertType.EQUIPMENT_FAILURE).count();

        assertEquals("Should have 1 temperature alert", 1, temperatureAlerts);
        assertEquals("Should have 1 equipment alert", 1, equipmentAlerts);
    }

    @Test
    public void testAlertContextDataContainsTemperatureInformation() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0");
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertFalse("Alert should exist", alerts.isEmpty());

        Alert alert = alerts.getFirst();
        String contextData = alert.getContextData();
        assertNotNull("Context data should not be null", contextData);
        assertTrue("Context data should contain temperature", contextData.contains("temperature"));
        assertTrue("Context data should contain threshold value", contextData.contains("thresholdValue"));
        assertTrue("Context data should contain threshold type", contextData.contains("thresholdType"));
    }

    @Test
    public void testAlertAcknowledgmentWorkflow() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0");
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertFalse("Alert should exist", alerts.isEmpty());
        Alert alert = alerts.getFirst();
        assertEquals("Alert should be OPEN initially", AlertStatus.OPEN, alert.getStatus());

        Alert acknowledgedAlert = alertService.acknowledgeAlert(alert.getId(), 1);

        assertEquals("Alert should be ACKNOWLEDGED", AlertStatus.ACKNOWLEDGED, acknowledgedAlert.getStatus());
        assertNotNull("Acknowledged at should be set", acknowledgedAlert.getAcknowledgedAt());
        assertNotNull("Acknowledged by should be set", acknowledgedAlert.getAcknowledgedBy());
        assertEquals("Acknowledged by user ID should be 1", "1", acknowledgedAlert.getAcknowledgedBy().getId());
    }

    @Test
    public void testAlertResolutionWorkflow() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0");
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        Alert alert = alerts.getFirst();

        Alert acknowledgedAlert = alertService.acknowledgeAlert(alert.getId(), 1);
        assertEquals("Alert should be ACKNOWLEDGED", AlertStatus.ACKNOWLEDGED, acknowledgedAlert.getStatus());

        String resolutionNotes = "Freezer repaired and temperature stabilized at -80°C";
        Alert resolvedAlert = alertService.resolveAlert(acknowledgedAlert.getId(), 1, resolutionNotes);

        assertEquals("Alert should be RESOLVED", AlertStatus.RESOLVED, resolvedAlert.getStatus());
        assertNotNull("Resolved at should be set", resolvedAlert.getResolvedAt());
        assertNotNull("Resolved by should be set", resolvedAlert.getResolvedBy());
        assertEquals("Resolution notes should match", resolutionNotes, resolvedAlert.getResolutionNotes());
        assertNotNull("End time should be set", resolvedAlert.getEndTime());
    }

    @Test
    public void testMultipleFreezersGenerateIndependentAlerts() throws InterruptedException {
        Long freezerId1 = 100L;
        Long freezerId2 = 101L;

        Freezer freezer1 = freezerService.findById(freezerId1).orElse(null);
        Freezer freezer2 = freezerService.findById(freezerId2).orElse(null);

        assertNotNull("Freezer 1 should exist", freezer1);
        assertNotNull("Freezer 2 should exist", freezer2);

        BigDecimal temp1 = new BigDecimal("5.0");
        BigDecimal temp2 = new BigDecimal("10.0");

        readingIngestionService.ingest(freezer1, OffsetDateTime.now(), temp1, null, true, null);
        readingIngestionService.ingest(freezer2, OffsetDateTime.now(), temp2, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts1 = alertService.getAlertsByEntity("Freezer", freezerId1);
        List<Alert> alerts2 = alertService.getAlertsByEntity("Freezer", freezerId2);

        assertEquals("Freezer 1 should have 1 alert", 1, alerts1.size());
        assertEquals("Freezer 2 should have 1 alert", 1, alerts2.size());

        assertNotEquals("Alerts should have different IDs", alerts1.get(0).getId(), alerts2.get(0).getId());
    }

    @Test
    public void testCountActiveAlertsForEntity() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", freezerId, AlertSeverity.CRITICAL, "Alert 1",
                "{}");
        alertService.createAlert(AlertType.EQUIPMENT_FAILURE, "Freezer", freezerId, AlertSeverity.WARNING, "Alert 2",
                "{}");
        Alert alert3 = alertService.createAlert(AlertType.INVENTORY_LOW, "Freezer", freezerId, AlertSeverity.WARNING,
                "Alert 3", "{}");

        alertService.acknowledgeAlert(alert3.getId(), 1);
        alertService.resolveAlert(alert3.getId(), 1, "Resolved");

        Long activeCount = alertService.countActiveAlertsForEntity("Freezer", freezerId);
        assertEquals("Should have 2 active alerts (OPEN alerts, resolved excluded)", Long.valueOf(2), activeCount);
    }
}
