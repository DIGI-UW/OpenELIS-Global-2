package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Wrapper class for AnalyzerLineInserter that applies field mappings before
 * delegating to plugin inserter
 * 
 * 
 * Wrapper logic: 1. Receive raw ASTM message segments from ASTMAnalyzerReader
 * 2. Call MappingApplicationService.applyMappings() to transform segments using
 * configured mappings 3. If mappings found and transformation successful:
 * Delegate transformed data to original plugin inserter 4. If mappings not
 * found or transformation fails: Create AnalyzerError record, return error (do
 * not delegate to plugin inserter)
 * 
 * Per research.md Section 7
 */
public class MappingAwareAnalyzerLineInserter extends AnalyzerLineInserter {

    private final AnalyzerLineInserter originalInserter;
    private final Analyzer analyzer;
    private final MappingApplicationService mappingApplicationService;
    private final AnalyzerErrorService analyzerErrorService;
    private final ASTMQSegmentParser astmQSegmentParser;
    private final QCResultExtractionService qcResultExtractionService;
    private final QCResultProcessingService qcResultProcessingService;
    private String error;

    public MappingAwareAnalyzerLineInserter(AnalyzerLineInserter originalInserter, Analyzer analyzer) {
        this.originalInserter = originalInserter;
        this.analyzer = analyzer;
        this.mappingApplicationService = SpringContext.getBean(MappingApplicationService.class);
        this.analyzerErrorService = SpringContext.getBean(AnalyzerErrorService.class);
        this.astmQSegmentParser = SpringContext.getBean(ASTMQSegmentParser.class);
        this.qcResultExtractionService = SpringContext.getBean(QCResultExtractionService.class);
        this.qcResultProcessingService = SpringContext.getBean(QCResultProcessingService.class);
        this.error = null;
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        error = null;

        if (lines == null || lines.isEmpty()) {
            error = "Empty message lines";
            return false;
        }

        if (analyzer == null || analyzer.getId() == null) {
            error = "Analyzer not configured";
            return false;
        }

        try {
            if (!mappingApplicationService.hasActiveMappings(analyzer.getId())) {
                // No mappings configured - delegate to original inserter (backward
                // compatibility)
                return originalInserter.insert(lines, currentUserId);
            }

            MappingApplicationResult result = mappingApplicationService.applyMappings(analyzer.getId(), lines);

            if (!result.isSuccess()) {
                String errorMessage = "Failed to apply mappings: " + String.join(", ", result.getErrors());
                createError(errorMessage, lines);
                error = errorMessage;
                return false;
            }

            if (!result.getUnmappedFields().isEmpty()) {
                // Some fields are unmapped - create error but still try to process
                String errorMessage = "Unmapped fields detected: " + String.join(", ", result.getUnmappedFields());
                createError(errorMessage, lines);
                // Continue processing - partial data may still be useful
            }

            boolean success = originalInserter.insert(result.getTransformedLines(), currentUserId);

            if (!success) {
                error = originalInserter.getError();
                if (error == null || error.isEmpty()) {
                    error = "Failed to insert analyzer data";
                }
            }

            // FR-021: QC results processed within same transaction as patient results
            if (success) {
                processQCSegments(lines);
            }

            return success;

        } catch (Exception e) {
            LogEvent.logError("Error applying mappings: " + e.getMessage(), e);
            String errorMessage = "Error applying mappings: " + e.getMessage();
            createError(errorMessage, lines);
            error = errorMessage;
            return false;
        }
    }

    @Override
    public String getError() {
        return error;
    }

    /**
     * Process Q-segments (QC results) from ASTM message
     * 
     * 
     * Detects Q-segments in ASTM message, parses them, extracts QC results using
     * field mappings, and processes them via QCResultProcessingService (which calls
     * Feature 003's QCResultService). QC processing occurs within the same
     * transaction as patient result processing (per FR-021).
     * 
     * @param lines Raw ASTM message lines
     */
    private void processQCSegments(List<String> lines) {
        try {
            String astmMessage = String.join("\r", lines);

            List<QCSegmentData> qcSegments = astmQSegmentParser.parseQSegments(astmMessage);

            if (qcSegments.isEmpty()) {
                return;
            }

            for (QCSegmentData qcSegmentData : qcSegments) {
                try {
                    QCResultDTO qcResultDTO = qcResultExtractionService.extractQCResult(qcSegmentData,
                            analyzer.getId());

                    qcResultProcessingService.processQCResult(qcResultDTO, analyzer.getId());

                } catch (Exception e) {
                    // Error type: QC_MAPPING_INCOMPLETE (per FR-011)
                    String errorMessage = String.format(
                            "Failed to process QC result for analyzer %s, test code %s, control lot %s: %s",
                            analyzer.getId(), qcSegmentData.getTestCode(), qcSegmentData.getControlLotNumber(),
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    createQCError(errorMessage, astmMessage);
                    LogEvent.logError("Failed to process QC segment: " + errorMessage, e);
                }
            }

        } catch (Exception e) {
            // Error parsing Q-segments - log but don't fail patient result processing
            LogEvent.logError("Error parsing Q-segments from ASTM message: " + e.getMessage(), e);
        }
    }

    /**
     * Create AnalyzerError record for QC processing failures
     * 
     * 
     * Creates AnalyzerError with type MAPPING (will be QC_MAPPING_INCOMPLETE per
     * QC_MAPPING_INCOMPLETE) and severity ERROR for QC mapping errors (per FR-011).
     * 
     * @param errorMessage Error message
     * @param rawMessage   Raw ASTM message
     */
    private void createQCError(String errorMessage, String rawMessage) {
        try {
            analyzerErrorService.createError(analyzer, AnalyzerError.ErrorType.QC_MAPPING_INCOMPLETE,
                    AnalyzerError.Severity.ERROR, errorMessage, rawMessage);
        } catch (Exception e) {
            LogEvent.logError("Failed to create AnalyzerError for QC processing: " + e.getMessage(), e);
        }
    }

    /**
     * Create AnalyzerError record for unmapped/failed messages
     * 
     * @param errorMessage Error message
     * @param lines        Raw message lines
     */
    private void createError(String errorMessage, List<String> lines) {
        try {
            String rawMessage = String.join("\n", lines);
            analyzerErrorService.createError(analyzer, AnalyzerError.ErrorType.MAPPING, AnalyzerError.Severity.ERROR,
                    errorMessage, rawMessage);
        } catch (Exception e) {
            LogEvent.logError("Failed to create AnalyzerError: " + e.getMessage(), e);
        }
    }
}
