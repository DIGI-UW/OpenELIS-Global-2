package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.reportqueue.service.ReportQueueService;
import org.openelisglobal.reportqueue.service.ReportQueueServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1031 AC-23: the home dashboard's Unprinted Results tile counts orders the
 * way the Report Print Queue does, not results, and only within the user's
 * sections.
 */
public class UnprintedResultsTileTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE = 9301L;
    private static final String ADMIN = "9301";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private ReportQueueService reportQueueService;

    private PatientDashBoardProvider provider;
    private String originalBillingLabel;

    @Before
    public void loadOrder() throws Exception {
        executeDataSetWithStateManagement("testdata/patient-report-v1.xml");
        originalBillingLabel = ConfigurationProperties.getInstance()
                .getPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL);
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL, "9309");
        removePrintHistory();
        ensureSystemRole(Constants.ROLE_GLOBAL_ADMIN);
        jdbcTemplate.update("INSERT INTO clinlims.system_user_role (system_user_id, role_id)"
                + " SELECT 9301, id FROM clinlims.system_role WHERE name = ?", Constants.ROLE_GLOBAL_ADMIN);
        ensureSiteInformation(ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO, "2026-01-01 00:00:00");
        jdbcTemplate.update("UPDATE clinlims.site_information SET value = '2026-01-01 00:00:00' WHERE name = ?",
                ReportQueueServiceImpl.INSTALL_DATE_SITE_INFO);
        statusService.refreshCache();
        jdbcTemplate.update("UPDATE clinlims.analysis SET released_date = now(), lastupdated = now()"
                + " WHERE id IN (9301, 9305)");
        provider = new PatientDashBoardProvider();
        provider.analysisService = analysisService;
        provider.iStatusService = statusService;
    }

    @After
    public void restore() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.BILLING_REFERENCE_NUMBER_LABEL,
                originalBillingLabel);
        jdbcTemplate.update("DELETE FROM clinlims.system_user_role WHERE system_user_id = 9301");
        removePrintHistory();
    }

    private void removePrintHistory() {
        jdbcTemplate.update("DELETE FROM clinlims.document_track WHERE row_id = 9301"
                + " AND table_id = (SELECT id FROM clinlims.reference_tables WHERE name = 'SAMPLE')");
        jdbcTemplate.update("DELETE FROM clinlims.report_print_event WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version_item WHERE report_version_id IN"
                + " (SELECT id FROM clinlims.report_version WHERE sample_id = ?)", SAMPLE);
        jdbcTemplate.update("DELETE FROM clinlims.report_version WHERE sample_id = ?", SAMPLE);
    }

    @Test
    public void theTileCountsOrdersReleasedTodayThatAreStillUnprinted() {
        assertEquals("two results released today on one order count once", 1, provider.unprintedOrdersToday(null));

        reportQueueService.print(SAMPLE, ADMIN).orElseThrow();

        assertEquals("a printed order leaves the tile", 0, provider.unprintedOrdersToday(null));
    }

    @Test
    public void theTileStaysWithinTheUsersSections() {
        assertEquals(1, provider.unprintedOrdersToday(List.of("9301")));
        assertEquals("another section sees nothing", 0, provider.unprintedOrdersToday(List.of("9302")));
    }
}
