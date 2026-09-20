package org.openelisglobal.coldstorage;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.ReadingIngestionService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.springframework.beans.factory.annotation.Autowired;

public class AlertFlowIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ReadingIngestionService readingIngestionService;

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private FreezerReadingService freezerReadingService;

    @Autowired
    private AlertService alertService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/alert_flow_integration.xml");
        // The fixture replaces system_user with its own testuser, so auto-resolution
        // has no resolvable identity unless the principal matches a fixture row.
        authenticateAs("testuser");
    }

    @Test
    public void testTemperatureReadingTriggersThresholdViolationAlert() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0"); // Way above -20°C critical threshold
        OffsetDateTime recordedAt = OffsetDateTime.now();

        readingIngestionService.ingest(freezer, recordedAt, criticalTemp, null, null, true, null);

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

        readingIngestionService.ingest(freezer, recordedAt, warningTemp, null, null, true, null);

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

        readingIngestionService.ingest(freezer, recordedAt1, criticalTemp, null, null, true, null);
        Thread.sleep(500); // Allow async processing

        List<Alert> alertsAfterFirst = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Should have 1 alert after first violation", 1, alertsAfterFirst.size());
        Alert firstAlert = alertsAfterFirst.getFirst();
        Long firstAlertId = firstAlert.getId();
        assertEquals("Duplicate count should be 0 initially", Integer.valueOf(0), firstAlert.getDuplicateCount());

        // When: Second temperature violation occurs within 30 minutes
        OffsetDateTime recordedAt2 = OffsetDateTime.now().plusMinutes(5);
        readingIngestionService.ingest(freezer, recordedAt2, criticalTemp, null, null, true, null);
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

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, null, true, null);
        Thread.sleep(500);

        readingIngestionService.ingest(freezer, OffsetDateTime.now().plusMinutes(5), criticalTemp, null, null, true,
                null);
        Thread.sleep(500);

        readingIngestionService.ingest(freezer, OffsetDateTime.now().plusMinutes(10), criticalTemp, null, null, true,
                null);
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

        readingIngestionService.ingest(freezer, recordedAt, normalTemp, null, null, true, null);
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
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, null, true, null);
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
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, null, true, null);
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
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, null, true, null);
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
        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, null, true, null);
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

        readingIngestionService.ingest(freezer1, OffsetDateTime.now(), temp1, null, null, true, null);
        readingIngestionService.ingest(freezer2, OffsetDateTime.now(), temp2, null, null, true, null);
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

    @Test
    public void testFirstBreachSuppressesAlertButStoresTruthfulStatus() throws InterruptedException {
        // Freezer 102 / profile 3 keeps the default 5-minute minExcursionMinutes.
        Long freezerId = 102L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0"); // Above -20°C critical max

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), criticalTemp, null, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertTrue("Hysteresis should suppress the alert on the first breach", alerts.isEmpty());

        Optional<FreezerReading> reading = freezerReadingService.getLatestReading(freezerId);
        assertTrue("Reading should have been saved", reading.isPresent());
        assertEquals(
                "Stored status must reflect the truthful instantaneous classification, "
                        + "not the hysteresis-suppressed one",
                FreezerReading.Status.CRITICAL, reading.get().getStatus());
    }

    @Test
    public void testSustainedBreachEscalatesWhenPollGapExceedsHysteresisWindow() throws InterruptedException {
        // Freezer 102 / profile 3 keeps the default 5-minute minExcursionMinutes,
        // polled
        // here every 15 minutes: the second breaching poll has to raise the alert.
        Long freezerId = 102L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        BigDecimal criticalTemp = new BigDecimal("5.0"); // Above -20°C critical max
        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(15);

        readingIngestionService.ingest(freezer, firstPoll, criticalTemp, null, null, true, null);
        Thread.sleep(500);
        assertTrue("Hysteresis should still suppress the alert on the first breach",
                alertService.getAlertsByEntity("Freezer", freezerId).isEmpty());

        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(15), criticalTemp, null, null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("A breach still present a poll later must escalate", 1, alerts.size());
        assertEquals(AlertType.FREEZER_TEMPERATURE, alerts.get(0).getAlertType());
    }

    @Test
    public void testOfflineAlertRequiresConsecutiveFailures() throws InterruptedException {
        // Default threshold is 3 consecutive transmission failures.
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        readingIngestionService.ingest(freezer, OffsetDateTime.now().minusMinutes(2), null, null, null, false,
                "timeout");
        readingIngestionService.ingest(freezer, OffsetDateTime.now().minusMinutes(1), null, null, null, false,
                "timeout");
        Thread.sleep(300);
        assertTrue("Two failures should not yet raise an offline alert",
                alertService.getAlertsByEntity("Freezer", freezerId).isEmpty());

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), null, null, null, false, "timeout");
        Thread.sleep(300);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Third consecutive failure should raise the offline alert", 1, alerts.size());
        assertEquals(AlertType.FREEZER_OFFLINE, alerts.get(0).getAlertType());
    }

    @Test
    public void testOfflineAlertNotRaisedWhenASuccessfulPollBreaksTheStreak() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(10);
        ingestFailedPolls(freezer, firstPoll, 2);
        // -40C is inside profile 1's warning band, so this poll raises no alert.
        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(2), new BigDecimal("-40.0"), null, null, true,
                null);
        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(3), null, null, null, false, "timeout");
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertTrue("A recovered poll breaks the streak, so no offline alert is due: " + alertTypes(alerts),
                alerts.stream().noneMatch(alert -> alert.getAlertType() == AlertType.FREEZER_OFFLINE));
    }

    @Test
    public void testOfflineAlertResolvesWhenTransmissionRecovers() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(10);
        ingestFailedPolls(freezer, firstPoll, 3);
        Thread.sleep(500);

        List<Alert> raised = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Three consecutive failures should raise one offline alert", 1, raised.size());
        assertEquals(AlertStatus.OPEN, raised.get(0).getStatus());

        // -40C sits inside profile 1's warning band, so this poll raises no
        // temperature alert of its own.
        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(3), new BigDecimal("-40.0"), null, null, true,
                null);
        Thread.sleep(500);

        List<Alert> afterRecovery = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Recovery should not add an alert", 1, afterRecovery.size());
        Alert offlineAlert = afterRecovery.get(0);
        assertEquals(AlertType.FREEZER_OFFLINE, offlineAlert.getAlertType());
        assertEquals("A device answering again must end the offline alert's lifecycle", AlertStatus.RESOLVED,
                offlineAlert.getStatus());
        assertNotNull("Resolution should record when the alert ended", offlineAlert.getEndTime());
    }

    @Test
    public void testOutageAfterRecoveryRaisesItsOwnOfflineAlert() throws InterruptedException {
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(20);
        ingestFailedPolls(freezer, firstPoll, 3);
        Thread.sleep(500);

        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(3), new BigDecimal("-40.0"), null, null, true,
                null);
        Thread.sleep(500);

        ingestFailedPolls(freezer, firstPoll.plusMinutes(4), 3);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("A second outage must raise its own alert, not bump a duplicate count", 2, alerts.size());
        for (Alert alert : alerts) {
            assertEquals(AlertType.FREEZER_OFFLINE, alert.getAlertType());
        }
        assertEquals("The first outage's alert should still be resolved", 1,
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.RESOLVED).count());
        assertEquals("The second outage should leave one open alert", 1,
                alerts.stream().filter(alert -> alert.getStatus() == AlertStatus.OPEN).count());
    }

    @Test
    public void testRecoveryLeavesAnOpenTemperatureAlertAlone() throws InterruptedException {
        // A lab that loses comms mid-excursion must not get a clean Active Alerts
        // panel back when the device answers again: recovery says nothing about the
        // temperature, so only the FREEZER_OFFLINE alert may be resolved.
        Long freezerId = 100L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Freezer should exist", freezer);

        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(30);

        readingIngestionService.ingest(freezer, firstPoll, new BigDecimal("5.0"), null, null, true, null);
        Thread.sleep(500);
        assertEquals("The warm reading should raise a temperature alert", AlertStatus.OPEN,
                soleAlertOfType(freezerId, AlertType.FREEZER_TEMPERATURE).getStatus());

        ingestFailedPolls(freezer, firstPoll.plusMinutes(1), 3);
        Thread.sleep(500);
        assertEquals("Three consecutive failures should raise the offline alert", AlertStatus.OPEN,
                soleAlertOfType(freezerId, AlertType.FREEZER_OFFLINE).getStatus());

        // -40C breaches none of profile 1's thresholds, so this poll raises no
        // temperature alert of its own.
        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(5), new BigDecimal("-40.0"), null, null, true,
                null);
        Thread.sleep(500);

        assertEquals("Recovery should resolve the offline alert", AlertStatus.RESOLVED,
                soleAlertOfType(freezerId, AlertType.FREEZER_OFFLINE).getStatus());

        Alert excursion = soleAlertOfType(freezerId, AlertType.FREEZER_TEMPERATURE);
        assertEquals("The excursion must survive the recovery still open", AlertStatus.OPEN, excursion.getStatus());
        assertNull("The excursion must not be stamped resolved", excursion.getResolvedAt());
        assertNull("The excursion must not carry the daemon's resolution note", excursion.getResolutionNotes());
        assertNull("The excursion's lifecycle must not have been ended", excursion.getEndTime());
    }

    private Alert soleAlertOfType(Long freezerId, AlertType alertType) {
        List<Alert> matching = alertService.getAlertsByEntity("Freezer", freezerId).stream()
                .filter(alert -> alert.getAlertType() == alertType).toList();
        assertEquals("Expected exactly one " + alertType + " alert for freezer " + freezerId, 1, matching.size());
        return matching.get(0);
    }

    private List<AlertType> alertTypes(List<Alert> alerts) {
        return alerts.stream().map(Alert::getAlertType).toList();
    }

    private void ingestFailedPolls(Freezer freezer, OffsetDateTime firstPoll, int count) {
        for (int i = 0; i < count; i++) {
            readingIngestionService.ingest(freezer, firstPoll.plusMinutes(i), null, null, null, false, "timeout");
        }
    }

    /**
     * Humidity fed the reading's stored status but nothing raised an alert for it,
     * so a cabinet could show a red Critical tag with no alert and no notification
     * behind it. The profile's temperature band is wide of this reading, so
     * humidity is the only thing out of range.
     */
    @Test
    public void testHumidityOnlyBreachRaisesItsOwnAlert() throws InterruptedException {
        Long freezerId = 103L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Humidity test fridge should exist", freezer);

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), new BigDecimal("-80.0"), new BigDecimal("82.0"),
                null, true, null);

        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("A humidity breach should raise exactly one alert", 1, alerts.size());

        Alert alert = alerts.getFirst();
        assertEquals("Alert type should be FREEZER_HUMIDITY", AlertType.FREEZER_HUMIDITY, alert.getAlertType());
        assertEquals("82% against a critical maximum of 75% is CRITICAL", AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, alert.getStatus());
        assertTrue("Message should name humidity, not temperature: " + alert.getMessage(),
                alert.getMessage().contains("Humidity threshold violated"));
        assertTrue("Message should carry the reading: " + alert.getMessage(), alert.getMessage().contains("82.0"));

        FreezerReading stored = freezerReadingService.getLatestReading(freezerId).orElse(null);
        assertNotNull("The reading should be stored", stored);
        assertEquals("The reading itself should be CRITICAL too", FreezerReading.Status.CRITICAL, stored.getStatus());
    }

    /** A humidity reading inside its band must not raise anything. */
    @Test
    public void testHumidityInsideItsBandRaisesNoAlert() throws InterruptedException {
        Freezer freezer = freezerService.findById(103L).orElse(null);
        assertNotNull("Humidity test fridge should exist", freezer);

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), new BigDecimal("-80.0"), new BigDecimal("45.0"),
                null, true, null);

        Thread.sleep(500);

        assertTrue("Nothing is out of band, so there should be no alert",
                alertService.getAlertsByEntity("Freezer", 103L).isEmpty());
    }

    @Test
    public void testTemperatureAndHumidityBreachesRaiseSeparateAlerts() throws InterruptedException {
        Long freezerId = 103L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Humidity test fridge should exist", freezer);

        readingIngestionService.ingest(freezer, OffsetDateTime.now(), new BigDecimal("5.0"), new BigDecimal("82.0"),
                null, true, null);

        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Both breaches should be visible, not one", 2, alerts.size());
        assertTrue("A temperature alert should be among them",
                alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.FREEZER_TEMPERATURE));
        assertTrue("A humidity alert should be among them",
                alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.FREEZER_HUMIDITY));
    }

    /**
     * Freezer 104 / profile 5 pairs humidity bounds with a non-zero
     * minExcursionMinutes, which is what makes a per-metric gate observable.
     */
    @Test
    public void testSustainedTemperatureBreachDoesNotEscalateAFirstHumidityBreach() throws InterruptedException {
        Long freezerId = 104L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Humidity + hysteresis fridge should exist", freezer);
        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(15);

        // Temperature out of band, humidity in band.
        readingIngestionService.ingest(freezer, firstPoll, new BigDecimal("5.0"), new BigDecimal("45.0"), null, true,
                null);
        Thread.sleep(500);
        assertTrue("Neither metric has accumulated any excursion time yet",
                alertService.getAlertsByEntity("Freezer", freezerId).isEmpty());

        // Temperature has now been out of band for 15 minutes; humidity is new.
        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(15), new BigDecimal("5.0"),
                new BigDecimal("82.0"), null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertTrue("The sustained temperature breach must still escalate",
                alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.FREEZER_TEMPERATURE));
        assertTrue(
                "The humidity breach has zero accumulated minutes, so it must stay suppressed: " + alertTypes(alerts),
                alerts.stream().noneMatch(a -> a.getAlertType() == AlertType.FREEZER_HUMIDITY));
    }

    @Test
    public void testSustainedHumidityBreachDoesNotEscalateAMomentaryTemperatureSpike() throws InterruptedException {
        Long freezerId = 104L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Humidity + hysteresis fridge should exist", freezer);
        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(15);

        // Humidity out of band, temperature in band.
        readingIngestionService.ingest(freezer, firstPoll, new BigDecimal("-80.0"), new BigDecimal("82.0"), null, true,
                null);
        Thread.sleep(500);
        assertTrue("Neither metric has accumulated any excursion time yet",
                alertService.getAlertsByEntity("Freezer", freezerId).isEmpty());

        // Humidity still out of band 15 minutes on; temperature spikes once.
        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(15), new BigDecimal("5.0"),
                new BigDecimal("82.0"), null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertTrue("The sustained humidity breach must still escalate",
                alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.FREEZER_HUMIDITY));
        assertTrue("The one-poll temperature spike must stay suppressed: " + alertTypes(alerts),
                alerts.stream().noneMatch(a -> a.getAlertType() == AlertType.FREEZER_TEMPERATURE));
    }

    @Test
    public void testBothMetricsEscalateOnceEachBreachIsSustained() throws InterruptedException {
        Long freezerId = 104L;
        Freezer freezer = freezerService.findById(freezerId).orElse(null);
        assertNotNull("Humidity + hysteresis fridge should exist", freezer);
        OffsetDateTime firstPoll = OffsetDateTime.now().minusMinutes(15);

        readingIngestionService.ingest(freezer, firstPoll, new BigDecimal("5.0"), new BigDecimal("82.0"), null, true,
                null);
        Thread.sleep(500);
        assertTrue("Both breaches are brand new, so both must be suppressed",
                alertService.getAlertsByEntity("Freezer", freezerId).isEmpty());

        readingIngestionService.ingest(freezer, firstPoll.plusMinutes(15), new BigDecimal("5.0"),
                new BigDecimal("82.0"), null, true, null);
        Thread.sleep(500);

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", freezerId);
        assertEquals("Each metric's own sustained breach must escalate: " + alertTypes(alerts), 2, alerts.size());
        assertTrue("A temperature alert should be among them",
                alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.FREEZER_TEMPERATURE));
        assertTrue("A humidity alert should be among them",
                alerts.stream().anyMatch(a -> a.getAlertType() == AlertType.FREEZER_HUMIDITY));
    }
}
