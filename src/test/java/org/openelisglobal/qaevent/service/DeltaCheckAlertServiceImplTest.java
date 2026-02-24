package org.openelisglobal.qaevent.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert.AlertStatus;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.springframework.beans.factory.annotation.Autowired;

public class DeltaCheckAlertServiceImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DeltaCheckAlertService alertService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/delta-check-alert.xml");
    }

    // --- dismissAlert tests ---

    @Test
    public void dismissAlert_validInputs_dismissesAlert() {
        DeltaCheckAlert result = alertService.dismissAlert(1, "Valid reason for dismissal", "testuser");

        assertEquals(AlertStatus.DISMISSED.name(), result.getStatus());
        assertEquals("Valid reason for dismissal", result.getDismissalReason());
        assertEquals("testuser", result.getDismissedBy());
        assertNotNull(result.getDismissedDate());

        // Verify persistence — reload from DB
        DeltaCheckAlert reloaded = alertService.get(1);
        assertEquals(AlertStatus.DISMISSED.name(), reloaded.getStatus());
        assertEquals("Valid reason for dismissal", reloaded.getDismissalReason());
    }

    @Test(expected = IllegalArgumentException.class)
    public void dismissAlert_alertNotFound_throwsException() {
        alertService.dismissAlert(999, "reason text here", "testuser");
    }

    @Test(expected = IllegalArgumentException.class)
    public void dismissAlert_alertNotActive_throwsException() {
        alertService.dismissAlert(3, "reason text here", "testuser");
    }

    @Test(expected = IllegalArgumentException.class)
    public void dismissAlert_emptyReason_throwsException() {
        alertService.dismissAlert(1, "", "testuser");
    }

    @Test(expected = IllegalArgumentException.class)
    public void dismissAlert_nullDismissedBy_throwsException() {
        alertService.dismissAlert(1, "Valid reason for dismissal", null);
    }

    // --- escalateAlertToNCE tests ---

    @Test
    public void escalateAlertToNCE_validInputs_escalatesAlert() {
        NcEvent ncEvent = new NcEvent();
        ncEvent.setId("1");

        DeltaCheckAlert result = alertService.escalateAlertToNCE(1, ncEvent);

        assertEquals(AlertStatus.ESCALATED_NCE.name(), result.getStatus());
        assertNotNull(result.getEscalatedNcEvent());
        assertEquals("1", result.getEscalatedNcEvent().getId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void escalateAlertToNCE_nullNcEvent_throwsException() {
        alertService.escalateAlertToNCE(1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void escalateAlertToNCE_alertNotActive_throwsException() {
        alertService.escalateAlertToNCE(3, new NcEvent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void escalateAlertToNCE_alertNotFound_throwsException() {
        alertService.escalateAlertToNCE(999, new NcEvent());
    }

    // --- getAlertStatistics tests ---

    @Test
    public void getAlertStatistics_returnsCorrectCounts() {
        // Use 500-day window to cover the test data (created April 2025)
        Map<String, Object> stats = alertService.getAlertStatistics(500);

        assertNotNull(stats);
        assertEquals(3L, stats.get("totalAlerts"));
        assertEquals(2L, stats.get("activeAlerts"));
        assertEquals(1L, stats.get("dismissedAlerts"));
        assertEquals(0L, stats.get("escalatedAlerts"));
        // Consistency check: active + dismissed + escalated == total
        long active = (Long) stats.get("activeAlerts");
        long dismissed = (Long) stats.get("dismissedAlerts");
        long escalated = (Long) stats.get("escalatedAlerts");
        assertEquals((long) (Long) stats.get("totalAlerts"), active + dismissed + escalated);
    }

    // --- getFilteredAlerts tests ---

    @Test
    public void getFilteredAlerts_withAnalysisIds_returnsMatchingAlerts() {
        List<String> analysisIds = Arrays.asList("1");
        List<DeltaCheckAlert> result = alertService.getFilteredAlerts(null, analysisIds, 7);

        assertNotNull(result);
        // All test data results (3, 4) belong to analysis 1 — all 3 alerts should match
        assertEquals(3, result.size());
        for (DeltaCheckAlert alert : result) {
            assertNotNull(alert.getResultId());
        }
    }

    @Test
    public void getFilteredAlerts_withStatusOnly_returnsMatchingAlerts() {
        List<DeltaCheckAlert> result = alertService.getFilteredAlerts(AlertStatus.ACTIVE, null, 7);

        assertNotNull(result);
        for (DeltaCheckAlert alert : result) {
            assertEquals(AlertStatus.ACTIVE.name(), alert.getStatus());
        }
    }

    @Test
    public void getFilteredAlerts_noFilters_returnsRecentAlerts() {
        // Use 500-day window to cover the test data (created April 2025)
        List<DeltaCheckAlert> result = alertService.getFilteredAlerts(null, null, 500);

        assertNotNull(result);
        assertEquals("Should return all 3 test data alerts", 3, result.size());
    }

    // --- bulkDismissAlerts tests ---

    @Test
    public void bulkDismissAlerts_allSucceed_returnsCount() {
        int dismissed = alertService.bulkDismissAlerts(Arrays.asList(1, 2), "Bulk dismiss reason text", "admin");

        assertEquals(2, dismissed);

        // Verify both alerts are actually dismissed
        DeltaCheckAlert alert1 = alertService.get(1);
        DeltaCheckAlert alert2 = alertService.get(2);
        assertEquals(AlertStatus.DISMISSED.name(), alert1.getStatus());
        assertEquals(AlertStatus.DISMISSED.name(), alert2.getStatus());
    }

    @Test
    public void bulkDismissAlerts_someFail_returnsPartialCount() {
        int dismissed = alertService.bulkDismissAlerts(Arrays.asList(1, 999), "Bulk dismiss reason text", "admin");

        assertEquals(1, dismissed);
    }

    @Test
    public void bulkDismissAlerts_emptyList_returnsZero() {
        int dismissed = alertService.bulkDismissAlerts(Collections.emptyList(), "Bulk dismiss reason text", "admin");

        assertEquals(0, dismissed);
    }

    @Test
    public void bulkDismissAlerts_allInvalid_returnsZero() {
        int dismissed = alertService.bulkDismissAlerts(Arrays.asList(997, 998, 999), "Bulk dismiss reason text",
                "admin");

        assertEquals(0, dismissed);
    }
}
