package org.openelisglobal.microbiology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * M-11 (FR-018) end-to-end: logging a microbiology critical communication
 * surfaces a real row in the generic Alerts Dashboard (through
 * {@link AlertService}), and acknowledging the communication keeps that row in
 * step, without a second, disconnected alerting path.
 */
public class MicroCriticalCommunicationAlertIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ALERT_ENTITY_TYPE = "MicrobiologyCriticalCommunication";

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private MicroCaseService caseService;

    @Autowired
    private MicroCriticalCommunicationService communicationService;

    @Autowired
    private AlertService alertService;

    private MicrobiologyTestFixtures fixtures;
    private String sampleItemId;
    private String methodId;
    private MicroCriticalCommunication communication;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        fixtures = new MicrobiologyTestFixtures(new JdbcTemplate(dataSource));
        methodId = fixtures.firstMethodId();
        sampleItemId = fixtures.insertSampleWithSampleItem("M11" + (System.nanoTime() % 100000000L));
        fixtures.insertReferenceData(methodId);
    }

    @After
    public void tearDown() {
        if (communication != null) {
            for (Alert alert : alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId())) {
                alertService.delete(alert);
            }
        }
        if (fixtures != null && sampleItemId != null) {
            fixtures.deleteCaseDataForSampleItem(sampleItemId);
            fixtures.deleteSampleItemAndSample(sampleItemId);
            fixtures.deleteReferenceData();
        }
    }

    @Test
    public void loggingACriticalCommunicationSurfacesAnOpenAlertsDashboardRow() {
        MicroCase microCase = caseService.createOrGetCase(sampleItemId, MicroWorkflowType.BACTERIOLOGY, methodId,
                MicrobiologyTestFixtures.DEFAULT_USER_ID);

        communication = communicationService.logCommunication(microCase.getId(), "Provider on call",
                "Positive blood culture called", true, MicrobiologyTestFixtures.DEFAULT_USER_ID);

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
                MicrobiologyTestFixtures.DEFAULT_USER_ID);
        communication = communicationService.logCommunication(microCase.getId(), "Provider on call",
                "Positive blood culture called", true, MicrobiologyTestFixtures.DEFAULT_USER_ID);

        communicationService.acknowledge(communication.getId(), MicrobiologyTestFixtures.DEFAULT_USER_ID);

        List<Alert> alerts = alertService.getAlertsByEntityRef(ALERT_ENTITY_TYPE, communication.getId());
        assertEquals(1, alerts.size());
        assertEquals(AlertStatus.ACKNOWLEDGED, alerts.get(0).getStatus());
    }
}
