package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.AllErrorsAcknowledgedEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.ConnectionTestFailedEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.ConnectionTestSucceededEvent;
import org.openelisglobal.analyzer.service.AnalyzerStatusEventListeners.UnacknowledgedErrorCreatedEvent;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

    @Test
    public void testOnSetupVerified_WhenInSetup_TransitionsThroughValidationToActive() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        eventListeners.onAnalyzerSetupVerified(new AnalyzerSetupVerifiedEvent(this, "1"));

        verify(transitionService).transitionToValidation("1");
        verify(transitionService).transitionToActive("1");
    }

    @Test
    public void testOnSetupVerified_WhenInValidation_TransitionsToActive() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        eventListeners.onAnalyzerSetupVerified(new AnalyzerSetupVerifiedEvent(this, "1"));

        verify(transitionService, never()).transitionToValidation(anyString());
        verify(transitionService).transitionToActive("1");
    }

    @Test
    public void testOnSetupVerified_IsHandledAfterVerificationTransactionCommits() throws Exception {
        Method listenerMethod = AnalyzerStatusEventListeners.class.getMethod("onAnalyzerSetupVerified",
                AnalyzerSetupVerifiedEvent.class);

        TransactionalEventListener listener = listenerMethod.getAnnotation(TransactionalEventListener.class);
        Transactional transaction = listenerMethod.getAnnotation(Transactional.class);

        assertNotNull(listener);
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
        assertNotNull(transaction);
        assertEquals(Propagation.REQUIRES_NEW, transaction.propagation());
    }

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

    // === Event Data Tests ===

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
