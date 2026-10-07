package org.openelisglobal.eqa.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.config.AppConfig;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

public class AlertsDashboardIntegrationTest extends BaseWebContextSensitiveTest {

    private static final int FIXTURE_USER = 1;
    private static final AtomicLong ENTITY_IDS = new AtomicLong(System.currentTimeMillis());

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
        OffsetDateTime stored = alertService.get(alert.getId()).getStartTime();

        assertTrue("startTime must not be a bare epoch number", row.get("startTime").isTextual());
        assertEquals(stored.toInstant(), OffsetDateTime.parse(row.get("startTime").asText()).toInstant());
        assertEquals(DateUtil.convertTimestampToStringDateAndConfiguredHourTime(Timestamp.from(stored.toInstant())),
                row.get("startTimeForDisplay").asText());
    }

    @Test
    public void acknowledgeSetsAcknowledgedAndRecordsWhoAndWhenWithoutResolving() {
        Alert alert = openAlert(AlertSeverity.CRITICAL);

        assertEquals(HttpStatus.OK, controller
                .acknowledgeAlert(alert.getId(), Map.of("notes", "Called the ward"), sessionRequest()).getStatusCode());

        Alert stored = alertService.get(alert.getId());
        assertEquals(AlertStatus.ACKNOWLEDGED, stored.getStatus());
        assertEquals(String.valueOf(FIXTURE_USER), stored.getAcknowledgedBy().getId());
        assertNotNull(stored.getAcknowledgedAt());
        assertEquals("Called the ward", stored.getAcknowledgmentNotes());
        assertNull(stored.getResolvedAt());
    }

    @Test
    public void acknowledgingACriticalAlertWithoutACommentIsRefused() {
        Alert alert = openAlert(AlertSeverity.CRITICAL);

        assertEquals(HttpStatus.BAD_REQUEST,
                controller.acknowledgeAlert(alert.getId(), Map.of("notes", " "), sessionRequest()).getStatusCode());
        assertEquals(AlertStatus.OPEN, alertService.get(alert.getId()).getStatus());
    }

    @Test
    public void anAcknowledgedAlertCanThenBeResolvedWithAComment() {
        Alert alert = openAlert(AlertSeverity.WARNING);
        controller.acknowledgeAlert(alert.getId(), null, sessionRequest());

        assertEquals(HttpStatus.OK, controller
                .resolveAlert(alert.getId(), Map.of("notes", "Sample re-run"), sessionRequest()).getStatusCode());

        Alert stored = alertService.get(alert.getId());
        assertEquals(AlertStatus.RESOLVED, stored.getStatus());
        assertEquals(String.valueOf(FIXTURE_USER), stored.getResolvedBy().getId());
        assertNotNull(stored.getResolvedAt());
        assertEquals("Sample re-run", stored.getResolutionNotes());
    }

    @Test
    public void resolvingWithoutACommentIsRefused() {
        Alert alert = openAlert(AlertSeverity.WARNING);
        controller.acknowledgeAlert(alert.getId(), null, sessionRequest());

        assertEquals(HttpStatus.BAD_REQUEST,
                controller.resolveAlert(alert.getId(), null, sessionRequest()).getStatusCode());
        assertEquals(AlertStatus.ACKNOWLEDGED, alertService.get(alert.getId()).getStatus());
    }

    @Test
    public void anAlertIsAcknowledgedBeforeItIsResolved() {
        Alert alert = openAlert(AlertSeverity.WARNING);

        assertEquals(HttpStatus.CONFLICT, controller
                .resolveAlert(alert.getId(), Map.of("notes", "Skipping ahead"), sessionRequest()).getStatusCode());
        assertEquals(AlertStatus.OPEN, alertService.get(alert.getId()).getStatus());
    }

    @Test
    public void acknowledgingAnAlertThatIsNotOpenIsRefused() {
        Alert alert = openAlert(AlertSeverity.WARNING);
        controller.acknowledgeAlert(alert.getId(), Map.of("notes", "First call"), sessionRequest());

        assertEquals(HttpStatus.CONFLICT, controller
                .acknowledgeAlert(alert.getId(), Map.of("notes", "Second call"), sessionRequest()).getStatusCode());

        Alert stored = alertService.get(alert.getId());
        assertEquals(AlertStatus.ACKNOWLEDGED, stored.getStatus());
        assertEquals("First call", stored.getAcknowledgmentNotes());
    }

    private Alert openAlert(AlertSeverity severity) {
        return alertService.createAlert(AlertType.STAT_OVERDUE, "Sample", ENTITY_IDS.incrementAndGet(), severity,
                "STAT order overdue", "{}");
    }

    private MockHttpServletRequest sessionRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(FIXTURE_USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return request;
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
