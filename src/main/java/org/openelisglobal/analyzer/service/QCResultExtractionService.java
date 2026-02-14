package org.openelisglobal.analyzer.service;

/**
 * Service interface for extracting QC result data from parsed Q-segments
 * 
 * 
 * This service applies QC field mappings to QCSegmentData (parsed from ASTM
 * Q-segments) to extract OpenELIS entity IDs and values needed to call Feature
 * 003's QCResultService.createQCResult() method.
 * 
 * Responsibilities: - Apply QC field mappings (test code → Test ID, control lot
 * number → Control Lot ID) - Convert control level string (L/N/H) to
 * ControlLevel enum - Convert result value string to BigDecimal - Apply unit
 * conversions via UnitMapping if configured - Validate required mappings are
 * present
 */
public interface QCResultExtractionService {

    /**
     * Extract QC result data from parsed Q-segment
     * 
     * 
     * Applies QC field mappings to QCSegmentData and returns QCResultDTO with all
     * required fields populated for calling Feature 003's QCResultService.
     * 
     * @param qcData     Parsed QC segment data from ASTM Q-segment
     * @param analyzerId Analyzer ID (from analyzer configuration)
     * @return QCResultDTO with mapped OpenELIS entity IDs and converted values
     * @throws LIMSRuntimeException if required mappings are missing or invalid
     */
    QCResultDTO extractQCResult(QCSegmentData qcData, String analyzerId);
}
