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
 * Service implementation for reprocessing analyzer errors.
 *
 * <p>
 * Reprocesses failed analyzer messages through ASTMAnalyzerReader after
 * mappings are created. QC result processing is handled automatically by the
 * FHIR import pipeline when the bridge re-sends or the message is replayed.
 */
@Service
@Transactional
public class AnalyzerReprocessingServiceImpl implements AnalyzerReprocessingService {

    @Autowired
    private AnalyzerFieldMappingDAO analyzerFieldMappingDAO;

    @Override
    @Transactional
    public boolean reprocessMessage(AnalyzerError error) {
        if (error == null || GenericValidator.isBlankOrNull(error.getRawMessage())) {
            LogEvent.logError(this.getClass().getSimpleName(), "reprocessMessage",
                    "Cannot reprocess error: raw message is null or empty");
            return false;
        }

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

            String systemUserId = "SYSTEM";
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
