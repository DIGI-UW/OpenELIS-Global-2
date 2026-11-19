package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCriticalCommunicationService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationTargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logging a microbiology critical call that needs follow-up surfaces a real row
 * in the generic Alerts Dashboard (through {@link AlertService}), and resolving
 * the follow-up keeps that row in step, without a second, disconnected alerting
 * path.
 */
@Transactional
public class MicroCriticalCommunicationAlertIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ALERT_ENTITY_TYPE = "MicrobiologyCriticalCommunication";

    @Autowired
    private MicrobiologyTestFixtures fixtures;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCriticalCommunicationService communicationService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private MicroCaseDAO caseDAO;

    private MicroCase microCase;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        String methodId = fixtures.createMethodId();
        String sampleItemId = fixtures.createSampleWithSampleItem("M11").getId();
        fixtures.createReferenceData(methodId);
        microCase = caseService.createOrGetCase(sampleItemId, methodId, fixtures.defaultUserId());
        microCase.setLabUnitId(fixtures.createLabUnit().getId());
        caseDAO.update(microCase);
    }

    @Test
    public void aCallNeedingFollowUpSurfacesAnOpenAlertsDashboardRow() {
        MicroCriticalCommunication communication = log("UNABLE_TO_REACH");

        List<Alert> alerts = alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId());
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals(AlertType.MICROBIOLOGY_CRITICAL, alert.getAlertType());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals("Positive blood culture called", alert.getMessage());
        assertTrue(alertService.getAll().stream().anyMatch(a -> a.getId().equals(alert.getId())));
    }

    @Test
    public void aCallWithoutFollowUpRaisesNoDashboardAlert() {
        MicroCriticalCommunication communication = log("CONFIRMED", false);

        assertTrue(alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId()).isEmpty());
        assertNull(communication.getAlertId());
    }

    @Test
    public void loggingWithoutAnOutcomeIsRejected() {
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> log(null));
        assertEquals("outcome is required", missing.getMessage());
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class, () -> log(" "));
        assertEquals("outcome is required", blank.getMessage());
        assertTrue(communicationService.getByCaseId(microCase.getId()).isEmpty());
    }

    @Test
    public void loggingWithAnUnknownOutcomeIsRejected() {
        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> log("LEFT_VOICEMAIL"));
        assertEquals("outcome is invalid", unknown.getMessage());
        assertTrue(communicationService.getByCaseId(microCase.getId()).isEmpty());
    }

    @Test
    public void theOutcomeIsStoredAndReturned() throws Exception {
        String body = "{\"targetType\":\"CASE\",\"recipient\":\"Provider on call\",\"communicationMethod\":\"PHONE\","
                + "\"outcome\":\"REACHED_NO_READBACK\",\"message\":\"Positive blood culture called\","
                + "\"followUpNeeded\":true}";

        mockMvc.perform(post("/rest/microbiology/cases/" + microCase.getId() + "/critical-communications")
                .requestAttr(IActionConstants.USER_SESSION_DATA, session()).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("REACHED_NO_READBACK"));

        assertEquals("REACHED_NO_READBACK", communicationService.getByCaseId(microCase.getId()).get(0).getOutcome());
        mockMvc.perform(get("/rest/microbiology/cases/" + microCase.getId() + "/critical-communications")
                .requestAttr(IActionConstants.USER_SESSION_DATA, session())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].outcome").value("REACHED_NO_READBACK"));
    }

    @Test
    public void loggingOverHttpWithoutAnOutcomeIsABadRequest() throws Exception {
        String body = "{\"targetType\":\"CASE\",\"recipient\":\"Provider on call\",\"communicationMethod\":\"PHONE\","
                + "\"message\":\"Positive blood culture called\",\"followUpNeeded\":true}";

        mockMvc.perform(post("/rest/microbiology/cases/" + microCase.getId() + "/critical-communications")
                .requestAttr(IActionConstants.USER_SESSION_DATA, session()).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("outcome is required"));
    }

    @Test
    public void closingAnOpenCommunicationWithANoteClosesItAndResolvesItsDashboardAlert() {
        MicroCriticalCommunication communication = log("UNABLE_TO_REACH");

        MicroCriticalCommunication closed = communicationService.close(communication.getId(),
                "Reached the ward on the second call", fixtures.defaultUserId());

        assertEquals(MicroCriticalCommunicationStatus.CLOSED.name(), closed.getAcknowledgementStatus());
        assertEquals("Reached the ward on the second call", closed.getResolutionNote());
        List<Alert> alerts = alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId());
        assertEquals(1, alerts.size());
        assertEquals(AlertStatus.RESOLVED, alerts.get(0).getStatus());
    }

    @Test
    public void closingWithoutANoteIsRejected() {
        MicroCriticalCommunication communication = log("UNABLE_TO_REACH");

        assertThrows(IllegalArgumentException.class,
                () -> communicationService.close(communication.getId(), " ", fixtures.defaultUserId()));
    }

    @Test
    public void closingACallWithoutFollowUpIsRejected() {
        MicroCriticalCommunication communication = log("CONFIRMED", false);

        assertThrows(IllegalStateException.class, () -> communicationService.close(communication.getId(),
                "Nothing to follow up", fixtures.defaultUserId()));
        assertEquals(MicroCriticalCommunicationStatus.OPEN.name(),
                communicationService.getByCaseId(microCase.getId()).get(0).getAcknowledgementStatus());
    }

    @Test
    public void dashboardAcknowledgementStillSyncsBackAndTheCommunicationCanThenBeClosed() {
        MicroCriticalCommunication communication = log("REACHED_NO_READBACK");
        Alert alert = alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId()).get(0);

        alertService.acknowledgeAlert(alert.getId(), Integer.valueOf(fixtures.defaultUserId()));

        assertEquals(MicroCriticalCommunicationStatus.ACKNOWLEDGED.name(),
                communicationService.getByCaseId(microCase.getId()).get(0).getAcknowledgementStatus());
        communicationService.close(communication.getId(), "Read-back documented", fixtures.defaultUserId());
        assertEquals(AlertStatus.RESOLVED, alertService.get(alert.getId()).getStatus());
    }

    private MicroCriticalCommunication log(String outcome) {
        return log(outcome, true);
    }

    private MicroCriticalCommunication log(String outcome, boolean followUpNeeded) {
        return communicationService.logCommunication(microCase.getId(), MicroCriticalCommunicationTargetType.CASE,
                microCase.getId(), "Provider on call", null, "PHONE", outcome, "Positive blood culture called",
                followUpNeeded, fixtures.defaultUserId());
    }

    private UserSessionData session() {
        UserSessionData session = new UserSessionData();
        session.setSytemUserId(Integer.parseInt(fixtures.createScenarioActor()));
        return session;
    }
}
