package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.builder.QCResultBuilder;
import org.openelisglobal.qc.builder.QCStatisticsBuilder;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Unit tests for QCStatisticsService (TDD - RED phase) Tests statistical
 * calculations per US6 (Manage QC Control Lots)
 *
 * Following Constitution V (TDD): Write tests FIRST, ensure they FAIL before
 * implementation
 */
@RunWith(MockitoJUnitRunner.class)
public class QCStatisticsServiceTest {

    @Mock
    private QCStatisticsDAO statisticsDAO;

    @Mock
    private QCResultDAO resultDAO;

    @InjectMocks
    private QCStatisticsServiceImpl statisticsService;

    private String testControlLotId = "test-lot-1";
    private QCStatistics testStatistics;

    @Before
    public void setUp() {
        testStatistics = QCStatisticsBuilder.create().withControlLotId(testControlLotId).withMean("100.0")
                .withStandardDeviation("5.0").withNumValues(20).build();
    }

    /**
     * Test calculating statistics from initial runs Per US6: Collect N runs,
     * calculate mean and SD, activate lot
     */
    @Test
    public void testCalculateInitialRunsStatistics_ShouldCalculateMeanAndStdDev() {
        // Arrange
        List<QCResult> initialResults = createTestResults("95.0", "100.0", "105.0", "98.0", "102.0", "99.0", "101.0",
                "97.0", "103.0", "100.0", "96.0", "104.0", "98.0", "102.0", "100.0", "99.0", "101.0", "97.0", "103.0",
                "100.0");
        when(resultDAO.findByControlLot(testControlLotId)).thenReturn(initialResults);
        when(statisticsDAO.insert(any(QCStatistics.class))).thenReturn(testStatistics);

        // Act
        QCStatistics result = statisticsService.calculateInitialRunsStatistics(testControlLotId, 20);

        // Assert
        assertNotNull("Statistics should not be null", result);
        assertNotNull("Mean should be calculated", result.getMean());
        assertNotNull("Standard deviation should be calculated", result.getStandardDeviation());
        assertEquals("Should use all 20 values", Integer.valueOf(20), result.getNumValues());
        assertEquals("Calculation method should be INITIAL_RUNS", "INITIAL_RUNS", result.getCalculationMethod());
        verify(statisticsDAO, times(1)).insert(any(QCStatistics.class));
    }

    /**
     * Test calculating rolling window statistics Per US6: Use most recent N results
     * to calculate mean/SD
     */
    @Test
    public void testCalculateRollingStatistics_ShouldUseMostRecentResults() {
        // Arrange
        List<QCResult> recentResults = createTestResults("98.0", "100.0", "102.0", "99.0", "101.0", "100.0", "97.0",
                "103.0", "100.0", "99.0");
        when(resultDAO.findByControlLot(testControlLotId)).thenReturn(recentResults);
        when(statisticsDAO.insert(any(QCStatistics.class))).thenReturn(testStatistics);

        // Act
        QCStatistics result = statisticsService.calculateRollingStatistics(testControlLotId, 10);

        // Assert
        assertNotNull("Statistics should not be null", result);
        assertNotNull("Mean should be calculated", result.getMean());
        assertNotNull("Standard deviation should be calculated", result.getStandardDeviation());
        assertEquals("Should use 10 most recent values", Integer.valueOf(10), result.getNumValues());
        assertEquals("Calculation method should be ROLLING", "ROLLING", result.getCalculationMethod());
        verify(statisticsDAO, times(1)).insert(any(QCStatistics.class));
    }

    /**
     * Test getting latest statistics for a control lot
     * Per US6: Retrieve current active statistics
     */
    @Test
    public void testGetLatestStatistics_ShouldReturnMostRecent() {
        // Arrange
        when(statisticsDAO.findLatestByControlLot(testControlLotId)).thenReturn(testStatistics);

        // Act
        QCStatistics result = statisticsService.getLatestStatistics(testControlLotId);

        // Assert
        assertNotNull("Latest statistics should not be null", result);
        assertEquals("Should return test statistics", testStatistics.getId(), result.getId());
        verify(statisticsDAO, times(1)).findLatestByControlLot(testControlLotId);
    }

    /**
     * Test calculating mean from result values Per US6: Mean = sum of values /
     * count
     */
    @Test
    public void testCalculateMean_ShouldReturnCorrectAverage() {
        // Arrange
        List<QCResult> results = createTestResults("100.0", "110.0", "90.0");

        // Act
        BigDecimal mean = statisticsService.calculateMean(results);

        // Assert
        assertNotNull("Mean should not be null", mean);
        assertEquals("Mean should be 100.0", new BigDecimal("100.0"), mean.setScale(1, BigDecimal.ROUND_HALF_UP));
    }

