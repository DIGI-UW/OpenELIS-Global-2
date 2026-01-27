package org.openelisglobal.alert.service;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.beans.factory.annotation.Autowired;

public class AlertServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertService alertService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/alert_service.xml");
    }

    @Test
    public void testCreateAlert_WithValidData_CreatesAlertForAnyEntityType() {
        Alert result = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");

        assertNotNull("Alert should not be null", result);
        assertNotNull("Alert ID should not be null", result.getId());
        assertEquals("Alert type should be FREEZER_TEMPERATURE", AlertType.FREEZER_TEMPERATURE, result.getAlertType());
        assertEquals("Alert entity type should be Freezer", "Freezer", result.getAlertEntityType());
        assertEquals("Alert entity ID should be 100", Long.valueOf(100), result.getAlertEntityId());
        assertEquals("Alert severity should be CRITICAL", AlertSeverity.CRITICAL, result.getSeverity());
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, result.getStatus());
    }

    @Test
    public void testCreateAlert_SetsCorrectInitialState() {
        Alert result = alertService.createAlert(AlertType.EQUIPMENT_FAILURE, "Equipment", 12L, AlertSeverity.WARNING,
                "Equipment malfunction", "{\"errorCode\": \"E-1234\"}");

        assertNotNull("Alert should not be null", result);
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, result.getStatus());
        assertEquals("Duplicate count should be 0", Integer.valueOf(0), result.getDuplicateCount());
        assertNotNull("Start time should not be null", result.getStartTime());
        assertEquals("Alert type should be EQUIPMENT_FAILURE", AlertType.EQUIPMENT_FAILURE, result.getAlertType());
    }

    @Test
    public void testAcknowledgeAlert_WithOpenAlert_TransitionsToAcknowledged() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");

        Alert result = alertService.acknowledgeAlert(alert.getId(), 1);

        assertNotNull("Alert should not be null", result);
        assertEquals("Alert status should be ACKNOWLEDGED", AlertStatus.ACKNOWLEDGED, result.getStatus());
        assertNotNull("Acknowledged at should not be null", result.getAcknowledgedAt());
        assertNotNull("Acknowledged by should not be null", result.getAcknowledgedBy());
        assertEquals("Acknowledged by user ID should be 1", "1", result.getAcknowledgedBy().getId());
    }

    @Test
    public void testResolveAlert_WithAcknowledgedAlert_TransitionsToResolved() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");
        alert = alertService.acknowledgeAlert(alert.getId(), 1);

        Alert result = alertService.resolveAlert(alert.getId(), 1, "Temperature stabilized at -20°C");

        assertNotNull("Alert should not be null", result);
        assertEquals("Alert status should be RESOLVED", AlertStatus.RESOLVED, result.getStatus());
        assertNotNull("Resolved at should not be null", result.getResolvedAt());
        assertNotNull("Resolved by should not be null", result.getResolvedBy());
        assertEquals("Resolution notes should match", "Temperature stabilized at -20°C", result.getResolutionNotes());
    }

    @Test
    public void testCountActiveAlertsForEntity_WithValidEntity_ReturnsCount() {
        // Create 3 alerts with different types to avoid deduplication
        alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL, "Alert 1",
                "{}");
        alertService.createAlert(AlertType.EQUIPMENT_FAILURE, "Freezer", 100L, AlertSeverity.WARNING, "Alert 2", "{}");
        alertService.createAlert(AlertType.INVENTORY_LOW, "Freezer", 100L, AlertSeverity.WARNING, "Alert 3", "{}");

        Long count = alertService.countActiveAlertsForEntity("Freezer", 100L);

        assertNotNull("Count should not be null", count);
        assertEquals("Should have 3 active alerts", Long.valueOf(3), count);
    }

    @Test
    public void testCreateAlert_WithDuplicateInWindow_IncrementsDuplicateCount() {
        Alert firstAlert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L,
                AlertSeverity.CRITICAL, "Temperature threshold violated", "{\"temperature\": -15.5}");

        alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");

        List<Alert> alerts = alertService.getAlertsByEntity("Freezer", 100L);
        assertEquals("Should only have 1 alert (deduplicated)", 1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals("Duplicate count should be 1", Integer.valueOf(1), alert.getDuplicateCount());
        assertNotNull("Last duplicate time should be set", alert.getLastDuplicateTime());
    }
}
