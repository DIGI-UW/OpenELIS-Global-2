package org.openelisglobal.analyzer.service;

import java.util.List;

/**
 * Service interface for parsing Q-segments (Quality Control result segments)
 * from ASTM messages
 * 
 * 
 * This service parses ASTM LIS2-A2 Q-segments to extract QC result data
 * including: instrument ID (from H-segment header), test code, control lot
 * number, control level (Low/Normal/High), result value (numeric or
 * qualitative), unit of measure, and timestamp.
 * 
 * Reference: ASTM LIS2-A2 specification for Q-segment format
 * 
 * Q-segment format:
 * Q|sequence|test_code^control_lot^control_level|result_value|unit|timestamp|flag
 */
public interface ASTMQSegmentParser {

    /**
     * Parse all Q-segments from ASTM message
     * 
     * 
     * Parses ASTM message to extract all Q-segments (Quality Control result
     * segments) and returns list of QCSegmentData objects containing extracted QC
     * data.
     * 
     * @param astmMessage The complete ASTM message containing H-segment (header)
     *                    and Q-segments (QC results)
     * @return List of QCSegmentData objects, one per Q-segment found in message.
     *         Returns empty list if no Q-segments found.
     * @throws LIMSRuntimeException if message is null, empty, or contains malformed
     *                              Q-segments with missing required fields
     */
    List<QCSegmentData> parseQSegments(String astmMessage);
}
