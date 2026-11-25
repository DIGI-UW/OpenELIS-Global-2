package org.openelisglobal.sample.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.paging.PagingProperties;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * OGC-1192 §1 — the order dashboard must list a freshly saved order, including
 * a patientless environmental one, no matter how many older samples the lab
 * has. The list is served one server page at a time: a request without
 * {@code page} runs the search and caches the matching orders in the session in
 * pages of paging.results.pageSize, and {@code ?page=k} re-slices that cache.
 * Fixture: {@code testdata/order-dashboard-patientless.xml} — 25 samples, the
 * newest (DASH-0025) environmental and without a patient.
 */
public class OrderDashboardPatientlessTest extends BaseWebContextSensitiveTest {

    private static final String NEWEST_ENVIRONMENTAL = "DASH-0025";

    @Autowired
    private IStatusService statusService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private ObservationHistoryTypeService observationHistoryTypeService;

    @Autowired
    private PagingProperties pagingProperties;

    private Integer resultsPageSizeBefore;

    private MockHttpSession session;
    private MockMvc dashboardMvc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        resultsPageSizeBefore = pagingProperties.getResultsPageSize();
        executeDataSetWithStateManagement("testdata/order-dashboard-patientless.xml");
        authenticateAs("testUser");
        PatientlessOrderObservations.create(sampleService.getSampleByAccessionNumber(NEWEST_ENVIRONMENTAL),
                observationHistoryService, observationHistoryTypeService);
        statusService.refreshCache();
        session = buildSession();
        // The test context does not component-scan org.openelisglobal.sample.controller
        // (AppTestConfig), so the controller is created against the real beans here.
        dashboardMvc = MockMvcBuilders.standaloneSetup(
                webApplicationContext.getAutowireCapableBeanFactory().createBean(OrderSearchRestController.class))
                .build();
    }

    private MockHttpSession buildSession() {
        UserDetails userDetails = User.withUsername("testUser").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);
        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return httpSession;
    }

    @After
    public void restorePageSize() {
        pagingProperties.setResultsPageSize(resultsPageSizeBefore);
    }

    @Test
    public void dashboardListsTheNewestOrderFirstOnItsFirstPage() throws Exception {
        dashboardMvc.perform(get("/rest/order/dashboard").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(25))
                .andExpect(jsonPath("$.orders[0].labNumber").value(NEWEST_ENVIRONMENTAL))
                .andExpect(jsonPath("$.orders[24].labNumber").value("DASH-0001"))
                .andExpect(jsonPath("$.paging.currentPage").value("1"))
                .andExpect(jsonPath("$.paging.totalPages").value("1")).andExpect(jsonPath("$.totalCount").value(25));
    }

    @Test
    public void environmentalWorkflowShowsThePatientlessOrderWithItsSite() throws Exception {
        dashboardMvc.perform(get("/rest/order/dashboard").param("workflowType", "environmental").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(1))
                .andExpect(jsonPath("$.orders[0].labNumber").value(NEWEST_ENVIRONMENTAL))
                .andExpect(jsonPath("$.orders[0].workflowType").value("environmental"))
                .andExpect(jsonPath("$.orders[0].samplingSiteName").value("CPHL"))
                .andExpect(jsonPath("$.orders[0].patientName").isEmpty())
                .andExpect(jsonPath("$.paging.totalPages").value("1"));
    }

    @Test
    public void clinicalWorkflowLeavesTheEnvironmentalOrderOut() throws Exception {
        dashboardMvc.perform(get("/rest/order/dashboard").param("workflowType", "clinical").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(24))
                .andExpect(jsonPath("$.orders[0].labNumber").value("DASH-0024"))
                .andExpect(jsonPath("$.totalCount").value(24));
    }

    @Test
    public void pageRequestsReSliceTheCachedListInPagesOfTheResultsPageSize() throws Exception {
        pagingProperties.setResultsPageSize(10);

        dashboardMvc.perform(get("/rest/order/dashboard").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(10))
                .andExpect(jsonPath("$.orders[0].labNumber").value(NEWEST_ENVIRONMENTAL))
                .andExpect(jsonPath("$.orders[9].labNumber").value("DASH-0016"))
                .andExpect(jsonPath("$.paging.currentPage").value("1"))
                .andExpect(jsonPath("$.paging.totalPages").value("3")).andExpect(jsonPath("$.totalCount").value(25));

        dashboardMvc.perform(get("/rest/order/dashboard").param("page", "2").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(10))
                .andExpect(jsonPath("$.orders[0].labNumber").value("DASH-0015"))
                .andExpect(jsonPath("$.orders[9].labNumber").value("DASH-0006"))
                .andExpect(jsonPath("$.paging.currentPage").value("2"))
                .andExpect(jsonPath("$.paging.totalPages").value("3"));

        dashboardMvc.perform(get("/rest/order/dashboard").param("page", "3").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(5))
                .andExpect(jsonPath("$.orders[4].labNumber").value("DASH-0001"))
                .andExpect(jsonPath("$.paging.currentPage").value("3"));
    }

    @Test
    public void pageRequestWithoutASearchAnswersAnEmptyPage() throws Exception {
        dashboardMvc.perform(get("/rest/order/dashboard").param("page", "2").session(buildSession()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(0))
                .andExpect(jsonPath("$.paging.totalPages").value("1")).andExpect(jsonPath("$.totalCount").value(0));
    }
}
