package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;

@RunWith(MockitoJUnitRunner.class)
public class MicroCriticalCommunicationServiceTest {

    @Mock
    private MicroCriticalCommunicationDAO communicationDAO;

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseActivityDAO activityDAO;

    private MicroCriticalCommunicationService service;

    @Before
    public void setUp() {
        service = new MicroCriticalCommunicationServiceImpl(communicationDAO, caseDAO, activityDAO);
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
}
