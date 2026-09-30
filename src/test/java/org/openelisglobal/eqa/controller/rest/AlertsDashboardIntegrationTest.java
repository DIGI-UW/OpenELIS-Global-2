package org.openelisglobal.eqa.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

public class AlertsDashboardIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AlertService alertService;

    private EQAAlertRestController controller;

    // The test context's JSON converter writes dates as strings; production's does
    // not, so serialize with the production mapper.
    private final ObjectMapper productionMapper = new AppConfig().jacksonMessageConverter().getObjectMapper();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/alert_flow_integration.xml");
        controller = new EQAAlertRestController();
        ReflectionTestUtils.setField(controller, "alertService", alertService);
    }

    @Test
    public void dashboardSendsTheCreationTimeAsTextAndInTheSiteFormat() throws Exception {
        Alert alert = alertService.createAlert(AlertType.FREEZER_TEMPERATURE, "Freezer", 100L, AlertSeverity.WARNING,
                "Temperature drifting", "{}");

        JsonNode row = dashboardRow(alert.getId());

        assertTrue("startTime must not be a bare epoch number", row.get("startTime").isTextual());
        assertEquals(alert.getStartTime().toInstant(), OffsetDateTime.parse(row.get("startTime").asText()).toInstant());
        assertEquals(DateUtil.convertTimestampToStringDateAndConfiguredHourTime(
                Timestamp.from(alert.getStartTime().toInstant())), row.get("startTimeForDisplay").asText());
    }

    private JsonNode dashboardRow(Long alertId) throws Exception {
        JsonNode body = productionMapper
                .valueToTree(controller.getAlertsDashboard(null, null, null, null, 0, 1000).getBody());
        for (JsonNode row : body.get("alerts")) {
            if (row.get("id").asLong() == alertId) {
                return row;
            }
        }
        throw new AssertionError("alert " + alertId + " missing from the dashboard");
    }
}
