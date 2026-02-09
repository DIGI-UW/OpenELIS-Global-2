package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerStatusTransitionServiceImpl.AnalyzerStatusChangeEvent;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration.AnalyzerStatus;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for AnalyzerStatusTransitionService
 * 
 * Task Reference: T151d, T153a Test Coverage Goal: >80%
 * 
 * Tests all status transition methods: - transitionToValidation -
 * transitionToActive - transitionToErrorPending - transitionToOffline -
 * transitionToActiveFromError - transitionToActiveFromOffline
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerStatusTransitionServiceTest {

    @Mock
    private AnalyzerConfigurationService configurationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnalyzerStatusTransitionServiceImpl transitionService;

    private Analyzer testAnalyzer;
    private AnalyzerConfiguration testConfig;

    @Before
    public void setUp() {
        testAnalyzer = new Analyzer();
        testAnalyzer.setId("1");
        testAnalyzer.setName("Test Analyzer");

        testConfig = new AnalyzerConfiguration();
        testConfig.setId("CONFIG-001");
        testConfig.setAnalyzer(testAnalyzer);
        testConfig.setStatus(AnalyzerStatus.SETUP);
    }

    // === transitionToValidation Tests ===

    @Test
    public void testTransitionToValidation_FromSetup_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToValidation("1");

        assertEquals(AnalyzerStatus.VALIDATION, result.getStatus());
        verify(configurationService).update(any(AnalyzerConfiguration.class));
        verify(eventPublisher).publishEvent(any(AnalyzerStatusChangeEvent.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToValidation_FromActive_ThrowsException() {
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        transitionService.transitionToValidation("1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransitionToValidation_AnalyzerNotFound_ThrowsException() {
        when(configurationService.getByAnalyzerId("999")).thenReturn(Optional.empty());

        transitionService.transitionToValidation("999");
    }

    // === transitionToActive Tests ===

    @Test
    public void testTransitionToActive_FromValidation_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.VALIDATION);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToActive("1");

        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getLastActivatedDate());
        verify(configurationService).update(any(AnalyzerConfiguration.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToActive_FromSetup_ThrowsException() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        transitionService.transitionToActive("1");
    }

    // === transitionToErrorPending Tests ===

    @Test
    public void testTransitionToErrorPending_FromActive_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToErrorPending("1");

        assertEquals(AnalyzerStatus.ERROR_PENDING, result.getStatus());
        verify(configurationService).update(any(AnalyzerConfiguration.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToErrorPending_FromValidation_ThrowsException() {
        testConfig.setStatus(AnalyzerStatus.VALIDATION);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        transitionService.transitionToErrorPending("1");
    }

    // === transitionToOffline Tests ===

    @Test
    public void testTransitionToOffline_FromActive_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToOffline("1");

        assertEquals(AnalyzerStatus.OFFLINE, result.getStatus());
        verify(configurationService).update(any(AnalyzerConfiguration.class));
    }

    @Test
    public void testTransitionToOffline_FromErrorPending_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.ERROR_PENDING);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToOffline("1");

        assertEquals(AnalyzerStatus.OFFLINE, result.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToOffline_FromSetup_ThrowsException() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        transitionService.transitionToOffline("1");
    }

    // === transitionToActiveFromError Tests ===

    @Test
    public void testTransitionToActiveFromError_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.ERROR_PENDING);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToActiveFromError("1");

        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
        verify(configurationService).update(any(AnalyzerConfiguration.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToActiveFromError_FromActive_ThrowsException() {
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        transitionService.transitionToActiveFromError("1");
    }

    // === transitionToActiveFromOffline Tests ===

    @Test
    public void testTransitionToActiveFromOffline_Succeeds() {
        testConfig.setStatus(AnalyzerStatus.OFFLINE);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        AnalyzerConfiguration result = transitionService.transitionToActiveFromOffline("1");

        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
        verify(configurationService).update(any(AnalyzerConfiguration.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testTransitionToActiveFromOffline_FromActive_ThrowsException() {
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        transitionService.transitionToActiveFromOffline("1");
    }

    // === Event Publishing Tests ===

    @Test
    public void testStatusTransition_PublishesEventWithCorrectData() {
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

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
        testConfig.setStatus(AnalyzerStatus.SETUP);
        when(configurationService.getByAnalyzerId("1")).thenReturn(Optional.of(testConfig));

        // SETUP -> VALIDATION
        AnalyzerConfiguration result = transitionService.transitionToValidation("1");
        assertEquals(AnalyzerStatus.VALIDATION, result.getStatus());

        // VALIDATION -> ACTIVE
        testConfig.setStatus(AnalyzerStatus.VALIDATION);
        result = transitionService.transitionToActive("1");
        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());

        // ACTIVE -> ERROR_PENDING
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        result = transitionService.transitionToErrorPending("1");
        assertEquals(AnalyzerStatus.ERROR_PENDING, result.getStatus());

        // ERROR_PENDING -> ACTIVE
        testConfig.setStatus(AnalyzerStatus.ERROR_PENDING);
        result = transitionService.transitionToActiveFromError("1");
        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());

        // ACTIVE -> OFFLINE
        testConfig.setStatus(AnalyzerStatus.ACTIVE);
        result = transitionService.transitionToOffline("1");
        assertEquals(AnalyzerStatus.OFFLINE, result.getStatus());

        // OFFLINE -> ACTIVE
        testConfig.setStatus(AnalyzerStatus.OFFLINE);
        result = transitionService.transitionToActiveFromOffline("1");
        assertEquals(AnalyzerStatus.ACTIVE, result.getStatus());
    }
}
