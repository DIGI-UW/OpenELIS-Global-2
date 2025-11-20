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
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;

/**
 * Unit tests for AnalyzerLifecycleScheduler
 * 
 * Task Reference: T153a, T153b
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerLifecycleSchedulerTest {

    @Mock
    private AnalyzerConfigurationService analyzerConfigurationService;

    @InjectMocks
    private AnalyzerLifecycleScheduler scheduler;

    private Analyzer testAnalyzer1;
    private Analyzer testAnalyzer2;
    private Analyzer testAnalyzer3;
    private AnalyzerConfiguration config1;
    private AnalyzerConfiguration config2;
    private AnalyzerConfiguration config3;

    @Before
    public void setUp() {
        // Setup test analyzers
        testAnalyzer1 = new Analyzer();
        testAnalyzer1.setId("ANALYZER-001");
        testAnalyzer1.setName("Test Analyzer 1");

        testAnalyzer2 = new Analyzer();
        testAnalyzer2.setId("ANALYZER-002");
        testAnalyzer2.setName("Test Analyzer 2");

        testAnalyzer3 = new Analyzer();
        testAnalyzer3.setId("ANALYZER-003");
        testAnalyzer3.setName("Test Analyzer 3");

        // Setup configurations
        config1 = new AnalyzerConfiguration();
        config1.setId("CONFIG-001");
        config1.setAnalyzer(testAnalyzer1);
        config1.setLifecycleStage(AnalyzerConfiguration.LifecycleStage.GO_LIVE);
        config1.setLastActivatedDate(getDateDaysAgo(8)); // 8 days ago - should transition

        config2 = new AnalyzerConfiguration();
        config2.setId("CONFIG-002");
        config2.setAnalyzer(testAnalyzer2);
        config2.setLifecycleStage(AnalyzerConfiguration.LifecycleStage.GO_LIVE);
        config2.setLastActivatedDate(getDateDaysAgo(5)); // 5 days ago - should NOT transition

        config3 = new AnalyzerConfiguration();
        config3.setId("CONFIG-003");
        config3.setAnalyzer(testAnalyzer3);
        config3.setLifecycleStage(AnalyzerConfiguration.LifecycleStage.VALIDATION); // Not GO_LIVE
        config3.setLastActivatedDate(getDateDaysAgo(10)); // Should not transition (wrong stage)
    }

    /**
     * Test: Transition to MAINTENANCE after 7 days updates stage Task Reference:
     * T153a
     */
    @Test
    public void testTransitionToMaintenance_After7Days_UpdatesStage() {
        // Arrange
        List<AnalyzerConfiguration> configurations = new ArrayList<>();
        configurations.add(config1);

        when(analyzerConfigurationService.getAllWithAnalyzers()).thenReturn(configurations);
        when(analyzerConfigurationService.update(any(AnalyzerConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        verify(analyzerConfigurationService, times(1)).update(config1);
        assertEquals("Configuration should be in MAINTENANCE stage", AnalyzerConfiguration.LifecycleStage.MAINTENANCE,
                config1.getLifecycleStage());
    }

    /**
     * Test: Transition to MAINTENANCE before 7 days does not update Task Reference:
     * T153a
     */
    @Test
    public void testTransitionToMaintenance_Before7Days_NoUpdate() {
        // Arrange
        List<AnalyzerConfiguration> configurations = new ArrayList<>();
        configurations.add(config2);

        when(analyzerConfigurationService.getAllWithAnalyzers()).thenReturn(configurations);

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        verify(analyzerConfigurationService, never()).update(any(AnalyzerConfiguration.class));
        assertEquals("Configuration should remain in GO_LIVE stage", AnalyzerConfiguration.LifecycleStage.GO_LIVE,
                config2.getLifecycleStage());
    }

    /**
     * Test: Transition to MAINTENANCE with multiple analyzers updates all eligible
     * Task Reference: T153a
     */
    @Test
    public void testTransitionToMaintenance_WithMultipleAnalyzers_UpdatesAll() {
        // Arrange
        AnalyzerConfiguration config4 = new AnalyzerConfiguration();
        config4.setId("CONFIG-004");
        config4.setAnalyzer(testAnalyzer3);
        config4.setLifecycleStage(AnalyzerConfiguration.LifecycleStage.GO_LIVE);
        config4.setLastActivatedDate(getDateDaysAgo(9)); // 9 days ago - should transition

        List<AnalyzerConfiguration> configurations = new ArrayList<>();
        configurations.add(config1); // 8 days ago - should transition
        configurations.add(config2); // 5 days ago - should NOT transition
        configurations.add(config3); // Wrong stage - should NOT transition
        configurations.add(config4); // 9 days ago - should transition

        when(analyzerConfigurationService.getAllWithAnalyzers()).thenReturn(configurations);
        when(analyzerConfigurationService.update(any(AnalyzerConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        verify(analyzerConfigurationService, times(2)).update(any(AnalyzerConfiguration.class));
        assertEquals("Config1 should be in MAINTENANCE stage", AnalyzerConfiguration.LifecycleStage.MAINTENANCE,
                config1.getLifecycleStage());
        assertEquals("Config2 should remain in GO_LIVE stage", AnalyzerConfiguration.LifecycleStage.GO_LIVE,
                config2.getLifecycleStage());
        assertEquals("Config3 should remain in VALIDATION stage", AnalyzerConfiguration.LifecycleStage.VALIDATION,
                config3.getLifecycleStage());
        assertEquals("Config4 should be in MAINTENANCE stage", AnalyzerConfiguration.LifecycleStage.MAINTENANCE,
                config4.getLifecycleStage());
    }

    /**
     * Test: Transition failure logs error and continues processing Task Reference:
     * T153b
     */
    @Test
    public void testTransitionFailure_LogsErrorAndContinuesProcessing() {
        // Arrange
        AnalyzerConfiguration config4 = new AnalyzerConfiguration();
        config4.setId("CONFIG-004");
        config4.setAnalyzer(testAnalyzer3);
        config4.setLifecycleStage(AnalyzerConfiguration.LifecycleStage.GO_LIVE);
        config4.setLastActivatedDate(getDateDaysAgo(9)); // 9 days ago - should transition

        List<AnalyzerConfiguration> configurations = new ArrayList<>();
        configurations.add(config1); // Will fail update
        configurations.add(config4); // Should succeed

        when(analyzerConfigurationService.getAllWithAnalyzers()).thenReturn(configurations);
        when(analyzerConfigurationService.update(config1)).thenThrow(new RuntimeException("Database error"));
        when(analyzerConfigurationService.update(config4)).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        scheduler.transitionToMaintenance();

        // Assert
        // Both should be attempted, but only config4 should succeed
        verify(analyzerConfigurationService, times(1)).update(config1);
        verify(analyzerConfigurationService, times(1)).update(config4);
        // Config4 should still be updated despite config1 failure
        assertEquals("Config4 should be in MAINTENANCE stage despite config1 failure",
                AnalyzerConfiguration.LifecycleStage.MAINTENANCE, config4.getLifecycleStage());
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
