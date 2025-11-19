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
 * Task Reference: T092
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

    @Override
    @Transactional
    public boolean reprocessMessage(AnalyzerError error) {
        // Validate input
        if (error == null || GenericValidator.isBlankOrNull(error.getRawMessage())) {
            LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                    "Cannot reprocess error: raw message is null or empty");
            return false;
        }

        // Check if analyzer has active mappings
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

        // Convert raw message string to InputStream
        InputStream messageStream = new ByteArrayInputStream(error.getRawMessage().getBytes(StandardCharsets.UTF_8));

        // Create ASTMAnalyzerReader and process message
        try {
            ASTMAnalyzerReader reader = (ASTMAnalyzerReader) AnalyzerReaderFactory.getReaderFor("astm");

            if (reader == null) {
                LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                        "Cannot reprocess error: ASTMAnalyzerReader not available");
                return false;
            }

            // Read the message stream
            boolean readSuccess = reader.readStream(messageStream);
            if (!readSuccess) {
                LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                        "Failed to read message stream: " + reader.getError());
                return false;
            }

            // Process the data (use system user ID for reprocessing)
            // Note: In production, we might want to use the original user ID or a
            // system user
            String systemUserId = "SYSTEM"; // TODO: Get actual system user ID
            boolean processSuccess = reader.processData(systemUserId);

            if (!processSuccess) {
                LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                        "Failed to process message: " + reader.getError());
                return false;
            }

            return true;
        } catch (Exception e) {
            LogEvent.logError(e);
            return false;
        }
    }
}
