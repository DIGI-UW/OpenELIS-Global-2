package org.openelisglobal.dashboard.management.quality;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

public class QualityRollupServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QualityDAO qualityDAO;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate.execute("TRUNCATE TABLE clinlims.nc_event CASCADE");
        for (String reported : new String[] { "2026-02-28", "2026-03-01", "2026-03-31", "2026-04-01" }) {
            jdbcTemplate.update(
                    "INSERT INTO clinlims.nc_event (id, report_date) VALUES (nextval('clinlims.nc_event_id_seq'),"
                            + " CAST(? AS date))",
                    reported);
        }
    }

    @Test
    public void ncesCountByReportDateOnBothEndsOfTheWindow() {
        assertEquals(2, qualityDAO.countNcesReported(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));
        assertEquals(0, qualityDAO.countNcesReported(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)));
    }
}
