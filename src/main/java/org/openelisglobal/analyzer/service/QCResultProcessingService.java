package org.openelisglobal.analyzer.service;

/**
 * Service interface for processing QC results from ASTM Q-segments
 * 
 * 
 * This service coordinates the complete QC result processing workflow: (1)
 * Q-segment parsing (via ASTMQSegmentParser), (2) Mapping application (via
 * QCResultExtractionService), (3) Persistence via Feature 003's
 * QCResultService.createQCResult() method.
 * 
 * The service ensures that QC results are processed within the same transaction
 * as patient result processing (per FR-021 requirement: "within the same
 * transaction").
 * 
 * Integration Pattern: Direct service call from Feature 004's message
 * processing service to Feature 003's QCResultService.createQCResult() method.
 * This ensures immediate consistency and follows the 5-layer architecture
 * pattern (004's service calls 003's service).
 * 
 * Error Handling: If Feature 003's QCResultService is unavailable or throws an
 * exception, this service should create an AnalyzerError with type
 * QC_MAPPING_INCOMPLETE (per FR-011).
 */
public interface QCResultProcessingService {

    /**
     * Processes QC result through Feature 003's QCResultService
     * 
     * This method coordinates: (1) Receives QCResultDTO from
     * QCResultExtractionService (after Q-segment parsing and mapping), (2) Calls
     * Feature 003's QCResultService.createQCResult() to persist the QC result, (3)
     * Returns the created QCResult entity.
     * 
     * Transaction Boundary: This method should use @Transactional with REQUIRED
     * propagation to ensure it runs within the same transaction as patient result
     * processing. This ensures atomicity: both patient and QC results are persisted
     * together or both fail together.
     * 
     * @param qcResultDTO QC result data extracted from Q-segment (from
     *                    QCResultExtractionService)
     * @param analyzerId  Analyzer ID (for association with analyzer configuration)
     * @return Created QCResult entity (from Feature 003's QCResultService)
     * @throws RuntimeException if Feature 003's QCResultService is unavailable or
     *                          throws an exception (should be wrapped and handled
     *                          gracefully)
     */
    Object processQCResult(QCResultDTO qcResultDTO, String analyzerId);
}
