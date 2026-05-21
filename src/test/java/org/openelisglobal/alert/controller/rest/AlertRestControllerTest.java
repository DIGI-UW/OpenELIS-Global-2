package org.openelisglobal.alert.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MvcResult;

@Rollback
public class AlertRestControllerTest extends BaseWebContextSensitiveTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AlertService alertService;

    private UserSessionData userSessionData;

    private Long alertId;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/alert_service.xml");

        userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(1);

        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.CRITICAL,
                "Temperature threshold violated", "{\"temperature\": -15.5}");
        alertId = alert.getId();
    }

    @Test
    public void getAlerts_shouldReturnArrayPayload() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

        List<?> alerts = objectMapper.readValue(result.getResponse().getContentAsString(), List.class);
        assertNotNull("Alerts response should deserialize", alerts);
        assertTrue("Alerts should include created alert", alerts.size() >= 1);
    }

    @Test
    public void getAlerts_withEntityFilter_shouldReturnFilteredList() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts").param("entityType", "Freezer").param("entityId", "100")
                .accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk()).andReturn();

        List<?> alerts = objectMapper.readValue(result.getResponse().getContentAsString(), List.class);
        assertNotNull("Filtered alerts response should deserialize", alerts);
        assertTrue("Filtered alerts should include created alert", alerts.size() >= 1);
    }

    @Test
    public void getAlertById_shouldReturnAlert() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts/{id}", alertId).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk()).andReturn();

        Map<?, ?> alert = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertNotNull("Alert by id response should deserialize", alert);
        assertEquals("Alert id should match", alertId.longValue(), ((Number) alert.get("id")).longValue());
        assertEquals("Alert status should be OPEN", "OPEN", alert.get("status"));
    }

    @Test
    public void getAlertById_withInvalidId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/rest/alerts/{id}", 999999L).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    public void countActiveAlerts_shouldReturnCount() throws Exception {
        MvcResult result = mockMvc.perform(get("/rest/alerts/count").param("entityType", "Freezer")
                .param("entityId", "100").accept(MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
                .andReturn();

        Map<?, ?> response = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertNotNull("Count response should deserialize", response);
        assertTrue("Count should be >= 1", response.containsKey("count"));
        assertTrue("Count value should be >= 1", ((Number) response.get("count")).longValue() >= 1);
    }

    @Test
    public void acknowledgeAlert_shouldPersistAcknowledgment() throws Exception {
        assertNotNull("Fixture precondition: alert should exist", alertId);
        Alert before = alertService.get(alertId);
        assertEquals("Fixture precondition: initial status should be OPEN", AlertStatus.OPEN, before.getStatus());

        String body = "{\"userId\": 1}";
        mockMvc.perform(put("/rest/alerts/{id}/acknowledge", alertId).contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body).accept(MediaType.APPLICATION_JSON_VALUE)
                .sessionAttr(IActionConstants.USER_SESSION_DATA, userSessionData)).andExpect(status().isOk());

        Alert after = alertService.get(alertId);
        assertEquals("Acknowledge endpoint should persist alert status", AlertStatus.ACKNOWLEDGED, after.getStatus());
    }

}
