package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerStatusTransitionServiceImpl.AnalyzerStatusChangeEvent;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for AnalyzerStatusTransitionService
 *
 *
 * Tests all status transition methods: - transitionToValidation -
 * transitionToActive - transitionToErrorPending - transitionToOffline -
 * transitionToActiveFromError - transitionToActiveFromOffline
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerStatusTransitionServiceTest {

    @Mock
    private AnalyzerService analyzerService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnalyzerStatusTransitionServiceImpl transitionService;

    private Analyzer testAnalyzer;

    @Before
    public void setUp() {
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
    }

    // === transitionToValidation Tests ===

    @Test
    public void testTransitionToValidation_FromSetup_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToValidation("1");

        assertEquals(AnalyzerStatus.VALIDATION, result.getStatus());
        verify(analyzerService).update(any(Analyzer.class));
        verify(eventPublisher).publishEvent(any(AnalyzerStatusChangeEvent.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToValidation_FromActive_ThrowsException() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToValidation("1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransitionToValidation_AnalyzerNotFound_ThrowsException() {
        when(analyzerService.get("999")).thenReturn(null);

        transitionService.transitionToValidation("999");
    }

    // === transitionToActive Tests ===

    @Test
    public void testTransitionToActive_FromValidation_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToActive("1");

        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getLastActivatedDate());
        verify(analyzerService).update(any(Analyzer.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToActive_FromSetup_ThrowsException() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToActive("1");
    }

    // === transitionToErrorPending Tests ===

    @Test
    public void testTransitionToErrorPending_FromActive_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToErrorPending("1");

        assertEquals(AnalyzerStatus.ERROR_PENDING, result.getStatus());
        verify(analyzerService).update(any(Analyzer.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToErrorPending_FromValidation_ThrowsException() {
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToErrorPending("1");
    }

    // === transitionToOffline Tests ===

    @Test
    public void testTransitionToOffline_FromActive_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToOffline("1");

        assertEquals(AnalyzerStatus.OFFLINE, result.getStatus());
        verify(analyzerService).update(any(Analyzer.class));
    }

    @Test
    public void testTransitionToOffline_FromErrorPending_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.ERROR_PENDING);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToOffline("1");

        assertEquals(AnalyzerStatus.OFFLINE, result.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToOffline_FromSetup_ThrowsException() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToOffline("1");
    }

    // === transitionToActiveFromError Tests ===

    @Test
    public void testTransitionToActiveFromError_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.ERROR_PENDING);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToActiveFromError("1");

        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
        verify(analyzerService).update(any(Analyzer.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToActiveFromError_FromActive_ThrowsException() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToActiveFromError("1");
    }

    // === transitionToActiveFromOffline Tests ===

    @Test
    public void testTransitionToActiveFromOffline_Succeeds() {
        testAnalyzer.setStatus(AnalyzerStatus.OFFLINE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        Analyzer result = transitionService.transitionToActiveFromOffline("1");

        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
        verify(analyzerService).update(any(Analyzer.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToActiveFromOffline_FromActive_ThrowsException() {
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToActiveFromOffline("1");
    }

    // === Event Publishing Tests ===

    @Test
    public void testStatusTransition_PublishesEventWithCorrectData() {
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        transitionService.transitionToValidation("1");

        ArgumentCaptor<AnalyzerStatusChangeEvent> eventCaptor = ArgumentCaptor
                .forClass(AnalyzerStatusChangeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        AnalyzerStatusChangeEvent event = eventCaptor.getValue();
        assertEquals("1", event.getAnalyzerId());
        assertEquals(AnalyzerStatus.SETUP, event.getOldStatus());
        assertEquals(AnalyzerStatus.VALIDATION, event.getNewStatus());
        assertEquals("First mapping created", event.getReason());
    }

    // === Complete Workflow Test ===

    @Test
    public void testCompleteWorkflow_AllTransitionsSucceed() {
        // Start in SETUP
        testAnalyzer.setStatus(AnalyzerStatus.SETUP);
        when(analyzerService.get("1")).thenReturn(testAnalyzer);

        // SETUP -> VALIDATION
        Analyzer result = transitionService.transitionToValidation("1");
        assertEquals(AnalyzerStatus.VALIDATION, result.getStatus());

        // VALIDATION -> ACTIVE
        testAnalyzer.setStatus(AnalyzerStatus.VALIDATION);
        result = transitionService.transitionToActive("1");
        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());

        // ACTIVE -> ERROR_PENDING
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        result = transitionService.transitionToErrorPending("1");
        assertEquals(AnalyzerStatus.ERROR_PENDING, result.getStatus());

        // ERROR_PENDING -> ACTIVE
        testAnalyzer.setStatus(AnalyzerStatus.ERROR_PENDING);
        result = transitionService.transitionToActiveFromError("1");
        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());

        // ACTIVE -> OFFLINE
        testAnalyzer.setStatus(AnalyzerStatus.ACTIVE);
        result = transitionService.transitionToOffline("1");
        assertEquals(AnalyzerStatus.OFFLINE, result.getStatus());

        // OFFLINE -> ACTIVE
        testAnalyzer.setStatus(AnalyzerStatus.OFFLINE);
        result = transitionService.transitionToActiveFromOffline("1");
        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
    }
}
