package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;

/**
 * Unit tests for ASTMQSegmentParser implementation
 * 
 * 
 * TDD Workflow (MANDATORY): - RED: Write failing test first (defines expected
 * behavior) - GREEN: Write minimal code to make test pass - REFACTOR: Improve
 * code quality while keeping tests green
 * 
 * Test Coverage Goal: >80% (measured via JaCoCo)
 * 
 * Test Naming: test{MethodName}_{Scenario}_{ExpectedResult}
 * 
 * Reference: ASTM LIS2-A2 specification for Q-segment format
 */
@RunWith(MockitoJUnitRunner.class)
public class ASTMQSegmentParserTest {

    private ASTMQSegmentParser parser;

    @Before
    public void setUp() {
        parser = new ASTMQSegmentParserImpl();
    }

    /**
     * Test: Parse valid Q-segment with all required fields
     * 
     * Verifies that parseQSegments() correctly extracts all fields from a valid
     * Q-segment including: test code, control lot number, control level, result
     * value, unit, and timestamp.
     * 
     * Q-segment format per ASTM LIS2-A2:
     * Q|sequence|test_code^control_lot^control_level|result_value|unit|timestamp|flag
     */
    @Test
    public void testParseQSegment_WithValidSegment_ParsesAllFields() {
        // Arrange
        // ASTM message with H-segment (header) and Q-segment (QC result)
        // H-segment contains instrument ID: H|\\^&|||INSTRUMENT_ID^MODEL^VERSION|...
        // Q-segment: Q|1|GLUCOSE^LOT123^N|105.5|mg/dL|20250127143000|N
        String astmMessage = "H|\\^&|||INSTRUMENT_ID^MODEL^VERSION|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT123^N|105.5|mg/dL|20250127143000|N\r";

        // Act
        List<QCSegmentData> results = parser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse one Q-segment", 1, results.size());

        QCSegmentData qcData = results.get(0);
        assertNotNull("QC data should not be null", qcData);
        assertEquals("Instrument ID should match", "INSTRUMENT_ID", qcData.getInstrumentId());
        assertEquals("Test code should match", "GLUCOSE", qcData.getTestCode());
        assertEquals("Control lot number should match", "LOT123", qcData.getControlLotNumber());
        assertEquals("Control level should match", "N", qcData.getControlLevel()); // N = Normal
        assertEquals("Result value should match", "105.5", qcData.getResultValue());
        assertEquals("Unit should match", "mg/dL", qcData.getUnit());
        assertNotNull("Timestamp should not be null", qcData.getTimestamp());
    }

    /**
     * Test: Extract instrument ID from H-segment header
     * 
     * Verifies that instrument ID is correctly extracted from ASTM message header
     * (H-segment) per FR-021 requirement.
     * 
     * H-segment format: H|\\^&|||MANUFACTURER^MODEL^VERSION|...
     */
    @Test
    public void testParseQSegment_ExtractsInstrumentId() {
        // Arrange
        String astmMessage = "H|\\^&|||ABC_ANALYZER^V2.0^1.0|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|CHOLESTEROL^LOT456^H|200.0|mg/dL|20250127143000|N\r";

        // Act
        List<QCSegmentData> results = parser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse one Q-segment", 1, results.size());
        assertEquals("Instrument ID should match", "ABC_ANALYZER", results.get(0).getInstrumentId());
    }

    /**
     * Test: Extract control level (Low/Normal/High)
     * 
     * Verifies that control level is correctly extracted from Q-segment. Control
     * level values: L (Low), N (Normal), H (High)
     */
    @Test
    public void testParseQSegment_ExtractsControlLevel() {
        // Arrange - Test all three control levels
        String astmMessageLow = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT1^L|85.0|mg/dL|20250127143000|N\r";

        String astmMessageNormal = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT1^N|105.5|mg/dL|20250127143000|N\r";

        String astmMessageHigh = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT1^H|125.0|mg/dL|20250127143000|N\r";

        // Act
        List<QCSegmentData> resultsLow = parser.parseQSegments(astmMessageLow);
        List<QCSegmentData> resultsNormal = parser.parseQSegments(astmMessageNormal);
        List<QCSegmentData> resultsHigh = parser.parseQSegments(astmMessageHigh);

        // Assert
        assertEquals("Low control level should be L", "L", resultsLow.get(0).getControlLevel());
        assertEquals("Normal control level should be N", "N", resultsNormal.get(0).getControlLevel());
        assertEquals("High control level should be H", "H", resultsHigh.get(0).getControlLevel());
    }

    /**
     * Test: Extract control lot number
     * 
     * Verifies that control lot number is correctly extracted from Q-segment.
     */
    @Test
    public void testParseQSegment_ExtractsLotNumber() {
        // Arrange
        String astmMessage = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|HEMOGLOBIN^QC_LOT_2025_001^N|14.2|g/dL|20250127143000|N\r";

        // Act
        List<QCSegmentData> results = parser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Control lot number should match", "QC_LOT_2025_001", results.get(0).getControlLotNumber());
    }

