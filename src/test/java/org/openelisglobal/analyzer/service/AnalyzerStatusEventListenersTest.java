package org.openelisglobal.analyzer.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.AllErrorsAcknowledgedEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.AllMappingsActivatedEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.ConnectionTestFailedEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.ConnectionTestSucceededEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.MappingCreatedEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.UnacknowledgedErrorCreatedEvent;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;

/**
 * Unit tests for AnalyzerStatusEventListeners
 *
 *
 * Tests all event listener methods trigger appropriate status transitions
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerStatusEventListenersTest {

    @Mock
    private AnalyzerStatusTransitionService transitionService;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AnalyzerStatusEventListeners eventListeners;

    private Analyzer testAnalyzer;

    @Before
    public void setUp() {
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");
    }

    // === onMappingCreated Tests ===

    @Test
    public void testOnMappingCreated_WhenInSetup_TriggersValidationTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        MappingCreatedEvent event = new MappingCreatedEvent(this, "1");
        eventListeners.onMappingCreated(event);

        verify(transitionService).transitionToValidation("1");
    }

    @Test
    public void testOnMappingCreated_WhenNotInSetup_DoesNotTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        MappingCreatedEvent event = new MappingCreatedEvent(this, "1");
        eventListeners.onMappingCreated(event);

        verify(transitionService, never()).transitionToValidation(anyString());
    }

    @Test
    public void testOnMappingCreated_WhenAnalyzerNotFound_DoesNotThrow() {
        when(analyzerService.get("999")).thenReturn(null);

        MappingCreatedEvent event = new MappingCreatedEvent(this, "999");
        eventListeners.onMappingCreated(event); // Should not throw
    }

    // === onAllMappingsActivated Tests ===

    @Test
    public void testOnAllMappingsActivated_WhenInValidation_TriggersActiveTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        AllMappingsActivatedEvent event = new AllMappingsActivatedEvent(this, "1");
        eventListeners.onAllMappingsActivated(event);

        verify(transitionService).transitionToActive("1");
    }

    @Test
    public void testOnAllMappingsActivated_WhenNotInValidation_DoesNotTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        AllMappingsActivatedEvent event = new AllMappingsActivatedEvent(this, "1");
        eventListeners.onAllMappingsActivated(event);

        verify(transitionService, never()).transitionToActive(anyString());
    }

    // === onUnacknowledgedErrorCreated Tests ===

    @Test
    public void testOnUnacknowledgedErrorCreated_WhenActive_TriggersErrorPendingTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        UnacknowledgedErrorCreatedEvent event = new UnacknowledgedErrorCreatedEvent(this, "1", "error-123");
        eventListeners.onUnacknowledgedErrorCreated(event);

        verify(transitionService).transitionToErrorPending("1");
    }

    @Test
    public void testOnUnacknowledgedErrorCreated_WhenNotActive_DoesNotTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        UnacknowledgedErrorCreatedEvent event = new UnacknowledgedErrorCreatedEvent(this, "1", "error-123");
        eventListeners.onUnacknowledgedErrorCreated(event);

        verify(transitionService, never()).transitionToErrorPending(anyString());
    }

    // === onConnectionTestFailed Tests ===

    @Test
    public void testOnConnectionTestFailed_WhenActive_TriggersOfflineTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        ConnectionTestFailedEvent event = new ConnectionTestFailedEvent(this, "1", "Connection timeout");
        eventListeners.onConnectionTestFailed(event);

        verify(transitionService).transitionToOffline("1");
    }

    @Test
    public void testOnConnectionTestFailed_WhenErrorPending_TriggersOfflineTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.ERROR_PENDING);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        ConnectionTestFailedEvent event = new ConnectionTestFailedEvent(this, "1", "Connection refused");
        eventListeners.onConnectionTestFailed(event);

        verify(transitionService).transitionToOffline("1");
    }

    @Test
    public void testOnConnectionTestFailed_WhenValidation_DoesNotTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        ConnectionTestFailedEvent event = new ConnectionTestFailedEvent(this, "1", "Connection failed");
        eventListeners.onConnectionTestFailed(event);

        verify(transitionService, never()).transitionToOffline(anyString());
    }

    // === onAllErrorsAcknowledged Tests ===

    @Test
    public void testOnAllErrorsAcknowledged_WhenErrorPending_TriggersActiveTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.ERROR_PENDING);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        AllErrorsAcknowledgedEvent event = new AllErrorsAcknowledgedEvent(this, "1");
        eventListeners.onAllErrorsAcknowledged(event);

        verify(transitionService).transitionToActiveFromError("1");
    }

    @Test
    public void testOnAllErrorsAcknowledged_WhenActive_DoesNotTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        AllErrorsAcknowledgedEvent event = new AllErrorsAcknowledgedEvent(this, "1");
        eventListeners.onAllErrorsAcknowledged(event);

        verify(transitionService, never()).transitionToActiveFromError(anyString());
    }

    // === onConnectionTestSucceeded Tests ===

    @Test
    public void testOnConnectionTestSucceeded_WhenOffline_TriggersActiveTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.OFFLINE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        ConnectionTestSucceededEvent event = new ConnectionTestSucceededEvent(this, "1");
        eventListeners.onConnectionTestSucceeded(event);

        verify(transitionService).transitionToActiveFromOffline("1");
    }

    @Test
    public void testOnConnectionTestSucceeded_WhenActive_DoesNotTransition() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        ConnectionTestSucceededEvent event = new ConnectionTestSucceededEvent(this, "1");
        eventListeners.onConnectionTestSucceeded(event);

        verify(transitionService, never()).transitionToActiveFromOffline(anyString());
    }

    // === Error Handling Tests ===

    @Test
    public void testOnMappingCreated_WhenTransitionFails_DoesNotThrow() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);
        when(transitionService.transitionToValidation("1")).thenThrow(new RuntimeException("Transition failed"));

        MappingCreatedEvent event = new MappingCreatedEvent(this, "1");
        eventListeners.onMappingCreated(event); // Should not throw, error is logged
    }

    // === Event Data Tests ===

    @Test
    public void testMappingCreatedEvent_ContainsAnalyzerId() {
        MappingCreatedEvent event = new MappingCreatedEvent(this, "1");
        org.junit.Assert.assertEquals("1", event.getAnalyzerId());
    }

    @Test
    public void testUnacknowledgedErrorCreatedEvent_ContainsErrorId() {
        UnacknowledgedErrorCreatedEvent event = new UnacknowledgedErrorCreatedEvent(this, "1", "error-123");
        org.junit.Assert.assertEquals("1", event.getAnalyzerId());
        org.junit.Assert.assertEquals("error-123", event.getErrorId());
    }

    @Test
    public void testConnectionTestFailedEvent_ContainsReason() {
        ConnectionTestFailedEvent event = new ConnectionTestFailedEvent(this, "1", "Connection timeout");
        org.junit.Assert.assertEquals("1", event.getAnalyzerId());
        org.junit.Assert.assertEquals("Connection timeout", event.getReason());
    }
}
