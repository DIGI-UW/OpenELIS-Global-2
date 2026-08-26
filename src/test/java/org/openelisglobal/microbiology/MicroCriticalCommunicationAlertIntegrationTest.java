package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCriticalCommunicationService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logging a microbiology critical communication surfaces a real row in the
 * generic Alerts Dashboard (through {@link AlertService}), and acknowledging
 * the communication keeps that row in step, without a second, disconnected
 * alerting path.
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

    private String sampleItemId;
    private String methodId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        methodId = fixtures.createMethodId();
        sampleItemId = fixtures.createSampleWithSampleItem("M11").getId();
        fixtures.createReferenceData(methodId);
    }

    @Test
    public void loggingACriticalCommunicationSurfacesAnOpenAlertsDashboardRow() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                fixtures.defaultUserId());

        MicroCriticalCommunication communication = communicationService.logCommunication(microCase.getId(),
                "Provider on call", "Positive blood culture called", true, fixtures.defaultUserId());

        List<Alert> alerts = alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId());
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals(AlertType.MICROBIOLOGY_CRITICAL, alert.getAlertType());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals("Positive blood culture called", alert.getMessage());
        assertTrue(alertService.getAll().stream().anyMatch(a -> a.getId().equals(alert.getId())));
    }

    @Test
    public void acknowledgingTheCommunicationAcknowledgesTheDashboardRow() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                fixtures.defaultUserId());
        MicroCriticalCommunication communication = communicationService.logCommunication(microCase.getId(),
                "Provider on call", "Positive blood culture called", true, fixtures.defaultUserId());

        communicationService.acknowledge(communication.getId(), fixtures.defaultUserId());

        List<Alert> alerts = alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId());
        assertEquals(1, alerts.size());
        assertEquals(AlertStatus.ACKNOWLEDGED, alerts.get(0).getStatus());
    }
}
