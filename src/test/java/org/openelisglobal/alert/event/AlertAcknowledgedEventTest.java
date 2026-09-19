package org.openelisglobal.alert.event;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import org.junit.Test;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;

public class AlertAcknowledgedEventTest {

    @Test
    public void testLegacyConstructor_DefaultsNewFieldsToNull() {
        Alert alert = new Alert();
        Object source = new Object();

        AlertAcknowledgedEvent event = new AlertAcknowledgedEvent(source, alert, 5L);

        assertEquals("Alert should match the one passed in", alert, event.getAlert());
        assertEquals("Acknowledged by user ID should be 5", Long.valueOf(5L), event.getAcknowledgedByUserId());
        assertNull("alertId should default to null via legacy constructor", event.getAlertId());
        assertNull("acknowledgementReason should default to null via legacy constructor",
                event.getAcknowledgementReason());
        assertNull("previousStatus should default to null via legacy constructor", event.getPreviousStatus());
        assertNull("currentStatus should default to null via legacy constructor", event.getCurrentStatus());
        assertNotNull("acknowledgedAt should default to now() when not provided", event.getAcknowledgedAt());
    }

    @Test
    public void testFullConstructor_SetsAllFieldsExplicitly() {
        Alert alert = new Alert();
        Object source = new Object();
        OffsetDateTime explicitTime = OffsetDateTime.now().minusMinutes(10);

        AlertAcknowledgedEvent event = new AlertAcknowledgedEvent(source, alert, 5L, 42L, "false positive",
                AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, explicitTime);

        assertEquals("alertId should match what was passed in", Long.valueOf(42L), event.getAlertId());
        assertEquals("acknowledgementReason should match what was passed in", "false positive",
                event.getAcknowledgementReason());
        assertEquals("previousStatus should be OPEN", AlertStatus.OPEN, event.getPreviousStatus());
        assertEquals("currentStatus should be ACKNOWLEDGED", AlertStatus.ACKNOWLEDGED, event.getCurrentStatus());
        assertEquals("acknowledgedAt should match the explicit timestamp passed in", explicitTime,
                event.getAcknowledgedAt());
    }

    @Test
    public void testFullConstructor_NullAcknowledgedAt_DefaultsToNow() {
        Alert alert = new Alert();
        Object source = new Object();

        AlertAcknowledgedEvent event = new AlertAcknowledgedEvent(source, alert, 5L, 42L, null, AlertStatus.OPEN,
                AlertStatus.ACKNOWLEDGED, null);

        assertNotNull("acknowledgedAt should default to now() when explicitly passed as null",
                event.getAcknowledgedAt());
    }
}