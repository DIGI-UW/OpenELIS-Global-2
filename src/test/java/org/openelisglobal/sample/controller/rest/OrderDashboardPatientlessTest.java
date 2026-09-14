package org.openelisglobal.sample.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
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
 * has. Fixture: {@code testdata/order-dashboard-patientless.xml} — 25 samples,
 * the newest (DASH-0025) environmental and without a patient.
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

    private MockHttpSession session;
    private MockMvc dashboardMvc;

    @Before
    public void setUp() throws Exception {
        super.setUp();
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

    @Test
    public void dashboard_listsTheNewestOrderFirst_evenBeyondTheDefaultPageSize() throws Exception {
        dashboardMvc.perform(get("/rest/order/dashboard").param("page", "1").param("pageSize", "100").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(25))
                .andExpect(jsonPath("$.orders[0].labNumber").value(NEWEST_ENVIRONMENTAL))
                .andExpect(jsonPath("$.orders[24].labNumber").value("DASH-0001"));
    }

    @Test
    public void dashboard_environmentalFilter_findsThePatientlessOrder() throws Exception {
        dashboardMvc
                .perform(get("/rest/order/dashboard").param("page", "1").param("pageSize", "100")
                        .param("workflowType", "environmental").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(1))
                .andExpect(jsonPath("$.orders[0].labNumber").value(NEWEST_ENVIRONMENTAL))
                .andExpect(jsonPath("$.orders[0].workflowType").value("environmental"))
                .andExpect(jsonPath("$.orders[0].samplingSiteName").value("CPHL"))
                .andExpect(jsonPath("$.orders[0].patientName").isEmpty());
    }

    @Test
    public void dashboard_honoursTheRequestedPageSize() throws Exception {
        dashboardMvc.perform(get("/rest/order/dashboard").param("page", "2").param("pageSize", "10").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orders.length()").value(10))
                .andExpect(jsonPath("$.orders[0].labNumber").value("DASH-0015"))
                .andExpect(jsonPath("$.orders[9].labNumber").value("DASH-0006"));
    }
}
