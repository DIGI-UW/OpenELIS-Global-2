package org.openelisglobal.dashboard.management;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.service.ManagementDashboardService;
import org.openelisglobal.dashboard.management.service.ManagementSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ManagementDashboardServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ScopeRange MARCH_2026 = new ScopeRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
            LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 28));

    @Autowired
    private ManagementDashboardService managementDashboardService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/management-dashboard.xml");
    }

    @Test
    public void countsSamplesReceivedOnEveryDayOfTheScopeAndNoOthers() {
        ManagementSummary summary = managementDashboardService.getSummary(MARCH_2026);

        assertEquals(2, summary.samplesReceived());
    }

    @Test
    public void detectsEachOptionalModuleFromItsOwnTable() {
        jdbcTemplate.execute("TRUNCATE clinlims.inventory_item, clinlims.eqa_lab_program_enrollment,"
                + " clinlims.analyzer, clinlims.micro_ast_reading, clinlims.qc_result CASCADE");

        assertEquals(Map.of("inventory", false, "eqa", false, "analyzers", false, "ast", false, "qc", false),
                managementDashboardService.getSummary(MARCH_2026).features());

        jdbcTemplate.update("INSERT INTO clinlims.analyzer (id, name, is_active) VALUES (89701, 'MGMT-A', true)");
        jdbcTemplate.update("INSERT INTO clinlims.test (id, name, description, guid)"
                + " VALUES (89701, 'MGMT-QC', 'MGMT-QC', 'mgmt-dashboard-qc-89701')");
        jdbcTemplate.update("INSERT INTO clinlims.qc_result (id, test_id, result_value, run_date_time,"
                + " result_status, sys_user_id, last_updated, source)"
                + " VALUES (89701, '89701', 1.0, now(), 'FINAL', '1', now(), 'MANUAL')");

        assertEquals(Map.of("inventory", false, "eqa", false, "analyzers", true, "ast", false, "qc", true),
                managementDashboardService.getSummary(MARCH_2026).features());
    }
}
