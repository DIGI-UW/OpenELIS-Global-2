package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.builder.QCControlLotBuilder;
import org.openelisglobal.qc.dao.QCControlLotDAO;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for QCResultService (T139) Tests QC result creation from ASTM
 * interface or manual entry
 *
 * Following Constitution V (TDD): Tests written FIRST, implementation follows
 */
@RunWith(MockitoJUnitRunner.class)
public class QCResultServiceTest {

    @Mock
    private QCResultDAO resultDAO;

    @Mock
    private QCControlLotDAO controlLotDAO;

    @Mock
    private QCStatisticsDAO statisticsDAO;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QCResultServiceImpl resultService;

    private QCControlLot testControlLot;
    private QCStatistics testStatistics;

    @Before
    public void setUp() {
        // Setup active control lot with statistics
        testControlLot = QCControlLotBuilder.create().withId("lot-001").withProductName("Glucose Control Level 1")
                .withLotNumber("LOT-2025-001").withTestId(1).withInstrumentId(1).withCalculationMethod("INITIAL_RUNS")
                .asActive().build();

        // Setup statistics with mean=100.0, SD=5.0
        testStatistics = new QCStatistics();
        testStatistics.setId("stats-001");
        testStatistics.setControlLotId("lot-001");
        testStatistics.setMean(new BigDecimal("100.0"));
        testStatistics.setStandardDeviation(new BigDecimal("5.0"));
        testStatistics.setCalculationMethod("INITIAL_RUNS");
    }

    /**
     * Test: CREATE QC Result with valid data Task Reference: T139
     *
     * Tests the primary integration point for Feature 004
     */
    @Test
    public void testCreateQCResult_WithValidData_ShouldCalculateZScoreAndPersist() {
        // Arrange
        when(controlLotDAO.get("lot-001")).thenReturn(Optional.of(testControlLot));
        when(statisticsDAO.findLatestByControlLot("lot-001")).thenReturn(testStatistics);
        when(resultDAO.insert(any(QCResult.class))).thenReturn("result-001");

        QCResult mockResult = new QCResult();
        mockResult.setId("result-001");
        mockResult.setResultValue(new BigDecimal("110.0"));
        mockResult.setZScore(new BigDecimal("2.0000")); // (110 - 100) / 5
        when(resultDAO.get("result-001")).thenReturn(Optional.of(mockResult));

        // Act
        QCResult result = resultService.createQCResult("1", // analyzerId
                "1", // testId
                "lot-001", // controlLotId
                "NORMAL", // controlLevel
                new BigDecimal("110.0"), // resultValue
                "mg/dL", // unit
                LocalDateTime.now() // timestamp
        );

        // Assert
        assertNotNull("Result should not be null", result);
        assertEquals("Result value should be 110.0", new BigDecimal("110.0"), result.getResultValue());
        assertEquals("Z-score should be 2.0", new BigDecimal("2.0000"), result.getZScore());

        verify(resultDAO, times(1)).insert(any(QCResult.class));
        verify(eventPublisher, times(1)).publishEvent(any(QCResultServiceImpl.QCResultCreatedEvent.class));
    }

    /**
     * Test: Z-score calculation Task Reference: T139
     *
     * Formula: (value - mean) / standard deviation
     */
    @Test
    public void testCreateQCResult_ShouldCalculateCorrectZScore() {
        // Arrange
        when(controlLotDAO.get("lot-001")).thenReturn(Optional.of(testControlLot));
        when(statisticsDAO.findLatestByControlLot("lot-001")).thenReturn(testStatistics);
        when(resultDAO.insert(any(QCResult.class))).thenAnswer(invocation -> {
            QCResult result = invocation.getArgument(0);
            // Verify z-score was calculated: (95 - 100) / 5 = -1.0
            assertEquals("Z-score should be -1.0000", new BigDecimal("-1.0000"), result.getZScore());
            return "result-002";
        });

        QCResult mockResult = new QCResult();
        mockResult.setId("result-002");
        mockResult.setZScore(new BigDecimal("-1.0000"));
        when(resultDAO.get("result-002")).thenReturn(Optional.of(mockResult));

        // Act
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("95.0"), // Below mean
                "mg/dL", LocalDateTime.now());

