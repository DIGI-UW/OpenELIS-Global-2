package org.openelisglobal.microbiology.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerEventPersistenceService;
import org.openelisglobal.analyzer.service.AnalyzerEventRegistration;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerReadingRequestForm;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerResultRequestForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;

@RunWith(MockitoJUnitRunner.class)
public class MicroAstAnalyzerEventServiceTest {

    @Mock
    private AnalyzerEventPersistenceService persistenceService;

    @Mock
    private MicroAstRunDAO runDAO;

    @Mock
    private MicroAstService astService;

    private MicroAstAnalyzerEventService service;

    @Before
    public void setUp() {
        service = new MicroAstAnalyzerEventService(persistenceService, runDAO, astService, new ObjectMapper());
    }

    @Test
    public void routesAnAstResultByAnalyzerAndCardAndMarksTheEnvelopeApplied() {
        AnalyzerEvent event = event("event-1", "RECEIVED");
        when(persistenceService.createIfAbsent(any())).thenReturn(new AnalyzerEventRegistration(event, true));
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        when(runDAO.getByAnalyzerAndCard("7", "card-42")).thenReturn(Optional.of(run));
        when(astService.applyAnalyzerResults(any(), any())).thenReturn(run);

        AnalyzerEvent result = service.receive(command("event-1", "7", "card-42", null), "1");

        ArgumentCaptor<MicroAstAnalyzerResultBatch> batch = ArgumentCaptor.forClass(MicroAstAnalyzerResultBatch.class);
        verify(astService).applyAnalyzerResults(batch.capture(), org.mockito.ArgumentMatchers.eq("1"));
        assertEquals("run-1", batch.getValue().runId());
        assertEquals("event-1", batch.getValue().sourceEventId());
        verify(persistenceService).markApplied(event, "run-1");
        assertSame(event, result);
    }

    @Test
    public void routesAnalyzerQcFailureToTheMatchedRunAndRetainsTheEventEvidence() {
        AnalyzerEvent event = event("event-qc-1", "RECEIVED");
        when(persistenceService.createIfAbsent(any())).thenReturn(new AnalyzerEventRegistration(event, true));
        MicroAstRun run = new MicroAstRun();
        run.setId("run-1");
        when(runDAO.getByAnalyzerAndCard("7", "card-42")).thenReturn(Optional.of(run));
        MicroAstAnalyzerResultRequestForm payload = new MicroAstAnalyzerResultRequestForm();
        payload.instrumentQcReference = "qc-17";
        payload.analyzerMessageCodes = List.of("CONTROL_OUT_OF_RANGE");
        MicroAstAnalyzerEventCommand command = new MicroAstAnalyzerEventCommand("event-qc-1", "AST_QC_FAIL", "7",
                "card-42", null, payload);

        AnalyzerEvent result = service.receive(command, "42");

        verify(astService).recordAnalyzerQcFailure("run-1", "qc-17", List.of("CONTROL_OUT_OF_RANGE"), "event-qc-1",
                "42");
        verify(astService, never()).applyAnalyzerResults(any(), any());
        verify(persistenceService).markApplied(event, "run-1");
        assertSame(event, result);
    }

    @Test
    public void unmatchedEventIsRetainedForExistingAnalyzerReconciliationSurface() {
        AnalyzerEvent event = event("event-2", "RECEIVED");
        when(persistenceService.createIfAbsent(any())).thenReturn(new AnalyzerEventRegistration(event, true));
        when(runDAO.getByAnalyzerAndCard("7", "unknown-card")).thenReturn(Optional.empty());

        service.receive(command("event-2", "7", "unknown-card", null), "1");

        verify(persistenceService).markFailed(event, "AST_ANALYZER_RUN_NOT_MATCHED");
        verify(astService, never()).applyAnalyzerResults(any(), any());
    }

    @Test
    public void alreadyProcessedExternalEventIsIdempotent() {
        AnalyzerEvent applied = event("event-3", "APPLIED");
        when(persistenceService.createIfAbsent(any())).thenReturn(new AnalyzerEventRegistration(applied, false));

        AnalyzerEvent result = service.receive(command("event-3", "7", "card-42", "run-1"), "1");

        assertSame(applied, result);
        verify(astService, never()).applyAnalyzerResults(any(), any());
        verify(persistenceService, never()).markApplied(any(), any());
    }

    @Test
    public void concurrentReceivedDuplicateDoesNotApplyAnalyzerResultsAgain() {
        AnalyzerEvent received = event("event-4", "RECEIVED");
        when(persistenceService.createIfAbsent(any())).thenReturn(new AnalyzerEventRegistration(received, false));

        AnalyzerEvent result = service.receive(command("event-4", "7", "card-42", "run-1"), "1");

        assertSame(received, result);
        verify(astService, never()).applyAnalyzerResults(any(), any());
        verify(persistenceService, never()).markApplied(any(), any());
        verify(persistenceService, never()).markFailed(any(), any());
    }

    private MicroAstAnalyzerEventCommand command(String externalId, String analyzerId, String sourceId,
            String targetRunId) {
        MicroAstAnalyzerResultRequestForm payload = new MicroAstAnalyzerResultRequestForm();
        payload.analyzerInstrumentId = analyzerId;
        payload.analyzerCardId = sourceId;
        payload.qcPassed = true;
        MicroAstAnalyzerReadingRequestForm reading = new MicroAstAnalyzerReadingRequestForm();
        reading.antibioticId = "abx-1";
        reading.rawValue = new BigDecimal("4");
        reading.instrumentInterpretation = "SUSCEPTIBLE";
        payload.readings = List.of(reading);
        return new MicroAstAnalyzerEventCommand(externalId, "AST_RESULT_AVAILABLE", analyzerId, sourceId, targetRunId,
                payload);
    }

    private AnalyzerEvent event(String externalId, String status) {
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId(externalId);
        event.setStatus(status);
        return event;
    }
}
