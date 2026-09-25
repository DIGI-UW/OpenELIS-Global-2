package org.openelisglobal.alert.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

public class AlertRestControllerTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertService alertService;

    private MockHttpSession session;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/alert_flow_integration.xml");
        session = buildAuthenticatedSession();
    }

    private MockHttpSession buildAuthenticatedSession() {
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

    @Test
    public void acknowledgeAlert_shouldPersistTheNotesFromTheRequestBody() throws Exception {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{}");
        String notes = "Door was left ajar during restock; monitoring for the next hour";

        mockMvc.perform(put("/rest/alerts/" + alert.getId() + "/acknowledge").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"" + notes + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acknowledgmentNotes").value(notes));

        assertEquals("Acknowledgment notes must survive on the alert row", notes,
                alertService.get(alert.getId()).getAcknowledgmentNotes());
    }

    @Test
    public void acknowledgeAlert_withoutNotes_shouldClearANotePreviouslyRecorded() throws Exception {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{}");
        mockMvc.perform(put("/rest/alerts/" + alert.getId() + "/acknowledge").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"entered by mistake\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/rest/alerts/" + alert.getId() + "/acknowledge").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgmentNotes").doesNotExist());

        assertNull("A re-acknowledgment carrying no note must not leave the earlier one behind",
                alertService.get(alert.getId()).getAcknowledgmentNotes());
    }

    @Test
    public void getAlerts_withEntityTypeAndNoEntityId_shouldNotLeakOtherEntityTypes() throws Exception {
        Alert freezerAlert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L,
                AlertSeverity.CRITICAL, "Temperature threshold violated", "{}");
        Alert analysisAlert = alertService.createAlert(AlertType.CRITICAL_RESULT, "ANALYSIS", 777L,
                AlertSeverity.CRITICAL, "Critical result awaiting notification", "{}");

        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + freezerAlert.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + analysisAlert.getId() + ")]").doesNotExist());
    }

    @Test
    public void deleteAlert_withUnknownId_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/rest/alerts/99999").session(session)).andExpect(status().isNotFound());
    }

    /**
     * The 204 is not the assertion: Clear has to leave the alert out of the list
     * the dashboard reloads from.
     */
    @Test
    public void deleteAlert_shouldRemoveTheAlertFromTheListTheDashboardReloads() throws Exception {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{}");

        mockMvc.perform(delete("/rest/alerts/" + alert.getId()).session(session)).andExpect(status().isNoContent());

        mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + alert.getId() + ")]").doesNotExist());
    }
}
