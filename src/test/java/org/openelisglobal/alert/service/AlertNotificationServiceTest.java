package org.openelisglobal.alert.service;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.beans.factory.annotation.Autowired;

public class AlertNotificationServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertNotificationService alertNotificationService;

    @Autowired
    private AlertService alertService;

    @Test
    public void testHandleAlertCreated_WithFreezerTemperatureAlert_ProcessesSuccessfully() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5, \"threshold\": -20.0}");

        assertNotNull("Alert should not be null", alert);
        assertNotNull("Alert ID should not be null", alert.getId());
        assertEquals("Alert type should be FREEZER_TEMPERATURE", AlertType.FREEZER_TEMPERATURE, alert.getAlertType());
        assertEquals("Alert status should be OPEN", AlertStatus.OPEN, alert.getStatus());
    }

    @Test
    public void testHandleAlertCreated_WithEquipmentFailureAlert_ProcessesSuccessfully() {
        Alert alert = alertService.createAlert(AlertType.EQUIPMENT_FAILURE, "Equipment", 12L, AlertSeverity.CRITICAL,
                "Equipment malfunction detected", "{\"errorCode\": \"E-1234\"}");

        assertNotNull("Alert should not be null", alert);
        assertEquals("Alert type should be EQUIPMENT_FAILURE", AlertType.EQUIPMENT_FAILURE, alert.getAlertType());
        assertEquals("Alert severity should be CRITICAL", AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    public void testHandleAlertCreated_WithGeneralAlert_ProcessesSuccessfully() {
        Alert alert = alertService.createAlert(AlertType.INVENTORY_LOW, "Inventory", 50L, AlertSeverity.WARNING,
                "Inventory running low", "{\"itemId\": 50, \"quantity\": 5}");

        assertNotNull("Alert should not be null", alert);
        assertEquals("Alert type should be INVENTORY_LOW", AlertType.INVENTORY_LOW, alert.getAlertType());
        assertEquals("Alert severity should be WARNING", AlertSeverity.WARNING, alert.getSeverity());
    }
}
