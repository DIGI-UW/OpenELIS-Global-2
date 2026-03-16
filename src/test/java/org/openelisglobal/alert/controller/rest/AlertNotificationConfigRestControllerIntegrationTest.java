package org.openelisglobal.alert.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class AlertNotificationConfigRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String BASE_PATH = "/rest/alert-notification-config";
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        objectMapper = new ObjectMapper();
        executeDataSetWithStateManagement("testdata/alert_notification_config.xml");
        cleanRowsInCurrentConnection(new String[] { "notification_config_option", "site_information" });
    }

    @Test
    public void getAlertNotificationConfig_ShouldReturnAllExpectedAlertTypes() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertNotNull("Response body should exist", root);
        assertNotNull("alertConfigs should exist", root.get("alertConfigs"));
        assertNotNull(
            "FREEZER_TEMPERATURE_ALERT should exist",
            root.path("alertConfigs").get("FREEZER_TEMPERATURE_ALERT"));
        assertNotNull("EQUIPMENT_ALERT should exist", root.path("alertConfigs").get("EQUIPMENT_ALERT"));
        assertNotNull("INVENTORY_ALERT should exist", root.path("alertConfigs").get("INVENTORY_ALERT"));
    }

    @Test
    public void saveAlertNotificationConfig_ShouldReturnSuccessMessage() throws Exception {
        String requestBody = objectMapper.writeValueAsString(createConfigPayload(true, false, true, 30, "lab@openelis.org"));

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Alert notification configuration saved successfully"));
    }

    @Test
    public void saveThenGetAlertNotificationConfig_ShouldPersistUpdatedValues() throws Exception {
        Map<String, Object> payload = createConfigPayload(true, false, true, 45, "alerts@openelis.org");

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());

        assertEquals(
                "FREEZER_TEMPERATURE_ALERT email should be true",
                true,
                root.path("alertConfigs").path("FREEZER_TEMPERATURE_ALERT").path("email").asBoolean());
        assertEquals(
                "FREEZER_TEMPERATURE_ALERT sms should be false",
                false,
                root.path("alertConfigs").path("FREEZER_TEMPERATURE_ALERT").path("sms").asBoolean());
        assertEquals("Escalation should be enabled", true, root.path("escalationEnabled").asBoolean());
        assertEquals("Escalation delay should be updated", 45, root.path("escalationDelayMinutes").asInt());
        assertEquals("Supervisor email should be updated", "alerts@openelis.org", root.path("supervisorEmail").asText());
    }

    private Map<String, Object> createConfigPayload(
            boolean enableEmail,
            boolean enableSms,
            boolean escalationEnabled,
            int escalationDelayMinutes,
            String supervisorEmail) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertConfigs", createAlertConfigs(enableEmail, enableSms));
        payload.put("escalationEnabled", escalationEnabled);
        payload.put("escalationDelayMinutes", escalationDelayMinutes);
        payload.put("supervisorEmail", supervisorEmail);
        return payload;
    }

    private Map<String, Map<String, Boolean>> createAlertConfigs(boolean enableEmail, boolean enableSms) {
        Map<String, Map<String, Boolean>> alertConfigs = new HashMap<>();

        Map<String, Boolean> freezerTemperatureConfig = new HashMap<>();
        freezerTemperatureConfig.put("email", enableEmail);
        freezerTemperatureConfig.put("sms", enableSms);
        alertConfigs.put("FREEZER_TEMPERATURE_ALERT", freezerTemperatureConfig);

        Map<String, Boolean> equipmentConfig = new HashMap<>();
        equipmentConfig.put("email", false);
        equipmentConfig.put("sms", false);
        alertConfigs.put("EQUIPMENT_ALERT", equipmentConfig);

        Map<String, Boolean> inventoryConfig = new HashMap<>();
        inventoryConfig.put("email", false);
        inventoryConfig.put("sms", false);
        alertConfigs.put("INVENTORY_ALERT", inventoryConfig);

        return alertConfigs;
    }
}
