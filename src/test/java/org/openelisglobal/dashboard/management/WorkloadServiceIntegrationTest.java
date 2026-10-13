package org.openelisglobal.dashboard.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.workload.WorkloadResponse;
import org.openelisglobal.dashboard.management.workload.WorkloadService;
import org.openelisglobal.test.service.TestSectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class WorkloadServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ScopeRange MARCH_1_TO_3 = new ScopeRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3),
            LocalDate.of(2026, 2, 26), LocalDate.of(2026, 2, 28));

    @Autowired
    private WorkloadService workloadService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        seedSection(89701, "Mgmt Chemistry");
        seedSection(89702, "Mgmt Microbiology");
        testSectionService.refreshNames();
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description, lastupdated)"
                + " VALUES (89700, 897, 'ANALYSIS', 'Mgmt Released', 'Mgmt Released', now()) ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description, lastupdated)"
                + " VALUES (89709, 898, 'ANALYSIS', 'Test Canceled', 'Test Canceled', now()) ON CONFLICT DO NOTHING");
        restoreTatDefault();
        executeDataSetWithStateManagement("testdata/management-workload.xml");
    }

    @After
    public void tearDown() {
        restoreTatDefault();
        jdbc.update("DELETE FROM clinlims.analysis WHERE id BETWEEN 89701 AND 89710");
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id BETWEEN 89701 AND 89708");
        jdbc.update("DELETE FROM clinlims.sample WHERE id BETWEEN 89701 AND 89708");
        jdbc.update("DELETE FROM clinlims.test WHERE id IN (89701, 89702)");
        jdbc.update("DELETE FROM clinlims.test_section WHERE id IN (89701, 89702)");
        jdbc.update("DELETE FROM clinlims.localization_value WHERE id IN (89701, 89702)");
        jdbc.update("DELETE FROM clinlims.localization WHERE id IN (89701, 89702)");
        jdbc.update("DELETE FROM clinlims.status_of_sample WHERE id IN (89700, 89709)");
    }

    private void seedSection(int id, String name) {
        jdbc.update("INSERT INTO clinlims.localization (id, description) VALUES (?, ?) ON CONFLICT DO NOTHING", id,
                name);
        jdbc.update("INSERT INTO clinlims.localization_value (id, localization_id, locale, value)"
                + " VALUES (?, ?, 'en', ?) ON CONFLICT DO NOTHING", id, id, name);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                        + " name_localization_id) VALUES (?, ?, ?, 'N', ?, ?) ON CONFLICT DO NOTHING",
                id, name, name, id, id);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, test_section_id, is_active, orderable,"
                        + " sort_order, lastupdated, name_localization_id, guid)"
                        + " VALUES (?, ?, ?, ?, 'Y', true, ?, now(), ?, ?) ON CONFLICT DO NOTHING",
                id, name, name, id, id, id, "mgmt-" + id);
    }

    private void restoreTatDefault() {
        jdbc.update("INSERT INTO clinlims.qi_config (id, indicator_key, is_enabled, lastupdated, last_updated)"
                + " SELECT nextval('clinlims.qi_config_id_seq'), 'TAT', true, now(), now() WHERE NOT EXISTS"
                + " (SELECT 1 FROM clinlims.qi_config WHERE indicator_key = 'TAT' AND test_category_id IS NULL)");
        jdbc.update("UPDATE clinlims.qi_config SET is_enabled = true, target_threshold = 24, action_threshold = 48"
                + " WHERE indicator_key = 'TAT' AND test_category_id IS NULL");
    }

    @Test
    public void zeroFillsEveryDayOfTheScope() {
        WorkloadResponse workload = workloadService.getWorkload(MARCH_1_TO_3);

        assertEquals(List.of("2026-03-01", "2026-03-02", "2026-03-03"), workload.days());
        assertEquals(List.of(2L, 1L, 0L), section(workload, "89701").counts());
        assertEquals(List.of(1L, 0L, 1L), section(workload, "89702").counts());
    }

    @Test
    public void countsEachSampleOncePerSectionAndUsesTheTestsSectionWhenTheAnalysisHasNone() {
        WorkloadResponse workload = workloadService.getWorkload(MARCH_1_TO_3);

        assertEquals(List.of("89701", "89702"), workload.sections().stream().map(s -> s.id()).toList());
        assertEquals("Mgmt Chemistry", section(workload, "89701").name());
        assertEquals(3, section(workload, "89701").samples());
        assertEquals(2, section(workload, "89702").samples());
    }

    @Test
    public void tatFiguresCoverValidatedAnalysesAgainstTheTatTarget() {
        WorkloadResponse workload = workloadService.getWorkload(MARCH_1_TO_3);

        WorkloadResponse.Section chemistry = section(workload, "89701");
        assertEquals(new BigDecimal("2.00"), chemistry.medianTat());
        assertEquals(new BigDecimal("30.00"), chemistry.p90Tat());
        assertEquals(75.0, chemistry.onTimePct().doubleValue(), 0.001);

        WorkloadResponse.Section microbiology = section(workload, "89702");
        assertEquals(new BigDecimal("48.00"), microbiology.medianTat());
        assertEquals(0.0, microbiology.onTimePct().doubleValue(), 0.001);
    }

    @Test
    public void onTimeIsAbsentWhenTheTatIndicatorIsOff() {
        jdbc.update("UPDATE clinlims.qi_config SET is_enabled = false WHERE indicator_key = 'TAT'"
                + " AND test_category_id IS NULL");

        WorkloadResponse workload = workloadService.getWorkload(MARCH_1_TO_3);

        assertNull(section(workload, "89701").onTimePct());
        assertEquals(new BigDecimal("2.00"), section(workload, "89701").medianTat());
    }

    @Test
    public void anEmptyScopeHasDaysAndNoSections() {
        WorkloadResponse workload = workloadService.getWorkload(new ScopeRange(LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 7), LocalDate.of(2024, 12, 25), LocalDate.of(2024, 12, 31)));

        assertEquals(7, workload.days().size());
        assertEquals(List.of(), workload.sections());
    }

    private static WorkloadResponse.Section section(WorkloadResponse workload, String id) {
        return workload.sections().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
}
