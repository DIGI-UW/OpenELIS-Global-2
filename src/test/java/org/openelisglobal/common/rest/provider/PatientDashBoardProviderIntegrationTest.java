package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile;
import org.openelisglobal.common.rest.provider.bean.homedashboard.OrderDisplayBean;
import org.openelisglobal.common.rest.provider.form.PatientDashBoardForm;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Home-dashboard order tiles against a real DB (fixture: result.xml). The
 * provider lives in a package the test context does not scan, so it is
 * constructed directly with the services the exercised tile needs (they are
 * package-private, and this test shares the package).
 *
 * <p>
 * The lab-unit assignment of the caller is the one thing that cannot be read
 * from the database here — it needs a Spring Security principal — so
 * {@link UserService} and {@link UserRoleService} are stubbed and everything
 * else runs for real.
 */
public class PatientDashBoardProviderIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String USER_ID = "1";
    private static final String TB_SECTION = "1";
    private static final String OTHER_SECTION = "2";
    private static final String TB_LAB_NUMBER = "12345";
    private static final String OTHER_LAB_NUMBER = "13333";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private org.openelisglobal.analysis.service.AnalysisAnchorService analysisAnchorService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private PatientDashBoardProvider provider;

    private UserService userService;

    private UserRoleService userRoleService;

    private JdbcTemplate jdbc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result.xml");
        jdbc = new JdbcTemplate(dataSource);
        userService = mock(UserService.class);
        userRoleService = mock(UserRoleService.class);
        provider = new PatientDashBoardProvider();
        provider.analysisService = analysisService;
        provider.iStatusService = statusService;
        provider.sampleHumanService = sampleHumanService;
        provider.analysisAnchorService = analysisAnchorService;
        provider.testSectionService = testSectionService;
        provider.userService = userService;
        provider.userRoleService = userRoleService;
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
        userHoldsEveryLabUnit();

        PatientDashBoardForm form = provider.getDashBoardDisplayList(request(),
                DashBoardTile.TileType.ORDERS_IN_PROGRESS, null, null);

        assertTrue("the patient-less order must be served with a blank patient id", form.getDisplayItems().stream()
                .anyMatch(bean -> TB_LAB_NUMBER.equals(bean.getLabNumber()) && "".equals(bean.getPatientId())));
    }

    /**
     * The list a user is paging through holds only their lab units. It used to
     * carry the whole lab and rely on the browser to hide the rest, so a user
     * assigned to one section was told how many pages the lab had and found most of
     * them empty.
     */
    @Test
    public void ordersInProgress_holdsOnlyTheSectionsTheUserIsAssigned() throws Exception {
        bothOrdersAwaitingResults();
        userHoldsEveryLabUnit();

        assertEquals("a user of the whole lab sees both orders", Arrays.asList(TB_LAB_NUMBER, OTHER_LAB_NUMBER),
                labNumbersOf(provider.getDashBoardDisplayList(request(), DashBoardTile.TileType.ORDERS_IN_PROGRESS,
                        null, null)));

        userHoldsSections(TB_SECTION);

        assertEquals("a user of one lab unit sees only that unit's order", Arrays.asList(TB_LAB_NUMBER), labNumbersOf(
                provider.getDashBoardDisplayList(request(), DashBoardTile.TileType.ORDERS_IN_PROGRESS, null, null)));
    }

    /**
     * Choosing a test-section tab narrows the list on the server, so the page count
     * describes the rows the tab shows, and a tab the user does not hold yields
     * nothing rather than someone else's orders.
     */
    @Test
    public void ordersInProgress_narrowsToTheChosenTestSectionWithinTheUsersScope() throws Exception {
        bothOrdersAwaitingResults();
        userHoldsSections(TB_SECTION, OTHER_SECTION);

        assertEquals("the chosen tab is the only section in the list", Arrays.asList(OTHER_LAB_NUMBER),
                labNumbersOf(provider.getDashBoardDisplayList(request(), DashBoardTile.TileType.ORDERS_IN_PROGRESS,
                        null, OTHER_SECTION)));

        userHoldsSections(TB_SECTION);

        assertEquals("a section the user does not hold yields nothing", new ArrayList<String>(), labNumbersOf(provider
                .getDashBoardDisplayList(request(), DashBoardTile.TileType.ORDERS_IN_PROGRESS, null, OTHER_SECTION)));
    }

    private void bothOrdersAwaitingResults() {
        jdbc.update("UPDATE clinlims.analysis SET status_id = ?::numeric WHERE id in (1, 2)",
                statusService.getStatusID(AnalysisStatus.NotStarted));
    }

    private void userHoldsEveryLabUnit() {
        when(userRoleService.userInRole(anyString(), anyString())).thenReturn(true);
    }

    private void userHoldsSections(String... sectionIds) {
        when(userRoleService.userInRole(anyString(), anyString())).thenReturn(false);
        List<IdValuePair> sections = Arrays.stream(sectionIds).map(id -> new IdValuePair(id, "section " + id))
                .collect(Collectors.toList());
        when(userService.getUserTestSections(USER_ID, null)).thenReturn(sections);
    }

    private List<String> labNumbersOf(PatientDashBoardForm form) {
        return form.getDisplayItems().stream().map(OrderDisplayBean::getLabNumber).sorted()
                .collect(Collectors.toList());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(Integer.parseInt(USER_ID));
        request.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return request;
    }
}
