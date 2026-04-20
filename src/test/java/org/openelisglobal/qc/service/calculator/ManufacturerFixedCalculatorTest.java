package org.openelisglobal.qc.service.calculator;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.qc.builder.QCControlLotBuilder;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Unit tests for ManufacturerFixedCalculator (T036) Tests fixed value usage
 * from manufacturer specifications (no calculation needed).
 *
 * Following Constitution V (TDD): Tests written FIRST, implementation follows
 */
public class ManufacturerFixedCalculatorTest {

    private ManufacturerFixedCalculator calculator;
    private QCControlLot controlLot;

    @Before
    public void setUp() {
        calculator = new ManufacturerFixedCalculator();
        controlLot = QCControlLotBuilder.create().withId("test-lot-1").withCalculationMethod("MANUFACTURER_FIXED")
                .withManufacturerValues(100.0, 5.0).build();
    }

    /**
     * Test supports method - should only support MANUFACTURER_FIXED
     */
    @Test
    public void testSupports_ManufacturerFixedMethod_ShouldReturnTrue() {
        assertTrue("Should support MANUFACTURER_FIXED", calculator.supports("MANUFACTURER_FIXED"));
    }

    @Test
    public void testSupports_OtherMethods_ShouldReturnFalse() {
        assertFalse("Should not support INITIAL_RUNS", calculator.supports("INITIAL_RUNS"));
        assertFalse("Should not support ROLLING", calculator.supports("ROLLING"));
    }

    /**
     * Test fixed value usage - should return manufacturer values without
     * calculation Per US6: Manufacturer fixed values should be immediately ACTIVE
     * and ready for evaluation
     */
    @Test
    public void testCalculate_WithManufacturerValues_ShouldReturnFixedValues() {
        // Arrange
        List<QCResult> results = new ArrayList<>(); // Empty list, values not needed

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should match manufacturer mean", 100.0, statistics.getMean().doubleValue(), 0.001);
        assertEquals("Standard deviation should match manufacturer SD", 5.0,
                statistics.getStandardDeviation().doubleValue(), 0.001);
        assertEquals("Num values should be 0 (not calculated from results)", Integer.valueOf(0),
                statistics.getNumValues());
        assertEquals("Calculation method should be MANUFACTURER_FIXED", "MANUFACTURER_FIXED",
                statistics.getCalculationMethod());
    }

    /**
     * Test that results list is not used (can be null or empty)
     */
    @Test
    public void testCalculate_WithNullResults_ShouldStillReturnFixedValues() {
        // Act
        QCStatistics statistics = calculator.calculate(controlLot, null);

        // Assert
        assertNotNull("Statistics should not be null even with null results", statistics);
        assertEquals("Mean should match manufacturer mean", 100.0, statistics.getMean().doubleValue(), 0.001);
        assertEquals("Standard deviation should match manufacturer SD", 5.0,
                statistics.getStandardDeviation().doubleValue(), 0.001);
    }

    /**
     * Test validation: manufacturer mean is required
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCalculate_WithNullManufacturerMean_ShouldThrowException() {
        // Arrange
        QCControlLot lotWithoutMean = QCControlLotBuilder.create().withId("test-lot-2")
                .withCalculationMethod("MANUFACTURER_FIXED").withManufacturerMean(null).withManufacturerStdDev(5.0)
                .build();
        List<QCResult> results = new ArrayList<>();

        // Act - should throw IllegalArgumentException
        calculator.calculate(lotWithoutMean, results);
    }

    /**
     * Test validation: manufacturer standard deviation is required
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCalculate_WithNullManufacturerStdDev_ShouldThrowException() {
        // Arrange
        QCControlLot lotWithoutStdDev = QCControlLotBuilder.create().withId("test-lot-3")
                .withCalculationMethod("MANUFACTURER_FIXED").withManufacturerMean(100.0).withManufacturerStdDev(null)
                .build();
        List<QCResult> results = new ArrayList<>();

        // Act - should throw IllegalArgumentException
        calculator.calculate(lotWithoutStdDev, results);
    }

    /**
     * Test validation: both manufacturer mean and SD are required
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCalculate_WithBothManufacturerValuesNull_ShouldThrowException() {
        // Arrange
        QCControlLot lotWithoutValues = QCControlLotBuilder.create().withId("test-lot-4")
                .withCalculationMethod("MANUFACTURER_FIXED").withManufacturerMean(null).withManufacturerStdDev(null)
                .build();
        List<QCResult> results = new ArrayList<>();

        // Act - should throw IllegalArgumentException
        calculator.calculate(lotWithoutValues, results);
    }

    /**
     * Test with different manufacturer values
     */
    @Test
    public void testCalculate_WithDifferentManufacturerValues_ShouldReturnCorrectValues() {
        // Arrange
        QCControlLot customLot = QCControlLotBuilder.create().withId("test-lot-5")
                .withCalculationMethod("MANUFACTURER_FIXED").withManufacturerValues(250.5, 12.3).build();
        List<QCResult> results = new ArrayList<>();

        // Act
        QCStatistics statistics = calculator.calculate(customLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should match custom manufacturer mean", 250.5, statistics.getMean().doubleValue(), 0.001);
        assertEquals("Standard deviation should match custom manufacturer SD", 12.3,
                statistics.getStandardDeviation().doubleValue(), 0.001);
    }

    /**
     * Test precision of manufacturer values
     */
    @Test
    public void testCalculate_WithHighPrecisionValues_ShouldPreservePrecision() {
        // Arrange
        QCControlLot precisionLot = QCControlLotBuilder.create().withId("test-lot-6")
                .withCalculationMethod("MANUFACTURER_FIXED").withManufacturerValues(100.123456, 5.987654).build();
        List<QCResult> results = new ArrayList<>();

        // Act
        QCStatistics statistics = calculator.calculate(precisionLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Mean should preserve precision", 100.123456, statistics.getMean().doubleValue(), 0.000001);
        assertEquals("Standard deviation should preserve precision", 5.987654,
                statistics.getStandardDeviation().doubleValue(), 0.000001);
    }

    /**
     * Test that control lot ID is correctly set in statistics
     */
    @Test
    public void testCalculate_ShouldSetControlLotIdCorrectly() {
        // Arrange
        List<QCResult> results = new ArrayList<>();

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertEquals("Control lot ID should match", "test-lot-1", statistics.getControlLotId());
    }

    /**
     * Test that calculation date is set
     */
    @Test
    public void testCalculate_ShouldSetCalculationDate() {
        // Arrange
        List<QCResult> results = new ArrayList<>();

        // Act
        QCStatistics statistics = calculator.calculate(controlLot, results);

        // Assert
        assertNotNull("Statistics should not be null", statistics);
        assertNotNull("Calculation date should be set", statistics.getCalculationDate());
    }
}
