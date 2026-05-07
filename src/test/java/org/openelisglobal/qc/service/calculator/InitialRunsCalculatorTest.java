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
 * Unit tests for InitialRunsCalculator (T034) Tests mean and standard deviation
 * calculation using first N runs for control lot establishment.
 *
 * Following Constitution V (TDD): Tests written FIRST, implementation follows
 */
public class InitialRunsCalculatorTest {

    private InitialRunsCalculator calculator;
    private QCControlLot controlLot;

    @Before
    public void setUp() {
        calculator = new InitialRunsCalculator();
        controlLot = QCControlLotBuilder.create().withId("test-lot-1").withCalculationMethod("INITIAL_RUNS")
                .withInitialRunsCount(20).build();
    }

    /**
     * Test supports method - should only support INITIAL_RUNS
     */
    @Test
    public void testSupports_InitialRunsMethod_ShouldReturnTrue() {
        assertTrue("Should support INITIAL_RUNS", calculator.supports("INITIAL_RUNS"));
    }

    @Test
    public void testSupports_OtherMethods_ShouldReturnFalse() {
        assertFalse("Should not support ROLLING", calculator.supports("ROLLING"));
        assertFalse("Should not support MANUFACTURER_FIXED", calculator.supports("MANUFACTURER_FIXED"));
    }

    /**
     * Test mean and standard deviation calculation with known reference dataset
     * Reference: 20 results with mean=100.0, SD=0.0 (all values same for
     * simplicity)
     */
    @Test
    public void testCalculate_WithExactly20Results_ShouldCalculateCorrectMeanAndSD() {
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
        assertEquals("Calculation method should be INITIAL_RUNS", "INITIAL_RUNS", statistics.getCalculationMethod());
    }

    /**
     * Test with more than required runs - should only use first N
     */
    @Test
    public void testCalculate_WithMoreThanRequiredRuns_ShouldUseOnlyFirstN() {
        // Arrange: Create 30 results, first 20 with mean=100, last 10 with mean=200
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
        assertEquals("Mean should be approximately 100.0 (only first 20 used)", 100.0,
                statistics.getMean().doubleValue(), 0.1);
        assertEquals("Num values should be 20", Integer.valueOf(20), statistics.getNumValues());
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
     * Test default initial runs count when not specified (should default to 20)
     */
    @Test
    public void testCalculate_WithNullInitialRunsCount_ShouldUseDefault20() {
        // Arrange
        QCControlLot lotWithoutCount = QCControlLotBuilder.create().withId("test-lot-2")
                .withCalculationMethod("INITIAL_RUNS").withInitialRunsCount(null).build();
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(lotWithoutCount, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Num values should be 20 (default)", Integer.valueOf(20), statistics.getNumValues());
    }

    /**
     * Test with zero initial runs count (should default to 20)
     */
    @Test
    public void testCalculate_WithZeroInitialRunsCount_ShouldUseDefault20() {
        // Arrange
        QCControlLot lotWithZeroCount = QCControlLotBuilder.create().withId("test-lot-3")
                .withCalculationMethod("INITIAL_RUNS").withInitialRunsCount(0).build();
        List<QCResult> results = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            results.add(QCResultBuilder.create().withResultValue(new BigDecimal("100.0")).build());
        }

        // Act
        QCStatistics statistics = calculator.calculate(lotWithZeroCount, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Num values should be 20 (default)", Integer.valueOf(20), statistics.getNumValues());
    }

    /**
     * Test accuracy with known reference values Dataset: [95, 96, 97, 98, 99, 100,
     * 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114] Known
     * Mean: 104.5 Known Sample SD: ~5.7663
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