    /**
     * Test: Extract numeric result value
     * 
     * Verifies that numeric result values are correctly extracted from Q-segment.
     */
    @Test
    public void testParseQSegment_ExtractsResultValue() {
        // Arrange - Test numeric result
        String astmMessageNumeric = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT1^N|105.5|mg/dL|20250127143000|N\r";

        // Act
        List<QCSegmentData> resultsNumeric = parser.parseQSegments(astmMessageNumeric);

        // Assert
        assertEquals("Numeric result value should match", "105.5", resultsNumeric.get(0).getResultValue());
    }

    /**
     * Test: Extract qualitative result value
     * 
     * Verifies that qualitative result values are correctly extracted from
     * Q-segment per FR-021 requirement.
     */
    @Test
    public void testParseQSegment_ExtractsQualitativeResult() {
        // Arrange - Test qualitative result (e.g., POSITIVE/NEGATIVE)
        String astmMessageQualitative = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|HIV_TEST^LOT1^N|POSITIVE||20250127143000|N\r";

        // Act
        List<QCSegmentData> resultsQualitative = parser.parseQSegments(astmMessageQualitative);

        // Assert
        assertEquals("Qualitative result value should match", "POSITIVE", resultsQualitative.get(0).getResultValue());
    }

    /**
     * Test: Extract timestamp from Q-segment
     * 
     * Verifies that timestamp is correctly extracted and parsed from Q-segment.
     * Format: YYYYMMDDHHMMSS
     */
    @Test
    public void testParseQSegment_ExtractsTimestamp() {
        // Arrange
        String astmMessage = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT1^N|105.5|mg/dL|20250127143000|N\r";

        // Act
        List<QCSegmentData> results = parser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Timestamp should not be null", results.get(0).getTimestamp());
        // Timestamp should be parseable as date/time
        assertTrue("Timestamp should contain date/time info", results.get(0).getTimestamp().toString().length() > 0);
    }

    /**
     * Test: Parse multiple Q-segments from single message
     * 
     * Verifies that parseQSegments() correctly extracts multiple Q-segments from a
     * single ASTM message per FR-021 requirement.
     */
    @Test
    public void testParseQSegment_WithMultipleSegments_ParsesAll() {
        // Arrange - Message with multiple Q-segments
        String astmMessage = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^LOT1^L|85.0|mg/dL|20250127143000|N\r"
                + "Q|2|GLUCOSE^LOT1^N|105.5|mg/dL|20250127143001|N\r"
                + "Q|3|GLUCOSE^LOT1^H|125.0|mg/dL|20250127143002|N\r";

        // Act
        List<QCSegmentData> results = parser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse three Q-segments", 3, results.size());
        assertEquals("First result should be Low level", "L", results.get(0).getControlLevel());
        assertEquals("Second result should be Normal level", "N", results.get(1).getControlLevel());
        assertEquals("Third result should be High level", "H", results.get(2).getControlLevel());
    }

    /**
     * Test: Handle malformed Q-segment with missing fields
     * 
     * Verifies that parseQSegments() throws exception for malformed Q-segments with
     * missing required fields per FR-021 error handling requirement.
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testParseQSegment_WithMalformedSegment_ThrowsException() {
        // Arrange - Malformed Q-segment missing required fields
        String astmMessage = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE|||20250127143000|N\r"; // Missing control lot, level, result value, unit

        // Act
        parser.parseQSegments(astmMessage);

        // Assert - Exception should be thrown (expected annotation)
    }

    /**
     * Test: Handle message with no Q-segments
     * 
     * Verifies that parseQSegments() returns empty list when message contains no
     * Q-segments (patient result only, no QC results).
     */
    @Test
    public void testParseQSegment_WithNoQSegments_ReturnsEmptyList() {
        // Arrange - ASTM message with patient results only (H, P, O, R segments)
        String astmMessage = "H|\\^&|||INSTR^MODEL^V1|INST||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "P|1||PATIENT_ID|||M|19800101||\r" + "O|1||ORDER_ID|||20250127||||\r"
                + "R|1|^^^GLUCOSE|105.5|mg/dL|N\r";

        // Act
        List<QCSegmentData> results = parser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertTrue("Should return empty list when no Q-segments", results.isEmpty());
    }

    /**
     * Test: Handle empty or null message
     * 
     * Verifies that parseQSegments() handles edge cases gracefully.
     */
    @Test(expected = LIMSRuntimeException.class)
    public void testParseQSegment_WithNullMessage_ThrowsException() {
        // Act
        parser.parseQSegments(null);

        // Assert - Exception should be thrown (expected annotation)
    }

    @Test(expected = LIMSRuntimeException.class)
    public void testParseQSegment_WithEmptyMessage_ThrowsException() {
        // Act
        parser.parseQSegments("");

        // Assert - Exception should be thrown (expected annotation)
    }
}
