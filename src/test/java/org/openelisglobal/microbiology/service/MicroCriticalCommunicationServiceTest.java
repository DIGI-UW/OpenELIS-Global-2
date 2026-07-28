package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.alert.service.AlertService;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertSeverity;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationTargetType;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.result.service.ResultService;

/**
 * M-11 (FR-018): the clinical critical-communication log stays the record of
 * truth; a generic {@link Alert} row is a surfacing projection into the
 * existing Alerts Dashboard rather than a second, parallel alerts system
 * (Constitution Principle X: no dual-write of the same record).
 */
@RunWith(MockitoJUnitRunner.class)
public class MicroCriticalCommunicationServiceTest {

    @Mock
    private MicroCriticalCommunicationDAO communicationDAO;

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    @Mock
    private MicroIsolateDAO isolateDAO;

    @Mock
    private ResultService resultService;

    @Mock
    private AlertService alertService;

    private MicroCriticalCommunicationService service;

    @Before
    public void setUp() {
        service = new MicroCriticalCommunicationServiceImpl(communicationDAO, caseDAO, activityDAO, isolateDAO,
                resultService, alertService);
    }

    @Test
    public void logsCriticalCommunicationWithFreeTextRecipientAndActivity() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        MicroCriticalCommunication communication = service.logCommunication("case-1", "Provider on call",
                "Positive blood culture called", true, "1");

        assertEquals("case-1", communication.getCaseId());
        assertEquals("Provider on call", communication.getRecipient());
        assertEquals(MicroCriticalCommunicationStatus.OPEN.name(), communication.getAcknowledgementStatus());
        verify(communicationDAO).insert(communication);
        ArgumentCaptor<MicroCaseActivity> activity = ArgumentCaptor.forClass(MicroCaseActivity.class);
        verify(activityDAO).insert(activity.capture());
        assertEquals(MicroCaseActivityType.CRITICAL_COMMUNICATION_LOGGED.name(), activity.getValue().getActivityType());
    }

    @Test
    public void logCommunicationSurfacesADashboardAlertProjection() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        MicroCriticalCommunication communication = service.logCommunication("case-1", "Provider on call",
                "Positive blood culture called", true, "1");

        verify(alertService).createAlert(eq(AlertType.MICROBIOLOGY_CRITICAL), eq("MicrobiologyCriticalCommunication"),
                eq(communication.getId()), eq(AlertSeverity.CRITICAL), eq("Positive blood culture called"),
                any(String.class));
    }

    @Test
    public void acknowledgeSetsAckStateWithoutChangingMessage() {
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setId("comm-1");
        communication.setMessage("Positive blood culture called");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        when(communicationDAO.get("comm-1")).thenReturn(Optional.of(communication));

        MicroCriticalCommunication acknowledged = service.acknowledge("comm-1", "2");

        assertEquals(MicroCriticalCommunicationStatus.ACKNOWLEDGED.name(), acknowledged.getAcknowledgementStatus());
        assertEquals("Positive blood culture called", acknowledged.getMessage());
        assertNotNull(acknowledged.getAcknowledgedAt());
        verify(communicationDAO).update(acknowledged);
    }

    @Test
    public void acknowledgeKeepsTheProjectedAlertInStep() {
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setId("comm-1");
        communication.setMessage("Positive blood culture called");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        when(communicationDAO.get("comm-1")).thenReturn(Optional.of(communication));
        Alert openAlert = new Alert();
        openAlert.setId(42L);
        openAlert.setStatus(AlertStatus.OPEN);
        when(alertService.getAlertsByEntityRef("MicrobiologyCriticalCommunication", "comm-1"))
                .thenReturn(List.of(openAlert));

        service.acknowledge("comm-1", "2");

        verify(alertService).acknowledgeAlert(42L, 2);
    }

    @Test
    public void acknowledgeSkipsAlreadyAcknowledgedProjectedAlerts() {
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setId("comm-1");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        when(communicationDAO.get("comm-1")).thenReturn(Optional.of(communication));
        Alert alreadyAcknowledged = new Alert();
        alreadyAcknowledged.setId(99L);
        alreadyAcknowledged.setStatus(AlertStatus.ACKNOWLEDGED);
        when(alertService.getAlertsByEntityRef("MicrobiologyCriticalCommunication", "comm-1"))
                .thenReturn(List.of(alreadyAcknowledged));

        service.acknowledge("comm-1", "2");

        verify(alertService, never()).acknowledgeAlert(any(Long.class), any(Integer.class));
    }

    @Test
    public void logsEverySupportedTargetAgainstTheCurrentCase() {
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setSampleItemId("sample-item-1");
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));
        MicroIsolate isolate = new MicroIsolate();
        isolate.setId("iso-1");
        isolate.setCaseId("case-1");
        when(isolateDAO.get("iso-1")).thenReturn(Optional.of(isolate));

        MicroCriticalCommunication communication = service.logCommunication("case-1",
                MicroCriticalCommunicationTargetType.ISOLATE, "iso-1", "Provider on call", "555-0100", "PHONE",
                "Positive culture called", true, "1");

        assertEquals(MicroCriticalCommunicationTargetType.ISOLATE.name(), communication.getTargetType());
        assertEquals("iso-1", communication.getTargetId());
        assertEquals("PHONE", communication.getCommunicationMethod());
    }

    @Test
    public void closeRequiresAcknowledgementAndResolvesTheProjection() {
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setId("comm-1");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.ACKNOWLEDGED.name());
        communication.setAlertId(42L);
        when(communicationDAO.get("comm-1")).thenReturn(Optional.of(communication));
        Alert acknowledgedAlert = new Alert();
        acknowledgedAlert.setId(42L);
        acknowledgedAlert.setStatus(AlertStatus.ACKNOWLEDGED);
        when(alertService.get(42L)).thenReturn(acknowledgedAlert);

        MicroCriticalCommunication closed = service.close("comm-1", "Provider documented follow-up", "2");

        assertEquals(MicroCriticalCommunicationStatus.CLOSED.name(), closed.getAcknowledgementStatus());
        assertEquals("Provider documented follow-up", closed.getResolutionNote());
        verify(alertService).resolveAlert(42L, 2, "Provider documented follow-up");
    }

    @Test
    public void alertAcknowledgementUpdatesTheClinicalRecordWithoutReprojecting() {
        MicroCriticalCommunication communication = new MicroCriticalCommunication();
        communication.setId("comm-1");
        communication.setCaseId("case-1");
        communication.setAcknowledgementStatus(MicroCriticalCommunicationStatus.OPEN.name());
        when(communicationDAO.get("comm-1")).thenReturn(Optional.of(communication));

        service.synchronizeAcknowledgementFromAlert("comm-1", "2");

        assertEquals(MicroCriticalCommunicationStatus.ACKNOWLEDGED.name(), communication.getAcknowledgementStatus());
        verify(alertService, never()).acknowledgeAlert(any(Long.class), any(Integer.class));
    }
}
