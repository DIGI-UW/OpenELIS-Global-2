package org.openelisglobal.sample.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class StatOverdueAlertSchedulerTest extends BaseWebContextSensitiveTest {

    private static final long STAT_SAMPLE = 1L;
    private static final long ROUTINE_SAMPLE = 2L;
    private static final long STAT_SAMPLE_WITHOUT_ANALYSES = 3L;

    @Autowired
    private StatOverdueAlertScheduler scheduler;

    @Autowired
    private AlertService alertService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    public void initData() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        ensureAnalysisStatusRows();
        executeDataSetWithStateManagement("testdata/eqa-order-status.xml");
        jdbc.update("DELETE FROM clinlims.alert WHERE alert_type = 'STAT_OVERDUE'");
        order(STAT_SAMPLE, "STAT", "36 hours");
        order(ROUTINE_SAMPLE, "ROUTINE", "36 hours");
        order(STAT_SAMPLE_WITHOUT_ANALYSES, "STAT", "36 hours");
        setAnalysisStatus(AnalysisStatus.NotStarted, 1, 2, 3);
    }

    @After
    public void restoreShippedTurnaround() {
        setTurnaroundMinutes("60");
    }

    @Test
    public void unresultedStatOrderPastTheShippedTurnaround_raisesACriticalOverdueStatAlert() {
        scheduler.checkStatTurnaround();

        List<Alert> alerts = statOverdueAlerts();
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals(Long.valueOf(STAT_SAMPLE), alert.getAlertEntityId());
        assertEquals("Sample", alert.getAlertEntityType());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    public void resultedStatOrder_raisesNoAlert() {
        setAnalysisStatus(AnalysisStatus.Finalized, 1, 2);

        scheduler.checkStatTurnaround();

        assertTrue(statOverdueAlerts().isEmpty());
    }

    @Test
    public void configuredTurnaround_decidesWhenTheOrderIsOverdue() {
        order(STAT_SAMPLE, "STAT", "90 minutes");

        setTurnaroundMinutes("120");
        scheduler.checkStatTurnaround();
        assertTrue(statOverdueAlerts().isEmpty());

        setTurnaroundMinutes("60");
        scheduler.checkStatTurnaround();
        assertEquals(1, statOverdueAlerts().size());
    }

    @Test
    public void repeatRuns_keepOneAlertPerOrder() {
        scheduler.checkStatTurnaround();
        scheduler.checkStatTurnaround();

        assertEquals(1, statOverdueAlerts().size());
    }

    private List<Alert> statOverdueAlerts() {
        return alertService.getAll().stream().filter(a -> a.getAlertType() == AlertType.STAT_OVERDUE)
                .collect(Collectors.toList());
    }

    private void order(long sampleId, String priority, String receivedAgo) {
        jdbc.update("UPDATE clinlims.sample SET order_priority = ?, received_date = now() - ?::interval WHERE id = ?",
                priority, receivedAgo, sampleId);
    }

    private void setAnalysisStatus(AnalysisStatus status, int... analysisIds) {
        for (int id : analysisIds) {
            jdbc.update("UPDATE clinlims.analysis SET status_id = ?::numeric WHERE id = ?",
                    statusService.getStatusID(status), id);
        }
    }

    private void setTurnaroundMinutes(String minutes) {
        jdbc.update("DELETE FROM clinlims.site_information WHERE name = 'statTurnaroundMinutes'");
        jdbc.update("INSERT INTO clinlims.site_information (id, name, value, value_type, lastupdated)"
                + " VALUES (nextval('clinlims.site_information_seq'), 'statTurnaroundMinutes', ?, 'text', now())",
                minutes);
    }

    // Other fixtures truncate status_of_sample, so restore the rows StatusService
    // maps by name before resolving ids through it.
    private void ensureAnalysisStatusRows() {
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " VALUES (1, 1, 'ANALYSIS', 'Fixture sentinel', 'restored by StatOverdueAlertSchedulerTest')"
                + " ON CONFLICT (id) DO NOTHING");
        String[][] canonical = { { "9604", "Not Tested" }, { "9606", "Finalized" } };
        for (String[] row : canonical) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                    + " SELECT ?::numeric, 1, 'ANALYSIS', ?, 'restored by StatOverdueAlertSchedulerTest'"
                    + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                    + "   WHERE name = ? AND status_type = 'ANALYSIS')", row[0], row[1], row[1]);
        }
        statusService.refreshCache();
    }
}
