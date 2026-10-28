package org.openelisglobal.dashboard.management.quality;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.dao.ManagementDashboardDAO;
import org.openelisglobal.dashboard.management.quality.QualityRollup.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

// A 2031 scope has no QC, EQA or release data, so only the NCE component has a value.
@Transactional
public class QualityRollupServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final ScopeRange MARCH_2031 = new ScopeRange(LocalDate.of(2031, 3, 1), LocalDate.of(2031, 3, 31),
            LocalDate.of(2031, 1, 29), LocalDate.of(2031, 2, 28));

    @Autowired
    private ManagementDashboardDAO managementDashboardDAO;

    @Autowired
    private QualityRollupService qualityRollupService;

    private int nextId = 89600;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate.update("DELETE FROM clinlims.qi_weights");
    }

    @Test
    public void ncesCountByReportDateOnBothEndsOfTheWindow() {
        for (String reported : new String[] { "2031-02-28", "2031-03-01", "2031-03-31", "2031-04-01" }) {
            nce(reported);
        }

        assertEquals(2, managementDashboardDAO.countNcesReported(MARCH_2031.from(), MARCH_2031.to()));
        assertEquals(0, managementDashboardDAO.countNcesReported(LocalDate.of(2031, 5, 1), LocalDate.of(2031, 5, 31)));
    }

    @Test
    public void theCompositeIsTheNceInverseWhenNoOtherComponentHasDataAndNeverGoesBelowZero() {
        for (int i = 0; i < 4; i++) {
            sample("2031-03-10");
        }
        nce("2031-03-12");
        sample("2031-02-10");
        sample("2031-02-11");
        for (int i = 0; i < 3; i++) {
            nce("2031-02-12");
        }

        QualityRollup rollup = qualityRollupService.getRollup(MARCH_2031);

        assertEquals(new BigDecimal("75.0"), rollup.composite());
        assertEquals(new BigDecimal("0.0"), rollup.prior());
        assertEquals(List.of(new Component("qcPass", null, new BigDecimal("0.30")),
                new Component("eqa", null, new BigDecimal("0.25")),
                new Component("nceInverse", new BigDecimal("75.00"), new BigDecimal("0.25")),
                new Component("amendmentInverse", null, new BigDecimal("0.20"))), rollup.components());
    }

    @Test
    public void withNoSamplesInTheScopeThereIsNoComposite() {
        assertNull(qualityRollupService.getRollup(MARCH_2031).composite());
    }

    private void sample(String received) {
        int id = nextId++;
        jdbcTemplate.update(
                "INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date)"
                        + " VALUES (?, ?, CAST(? AS timestamp), CAST(? AS timestamp))",
                id, "QR-" + id, received, received);
    }

    private void nce(String reported) {
        jdbcTemplate.update("INSERT INTO clinlims.nc_event (id, report_date)"
                + " VALUES (nextval('clinlims.nc_event_id_seq'), CAST(? AS date))", reported);
    }
}