    /**
     * Test calculating standard deviation from result values Per US6: SD =
     * sqrt(sum((x - mean)^2) / (n-1))
     */
    @Test
    public void testCalculateStandardDeviation_ShouldReturnCorrectSD() {
        // Arrange
        List<QCResult> results = createTestResults("95.0", "100.0", "105.0");

        // Act
        BigDecimal stdDev = statisticsService.calculateStandardDeviation(results);

        // Assert
        assertNotNull("Standard deviation should not be null", stdDev);
        assertTrue("Standard deviation should be positive", stdDev.compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Test validation: insufficient data for initial runs Per US6: Should reject if
     * not enough results collected
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateInitialRunsStatistics_InsufficientData_ShouldThrowException() {
        // Arrange
        List<QCResult> insufficientResults = createTestResults("100.0", "101.0", "99.0");
        when(resultDAO.findByControlLot(testControlLotId)).thenReturn(insufficientResults);

        // Act
        statisticsService.calculateInitialRunsStatistics(testControlLotId, 20); // Should throw
    }

    /**
     * Test validation: empty result set Per US6: Should reject if no results
     * available
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCalculateMean_EmptyResults_ShouldThrowException() {
        // Arrange
        List<QCResult> emptyResults = Arrays.asList();

        // Act
        statisticsService.calculateMean(emptyResults); // Should throw
    }

    /**
     * Test getting statistics by calculation method Per US6: Support different
     * calculation approaches
     */
    @Test
    public void testGetStatisticsByMethod_ShouldFilterByMethod() {
        // Arrange
        QCStatistics initialRunsStats = QCStatisticsBuilder.create().asInitialRuns(20).build();
        when(statisticsDAO.findByCalculationMethod(testControlLotId, "INITIAL_RUNS"))
                .thenReturn(Arrays.asList(initialRunsStats));

        // Act
        List<QCStatistics> results = statisticsService.getStatisticsByMethod(testControlLotId, "INITIAL_RUNS");

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 1 statistics record", 1, results.size());
        assertEquals("Method should be INITIAL_RUNS", "INITIAL_RUNS", results.get(0).getCalculationMethod());
        verify(statisticsDAO, times(1)).findByCalculationMethod(testControlLotId, "INITIAL_RUNS");
    }

    /**
     * Test updating statistics with validity period Per US6: Track when statistics
     * were calculated and their validity
     */
    @Test
    public void testUpdateStatistics_ShouldSetValidityDates() {
        // Arrange
        Timestamp now = new Timestamp(System.currentTimeMillis());
        testStatistics.setValidityStart(now);
        when(statisticsDAO.update(any(QCStatistics.class))).thenReturn(testStatistics);

        // Act
        QCStatistics result = statisticsService.updateStatistics(testStatistics);

        // Assert
        assertNotNull("Updated statistics should not be null", result);
        assertNotNull("Validity start should be set", result.getValidityStart());
        verify(statisticsDAO, times(1)).update(any(QCStatistics.class));
    }

    /**
     * Test invalidating old statistics when new ones are calculated Per US6: Only
     * one active statistics set per control lot
     */
    @Test
    public void testInvalidateOldStatistics_ShouldSetValidityEnd() {
        // Arrange
        QCStatistics oldStatistics = QCStatisticsBuilder.create().withControlLotId(testControlLotId)
                .withValidityStart(new Timestamp(System.currentTimeMillis() - 86400000L)).build();
        when(statisticsDAO.findLatestByControlLot(testControlLotId)).thenReturn(oldStatistics);
        when(statisticsDAO.update(any(QCStatistics.class))).thenReturn(oldStatistics);

        // Act
        statisticsService.invalidateOldStatistics(testControlLotId);

        // Assert
        verify(statisticsDAO, times(1)).findLatestByControlLot(testControlLotId);
        verify(statisticsDAO, times(1)).update(argThat(stats -> stats.getValidityEnd() != null));
    }

    // Helper method to create test results
    private List<QCResult> createTestResults(String... values) {
        return Arrays.stream(values)
                .map(value -> QCResultBuilder.create().withControlLotId(testControlLotId).withResultValue(value)
                        .withRunDateTime(new Timestamp(System.currentTimeMillis())).build())
                .collect(java.util.stream.Collectors.toList());
    }
}
