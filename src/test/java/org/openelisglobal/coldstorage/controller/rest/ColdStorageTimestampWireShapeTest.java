package org.openelisglobal.coldstorage.controller.rest;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * Stops someone dropping @JsonFormat: Jackson sends OffsetDateTime as epoch
 * seconds.
 */
public class ColdStorageTimestampWireShapeTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertService alertService;

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/freezer_second_probe.xml");
        session = buildAdminSession();
    }

    @Test
    public void alertListShouldSendStartTimeAsAString() throws Exception {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{}");

        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").session(session)).andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.id == " + alert.getId() + ")].startTime", hasItem(instanceOf(String.class))));
    }

    @Test
    public void thresholdAssignmentShouldSendItsEffectiveWindowAsStrings() throws Exception {
        String created = mockMvc
                .perform(post("/rest/coldstorage/thresholds").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Wire shape probe\",\"criticalMin\":-80,\"criticalMax\":-60}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long profileId = JsonPath.parse(created).read("$.id", Long.class);

        mockMvc.perform(post("/rest/coldstorage/100/thresholds/" + profileId + "/assign").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveStart\":\"2026-01-02T03:04:05Z\",\"effectiveEnd\":\"2026-02-02T03:04:05Z\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.effectiveStart").value(instanceOf(String.class)))
                .andExpect(jsonPath("$.effectiveEnd").value(instanceOf(String.class)));
    }

    private MockHttpSession buildAdminSession() {
        UserDetails userDetails = User.withUsername("admin").password("N/A").authorities("ROLE_ADMIN").build();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, "N/A", userDetails.getAuthorities()));

        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);

        MockHttpSession httpSession = new MockHttpSession();
        httpSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        httpSession.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return httpSession;
    }
}
