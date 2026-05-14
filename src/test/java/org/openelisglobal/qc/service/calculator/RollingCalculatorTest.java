package org.openelisglobal.qc.service.calculator;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.qc.builder.QCControlLotBuilder;
import org.openelisglobal.qc.builder.QCResultBuilder;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Unit tests for RollingCalculator (T035) Tests rolling window statistics
 * calculation using most recent N results.
 *
 * Following Constitution V (TDD): Tests written FIRST, implementation follows
 */
public class RollingCalculatorTest {

    private RollingCalculator calculator;
    private QCControlLot controlLot;

    @Before
    public void setUp() {
        calculator = new RollingCalculator();
        controlLot = QCControlLotBuilder.create().withId("test-lot-1").withCalculationMethod("ROLLING")
                .withInitialRunsCount(20).build();
    }

    /**
     * Test supports method - should only support ROLLING
     */
    @Test
    public void testSupports_RollingMethod_ShouldReturnTrue() {
        assertTrue("Should support ROLLING", calculator.supports("ROLLING"));
    }

    @Test
    public void testSupports_OtherMethods_ShouldReturnFalse() {
        assertFalse("Should not support INITIAL_RUNS", calculator.supports("INITIAL_RUNS"));
        assertFalse("Should not support MANUFACTURER_FIXED", calculator.supports("MANUFACTURER_FIXED"));
    }

    /**
     * Test rolling window calculation with exactly window size results
     */
    @Test
    public void testCalculate_WithExactlyWindowSizeResults_ShouldCalculateCorrectly() {
        // Arrange: Create 20 results with constant value 100.0
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should be 100.0", 100.0, statistics.getMean().doubleValue(), 0.01);
        assertEquals("Standard deviation should be 0.0 (all values same)", 0.0,
                statistics.getStandardDeviation().doubleValue(), 0.01);
        assertEquals("Num values should be 20", Integer.valueOf(20), statistics.getNumValues());
        assertEquals("Calculation method should be ROLLING", "ROLLING", statistics.getCalculationMethod());
    }

    /**
     * Test rolling window with more than window size - should only use most recent
     * N Per US6: Rolling window should recalculate with each new result using most
     * recent N
     */
    @Test
    public void testCalculate_WithMoreThanWindowSize_ShouldUseOnlyRecentN() {
        // Arrange: Create 30 results
        // Most recent 20 (first in list, assuming DESC order) have mean=100
        // Older 10 have mean=200
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }
        for (int i = 0; i < 10; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("200.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should be approximately 100.0 (only recent 20 used)", 100.0,
                statistics.getMean().doubleValue(), 0.1);
        assertEquals("Num values should be 20", Integer.valueOf(20), statistics.getNumValues());
    }

    /**
     * Test window size configuration - should use configured window size
     */
    @Test
    public void testCalculate_WithCustomWindowSize_ShouldUseConfiguredSize() {
        // Arrange: Set custom window size of 10
        QCControlLot customLot = QCControlLotBuilder.create().withId("test-lot-2").withCalculationMethod("ROLLING")
                .withInitialRunsCount(10).build();

        // Create 15 results: first 10 with mean=100, last 5 with mean=200
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }
        for (int i = 0; i < 5; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("200.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(customLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should be approximately 100.0 (only first 10 used)", 100.0,
                statistics.getMean().doubleValue(), 0.1);
        assertEquals("Num values should be 10", Integer.valueOf(10), statistics.getNumValues());
    }

    /**
     * Test insufficient data handling - should return null
     */
    @Test
    public void testCalculate_WithInsufficientData_ShouldReturnNull() {
        // Arrange: Create only 15 results (need 20)
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNull("Statistics should be null when insufficient data", statistics);
    }

    /**
     * Test with null results list
     */
    @Test
    public void testCalculate_WithNullResults_ShouldReturnNull() {
        // Act
        QCStatistics statistics = calculator.calculate(controlLot, null);

        // Assert
        assertNull("Statistics should be null when results is null", statistics);
    }

    /**
     * Test with empty results list
     */
    @Test
    public void testCalculate_WithEmptyResults_ShouldReturnNull() {
        // Arrange
        List<QCResult> results = new ArrayList<>();

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNull("Statistics should be null when results is empty", statistics);
    }

    /**
     * Test default window size when not specified (should default to 20)
     */
    @Test
    public void testCalculate_WithNullWindowSize_ShouldUseDefault20() {
        // Arrange
        QCControlLot lotWithoutSize = QCControlLotBuilder.create().withId("test-lot-3").withCalculationMethod("ROLLING")
                .withInitialRunsCount(null).build();
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(lotWithoutSize, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Num values should be 20 (default)", Integer.valueOf(20), statistics.getNumValues());
    }

    /**
     * Test rolling window updates correctly as new results arrive Simulates adding
     * new result and recalculating
     */
    @Test
    public void testCalculate_WithNewResult_ShouldRecalculateWindow() {
        // Arrange: Create initial 20 results with mean=100
        List<QCResult> initialResults = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            initialResults.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }

        // Calculate initial statistics
        QCStatistics initialStats = calculator.calculate(controlLot, initialResults);
        assertNotNull("Initial statistics should not be null", initialStats);
        assertEquals("Initial mean should be 100.0", 100.0, initialStats.getMean().doubleValue(), 0.01);

        // Add new result with value 200 at the beginning (most recent)
        List<QCResult> updatedResults = new ArrayList<>();
        updatedResults.add(QCResultBuilder.create().withResultValue(new BigDecimal("200.0")).build());
        updatedResults.addAll(initialResults.subList(0, 19)); // Keep only 19 old results

        // Calculate updated statistics
        QCStatistics updatedStats = calculator.calculate(controlLot, updatedResults);

        // Assert: Mean should have increased (window includes one 200, nineteen 100s)
        assertNotNull("Updated statistics should not be null", updatedStats);
        assertTrue("Mean should increase with new higher value",
                updatedStats.getMean().doubleValue() > initialStats.getMean().doubleValue());
        assertEquals("Updated mean should be approximately 105.0", 105.0, updatedStats.getMean().doubleValue(), 0.1);
    }

    /**
     * Test accuracy with known reference values Dataset (most recent 20): [95, 96,
     * 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
     * 113, 114] Known Mean: 104.5 Known Sample SD: ~5.7663
     */
    @Test
    public void testCalculate_WithReferenceDataset_ShouldMatchKnownValues() {
        // Arrange: Create known dataset
        List<QCResult> results = new ArrayList<>();
        for (int i = 95; i <= 114; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal(i)).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should be 104.5", 104.5, statistics.getMean().doubleValue(), 0.01);
        assertEquals("Standard deviation should be approximately 5.7663", 5.7663,
                statistics.getStandardDeviation().doubleValue(), 0.01);
    }

}
