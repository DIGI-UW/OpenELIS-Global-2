package org.openelisglobal.qaevent.daoimpl;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.dao.DeltaCheckAlertDAO;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert.AlertStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DAO integration test for DeltaCheckAlertDAOImpl. Uses
 * BaseWebContextSensitiveTest with DBUnit test data for database-backed tests.
 */
public class DeltaCheckAlertDAOImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DeltaCheckAlertDAO dao;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/delta-check-alert.xml");
    }

    @Test
    public void getAlertsByStatus_active_returnsTwoAlerts() {
        List<DeltaCheckAlert> alerts = dao.getAlertsByStatus(AlertStatus.ACTIVE);

        assertEquals(2, alerts.size());
    }

    @Test
    public void getAlertsByStatus_dismissed_returnsOneAlert() {
        List<DeltaCheckAlert> alerts = dao.getAlertsByStatus(AlertStatus.DISMISSED);

        assertEquals(1, alerts.size());
        assertEquals("DISMISSED", alerts.get(0).getStatus());
    }

    @Test
    public void getAlertsByStatus_escalated_returnsEmpty() {
        List<DeltaCheckAlert> alerts = dao.getAlertsByStatus(AlertStatus.ESCALATED_NCE);

        assertTrue(alerts.isEmpty());
    }

    @Test
    public void getActiveAlertsForResult_resultWithActiveAlerts_returnsAlerts() {
        List<DeltaCheckAlert> alerts = dao.getActiveAlertsForResult("3");

        assertEquals(1, alerts.size());
        assertEquals("ACTIVE", alerts.get(0).getStatus());
    }

    @Test
    public void getActiveAlertsForResult_noActiveAlerts_returnsEmpty() {
        List<DeltaCheckAlert> alerts = dao.getActiveAlertsForResult("999");

        assertTrue(alerts.isEmpty());
    }

    @Test
    public void getAllAlertsForResult_resultWithMultipleAlerts_returnsAll() {
        List<DeltaCheckAlert> alerts = dao.getAllAlertsForResult("3");

        // Result 3 has alerts 1 (ACTIVE) and 3 (DISMISSED)
        assertEquals(2, alerts.size());
    }

    @Test
    public void alertExistsForResultComparison_existing_returnsTrue() {
        // Alert 1: result_id=3, previous_result_id=4
        assertTrue(dao.alertExistsForResultComparison("3", "4"));
    }

    @Test
    public void alertExistsForResultComparison_nonExistent_returnsFalse() {
        assertFalse(dao.alertExistsForResultComparison("3", "999"));
    }

    @Test
    public void countActiveAlertsForResult_resultWithOneActive_returnsOne() {
        int count = dao.countActiveAlertsForResult("3");

        assertEquals(1, count);
    }

    @Test
    public void countAlertsByStatus_active_returnsTwo() {
        int count = dao.countAlertsByStatus(AlertStatus.ACTIVE);

        assertEquals(2, count);
    }

    @Test
    public void countAlertsByStatus_dismissed_returnsOne() {
        int count = dao.countAlertsByStatus(AlertStatus.DISMISSED);

        assertEquals(1, count);
    }

    @Test
    public void getAlertsDismissedBy_admin_returnsDismissedAlerts() {
        List<DeltaCheckAlert> alerts = dao.getAlertsDismissedBy("admin");

        assertEquals(1, alerts.size());
        assertEquals("Expected change due to treatment", alerts.get(0).getDismissalReason());
    }

    @Test
    public void getAlertsAboveThreshold_lowThreshold_returnsAlerts() {
        List<DeltaCheckAlert> alerts = dao.getAlertsAboveThreshold(20.0);

        // Alerts with change_percent > 20: alert 1 (25%), alert 2 (50%), alert 3 (50%)
        assertEquals(3, alerts.size());
    }

    @Test
    public void getAlertsAboveThreshold_highThreshold_returnsSubset() {
        List<DeltaCheckAlert> alerts = dao.getAlertsAboveThreshold(40.0);

        // Only alerts with change_percent > 40: alert 2 (50%), alert 3 (50%)
        assertEquals(2, alerts.size());
    }
}
