package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for ASTMQSegmentParser implementation
 * 
 * 
 * Tests the complete Q-segment parsing workflow with Spring Boot context: -
 * Parse full ASTM message with Q-segments extracts QC data - Parse multiple
 * Q-segments from single message extracts all QC results
 * 
 * Uses @SpringBootTest via BaseWebContextSensitiveTest for full integration
 * with Spring container.
 */
public class ASTMQSegmentParserIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ASTMQSegmentParser astmQSegmentParser;

    /**
     * Test: Parse full ASTM message with Q-segments extracts QC data
     * 
     * Verifies that parseQSegments() correctly extracts QC data from full ASTM
     * message including H-segment (header) and Q-segments (QC results).
     */
    @Test
    public void testParseFullAstmMessage_WithQSegments_ExtractsQCData() {
        // Arrange - Full ASTM message with H-segment (header) and Q-segment (QC result)
        String astmMessage = "H|\\^&|||TEST_ANALYZER^MODEL_1.0^V1.0|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "P|1||PATIENT_001|||M|19800101||\r" + "O|1||ORDER_001|||20250127||||\r"
                + "R|1|^^^GLUCOSE|105.5|mg/dL|N\r" + "Q|1|GLUCOSE^QC_LOT_2025_001^N|105.2|mg/dL|20250127143000|N\r"
                + "L|1|N\r";

        // Act
        List<QCSegmentData> results = astmQSegmentParser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse one Q-segment", 1, results.size());

        QCSegmentData qcData = results.get(0);
        assertNotNull("QC data should not be null", qcData);
        assertEquals("Instrument ID should match", "TEST_ANALYZER", qcData.getInstrumentId());
        assertEquals("Test code should match", "GLUCOSE", qcData.getTestCode());
        assertEquals("Control lot number should match", "QC_LOT_2025_001", qcData.getControlLotNumber());
        assertEquals("Control level should match", "N", qcData.getControlLevel()); // N = Normal
        assertEquals("Result value should match", "105.2", qcData.getResultValue());
        assertEquals("Unit should match", "mg/dL", qcData.getUnit());
        assertNotNull("Timestamp should not be null", qcData.getTimestamp());
    }

    /**
     * Test: Parse multiple Q-segments from single message
     * 
     * Verifies that parseQSegments() correctly extracts all Q-segments from a
     * single ASTM message containing multiple QC results (e.g., multiple control
     * levels for same test or different tests).
     */
    @Test
    public void testParseMultipleQSegments_FromSingleMessage() {
        // Arrange - ASTM message with multiple Q-segments (Low, Normal, High control
        // levels)
        String astmMessage = "H|\\^&|||TEST_ANALYZER^MODEL_1.0^V1.0|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "P|1||PATIENT_001|||M|19800101||\r" + "O|1||ORDER_001|||20250127||||\r"
                + "R|1|^^^GLUCOSE|105.5|mg/dL|N\r" + "Q|1|GLUCOSE^QC_LOT_2025_001^L|85.0|mg/dL|20250127143000|N\r"
                + "Q|2|GLUCOSE^QC_LOT_2025_001^N|105.2|mg/dL|20250127143001|N\r"
                + "Q|3|GLUCOSE^QC_LOT_2025_001^H|125.0|mg/dL|20250127143002|N\r"
                + "Q|4|CHOLESTEROL^QC_LOT_2025_002^N|200.5|mg/dL|20250127143003|N\r" + "L|1|N\r";

        // Act
        List<QCSegmentData> results = astmQSegmentParser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse four Q-segments", 4, results.size());

        // Verify first Q-segment (Low control level)
        QCSegmentData qcData1 = results.get(0);
        assertEquals("First result should be Low level", "L", qcData1.getControlLevel());
        assertEquals("First result test code should be GLUCOSE", "GLUCOSE", qcData1.getTestCode());
        assertEquals("First result value should be 85.0", "85.0", qcData1.getResultValue());

        // Verify second Q-segment (Normal control level)
        QCSegmentData qcData2 = results.get(1);
        assertEquals("Second result should be Normal level", "N", qcData2.getControlLevel());
        assertEquals("Second result test code should be GLUCOSE", "GLUCOSE", qcData2.getTestCode());
        assertEquals("Second result value should be 105.2", "105.2", qcData2.getResultValue());

        // Verify third Q-segment (High control level)
        QCSegmentData qcData3 = results.get(2);
        assertEquals("Third result should be High level", "H", qcData3.getControlLevel());
        assertEquals("Third result test code should be GLUCOSE", "GLUCOSE", qcData3.getTestCode());
        assertEquals("Third result value should be 125.0", "125.0", qcData3.getResultValue());

        // Verify fourth Q-segment (Different test)
        QCSegmentData qcData4 = results.get(3);
        assertEquals("Fourth result test code should be CHOLESTEROL", "CHOLESTEROL", qcData4.getTestCode());
        assertEquals("Fourth result control lot should be QC_LOT_2025_002", "QC_LOT_2025_002",
                qcData4.getControlLotNumber());
        assertEquals("Fourth result value should be 200.5", "200.5", qcData4.getResultValue());

        // All should have same instrument ID from header
        for (QCSegmentData qcData : results) {
            assertEquals("All results should have same instrument ID", "TEST_ANALYZER", qcData.getInstrumentId());
            assertNotNull("All results should have timestamp", qcData.getTimestamp());
        }
    }

    /**
     * Test: Parse message with Q-segments only (no patient results)
     * 
     * Verifies that parseQSegments() correctly extracts Q-segments even when
     * message contains only QC results (no patient data).
     */
    @Test
    public void testParseMessage_WithQSegmentsOnly_ExtractsQCData() {
        // Arrange - ASTM message with only H-segment and Q-segments (QC-only run)
        String astmMessage = "H|\\^&|||TEST_ANALYZER^MODEL_1.0^V1.0|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "Q|1|GLUCOSE^QC_LOT_2025_001^N|105.2|mg/dL|20250127143000|N\r" + "L|1|N\r";

        // Act
        List<QCSegmentData> results = astmQSegmentParser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse one Q-segment", 1, results.size());
        assertEquals("Test code should match", "GLUCOSE", results.get(0).getTestCode());
        assertEquals("Instrument ID should match", "TEST_ANALYZER", results.get(0).getInstrumentId());
    }

    /**
     * Test: Parse message with mixed segments (patient results + QC results)
     * 
     * Verifies that parseQSegments() correctly extracts only Q-segments from
     * message containing both patient results (P, O, R segments) and QC results (Q
     * segments).
     */
    @Test
    public void testParseMessage_WithMixedSegments_ExtractsOnlyQSegments() {
        // Arrange - ASTM message with patient results and QC results
        String astmMessage = "H|\\^&|||TEST_ANALYZER^MODEL_1.0^V1.0|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "P|1||PATIENT_001|||M|19800101||\r" + "O|1||ORDER_001|||20250127||||\r"
                + "R|1|^^^GLUCOSE|105.5|mg/dL|N\r" + "R|2|^^^CHOLESTEROL|200.0|mg/dL|N\r"
                + "Q|1|GLUCOSE^QC_LOT_2025_001^N|105.2|mg/dL|20250127143000|N\r"
                + "Q|2|CHOLESTEROL^QC_LOT_2025_002^N|200.5|mg/dL|20250127143001|N\r" + "L|1|N\r";

        // Act
        List<QCSegmentData> results = astmQSegmentParser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should parse only Q-segments (2), not patient results", 2, results.size());

        // Verify only Q-segments are parsed, not R-segments
        for (QCSegmentData qcData : results) {
            assertTrue("All results should be from Q-segments", qcData.getTestCode() != null);
            assertNotNull("All results should have control lot number", qcData.getControlLotNumber());
            assertNotNull("All results should have control level", qcData.getControlLevel());
        }
    }

    /**
     * Test: Parse message with no Q-segments returns empty list
     * 
     * Verifies that parseQSegments() returns empty list when message contains no
     * Q-segments (patient results only).
     */
    @Test
    public void testParseMessage_WithNoQSegments_ReturnsEmptyList() {
        // Arrange - ASTM message with patient results only (no Q-segments)
        String astmMessage = "H|\\^&|||TEST_ANALYZER^MODEL_1.0^V1.0|INSTITUTION||20250127|||ASTM^LIS2-A2^LIS2-A2\r"
                + "P|1||PATIENT_001|||M|19800101||\r" + "O|1||ORDER_001|||20250127||||\r"
                + "R|1|^^^GLUCOSE|105.5|mg/dL|N\r" + "L|1|N\r";

        // Act
        List<QCSegmentData> results = astmQSegmentParser.parseQSegments(astmMessage);

        // Assert
        assertNotNull("Results should not be null", results);
        assertTrue("Should return empty list when no Q-segments", results.isEmpty());
    }
}
