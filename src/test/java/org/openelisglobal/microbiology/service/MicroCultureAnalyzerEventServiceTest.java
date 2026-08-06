package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerEventPersistenceService;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;

@RunWith(MockitoJUnitRunner.class)
public class MicroCultureAnalyzerEventServiceTest {

    @Mock
    private AnalyzerEventPersistenceService persistenceService;

    @Mock
    private MicroCaseDAO caseDAO;

    @Mock
    private MicroCaseInoculationDAO inoculationDAO;

    @Mock
    private MicroCaseStateService stateService;

    private MicroCultureAnalyzerEventService service;

    @Before
    public void setUp() {
        service = new MicroCultureAnalyzerEventService(persistenceService, caseDAO, inoculationDAO, stateService,
                new ObjectMapper());
    }

    @Test
    public void positiveSignalResolvesContainerAndAdvancesTheCase() {
        AnalyzerEvent received = receivedEvent();
        MicroCaseInoculation inoculation = new MicroCaseInoculation();
        inoculation.setCaseId("case-1");
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        microCase.setStage(MicroCaseStage.INCUBATING.name());
        when(persistenceService.createIfAbsent(any(AnalyzerEvent.class))).thenReturn(received);
        when(inoculationDAO.getByContainerIdentifier("bottle-42")).thenReturn(List.of(inoculation));
        when(caseDAO.get("case-1")).thenReturn(Optional.of(microCase));

        AnalyzerEvent result = service.receive(new MicroCultureAnalyzerEventCommand("evt-1", "POSITIVE_SIGNAL",
                "analyzer-7", "bottle-42", null, "Instrument flagged positive"), "bridge-user");

        assertEquals("evt-1", result.getExternalEventId());
        verify(stateService).advanceStage("case-1", MicroCaseStage.POSITIVE_SIGNAL, "bridge-user",
                "Instrument flagged positive");
        verify(persistenceService).markApplied(received, "case-1");
    }

    @Test
    public void ambiguousContainerIsRetainedForReconciliation() {
        AnalyzerEvent received = receivedEvent();
        MicroCaseInoculation first = new MicroCaseInoculation();
        first.setCaseId("case-1");
        MicroCaseInoculation second = new MicroCaseInoculation();
        second.setCaseId("case-2");
        when(persistenceService.createIfAbsent(any(AnalyzerEvent.class))).thenReturn(received);
        when(inoculationDAO.getByContainerIdentifier("bottle-42")).thenReturn(List.of(first, second));

        service.receive(
                new MicroCultureAnalyzerEventCommand("evt-1", "POSITIVE_SIGNAL", "analyzer-7", "bottle-42", null, null),
                "bridge-user");

        verify(stateService, never()).advanceStage(any(String.class), any(MicroCaseStage.class), any(String.class),
                any(String.class));
        verify(persistenceService).markFailed(received, "CULTURE_ANALYZER_SOURCE_AMBIGUOUS");
    }

    private AnalyzerEvent receivedEvent() {
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId("evt-1");
        event.setStatus("RECEIVED");
        return event;
    }
}
