package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;

/**
 * Unit tests for AnalyzerLifecycleScheduler
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerLifecycleSchedulerTest {

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AnalyzerLifecycleScheduler scheduler;

    private Analyzer testAnalyzer1;
    private Analyzer testAnalyzer2;
    private Analyzer testAnalyzer3;

    @Before
    public void setUp() {
        // Setup test analyzers
        testAnalyzer1 = new Analyzer();
        testAnalyzer1.setId("ANALYZER-001");
        testAnalyzer1.setName("Test Analyzer 1");
        testAnalyzer1.setStatus(AnalyzerStatus.ACTIVE);
        testAnalyzer1.setLastActivatedDate(getDateDaysAgo(8));

        testAnalyzer2 = new Analyzer();
        testAnalyzer2.setId("ANALYZER-002");
        testAnalyzer2.setName("Test Analyzer 2");
        testAnalyzer2.setStatus(AnalyzerStatus.ACTIVE);
        testAnalyzer2.setLastActivatedDate(getDateDaysAgo(5));

        testAnalyzer3 = new Analyzer();
        testAnalyzer3.setId("ANALYZER-003");
        testAnalyzer3.setName("Test Analyzer 3");
        testAnalyzer3.setStatus(AnalyzerStatus.VALIDATION);
        testAnalyzer3.setLastActivatedDate(getDateDaysAgo(10));
    }

    /**
     * Test: Transition to MAINTENANCE after 7 days updates stage
     */
    @Test
    public void testTransitionToMaintenance_After7Days_UpdatesStage() {
        // Arrange
        List<Analyzer> analyzers = new ArrayList<>();
        analyzers.add(testAnalyzer1);

        when(analyzerService.getAll()).thenReturn(analyzers);
        when(analyzerService.update(any(Analyzer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        verify(analyzerService, times(1)).update(testAnalyzer1);
        assertEquals("Analyzer should be in OFFLINE status", AnalyzerStatus.OFFLINE, testAnalyzer1.getStatus());
    }

    /**
     * Test: Transition to MAINTENANCE before 7 days does not update
     */
    @Test
    public void testTransitionToMaintenance_Before7Days_NoUpdate() {
        // Arrange
        List<Analyzer> analyzers = new ArrayList<>();
        analyzers.add(testAnalyzer2);

        when(analyzerService.getAll()).thenReturn(analyzers);

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        verify(analyzerService, never()).update(any(Analyzer.class));
        assertEquals("Analyzer should remain in ACTIVE status", AnalyzerStatus.ACTIVE, testAnalyzer2.getStatus());
    }

    /**
     * Test: Transition to MAINTENANCE with multiple analyzers updates all eligible
     */
    @Test
    public void testTransitionToMaintenance_WithMultipleAnalyzers_UpdatesAll() {
        // Arrange
        Analyzer testAnalyzer4 = new Analyzer();
        testAnalyzer4.setId("ANALYZER-004");
        testAnalyzer4.setName("Test Analyzer 4");
        testAnalyzer4.setStatus(AnalyzerStatus.ACTIVE);
        testAnalyzer4.setLastActivatedDate(getDateDaysAgo(9)); // 9 days ago - should transition

        List<Analyzer> analyzers = new ArrayList<>();
        analyzers.add(testAnalyzer1); // 8 days ago - should transition
        analyzers.add(testAnalyzer2); // 5 days ago - should NOT transition
        analyzers.add(testAnalyzer3); // Wrong status - should NOT transition
        analyzers.add(testAnalyzer4); // 9 days ago - should transition

        when(analyzerService.getAll()).thenReturn(analyzers);
        when(analyzerService.update(any(Analyzer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        verify(analyzerService, times(2)).update(any(Analyzer.class));
        assertEquals("Analyzer1 should be in OFFLINE status", AnalyzerStatus.OFFLINE, testAnalyzer1.getStatus());
        assertEquals("Analyzer2 should remain in ACTIVE status", AnalyzerStatus.ACTIVE, testAnalyzer2.getStatus());
        assertEquals("Analyzer3 should remain in VALIDATION status", AnalyzerStatus.VALIDATION,
                testAnalyzer3.getStatus());
        assertEquals("Analyzer4 should be in OFFLINE status", AnalyzerStatus.OFFLINE, testAnalyzer4.getStatus());
    }

    /**
     * Test: Transition failure logs error and continues processing
     */
    @Test
    public void testTransitionFailure_LogsErrorAndContinuesProcessing() {
        // Arrange
        Analyzer testAnalyzer4 = new Analyzer();
        testAnalyzer4.setId("ANALYZER-004");
        testAnalyzer4.setName("Test Analyzer 4");
        testAnalyzer4.setStatus(AnalyzerStatus.ACTIVE);
        testAnalyzer4.setLastActivatedDate(getDateDaysAgo(9)); // 9 days ago - should transition

        List<Analyzer> analyzers = new ArrayList<>();
        analyzers.add(testAnalyzer1); // Will fail update
        analyzers.add(testAnalyzer4); // Should succeed

        when(analyzerService.getAll()).thenReturn(analyzers);
        when(analyzerService.update(testAnalyzer1)).thenThrow(new RuntimeException("Database error"));
        when(analyzerService.update(testAnalyzer4)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        // Both should be attempted, but only testAnalyzer4 should succeed
        verify(analyzerService, times(1)).update(testAnalyzer1);
        verify(analyzerService, times(1)).update(testAnalyzer4);
        // testAnalyzer4 should still be updated despite testAnalyzer1 failure
        assertEquals("Analyzer4 should be in OFFLINE status despite Analyzer1 failure", AnalyzerStatus.OFFLINE,
                testAnalyzer4.getStatus());
    }

    /**
     * Helper method to get date N days ago
     */
    private Date getDateDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -days);
        return cal.getTime();
    }
}