        // Assert verified in mock
        verify(resultDAO).insert(any(QCResult.class));
    }

    /**
     * Test: Event publishing for async rule evaluation Task Reference: T139
     *
     * QCResultCreatedEvent triggers async Westgard rule evaluation
     */
    @Test
    public void testCreateQCResult_ShouldPublishQCResultCreatedEvent() {
        // Arrange
        when(controlLotDAO.get("lot-001")).thenReturn(Optional.of(testControlLot));
        when(statisticsDAO.findLatestByControlLot("lot-001")).thenReturn(testStatistics);
        when(resultDAO.insert(any(QCResult.class))).thenReturn("result-003");

        QCResult mockResult = new QCResult();
        mockResult.setId("result-003");
        when(resultDAO.get("result-003")).thenReturn(Optional.of(mockResult));

        // Act
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());

        // Assert: Event was published for async rule evaluation
        verify(eventPublisher, times(1)).publishEvent(any(QCResultServiceImpl.QCResultCreatedEvent.class));
    }

    /**
     * Test: Control lot not found Task Reference: T139
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateQCResult_WithInvalidControlLot_ShouldThrowException() {
        // Arrange
        when(controlLotDAO.get("invalid-lot")).thenReturn(Optional.empty());

        // Act - should throw IllegalArgumentException
        resultService.createQCResult("1", "1", "invalid-lot", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }

    /**
     * Test: Control lot not ACTIVE Task Reference: T139
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateQCResult_WithExpiredControlLot_ShouldThrowException() {
        // Arrange: Control lot is EXPIRED
        QCControlLot expiredLot = QCControlLotBuilder.create().withId("lot-expired").asExpired().build();
        when(controlLotDAO.get("lot-expired")).thenReturn(Optional.of(expiredLot));

        // Act - should throw IllegalArgumentException
        resultService.createQCResult("1", "1", "lot-expired", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }

    /**
     * Test: Null result value Task Reference: T139
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateQCResult_WithNullResultValue_ShouldThrowException() {
        // Act - should throw IllegalArgumentException before any DAO calls
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", null, // null value
                "mg/dL", LocalDateTime.now());
    }

    /**
     * Test: Missing statistics for control lot Task Reference: T139
     *
     * Cannot calculate z-score without mean/SD
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateQCResult_WithMissingStatistics_ShouldThrowException() {
        // Arrange
        when(controlLotDAO.get("lot-001")).thenReturn(Optional.of(testControlLot));
        when(statisticsDAO.findLatestByControlLot("lot-001")).thenReturn(null); // No statistics

        // Act - should throw IllegalArgumentException
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("100.0"), "mg/dL",
                LocalDateTime.now());
    }

    /**
     * Test: Result persisted with correct attributes Task Reference: T139
     */
    @Test
    public void testCreateQCResult_ShouldPersistResultWithCorrectAttributes() {
        // Arrange
        when(controlLotDAO.get("lot-001")).thenReturn(Optional.of(testControlLot));
        when(statisticsDAO.findLatestByControlLot("lot-001")).thenReturn(testStatistics);

        LocalDateTime testTime = LocalDateTime.of(2025, 12, 1, 10, 30);
        when(resultDAO.insert(any(QCResult.class))).thenAnswer(invocation -> {
            QCResult result = invocation.getArgument(0);
            // Verify all attributes are set correctly
            assertEquals("Control lot ID should match", "lot-001", result.getControlLotId());
            assertEquals("Test ID should be 1", Integer.valueOf(1), result.getTestId());
            assertEquals("Instrument ID should be 1", Integer.valueOf(1), result.getInstrumentId());
            assertEquals("Result value should be 105.0", new BigDecimal("105.0"), result.getResultValue());
            assertEquals("Unit should be mg/dL", "mg/dL", result.getUnitOfMeasure());
            assertEquals("Status should be PENDING", "PENDING", result.getResultStatus());
            assertFalse("Non-conformity flag should be false initially", result.getNonConformityFlag());
            assertNotNull("Run date/time should be set", result.getRunDateTime());
            return "result-004";
        });

        QCResult mockResult = new QCResult();
        mockResult.setId("result-004");
        when(resultDAO.get("result-004")).thenReturn(Optional.of(mockResult));

        // Act
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("105.0"), "mg/dL", testTime);

        // Assert verified in mock
        verify(resultDAO).insert(any(QCResult.class));
    }

    /**
     * Test: High precision z-score calculation Task Reference: T139
     *
     * Z-score should be calculated to 4 decimal places
     */
    @Test
    public void testCreateQCResult_ShouldCalculateHighPrecisionZScore() {
        // Arrange
        when(controlLotDAO.get("lot-001")).thenReturn(Optional.of(testControlLot));
        when(statisticsDAO.findLatestByControlLot("lot-001")).thenReturn(testStatistics);
        when(resultDAO.insert(any(QCResult.class))).thenAnswer(invocation -> {
            QCResult result = invocation.getArgument(0);
            // (103.3 - 100) / 5 = 0.66
            assertEquals("Z-score should be 0.6600", new BigDecimal("0.6600"), result.getZScore());
            return "result-005";
        });

        QCResult mockResult = new QCResult();
        mockResult.setId("result-005");
        when(resultDAO.get("result-005")).thenReturn(Optional.of(mockResult));

        // Act
        resultService.createQCResult("1", "1", "lot-001", "NORMAL", new BigDecimal("103.3"), "mg/dL",
                LocalDateTime.now());

        // Assert verified in mock
        verify(resultDAO).insert(any(QCResult.class));
    }
}
