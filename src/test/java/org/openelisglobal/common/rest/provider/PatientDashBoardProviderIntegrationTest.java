package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile;
import org.openelisglobal.common.rest.provider.form.PatientDashBoardForm;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Home-dashboard order tiles against a real DB (fixture: result.xml). The
 * provider lives in a package the test context does not scan, so it is
 * constructed directly with the services the exercised tile needs (they are
 * package-private, and this test shares the package).
 */
public class PatientDashBoardProviderIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisAnchorService analysisAnchorService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private PatientDashBoardProvider provider;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
        provider = new PatientDashBoardProvider();
        provider.analysisService = analysisService;
        provider.iStatusService = statusService;
        provider.sampleHumanService = sampleHumanService;
        provider.analysisAnchorService = analysisAnchorService;
        // result.xml carries only the Finalized ANALYSIS status; the tile query
        // resolves NotStarted by name (StatusService.addToAnalysisMap).
        Long notTested = jdbc.queryForObject("SELECT count(*) FROM clinlims.status_of_sample WHERE name = 'Not Tested'",
                Long.class);
        if (notTested == 0) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, name, code, status_type, is_active, display_key,"
                    + " description, lastupdated) VALUES (9101, 'Not Tested', 4, 'ANALYSIS', 'Y', 'status.9101',"
                    + " 'Not Tested', NOW())");
        }
        // the status map caches at first use, which may predate the seed above
        statusService.refreshCache();
    }

    /**
     * Regression for the 500 on /rest/home-dashboard/ORDERS_IN_PROGRESS: a sample
     * with no patient link (environmental/vector orders legitimately have none;
     * legacy data can too) NPE'd the bean conversion and took down the whole tile.
     * The orphan order must be served with a blank patient id instead.
     */
    @Test
    public void ordersInProgress_toleratesSampleWithoutPatientLink() throws Exception {
        jdbc.update("UPDATE clinlims.analysis SET status_id = ?::numeric WHERE id = 1",
                statusService.getStatusID(AnalysisStatus.NotStarted));
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id = 1");

        PatientDashBoardForm form = provider.getDashBoardDisplayList(new MockHttpServletRequest(),
                DashBoardTile.TileType.ORDERS_IN_PROGRESS, null);

        assertTrue("the patient-less order must be served with a blank patient id", form.getDisplayItems().stream()
                .anyMatch(bean -> "12345".equals(bean.getLabNumber()) && "".equals(bean.getPatientId())));
    }
}
