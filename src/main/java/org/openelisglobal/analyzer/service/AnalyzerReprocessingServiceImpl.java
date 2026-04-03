package org.openelisglobal.analyzer.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzer.dao.AnalyzerFieldMappingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping;
import org.openelisglobal.analyzerimport.analyzerreaders.ASTMAnalyzerReader;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderFactory;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for reprocessing analyzer errors
 * 
 * 
 * Provides business logic for reprocessing failed analyzer messages after
 * mappings are created. Reprocesses raw ASTM messages through
 * ASTMAnalyzerReader.
 */
@Service
@Transactional
public class AnalyzerReprocessingServiceImpl implements AnalyzerReprocessingService {

    @Autowired
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Autowired
    private ASTMQSegmentParser astmQSegmentParser;

    @Autowired
    private QCResultExtractionService qcResultExtractionService;

    @Autowired
    private QCResultProcessingService qcResultProcessingService;

    @Override
    @Transactional
    public boolean reprocessMessage(AnalyzerError error) {
        if (error == null || GenericValidator.isBlankOrNull(error.getRawMessage())) {
            LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                    "Cannot reprocess error: raw message is null or empty");
            return false;
        }

        // Note: Analyzer uses String IDs in Java, but findActiveMappingsByAnalyzerId
        // accepts String and handles conversion internally
        // Reference: ID_TYPE_ANALYSIS.md
        String analyzerId = error.getAnalyzer().getId();
        List<AnalyzerFieldMapping> activeMappings = analyzerFieldMappingDAO.findActiveMappingsByAnalyzerId(analyzerId);

        if (activeMappings == null || activeMappings.isEmpty()) {
            LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                    "Cannot reprocess error: no active mappings found for analyzer " + analyzerId);
            return false;
        }

        InputStream messageStream = new ByteArrayInputStream(error.getRawMessage().getBytes(StandardCharsets.UTF_8));

        try {
            ASTMAnalyzerReader reader = (ASTMAnalyzerReader) AnalyzerReaderFactory.getReaderFor("astm");

            if (reader == null) {
                LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                        "Cannot reprocess error: ASTMAnalyzerReader not available");
                return false;
            }

            boolean readSuccess = reader.readStream(messageStream);
            if (!readSuccess) {
                LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                        "Failed to read message stream: " + reader.getError());
                return false;
            }

            // Uses SYSTEM user; SecurityContext integration deferred to Phase 2
            String systemUserId = "SYSTEM";
            boolean processSuccess = reader.processData(systemUserId);

            if (!processSuccess) {
                LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                        "Failed to process message: " + reader.getError());
                return false;
            }

            // FR-011: QC messages are reprocessed when mappings are resolved
            processQCSegmentsInReprocessedMessage(error);

            return true;
        } catch (Exception e) {
            LogEvent.logError(e);
            return false;
        }
    }

    /**
     * Process Q-segments (QC results) in reprocessed message
     * 
     * 
     * Detects Q-segments in reprocessed message, parses them, extracts QC results
     * with updated mappings, and processes them via QCResultProcessingService. This
     * ensures QC messages are reprocessed when mappings are resolved (per FR-011).
     * 
     * @param error AnalyzerError containing the raw message
     */
    private void processQCSegmentsInReprocessedMessage(AnalyzerError error) {
        try {
            String rawMessage = error.getRawMessage();
            if (GenericValidator.isBlankOrNull(rawMessage)) {
                return;
            }

            String analyzerId = error.getAnalyzer().getId();

            List<org.openelisglobal.analyzer.service.QCSegmentData> qcSegments = astmQSegmentParser
                    .parseQSegments(rawMessage);

            if (qcSegments.isEmpty()) {
                return;
            }

            for (org.openelisglobal.analyzer.service.QCSegmentData qcSegmentData : qcSegments) {
                try {
                    org.openelisglobal.analyzer.service.QCResultDTO qcResultDTO = qcResultExtractionService
                            .extractQCResult(qcSegmentData, analyzerId);

                    qcResultProcessingService.processQCResult(qcResultDTO, analyzerId);

                } catch (Exception e) {
                    // QC mapping or processing error - log but don't fail reprocessing
                    // (patient results may have been successfully reprocessed)
                    LogEvent.logError(String.format(
                            "Failed to process QC result during reprocessing for analyzer %s, test code %s, control lot %s: %s",
                            analyzerId, qcSegmentData.getTestCode(), qcSegmentData.getControlLotNumber(),
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
                }
            }

        } catch (Exception e) {
            // Error parsing Q-segments - log but don't fail reprocessing
            LogEvent.logError("Error parsing Q-segments during reprocessing: " + e.getMessage(), e);
        }
    }
}
