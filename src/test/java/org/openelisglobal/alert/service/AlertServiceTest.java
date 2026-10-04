package org.openelisglobal.alert.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.event.AlertCreatedEvent;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;

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
    public void testAcknowledgeAlert_CapturesPreviousStatusBeforeTransition() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");

        assertEquals("Newly created alert should start OPEN", AlertStatus.OPEN, alert.getStatus());

        Alert result = alertService.acknowledgeAlert(alert.getId(), 1);

        assertEquals("Alert status should transition to ACKNOWLEDGED", AlertStatus.ACKNOWLEDGED, result.getStatus());
        assertNotEquals("Status should have changed from the original OPEN state", AlertStatus.OPEN,
                result.getStatus());
    }

    @Test
    public void testAcknowledgeAlert_WithNonExistentAlert_ThrowsIllegalArgumentException() {
        try {
            alertService.acknowledgeAlert(999999L, 1);
            fail("Expected IllegalArgumentException for non-existent alert ID");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should reference the alert ID", e.getMessage().contains("999999"));
        }
    }

    @Test
    public void testAcknowledgeAlert_WithNonExistentUser_ThrowsIllegalArgumentException() {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");

        try {
            alertService.acknowledgeAlert(alert.getId(), 999999);
            fail("Expected IllegalArgumentException for non-existent user ID");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should reference the user ID", e.getMessage().contains("999999"));
        }
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

    @Test
    public void testCreateAlert_WhenSeverityWorsens_EscalatesTheOpenAlert() {
        Alert warning = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 601L, AlertSeverity.WARNING,
                "Temperature threshold violated: Current -17.0C", "{\"temperature\": -17.0}");

        Alert escalated = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 601L,
                AlertSeverity.CRITICAL, "Temperature threshold violated: Current 5.0C", "{\"temperature\": 5.0}");

        assertEquals("Escalation must reuse the open alert, not open a second one", warning.getId(), escalated.getId());
        assertEquals("Severity should now be CRITICAL", AlertSeverity.CRITICAL, escalated.getSeverity());
        assertEquals("The message should describe the breach it escalated on",
                "Temperature threshold violated: Current 5.0C", escalated.getMessage());
        assertEquals("Duplicate count still counts the repeat", Integer.valueOf(1), escalated.getDuplicateCount());
    }

    @Test
    public void testCreateAlert_WhenSeverityImproves_DoesNotDowngradeTheOpenAlert() {
        Alert critical = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 602L,
                AlertSeverity.CRITICAL, "Temperature threshold violated: Current 5.0C", "{\"temperature\": 5.0}");

        Alert repeat = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 602L, AlertSeverity.WARNING,
                "Temperature threshold violated: Current -17.0C", "{\"temperature\": -17.0}");

        assertEquals("Should still be the same alert", critical.getId(), repeat.getId());
        assertEquals("Severity should stay CRITICAL", AlertSeverity.CRITICAL, repeat.getSeverity());
        assertEquals("The message should stay the one it escalated on", "Temperature threshold violated: Current 5.0C",
                repeat.getMessage());
    }

    /**
     * One AlertCreatedEvent is one notification dispatch, so an unchanged excursion
     * re-reported each cycle must not publish one.
     */
    @Test
    public void testCreateAlert_WhenSeverityIsUnchanged_PublishesNoFurtherCreatedEvent() {
        int published = countAlertCreatedEventsDuring(() -> {
            alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 611L, AlertSeverity.CRITICAL,
                    "Temperature threshold violated: Current 5.0C", "{\"temperature\": 5.0}");
            alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 611L, AlertSeverity.CRITICAL,
                    "Temperature threshold violated: Current 5.1C", "{\"temperature\": 5.1}");
            alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 611L, AlertSeverity.CRITICAL,
                    "Temperature threshold violated: Current 5.2C", "{\"temperature\": 5.2}");
        });

        assertEquals("Only the initial creation notifies; the two repeat polls must not", 1, published);
    }

    @Test
    public void testCreateAlert_WhenSeverityWorsens_PublishesOneFurtherCreatedEvent() {
        int published = countAlertCreatedEventsDuring(() -> {
            alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 612L, AlertSeverity.WARNING,
                    "Temperature threshold violated: Current -17.0C", "{\"temperature\": -17.0}");
            alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 612L, AlertSeverity.CRITICAL,
                    "Temperature threshold violated: Current 5.0C", "{\"temperature\": 5.0}");
            alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 612L, AlertSeverity.CRITICAL,
                    "Temperature threshold violated: Current 5.1C", "{\"temperature\": 5.1}");
        });

        assertEquals("The raise and the escalation notify; the poll after the escalation must not", 2, published);
    }

    private int countAlertCreatedEventsDuring(Runnable work) {
        AtomicInteger published = new AtomicInteger();
        ApplicationListener<AlertCreatedEvent> counter = new ApplicationListener<AlertCreatedEvent>() {
            @Override
            public void onApplicationEvent(AlertCreatedEvent event) {
                published.incrementAndGet();
            }
        };
        ApplicationEventMulticaster multicaster = applicationContext.getBean(
                AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
        multicaster.addApplicationListener(counter);
        try {
            work.run();
        } finally {
            multicaster.removeApplicationListener(counter);
        }
        return published.get();
    }

    /**
     * Microbiology criticals key by a UUID string, not a numeric entity id. The
     * entity-ref path must round-trip through Alert without touching the numeric
     * alert_entity_id column, so Freezer/Equipment alerts stay unaffected.
     */
    @Test
    public void testCreateAlert_WithStringEntityRef_PersistsWithoutNumericEntityId() {
        Alert result = alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, "MicrobiologyCriticalCommunication",
                "comm-uuid-1234", AlertSeverity.CRITICAL, "Positive blood culture called",
                "{\"caseId\":\"case-uuid-1\"}");

        assertNotNull("Alert should not be null", result);
        assertNotNull("Alert ID should not be null", result.getId());
        assertEquals("comm-uuid-1234", result.getAlertEntityRef());
        assertNull("Numeric entity id must stay null for ref-keyed alerts", result.getAlertEntityId());
        assertEquals(AlertStatus.OPEN, result.getStatus());
    }

    @Test
    public void testGetAlertsByEntityRef_ReturnsOnlyMatchingRefAlerts() {
        alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, "MicrobiologyCriticalCommunication", "comm-a",
                AlertSeverity.CRITICAL, "Message A", "{}");
        alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, "MicrobiologyCriticalCommunication", "comm-b",
                AlertSeverity.CRITICAL, "Message B", "{}");

        List<Alert> alerts = alertService.getAlertsByEntityRef("MicrobiologyCriticalCommunication", "comm-a");

        assertEquals(1, alerts.size());
        assertEquals("comm-a", alerts.get(0).getAlertEntityRef());
    }

    @Test
    public void testCreateAlert_WithDuplicateEntityRefInWindow_IncrementsDuplicateCount() {
        alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, "MicrobiologyCriticalCommunication", "comm-dup",
                AlertSeverity.CRITICAL, "Positive blood culture called", "{}");

        alertService.createAlert(AlertType.MICROBIOLOGY_CRITICAL, "MicrobiologyCriticalCommunication", "comm-dup",
                AlertSeverity.CRITICAL, "Positive blood culture called", "{}");

        List<Alert> alerts = alertService.getAlertsByEntityRef("MicrobiologyCriticalCommunication", "comm-dup");
        assertEquals("Should only have 1 alert (deduplicated)", 1, alerts.size());
        assertEquals(Integer.valueOf(1), alerts.get(0).getDuplicateCount());
    }
}
