package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of QCResultProcessingService for processing QC results from
 * ASTM Q-segments
 * 
 * 
 * This service coordinates the complete QC result processing workflow: (1)
 * Receives QCResultDTO from QCResultExtractionService (after Q-segment parsing
 * and mapping), (2) Calls Feature 003's QCResultService.createQCResult() to
 * persist the QC result, (3) Returns the created QCResult entity.
 * 
 * Transaction Boundary: Uses @Transactional with REQUIRED propagation to ensure
 * it runs within the same transaction as patient result processing. This
 * ensures atomicity: both patient and QC results are persisted together or both
 * fail together (per FR-021 requirement: "within the same transaction").
 * 
 * Integration Pattern: Direct service call from Feature 004's message
 * processing service to Feature 003's QCResultService.createQCResult() method.
 * This ensures immediate consistency and follows the 5-layer architecture
 * pattern (004's service calls 003's service).
 * 
 * Error Handling: If Feature 003's QCResultService is unavailable or throws an
 * exception, this service creates an AnalyzerError with type MAPPING (will be
 * QC_MAPPING_INCOMPLETE and severity ERROR (per FR-011).
 * 
 * Note: Feature 003's QCResultService is injected via @Autowired(required =
 * false) since it may not be implemented yet. When Feature 003 is implemented,
 * this service will automatically use the real QCResultService.
 */
@Service
@Transactional
public class QCResultProcessingServiceImpl implements QCResultProcessingService {

    /**
     * Feature 003's QCResultService interface
     * 
     * This service is NOT autowired since Feature 003 may not be implemented yet.
     * When Feature 003 is implemented, this can be changed to:
     * 
     * @Autowired(required = false) private
     *                     org.openelisglobal.qc.service.QCResultService
     *                     qcResultService;
     * 
     *                     For now, it will be null and handled gracefully. It can
     *                     be set manually via setter for testing or when Feature
     *                     003 is available.
     * 
     *                     The interface signature matches Feature 003's spec.md
     *                     FR-008 and research.md line 173-190.
     */
    private Object qcResultService; // Placeholder for Feature 003's QCResultService - NOT autowired

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    @Autowired
    private AnalyzerDAO analyzerDAO;

    /**
     * Setter for testing purposes (allows Mockito injection)
     */
    public void setQcResultService(Object qcResultService) {
        this.qcResultService = qcResultService;
    }

    /**
     * Setter for testing purposes (allows Mockito injection)
     */
    public void setAnalyzerErrorService(AnalyzerErrorService analyzerErrorService) {
        this.analyzerErrorService = analyzerErrorService;
    }

    /**
     * Setter for testing purposes (allows Mockito injection)
     */
    public void setAnalyzerDAO(AnalyzerDAO analyzerDAO) {
        this.analyzerDAO = analyzerDAO;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Object processQCResult(QCResultDTO qcResultDTO, String analyzerId) {
        if (qcResultDTO == null) {
            throw new IllegalArgumentException("QCResultDTO cannot be null");
        }
        if (analyzerId == null || analyzerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Analyzer ID cannot be null or empty");
        }

        if (qcResultService == null) {
            // Service unavailable - create AnalyzerError (per FR-011)
            String errorMessage = String.format(
                    "QCResultService not available for analyzer %s. QC result processing requires Feature 003 (Westgard QC) to be implemented.",
                    analyzerId);
            createQCError(analyzerId, errorMessage, null);
            throw new LIMSRuntimeException("QCResultService not available: " + errorMessage);
        }

        try {
            Analyzer analyzer = analyzerDAO.get(analyzerId)
                    .orElseThrow(() -> new LIMSRuntimeException("Analyzer not found: " + analyzerId));

            Timestamp timestamp = qcResultDTO.getTimestamp() != null
                    ? new Timestamp(qcResultDTO.getTimestamp().getTime())
                    : new Timestamp(System.currentTimeMillis());

            // Call Feature 003's QCResultService.createQCResult()
            // Note: Using reflection since Feature 003's service doesn't exist yet
            // When Feature 003 is implemented, this can be replaced with direct method
            // call:
            // return qcResultService.createQCResult(
            // qcResultDTO.getAnalyzerId(),
            // qcResultDTO.getTestId(),
            // qcResultDTO.getControlLotId(),
            // qcResultDTO.getControlLevel(),
            // qcResultDTO.getResultValue(),
            // qcResultDTO.getUnit(),
            // timestamp);

            try {
                java.lang.reflect.Method createQCResultMethod = qcResultService.getClass().getMethod("createQCResult",
                        String.class, String.class, String.class, QCResultDTO.ControlLevel.class, BigDecimal.class,
                        String.class, Timestamp.class);

                Object qcResult = createQCResultMethod.invoke(qcResultService,
                        qcResultDTO.getAnalyzerId() != null ? qcResultDTO.getAnalyzerId() : analyzerId,
                        qcResultDTO.getTestId(), qcResultDTO.getControlLotId(), qcResultDTO.getControlLevel(),
                        qcResultDTO.getResultValue(), qcResultDTO.getUnit(), timestamp);

                return qcResult;

            } catch (Exception e) {
                // Method call failed - create AnalyzerError (per FR-011)
                String errorMessage = String.format(
                        "Failed to create QC result for analyzer %s, test %s, control lot %s: %s", analyzerId,
                        qcResultDTO.getTestId(), qcResultDTO.getControlLotId(),
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                createQCError(analyzerId, errorMessage, null);
                throw new LIMSRuntimeException("Failed to create QC result: " + errorMessage, e);
            }

        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Unexpected error - create AnalyzerError (per FR-011)
            String errorMessage = String.format("Unexpected error processing QC result for analyzer %s: %s", analyzerId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            createQCError(analyzerId, errorMessage, null);
            LogEvent.logError(errorMessage, e);
            throw new LIMSRuntimeException("Failed to process QC result: " + errorMessage, e);
        }
    }

    /**
     * Create AnalyzerError for QC processing failures
     * 
     * @param analyzerId   Analyzer ID
     * @param errorMessage Error message
     * @param rawMessage   Raw ASTM message (if available)
     */
    private void createQCError(String analyzerId, String errorMessage, String rawMessage) {
        try {
            Analyzer analyzer = analyzerDAO.get(analyzerId).orElse(null);
            if (analyzer == null) {
                LogEvent.logError("QCResultProcessingServiceImpl", "createQCError",
                        "Cannot create error: Analyzer not found: " + analyzerId);
                return;
            }

            // Create error with type QC_MAPPING_INCOMPLETE (per FR-011)
            analyzerErrorService.createError(analyzer, AnalyzerError.ErrorType.QC_MAPPING_INCOMPLETE,
                    AnalyzerError.Severity.ERROR, errorMessage, rawMessage);

        } catch (Exception e) {
            // Log error creation failure but don't propagate (error logging shouldn't
            // fail processing)
            LogEvent.logError("Failed to create AnalyzerError: " + e.getMessage(), e);
        }
    }
}
