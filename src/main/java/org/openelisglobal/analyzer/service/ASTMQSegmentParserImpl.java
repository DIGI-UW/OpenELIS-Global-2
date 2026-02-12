package org.openelisglobal.analyzer.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Service;

/**
 * Implementation of ASTMQSegmentParser for parsing Q-segments from ASTM
 * messages
 * 
 * 
 * Parses ASTM LIS2-A2 Q-segments to extract QC result data. Handles: -
 * Extracting instrument ID from H-segment header - Parsing Q-segments with all
 * required fields - Handling multiple Q-segments in single message - Error
 * handling for malformed segments
 * 
 * Reference: ASTM LIS2-A2 specification for Q-segment format
 * 
 * Q-segment format:
 * Q|sequence|test_code^control_lot^control_level|result_value|unit|timestamp|flag
 */
@Service
public class ASTMQSegmentParserImpl implements ASTMQSegmentParser {

    private static final String Q_SEGMENT_PREFIX = "Q|";
    private static final String H_SEGMENT_PREFIX = "H|";
    private static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmss";
    private static final String FIELD_DELIMITER = "|";
    private static final String COMPOSITE_DELIMITER = "^";

    @Override
    public List<QCSegmentData> parseQSegments(String astmMessage) {
        if (astmMessage == null || astmMessage.trim().isEmpty()) {
            throw new LIMSRuntimeException("ASTM message cannot be null or empty");
        }

        String instrumentId = extractInstrumentIdFromHeader(astmMessage);

        List<QCSegmentData> qcResults = new ArrayList<>();
        String[] lines = astmMessage.split("\r|\n|\r\n");

        for (String line : lines) {
            if (line != null && line.startsWith(Q_SEGMENT_PREFIX)) {
                try {
                    QCSegmentData qcData = parseQSegment(line, instrumentId);
                    if (qcData != null) {
                        qcResults.add(qcData);
                    }
                } catch (Exception e) {
                    LogEvent.logError("Error parsing Q-segment: " + line, e);
                    throw new LIMSRuntimeException("Malformed Q-segment: " + e.getMessage());
                }
            }
        }

        return qcResults;
    }

    /**
     * Extract instrument ID from H-segment header
     * 
     * H-segment format: H|\\^&|||MANUFACTURER^MODEL^VERSION|... Instrument ID is
     * the first component of field 4 (MANUFACTURER)
     * 
     * @param astmMessage The complete ASTM message
     * @return Instrument ID extracted from H-segment, or null if not found
     */
    private String extractInstrumentIdFromHeader(String astmMessage) {
        String[] lines = astmMessage.split("\r|\n|\r\n");
        for (String line : lines) {
            if (line != null && line.startsWith(H_SEGMENT_PREFIX)) {
                String[] fields = line.split("\\" + FIELD_DELIMITER);
                if (fields.length >= 5) {
                    String manufacturerModel = fields[4]; // Field 4 (0-indexed)
                    if (manufacturerModel != null && !manufacturerModel.trim().isEmpty()) {
                        String[] components = manufacturerModel.split("\\" + COMPOSITE_DELIMITER);
                        if (components.length > 0 && components[0] != null && !components[0].trim().isEmpty()) {
                            return components[0].trim(); // Return first component (manufacturer/instrument ID)
                        }
                    }
                }
            }
        }
        return null; // Instrument ID not found in header
    }

    /**
     * Parse a single Q-segment line into QCSegmentData
     * 
     * Q-segment format:
     * Q|sequence|test_code^control_lot^control_level|result_value|unit|timestamp|flag
     * 
     * @param qSegmentLine The Q-segment line to parse
     * @param instrumentId The instrument ID extracted from H-segment header
     * @return QCSegmentData object with parsed fields
     * @throws LIMSRuntimeException if required fields are missing or malformed
     */
    private QCSegmentData parseQSegment(String qSegmentLine, String instrumentId) {
        if (qSegmentLine == null || !qSegmentLine.startsWith(Q_SEGMENT_PREFIX)) {
            throw new LIMSRuntimeException("Invalid Q-segment line: " + qSegmentLine);
        }

        String[] fields = qSegmentLine.split("\\" + FIELD_DELIMITER);

        // Validate minimum required fields: Q|sequence|test_info|result|unit|timestamp
        if (fields.length < 6) {
            throw new LIMSRuntimeException(
                    "Q-segment missing required fields. Expected at least 6 fields, got: " + fields.length);
        }

        // Field 0: Q (segment type) - already validated
        // Field 1: sequence number - not used but validated
        // Field 2: test_code^control_lot^control_level (composite field)
        // Field 3: result_value
        // Field 4: unit
        // Field 5: timestamp
        // Field 6+: flags and optional fields

        String testInfo = fields.length > 2 ? fields[2] : null;
        String resultValue = fields.length > 3 ? fields[3] : null;
        String unit = fields.length > 4 ? fields[4] : null;
        String timestampStr = fields.length > 5 ? fields[5] : null;

        if (testInfo == null || testInfo.trim().isEmpty()) {
            throw new LIMSRuntimeException("Q-segment missing test code/control lot/level");
        }
        if (resultValue == null || resultValue.trim().isEmpty()) {
            throw new LIMSRuntimeException("Q-segment missing result value");
        }
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            throw new LIMSRuntimeException("Q-segment missing timestamp");
        }

        // Parse composite field: test_code^control_lot^control_level
        String[] testComponents = testInfo.split("\\" + COMPOSITE_DELIMITER);
        String testCode = testComponents.length > 0 ? testComponents[0] : null;
        String controlLotNumber = testComponents.length > 1 ? testComponents[1] : null;
        String controlLevel = testComponents.length > 2 ? testComponents[2] : null;

        if (testCode == null || testCode.trim().isEmpty()) {
            throw new LIMSRuntimeException("Q-segment missing test code");
        }
        if (controlLotNumber == null || controlLotNumber.trim().isEmpty()) {
            throw new LIMSRuntimeException("Q-segment missing control lot number");
        }
        if (controlLevel == null || controlLevel.trim().isEmpty()) {
            throw new LIMSRuntimeException("Q-segment missing control level");
        }

        // Validate control level (must be L, N, or H)
        if (!"L".equals(controlLevel) && !"N".equals(controlLevel) && !"H".equals(controlLevel)) {
            throw new LIMSRuntimeException("Invalid control level: " + controlLevel + ". Must be L, N, or H");
        }

        // Parse timestamp (format: yyyyMMddHHmmss)
        Date timestamp = parseTimestamp(timestampStr.trim());

        QCSegmentData qcData = new QCSegmentData();
        qcData.setInstrumentId(instrumentId);
        qcData.setTestCode(testCode.trim());
        qcData.setControlLotNumber(controlLotNumber.trim());
        qcData.setControlLevel(controlLevel.trim());
        qcData.setResultValue(resultValue.trim());
        qcData.setUnit(unit != null ? unit.trim() : ""); // Unit may be empty for qualitative results
        qcData.setTimestamp(timestamp);

        return qcData;
    }

    /**
     * Parse timestamp from string format yyyyMMddHHmmss to Date
     * 
     * @param timestampStr Timestamp string in format yyyyMMddHHmmss
     * @return Parsed Date object
     * @throws LIMSRuntimeException if timestamp cannot be parsed
     */
    private Date parseTimestamp(String timestampStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
            sdf.setLenient(false); // Strict parsing
            return sdf.parse(timestampStr);
        } catch (ParseException e) {
            throw new LIMSRuntimeException(
                    "Invalid timestamp format: " + timestampStr + ". Expected: " + TIMESTAMP_FORMAT);
        }
    }
}
