package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerError;

/**
 * Service interface for reprocessing analyzer errors
 * 
 * 
 * Provides methods for reprocessing failed analyzer messages after mappings are
 * created.
 */
public interface AnalyzerReprocessingService {

    /**
     * Reprocess a raw message from an AnalyzerError
     * 
     * This method retrieves the raw ASTM message from the error, checks if active
     * mappings exist for the analyzer, and reprocesses the message through
     * ASTMAnalyzerReader.
     * 
     * @param error The error containing the raw message to reprocess
     * @return true if reprocessing succeeded, false otherwise
     */
    boolean reprocessMessage(AnalyzerError error);
}
